package muvi.anime.hub.models;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String user_name;
    private String user_id;
    private String user_email;
    private List<MovieShort> liked_shorts;
    private List<User> followers;
    private List<MovieShort> favorites;
    private List<User> following;
    private String profile_url;

    //Media Auth
    private String media_password;
    private String media_user_name;

    public User(String user_name, String user_id, String user_email, List<MovieShort> liked_shorts, List<User> followers, List<MovieShort> favorites, List<User> following, String profileUrl, String mediaPassword, String mediaUserName) {
        this.user_name = user_name;
        this.user_id = user_id;
        this.user_email = user_email;
        this.liked_shorts = liked_shorts;
        this.followers = followers;
        this.favorites = favorites;
        this.following = following;
        this.profile_url = profileUrl;
        this.media_password = mediaPassword;
        this.media_user_name = mediaUserName;
    }

    public String getUser_name() {
        return user_name;
    }

    public void setUser_name(String user_name) {
        this.user_name = user_name;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getUser_email() {
        return user_email;
    }

    public void setUser_email(String user_email) {
        this.user_email = user_email;
    }

    public List<MovieShort> getLiked_shorts() {
        return liked_shorts != null ? liked_shorts : new ArrayList<>();
    }

    public void setLiked_shorts(List<MovieShort> liked_shorts) {
        this.liked_shorts = liked_shorts;
    }

    public List<User> getFollowers() {
        return followers;
    }

    public void setFollowers(List<User> followers) {
        this.followers = followers;
    }

    public List<MovieShort> getFavorites() {
        return favorites;
    }

    public void setFavorites(List<MovieShort> favorites) {
        this.favorites = favorites;
    }

    public List<User> getFollowing() {
        return following;
    }

    public void setFollowing(List<User> following) {
        this.following = following;
    }

    public String getProfile_url() {
        return profile_url;
    }

    public void setProfile_url(String profile_url) {
        this.profile_url = profile_url;
    }

    public String getMedia_password() {
        return media_password;
    }

    public void setMedia_password(String media_password) {
        this.media_password = media_password;
    }

    public String getMedia_user_name() {
        return media_user_name;
    }

    public void setMedia_user_name(String media_user_name) {
        this.media_user_name = media_user_name;
    }
}
