package muvi.anime.hub;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


import java.util.Objects;

import muvi.anime.hub.api.SecureClient;
import muvi.anime.hub.api.SecureService;
import muvi.anime.hub.api.TMDBApi;
import muvi.anime.hub.api.TMDBClient;
import muvi.anime.hub.data.Utils;
import muvi.anime.hub.data.movie.SupabaseMovie;
import muvi.anime.hub.data.movie.TMDBMovieDetails;
import muvi.anime.hub.data.tv.SupabaseTv;
import muvi.anime.hub.data.tv.TMDBTv;
import muvi.anime.hub.fcm.FCMManager;
import muvi.anime.hub.managers.AdManagerCoordinator;
import muvi.anime.hub.managers.BannerAdManager;
import muvi.anime.hub.managers.NavigationManager;
import muvi.anime.hub.managers.DownloadService;
import muvi.anime.hub.managers.SyncDialog;
import muvi.anime.hub.managers.UserManager;
import muvi.anime.hub.models.User;
import muvi.anime.hub.pages.DownloadsFragment;
import muvi.anime.hub.pages.Library;
import muvi.anime.hub.pages.LogIn;
import muvi.anime.hub.pages.MovieDetails;
import muvi.anime.hub.pages.MoviesFragment;
import muvi.anime.hub.pages.ShortsFragment;
import muvi.anime.hub.pages.TvDetails;
import muvi.anime.hub.pages.TvShowsFragment;
import muvi.anime.hub.storage.PreferenceHelper;
import muvi.anime.hub.storage.UserStatsManager;
import muvi.anime.hub.ui.FullScreenPreloader;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private final Context context = this;
    private AppUpdateManager appUpdateManager;
    private static final int UPDATE_REQUEST_CODE = 100;
    private DrawerLayout drawerLayout;
    private SecureService movieService;
    private SecureService tvService;
    private TMDBApi tmdbApi;
    private FullScreenPreloader preloader;
    private NavigationManager navigationManager;
    private ImageView profileImage;

    private UserManager userManager;
    private UserStatsManager statsManager;
    private NavigationView navigationView;
    private TextView userNameText, userEmailText, coinAmount, downloadAmount, watchAmount;
    private static final int NOTIFICATION_PERMISSION_CODE = 1001;
    private boolean doubleBackToExitPressedOnce = false;
    private final Handler handler = new Handler();
    private static final int AD_CHECK_INTERVAL = 2000;
    private FrameLayout bannerAdContainer;

    private BannerAdManager bannerAdManager;
    private Handler adCheckHandler;
    private Runnable adCheckRunnable;
    private boolean isActivityVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, 0);
            return insets;
        });
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bannerAdContainer = findViewById(R.id.banner_ad_container);
        tmdbApi = TMDBClient.getApi(this);
        movieService = SecureClient.getApi(this);
        tvService = SecureClient.getApi(this);
        preloader = new FullScreenPreloader(this);
        navigationManager = NavigationManager.getInstance(context);
        statsManager = new UserStatsManager(context);
        userManager = UserManager.getInstance(this);

        // Initialize the coordinator which handles consent and ad loading
        AdManagerCoordinator adManagerCoordinator = new AdManagerCoordinator(this);
        adManagerCoordinator.initialize(this); // Pass activity context here
        // initializeUserManager();

        // request for post notifications
        requestNotificationPermission();

        // handle back pressed
        handleOnBackPressed();

        // Handle Notification Click
        if (savedInstanceState == null) {
            handleNotificationClick(getIntent());
        }

        // save status
        PreferenceHelper preferenceHelper = new PreferenceHelper(this);
        preferenceHelper.saveStatus(true);

        // Side navigation
        setUpSidePanel(savedInstanceState);

        // Handle deep links
        // handleIntent(getIntent());

        // Show banner
        bannerAdManager = BannerAdManager.getInstance(this);
        setupAdCheckHandler();

        // Firebase
        FCMManager.initialize();

        // Initialize auth and load profile
        View header = navigationView.getHeaderView(0);
        profileImage = header.findViewById(R.id.profile_image);
        userNameText = header.findViewById(R.id.userName);
        userEmailText = header.findViewById(R.id.userEmail);
        coinAmount = header.findViewById(R.id.coin_amount);
        watchAmount = header.findViewById(R.id.watch_amount);
        downloadAmount = header.findViewById(R.id.download_amount);

        // Initialize the AppUpdateManager with the new library
        appUpdateManager = AppUpdateManagerFactory.create(getApplicationContext());

        checkForUpdates();

        toggleSections(this, bottomNavigationView);
    }

    private void initializeUserManager() {
        User currentUser = userManager.getCurrentUser();
        if (currentUser == null) {
            SyncDialog syncDialog = new SyncDialog(this);

            syncDialog.setOnRetryCallback(() -> {
                initializeUserManager();
                return null;
            });

            syncDialog.show();
            syncDialog.setDialogPositionPercentage(0.25f); // 25% from top

            userManager.getOrCreateUser(mAuth.getCurrentUser(), new UserManager.UserCallback() {
                @Override
                public void onSuccess(User user) {
                    syncDialog.updateStatus(
                            SyncDialog.SyncStatus.SUCCESS,
                            "User sync success, " + userManager.getUserEmail()
                    );

                    // Wait a bit to show success, then dismiss and navigate
                    // do something after success
                    new Handler(Looper.getMainLooper()).postDelayed(syncDialog::dismiss, 2000); // 2 second delay
                }

                @Override
                public void onError(String error) {

                }
            });
        }
    }

    private void setupAdCheckHandler() {
        adCheckHandler = new Handler();
        adCheckRunnable = new Runnable() {
            @Override
            public void run() {
                if (isActivityVisible) {
                    showBannerAd();

                    // If ad is not loaded yet, schedule another check
                    if (!bannerAdManager.isAdLoaded()) {
                        adCheckHandler.postDelayed(this, AD_CHECK_INTERVAL);
                    } else {
                        Log.d("W&Q", "Ad is now loaded and displayed");
                    }
                }
            }
        };
    }

    private void showBannerAd() {
        bannerAdManager.displayBannerAd(bannerAdContainer, new BannerAdManager.OnBannerAdCallback() {
            @Override
            public void onBannerAdAvailable() {
                // Ad is available and displayed
                Log.d("YourActivity", "Banner ad is now showing");

                // Stop checking since ad is now available
                adCheckHandler.removeCallbacks(adCheckRunnable);

                // Maybe update UI to accommodate the ad
                // For example, adjust margins or padding of other elements
            }

            @Override
            public void onBannerAdNotAvailable() {
                // Ad is not available yet
                Log.d("YourActivity", "Banner ad is not available, will check again");

                // You might want to show a placeholder or leave space for the ad
                // Alternatively, you can collapse the space until the ad is available
            }

            @Override
            public void onBannerAdClicked() {
                Log.d("YourActivity", "Banner ad was clicked");
            }

            @Override
            public void onBannerAdClosed() {
                Log.d("YourActivity", "Banner ad was closed");
            }
        });
    }

    private void handleOnBackPressed() {
        // Handle back press using OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (doubleBackToExitPressedOnce) {
                    finish(); // Exit the app
                } else {
                    doubleBackToExitPressedOnce = true;
                    Toast.makeText(MainActivity.this, "Press back again to exit", Toast.LENGTH_SHORT).show();

                    // Reset the flag after 2 seconds
                    handler.postDelayed(() -> doubleBackToExitPressedOnce = false, 2000);
                }
            }
        });
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    private void toggleSections(MainActivity mainActivity, BottomNavigationView bottomNavigationView) {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment fragmentSelected = null;
            int itemId = item.getItemId();
            if (itemId == R.id.moviesBtn) {
                fragmentSelected = new MoviesFragment();
            } else if (itemId == R.id.tvShowsBtn) {
                fragmentSelected = new TvShowsFragment();
            } else if (itemId == R.id.downloadsBtn) {
                fragmentSelected = new DownloadsFragment();
            } else if (itemId == R.id.shortsBtn) {
                fragmentSelected = new ShortsFragment();
            }
            if (fragmentSelected != null) {
                mainActivity.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainerView, fragmentSelected)
                        .commit();
                return true;
            }
            return false;
        });

        // Only set default selected item, don't manually add the fragment
        bottomNavigationView.setSelectedItemId(R.id.moviesBtn);
    }

    private void updateProfileStats(FirebaseUser currentUser) {
        // Update stats first (these should always work)
        coinAmount.setText(String.valueOf(statsManager.getCoins()));
        downloadAmount.setText(String.valueOf(statsManager.getDownloads()));
        watchAmount.setText(String.valueOf(statsManager.getMoviesWatched()));

        if (currentUser != null) {
            // Check if UserManager has current user data
            userManager.getOrCreateUser(currentUser, new UserManager.UserCallback() {
                @Override
                public void onSuccess(User user) {
                    if (user.getProfile_url() != null && !user.getProfile_url().isEmpty()) {
                        Glide.with(MainActivity.this)
                                .load(user.getProfile_url())
                                .into(profileImage);
                    } else {
                        Glide.with(MainActivity.this)
                                .load("https://cdn.framework7.io/placeholder/cats-300x300-1.jpg")
                                .into(profileImage);
                    }

                    // Update username
                    String email = currentUser.getEmail();
                    String displayName = currentUser.getDisplayName();

                    if (user.getUser_name() != null && !Objects.requireNonNull(displayName).isEmpty()) {
                        userNameText.setText(displayName);
                    } else {
                        userNameText.setText(email != null ? email.split("@")[0] : "User");
                    }

                    // Update email
                    if (email != null && !email.isEmpty()) {
                        userEmailText.setText(email);
                    } else {
                        userEmailText.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onError(String error) {

                }
            });
        }
    }

    private void navigateToMovieDetails(TMDBMovieDetails movieDetails, SupabaseMovie supabaseMovie) {
        Intent intent = new Intent(context, MovieDetails.class);
        intent.putExtra("supabasemovie", supabaseMovie);
        intent.putExtra("tmdbdetails", movieDetails);

        // show ad and navigate
        this.startActivity(intent);
    }

    private void fetchMovieAndNavigate(String id) {
        preloader.show();

        Call<SupabaseMovie> supabaseMovieCall = movieService.findMovie(
                id,
                Utils.getMovieFields()
        );
        supabaseMovieCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<SupabaseMovie> call, @NonNull Response<SupabaseMovie> response) {
                if (response.isSuccessful() && response.body() != null) {
                    SupabaseMovie supabaseMovie = response.body();

                    Log.e(Utils.getTag(), supabaseMovie.getTitle());
                    // get tmdb details

                    Call<TMDBMovieDetails> tmdbMovieDetailsCall = tmdbApi.getMovieDetails(Integer.parseInt(id), "images,videos,credits");
                    tmdbMovieDetailsCall.enqueue(new Callback<>() {
                        @Override
                        public void onResponse(@NonNull Call<TMDBMovieDetails> call, @NonNull Response<TMDBMovieDetails> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                TMDBMovieDetails tmdbMovieDetails = response.body();
                                Log.e(Utils.getTag(), tmdbMovieDetails.getOverview());
                                navigateToMovieDetails(tmdbMovieDetails, supabaseMovie);
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<TMDBMovieDetails> call, @NonNull Throwable throwable) {
                            preloader.dismiss();
                            Log.e(Utils.getTag(), Objects.requireNonNull(throwable.getMessage()));
                            Log.e(Utils.getTag(), "Response " + response);
                        }
                    });
                }
            }

            @Override
            public void onFailure(@NonNull Call<SupabaseMovie> call, @NonNull Throwable throwable) {

            }
        });
    }

    private void fetchTvAndNavigate(String id) {
        preloader.show();

        Call<SupabaseTv> supabaseTvCall = tvService.findTv(
                Utils.getTvFields(),
                id
        );

        supabaseTvCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<SupabaseTv> call, @NonNull Response<SupabaseTv> response) {
                if (response.isSuccessful() && response.body() != null) {
                    SupabaseTv supabaseTv = response.body();

                    Call<TMDBTv> tmdbTvDetailsCall = tmdbApi.getTvDetails(Integer.parseInt(id), "images,videos,credits");

                    tmdbTvDetailsCall.enqueue(new Callback<>() {
                        @Override
                        public void onResponse(@NonNull Call<TMDBTv> call, @NonNull Response<TMDBTv> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                TMDBTv tmdbTv = response.body();
                                navigateToTvDetails(supabaseTv, tmdbTv);
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<TMDBTv> call, @NonNull Throwable throwable) {

                        }
                    });
                }
            }

            @Override
            public void onFailure(@NonNull Call<SupabaseTv> call, @NonNull Throwable throwable) {

            }
        });
    }

    private void navigateToTvDetails(SupabaseTv supabaseTv, TMDBTv tmdbTv) {
        Intent intent = new Intent(context, TvDetails.class);
        intent.putExtra("supabasetv", supabaseTv);
        intent.putExtra("tmdbdetails", tmdbTv);

        // show ad ad navigate
        navigationManager.navigateWithAd(this, intent, preloader);
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        Uri data = intent.getData();

        if (Intent.ACTION_VIEW.equals(action) && data != null) {
            String path = data.getPath();
            if (path != null) {
                // Remove the leading "/media/" from the path
                path = path.replaceFirst("^/media/", "");

                // Split the remaining path into segments
                String[] segments = path.split("/");

                if (segments.length >= 2) {
                    String type = segments[0];    // "movie"
                    String id = segments[1];      // "343"

                    // Log the parsed data
                    Log.d(Utils.getTag(), "Type: " + type + ", ID: " + id);

                    // Handle different types of media
                    if (type.equals("movie")) {
                        fetchMovieAndNavigate(id);
                    } else if (type.equals("tv")) {
                        fetchTvAndNavigate(id);
                    }
                }
            }
        }
    }

    // Method to open the drawer from fragments
    public void openDrawer() {
        if (drawerLayout != null) {
            updateProfileStats(mAuth.getCurrentUser());
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }

    public void setUpSidePanel(Bundle savedInstanceState) {
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            boolean handled = false;

            if (itemId == R.id.group_btn) {
                // Handle Telegram intent (no fragment creation)
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://t.me/muvi_telegram_te"));
                intent.setPackage("org.telegram.messenger");

                try {
                    startActivity(intent);
                    handled = true;
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(this, "Telegram is not installed.", Toast.LENGTH_SHORT).show();
                }
            } else if (itemId == R.id.library_btn) {
                item.setChecked(true);
                Intent intent = new Intent(context, Library.class);
                this.startActivity(intent);
                handled = true;
            }

            if (handled) {
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
            return false;
        });
    }

    private void handleNotificationClick(Intent intent) {
        if (intent != null && intent.getBooleanExtra("FROM_NOTIFICATION", false)) {
            String notificationId = intent.getStringExtra("NOTIFICATION_ID");
            String notificationType = intent.getStringExtra("NOTIFICATION_TYPE");

            Log.d(Utils.getTag(), "Received notification: type=" + notificationType + ", id=" + notificationId);

            if (notificationId != null && notificationType != null) {
                // Process based on notification type
                if ("movie".equals(notificationType)) {
                    fetchMovieAndNavigate(notificationId);
                } else if ("tv".equals(notificationType)) {
                    fetchTvAndNavigate(notificationId);
                }
            }
        }
    }

    // Optional: Method to handle sign out
    private void signOut() {
        mAuth.signOut();
        startActivity(new Intent(this, LogIn.class));
        finish();
    }

    // ======================================= IN APP UPDATE ====================================== //

    private void checkForUpdates() {

    }

    // ======================================= IN APP UPDATE ======================================= //

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        handleNotificationClick(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        isActivityVisible = true;
        showBannerAd();
        if (!bannerAdManager.isAdLoaded()) {
            adCheckHandler.postDelayed(adCheckRunnable, AD_CHECK_INTERVAL);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // If not signed in, redirect to SignIn activity
            startActivity(new Intent(this, LogIn.class));
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        if (isFinishing()) {
            stopDownloadService();
        }

        // Clean up the handler to prevent memory leaks
        if (adCheckHandler != null) {
            adCheckHandler.removeCallbacks(adCheckRunnable);
        }

        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        isActivityVisible = false;

        // Stop periodic checks when activity is not visible
        adCheckHandler.removeCallbacks(adCheckRunnable);
    }

    private void stopDownloadService() {
        if (DownloadService.isServiceRunning) {
            Intent serviceIntent = new Intent(this, DownloadService.class);
            serviceIntent.setAction(DownloadService.ACTION_STOP_SERVICE);

            // Use the appropriate method based on Android version
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        }
    }

    private void openAppSettings() {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void showGoToSettingsDialog() {
        new MaterialAlertDialogBuilder(context)
                .setTitle("Enable Notifications")
                .setMessage("To receive notifications, please enable the permission in Settings.")
                .setPositiveButton("OK", (dialog, which) -> openAppSettings())
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context, "Permission granted successfully u will receive notifications", Toast.LENGTH_SHORT).show();
            } else {
                // Permission denied, handle accordingly
                boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS);

                if (!showRationale) {
                    showGoToSettingsDialog();
                } else {
                    Toast.makeText(this, "Notifications permission denied!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}