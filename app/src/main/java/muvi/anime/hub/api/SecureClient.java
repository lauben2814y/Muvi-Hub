package muvi.anime.hub.api;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import muvi.anime.hub.managers.AppSignature;
import muvi.anime.hub.managers.DeviceFingerprint;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

// SecureClient.java - Complete client with dual security

public class SecureClient {
    private static final String BASE_URL = "https://muvihub-server.heroware.xyz/api/"; // https://drama.iqube.sbs/api/
    private static Retrofit retrofit;
    private static String deviceFingerprint;

    public static Retrofit getInstance(Context context) {
        if (retrofit == null) {
            deviceFingerprint = DeviceFingerprint.generateFingerprint(context);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new SecurityInterceptor(context))
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static SecureService getApi(Context context) {
        return getInstance(context).create(SecureService.class);
    }

    private record SecurityInterceptor(Context context) implements Interceptor {
        @NonNull
        @Override
        public Response intercept(@NonNull Chain chain) throws IOException {
            // Perform security checks
            if (DeviceFingerprint.isAppTampered(context)) {
                throw new IOException("App integrity check failed");
            }

            if (DeviceFingerprint.isDeviceRooted()) {
                Log.w("SECURITY", "Device is rooted - potential security risk");
                // You can choose to block rooted devices or just log
                // throw new IOException("Rooted device detected");
            }

            if (DeviceFingerprint.isDebuggable(context)) {
                Log.w("SECURITY", "App is debuggable - potential security risk");
            }

            Request originalRequest = chain.request();
            Request.Builder requestBuilder = originalRequest.newBuilder();

            // Add security headers
            requestBuilder.addHeader("X-Device-Fingerprint", deviceFingerprint);
            requestBuilder.addHeader("X-App-Signature", AppSignature.generateDynamicSignature(context));
            requestBuilder.addHeader("X-Timestamp", String.valueOf(System.currentTimeMillis()));
            requestBuilder.addHeader("X-Package-Name", context.getPackageName());
            requestBuilder.addHeader("Content-Type", "application/json");

            Log.d("SECURITY", "Added security headers to request");

            return chain.proceed(requestBuilder.build());
        }
    }
}
