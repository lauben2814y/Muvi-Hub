package muvi.anime.hub.models;

public class Comment {
    private int id;
    private String user_id;
    private String user_name;
    private int short_id;
    private String comment;
    private String created_at;
    private int likes;
    private int dislikes;
    private String comment_date;

    public Comment(int id, String user_id, String user_name, int short_id, String comment, String created_at, int likes, int dislikes, String commentDate) {
        this.id = id;
        this.user_id = user_id;
        this.user_name = user_name;
        this.short_id = short_id;
        this.comment = comment;
        this.created_at = created_at;
        this.likes = likes;
        this.dislikes = dislikes;
        this.comment_date = commentDate;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getUser_name() {
        return user_name;
    }

    public void setUser_name(String user_name) {
        this.user_name = user_name;
    }

    public int getShort_id() {
        return short_id;
    }

    public void setShort_id(int short_id) {
        this.short_id = short_id;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
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

    public String getComment_date() {
        return comment_date;
    }

    public void setComment_date(String comment_date) {
        this.comment_date = comment_date;
    }
}
