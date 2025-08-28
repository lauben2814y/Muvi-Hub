package muvi.anime.hub.api;

import muvi.anime.hub.models.GitHubRelease;
import muvi.anime.hub.models.UpdateResponse;
import muvi.anime.hub.models.UpdateStatsResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface UpdateApiService {

    @GET("api/v1/update/check")
    Call<UpdateResponse> checkForUpdate(
            @Query("package_name") String packageName,
            @Query("current_version") int currentVersionCode,
            @Query("platform") String platform
    );

    @GET("api/v1/update/info/{versionCode}")
    Call<UpdateResponse> getUpdateInfo(@Path("versionCode") int versionCode);

    @GET("api/v1/update/stats")
    Call<UpdateStatsResponse> getStats();

    // GitHub API integration
    @GET("repos/{owner}/{repo}/releases/latest")
    Call<GitHubRelease> getLatestRelease(
            @Path("owner") String owner,
            @Path("repo") String repo,
            @Header("Accept") String acceptHeader
    );

    @GET("repos/{owner}/{repo}/releases")
    Call<java.util.List<GitHubRelease>> getAllReleases(
            @Path("owner") String owner,
            @Path("repo") String repo,
            @Header("Accept") String acceptHeader
    );
}
