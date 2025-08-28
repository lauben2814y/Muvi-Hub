package muvi.anime.hub.pages;

import android.annotation.SuppressLint;
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

import java.util.List;

import muvi.anime.hub.api.SecureClient;
import muvi.anime.hub.api.SecureService;
import muvi.anime.hub.managers.NavigationManager;
import muvi.anime.hub.R;

import muvi.anime.hub.adapters.tv.MoreAdapter;
import muvi.anime.hub.api.TMDBApi;
import muvi.anime.hub.api.TMDBClient;
import muvi.anime.hub.data.FilterData;
import muvi.anime.hub.data.tv.SupabaseTv;
import muvi.anime.hub.data.tv.SupabaseTvSection;
import muvi.anime.hub.data.tv.TMDBTv;
import muvi.anime.hub.interfaces.OnTvPosterClicked;
import muvi.anime.hub.managers.UserManager;
import muvi.anime.hub.models.User;
import muvi.anime.hub.ui.FullScreenPreloader;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TvMore extends AppCompatActivity implements OnTvPosterClicked {
    private FirebaseAuth firebaseAuth;
    private UserManager userManager;

    private RecyclerView moreRecycler;
    private MoreAdapter moreAdapter;
    private boolean isLoading = false;
    private List<SupabaseTv> supabaseTvs;
    private SupabaseTvSection section;
    private static final int VISIBLE_THRESHOLD = 3;
    private boolean isInitialLoad = true;
    private final static String TAG = "Muvi-Hub";
    private boolean hasScrolled = false;
    private MaterialToolbar toolbar;
    private SecureService tvService;
    private FilterData currFilterData;
    private FullScreenPreloader preloader;
    private final Context context = this;
    private static final int REQUEST_CODE = 1;
    private NavigationManager navigationManager;
    private String order;
    private static final String select = "name,poster_path,first_air_date,id,seasons,created_at,vj,genres,overview,backdrop_path,logo_path,vote_average";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tv_more);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, 0);
            return insets;
        });

        firebaseAuth = FirebaseAuth.getInstance();
        userManager = UserManager.getInstance(context);

        moreRecycler = findViewById(R.id.tvMoreRecycler);
        section = (SupabaseTvSection) getIntent().getSerializableExtra("sectiondata");
        toolbar = findViewById(R.id.tvToolbar);
        tvService = SecureClient.getApi(context);
        preloader = new FullScreenPreloader(context);

        // Interstitial Ad Config
        navigationManager = NavigationManager.getInstance(context);

        if (section != null) {
            supabaseTvs = section.getSupabaseTvs();
            int initialPage = (section.getSupabaseTvs().size() / 20) + 1;
            currFilterData = new FilterData(section.getHeader(), initialPage, null, null, null);
            initializeOrder();

            // Register the filter result callback
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

            // show filter page
            toolbar.setTitle(section.getHeader());
            toolbar.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.filterBtn) {
                    Intent intent = new Intent(this, FilterTv.class);
                    intent.putExtra("initialData", currFilterData);
                    activityResultLauncher.launch(intent);
                    return true;
                }
                return false;
            });

            toolbar.setNavigationOnClickListener(view -> finish());

            // Handle vertical more scrolling
            GridLayoutManager layoutManager = new GridLayoutManager(this, 3);

            // make loading indicator span full width
            layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    return moreAdapter.getItemViewType(position) == MoreAdapter.TYPE_LOADING ? 3 : 1;
                }
            });

            // handle vertical scrolling
            moreRecycler.setLayoutManager(layoutManager);
            moreAdapter = new MoreAdapter(supabaseTvs, this, this);
            moreRecycler.setAdapter(moreAdapter);
            moreRecycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    GridLayoutManager gridLayoutManager = (GridLayoutManager) recyclerView.getLayoutManager();

                    // only check if we are scrolling down
                    if (dy > 0) {
                        if (!isLoading) {
                            int totalItemCount = gridLayoutManager.getItemCount();
                            int lastVisibleItem = gridLayoutManager.findLastVisibleItemPosition();

                            if (lastVisibleItem + VISIBLE_THRESHOLD >= totalItemCount) {
                                isLoading = true;
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    fetchMoreTvs();
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
                order = "first_air_date.desc";
                break;
            case "Popular":
                order = "popularity.desc,first_air_date.desc";
                break;
            case "Top Rated":
                order = "vote_count.desc,vote_average.desc";
                break;
            default:
                order = null;
                break;
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadFilteredData() {
        // clear initial supabase tv list
        supabaseTvs.clear();
        moreAdapter.notifyDataSetChanged();

        // show loading indicator
        supabaseTvs.add(new SupabaseTv(true));
        moreAdapter.notifyItemInserted(supabaseTvs.size() - 1);

        Call<List<SupabaseTv>> call = tvService.getFilteredTvs(
                currFilterData.getPage(),
                20,
                select,
                order,
                currFilterData.getGenreQuery(),
                currFilterData.getCountryQuery(),
                currFilterData.getVjQuery()
        );

        call.enqueue(new Callback<List<SupabaseTv>>() {
            @Override
            public void onResponse(@NonNull Call<List<SupabaseTv>> call, @NonNull Response<List<SupabaseTv>> response) {
                if (response.isSuccessful() && response.body() != null) {

                    // remove loading indicator
                    supabaseTvs.remove(supabaseTvs.size() - 1);
                    moreAdapter.notifyItemRemoved(supabaseTvs.size());

                    List<SupabaseTv> newSupabaseTvs = response.body();

                    if (newSupabaseTvs != null) {
                        supabaseTvs.addAll(newSupabaseTvs);
                        moreAdapter.notifyItemRangeInserted(supabaseTvs.size(), newSupabaseTvs.size());
                        currFilterData.setPage(currFilterData.getPage() + 1);
                    } else {
                        Toast.makeText(TvMore.this, "No more data to load", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<List<SupabaseTv>> call, Throwable throwable) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    Log.e(TAG, "Failed to Fetch filtered Data " + throwable.getMessage());
                    Log.d(TAG, "Filtered Data Genres" + currFilterData);
                    Log.d(TAG, "Filtered Data Page" + currFilterData.getPageQuery());
                    Log.d(TAG, "Filtered Data Vjs" + currFilterData.getVjQuery());
                    Log.d(TAG, "Filtered Data Countries" + currFilterData.getCountryQuery());
                    isLoading = false;
                });
            }
        });
    }

    private void fetchMoreTvs() {
        Log.d(TAG, "Last item before fetch " + section.getSupabaseTvs().get(section.getSupabaseTvs().size() - 1).getName());

        // show loading indicator
        supabaseTvs.add(new SupabaseTv(true));
        moreAdapter.notifyItemInserted(supabaseTvs.size() - 1);

        Log.d(TAG, "Current page before fetch " + currFilterData.getPage());
        Log.d(TAG, "Current page query before fetch " + currFilterData.getPageQuery());

        Call<List<SupabaseTv>> call = tvService.getFilteredTvs(
                currFilterData.getPage(),
                20,
                select,
                order,
                currFilterData.getGenreQuery(),
                currFilterData.getCountryQuery(),
                currFilterData.getVjQuery()
        );
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<SupabaseTv>> call, @NonNull Response<List<SupabaseTv>> response) {
                // Check if the list is not empty before trying to remove the loading indicator
                if (!supabaseTvs.isEmpty()) {
                    // Remove loading indicator
                    int lastIndex = supabaseTvs.size() - 1;
                    if (lastIndex >= 0) {
                        supabaseTvs.remove(lastIndex);
                        moreAdapter.notifyItemRemoved(lastIndex);
                    }
                }

                if (response.isSuccessful() && response.body() != null) {
                    // Add new TVs
                    List<SupabaseTv> newSupabaseTvs = response.body();

                    if (!newSupabaseTvs.isEmpty()) {
                        int previousSize = supabaseTvs.size();
                        supabaseTvs.addAll(newSupabaseTvs);
                        moreAdapter.notifyItemRangeInserted(previousSize, newSupabaseTvs.size());
                        currFilterData.setPage(currFilterData.getPage() + 1);
                    } else {
                        // Safely show toast with context check
                        if (!isFinishing()) {
                            Toast.makeText(TvMore.this, "No more data to load", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                isLoading = false;
            }

            @Override
            public void onFailure(@NonNull Call<List<SupabaseTv>> call, @NonNull Throwable throwable) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    isLoading = false;
                });
            }
        });
    }

    private void navigateToTvDetails(SupabaseTv supabaseTv, TMDBTv tmdbTv) {
        Intent intent = new Intent(context, TvDetails.class);
        intent.putExtra("supabasetv", supabaseTv);
        intent.putExtra("tmdbdetails", tmdbTv);

        // navigate with  ad
        navigationManager.navigateWithAd(this, intent, preloader);
    }

    @Override
    public void onTvPosterClick(SupabaseTv supabaseTv) {
        preloader.show();

        userManager.getOrCreateUser(firebaseAuth.getCurrentUser(), new UserManager.UserCallback() {
            @Override
            public void onSuccess(User user) {
                TMDBApi tmdbApi = TMDBClient.getApi(context);
                Call<TMDBTv> tmdbTvDetailsCall = tmdbApi.getTvDetails(supabaseTv.getId(), "images,videos,credits");

                tmdbTvDetailsCall.enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<TMDBTv> call, @NonNull Response<TMDBTv> response) {
                        TMDBTv tmdbTv = response.body();
                        if (response.isSuccessful() && response.body() != null) {
                            try {
                                navigateToTvDetails(supabaseTv, tmdbTv);
                            } catch (Exception e) {
                                // Log the error but don't crash
                                Log.e("DialogDismiss", "Error dismissing dialog: " + e.getMessage());
                                // Still try to navigate even if dialog dismiss fails
                                navigateToTvDetails(supabaseTv, tmdbTv);
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<TMDBTv> call, @NonNull Throwable throwable) {

                    }
                });
            }

            @Override
            public void onError(String error) {

            }
        });
    }
}