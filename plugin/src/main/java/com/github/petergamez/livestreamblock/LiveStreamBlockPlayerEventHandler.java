package com.github.petergamez.livestreamblock;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.arbjerg.lavalink.api.IPlayer;
import dev.arbjerg.lavalink.api.ISocketContext;
import dev.arbjerg.lavalink.api.PluginEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Event handler registering an AudioEventListener on every new player instance
 * to prevent playback of live streams and radio streams.
 */
@Component
public class LiveStreamBlockPlayerEventHandler extends PluginEventHandler {

    private static final Logger log = LoggerFactory.getLogger(LiveStreamBlockPlayerEventHandler.class);

    private final LiveStreamBlockPluginConfig config;
    private final LiveStreamDetector detector;

    public LiveStreamBlockPlayerEventHandler(LiveStreamBlockPluginConfig config, LiveStreamDetector detector) {
        this.config = config;
        this.detector = detector;
    }

    @Override
    public void onNewPlayer(ISocketContext context, IPlayer player) {
        if (player == null || player.getAudioPlayer() == null) {
            return;
        }

        player.getAudioPlayer().addListener(new AudioEventAdapter() {
            @Override
            public void onTrackStart(AudioPlayer audioPlayer, AudioTrack track) {
                if (!config.isEnabled() || !config.isBlockPlayback() || track == null) {
                    return;
                }

                DetectionResult result = detector.check(track);
                if (result.isBlocked()) {
                    log.warn("Intercepted and stopped live stream/radio track playback: '{}' ({}) on guild {}",
                            track.getInfo().title, track.getInfo().uri, player.getGuildId());
                    detector.getStats().incrementPlaybackStopped();
                    audioPlayer.stopTrack();
                }
            }
        });
    }
}

