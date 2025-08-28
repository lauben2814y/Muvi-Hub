package muvi.anime.hub.api;

import android.content.Context;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class TMDBClient {
    private static Retrofit retrofit;
    private static final String BASE_URL = "https://api.themoviedb.org/3/";
    private static final String AUTHORIZATION_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiIzMDZjMzQ1MjY2MzBjNGQ5Y2I3ZjhhNjBiMjgzMzljMSIsIm5iZiI6MTczMTkxNjE4Ny43NzM2NzU0LCJzdWIiOiI2NTVmN2Q4NTJiMTEzZDAxMmQwMWJlYjIiLCJzY29wZXMiOlsiYXBpX3JlYWQiXSwidmVyc2lvbiI6MX0.6FFkm58cNLwFixmW45IUf-hBV-cHyniRwoj9m2886p4";

    private static Interceptor createAuthInterceptor() {
        return new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request original = chain.request();

                Request request = original.newBuilder()
                        .header("Authorization", "Bearer " + AUTHORIZATION_TOKEN)
                        .method(original.method(), original.body())
                        .build();
                return chain.proceed(request);
            }
        };
    }

    private static OkHttpClient createOkHttpClient() {
        return new OkHttpClient.Builder()
                .addInterceptor(createAuthInterceptor())
                .build();
    }

    public static synchronized Retrofit getRetrofitInstance(Context context) {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(createOkHttpClient())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static TMDBApi getApi(Context context) {
        return getRetrofitInstance(context).create(TMDBApi.class);
    }
}
