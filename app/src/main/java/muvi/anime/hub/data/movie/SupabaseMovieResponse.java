package muvi.anime.hub.data.movie;

import java.util.List;

public class SupabaseMovieResponse {
    private int count;
    private List<SupabaseMovie> data;
    private String message;

    public SupabaseMovieResponse(int count, List<SupabaseMovie> data, String message) {
        this.count = count;
        this.data = data;
        this.message = message;
    }

    public int getCount() {
        return count;
    }

    public List<SupabaseMovie> getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }
}
