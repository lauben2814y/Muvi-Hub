package muvi.anime.hub;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import muvi.anime.hub.api.ReviewClient;
import muvi.anime.hub.api.ReviewService;
import muvi.anime.hub.managers.SyncDialog;
import muvi.anime.hub.managers.UserManager;
import muvi.anime.hub.models.User;
import muvi.anime.hub.pages.SignUp;
import muvi.anime.hub.storage.PreferenceHelper;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LauncherActivity extends AppCompatActivity {
    private boolean initialStatus;
    private final Context context = this;
    private UserManager userManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_launcher);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize firebase auth
        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        // Initialize PreferenceHelper
        PreferenceHelper preferenceHelper = new PreferenceHelper(this);
        initialStatus = preferenceHelper.getStatus();
        userManager = UserManager.getInstance(context);

        // Check if user is signed in and navigate accordingly
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            launchStarterActivity(currentUser);
        } else {
            startSignInActivity();
        }
    }

    private void startSignInActivity() {
        Intent intent = new Intent(this, SignUp.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToMain(FirebaseUser currentUser) {
        SyncDialog syncDialog = new SyncDialog(this);

        syncDialog.setOnRetryCallback(() -> {
            navigateToMain(currentUser);
            return null;
        });

        syncDialog.setOnDismissCallback(() -> {
            finish();
            return null;
        });

        syncDialog.show();
        syncDialog.setDialogPositionPercentage(0.25f);

        userManager.getOrCreateUser(currentUser, new UserManager.UserCallback() {
            @Override
            public void onSuccess(User user) {
                syncDialog.updateStatus(
                        SyncDialog.SyncStatus.SUCCESS,
                        "Welcome back, " + userManager.getUserEmail()
                );

                Log.d("Muvi-Hub", "Response successful");

                // Wait a bit to show success, then dismiss and navigate
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    syncDialog.dismiss();

                    Intent intent = new Intent(context, MainActivity.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0); // Remove animation
                    finish();

                }, 1000); // 1 second delay
            }

            @Override
            public void onError(String error) {
                syncDialog.updateStatus(
                        SyncDialog.SyncStatus.ERROR,
                        "Failed to sync with server, Check network !"
                );
            }
        });
    }

    private void checkEmail(FirebaseUser currentUser) {
        String email = currentUser.getEmail();

        if (email != null && !email.equalsIgnoreCase("fav2815@gmail.com") && email.toLowerCase().endsWith("@gmail.com")) {
            // ready to go
            navigateToMain(currentUser);
        } else {
            // still in review
            Intent intent = new Intent(context, MainActivity2.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    private void launchStarterActivity(FirebaseUser currentUser) {
        if (initialStatus) {
            navigateToMain(currentUser);
        } else {
            ReviewService reviewService = ReviewClient.getClient().create(ReviewService.class);
            Call<Boolean> call = reviewService.checkStatus();

            call.enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<Boolean> call, @NonNull Response<Boolean> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        boolean status = response.body();

                        if (!status) {
                            // still in review
                            Intent intent = new Intent(context, MainActivity2.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            checkEmail(currentUser);
                        }
                    } else {
                        Log.e("Retrofit", "Response Failed: " + response.code());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Boolean> call, @NonNull Throwable throwable) {

                }
            });
        }
    }
}