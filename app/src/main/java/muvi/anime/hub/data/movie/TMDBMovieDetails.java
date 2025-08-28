package muvi.anime.hub.data.movie;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.List;

public class TMDBMovieDetails implements Serializable {
    private String overview;
    private boolean adult;
    private TMDBMovieCollection belongs_to_collection;
    private int budget;
    private String poster_path;
    private String backdrop_path;
    private List<String> origin_country;
    private String original_language;
    private double popularity;
    private int revenue;
    private TMDBVideoResult videos;
    private double vote_average;
    private int vote_count;
    private TMDBMovieCredit credits;
    private String release_date;
    private int runtime;


    public TMDBMovieDetails(String overview, boolean adult, TMDBMovieCollection belongs_to_collection, int budget, String posterPath, String backdropPath, List<String> origin_country, String original_language, double popularity, int revenue, TMDBVideoResult videos, double vote_average, int vote_count, TMDBMovieCredit credits, String releaseDate, int runtime) {
        this.overview = overview;
        this.adult = adult;
        this.belongs_to_collection = belongs_to_collection;
        this.budget = budget;
        this.poster_path = posterPath;
        this.backdrop_path = backdropPath;
        this.origin_country = origin_country;
        this.original_language = original_language;
        this.popularity = popularity;
        this.revenue = revenue;
        this.videos = videos;
        this.vote_average = vote_average;
        this.vote_count = vote_count;
        this.credits = credits;
        this.release_date = releaseDate;
        this.runtime = runtime;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public boolean getAdult() {
        return adult;
    }

    public void setAdult(boolean adult) {
        this.adult = adult;
    }

    public TMDBMovieCollection getBelongs_to_collection() {
        return belongs_to_collection;
    }

    public void setBelongs_to_collection(TMDBMovieCollection belongs_to_collection) {
        this.belongs_to_collection = belongs_to_collection;
    }

    public int getBudget() {
        return budget;
    }

    public void setBudget(int budget) {
        this.budget = budget;
    }

    public List<String> getOrigin_country() {
        return origin_country;
    }

    public void setOrigin_country(List<String> origin_country) {
        this.origin_country = origin_country;
    }

    public String getOriginal_language() {
        return original_language;
    }

    public void setOriginal_language(String original_language) {
        this.original_language = original_language;
    }

    public double getPopularity() {
        return popularity;
    }

    public void setPopularity(double popularity) {
        this.popularity = popularity;
    }

    public int getRevenue() {
        return revenue;
    }

    public void setRevenue(int revenue) {
        this.revenue = revenue;
    }

    public TMDBVideoResult getVideos() {
        return videos;
    }

    public void setVideos(TMDBVideoResult videos) {
        this.videos = videos;
    }

    public double getVote_average() {
        return vote_average;
    }

    public void setVote_average(double vote_average) {
        this.vote_average = vote_average;
    }

    public int getVote_count() {
        return vote_count;
    }

    public void setVote_count(int vote_count) {
        this.vote_count = vote_count;
    }

    public TMDBMovieCredit getCredits() {
        return credits;
    }

    public void setCredits(TMDBMovieCredit credits) {
        this.credits = credits;
    }

    public String getPoster_path() {
        return poster_path;
    }

    public void setPoster_path(String poster_path) {
        this.poster_path = poster_path;
    }

    public String getBackdrop_path() {
        return "https://image.tmdb.org/t/p/w1280" + backdrop_path;
    }

    public void setBackdrop_path(String backdrop_path) {
        this.backdrop_path = backdrop_path;
    }

    public String getRelease_date() {
        return release_date;
    }

    public void setRelease_date(String release_date) {
        this.release_date = release_date;
    }

    public int getRuntime() {
        return runtime;
    }

    public void setRuntime(int runtime) {
        this.runtime = runtime;
    }
}
