package muvi.anime.hub.data.movie;

public class SupabaseCollectionRequestBody {
    private int id;

    public SupabaseCollectionRequestBody(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
