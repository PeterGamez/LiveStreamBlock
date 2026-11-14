package com.github.petergamez.livestreamblock;

import dev.arbjerg.lavalink.protocol.v4.LoadResult;
import dev.arbjerg.lavalink.protocol.v4.Playlist;
import dev.arbjerg.lavalink.protocol.v4.PlaylistInfo;
import dev.arbjerg.lavalink.protocol.v4.Track;
import dev.arbjerg.lavalink.protocol.v4.TrackInfo;
import kotlinx.serialization.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LiveStreamBlockResponseBodyAdviceTest {

    private LiveStreamBlockPluginConfig config;
    private LiveStreamDetector detector;
    private LiveStreamBlockResponseBodyAdvice advice;

    private static final JsonObject EMPTY_JSON = new JsonObject(Collections.emptyMap());

    @BeforeEach
    void setUp() {
        config = new LiveStreamBlockPluginConfig();
        detector = new LiveStreamDetector(config);
        advice = new LiveStreamBlockResponseBodyAdvice(config, detector);
    }

    private Track createTrack(String title, String author, String uri, String sourceName, boolean isStream, long length) {
        TrackInfo info = new TrackInfo(
                "id123",
                !isStream,
                author,
                length,
                isStream,
                0L,
                title,
                uri,
                sourceName,
                null,
                null
        );
        return new Track("encodedTrackString", info, EMPTY_JSON, EMPTY_JSON);
    }

    @Test
    @DisplayName("Normal YouTube video should pass through unmodified")
    void testNormalTrackPassed() {
        Track normalTrack = createTrack("Shape of You", "Ed Sheeran", "https://youtube.com/watch?v=123", "youtube", false, 240000L);
        LoadResult.TrackLoaded input = new LoadResult.TrackLoaded(normalTrack);

        Object result = advice.beforeBodyWrite(input, null, null, null, null, null);
        assertSame(input, result);
    }

    @Test
    @DisplayName("YouTube livestream should be replaced with LoadFailed when action=EXCEPTION")
    void testLiveStreamBlockedException() {
        config.setAction("EXCEPTION");
        Track liveTrack = createTrack("Live Concert", "Band", "https://youtube.com/watch?v=live1", "youtube", true, Long.MAX_VALUE);
        LoadResult.TrackLoaded input = new LoadResult.TrackLoaded(liveTrack);

        Object result = advice.beforeBodyWrite(input, null, null, null, null, null);
        assertInstanceOf(LoadResult.LoadFailed.class, result);
        LoadResult.LoadFailed failed = (LoadResult.LoadFailed) result;
        assertEquals(config.getBlockMessage(), failed.getData().getMessage());
    }

    @Test
    @DisplayName("YouTube livestream should be replaced with NoMatches when action=EMPTY")
    void testLiveStreamBlockedEmpty() {
        config.setAction("EMPTY");
        Track liveTrack = createTrack("Live Concert", "Band", "https://youtube.com/watch?v=live1", "youtube", true, Long.MAX_VALUE);
        LoadResult.TrackLoaded input = new LoadResult.TrackLoaded(liveTrack);

        Object result = advice.beforeBodyWrite(input, null, null, null, null, null);
        assertInstanceOf(LoadResult.NoMatches.class, result);
    }

    @Test
    @DisplayName("Radio stream should be blocked when blockRadio is enabled")
    void testRadioStreamBlocked() {
        config.setAction("EXCEPTION");
        Track radioTrack = createTrack("Radio One", "BBC", "http://stream.example.com:8000/live", "http", true, Long.MAX_VALUE);
        LoadResult.TrackLoaded input = new LoadResult.TrackLoaded(radioTrack);

        Object result = advice.beforeBodyWrite(input, null, null, null, null, null);
        assertInstanceOf(LoadResult.LoadFailed.class, result);
    }

    @Test
    @DisplayName("Playlists should filter out livestream tracks while keeping normal tracks")
    void testPlaylistFiltering() {
        Track normalTrack = createTrack("Song 1", "Artist 1", "https://youtube.com/watch?v=song1", "youtube", false, 200000L);
        Track liveTrack = createTrack("Live Stream", "Streamer", "https://youtube.com/watch?v=live", "youtube", true, Long.MAX_VALUE);

        Playlist playlist = new Playlist(
                new PlaylistInfo("My Playlist", 0),
                EMPTY_JSON,
                List.of(normalTrack, liveTrack)
        );
        LoadResult.PlaylistLoaded input = new LoadResult.PlaylistLoaded(playlist);

        Object result = advice.beforeBodyWrite(input, null, null, null, null, null);
        assertInstanceOf(LoadResult.PlaylistLoaded.class, result);

        LoadResult.PlaylistLoaded output = (LoadResult.PlaylistLoaded) result;
        assertEquals(1, output.getData().getTracks().size());
        assertEquals("Song 1", output.getData().getTracks().get(0).getInfo().getTitle());
    }

    @Test
    @DisplayName("Search results should filter out livestream tracks")
    void testSearchResultFiltering() {
        Track normalTrack = createTrack("Song A", "Artist A", "https://youtube.com/watch?v=songA", "youtube", false, 180000L);
        Track liveTrack = createTrack("Live Stream B", "Streamer B", "https://youtube.com/watch?v=liveB", "youtube", true, Long.MAX_VALUE);

        LoadResult.SearchResult.Data data = new LoadResult.SearchResult.Data(List.of(normalTrack, liveTrack));
        LoadResult.SearchResult input = new LoadResult.SearchResult(data);

        Object result = advice.beforeBodyWrite(input, null, null, null, null, null);
        assertInstanceOf(LoadResult.SearchResult.class, result);

        LoadResult.SearchResult output = (LoadResult.SearchResult) result;
        assertEquals(1, output.getData().getTracks().size());
        assertEquals("Song A", output.getData().getTracks().get(0).getInfo().getTitle());
    }

    @Test
    @DisplayName("createLoadFailed should safely return LoadResult.LoadFailed across versions")
    void testCreateLoadFailedCompatibility() {
        LoadResult result = advice.createLoadFailed("Blocked message", dev.arbjerg.lavalink.protocol.v4.Exception.Severity.COMMON, "Cause info");
        assertInstanceOf(LoadResult.LoadFailed.class, result);
        LoadResult.LoadFailed failed = (LoadResult.LoadFailed) result;
        assertEquals("Blocked message", failed.getData().getMessage());
        assertEquals(dev.arbjerg.lavalink.protocol.v4.Exception.Severity.COMMON, failed.getData().getSeverity());
    }
}

