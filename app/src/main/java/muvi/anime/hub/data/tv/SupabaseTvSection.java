package muvi.anime.hub.data.tv;

import java.io.Serializable;
import java.util.List;

public class SupabaseTvSection implements Serializable {
    private String header;
    private List<SupabaseTv> supabaseTvs;
    private boolean placeHolder;

    public SupabaseTvSection(String header, List<SupabaseTv> supabaseTvs) {
        this.header = header;
        this.supabaseTvs = supabaseTvs;
    }

    public SupabaseTvSection(boolean placeHolder) {
        this.placeHolder = placeHolder;
    }

    public String getHeader() {
        return header;
    }

    public List<SupabaseTv> getSupabaseTvs() {
        return supabaseTvs;
    }

    public void setSupabaseTvs(List<SupabaseTv> supabaseTvs) {
        this.supabaseTvs = supabaseTvs;
    }

    public boolean isPlaceHolder() {
        return placeHolder;
    }

    public void setPlaceHolder(boolean placeHolder) {
        this.placeHolder = placeHolder;
    }
}
