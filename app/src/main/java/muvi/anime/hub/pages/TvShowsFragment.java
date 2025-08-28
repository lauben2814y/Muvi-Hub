package muvi.anime.hub.pages;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.search.SearchBar;
import com.google.android.material.search.SearchView;
import com.google.firebase.auth.FirebaseAuth;
import com.jackandphantom.carouselrecyclerview.CarouselLayoutManager;
import com.jackandphantom.carouselrecyclerview.CarouselRecyclerview;
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import muvi.anime.hub.MainActivity;
import muvi.anime.hub.api.SecureClient;
import muvi.anime.hub.api.SecureService;
import muvi.anime.hub.data.Trailer;
import muvi.anime.hub.managers.NavigationManager;
import muvi.anime.hub.R;
import muvi.anime.hub.adapters.TrailerAdapter;
import muvi.anime.hub.adapters.TvCarouselAdapter;
import muvi.anime.hub.api.TMDBApi;
import muvi.anime.hub.api.TMDBClient;
import muvi.anime.hub.api.TvCallHandler;
import muvi.anime.hub.data.Utils;
import muvi.anime.hub.data.tv.SupabaseTv;
import muvi.anime.hub.data.tv.TMDBTv;
import muvi.anime.hub.managers.UserManager;
import muvi.anime.hub.models.User;
import muvi.anime.hub.storage.TvStorage;
import muvi.anime.hub.ui.FullScreenPreloader;
import muvi.anime.hub.adapters.tv.SearchAdapter;
import muvi.anime.hub.adapters.tv.DbSectionAdapter;
import muvi.anime.hub.interfaces.OnTvPosterClicked;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TvShowsFragment extends BaseFragment implements OnTvPosterClicked {
    private UserManager userManager;
    private FirebaseAuth firebaseAuth;

    private NestedScrollView nestedScrollView;
    private Context context;
    private CarouselRecyclerview carouselRecyclerview;
    private TextView carouselPosterName;
    private TextView carouselPosterGenres;
    private RecyclerView mainListRecycler;
    private DbSectionAdapter dbSectionAdapter;
    private Button previewTvBtn;
    private SupabaseTv supabaseTvActive;
    private FullScreenPreloader preloader;
    private MaterialToolbar toolbar;
    private SearchBar searchBar;
    private SearchView searchView;
    private Call<List<SupabaseTv>> currentTvResponseCall;
    private static final String SEARCH_TAG = "Muvi-Hub";
    private RecyclerView searchResultsRecycler;
    private SearchAdapter searchAdapter;
    private TvCallHandler tvCallHandler;
    private TvCarouselAdapter tvCarouselAdapter;
    private ViewPager2 trailerPager;
    private DotsIndicator dotsIndicator;
    private TrailerAdapter trailerAdapter;
    private SecureService tvService;

    private NavigationManager navigationManager;
    private boolean isLoading = false;
    private final List<String> loadedSections = new ArrayList<>();

    private TvStorage tvStorage;

    // Order types
    private static final String latest_order = "first_air_date.desc";
    private static final String recent_order = "created_at.desc";
    private static final String popular_order = "popularity.desc,first_air_date.desc";
    private static final String top_order = "vote_count.desc,vote_average.desc";

    // Select columns
    private static final String select = "name,poster_path,first_air_date,id,seasons,created_at,vj,genres,overview,backdrop_path,logo_path,vote_average";

    private View wishListBtn;
    private TextView wishListTxt;

    private ImageView wishListImg;

    public TvShowsFragment() {
        // Required empty public constructor
    }

    public static TvShowsFragment newInstance() {
        TvShowsFragment fragment = new TvShowsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    protected int getScrollViewId() {
        return R.id.tvs_nested_scroll;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_tv_shows, container, false);
        context = requireContext();

        userManager = UserManager.getInstance(context);
        firebaseAuth = FirebaseAuth.getInstance();

        carouselRecyclerview = view.findViewById(R.id.tvCarousel);
        carouselPosterName = view.findViewById(R.id.tvCarouselPosterName);
        carouselPosterGenres = view.findViewById(R.id.tvCarouselPosterGenres);
        mainListRecycler = view.findViewById(R.id.mainListRecyclerView3);
        previewTvBtn = view.findViewById(R.id.previewTvBtn);
        toolbar = view.findViewById(R.id.tvToolBar);
        searchBar = view.findViewById(R.id.searchBarTv);
        searchView = view.findViewById(R.id.searchViewTv);
        searchResultsRecycler = view.findViewById(R.id.searchTvResultsRecyclerView);
        trailerPager = view.findViewById(R.id.tvTrailerCarouselPager);
        dotsIndicator = view.findViewById(R.id.tv_dots_indicator);
        trailerAdapter = new TrailerAdapter(this);
        preloader = new FullScreenPreloader(context);
        tvService = SecureClient.getApi(context);
        wishListBtn = view.findViewById(R.id.tvCarouselActionsLeft);
        wishListTxt = view.findViewById(R.id.tvCarouselActionWishListTxt);
        wishListImg = view.findViewById(R.id.tvCarouselActionWishList);
        tvStorage = new TvStorage(requireContext());
        nestedScrollView = view.findViewById(R.id.tvs_nested_scroll);

        // InterstitialAdConfig
        navigationManager = NavigationManager.getInstance(context);

        tvCarouselAdapter = new TvCarouselAdapter(context, this);
        carouselRecyclerview.setAdapter(tvCarouselAdapter);
        carouselRecyclerview.setIntervalRatio(0.6f);
        carouselRecyclerview.setInfinite(true);
        carouselRecyclerview.setItemSelectListener(new CarouselLayoutManager.OnSelected() {
            @Override
            public void onItemSelected(int i) {
                updateCarouselDetails(i);
            }

            private void updateCarouselDetails(int position) {
                List<SupabaseTv> supabaseTvList = tvCarouselAdapter.getSupabaseTvList();

                if (position < supabaseTvList.size() && !supabaseTvList.get(position).isPlaceHolder()) {
                    SupabaseTv supabaseTv = supabaseTvList.get(position);
                    supabaseTvActive = supabaseTv;
                    setUpFav();
                    carouselPosterName.setText(supabaseTv.getName());
                    carouselPosterGenres.setText(String.join(", ", supabaseTv.getGenres()));
                }
            }
        });

        mainListRecycler.setLayoutManager(new LinearLayoutManager(context));
        dbSectionAdapter = new DbSectionAdapter(new ArrayList<>(), context, this);
        mainListRecycler.setAdapter(dbSectionAdapter);

        searchResultsRecycler.setLayoutManager(new LinearLayoutManager(context));
        searchAdapter = new SearchAdapter(context, this);
        searchResultsRecycler.setAdapter(searchAdapter);
        trailerPager.setAdapter(trailerAdapter);
        tvCallHandler = new TvCallHandler(getActivity(), dbSectionAdapter);

        setUpSearch();
        setUpSideNavigation();

        // Set up scroll listener
        nestedScrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {

            // Check if user is near the bottom (within 200px)
            if (!isLoading && (v.getChildAt(0).getMeasuredHeight() - v.getMeasuredHeight() - scrollY) < 50) {
                Log.d(SEARCH_TAG, "Reached the end starting to load ...");
                loadNextSection();
            }
        });

        // Load first section immediately
        loadNextSection();

        return view;
    }

    private void checkIfMoreSectionsNeeded() {
        nestedScrollView.post(() -> {
            if (nestedScrollView.getChildAt(0).getHeight() <= nestedScrollView.getHeight() &&
                    loadedSections.size() < 5) {
                isLoading = false;
                loadNextSection();
            } else {
                isLoading = false;
            }
        });
    }

    private void loadNextSection() {
        if (isLoading) return;
        isLoading = true;
        // Check which section to load next
        if (!loadedSections.contains("carousel")) {
            loadCarouselSection();
        } else if (!loadedSections.contains("latest")) {
            loadLatestTvs();
        } else if (!loadedSections.contains("popular")) {
            loadPopularTvs();
        } else if (!loadedSections.contains("toprated")) {
            loadTopRatedTvs();
        }
    }

    private void loadCarouselSection() {
        tvCallHandler.fetchTvSection(
                tvService.getFilteredTvs(
                        1,
                        20,
                        select,
                        recent_order,
                        null,
                        null,
                        null),
                "Recently Uploaded",
                tvs -> {
                    if (!tvs.isEmpty()) {
                        SupabaseTv supabaseTv = tvs.get(0);
                        supabaseTvActive = supabaseTv;
                        tvCarouselAdapter.setSupabaseTvList(tvs);
                        carouselPosterName.setText(supabaseTv.getName());
                        carouselPosterGenres.setText(String.join(", ", supabaseTv.getGenres()));
                        //set preview btn click
                        setUpFav();
                        previewTvBtn.setOnClickListener(view -> {
                            onTvPosterClick(supabaseTvActive);
                        });
                        loadedSections.add("carousel");
                        isLoading = false;
                        loadTrailers();
                    }
                }

        );
    }

    private void setUpFav() {
        boolean wishListed = tvStorage.tvExists(supabaseTvActive.getId());
        if (wishListed) {
            wishListImg.setImageResource(R.drawable.bookmark_added_24px);
            wishListTxt.setText("Added");
        } else {
            wishListImg.setImageResource(R.drawable.bookmark_add_24px);
            wishListTxt.setText("Wishlist");

            wishListBtn.setOnClickListener(view -> {
                tvStorage.addTv(supabaseTvActive);
                wishListImg.setImageResource(R.drawable.bookmark_added_24px);
                wishListTxt.setText("Added");
            });
        }
    }

    private void setUpSideNavigation() {
        toolbar.setNavigationOnClickListener(view -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openDrawer();
            }
        });
    }

    private void loadTrailers() {
        Call<List<Trailer>> trailerCall = tvService.getTvTrailers(
                1,
                15,
                Utils.getTrailerFields(),
                "created_at.desc"
        );

        trailerCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<Trailer>> call, @NonNull Response<List<Trailer>> response) {
                if (!isAdded() || getActivity() == null) {
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    List<Trailer> trailers = response.body();

                    List<String> videoIds = trailers.stream()
                            .filter(trailer -> !trailer.isPlaceholder() && trailer.getVideo_id() != null)
                            .map(Trailer::getVideo_id)
                            .collect(Collectors.toList());

                    trailerAdapter.setTrailers(videoIds);

                    dotsIndicator.attachTo(trailerPager);

                    loadedSections.add("trailers");
                    isLoading = false;
                    checkIfMoreSectionsNeeded();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Trailer>> call, @NonNull Throwable throwable) {

            }
        });
    }

    private void loadLatestTvs() {
        tvCallHandler.fetchTvSection(
                tvService.getFilteredTvs(
                        1,
                        20,
                        select,
                        latest_order,
                        null,
                        null,
                        null),
                "Latest on Muvi",
                tvs -> {
                    loadedSections.add("latest");
                    isLoading = false;
                    checkIfMoreSectionsNeeded();
                }
        );
    }

    private void loadPopularTvs() {
        tvCallHandler.fetchTvSection(
                tvService.getFilteredTvs(
                        1,
                        20,
                        select,
                        popular_order,
                        null,
                        null,
                        null),
                "Popular",
                tvs -> {
                    loadedSections.add("popular");
                    isLoading = false;
                    checkIfMoreSectionsNeeded();
                }
        );
    }

    private void loadTopRatedTvs() {
        tvCallHandler.fetchTvSection(
                tvService.getFilteredTvs(
                        1,
                        20,
                        select,
                        top_order,
                        null,
                        null,
                        null),
                "Top Rated",
                tvs -> {
                    loadedSections.add("toprated");
                    isLoading = false;
                }
        );
    }

    private void setUpSearch() {
        searchView.getEditText().addTextChangedListener(new TextWatcher() {
            private final long DEBOUNCE_DELAY = 1000;
            private final Handler handler = new Handler();
            private Runnable searchRunnable;

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (searchRunnable != null) {
                    handler.removeCallbacks(searchRunnable);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String query = editable.toString().trim();

                searchRunnable = () -> {
                    if (!query.isEmpty()) {
                        performSearch(query);
                    }
                };
                handler.postDelayed(searchRunnable, DEBOUNCE_DELAY);
            }
        });

        searchView.addTransitionListener(((searchView1, previousState, newState) -> {
            if (newState == SearchView.TransitionState.HIDDEN) {

            }
        }));
    }

    private void performSearch(String query) {
        if (currentTvResponseCall != null && !currentTvResponseCall.isCanceled()) {
            currentTvResponseCall.cancel();
            Log.d(SEARCH_TAG, "performSearch: Search cancelled");
        }

        searchAdapter.showLoading();

        Log.d(SEARCH_TAG, "performSearch: Search triggered starting ...");

        currentTvResponseCall = tvService.searchTvs(
                Utils.getTvFields(),
                query
        );

        currentTvResponseCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<SupabaseTv>> call, @NonNull Response<List<SupabaseTv>> response) {
                Log.d(SEARCH_TAG, "performSearch: Call Search triggered and response got");
                List<SupabaseTv> searchList = response.body();

                Log.d(SEARCH_TAG, "performSearch: Supabase Movie Response Body" + response.body());

                if (call == currentTvResponseCall && response.isSuccessful() && response.body() != null) {

                    Log.d(SEARCH_TAG, "performSearch: Search successfully and results got for" + query);

                    searchAdapter.setSupabaseTvs(searchList);

                } else {
                    Log.d(SEARCH_TAG, "performSearch: Stale response ignored for " + query);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<SupabaseTv>> call, @NonNull Throwable throwable) {
                if (call == currentTvResponseCall && !call.isCanceled()) {
                    throwable.getMessage();
                } else {
                    Log.d(SEARCH_TAG, "Cancelled call ignored for " + query);
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (tvCallHandler != null) {
            tvCallHandler.cleanUp();
        }
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
                            navigateToTvDetails(supabaseTv, tmdbTv);
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

    private void navigateToTvDetails(SupabaseTv supabaseTv, TMDBTv tmdbTv) {
        Intent intent = new Intent(context, TvDetails.class);
        intent.putExtra("supabasetv", supabaseTv);
        intent.putExtra("tmdbdetails", tmdbTv);

        // show ad ad navigate
        navigationManager.navigateWithAd(getActivity(), intent, preloader);
    }
}