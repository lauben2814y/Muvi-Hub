package muvi.anime.hub.player;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VideoPlaybackManager {
    private static final String PREFS_NAME = "VideoPlaybackPrefs";
    private static final String KEY_VIDEOS = "saved_videos";
    private final SharedPreferences prefs;
    private final Gson gson;

    public VideoPlaybackManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    // Save video progress with meta data
    public void saveVideoProgress(VideoMetaData videoMetaData) {
        Map<String, VideoMetaData> savedVideos = getAllVideosMap();

        // update timestamp and add/update video
        videoMetaData.timestamp = System.currentTimeMillis();
        savedVideos.put(String.valueOf(videoMetaData.supabaseMovie.getId()), videoMetaData);

        // save updated map
        saveVideosMap(savedVideos);
    }

    // Get saved video progress
    public VideoMetaData getVideoProgress(String videoId) {
        Map<String, VideoMetaData> savedVideos = getAllVideosMap();
        return savedVideos.get(videoId);
    }

    // clear video progress
    public void clearVideoProgress(String videoId) {
        Map<String, VideoMetaData> savedVideos = getAllVideosMap();
        savedVideos.remove(videoId);
        saveVideosMap(savedVideos);
    }

    // get all saved videos
    public List<VideoMetaData> getAllSavedVideos() {
        Map<String, VideoMetaData> savedVideos = getAllVideosMap();

        // sort by timestamp most recent first
        return savedVideos.values().stream()
                .sorted(((videoMetaData, t1) -> Long.compare(t1.timestamp, videoMetaData.timestamp)))
                .collect(Collectors.toList());
    }

    // get recently played videos (with in lats 7 days)
    public List<VideoMetaData> getRecentlyPlayed() {
        long sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000);
        return getAllSavedVideos().stream()
                .filter(videoMetaData -> videoMetaData.timestamp > sevenDaysAgo)
                .collect(Collectors.toList());
    }

    private Map<String, VideoMetaData> getAllVideosMap() {
        String json = prefs.getString(KEY_VIDEOS, "{}");
        Type type = new TypeToken<Map<String, VideoMetaData>>(){}.getType();
        return gson.fromJson(json, type);
    }

    private void saveVideosMap(Map<String, VideoMetaData> videos) {
        String json = gson.toJson(videos);
        prefs.edit().putString(KEY_VIDEOS, json).apply();
    }
}
