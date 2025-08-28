package muvi.anime.hub.data.movie;

import java.util.List;

public class TMDBMovieSection {
    private String header;
    private List<TMDBMovie> tmdbMovieList;
    private int nextPage = 2;

    public TMDBMovieSection(String header, List<TMDBMovie> tmdbMovieList) {
        this.header = header;
        this.tmdbMovieList = tmdbMovieList;
    }

    public String getHeader() {
        return header;
    }

    public List<TMDBMovie> getTmdbMovieList() {
        return tmdbMovieList;
    }

    public int getNextPage() {
        return nextPage;
    }

    public void setNextPage(int nextPage) {
        this.nextPage = nextPage;
    }

    public void incrementNextPage() {
        this.nextPage++;
    }
}
