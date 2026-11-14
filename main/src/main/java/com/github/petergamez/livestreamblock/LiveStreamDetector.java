package com.github.petergamez.livestreamblock;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Core detector identifying whether an audio track or stream is a live stream or radio station.
 */
public class LiveStreamDetector {

    private final LiveStreamBlockConfig config;
    private final LiveStreamBlockStats stats;

    // Common radio stream extensions and URL keywords
    private static final String[] RADIO_EXTENSIONS = {
            ".m3u8", ".m3u", ".pls", ".asx"
    };

    private static final String[] RADIO_PATH_KEYWORDS = {
            "/listen", "/icecast", "/shoutcast", "/radio", "/station", "/broadcast"
    };

    private static final String[] RADIO_DOMAINS = {
            "tunein.com", "iheart.com", "radio.net", "zeno.fm", "laut.fm",
            "radioparadise.com", "somafm.com", "internet-radio.com", "streamguys.com",
            "radionomy.com", "online-radio.eu"
    };

    private static final String[] RADIO_TITLE_KEYWORDS = {
            "radio", "fm", "station", "broadcast", "webradio", "airplay"
    };

    public LiveStreamDetector(LiveStreamBlockConfig config, LiveStreamBlockStats stats) {
        this.config = config;
        this.stats = stats;
    }

    public LiveStreamDetector(LiveStreamBlockConfig config) {
        this(config, new LiveStreamBlockStats());
    }

    public LiveStreamBlockConfig getConfig() {
        return config;
    }

    public LiveStreamBlockStats getStats() {
        return stats;
    }

    /**
     * Evaluates an AudioTrack.
     */
    public DetectionResult check(AudioTrack track) {
        if (track == null) {
            return DetectionResult.allowed();
        }
        AudioTrackInfo info = track.getInfo();
        String sourceName = track.getSourceManager() != null ? track.getSourceManager().getSourceName() : null;
        return check(info.title, info.author, info.uri, info.identifier, sourceName, info.isStream, info.length);
    }

    /**
     * Evaluates track attributes directly (supports Lavalink protocol TrackInfo without Lavaplayer instance).
     */
    public DetectionResult check(String title,
                                 String author,
                                 String uri,
                                 String identifier,
                                 String sourceName,
                                 boolean isStream,
                                 long length) {
        stats.incrementTracksChecked();

        if (!config.isEnabled()) {
            return DetectionResult.allowed();
        }

        // 1. Whitelist Check
        if (matchesAnyPattern(uri, config.getAllowedUrls()) || matchesAnyPattern(identifier, config.getAllowedUrls())) {
            return DetectionResult.whitelisted("Matched allowedUrls whitelist");
        }

        // 2. Custom Blacklist Check
        if (matchesAnyPattern(uri, config.getBlockedUrls()) || matchesAnyPattern(identifier, config.getBlockedUrls())) {
            DetectionResult res = DetectionResult.blocked(DetectionReason.BLOCKED_CUSTOM_URL,
                    config.getBlockMessage(), "Matched custom blockedUrls pattern");
            stats.incrementBlocked(res.getReason());
            return res;
        }

        if (matchesAnyKeyword(title, config.getBlockedKeywords()) || matchesAnyKeyword(author, config.getBlockedKeywords())) {
            DetectionResult res = DetectionResult.blocked(DetectionReason.BLOCKED_CUSTOM_KEYWORD,
                    config.getBlockMessage(), "Matched custom blockedKeywords pattern");
            stats.incrementBlocked(res.getReason());
            return res;
        }

        // Determine source identity
        String effectiveSource = sourceName != null ? sourceName.toLowerCase(Locale.ROOT) : detectSourceFromUri(uri);

        // 3. YouTube Live Stream
        if (isYouTube(effectiveSource, uri)) {
            if (isStream || length == Long.MAX_VALUE) {
                if (config.isBlockLiveStreams() && config.getSources().isYoutube()) {
                    DetectionResult res = DetectionResult.blocked(DetectionReason.BLOCKED_LIVESTREAM,
                            config.getBlockMessage(), "YouTube Live Stream");
                    stats.incrementBlocked(res.getReason());
                    return res;
                }
            }
            return DetectionResult.allowed();
        }

        // 4. Twitch Stream
        if (isTwitch(effectiveSource, uri)) {
            if (config.isBlockLiveStreams() && config.getSources().isTwitch()) {
                DetectionResult res = DetectionResult.blocked(DetectionReason.BLOCKED_LIVESTREAM,
                        config.getBlockMessage(), "Twitch Live Stream");
                stats.incrementBlocked(res.getReason());
                return res;
            }
            return DetectionResult.allowed();
        }

        // 5. Other Video Livestream Platforms (Kick, TikTok, Vimeo, Nico, etc.)
        if (isVideoLivePlatform(effectiveSource, uri)) {
            if (isStream || length == Long.MAX_VALUE) {
                if (config.isBlockLiveStreams()) {
                    DetectionResult res = DetectionResult.blocked(DetectionReason.BLOCKED_LIVESTREAM,
                            config.getBlockMessage(), "Live Stream (" + effectiveSource + ")");
                    stats.incrementBlocked(res.getReason());
                    return res;
                }
            }
            return DetectionResult.allowed();
        }

        // 6. Radio / Audio Broadcast Detection
        boolean isRadio = isRadioStream(effectiveSource, uri, title, author, isStream, length);
        if (isRadio) {
            if (config.isBlockRadio()) {
                DetectionResult res = DetectionResult.blocked(DetectionReason.BLOCKED_RADIO,
                        config.getBlockMessage(), "Radio / Audio Broadcast");
                stats.incrementBlocked(res.getReason());
                return res;
            }
            return DetectionResult.allowed();
        }

        // 7. General Streams (isStream == true or length == Long.MAX_VALUE)
        if (isStream || length == Long.MAX_VALUE) {
            // Check SoundCloud stream
            if ("soundcloud".equals(effectiveSource) && !config.getSources().isSoundcloud()) {
                return DetectionResult.allowed();
            }

            if (config.isBlockOtherStreams()) {
                DetectionReason reason = config.isBlockLiveStreams() ? DetectionReason.BLOCKED_LIVESTREAM
                        : (config.isBlockRadio() ? DetectionReason.BLOCKED_RADIO : DetectionReason.BLOCKED_OTHER_STREAM);
                DetectionResult res = DetectionResult.blocked(reason, config.getBlockMessage(),
                        "Live Stream (" + (effectiveSource != null ? effectiveSource : "unknown") + ")");
                stats.incrementBlocked(res.getReason());
                return res;
            }
        }

        return DetectionResult.allowed();
    }

