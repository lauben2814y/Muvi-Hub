package muvi.anime.hub.data.movie;

public class CollectionPart {
    private int id;
    private String title;
    private String overview;
    private String media_type;
    private String poster_path;

    public CollectionPart(int id, String title, String overview, String media_type, String poster_path) {
        this.id = id;
        this.title = title;
        this.overview = overview;
        this.media_type = media_type;
        this.poster_path = poster_path;
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

    public String getMedia_type() {
        return media_type;
    }

    public void setMedia_type(String media_type) {
        this.media_type = media_type;
    }

    public String getPoster_path() {
        return "http://image.tmdb.org/t/p/w500" + poster_path;
    }

    public void setPoster_path(String poster_path) {
        this.poster_path = poster_path;
    }
}
