package com.github.petergamez.livestreamblock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

/**
 * Service bootstrap for the LiveStreamBlock plugin.
 */
@Service
public class LiveStreamBlockPlugin {

    private static final Logger log = LoggerFactory.getLogger(LiveStreamBlockPlugin.class);

    private final LiveStreamBlockPluginConfig config;
    private final LiveStreamBlockStats stats;
    private final LiveStreamDetector detector;

    public LiveStreamBlockPlugin(LiveStreamBlockPluginConfig config) {
        this.config = config;
        this.stats = new LiveStreamBlockStats();
        this.detector = new LiveStreamDetector(config, stats);

        log.info("LiveStreamBlock Plugin loaded! (Enabled: {}, Block Live: {}, Block Radio: {}, Block Other: {}, Playback Enforce: {}, Action: {})",
                config.isEnabled(),
                config.isBlockLiveStreams(),
                config.isBlockRadio(),
                config.isBlockOtherStreams(),
                config.isBlockPlayback(),
                config.getAction());
    }

    @Bean
    public LiveStreamDetector liveStreamDetector() {
        return detector;
    }

    @Bean
    public LiveStreamBlockStats liveStreamBlockStats() {
        return stats;
    }

    public LiveStreamBlockPluginConfig getConfig() {
        return config;
    }

    public LiveStreamBlockStats getStats() {
        return stats;
    }

    public LiveStreamDetector getDetector() {
        return detector;
    }
}

