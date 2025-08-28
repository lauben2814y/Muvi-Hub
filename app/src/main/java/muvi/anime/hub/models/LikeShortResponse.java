package muvi.anime.hub.models;

public class LikeShortResponse {
    private MovieShort movieShort;
    private boolean success;
    private User user;

    public LikeShortResponse(MovieShort movieShort, boolean success, User user) {
        this.movieShort = movieShort;
        this.success = success;
        this.user = user;
    }

    public MovieShort getMovieShort() {
        return movieShort;
    }

    public void setMovieShort(MovieShort movieShort) {
        this.movieShort = movieShort;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
