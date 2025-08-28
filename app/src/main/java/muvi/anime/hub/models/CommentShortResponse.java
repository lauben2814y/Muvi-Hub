package muvi.anime.hub.models;

public class CommentShortResponse {
    private Comment comment;
    private MovieShort movie_short;

    public CommentShortResponse(Comment comment, MovieShort movie_short) {
        this.comment = comment;
        this.movie_short = movie_short;
    }

    public Comment getComment() {
        return comment;
    }

    public void setComment(Comment comment) {
        this.comment = comment;
    }

    public MovieShort getMovie_short() {
        return movie_short;
    }

    public void setMovie_short(MovieShort movie_short) {
        this.movie_short = movie_short;
    }
}
