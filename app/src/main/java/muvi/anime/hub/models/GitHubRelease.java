package muvi.anime.hub.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class GitHubRelease {

    @SerializedName("id")
    public long id;

    @SerializedName("tag_name")
    public String tagName;

    @SerializedName("name")
    public String name;

    @SerializedName("body")
    public String body; // Release notes/changelog

    @SerializedName("draft")
    public boolean draft;

    @SerializedName("prerelease")
    public boolean prerelease;

    @SerializedName("created_at")
    public String createdAt;

    @SerializedName("published_at")
    public String publishedAt;

    @SerializedName("assets")
    public List<GitHubAsset> assets;

    @SerializedName("html_url")
    public String htmlUrl;

    public static class GitHubAsset {
        @SerializedName("id")
        public long id;

        @SerializedName("name")
        public String name;

        @SerializedName("size")
        public long size;

        @SerializedName("download_count")
        public int downloadCount;

        @SerializedName("browser_download_url")
        public String browserDownloadUrl;

        @SerializedName("content_type")
        public String contentType;

        @SerializedName("created_at")
        public String createdAt;

        @SerializedName("updated_at")
        public String updatedAt;

        // Helper method to check if this is an APK
        public boolean isApk() {
            return name != null && name.toLowerCase().endsWith(".apk");
        }
    }

    // Helper method to get APK asset
    public GitHubAsset getApkAsset() {
        if (assets != null) {
            for (GitHubAsset asset : assets) {
                if (asset.isApk()) {
                    return asset;
                }
            }
        }
        return null;
    }

    // Helper method to extract version code from tag (e.g., "v1.2.0" -> extract version code)
    public int extractVersionCode() {
        try {
            // Assuming your tag format is like "v1.2.0" or "1.2.0"
            String version = tagName.replaceAll("[^0-9.]", "");
            String[] parts = version.split("\\.");
            if (parts.length >= 3) {
                // Convert semantic version to version code (e.g., 1.2.3 -> 10203)
                return Integer.parseInt(parts[0]) * 10000 +
                        Integer.parseInt(parts[1]) * 100 +
                        Integer.parseInt(parts[2]);
            } else if (parts.length == 2) {
                return Integer.parseInt(parts[0]) * 100 + Integer.parseInt(parts[1]);
            } else if (parts.length == 1) {
                return Integer.parseInt(parts[0]);
            }
        } catch (Exception e) {
            // Fallback: try to extract numbers from tag name
        }
        return 0;
    }
}
