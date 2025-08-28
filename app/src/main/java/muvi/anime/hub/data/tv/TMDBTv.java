package muvi.anime.hub.data.tv;

import java.io.Serializable;
import java.util.List;

public class TMDBTv implements Serializable {
    private boolean adult;
    private String backdrop_path;
    private List<Integer> genre_ids;
    private int id;
    private String original_language;
    private String original_name;
    private String overview;
    private double popularity;
    private String poster_path;
    private String first_air_date;
    private String name;
    private double vote_average;
    private int vote_count;
    private TMDBTvCredit credits;

    public TMDBTv(boolean adult, String backdrop_path, List<Integer> genre_ids, int id, String original_language, String original_name, String overview, double popularity, String poster_path, String first_air_date, String name, double vote_average, int vote_count, TMDBTvCredit credits) {
        this.adult = adult;
        this.backdrop_path = backdrop_path;
        this.genre_ids = genre_ids;
        this.id = id;
        this.original_language = original_language;
        this.original_name = original_name;
        this.overview = overview;
        this.popularity = popularity;
        this.poster_path = poster_path;
        this.first_air_date = first_air_date;
        this.name = name;
        this.vote_average = vote_average;
        this.vote_count = vote_count;
        this.credits = credits;
    }

    public boolean isAdult() {
        return adult;
    }

    public String getBackdrop_path() {
        return "https://image.tmdb.org/t/p/w1280" + backdrop_path;
    }

    public List<Integer> getGenre_ids() {
        return genre_ids;
    }

    public int getId() {
        return id;
    }

    public String getOriginal_language() {
        return original_language;
    }

    public String getOriginal_name() {
        return original_name;
    }

    public String getOverview() {
        return overview;
    }

    public double getPopularity() {
        return popularity;
    }

    public String getPoster_path() {
        return poster_path;
    }

    public String getFirst_air_date() {
        return first_air_date;
    }

    public String getName() {
        return name;
    }

    public double getVote_average() {
        return vote_average;
    }

    public int getVote_count() {
        return vote_count;
    }

    public TMDBTvCredit getCredits() {
        return credits;
    }

    public void setCredits(TMDBTvCredit credits) {
        this.credits = credits;
    }
}
