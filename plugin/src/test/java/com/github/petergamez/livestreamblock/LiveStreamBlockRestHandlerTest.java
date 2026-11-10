package com.github.petergamez.livestreamblock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class LiveStreamBlockRestHandlerTest {

    private LiveStreamBlockPluginConfig config;
    private LiveStreamDetector detector;
    private LiveStreamBlockRestHandler handler;

    @BeforeEach
    void setUp() {
        config = new LiveStreamBlockPluginConfig();
        detector = new LiveStreamDetector(config);
        handler = new LiveStreamBlockRestHandler(config, detector, "test-password");
    }

    @Test
    @DisplayName("Unauthorized requests should return 401")
    void testUnauthorized() {
        ResponseEntity<?> response = handler.getConfig("wrong-password", null);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @DisplayName("Authorized requests should return 200 with config")
    void testAuthorizedGetConfig() {
        ResponseEntity<?> response = handler.getConfig("test-password", null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertInstanceOf(Map.class, response.getBody());
        Map<?, ?> map = (Map<?, ?>) response.getBody();
        assertEquals(true, map.get("enabled"));
        assertEquals(true, map.get("blockLiveStreams"));
        assertEquals(true, map.get("blockRadio"));
    }

    @Test
    @DisplayName("Updating config dynamically should change config values")
    void testUpdateConfig() {
        Map<String, Object> updates = Map.of(
                "blockLiveStreams", false,
                "blockRadio", true,
                "action", "EMPTY"
        );
        ResponseEntity<?> response = handler.updateConfig("test-password", null, updates);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertFalse(config.isBlockLiveStreams());
        assertTrue(config.isBlockRadio());
        assertEquals("EMPTY", config.getAction());
    }

    @Test
    @DisplayName("Stats endpoint should return valid stats map")
    void testGetStats() {
        ResponseEntity<?> response = handler.getStats("test-password", null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertInstanceOf(Map.class, response.getBody());
        Map<?, ?> map = (Map<?, ?>) response.getBody();
        assertTrue(map.containsKey("tracksChecked"));
        assertTrue(map.containsKey("totalBlocked"));
    }

    @Test
    @DisplayName("Test endpoint should report whether a track is blocked")
    void testTrackEndpoint() {
        ResponseEntity<?> response = handler.testTrack(
                "test-password", null,
                "https://www.youtube.com/watch?v=live123",
                "youtube",
                true,
                -1
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertInstanceOf(Map.class, response.getBody());
        Map<?, ?> map = (Map<?, ?>) response.getBody();
        assertEquals(true, map.get("blocked"));
        assertEquals(DetectionReason.BLOCKED_LIVESTREAM.name(), map.get("reason"));
    }
}

