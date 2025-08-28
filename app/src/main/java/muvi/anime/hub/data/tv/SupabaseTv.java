package muvi.anime.hub.data.tv;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class SupabaseTv implements Serializable {
    private String name;
    private String poster_path;
    private String first_air_date;
    private int id;
    private List<Season> seasons;
    private String vj;
    private List<String> genres;
    private boolean placeHolder;
    private String logo_path;
    private String overview;

    public SupabaseTv(String name, String poster_path, String first_air_date, int id, List<Season> seasons, String vj, List<String> genres, String logoPath, String overview) {
        this.name = name;
        this.poster_path = poster_path;
        this.first_air_date = first_air_date;
        this.id = id;
        this.seasons = seasons;
        this.vj = vj;
        this.genres = genres;
        this.logo_path = logoPath;
        this.overview = overview;
    }

    public SupabaseTv(boolean placeHolder) {
        this.placeHolder = placeHolder;
    }

    public String getName() {
        return name;
    }

    public String getPoster_path() {
        return "https://image.tmdb.org/t/p/w500" + poster_path;
    }

    public String getYearFromDate(String date) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date parsedDate = dateFormat.parse(date);
            SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy", Locale.getDefault());
            return yearFormat.format(parsedDate);
        } catch (ParseException e) {
            e.printStackTrace();
            return "Invalid Date";
        }
    }

    public String getFirst_air_date() {
        return first_air_date;
    }

    public int getId() {
        return id;
    }

    public List<Season> getSeasons() {
        List<Season> filteredSeasons = new ArrayList<>();
        for (Season season : seasons) {
            if (season.getEpisodes() != null && !season.getEpisodes().isEmpty()) {
                filteredSeasons.add(season);
            }
        }
        return filteredSeasons;
    }

    public String getVj() {
        return  (!Objects.equals(vj, "") && vj != null)  ? vj : "Non translated";
    }

    public List<String> getGenres() {
        return genres;
    }

    public boolean isPlaceHolder() {
        return placeHolder;
    }

    public String getLogo_path() {
        return (logo_path != null && !Objects.equals(logo_path, "")) ? "https://image.tmdb.org/t/p/w300" + logo_path : null;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }
}
