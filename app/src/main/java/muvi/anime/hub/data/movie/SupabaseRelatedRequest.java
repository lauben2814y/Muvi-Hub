package muvi.anime.hub.data.movie;

import java.util.List;

public class SupabaseRelatedRequest {
    private int id;
    private int page;
    private List<String> genres;

    public SupabaseRelatedRequest(int id, int page, List<String> genres) {
        this.id = id;
        this.page = page;
        this.genres = genres;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public List<String> getGenres() {
        return genres;
    }

    public void setGenres(List<String> genres) {
        this.genres = genres;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
