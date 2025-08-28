package muvi.anime.hub.player.tv;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VideoTvPlaybackManager {
    private static final String PREFS_NAME = "VideoPlaybackTvPrefs";
    private static final String KEY_VIDEOS = "saved_tv_videos";
    private static final int MAX_VIDEOS = 50; // Maximum number of videos to store
    private static final int PRUNE_COUNT = 10; // Number of videos to remove when exceeding limit

    private final SharedPreferences prefs;
    private final Gson gson;

    public VideoTvPlaybackManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    public void saveVideoProgress(VideoMetaData videoMetaData) {
        Map<String, VideoMetaData> savedVideos = getAllVideosMap();

        // Prune if we've reached the size limit
        if (savedVideos.size() >= MAX_VIDEOS) {
            pruneOldestEntries(savedVideos, PRUNE_COUNT);
        }

        videoMetaData.timestamp = System.currentTimeMillis();
        savedVideos.put(videoMetaData.episode.getCurrentStreamUrl(), videoMetaData);
        saveVideosMap(savedVideos);
    }

    /**
     * Removes the oldest entries from the videos map based on timestamp
     * @param videos The map of videos to prune
     * @param count Number of entries to remove
     */
    private void pruneOldestEntries(Map<String, VideoMetaData> videos, int count) {
        // If map is already small enough, don't prune
        if (videos.size() <= count) {
            return;
        }

        // Find the oldest entries
        List<Map.Entry<String, VideoMetaData>> entries = new ArrayList<>(videos.entrySet());
        entries.sort(Comparator.comparingLong(entry -> entry.getValue().timestamp));

        // Remove the oldest entries
        int pruneCount = Math.min(count, entries.size());
        for (int i = 0; i < pruneCount; i++) {
            videos.remove(entries.get(i).getKey());
        }

        // Log the pruning for debugging
        Log.d("VideoTvPlaybackManager", "Pruned " + pruneCount + " oldest video entries");
    }

    public VideoMetaData getVideoProgress(String videoId) {
        Map<String, VideoMetaData> savedVideos = getAllVideosMap();
        return savedVideos.get(videoId);
    }

    public void clearVideoProgress(String videoId) {
        Map<String, VideoMetaData> savedVideos = getAllVideosMap();
        savedVideos.remove(videoId);
        saveVideosMap(savedVideos);
    }

    public List<VideoMetaData> getAllSavedVideos() {
        Map<String, VideoMetaData> savedVideos = getAllVideosMap();

        return savedVideos.values().stream()
                .sorted(((videoMetaData, t1) -> Long.compare(t1.timestamp, videoMetaData.timestamp)))
                .collect(Collectors.toList());
    }

    public List<VideoMetaData> getRecentlyPlayed() {
        long sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000);
        return getAllSavedVideos().stream()
                .filter(videoMetaData -> videoMetaData.timestamp > sevenDaysAgo)
                .collect(Collectors.toList());
    }

    private Map<String, VideoMetaData> getAllVideosMap() {
        String json = prefs.getString(KEY_VIDEOS, "{}");
        Type type = new TypeToken<Map<String, VideoMetaData>>(){}.getType();
        try {
            return gson.fromJson(json, type);
        } catch (Exception e) {
            Log.e("VideoTvPlaybackManager", "Error parsing videos map", e);
            return new HashMap<>(); // Return empty map if there's an error
        }
    }

    private void saveVideosMap(Map<String, VideoMetaData> videos) {
        try {
            String json = gson.toJson(videos);
            prefs.edit().putString(KEY_VIDEOS, json).apply();
        } catch (OutOfMemoryError e) {
            Log.e("VideoTvPlaybackManager", "Out of memory while saving videos", e);

            // Emergency pruning to recover
            if (videos.size() > PRUNE_COUNT * 2) {
                pruneOldestEntries(videos, videos.size() / 2); // Remove half the entries
                try {
                    String json = gson.toJson(videos);
                    prefs.edit().putString(KEY_VIDEOS, json).apply();
                } catch (Exception innerE) {
                    // If we still can't save, clear everything
                    prefs.edit().remove(KEY_VIDEOS).apply();
                    Log.e("VideoTvPlaybackManager", "Emergency clearing of video progress data", innerE);
                }
            }
        }
    }

    /**
     * Cleans up old entries to prevent the cache from growing too large
     */
    public void performMaintenance() {
        Map<String, VideoMetaData> savedVideos = getAllVideosMap();

        // If we have more than MAX_VIDEOS, prune down to MAX_VIDEOS - PRUNE_COUNT
        if (savedVideos.size() > MAX_VIDEOS) {
            int excessCount = savedVideos.size() - (MAX_VIDEOS - PRUNE_COUNT);
            pruneOldestEntries(savedVideos, excessCount);
            saveVideosMap(savedVideos);
        }

        // Alternatively, remove videos older than 30 days
        long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
        savedVideos.entrySet().removeIf(entry -> entry.getValue().timestamp < thirtyDaysAgo);
        saveVideosMap(savedVideos);
    }
}