    public static boolean isYouTube(String sourceName, String uri) {
        if ("youtube".equalsIgnoreCase(sourceName)) {
            return true;
        }
        if (uri == null) return false;
        String lower = uri.toLowerCase(Locale.ROOT);
        return lower.contains("youtube.com") || lower.contains("youtu.be");
    }

    public static boolean isTwitch(String sourceName, String uri) {
        if ("twitch".equalsIgnoreCase(sourceName)) {
            return true;
        }
        if (uri == null) return false;
        String lower = uri.toLowerCase(Locale.ROOT);
        return lower.contains("twitch.tv");
    }

    public static boolean isVideoLivePlatform(String sourceName, String uri) {
        if (sourceName != null) {
            String lower = sourceName.toLowerCase(Locale.ROOT);
            if (lower.contains("twitch") || lower.contains("kick") || lower.contains("tiktok")
                    || lower.contains("vimeo") || lower.contains("nico") || lower.contains("bilibili")) {
                return true;
            }
        }
        if (uri != null) {
            String lower = uri.toLowerCase(Locale.ROOT);
            return lower.contains("kick.com") || lower.contains("tiktok.com")
                    || lower.contains("vimeo.com") || lower.contains("nicovideo.jp")
                    || lower.contains("bilibili.com");
        }
        return false;
    }

