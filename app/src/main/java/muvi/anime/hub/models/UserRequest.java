package muvi.anime.hub.models;

public class UserRequest {
    private String p_user_id;
    private String p_user_name;
    private String p_user_email;

    public UserRequest(String p_user_id, String p_user_name, String p_user_email) {
        this.p_user_id = p_user_id;
        this.p_user_name = p_user_name;
        this.p_user_email = p_user_email;
    }

    public String getP_user_id() {
        return p_user_id;
    }

    public void setP_user_id(String p_user_id) {
        this.p_user_id = p_user_id;
    }

    public String getP_user_name() {
        return p_user_name;
    }

    public void setP_user_name(String p_user_name) {
        this.p_user_name = p_user_name;
    }

    public String getP_user_email() {
        return p_user_email;
    }

    public void setP_user_email(String p_user_email) {
        this.p_user_email = p_user_email;
    }
}
