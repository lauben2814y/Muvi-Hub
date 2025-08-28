package muvi.anime.hub.fcm;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import muvi.anime.hub.MainActivity;
import muvi.anime.hub.R;

public class MessagingService extends FirebaseMessagingService {
    private static final String TAG = "MyFirebaseMsgService";
    private static final String CHANNEL_ID = "media_notifications";
    private static final AtomicInteger notificationIdCounter = new AtomicInteger();

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }

        // Check if message contains data payload
        if (!remoteMessage.getData().isEmpty()) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            Map<String, String> data = remoteMessage.getData();

            String isForeground = data.get("notification_foreground");
            String notificationId = data.get("notification_id");
            String notificationType = data.get("notification_type");
            String notificationCategory = data.get("notification_category");

            // Handle based on if app is in foreground
            if ("true".equals(isForeground)) {
                // App in foreground - we can decide to show a notification or handle in-app
                handleNotification(remoteMessage);
            } else {
                // App in background or killed state - show notification
                handleNotification(remoteMessage);
            }
        }
    }

    private void handleNotification(RemoteMessage remoteMessage) {
        // Extract notification data
        String title = Objects.requireNonNull(remoteMessage.getNotification()).getTitle();
        String body = remoteMessage.getNotification().getBody();
        String imageUrl = null;

        // Extract image URL if available
        if (remoteMessage.getNotification() != null) {
            imageUrl = remoteMessage.getNotification().getImageUrl() != null
                    ? remoteMessage.getNotification().getImageUrl().toString()
                    : null;
        }

        // Extract data payload
        Map<String, String> data = remoteMessage.getData();
        String notificationId = data.get("notification_id");
        String notificationType = data.get("notification_type");
        String notificationCategory = data.get("notification_category");

        // Create notification channel for Android 8.0 and above
        createNotificationChannel();

        // Generate unique notification ID if not provided
        int notifId = (notificationId != null) ? Integer.parseInt(notificationId) : notificationIdCounter.getAndIncrement();

        Intent contentIntent = new Intent(this, MainActivity.class);

        contentIntent.putExtra("FROM_NOTIFICATION", true);
        contentIntent.putExtra("NOTIFICATION_ID", notificationId);
        contentIntent.putExtra("NOTIFICATION_TYPE", notificationType);
        contentIntent.putExtra("NOTIFICATION_CATEGORY", notificationCategory);

        // Set flags to create a new task and clear top
        contentIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Create pending intent
        PendingIntent pendingIntent = PendingIntent.getActivity(this, notifId, contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Create notification builder
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.logo)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

        // Add action buttons based on type
        if ("movie".equals(notificationType)) {
            // Add Watch Later action
            Intent watchLaterIntent = new Intent(this, NotificationActionReceiver.class);
            watchLaterIntent.setAction("WATCH_LATER_ACTION");
            watchLaterIntent.putExtra("NOTIFICATION_ID", notifId);
            watchLaterIntent.putExtra("MOVIE_ID", notificationId);
            PendingIntent watchLaterPendingIntent = PendingIntent.getBroadcast(
                    this, notifId + 100, watchLaterIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            notificationBuilder.addAction(
                    R.drawable.play_arrow_player,
                    "Watch Later",
                    watchLaterPendingIntent);
        }
        else if ("tv".equals(notificationType)) {
            // Add to Watchlist action
            Intent addWatchlistIntent = new Intent(this, NotificationActionReceiver.class);
            addWatchlistIntent.setAction("ADD_WATCHLIST_ACTION");
            addWatchlistIntent.putExtra("NOTIFICATION_ID", notifId);
            addWatchlistIntent.putExtra("TV_ID", notificationId);
            PendingIntent addWatchlistPendingIntent = PendingIntent.getBroadcast(
                    this, notifId + 200, addWatchlistIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            notificationBuilder.addAction(
                    R.drawable.bookmark_add_24px,
                    "Add to Watchlist",
                    addWatchlistPendingIntent);
        }

        // If there's an image, load it with Glide and then show the notification
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(getApplicationContext())
                    .asBitmap()
                    .load(imageUrl)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            notificationBuilder.setLargeIcon(resource)
                                    .setStyle(new NotificationCompat.BigPictureStyle()
                                            .bigPicture(resource));  // Remove the bigLargeIcon(null)

                            // Show notification with image
                            showNotification(notifId, notificationBuilder);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                            // Show notification without image in case of failure
                            showNotification(notifId, notificationBuilder);
                        }

                        @Override
                        public void onLoadFailed(@Nullable Drawable errorDrawable) {
                            // Show notification without image in case of failure
                            showNotification(notifId, notificationBuilder);
                        }
                    });
        }
        else {
            // Show notification without image
            showNotification(notifId, notificationBuilder);
        }
    }

    private void showNotification(int notificationId, NotificationCompat.Builder builder) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        try {
            notificationManager.notify(notificationId, builder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "No notification permission: " + e.getMessage());
        }
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ (Android 8.0) because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Media Notifications";
            String description = "Notifications about new movies and TV shows";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            // Register the channel with the system
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed token: " + token);

        // Send token to your server
        sendRegistrationToServer(token);
    }

    private void sendRegistrationToServer(String token) {
        // Implement code to send token to your server
        // This is where you would make an API call to your Node.js backend
        // to register this device token with a user
    }
}
