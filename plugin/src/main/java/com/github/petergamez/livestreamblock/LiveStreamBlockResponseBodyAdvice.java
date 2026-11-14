package com.github.petergamez.livestreamblock;

import dev.arbjerg.lavalink.protocol.v4.Exception;
import dev.arbjerg.lavalink.protocol.v4.LoadResult;
import dev.arbjerg.lavalink.protocol.v4.Playlist;
import dev.arbjerg.lavalink.protocol.v4.Track;
import dev.arbjerg.lavalink.protocol.v4.TrackInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.ArrayList;
import java.util.List;

/**
 * Intercepts /v4/loadtracks responses and filters or blocks live streams and radio stations.
 */
@ControllerAdvice
public class LiveStreamBlockResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    private static final Logger log = LoggerFactory.getLogger(LiveStreamBlockResponseBodyAdvice.class);

    private final LiveStreamBlockPluginConfig config;
    private final LiveStreamDetector detector;

    public LiveStreamBlockResponseBodyAdvice(LiveStreamBlockPluginConfig config, LiveStreamDetector detector) {
        this.config = config;
        this.detector = detector;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return config.isEnabled();
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        if (body == null || !config.isEnabled()) {
            return body;
        }

        try {
            if (body instanceof LoadResult loadResult) {
                return handleLoadResult(loadResult);
            }
        } catch (java.lang.Exception e) {
            log.error("Error in LiveStreamBlockResponseBodyAdvice while inspecting response: {}", e.getMessage(), e);
        }

        return body;
    }

    private LoadResult handleLoadResult(LoadResult loadResult) {
        // 1. Single Track Loaded
        if (loadResult instanceof LoadResult.TrackLoaded trackLoaded) {
            Track track = trackLoaded.getData();
            TrackInfo info = track.getInfo();
            DetectionResult result = detector.check(
                    info.getTitle(),
                    info.getAuthor(),
                    info.getUri(),
                    info.getIdentifier(),
                    info.getSourceName(),
                    info.isStream(),
                    info.getLength()
            );

            if (result.isBlocked()) {
                log.info("Blocked track '{}' by '{}' ({}) - Reason: {} [{}]",
                        info.getTitle(), info.getAuthor(), info.getUri(),
                        result.getReason(), result.getDetails());

                if (config.isActionEmpty()) {
                    return LoadResult.NoMatches.Companion.invoke();
                } else {
                    return createLoadFailed(
                            result.getMessage(),
                            Exception.Severity.COMMON,
                            result.getReason().name() + ": " + result.getDetails()
                    );
                }
            }
            return loadResult;
        }

        // 2. Playlist Loaded
        if (loadResult instanceof LoadResult.PlaylistLoaded playlistLoaded) {
            Playlist playlist = playlistLoaded.getData();
            List<Track> originalTracks = playlist.getTracks();

            if (!config.isFilterPlaylists() || originalTracks == null || originalTracks.isEmpty()) {
                return loadResult;
            }

            List<Track> allowedTracks = new ArrayList<>();
            int blockedCount = 0;

            for (Track track : originalTracks) {
                TrackInfo info = track.getInfo();
                DetectionResult result = detector.check(
                        info.getTitle(),
                        info.getAuthor(),
                        info.getUri(),
                        info.getIdentifier(),
                        info.getSourceName(),
                        info.isStream(),
                        info.getLength()
                );

                if (result.isBlocked()) {
                    blockedCount++;
                } else {
                    allowedTracks.add(track);
                }
            }

            if (blockedCount > 0) {
                detector.getStats().recordPlaylistFiltered(blockedCount);
                log.info("Filtered {} blocked live stream/radio track(s) from playlist '{}' ({} remaining)",
                        blockedCount, playlist.getInfo().getName(), allowedTracks.size());

                if (allowedTracks.isEmpty()) {
                    if (config.isActionEmpty()) {
                        return LoadResult.NoMatches.Companion.invoke();
                    } else {
                        return createLoadFailed(
                                config.getBlockMessage(),
                                Exception.Severity.COMMON,
                                "All tracks in playlist were blocked live streams or radio stations"
                        );
                    }
                }

                return new LoadResult.PlaylistLoaded(new Playlist(
                        playlist.getInfo(),
                        playlist.getPluginInfo(),
                        allowedTracks
                ));
            }

            return loadResult;
        }

        // 3. Search Result
        if (loadResult instanceof LoadResult.SearchResult searchResult) {
            List<Track> originalTracks = searchResult.getData().getTracks();

            if (!config.isFilterSearch() || originalTracks == null || originalTracks.isEmpty()) {
                return loadResult;
            }

            List<Track> allowedTracks = new ArrayList<>();
            int blockedCount = 0;

            for (Track track : originalTracks) {
                TrackInfo info = track.getInfo();
                DetectionResult result = detector.check(
                        info.getTitle(),
                        info.getAuthor(),
                        info.getUri(),
                        info.getIdentifier(),
                        info.getSourceName(),
                        info.isStream(),
                        info.getLength()
                );

                if (result.isBlocked()) {
                    blockedCount++;
                } else {
                    allowedTracks.add(track);
                }
            }

            if (blockedCount > 0) {
                detector.getStats().recordSearchFiltered(blockedCount);
                log.debug("Filtered {} blocked live stream/radio track(s) from search results ({} remaining)",
                        blockedCount, allowedTracks.size());

                if (allowedTracks.isEmpty()) {
                    return LoadResult.NoMatches.Companion.invoke();
                }

                return new LoadResult.SearchResult(new LoadResult.SearchResult.Data(allowedTracks));
            }

            return loadResult;
        }

        return loadResult;
    }

    /**
     * Creates a {@link LoadResult.LoadFailed} instance safely across different Lavalink v4 versions.
     * Lavalink 4.0.0 - 4.1.x uses Exception(String, Severity, String) [3 args]
     * Lavalink 4.2.0+ uses Exception(String, Severity, String, String) [4 args with causeStackTrace]
     */
    LoadResult createLoadFailed(String message, Exception.Severity severity, String cause) {
        try {
            Exception ex = createException(message, severity, cause);
            return new LoadResult.LoadFailed(ex);
        } catch (java.lang.Exception e) {
            log.error("Failed to construct LoadResult.LoadFailed: {}", e.getMessage(), e);
            return LoadResult.NoMatches.Companion.invoke();
        }
    }

    /**
     * Instantiates {@link Exception} using reflection to maintain cross-version compatibility.
     */
    Exception createException(String message, Exception.Severity severity, String cause) {
        String safeCause = cause != null ? cause : "";

        // 1. Try 4-parameter constructor (Lavalink 4.2.0+)
        // Exception(String message, Severity severity, String cause, String causeStackTrace)
        try {
            var constructor4 = Exception.class.getConstructor(
                    String.class, Exception.Severity.class, String.class, String.class
            );
            return constructor4.newInstance(message, severity, safeCause, safeCause);
        } catch (NoSuchMethodException ignored) {
            // Not running on Lavalink 4.2+, fallback to 3-parameter constructor
        } catch (java.lang.Exception e) {
            log.warn("Failed to invoke 4-parameter Exception constructor: {}", e.getMessage());
        }

        // 2. Try 3-parameter constructor (Lavalink 4.0.0 - 4.1.x)
        // Exception(String message, Severity severity, String cause)
        try {
            var constructor3 = Exception.class.getConstructor(
                    String.class, Exception.Severity.class, String.class
            );
            return constructor3.newInstance(message, severity, safeCause);
        } catch (java.lang.Exception e) {
            throw new RuntimeException("Could not create dev.arbjerg.lavalink.protocol.v4.Exception instance", e);
        }
    }
}

