package muvi.anime.hub.managers.update;

public class UpdateConfig {
    // Your backend configuration
    public static final String BACKEND_URL = "https://muvihub-update.heroware.xyz/";
    public static final String GITHUB_OWNER = "lauben2814y";
    public static final String GITHUB_REPO = "Muvi-Hub";

    // Package name (should match your app)
    public static final String PACKAGE_NAME = "muvi.anime.hub";

    // Update check intervals
    public static final long CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000; // 24 hours
    public static final long MIN_CHECK_INTERVAL_MS = 4 * 60 * 60 * 1000; // 4 hours minimum

    // File provider authority (matches AndroidManifest.xml)
    public static final String FILE_PROVIDER_AUTHORITY = PACKAGE_NAME + ".fileprovider";

    // Debug settings
    public static final boolean DEBUG_UPDATES = true; // Set to false for production

    // User-Agent for API calls
    public static final String USER_AGENT = "MuviHub-Android/1.0";
}
