package muvi.anime.hub.models;

import com.google.gson.annotations.SerializedName;

public class UpdateStatsResponse {

    @SerializedName("totalChecks")
    public int totalChecks;

    @SerializedName("uniqueUsers")
    public int uniqueUsers;

    @SerializedName("totalReleases")
    public int totalReleases;

    @SerializedName("versionDistribution")
    public java.util.List<VersionDistribution> versionDistribution;

    @SerializedName("timestamp")
    public String timestamp;

    public static class VersionDistribution {
        @SerializedName("current_version")
        public int currentVersion;

        @SerializedName("count")
        public long count;
    }
}
