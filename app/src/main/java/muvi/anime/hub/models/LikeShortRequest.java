package muvi.anime.hub.models;

public class LikeShortRequest {
    private String input_user_id;
    private int short_id;
    private MovieShort short_data;

    public LikeShortRequest(String input_user_id, int short_id, MovieShort short_data) {
        this.input_user_id = input_user_id;
        this.short_id = short_id;
        this.short_data = short_data;
    }

    public String getInput_user_id() {
        return input_user_id;
    }

    public void setInput_user_id(String input_user_id) {
        this.input_user_id = input_user_id;
    }

    public int getShort_id() {
        return short_id;
    }

    public void setShort_id(int short_id) {
        this.short_id = short_id;
    }

    public MovieShort getShort_data() {
        return short_data;
    }

    public void setShort_data(MovieShort short_data) {
        this.short_data = short_data;
    }
}
