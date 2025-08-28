package muvi.anime.hub;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;

import muvi.anime.hub.api.SecureClient;

public class MuviHub extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
    }
}
