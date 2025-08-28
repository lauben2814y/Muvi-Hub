package muvi.anime.hub.data.movie;

public class SupabaseMovieSearchBody {
    private String query;

    public SupabaseMovieSearchBody(String query) {
        this.query = query;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}
