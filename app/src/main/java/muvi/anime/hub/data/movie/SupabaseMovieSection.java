package muvi.anime.hub.data.movie;

import java.io.Serializable;
import java.util.List;

public class SupabaseMovieSection implements Serializable {
    private String header;
    private List<SupabaseMovie> supabaseMovieList;
    private int page = 2;
    private boolean placeHolder;

    public SupabaseMovieSection(String header, List<SupabaseMovie> supabaseMovieList) {
        this.header = header;
        this.supabaseMovieList = supabaseMovieList;
    }

    public SupabaseMovieSection(boolean placeHolder) {
        this.placeHolder = placeHolder;
    }

    public String getHeader() {
        return header;
    }

    public List<SupabaseMovie> getSupabaseMovieList() {
        return supabaseMovieList;
    }

    public void setSupabaseMovieList(List<SupabaseMovie> newMovies) {
        this.supabaseMovieList = newMovies;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public boolean isPlaceHolder() {
        return placeHolder;
    }

    public void setPlaceHolder(boolean placeHolder) {
        this.placeHolder = placeHolder;
    }
}
