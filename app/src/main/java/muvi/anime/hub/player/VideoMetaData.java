package muvi.anime.hub.player;

import java.io.Serializable;

import muvi.anime.hub.data.movie.SupabaseMovie;

public class VideoMetaData implements Serializable {
    public SupabaseMovie supabaseMovie;
    public long lastPosition;
    public long timestamp;

    public VideoMetaData(SupabaseMovie supabaseMovie, long lastPosition, long timestamp) {
        this.supabaseMovie = supabaseMovie;
        this.lastPosition = lastPosition;
        this.timestamp = timestamp;
    }
}
