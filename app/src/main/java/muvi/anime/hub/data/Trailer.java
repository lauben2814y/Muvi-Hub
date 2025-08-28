package muvi.anime.hub.data;

public class Trailer {
    private String video_id;
    private boolean placeholder;

    public Trailer(String video_id) {
        this.video_id = video_id;
    }

    public Trailer(boolean placeholder) {
        this.placeholder = placeholder;
    }

    public String getVideo_id() {
        return video_id;
    }

    public void setVideo_id(String video_id) {
        this.video_id = video_id;
    }

    public boolean isPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(boolean placeholder) {
        this.placeholder = placeholder;
    }
}
