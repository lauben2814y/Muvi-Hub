package muvi.anime.hub.api.kotlin

import retrofit2.http.GET

interface ApiService {
    @GET("supabase/ultra17/review")
    suspend fun checkStatus(): Boolean
}