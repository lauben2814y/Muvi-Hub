package muvi.anime.hub.data;

import java.io.Serializable;

public class DummyMedia implements Serializable {
    private int id;
    private String title;
    private String overview;
    private String release_date;
    private String poster_path;
    private String backdrop_path;
    private String watch_on;
    private String genres;

    public DummyMedia(int id, String title, String overview, String release_date, String poster, String backdropPath, String watchOn, String genres) {
        this.id = id;
        this.title = title;
        this.overview = overview;
        this.release_date = release_date;
        this.poster_path = poster;
        backdrop_path = backdropPath;
        watch_on = watchOn;
        this.genres = genres;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public String getRelease_date() {
        return release_date;
    }

    public void setRelease_date(String release_date) {
        this.release_date = release_date;
    }

    public String getPoster() {
        return poster_path;
    }

    public void setPoster(String poster) {
        this.poster_path = poster;
    }

    public String getBackdrop_path() {
        return backdrop_path;
    }

    public void setBackdrop_path(String backdrop_path) {
        this.backdrop_path = backdrop_path;
    }

    public String getWatch_on() {
        return watch_on;
    }

    public void setWatch_on(String watch_on) {
        this.watch_on = watch_on;
    }

    public String getGenres() {
        return genres;
    }
}
