package com.github.petergamez.livestreamblock;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Configuration POJO for LiveStreamBlock.
 * Supports granular toggles for live streams, radio stations, sources, and filters.
 */
public class LiveStreamBlockConfig {

    private volatile boolean enabled = true;
    private volatile boolean blockLiveStreams = true;
    private volatile boolean blockRadio = true;
    private volatile boolean blockOtherStreams = true;
    private volatile boolean blockPlayback = true;
    private volatile boolean filterPlaylists = true;
    private volatile boolean filterSearch = true;
    private volatile String action = "EXCEPTION"; // "EXCEPTION" or "EMPTY"
    private volatile String blockMessage = "Live streams and radio stations are blocked on this server.";

    private final List<String> allowedUrls = new CopyOnWriteArrayList<>();
    private final List<String> blockedUrls = new CopyOnWriteArrayList<>();
    private final List<String> blockedKeywords = new CopyOnWriteArrayList<>();

    private SourcesConfig sources = new SourcesConfig();
    private RestApiConfig restApi = new RestApiConfig();

    public static class SourcesConfig {
        private volatile boolean youtube = true;
        private volatile boolean twitch = true;
        private volatile boolean http = true;
        private volatile boolean soundcloud = true;

        public boolean isYoutube() {
            return youtube;
        }

        public void setYoutube(boolean youtube) {
            this.youtube = youtube;
        }

        public boolean isTwitch() {
            return twitch;
        }

        public void setTwitch(boolean twitch) {
            this.twitch = twitch;
        }

        public boolean isHttp() {
            return http;
        }

        public void setHttp(boolean http) {
            this.http = http;
        }

        public boolean isSoundcloud() {
            return soundcloud;
        }

        public void setSoundcloud(boolean soundcloud) {
            this.soundcloud = soundcloud;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("youtube", youtube);
            map.put("twitch", twitch);
            map.put("http", http);
            map.put("soundcloud", soundcloud);
            return map;
        }

        @SuppressWarnings("unchecked")
        public void updateFromMap(Map<String, Object> map) {
            if (map == null) return;
            if (map.containsKey("youtube")) this.youtube = Boolean.parseBoolean(String.valueOf(map.get("youtube")));
            if (map.containsKey("twitch")) this.twitch = Boolean.parseBoolean(String.valueOf(map.get("twitch")));
            if (map.containsKey("http")) this.http = Boolean.parseBoolean(String.valueOf(map.get("http")));
            if (map.containsKey("soundcloud")) this.soundcloud = Boolean.parseBoolean(String.valueOf(map.get("soundcloud")));
        }
    }

    public static class RestApiConfig {
        private volatile boolean enabled = true;
        private volatile String password = "";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password != null ? password : "";
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("enabled", enabled);
            map.put("hasPassword", password != null && !password.isEmpty());
            return map;
        }

