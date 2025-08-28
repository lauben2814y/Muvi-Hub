package muvi.anime.hub.data.tv;

import java.util.List;

public class TMDBTvResponse {
    private int page;
    private List<TMDBTv> results;
    private int total_pages;
    private int total_results;

    public TMDBTvResponse(int page, List<TMDBTv> results, int total_pages, int total_results) {
        this.page = page;
        this.results = results;
        this.total_pages = total_pages;
        this.total_results = total_results;
    }

    public int getPage() {
        return page;
    }

    public List<TMDBTv> getResults() {
        return results;
    }

    public int getTotal_pages() {
        return total_pages;
    }

    public int getTotal_results() {
        return total_results;
    }
}
