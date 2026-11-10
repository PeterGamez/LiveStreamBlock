package com.github.petergamez.livestreamblock;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Result of evaluating whether a track should be blocked.
 */
public class DetectionResult {

    private static final DetectionResult ALLOWED_INSTANCE = new DetectionResult(false, DetectionReason.ALLOWED, null, null);
    private static final DetectionResult WHITELISTED_INSTANCE = new DetectionResult(false, DetectionReason.WHITELISTED, null, null);

    private final boolean blocked;
    private final DetectionReason reason;
    private final String message;
    private final String details;

    public DetectionResult(boolean blocked, DetectionReason reason, String message, String details) {
        this.blocked = blocked;
        this.reason = reason;
        this.message = message;
        this.details = details;
    }

    public static DetectionResult allowed() {
        return ALLOWED_INSTANCE;
    }

    public static DetectionResult whitelisted(String details) {
        return new DetectionResult(false, DetectionReason.WHITELISTED, null, details);
    }

    public static DetectionResult blocked(DetectionReason reason, String message, String details) {
        return new DetectionResult(true, reason, message, details);
    }

    public boolean isBlocked() {
        return blocked;
    }

    public DetectionReason getReason() {
        return reason;
    }

    public String getMessage() {
        return message;
    }

    public String getDetails() {
        return details;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("blocked", blocked);
        map.put("reason", reason != null ? reason.name() : null);
        map.put("reasonDescription", reason != null ? reason.getDescription() : null);
        map.put("message", message);
        map.put("details", details);
        return map;
    }

    @Override
    public String toString() {
        return "DetectionResult{" +
                "blocked=" + blocked +
                ", reason=" + reason +
                ", message='" + message + '\'' +
                ", details='" + details + '\'' +
                '}';
    }
}

