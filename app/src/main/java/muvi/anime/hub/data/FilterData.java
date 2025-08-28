package muvi.anime.hub.data;

import java.io.Serializable;
import java.util.List;

public class FilterData implements Serializable {
    private String orderBy;
    private int page;
    private List<String> genres;
    private List<String> vjs;
    private List<String> countries;

    public FilterData(String orderBy, int page, List<String> genres, List<String> vjs, List<String> countries) {
        this.orderBy = orderBy;
        this.page = page;
        this.genres = genres;
        this.vjs = vjs;
        this.countries = countries;
    }

    public List<String> getGenres() {
        return genres;
    }

    public void setGenres(List<String> genres) {
        this.genres = genres;
    }

    public List<String> getVjs() {
        return vjs;
    }

    public List<String> getCountries() {
        return countries;
    }

    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    // Formatted Filters
    public String getCountryQuery() {
        return getCountries() != null ? String.join(",", this.getCountries()) : null;
    }

    public String getVjQuery() {
        return getVjs() != null ? String.join(",", this.getVjs()) : null;
    }

    public String getGenreQuery() {
        return getGenres() != null ? String.join(",", this.getGenres()) : null;
    }

    public String getPageQuery() {
        int from = (this.getPage() - 1) * 20;
        int to = from + 20 - 1;

        return from + "-" + to;
    }
}
