package muvi.anime.hub.api;

import java.util.List;

import muvi.anime.hub.data.Trailer;
import muvi.anime.hub.data.movie.SupabaseMovie;
import muvi.anime.hub.data.tv.SupabaseTv;
import muvi.anime.hub.models.MovieShort;
import muvi.anime.hub.models.User;
import muvi.anime.hub.models.CommentShortRequest;
import muvi.anime.hub.models.CommentShortResponse;
import muvi.anime.hub.models.LikeShortRequest;
import muvi.anime.hub.models.LikeShortResponse;
import muvi.anime.hub.models.ProfileImageResponse;
import muvi.anime.hub.models.UpdateProfileRequest;
import muvi.anime.hub.models.UserRequest;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface SecureService {

    @GET("trailers")
    Call<List<Trailer>> getTrailers(
            @Query("page") int page,
            @Query("limit") int limit,
            @Query("select") String select,
            @Query("order") String order
    );

//    MOVIE ENDPOINTS

    @GET("movies/filtered")
    Call<List<SupabaseMovie>> getFilteredMovies(
            @Query("page") int page,
            @Query("limit") int limit,
            @Query("select") String select,
            @Query("order") String order,
            @Query("genres") String genres,
            @Query("origin_countries") String originCountry,
            @Query("vjs") String vjs
    );

    @GET("movies/collection")
    Call <List<SupabaseMovie>> getCollectionMovies(
            @Query("collection_id") String collectionId,
            @Query("select") String select,
            @Query("order") String order
    );

    @GET("movies/related")
    Call<List<SupabaseMovie>> getRelatedMovies(
            @Query("page") int page,
            @Query("limit") int limit,
            @Query("select") String select,
            @Query("genres") String genres,
            @Query("order") String order
    );

    @GET("movies/search")
    Call<List<SupabaseMovie>> searchMovies(
            @Query("select") String select,
            @Query("title") String title
    );

    @GET("movies/find/{id}")
    Call<SupabaseMovie> findMovie(
            @Path("id") String movieId,
            @Query("select") String select
    );

    @GET("shorts")
    Call<List<MovieShort>> getShorts(
            @Query("page") int page,
            @Query("limit") int limit,
            @Query("select") String select
    );

    @POST("shorts/like")
    Call<LikeShortResponse> likeShort(@Body LikeShortRequest request);

    @POST("shorts/comment")
    Call<CommentShortResponse> commentShort(@Body CommentShortRequest request);

    // User management
    @POST("user/update-profile")
    Call<User> updateUserProfile(@Body UpdateProfileRequest request);

    @POST("user/get-or-create")
    Call<User> getInsertUser(@Body UserRequest request);

    @Multipart
    @POST("storage/upload/{bucketName}/{path}")
    Call<ProfileImageResponse> uploadFile(
            @Path("bucketName") String bucketName,
            @Path("path") String path,
            @Part MultipartBody.Part file
    );

//    TV ENDPOINTS

    @GET("tvs/filtered")
    Call<List<SupabaseTv>> getFilteredTvs(
            @Query("page") int page,
            @Query("limit") int limit,
            @Query("select") String select,
            @Query("order") String order,
            @Query("genres") String genres,
            @Query("origin_countries") String originCountry,
            @Query("vjs") String vjs
    );

    @GET("tvs/search")
    Call<List<SupabaseTv>> searchTvs(
            @Query("select") String select, // select columns
            @Query("name") String name // Case-insensitive search (ILIKE)
    );

    @GET("tvs/find/{id}")
    Call<SupabaseTv> findTv(
            @Query("id") String idFilter, // Exact ID match
            @Query("select") String select
    );

    @GET("tvs/related")
    Call<List<SupabaseTv>> getRelatedTvs(
            @Query("page") int page,
            @Query("limit") int limit,
            @Query("select") String select,
            @Query("genres") String genres,
            @Query("order") String order
    );

    @GET("tvs/trailers")
    Call<List<Trailer>> getTvTrailers(
            @Query("page") int page,
            @Query("limit") int limit,
            @Query("select") String select,
            @Query("order") String order
    );
}