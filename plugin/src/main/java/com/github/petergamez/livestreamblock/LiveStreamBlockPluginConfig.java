package com.github.petergamez.livestreamblock;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Lavalink v4 plugin configuration binding for LiveStreamBlock.
 * Binds properties from application.yml under 'plugins.livestreamblock'.
 */
@Component
@ConfigurationProperties(prefix = "plugins.livestreamblock")
public class LiveStreamBlockPluginConfig extends LiveStreamBlockConfig {
}

