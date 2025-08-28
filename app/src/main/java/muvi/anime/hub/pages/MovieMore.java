package muvi.anime.hub.pages;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;

import java.util.List;

import muvi.anime.hub.api.SecureClient;
import muvi.anime.hub.managers.NavigationManager;
import muvi.anime.hub.R;
import muvi.anime.hub.api.SecureService;
import muvi.anime.hub.api.TMDBApi;
import muvi.anime.hub.api.TMDBClient;
import muvi.anime.hub.data.FilterData;
import muvi.anime.hub.data.Utils;
import muvi.anime.hub.data.movie.SupabaseMovie;
import muvi.anime.hub.data.movie.SupabaseMovieSection;
import muvi.anime.hub.adapters.movie.MoreAdapter;
import muvi.anime.hub.data.movie.TMDBMovieDetails;
import muvi.anime.hub.interfaces.OnMoviePosterClicked;
import muvi.anime.hub.managers.UserManager;
import muvi.anime.hub.models.User;
import muvi.anime.hub.ui.FullScreenPreloader;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MovieMore extends AppCompatActivity implements OnMoviePosterClicked {
    private FirebaseAuth firebaseAuth;
    private UserManager userManager;

    private RecyclerView moreRecycler;
    private MoreAdapter moreAdapter;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private List<SupabaseMovie> supabaseMovies;
    private SupabaseMovieSection section;
    private static final int VISIBLE_THRESHOLD = 3;
    private boolean isInitialLoad = true;  // Add this flag
    private final static String TAG = Utils.getTag();
    private MaterialToolbar toolbar;
    private SecureService movieService;
    private FilterData currFilterData;
    private final Context context = this;
    private FullScreenPreloader preloader;
    private NavigationManager navigationManager;
    private String order;
    private Gson gson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_movie_more);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, 0);
            return insets;
        });

        firebaseAuth = FirebaseAuth.getInstance();
        userManager = UserManager.getInstance(context);

        moreRecycler = findViewById(R.id.movieMoreRecycler);
        section = (SupabaseMovieSection) getIntent().getSerializableExtra("sectiondata");
        toolbar = findViewById(R.id.moviesToolbar);
        movieService = SecureClient.getApi(context);
        preloader = new FullScreenPreloader(context);
        gson = new Gson();

        // Interstitial Ad Config and other configs
        navigationManager = NavigationManager.getInstance(context);

        if (section != null) {
            supabaseMovies = section.getSupabaseMovieList();
            int initialPage = (section.getSupabaseMovieList().size() / 20) + 1;
            currFilterData = new FilterData(section.getHeader(), initialPage, null, null, null);
            initializeOrder();

            // Register the result callback
            ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            FilterData filterData = (FilterData) result.getData().getSerializableExtra("resultFilterData");

                            if (filterData != null) {
                                currFilterData = filterData;
                                loadFilteredData();
                            }
                        }
                    }
            );

            toolbar.setTitle(section.getHeader());
            toolbar.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.filterBtn) {
                    // Show search bar and open search view
                    Intent intent = new Intent(this, Filter.class);
                    intent.putExtra("initialData", currFilterData);
                    activityResultLauncher.launch(intent);
                    return true;
                }
                return false;
            });

            toolbar.setNavigationOnClickListener(view -> {
                finish();
            });

            // Handle vertical more scrolling
            GridLayoutManager layoutManager = new GridLayoutManager(this, 3);

            // make loading indicator span full width
            layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    return moreAdapter.getItemViewType(position) == MoreAdapter.TYPE_LOADING ? 3 : 1;
                }
            });

            moreRecycler.setLayoutManager(layoutManager);
            moreAdapter = new MoreAdapter(supabaseMovies, this, this);
            moreRecycler.setAdapter(moreAdapter);
            moreRecycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    GridLayoutManager gridLayoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
                    // Only check if we're actually scrolling down
                    if (dy > 0) {
                        if (!isLoading) {
                            int totalItemCount = gridLayoutManager.getItemCount();
                            int lastVisibleItem = gridLayoutManager.findLastVisibleItemPosition();

                            if (lastVisibleItem + VISIBLE_THRESHOLD >= totalItemCount) {
                                isLoading = true;
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    fetchMoreMovies();
                                });
                            }
                        }
                    }
                }
            });
        }
    }

    private void initializeOrder() {
        switch (section.getHeader()) {
            case "Recently Uploaded":
                order = "created_at.desc";
                break;
            case "Latest on Muvi":
                order = "release_date.desc";
                break;
            case "Popular":
                order = "popularity.desc,release_date.desc";
                break;
            case "Top Rated":
                order = "vote_count.desc,vote_average.desc";
                break;
            default:
                order = null;
                break;
        }
    }

    private void loadFilteredData() {
        // clear initial supabase movie list
        supabaseMovies.clear();
        moreAdapter.notifyDataSetChanged();

        // show loading indicator
        supabaseMovies.add(new SupabaseMovie(true));
        moreAdapter.notifyItemInserted(supabaseMovies.size() - 1);

        Call<List<SupabaseMovie>> call = movieService.getFilteredMovies(
                currFilterData.getPage(),
                20,
                Utils.getMovieFields(),
                order,
                currFilterData.getGenreQuery(),
                currFilterData.getCountryQuery(),
                currFilterData.getVjQuery()
        );

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<SupabaseMovie>> call, @NonNull Response<List<SupabaseMovie>> response) {

                if (!supabaseMovies.isEmpty()) {
                    int lastIndex = supabaseMovies.size() - 1;
                    if (lastIndex >= 0) {
                        supabaseMovies.remove(lastIndex);
                        moreAdapter.notifyItemRemoved(lastIndex);
                    }
                }

                if (response.isSuccessful() && response.body() != null) {
                    List<SupabaseMovie> newMovies = response.body();

                    if (!newMovies.isEmpty()) {
                        int previousSize = supabaseMovies.size();
                        supabaseMovies.addAll(newMovies);
                        moreAdapter.notifyItemRangeInserted(previousSize, newMovies.size());
                        currFilterData.setPage(currFilterData.getPage() + 1);
                    } else {
                        if (!isFinishing()) {
                            Toast.makeText(MovieMore.this, "", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                isLoading = false;
            }

            @Override
            public void onFailure(@NonNull Call<List<SupabaseMovie>> call, @NonNull Throwable throwable) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    isLoading = false;
                });
            }
        });
    }

    private void fetchMoreMovies() {
        Log.d(TAG, "Last item before fetch " + section.getSupabaseMovieList().get(section.getSupabaseMovieList().size() - 1).getTitle());

        // show loading indicator
        supabaseMovies.add(new SupabaseMovie(true));
        moreAdapter.notifyItemInserted(supabaseMovies.size() - 1);

        Log.d(TAG, "Current page before fetch " + currFilterData.getPage());
        Log.d(TAG, "Current page query before fetch " + currFilterData.getPageQuery());

        Call<List<SupabaseMovie>> call = movieService.getFilteredMovies(
                currFilterData.getPage(),
                20,
                Utils.getMovieFields(),
                order,
                currFilterData.getGenreQuery(),
                currFilterData.getCountryQuery(),
                currFilterData.getVjQuery()
        );
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<SupabaseMovie>> call, @NonNull Response<List<SupabaseMovie>> response) {

                // remove loading indicator
                supabaseMovies.remove(supabaseMovies.size() - 1);
                moreAdapter.notifyItemRemoved(supabaseMovies.size());

                if (response.isSuccessful() && response.body() != null) {
                    // Add new movies
                    List<SupabaseMovie> newMovies = response.body();

                    if (!newMovies.isEmpty()) {
                        supabaseMovies.addAll(newMovies);
                        moreAdapter.notifyItemRangeInserted(supabaseMovies.size(), newMovies.size());
                        currFilterData.setPage(currFilterData.getPage() + 1);

                    } else {
                        Toast.makeText(MovieMore.this, "No more data to load", Toast.LENGTH_SHORT).show();
                    }
                }
                isLoading = false;
            }

            @Override
            public void onFailure(@NonNull Call<List<SupabaseMovie>> call, @NonNull Throwable throwable) {
                // remove loading indicator
                supabaseMovies.remove(supabaseMovies.size() - 1);
                moreAdapter.notifyItemRemoved(supabaseMovies.size());

                new Handler(Looper.getMainLooper()).post(() -> {
                    isLoading = false;
                });
            }
        });
    }

    private void navigateToMovieDetails(TMDBMovieDetails tmdbMovieDetails, SupabaseMovie supabaseMovie) {
        Intent intent = new Intent(context, MovieDetails.class);
        intent.putExtra("supabasemovie", supabaseMovie);
        intent.putExtra("tmdbdetails", tmdbMovieDetails);

        // show AD and navigate
        navigationManager.navigateWithAd(this, intent, preloader);
    }

    @Override
    public void onMoviePosterClick(SupabaseMovie supabaseMovie) {
        preloader.show();

        userManager.getOrCreateUser(firebaseAuth.getCurrentUser(), new UserManager.UserCallback() {
            @Override
            public void onSuccess(User user) {
                TMDBApi tmdbApi = TMDBClient.getApi(context);
                Call<TMDBMovieDetails> tmdbMovieDetailsCall = tmdbApi.getMovieDetails(supabaseMovie.getId(), "images,videos,credits");
                tmdbMovieDetailsCall.enqueue(new Callback<TMDBMovieDetails>() {
                    @Override
                    public void onResponse(@NonNull Call<TMDBMovieDetails> call, @NonNull Response<TMDBMovieDetails> response) {
                        TMDBMovieDetails tmdbMovieDetails = response.body();
                        if (response.isSuccessful() && response.body() != null) {
                            navigateToMovieDetails(tmdbMovieDetails, supabaseMovie);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<TMDBMovieDetails> call, @NonNull Throwable throwable) {

                    }
                });
            }

            @Override
            public void onError(String error) {

            }
        });
    }
}