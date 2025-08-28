package muvi.anime.hub.fcm;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessaging;

public class FCMManager {
    private static final String TAG = "FCMManager";

    public static void initialize() {
        // Get current FCM token
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    // Get new FCM registration token
                    String token = task.getResult();
                    Log.d(TAG, "FCM Token: " + token);

                    // Send token to your server
                    sendRegistrationToServer(token);

                    // Subscribe to topics
                    subscribeToTopics();
                });
    }

    private static void sendRegistrationToServer(String token) {
        // TODO: Implement API call to your server to register this token
    }

    private static void subscribeToTopics() {
        // Subscribe to the "latest" topic and any other topics you need
        FirebaseMessaging.getInstance().subscribeToTopic("latest")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Subscribed to topic: latest");
                    } else {
                        Log.e(TAG, "Failed to subscribe to topic: latest", task.getException());
                    }
                });
    }

    public static void subscribeTopic(String topic) {
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Subscribed to topic: " + topic);
                    } else {
                        Log.e(TAG, "Failed to subscribe to topic: " + topic, task.getException());
                    }
                });
    }

    // Method to unsubscribe from a specific topic
    public static void unsubscribeTopic(String topic) {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Unsubscribed from topic: " + topic);
                    } else {
                        Log.e(TAG, "Failed to unsubscribe from topic: " + topic, task.getException());
                    }
                });
    }

}
