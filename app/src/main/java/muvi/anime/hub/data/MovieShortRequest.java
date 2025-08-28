package muvi.anime.hub.data;

public class MovieShortRequest {
    private int id;
    private String column;

    public MovieShortRequest(int id, String column) {
        this.id = id;
        this.column = column;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }
}
