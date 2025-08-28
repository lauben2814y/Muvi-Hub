package muvi.anime.hub.models;

public class CommentShortRequest {
    private String input_user_id;
    private String input_user_name;
    private int short_id;
    private String comment_text;
    private String input_comment_date;

    public CommentShortRequest(String input_user_id, String input_user_name, int short_id, String comment_text, String inputCommentDate) {
        this.input_user_id = input_user_id;
        this.input_user_name = input_user_name;
        this.short_id = short_id;
        this.comment_text = comment_text;
        this.input_comment_date = inputCommentDate;
    }

    public String getInput_user_id() {
        return input_user_id;
    }

    public void setInput_user_id(String input_user_id) {
        this.input_user_id = input_user_id;
    }

    public String getInput_user_name() {
        return input_user_name;
    }

    public void setInput_user_name(String input_user_name) {
        this.input_user_name = input_user_name;
    }

    public int getShort_id() {
        return short_id;
    }

    public void setShort_id(int short_id) {
        this.short_id = short_id;
    }

    public String getComment_text() {
        return comment_text;
    }

    public void setComment_text(String comment_text) {
        this.comment_text = comment_text;
    }

    public String getInput_comment_date() {
        return input_comment_date;
    }

    public void setInput_comment_date(String input_comment_date) {
        this.input_comment_date = input_comment_date;
    }
}
