package muvi.anime.hub.api;

import retrofit2.Call;
import retrofit2.http.GET;

public interface ReviewService {
    @GET("supabase/ultra17/review")
    Call<Boolean> checkStatus();
}
