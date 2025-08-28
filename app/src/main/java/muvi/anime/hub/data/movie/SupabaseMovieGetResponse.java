package muvi.anime.hub.data.movie;

public class SupabaseMovieGetResponse {
    private int count;
    private SupabaseMovie data;
    private String message;

    public SupabaseMovieGetResponse(int count, SupabaseMovie data, String message) {
        this.count = count;
        this.data = data;
        this.message = message;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public SupabaseMovie getData() {
        return data;
    }

    public void setData(SupabaseMovie data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
