package muvi.anime.hub.data.movie;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.io.Serializable;

public class TMDBMovieCast implements Serializable {
    private boolean adult;
    private int cast_id;
    private String character;
    private int id;
    private String name;
    private String profile_path;
    private String credit_id;

    public TMDBMovieCast(boolean adult, int cast_id, String character, int id, String name, String profile_path, String credit_id) {
        this.adult = adult;
        this.cast_id = cast_id;
        this.character = character;
        this.id = id;
        this.name = name;
        this.profile_path = profile_path;
        this.credit_id = credit_id;
    }

    public boolean isAdult() {
        return adult;
    }

    public void setAdult(boolean adult) {
        this.adult = adult;
    }

    public int getCast_id() {
        return cast_id;
    }

    public void setCast_id(int cast_id) {
        this.cast_id = cast_id;
    }

    public String getCharacter() {
        return character;
    }

    public void setCharacter(String character) {
        this.character = character;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProfile_path() {
        return "https://image.tmdb.org/t/p/w185" + profile_path;
    }

    public void setProfile_path(String profile_path) {
        this.profile_path = profile_path;
    }

    public String getCredit_id() {
        return credit_id;
    }

    public void setCredit_id(String credit_id) {
        this.credit_id = credit_id;
    }
}
