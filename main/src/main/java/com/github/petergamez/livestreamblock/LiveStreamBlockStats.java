package com.github.petergamez.livestreamblock;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe statistics tracking for the LiveStreamBlock plugin.
 */
public class LiveStreamBlockStats {

    private final AtomicLong tracksChecked = new AtomicLong();
    private final AtomicLong liveStreamsBlocked = new AtomicLong();
    private final AtomicLong radioBlocked = new AtomicLong();
    private final AtomicLong otherStreamsBlocked = new AtomicLong();
    private final AtomicLong customBlocked = new AtomicLong();
    private final AtomicLong playlistsFiltered = new AtomicLong();
    private final AtomicLong tracksFilteredFromPlaylists = new AtomicLong();
    private final AtomicLong searchResultsFiltered = new AtomicLong();
    private final AtomicLong tracksFilteredFromSearch = new AtomicLong();
    private final AtomicLong playbackStopped = new AtomicLong();

    public void incrementTracksChecked() {
        tracksChecked.incrementAndGet();
    }

    public void incrementBlocked(DetectionReason reason) {
        if (reason == null) return;
        switch (reason) {
            case BLOCKED_LIVESTREAM -> liveStreamsBlocked.incrementAndGet();
            case BLOCKED_RADIO -> radioBlocked.incrementAndGet();
            case BLOCKED_OTHER_STREAM -> otherStreamsBlocked.incrementAndGet();
            case BLOCKED_CUSTOM_URL, BLOCKED_CUSTOM_KEYWORD -> customBlocked.incrementAndGet();
            default -> {}
        }
    }

    public void recordPlaylistFiltered(long tracksRemoved) {
        playlistsFiltered.incrementAndGet();
        tracksFilteredFromPlaylists.addAndGet(tracksRemoved);
    }

    public void recordSearchFiltered(long tracksRemoved) {
        searchResultsFiltered.incrementAndGet();
        tracksFilteredFromSearch.addAndGet(tracksRemoved);
    }

    public void incrementPlaybackStopped() {
        playbackStopped.incrementAndGet();
    }

    public long getTotalBlocked() {
        return liveStreamsBlocked.get()
                + radioBlocked.get()
                + otherStreamsBlocked.get()
                + customBlocked.get();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("tracksChecked", tracksChecked.get());
        map.put("totalBlocked", getTotalBlocked());
        map.put("liveStreamsBlocked", liveStreamsBlocked.get());
        map.put("radioBlocked", radioBlocked.get());
        map.put("otherStreamsBlocked", otherStreamsBlocked.get());
        map.put("customBlocked", customBlocked.get());
        map.put("playlistsFiltered", playlistsFiltered.get());
        map.put("tracksFilteredFromPlaylists", tracksFilteredFromPlaylists.get());
        map.put("searchResultsFiltered", searchResultsFiltered.get());
        map.put("tracksFilteredFromSearch", tracksFilteredFromSearch.get());
        map.put("playbackStopped", playbackStopped.get());
        return map;
    }

    public void reset() {
        tracksChecked.set(0);
        liveStreamsBlocked.set(0);
        radioBlocked.set(0);
        otherStreamsBlocked.set(0);
        customBlocked.set(0);
        playlistsFiltered.set(0);
        tracksFilteredFromPlaylists.set(0);
        searchResultsFiltered.set(0);
        tracksFilteredFromSearch.set(0);
        playbackStopped.set(0);
    }
}

