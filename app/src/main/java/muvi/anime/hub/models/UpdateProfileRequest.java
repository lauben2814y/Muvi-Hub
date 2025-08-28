package muvi.anime.hub.models;

public class UpdateProfileRequest {
    private String p_user_id;
    private String p_profile_url;

    public UpdateProfileRequest(String p_user_id, String p_profile_url) {
        this.p_user_id = p_user_id;
        this.p_profile_url = p_profile_url;
    }

    public String getP_user_id() {
        return p_user_id;
    }

    public void setP_user_id(String p_user_id) {
        this.p_user_id = p_user_id;
    }

    public String getP_profile_url() {
        return p_profile_url;
    }

    public void setP_profile_url(String p_profile_url) {
        this.p_profile_url = p_profile_url;
    }
}
