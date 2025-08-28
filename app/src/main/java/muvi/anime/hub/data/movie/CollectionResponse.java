package muvi.anime.hub.data.movie;

import java.util.List;

public class CollectionResponse {
    private int id;
    private String name;
    private String overview;
    private String poster_path;
    private String backdrop_path;
    private List<CollectionPart> parts;

    public CollectionResponse(int id, String name, String overview, String poster_path, String backdrop_path, List<CollectionPart> parts) {
        this.id = id;
        this.name = name;
        this.overview = overview;
        this.poster_path = poster_path;
        this.backdrop_path = backdrop_path;
        this.parts = parts;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public String getPoster_path() {
        return poster_path;
    }

    public void setPoster_path(String poster_path) {
        this.poster_path = poster_path;
    }

    public String getBackdrop_path() {
        return backdrop_path;
    }

    public void setBackdrop_path(String backdrop_path) {
        this.backdrop_path = backdrop_path;
    }

    public List<CollectionPart> getParts() {
        return parts;
    }

    public void setParts(List<CollectionPart> parts) {
        this.parts = parts;
    }
}
