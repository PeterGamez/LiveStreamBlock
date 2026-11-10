package com.github.petergamez.livestreamblock;

/**
 * Reason why a track was allowed or blocked.
 */
public enum DetectionReason {
    ALLOWED("Track is allowed"),
    WHITELISTED("Track URL is whitelisted"),
    BLOCKED_LIVESTREAM("Live stream blocked"),
    BLOCKED_RADIO("Radio / audio broadcast blocked"),
    BLOCKED_OTHER_STREAM("General live stream blocked"),
    BLOCKED_CUSTOM_URL("Blocked by custom URL rule"),
    BLOCKED_CUSTOM_KEYWORD("Blocked by custom keyword rule");

    private final String description;

    DetectionReason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