        public void updateFromMap(Map<String, Object> map) {
            if (map == null) return;
            if (map.containsKey("enabled")) this.enabled = Boolean.parseBoolean(String.valueOf(map.get("enabled")));
            if (map.containsKey("password")) setPassword(String.valueOf(map.get("password")));
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isBlockLiveStreams() {
        return blockLiveStreams;
    }

    public void setBlockLiveStreams(boolean blockLiveStreams) {
        this.blockLiveStreams = blockLiveStreams;
    }

    public boolean isBlockRadio() {
        return blockRadio;
    }

    public void setBlockRadio(boolean blockRadio) {
        this.blockRadio = blockRadio;
    }

    public boolean isBlockOtherStreams() {
        return blockOtherStreams;
    }

    public void setBlockOtherStreams(boolean blockOtherStreams) {
        this.blockOtherStreams = blockOtherStreams;
    }

    public boolean isBlockPlayback() {
        return blockPlayback;
    }

    public void setBlockPlayback(boolean blockPlayback) {
        this.blockPlayback = blockPlayback;
    }

    public boolean isFilterPlaylists() {
        return filterPlaylists;
    }

    public void setFilterPlaylists(boolean filterPlaylists) {
        this.filterPlaylists = filterPlaylists;
    }

    public boolean isFilterSearch() {
        return filterSearch;
    }

    public void setFilterSearch(boolean filterSearch) {
        this.filterSearch = filterSearch;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        if (action != null) {
            String upper = action.trim().toUpperCase(Locale.ROOT);
            if ("EMPTY".equals(upper) || "NO_MATCHES".equals(upper) || "NONE".equals(upper)) {
                this.action = "EMPTY";
            } else {
                this.action = "EXCEPTION";
            }
        }
    }

    public boolean isActionEmpty() {
        return "EMPTY".equalsIgnoreCase(action);
    }

    public String getBlockMessage() {
        return blockMessage;
    }

    public void setBlockMessage(String blockMessage) {
        this.blockMessage = blockMessage != null ? blockMessage : "Live streams and radio stations are blocked on this server.";
    }

    public List<String> getAllowedUrls() {
        return allowedUrls;
    }

    public void setAllowedUrls(List<String> list) {
        this.allowedUrls.clear();
        if (list != null) {
            this.allowedUrls.addAll(list);
        }
    }

    public List<String> getBlockedUrls() {
        return blockedUrls;
    }

    public void setBlockedUrls(List<String> list) {
        this.blockedUrls.clear();
        if (list != null) {
            this.blockedUrls.addAll(list);
        }
    }

    public List<String> getBlockedKeywords() {
        return blockedKeywords;
    }

    public void setBlockedKeywords(List<String> list) {
        this.blockedKeywords.clear();
        if (list != null) {
            this.blockedKeywords.addAll(list);
        }
    }

    public SourcesConfig getSources() {
        return sources;
    }

    public void setSources(SourcesConfig sources) {
        this.sources = sources != null ? sources : new SourcesConfig();
    }

    public RestApiConfig getRestApi() {
        return restApi;
    }

    public void setRestApi(RestApiConfig restApi) {
        this.restApi = restApi != null ? restApi : new RestApiConfig();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("enabled", enabled);
        map.put("blockLiveStreams", blockLiveStreams);
        map.put("blockRadio", blockRadio);
        map.put("blockOtherStreams", blockOtherStreams);
        map.put("blockPlayback", blockPlayback);
        map.put("filterPlaylists", filterPlaylists);
        map.put("filterSearch", filterSearch);
        map.put("action", action);
        map.put("blockMessage", blockMessage);
        map.put("allowedUrls", new ArrayList<>(allowedUrls));
        map.put("blockedUrls", new ArrayList<>(blockedUrls));
        map.put("blockedKeywords", new ArrayList<>(blockedKeywords));
        map.put("sources", sources.toMap());
        map.put("restApi", restApi.toMap());
        return map;
    }

    @SuppressWarnings("unchecked")
    public void updateFromMap(Map<String, Object> map) {
        if (map == null) return;
        if (map.containsKey("enabled")) this.enabled = Boolean.parseBoolean(String.valueOf(map.get("enabled")));
        if (map.containsKey("blockLiveStreams")) this.blockLiveStreams = Boolean.parseBoolean(String.valueOf(map.get("blockLiveStreams")));
        if (map.containsKey("blockRadio")) this.blockRadio = Boolean.parseBoolean(String.valueOf(map.get("blockRadio")));
        if (map.containsKey("blockOtherStreams")) this.blockOtherStreams = Boolean.parseBoolean(String.valueOf(map.get("blockOtherStreams")));
        if (map.containsKey("blockPlayback")) this.blockPlayback = Boolean.parseBoolean(String.valueOf(map.get("blockPlayback")));
        if (map.containsKey("filterPlaylists")) this.filterPlaylists = Boolean.parseBoolean(String.valueOf(map.get("filterPlaylists")));
        if (map.containsKey("filterSearch")) this.filterSearch = Boolean.parseBoolean(String.valueOf(map.get("filterSearch")));
        if (map.containsKey("action")) setAction(String.valueOf(map.get("action")));
        if (map.containsKey("blockMessage")) setBlockMessage(String.valueOf(map.get("blockMessage")));

        if (map.get("allowedUrls") instanceof List<?> list) {
            List<String> strList = new ArrayList<>();
            for (Object item : list) {
                if (item != null) strList.add(String.valueOf(item));
            }
            setAllowedUrls(strList);
        }
        if (map.get("blockedUrls") instanceof List<?> list) {
            List<String> strList = new ArrayList<>();
            for (Object item : list) {
                if (item != null) strList.add(String.valueOf(item));
            }
            setBlockedUrls(strList);
        }
        if (map.get("blockedKeywords") instanceof List<?> list) {
            List<String> strList = new ArrayList<>();
            for (Object item : list) {
                if (item != null) strList.add(String.valueOf(item));
            }
            setBlockedKeywords(strList);
        }
        if (map.get("sources") instanceof Map<?, ?> sourcesMap) {
            sources.updateFromMap((Map<String, Object>) sourcesMap);
        }
        if (map.get("restApi") instanceof Map<?, ?> restMap) {
            restApi.updateFromMap((Map<String, Object>) restMap);
        }
    }
}

