package muvi.anime.hub.data.movie;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class SupabaseMovie implements Serializable {
    private String title;
    private int id;
    private String translated;
    private String translated_2;
    private String non_translated;
    private String vj;
    private List<String> genres;
    private boolean placeHolder;
    private String isLoading;
    private String logo_path;
    private String currentStreamUrl;
    private String poster_path;
    private String release_date;
    private String overview;
    private String backdrop_path;

    public SupabaseMovie(String title, int id, String translated, String translated2, String non_translated, String vj, List<String> genres, String logoPath, String posterPath, String releaseDate, String overview, String backdropPath) {
        this.title = title;
        this.id = id;
        this.translated = translated;
        this.translated_2 = translated2;
        this.non_translated = non_translated;
        this.vj = vj;
        this.genres = genres;
        this.logo_path = logoPath;
        this.poster_path = posterPath;
        this.release_date = releaseDate;
        this.overview = overview;
        backdrop_path = backdropPath;
    }

    public SupabaseMovie(boolean placeHolder) {
        this.placeHolder = placeHolder;
    }

    public SupabaseMovie(String isLoading) {
        this.isLoading = isLoading;
    }

    public String getIsLoading() {
        return isLoading;
    }

    public String getTitle() {
        return title;
    }

    public int getId() {
        return id;
    }

    public String getLogo_path() {
        return (logo_path != null && !Objects.equals(logo_path, "")) ? "https://image.tmdb.org/t/p/w300" + logo_path : null;
    }

    public String getTranslated() {
        return translated;
    }

    public String getNon_translated() {
        return non_translated;
    }

    public String getVj() {
        return  (!Objects.equals(vj, "") && vj != null)  ? vj : "Non translated";
    }

    public String getPoster_path() {
        return "https://image.tmdb.org/t/p/w500" + poster_path;
    }

    public String getRelease_date() {
        return release_date;
    }

    public List<String> getGenres() {
        return genres;
    }


    public boolean isPlaceHolder() {
        return placeHolder;
    }

    public String formatRuntime(int totalMinutes) {
        if (totalMinutes <= 0) {
            return "0min";
        }

        int hours = totalMinutes / 60; // Calculate hours
        int minutes = totalMinutes % 60; // Calculate remaining minutes

        // Build the formatted string
        StringBuilder formattedTime = new StringBuilder();
        if (hours > 0) {
            formattedTime.append(hours).append("hr");
        }
        if (minutes > 0) {
            if (hours > 0) {
                formattedTime.append(" ");
            }
            formattedTime.append(minutes).append("min");
        }

        return formattedTime.toString();
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

    public double getFormattedVote(double value) {
        return Math.round(value * 10) / 10.0;
    }

    public String getTranslated_2() {
        return translated_2;
    }

    public String getCurrentStreamUrl() {
        return currentStreamUrl;
    }

    public void setCurrentStreamUrl(String currentStreamUrl) {
        this.currentStreamUrl = currentStreamUrl;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public String getBackdrop_path() {
        return "https://image.tmdb.org/t/p/w1280" + backdrop_path;
    }

    public void setBackdrop_path(String backdrop_path) {
        this.backdrop_path = backdrop_path;
    }
}
