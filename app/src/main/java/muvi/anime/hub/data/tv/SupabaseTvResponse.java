package muvi.anime.hub.data.tv;

import java.util.List;

public class SupabaseTvResponse {
    private String message;
    private List<SupabaseTv> data;
    private int count;

    public SupabaseTvResponse(String message, List<SupabaseTv> data, int count) {
        this.message = message;
        this.data = data;
        this.count = count;
    }

    public String getMessage() {
        return message;
    }

    public List<SupabaseTv> getData() {
        return data;
    }

    public int getCount() {
        return count;
    }
}
