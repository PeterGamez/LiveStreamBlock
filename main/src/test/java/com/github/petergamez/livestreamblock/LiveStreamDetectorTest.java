package com.github.petergamez.livestreamblock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LiveStreamDetectorTest {

    private LiveStreamBlockConfig config;
    private LiveStreamDetector detector;

    @BeforeEach
    void setUp() {
        config = new LiveStreamBlockConfig();
        detector = new LiveStreamDetector(config);
    }

    @Test
    @DisplayName("Normal YouTube video should be allowed")
    void testNormalYouTubeVideo() {
        DetectionResult result = detector.check(
                "Never Gonna Give You Up",
                "Rick Astley",
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                "dQw4w9WgXcQ",
                "youtube",
                false,
                212000L
        );
        assertFalse(result.isBlocked());
        assertEquals(DetectionReason.ALLOWED, result.getReason());
    }

    @Test
    @DisplayName("YouTube live stream should be blocked when blockLiveStreams is enabled")
    void testYouTubeLiveStream() {
        DetectionResult result = detector.check(
                "lofi hip hop radio - beats to relax/study to",
                "Lofi Girl",
                "https://www.youtube.com/watch?v=jfKfPfyJRdk",
                "jfKfPfyJRdk",
                "youtube",
                true,
                Long.MAX_VALUE
        );
        assertTrue(result.isBlocked());
        assertEquals(DetectionReason.BLOCKED_LIVESTREAM, result.getReason());
    }

    @Test
    @DisplayName("Twitch live stream should be blocked")
    void testTwitchLiveStream() {
        DetectionResult result = detector.check(
                "Live Gaming Broadcast",
                "StreamerName",
                "https://www.twitch.tv/streamer",
                "streamer",
                "twitch",
                true,
                Long.MAX_VALUE
        );
        assertTrue(result.isBlocked());
        assertEquals(DetectionReason.BLOCKED_LIVESTREAM, result.getReason());
    }

    @Test
    @DisplayName("Icecast/Shoutcast radio stream should be blocked when blockRadio is enabled")
    void testIcecastRadio() {
        DetectionResult result = detector.check(
                "Cat Radio 94.5 FM",
                "Cat Radio",
                "http://stream.catradio.in.th:8000/catradio",
                "http://stream.catradio.in.th:8000/catradio",
                "http",
                true,
                Long.MAX_VALUE
        );
        assertTrue(result.isBlocked());
        assertEquals(DetectionReason.BLOCKED_RADIO, result.getReason());
    }

    @Test
    @DisplayName("M3U8 radio playlist stream should be blocked")
    void testM3u8Radio() {
        DetectionResult result = detector.check(
                "BBC World Service",
                "BBC",
                "http://stream.live.vc.bbcmedia.co.uk/bbc_world_service.m3u8",
                "http://stream.live.vc.bbcmedia.co.uk/bbc_world_service.m3u8",
                "http",
                true,
                Long.MAX_VALUE
        );
        assertTrue(result.isBlocked());
        assertEquals(DetectionReason.BLOCKED_RADIO, result.getReason());
    }

    @Test
    @DisplayName("Regular HTTP MP3 file with duration should be allowed")
    void testHttpDirectMp3File() {
        DetectionResult result = detector.check(
                "Podcast Episode 1",
                "Podcaster",
                "https://cdn.example.com/audio/episode1.mp3",
                "https://cdn.example.com/audio/episode1.mp3",
                "http",
                false,
                1800000L // 30 mins
        );
        assertFalse(result.isBlocked());
        assertEquals(DetectionReason.ALLOWED, result.getReason());
    }

    @Test
    @DisplayName("Master switch disabled should allow all streams")
    void testMasterSwitchDisabled() {
        config.setEnabled(false);

        DetectionResult ytResult = detector.check(
                "Live stream", "Author", "https://youtube.com/watch?v=live123",
                "live123", "youtube", true, Long.MAX_VALUE
        );
        assertFalse(ytResult.isBlocked());

        DetectionResult radioResult = detector.check(
                "Radio FM", "Station", "http://radio.example.com:8000/stream",
                "stream", "http", true, Long.MAX_VALUE
        );
        assertFalse(radioResult.isBlocked());
    }

    @Test
    @DisplayName("Toggle: blockLiveStreams=false should allow YouTube live but still block radio")
    void testAllowLiveStreamsBlockRadio() {
        config.setBlockLiveStreams(false);
        config.setBlockRadio(true);

        DetectionResult ytResult = detector.check(
                "Live stream", "Author", "https://youtube.com/watch?v=live123",
                "live123", "youtube", true, Long.MAX_VALUE
        );
        assertFalse(ytResult.isBlocked());

        DetectionResult radioResult = detector.check(
                "Radio FM", "Station", "http://radio.example.com:8000/stream",
                "stream", "http", true, Long.MAX_VALUE
        );
        assertTrue(radioResult.isBlocked());
        assertEquals(DetectionReason.BLOCKED_RADIO, radioResult.getReason());
    }

    @Test
    @DisplayName("Toggle: blockRadio=false should allow radio but still block YouTube live")
    void testAllowRadioBlockLiveStreams() {
        config.setBlockLiveStreams(true);
        config.setBlockRadio(false);

        DetectionResult ytResult = detector.check(
                "Live stream", "Author", "https://youtube.com/watch?v=live123",
                "live123", "youtube", true, Long.MAX_VALUE
        );
        assertTrue(ytResult.isBlocked());
        assertEquals(DetectionReason.BLOCKED_LIVESTREAM, ytResult.getReason());

        DetectionResult radioResult = detector.check(
                "Radio FM", "Station", "http://radio.example.com:8000/stream",
                "stream", "http", true, Long.MAX_VALUE
        );
        assertFalse(radioResult.isBlocked());
    }

    @Test
    @DisplayName("Whitelisted URL should be allowed even if it is a livestream")
    void testWhitelistedUrl() {
        config.setAllowedUrls(List.of("https://www.youtube.com/watch?v=whitelistedLive"));

        DetectionResult result = detector.check(
                "Special Event Live", "Host",
                "https://www.youtube.com/watch?v=whitelistedLive",
                "whitelistedLive", "youtube", true, Long.MAX_VALUE
        );
        assertFalse(result.isBlocked());
        assertEquals(DetectionReason.WHITELISTED, result.getReason());
    }

    @Test
    @DisplayName("Custom blocked URL should be blocked regardless of track type")
    void testCustomBlockedUrl() {
        config.setBlockedUrls(List.of(".*baddomain\\.com.*"));

        DetectionResult result = detector.check(
                "Song", "Artist",
                "https://baddomain.com/song.mp3",
                "song.mp3", "http", false, 120000L
        );
        assertTrue(result.isBlocked());
        assertEquals(DetectionReason.BLOCKED_CUSTOM_URL, result.getReason());
    }

    @Test
    @DisplayName("Custom blocked keyword should be blocked")
    void testCustomBlockedKeyword() {
        config.setBlockedKeywords(List.of("24/7 radio", "livestream"));

        DetectionResult result = detector.check(
                "Best Hits 24/7 Radio", "DJ",
                "https://example.com/track",
                "track", "http", false, 120000L
        );
        assertTrue(result.isBlocked());
        assertEquals(DetectionReason.BLOCKED_CUSTOM_KEYWORD, result.getReason());
    }
}