    public static boolean isRadioStream(String sourceName,
                                        String uri,
                                        String title,
                                        String author,
                                        boolean isStream,
                                        long length) {
        boolean isIndefinite = isStream || length == Long.MAX_VALUE;

        // Specific HTTP source checks
        boolean isHttpSource = "http".equalsIgnoreCase(sourceName)
                || "local".equalsIgnoreCase(sourceName)
                || (uri != null && (uri.startsWith("http://") || uri.startsWith("https://")));

        if (uri != null) {
            String lowerUri = uri.toLowerCase(Locale.ROOT);

            // 1. Radio playlist file extensions (.m3u, .pls, etc.)
            for (String ext : RADIO_EXTENSIONS) {
                if (lowerUri.endsWith(ext) || lowerUri.contains(ext + "?")) {
                    return true;
                }
            }

            // 2. Radio streaming domains
            for (String domain : RADIO_DOMAINS) {
                if (lowerUri.contains(domain)) {
                    return true;
                }
            }

            // 3. Common radio stream port numbers (:8000, :8080, :8002, :8443, :8004, :8006, :8008)
            if (lowerUri.contains(":8")) {
                try {
                    URI parsed = URI.create(uri);
                    int port = parsed.getPort();
                    if (port == 8000 || port == 8080 || port == 8002 || port == 8004
                            || port == 8006 || port == 8008 || port == 8443) {
                        return true;
                    }
                } catch (Exception ignored) {
                }
            }

            // 4. URL path markers
            for (String kw : RADIO_PATH_KEYWORDS) {
                if (lowerUri.contains(kw) && isIndefinite) {
                    return true;
                }
            }
        }

        // 5. Title / Author contains radio terms while being an indefinite stream
        if (isIndefinite) {
            String lowerTitle = title != null ? title.toLowerCase(Locale.ROOT) : "";
            String lowerAuthor = author != null ? author.toLowerCase(Locale.ROOT) : "";

            for (String kw : RADIO_TITLE_KEYWORDS) {
                if (hasWord(lowerTitle, kw) || hasWord(lowerAuthor, kw)) {
                    return true;
                }
            }

            // 6. Any HTTP stream that is indefinite duration
            if (isHttpSource) {
                return true;
            }
        }

        return false;
    }

    private static String detectSourceFromUri(String uri) {
        if (uri == null) return "unknown";
        String lower = uri.toLowerCase(Locale.ROOT);
        if (lower.contains("youtube.com") || lower.contains("youtu.be")) return "youtube";
        if (lower.contains("twitch.tv")) return "twitch";
        if (lower.contains("soundcloud.com")) return "soundcloud";
        if (lower.startsWith("http://") || lower.startsWith("https://")) return "http";
        return "unknown";
    }

    private static final Map<String, Pattern> COMPILED_PATTERN_CACHE = new ConcurrentHashMap<>();
    private static final Pattern INVALID_PATTERN = Pattern.compile("^\\b$");

    private static Pattern getCompiledPattern(String patternStr) {
        Pattern cached = COMPILED_PATTERN_CACHE.get(patternStr);
        if (cached != null) {
            return cached == INVALID_PATTERN ? null : cached;
        }
        try {
            Pattern compiled = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
            if (COMPILED_PATTERN_CACHE.size() > 1000) {
                COMPILED_PATTERN_CACHE.clear();
            }
            COMPILED_PATTERN_CACHE.put(patternStr, compiled);
            return compiled;
        } catch (PatternSyntaxException e) {
            COMPILED_PATTERN_CACHE.put(patternStr, INVALID_PATTERN);
            return null;
        }
    }

    private static boolean matchesAnyPattern(String text, List<String> patterns) {
        if (text == null || patterns == null || patterns.isEmpty()) {
            return false;
        }
        for (String pattern : patterns) {
            if (pattern == null || pattern.trim().isEmpty()) continue;
            String trimmed = pattern.trim();
            // Check direct contains / equality
            if (text.equalsIgnoreCase(trimmed) || text.contains(trimmed)) {
                return true;
            }
            // Check regex via cached pattern
            Pattern compiled = getCompiledPattern(trimmed);
            if (compiled != null && compiled.matcher(text).find()) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesAnyKeyword(String text, List<String> keywords) {
        if (text == null || keywords == null || keywords.isEmpty()) {
            return false;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        for (String kw : keywords) {
            if (kw == null || kw.trim().isEmpty()) continue;
            if (lower.contains(kw.trim().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasWord(String text, String word) {
        if (text == null || word == null || text.isEmpty()) return false;
        // Simple word boundary check
        int idx = text.indexOf(word);
        while (idx >= 0) {
            boolean startOk = idx == 0 || !Character.isLetterOrDigit(text.charAt(idx - 1));
            int endIdx = idx + word.length();
            boolean endOk = endIdx == text.length() || !Character.isLetterOrDigit(text.charAt(endIdx));
            if (startOk && endOk) {
                return true;
            }
            idx = text.indexOf(word, idx + 1);
        }
        return false;
    }
}

