package muvi.anime.hub.pages;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.tonyodev.fetch2.Fetch;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import muvi.anime.hub.managers.DownloadManager;
import muvi.anime.hub.managers.FetchSingleton;
import muvi.anime.hub.managers.NavigationManager;
import muvi.anime.hub.R;
import muvi.anime.hub.managers.RewardedAdManager;
import muvi.anime.hub.api.TMDBApi;
import muvi.anime.hub.api.TMDBClient;
import muvi.anime.hub.data.Utils;
import muvi.anime.hub.data.movie.SupabaseMovie;
import muvi.anime.hub.data.movie.TMDBMovieDetails;
import muvi.anime.hub.adapters.movie.DetailsPagerAdapter;
import muvi.anime.hub.managers.UserManager;
import muvi.anime.hub.player.VideoMetaData;
import muvi.anime.hub.storage.MovieStorage;
import muvi.anime.hub.tabs.movie.cast;
import muvi.anime.hub.tabs.movie.collection;
import muvi.anime.hub.tabs.movie.overview;
import muvi.anime.hub.tabs.movie.related;
import muvi.anime.hub.adapters.movie.CollectionAdapter;
import muvi.anime.hub.ui.FullScreenPreloader;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MovieDetails extends AppCompatActivity implements CollectionAdapter.onCollectionPosterClickedListener {
    private UserManager userManager;

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private DetailsPagerAdapter detailsPagerAdapter;
    private FullScreenPreloader preloader;
    private SupabaseMovie supabaseMovie;
    private TMDBMovieDetails tmdbMovieDetails;
    private final Context context = this;
    private MaterialButton downloadButton;
    private Fetch fetch;
    private DownloadManager downloadManager;
    private Button watchButton;
    private MovieStorage movieStorage;

    private RewardedAdManager rewardedAdManager;
    private static final String TAG = Utils.getTag();
    private NavigationManager navigationManager;
    private ImageButton wishListBtn, shareBtn, reportBtn, trailerBtn;

    private TextView wishListTxt, reportTxt, movieTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_movie_details);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.movieDetailsMain), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, 0);
            return insets;
        });

        ImageView headerImage = findViewById(R.id.movieDetailsHeaderImg);
        ImageView posterImage = findViewById(R.id.movieDetailsPoster);
        ImageView logoImage = findViewById(R.id.movieDetailsLogo);
        TextView genres = findViewById(R.id.movieDetailsGenres);
        TextView vote = findViewById(R.id.movieDetailsVote);
        TextView date = findViewById(R.id.movieDetailsDate);
        TextView runtime = findViewById(R.id.movieDetailsRuntime);
        tabLayout = findViewById(R.id.movieDetailsTabLayout);
        viewPager = findViewById(R.id.movieDetailsPager);
        detailsPagerAdapter = new DetailsPagerAdapter(this);
        viewPager.setAdapter(detailsPagerAdapter);
        downloadButton = findViewById(R.id.downloadButton);
        watchButton = findViewById(R.id.watchButton);
        movieTitle = findViewById(R.id.movieDetailsName);

        // Action Buttons
        wishListBtn = findViewById(R.id.wishlistBtnImg);
        shareBtn = findViewById(R.id.shareBtnImg);
        reportBtn = findViewById(R.id.reportBtnImg);
        trailerBtn = findViewById(R.id.trailerBtnImg);

        wishListTxt = findViewById(R.id.wishlistTxt);
        reportTxt = findViewById(R.id.reportTxt);

        // Interstitial and Rewarded Ad Config
        navigationManager = NavigationManager.getInstance(context);
        rewardedAdManager = RewardedAdManager.getInstance(context);

        // User Config
        userManager = UserManager.getInstance(this);

        // Download, Fetch and Storage config ...
        fetch = FetchSingleton.getFetchInstance(context);
        downloadManager = new DownloadManager(this, fetch, context);
        movieStorage = new MovieStorage(this);

        supabaseMovie = (SupabaseMovie) getIntent().getSerializableExtra("supabasemovie");

        tmdbMovieDetails = (TMDBMovieDetails) getIntent().getSerializableExtra("tmdbdetails");

        if (supabaseMovie != null && tmdbMovieDetails != null) {
            loadImage(tmdbMovieDetails.getBackdrop_path(), headerImage, false);

            loadImage(supabaseMovie.getPoster_path(), posterImage, true);

            if (supabaseMovie.getLogo_path() != null) {
                loadImage(supabaseMovie.getLogo_path(), logoImage, false);
            } else {
                logoImage.setVisibility(View.GONE);
                movieTitle.setVisibility(View.VISIBLE);
                movieTitle.setText(supabaseMovie.getTitle());
            }

            genres.setText(String.join(", ", supabaseMovie.getGenres()));
            vote.setText(String.valueOf(supabaseMovie.getFormattedVote(tmdbMovieDetails.getVote_average())));
            date.setText(supabaseMovie.getYearFromDate(supabaseMovie.getRelease_date()));
            runtime.setText(supabaseMovie.formatRuntime(tmdbMovieDetails.getRuntime()));

            // handle file size determination
            setUpFileSizeDetermination();

            // Handle action buttons
            handleActionButtons();

            if (tmdbMovieDetails != null) {
                setUpTabs(tmdbMovieDetails.getBelongs_to_collection() != null, tmdbMovieDetails, supabaseMovie);
            }

            // downloadOptions
            downloadButton.setOnClickListener(view -> {
                downloadOptions();
            });

            // stream options
            watchButton.setOnClickListener(view -> streamOptions());
        }
    }

    @SuppressLint("DefaultLocale")
    private String formatFileSize(long sizeInBytes) {
        if (sizeInBytes < 0) {
            return "";
        }

        double sizeInKB = sizeInBytes / 1024.0;
        double sizeInMB = sizeInKB / 1024.0;
        double sizeInGB = sizeInMB / 1024.0;

        if (sizeInGB >= 1) {
            return String.format("%.2f GB", sizeInGB);
        } else if (sizeInMB >= 1) {
            return String.format("%.2f MB", sizeInMB);
        } else {
            return String.format("%.2f KB", sizeInKB);
        }
    }

    private void showDownloadSizeLoading() {
        downloadButton.setText("");

        // Create and show the CircularProgressIndicator
        CircularProgressIndicator progressIndicator = new CircularProgressIndicator(this);
        progressIndicator.setIndicatorSize(56);
        progressIndicator.setIndicatorColor(getResources().getColor(R.color.md_theme_onPrimary));
        progressIndicator.setTrackColor(Color.TRANSPARENT);

        // Add the indicator to the button
        downloadButton.setIcon(progressIndicator.getIndeterminateDrawable());
        downloadButton.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
        downloadButton.setIconPadding(0);
        downloadButton.setIconTint(null);
    }

    private long getFileSize(String videoUrl) {
        long fileSize = -1;
        try {
            URL url = new URL(videoUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Add Basic Authentication header
            String credentials = userManager.getCurrentUser()
                    .getMedia_user_name() + ":" +
                    userManager.getCurrentUser().getMedia_password();
            String basicAuth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
            connection.setRequestProperty("Authorization", basicAuth);

            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.connect();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                fileSize = connection.getContentLengthLong();
            }
            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fileSize;
    }

    @SuppressLint("DefaultLocale")
    private void setUpFileSizeDetermination() {
        if (supabaseMovie.getTranslated() != null) {
            showDownloadSizeLoading();

            new Thread(() -> {
                long fileSize = getFileSize(supabaseMovie.getTranslated());

                new Handler(Looper.getMainLooper()).post(() -> {
                    downloadButton.setIcon(null);

                    if (fileSize > 0) {
                        String formattedSize = formatFileSize(fileSize);
                        downloadButton.setText(formattedSize);
                    } else {
                        downloadButton.setText(R.string.download);
                    }
                });
            }).start();
        }
    }

    private void handleActionButtons() {
        // WishListBtn
        boolean wishListed = movieStorage.movieExists(supabaseMovie.getId());
        if (wishListed) {
            wishListBtn.setImageResource(R.drawable.bookmark_added_24px);
            wishListTxt.setText("Added");
        } else {
            wishListBtn.setOnClickListener(view -> {
                movieStorage.addMovie(supabaseMovie);
                wishListBtn.setImageResource(R.drawable.bookmark_added_24px);
                wishListTxt.setText("Added");
            });
        }

        // Share Btn
        shareBtn.setOnClickListener(view -> {
            shareMovieURL("https://movieapp-a6a2a.web.app/media/movie/" + supabaseMovie.getId());
        });

        // Report btn
        reportBtn.setOnClickListener(view -> {

        });

        // TrailerDetails Btn
    }

    private void shareMovieURL(String url) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out " +
                supabaseMovie.getTitle() + ":\n" + url);

        // Show chooser dialog
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }

    private void loadImage(String url, ImageView imageView, boolean centerCrop) {
        DrawableCrossFadeFactory fadeFactory = new DrawableCrossFadeFactory.Builder(500)
                .setCrossFadeEnabled(true)
                .build();

        Glide.with(this)
                .load(url)
                .transition(DrawableTransitionOptions.withCrossFade(fadeFactory))
                .apply(centerCrop ? RequestOptions.centerCropTransform() : RequestOptions.noTransformation())
                .into(imageView);
    }

    public static String getFileExtension(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        int lastIndexOfDot = url.lastIndexOf('.');
        int lastIndexOfSlash = url.lastIndexOf('/');

        // Ensure that the dot is after the last slash to avoid confusion with URLs like "www.example.com/index.html?file=test.mp4"
        if (lastIndexOfDot > lastIndexOfSlash && lastIndexOfDot != -1) {
            return url.substring(lastIndexOfDot + 1);
        }
        return null; // No valid extension found
    }

    private void downloadOptions() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
        View sheetview = getLayoutInflater().inflate(R.layout.movie_details_download_sheet, null);
        bottomSheetDialog.setContentView(sheetview);

        // Buttons
        Button btnTranslatedPart1 = sheetview.findViewById(R.id.btn_translated_part_1);
        Button btnTranslatedPart2 = sheetview.findViewById(R.id.btn_translated_2_part_2);
        Button btnNonTranslated = sheetview.findViewById(R.id.btn_non_translated);

        // set visibility
        if (supabaseMovie.getTranslated() != null && supabaseMovie.getTranslated_2() != null && supabaseMovie.getNon_translated() != null) {
            btnTranslatedPart1.setVisibility(View.VISIBLE);
            btnTranslatedPart2.setVisibility(View.VISIBLE);
            btnNonTranslated.setVisibility(View.VISIBLE);
        } else {
            if (supabaseMovie.getTranslated() != null && supabaseMovie.getTranslated_2() != null) {
                btnTranslatedPart1.setText("Download Part 1");
                btnTranslatedPart2.setText("Download Part 2");
                btnTranslatedPart1.setVisibility(View.VISIBLE);
                btnTranslatedPart2.setVisibility(View.VISIBLE);
            } else if (supabaseMovie.getTranslated() != null) {
                btnTranslatedPart1.setText("Download");
                btnTranslatedPart1.setVisibility(View.VISIBLE);
            } else if (supabaseMovie.getNon_translated() != null) {
                btnTranslatedPart1.setText("Download Non-Translated");
                btnNonTranslated.setVisibility(View.VISIBLE);
            }
        }

        // handle clicks
        btnTranslatedPart1.setOnClickListener(view -> {
            String fileUrl = supabaseMovie.getTranslated();
            supabaseMovie.setCurrentStreamUrl(supabaseMovie.getTranslated());
            String fileName = supabaseMovie.getTitle().replaceAll("[:,\\-_\\s?!'\"@()]", "") + "." + getFileExtension(fileUrl);
            String fileImage = tmdbMovieDetails.getBackdrop_path();

            downloadManager.startDownload(fileName, fileUrl, fileImage);
            bottomSheetDialog.dismiss();
        });

        btnTranslatedPart2.setOnClickListener(view -> {
            String fileUrl = supabaseMovie.getTranslated_2();
            String fileName = supabaseMovie.getTitle().replaceAll("[:,\\-_\\s?!'\"@()]", "") + "part2" + "." + getFileExtension(fileUrl);
            String fileImage = tmdbMovieDetails.getBackdrop_path();
            downloadManager.startDownload(fileName, fileUrl, fileImage);
            bottomSheetDialog.dismiss();
        });

        btnNonTranslated.setOnClickListener(view -> {
            String fileUrl = supabaseMovie.getNon_translated();
            String fileName = supabaseMovie.getTitle().replaceAll("[:,\\-_\\s?!'\"@()]", "") + "." + getFileExtension(fileUrl);
            String fileImage = tmdbMovieDetails.getBackdrop_path();
            downloadManager.startDownload(fileName, fileUrl, fileImage);
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();

    }

    private void navigateToPlayer(VideoMetaData videoMetaData) {
        Intent intent = new Intent(context, PlayerMovie.class);
        intent.putExtra("metadata", videoMetaData);
        this.startActivity(intent);
    }

    private void streamOptions() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
        View sheetview = getLayoutInflater().inflate(R.layout.movie_details_stream_sheet, null);
        bottomSheetDialog.setContentView(sheetview);

        // Buttons
        Button btnTranslatedPart1 = sheetview.findViewById(R.id.btn_stream_part_1);
        Button btnTranslatedPart2 = sheetview.findViewById(R.id.btn_stream_part_2);
        Button btnNonTranslated = sheetview.findViewById(R.id.btn_stream_non_translated);

        // set visibility
        if (supabaseMovie.getTranslated() != null && supabaseMovie.getTranslated_2() != null && supabaseMovie.getNon_translated() != null) {
            btnTranslatedPart1.setVisibility(View.VISIBLE);
            btnTranslatedPart2.setVisibility(View.VISIBLE);
            btnNonTranslated.setVisibility(View.VISIBLE);
        } else {
            if (supabaseMovie.getTranslated() != null && supabaseMovie.getTranslated_2() != null) {
                btnTranslatedPart1.setText("Watch Part 1");
                btnTranslatedPart2.setText("Watch Part 2");
                btnTranslatedPart1.setVisibility(View.VISIBLE);
                btnTranslatedPart2.setVisibility(View.VISIBLE);
            } else if (supabaseMovie.getTranslated() != null) {
                btnTranslatedPart1.setText("Watch");
                btnTranslatedPart1.setVisibility(View.VISIBLE);
            } else if (supabaseMovie.getNon_translated() != null) {
                btnTranslatedPart1.setText("Watch Non-Translated");
                btnNonTranslated.setVisibility(View.VISIBLE);
            }
        }

        // handle clicks
        btnTranslatedPart1.setOnClickListener(view -> {
            supabaseMovie.setCurrentStreamUrl(supabaseMovie.getTranslated());
            navigateToPlayer(new VideoMetaData(supabaseMovie, 0, System.currentTimeMillis()));
            bottomSheetDialog.dismiss();
        });

        btnTranslatedPart2.setOnClickListener(view -> {
            supabaseMovie.setCurrentStreamUrl(supabaseMovie.getTranslated_2());
            navigateToPlayer(new VideoMetaData(supabaseMovie, 0, System.currentTimeMillis()));
            bottomSheetDialog.dismiss();
        });

        btnNonTranslated.setOnClickListener(view -> {
            navigateToPlayer(new VideoMetaData(supabaseMovie, 0, System.currentTimeMillis()));
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        downloadManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void setUpTabs(boolean showAllTabs, TMDBMovieDetails tmdbMovieDetails, SupabaseMovie supabaseMovie) {
        List<Fragment> fragmentList = new ArrayList<>();
        List<String> fragmentTitles = new ArrayList<>();

        if (showAllTabs) {
            fragmentList.add(overview.newInstance(tmdbMovieDetails));
            fragmentList.add(collection.newInstance(tmdbMovieDetails.getBelongs_to_collection()));
            fragmentList.add(cast.newInstance(tmdbMovieDetails.getCredits()));
            fragmentList.add(related.newInstance(supabaseMovie));

            fragmentTitles.add("Overview");
            fragmentTitles.add("Collection");
            fragmentTitles.add("Cast");
            fragmentTitles.add("Related");
        } else {
            fragmentList.add(overview.newInstance(tmdbMovieDetails));
            fragmentList.add(cast.newInstance(tmdbMovieDetails.getCredits()));
            fragmentList.add(related.newInstance(supabaseMovie));

            fragmentTitles.add("Overview");
            fragmentTitles.add("Cast");
            fragmentTitles.add("Related");
        }

        detailsPagerAdapter.updateFragments(fragmentList, fragmentTitles);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) ->
                tab.setText(fragmentTitles.get(position))
        ).attach();
    }

    private void navigateToMovieDetails(TMDBMovieDetails tmdbMovieDetails, SupabaseMovie supabaseMovie) {
        Intent intent = new Intent(context, MovieDetails.class);
        intent.putExtra("supabasemovie", supabaseMovie);
        intent.putExtra("tmdbdetails", tmdbMovieDetails);

        // show ad and navigate
        navigationManager.navigateWithAd(this, intent, preloader);
    }

    @Override
    public void onPosterClicked(SupabaseMovie supabaseMovie1) {
        if (supabaseMovie1.getId() != supabaseMovie.getId()) {
            preloader = new FullScreenPreloader(this);
            preloader.show();

            TMDBApi tmdbApi = TMDBClient.getApi(this);
            Call<TMDBMovieDetails> tmdbMovieDetailsCall = tmdbApi.getMovieDetails(supabaseMovie1.getId(), "images,videos,credits");

            tmdbMovieDetailsCall.enqueue(new Callback<TMDBMovieDetails>() {
                @Override
                public void onResponse(@NonNull Call<TMDBMovieDetails> call, @NonNull Response<TMDBMovieDetails> response) {
                    TMDBMovieDetails tmdbMovieDetails = response.body();
                    if (response.isSuccessful() && response.body() != null) {
                        navigateToMovieDetails(tmdbMovieDetails, supabaseMovie1);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<TMDBMovieDetails> call, @NonNull Throwable throwable) {
                    preloader.dismiss();
                    Toast.makeText(context, "Failed to load movie details", Toast.LENGTH_SHORT).show();
                }
            });

        } else {
            Toast.makeText(this, "This movie " + supabaseMovie1.getTitle() + " is selected", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}