package muvi.anime.hub.data.tv;

import java.util.List;

public class TMDBTvSection {
    private String header;
    private List<TMDBTv> tmdbTvList;

    public TMDBTvSection(String header, List<TMDBTv> tmdbTvList) {
        this.header = header;
        this.tmdbTvList = tmdbTvList;
    }

    public String getHeader() {
        return header;
    }

    public List<TMDBTv> getTmdbTvList() {
        return tmdbTvList;
    }
}
