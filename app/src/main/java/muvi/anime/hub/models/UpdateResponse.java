package muvi.anime.hub.models;

import com.google.gson.annotations.SerializedName;

public class UpdateResponse {

    @SerializedName("has_update")
    public boolean hasUpdate;

    @SerializedName("version_name")
    public String versionName;

    @SerializedName("version_code")
    public int versionCode;

    @SerializedName("download_url")
    public String downloadUrl;

    @SerializedName("file_size")
    public long fileSize;

    @SerializedName("file_checksum")
    public String fileChecksum;

    @SerializedName("changelog")
    public String changelog;

    @SerializedName("force_update")
    public boolean forceUpdate;

    @SerializedName("min_supported_version")
    public int minSupportedVersion;

    @SerializedName("release_date")
    public String releaseDate;

    @SerializedName("security_critical")
    public boolean securityCritical;

    @SerializedName("github_release_url")
    public String githubReleaseUrl;

    @SerializedName("github_tag_name")
    public String githubTagName;

    @SerializedName("current_version")
    public int currentVersion;

    @SerializedName("latest_version")
    public int latestVersion;

    @SerializedName("message")
    public String message;
}
