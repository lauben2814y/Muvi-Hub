package muvi.anime.hub.player.tv;

import java.io.Serializable;

import muvi.anime.hub.data.tv.Episode;
import muvi.anime.hub.data.tv.SupabaseTv;

public class VideoMetaData implements Serializable {
    public Episode episode;
    public long lastPosition;
    public long timestamp;
    public SupabaseTv supabaseTv;

    public VideoMetaData(Episode episode, long lastPosition, long timestamp, SupabaseTv supabaseTv) {
        this.episode = episode;
        this.lastPosition = lastPosition;
        this.timestamp = timestamp;
        this.supabaseTv = supabaseTv;
    }
}
