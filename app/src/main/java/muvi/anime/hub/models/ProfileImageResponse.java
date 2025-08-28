package muvi.anime.hub.models;

import com.google.gson.annotations.SerializedName;

public class ProfileImageResponse {
    @SerializedName("Id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("path")
    private String path;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }
}
