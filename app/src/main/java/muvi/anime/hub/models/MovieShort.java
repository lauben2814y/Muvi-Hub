package muvi.anime.hub.models;

import java.io.Serializable;
import java.util.List;

public class MovieShort implements Serializable {
    private int id;
    private long start;
    private String created_at;
    private long end;
    private int movie_id;
    private String translated_url;
    private String title;
    private List<String> genres;
    private String overview;
    private int likes;
    private int dislikes;
    private boolean isLiked = false;
    private List<Comment> comments;

    public MovieShort(int id, long start, String createdAt, long end, int movie_id, String translated_url, String title, List<String> genres, String overview, int likes, int dislikes, List<Comment> comments) {
        this.id = id;
        this.start = start;
        this.created_at = createdAt;
        this.end = end;
        this.movie_id = movie_id;
        this.translated_url = translated_url;
        this.title = title;
        this.genres = genres;
        this.overview = overview;
        this.likes = likes;
        this.dislikes = dislikes;
        this.comments = comments;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public int getMovie_id() {
        return movie_id;
    }

    public void setMovie_id(int movie_id) {
        this.movie_id = movie_id;
    }

    public String getTranslated_url() {
        return translated_url;
    }

    public void setTranslated_url(String translated_url) {
        this.translated_url = translated_url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public List<String> getGenres() {
        return genres;
    }

    public String getOverview() {
        return overview;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public int getDislikes() {
        return dislikes;
    }

    public void setDislikes(int dislikes) {
        this.dislikes = dislikes;
    }

    public boolean isLiked() {
        return isLiked;
    }

    public void setLiked(boolean liked) {
        isLiked = liked;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }
}