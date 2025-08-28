package muvi.anime.hub.data.movie;

import java.util.List;

public class TMDBMovieResponse {
    private int page;
    private List<TMDBMovie> results;
    private int total_pages;
    private int total_results;

    public TMDBMovieResponse(int page, List<TMDBMovie> results, int total_pages, int total_results) {
        this.page = page;
        this.results = results;
        this.total_pages = total_pages;
        this.total_results = total_results;
    }

    public int getPage() {
        return page;
    }

    public List<TMDBMovie> getResults() {
        return results;
    }

    public int getTotal_pages() {
        return total_pages;
    }

    public int getTotal_results() {
        return total_results;
    }
}
