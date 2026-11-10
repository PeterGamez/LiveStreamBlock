package com.github.petergamez.livestreamblock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST management endpoints for the LiveStreamBlock plugin.
 * Base path: /v4/livestreamblock
 */
@RestController
@RequestMapping("/v4/livestreamblock")
@ConditionalOnProperty(prefix = "plugins.livestreamblock.rest-api", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LiveStreamBlockRestHandler {

    private static final Logger log = LoggerFactory.getLogger(LiveStreamBlockRestHandler.class);

    private final LiveStreamBlockPluginConfig config;
    private final LiveStreamDetector detector;
    private final String serverPassword;

    @Autowired
    public LiveStreamBlockRestHandler(LiveStreamBlockPluginConfig config,
                                     LiveStreamDetector detector,
                                     @Value("${lavalink.server.password:${server.password:}}") String serverPassword) {
        this.config = config;
        this.detector = detector;
        this.serverPassword = serverPassword != null ? serverPassword.trim() : "";
    }

    public LiveStreamBlockRestHandler(LiveStreamBlockPluginConfig config, LiveStreamDetector detector) {
        this(config, detector, "");
    }

    @GetMapping("/config")
    public ResponseEntity<?> getConfig(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-LiveStreamBlock-Password", required = false) String pluginPassword) {
        if (!isAuthorized(authHeader, pluginPassword)) {
            return unauthorizedResponse();
        }
        return ResponseEntity.ok(config.toMap());
    }

    @RequestMapping(value = "/config", method = {RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<?> updateConfig(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-LiveStreamBlock-Password", required = false) String pluginPassword,
            @RequestBody Map<String, Object> updates) {
        if (!isAuthorized(authHeader, pluginPassword)) {
            return unauthorizedResponse();
        }
        if (updates == null || updates.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Request body cannot be empty"));
        }

        config.updateFromMap(updates);
        log.info("LiveStreamBlock configuration updated via REST API: {}", updates);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", true);
        resp.put("message", "Configuration updated successfully");
        resp.put("config", config.toMap());
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-LiveStreamBlock-Password", required = false) String pluginPassword) {
        if (!isAuthorized(authHeader, pluginPassword)) {
            return unauthorizedResponse();
        }
        return ResponseEntity.ok(detector.getStats().toMap());
    }

    @PostMapping("/stats/reset")
    public ResponseEntity<?> resetStats(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-LiveStreamBlock-Password", required = false) String pluginPassword) {
        if (!isAuthorized(authHeader, pluginPassword)) {
            return unauthorizedResponse();
        }
        detector.getStats().reset();
        log.info("LiveStreamBlock statistics reset via REST API");
        return ResponseEntity.ok(Map.of("success", true, "message", "Statistics reset successfully"));
    }

    @GetMapping("/test")
    public ResponseEntity<?> testTrack(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-LiveStreamBlock-Password", required = false) String pluginPassword,
            @RequestParam("identifier") String identifier,
            @RequestParam(value = "source", required = false) String source,
            @RequestParam(value = "isStream", required = false, defaultValue = "false") boolean isStream,
            @RequestParam(value = "length", required = false, defaultValue = "-1") long length) {
        if (!isAuthorized(authHeader, pluginPassword)) {
            return unauthorizedResponse();
        }

        DetectionResult result = detector.check(
                identifier,
                "",
                identifier,
                identifier,
                source,
                isStream,
                length > 0 ? length : (isStream ? Long.MAX_VALUE : 180000L)
        );

        Map<String, Object> resp = new LinkedHashMap<>(result.toMap());
        resp.put("identifier", identifier);
        resp.put("source", source);
        resp.put("isStream", isStream);
        return ResponseEntity.ok(resp);
    }

    private boolean isAuthorized(String authHeader, String pluginPassword) {
        String configuredPluginPassword = config.getRestApi().getPassword();
        if (configuredPluginPassword != null && !configuredPluginPassword.isEmpty()) {
            if (configuredPluginPassword.equals(pluginPassword) || configuredPluginPassword.equals(authHeader)) {
                return true;
            }
        }

        if (serverPassword != null && !serverPassword.isEmpty()) {
            return serverPassword.equals(authHeader);
        }

        return true;
    }

    private ResponseEntity<Map<String, Object>> unauthorizedResponse() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Collections.singletonMap("error", "Unauthorized: Invalid or missing authorization credentials"));
    }
}

