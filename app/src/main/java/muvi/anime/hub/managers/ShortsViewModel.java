package muvi.anime.hub.managers;

import android.util.SparseArray;

import androidx.lifecycle.ViewModel;
import androidx.media3.exoplayer.ExoPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import muvi.anime.hub.models.MovieShort;

public class ShortsViewModel extends ViewModel {
    private final List<MovieShort> videos = new ArrayList<>();
    private final SparseArray<ExoPlayer> playerCache = new SparseArray<>();
    private int currentPosition = 0;

    public List<MovieShort> getVideos() {
        return videos;
    }

    public void setVideos(List<MovieShort> videos) {
        this.videos.addAll(videos);
    }

    public void cachePlayer(int position, ExoPlayer player) {
        playerCache.put(position, player);
    }

    public void removeCachePlayer(int position) {
        playerCache.remove(position);
    }

    public void maintainPlayerCache(int currentPosition) {
        List<Integer> positions = new ArrayList<>();

        // Collect all existing keys (player positions)
        for (int i = 0; i < playerCache.size(); i++) {
            positions.add(playerCache.keyAt(i));
        }

        // Sort positions in ascending order
        Collections.sort(positions);

        Integer prev = null, next = null;

        // Find the direct previous and next positions
        for (int i = 0; i < positions.size(); i++) {
            int pos = positions.get(i);
            if (pos == currentPosition - 1) prev = pos;  // Only keep the immediate predecessor
            if (pos == currentPosition + 1) {
                next = pos;  // Only keep the immediate successor
                break;       // No need to check further
            }
        }

        // Determine which positions to keep
        Set<Integer> keep = new HashSet<>();
        keep.add(currentPosition);
        if (prev != null) keep.add(prev);
        if (next != null) keep.add(next);

        // Release and remove all other players
        for (int pos : positions) {
            if (!keep.contains(pos)) {
                releaseAndRemovePlayer(pos);
            }
        }
    }

    private void releaseAndRemovePlayer(int position) {
        ExoPlayer player = playerCache.get(position);
        if (player != null) {
            player.release(); // Release the ExoPlayer instance
            playerCache.remove(position); // Remove from cache
        }
    }


    public ExoPlayer getPlayer(int position) {
        return playerCache.get(position);
    }

    // Called when fragment enters background
    public void pauseAllPlayers() {
        for (int i = 0; i < playerCache.size(); i++) {
            int position = playerCache.keyAt(i);
            ExoPlayer player = playerCache.valueAt(i);

            if (player != null) {
                player.setPlayWhenReady(false);
            }
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Only release players if app is actually closing
        for (int i = 0; i < playerCache.size(); i++) {
            ExoPlayer player = playerCache.valueAt(i);
            if (player != null) {
                player.release();
            }
        }
        playerCache.clear();
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    public void setCurrentPosition(int currentPosition) {
        this.currentPosition = currentPosition;
    }

    public int cacheSize() {
        return playerCache.size();
    }
}
