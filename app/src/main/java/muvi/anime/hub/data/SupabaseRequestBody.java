package muvi.anime.hub.data;

public class SupabaseRequestBody {
    private int page;

    public SupabaseRequestBody(int page) {
        this.page = page;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }
}
