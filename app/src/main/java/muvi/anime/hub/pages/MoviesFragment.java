package muvi.anime.hub.pages;

import static androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import muvi.anime.hub.MainActivity;
import muvi.anime.hub.api.SecureClient;
import muvi.anime.hub.data.Trailer;
import muvi.anime.hub.managers.NavigationManager;
import muvi.anime.hub.api.SecureService;
import muvi.anime.hub.api.TMDBApi;
import muvi.anime.hub.api.TMDBClient;
import muvi.anime.hub.data.Utils;
import muvi.anime.hub.data.movie.TMDBMovieDetails;
import muvi.anime.hub.interfaces.OnMoviePosterClicked;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.search.SearchBar;
import com.google.android.material.search.SearchView;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.jackandphantom.carouselrecyclerview.CarouselLayoutManager;
import com.jackandphantom.carouselrecyclerview.CarouselRecyclerview;
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import muvi.anime.hub.R;
import muvi.anime.hub.adapters.MovieCarouselAdapter;
import muvi.anime.hub.api.MovieCallHandler;
import muvi.anime.hub.data.movie.SupabaseMovie;
import muvi.anime.hub.adapters.movie.DbSectionAdapter;
import muvi.anime.hub.adapters.movie.SearchAdapter;
import muvi.anime.hub.adapters.TrailerAdapter;
import muvi.anime.hub.managers.UserManager;
import muvi.anime.hub.models.User;
import muvi.anime.hub.storage.MovieStorage;
import muvi.anime.hub.ui.FullScreenPreloader;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MoviesFragment extends BaseFragment implements OnMoviePosterClicked {
    private NestedScrollView nestedScrollView;
    private TextView carouselPosterTitle;
    private TextView carouselPosterGenres;
    private RecyclerView mainRecycler;
    private Context context;
    private DbSectionAdapter dbSectionAdapter;
    private MaterialToolbar toolbar;
    private SearchBar searchBar;
    private SearchView searchView;
    private Call<List<SupabaseMovie>> currentMovieResponseCall;
    private static final String SEARCH_TAG = "Muvi-Hub";
    private RecyclerView searchResultsRecycler;
    private SearchAdapter searchAdapter;
    private ViewPager2 trailerPager;
    private DotsIndicator dotsIndicator;
    private TrailerAdapter trailerAdapter;
    private MovieCallHandler movieCallHandler;
    private MovieCarouselAdapter movieCarouselAdapter;
    private Button previewBtn;
    private SupabaseMovie currentSliderMovie;
    private FullScreenPreloader preloader;
    private SecureService movieService;

    private boolean isLoading = false;
    private List<String> loadedSections = new ArrayList<>();

    private NavigationManager navigationManager;
    private MovieStorage movieStorage;

    // Order types
    private static final String latest_order = "release_date.desc";
    private static final String recent_order = "created_at.desc";
    private static final String popular_order = "popularity.desc,release_date.desc";
    private static final String top_order = "vote_count.desc,vote_average.desc";

    private View wishListBtn;
    private TextView wishListTxt;

    private ImageView wishListImg;

    private FirebaseAuth mAuth;
    private UserManager userManager;

    public MoviesFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_movies, container, false);
        CarouselRecyclerview carouselRecyclerview = view.findViewById(R.id.moviesCarousel);
        context = requireContext();
        previewBtn = view.findViewById(R.id.previewMovieBtn);
        carouselPosterTitle = view.findViewById(R.id.movieCarouselPostertitle);
        carouselPosterGenres = view.findViewById(R.id.movieCarouselPosterGenres);
        mainRecycler = view.findViewById(R.id.mainListRecyclerView);
        toolbar = view.findViewById(R.id.moviesToolbar);
        searchBar = view.findViewById(R.id.searchBar);
        searchView = view.findViewById(R.id.searchView);
        searchResultsRecycler = view.findViewById(R.id.searchResultsRecyclerView);
        trailerPager = view.findViewById(R.id.trailerCarouselPager);
        dotsIndicator = view.findViewById(R.id.dots_indicator);
        trailerPager.setOrientation(ORIENTATION_HORIZONTAL);
        trailerAdapter = new TrailerAdapter(this);
        preloader = new FullScreenPreloader(context);
        movieService = SecureClient.getApi(context);
        movieStorage = new MovieStorage(requireContext());
        wishListBtn = view.findViewById(R.id.movieCarouselActionsLeft);
        wishListTxt = view.findViewById(R.id.movieCarouselActionWishListTxt);
        wishListImg = view.findViewById(R.id.movieCarouselActionWishList);
        nestedScrollView = view.findViewById(R.id.movies_nested_scroll);

        // InterstitialAdConfig
        navigationManager = NavigationManager.getInstance(context);

        movieCarouselAdapter = new MovieCarouselAdapter(this, this);
        carouselRecyclerview.setAdapter(movieCarouselAdapter);
        carouselRecyclerview.setIntervalRatio(0.6f);
        carouselRecyclerview.setInfinite(true);
        carouselRecyclerview.setItemSelectListener(new CarouselLayoutManager.OnSelected() {
            @Override
            public void onItemSelected(int i) {
                updateCarouselDetails(i);
            }

            private void updateCarouselDetails(int position) {
                List<SupabaseMovie> supabaseMovieList = movieCarouselAdapter.getSupabaseMovieList();

                if (position < supabaseMovieList.size() && !supabaseMovieList.get(position).isPlaceHolder()) {
                    SupabaseMovie supabaseMovie = supabaseMovieList.get(position);
                    currentSliderMovie = supabaseMovie;
                    setUpFav();
                    carouselPosterTitle.setText(supabaseMovie.getTitle());
                    carouselPosterGenres.setText(String.join(", ", supabaseMovie.getGenres()));
                }
            }
        });

        mainRecycler.setLayoutManager(new LinearLayoutManager(context));
        dbSectionAdapter = new DbSectionAdapter(new ArrayList<>(), context, this);
        mainRecycler.setAdapter(dbSectionAdapter);
        searchResultsRecycler.setLayoutManager(new LinearLayoutManager(context));
        searchAdapter = new SearchAdapter(context, this);
        searchResultsRecycler.setAdapter(searchAdapter);
        trailerPager.setAdapter(trailerAdapter);
        movieCallHandler = new MovieCallHandler(getActivity(), dbSectionAdapter);

        userManager = UserManager.getInstance(requireContext());
        mAuth = FirebaseAuth.getInstance();

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
            // If content still fits on screen and we have more sections to load
            if (nestedScrollView.getChildAt(0).getHeight() <= nestedScrollView.getHeight() &&
                    loadedSections.size() < 5) { // 5 is the total number of sections
                isLoading = false;
                Log.w(SEARCH_TAG, "Content still visible: Loading another section ....");
                loadNextSection();
            } else {
                isLoading = false;
            }
        });
    }

    private void loadCarouselSection() {
        movieCallHandler.fetchMovieSection(
                movieService.getFilteredMovies(
                        1,
                        20,
                        Utils.getMovieFields(),
                        recent_order,
                        null,
                        null,
                        null),
                "Recently Uploaded",
                movies -> {
                    if (!movies.isEmpty()) {
                        // load carousel details
                        SupabaseMovie supabaseMovie = movies.get(0);
                        movieCarouselAdapter.setSupabaseMovieList(movies);
                        carouselPosterTitle.setText(supabaseMovie.getTitle());
                        carouselPosterGenres.setText(String.join(", ", supabaseMovie.getGenres()));
                        currentSliderMovie = supabaseMovie;
                        // set preview on click
                        previewBtn.setOnClickListener(view -> onMoviePosterClick(currentSliderMovie));
                        setUpFav();

                        // Mark this section as loaded
                        loadedSections.add("carousel");
                        isLoading = false;
                        loadTrailers();
                    }
                }
        );
    }

    private void loadNextSection() {
        if (isLoading) return;

        isLoading = true;

        // Check which section to load next
        if (!loadedSections.contains("carousel")) {
            loadCarouselSection();
        } else if (!loadedSections.contains("latest")) {
            loadLatestMovies();
        } else if (!loadedSections.contains("popular")) {
            loadPopularMovies();
        } else if (!loadedSections.contains("toprated")) {
            loadTopRatedMovies();
        }
    }

    private void setUpFav() {
        boolean wishListed = movieStorage.movieExists(currentSliderMovie.getId());
        if (wishListed) {
            wishListImg.setImageResource(R.drawable.bookmark_added_24px);
            wishListTxt.setText("Added");
        } else {
            wishListImg.setImageResource(R.drawable.bookmark_add_24px);
            wishListTxt.setText(R.string.wishlist);

            wishListBtn.setOnClickListener(view -> {
                movieStorage.addMovie(currentSliderMovie);
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
        Call<List<Trailer>> trailerCall = movieService.getTrailers(
                1,
                15,
                Utils.getTrailerFields(),
                "created_at.desc"
        );

        trailerCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<Trailer>> call, @NonNull Response<List<Trailer>> response) {
                // Check if fragment is still attached
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
                isLoading = false;
                checkIfMoreSectionsNeeded();
            }
        });
    }

    private void loadLatestMovies() {
        movieCallHandler.fetchMovieSection(
                movieService.getFilteredMovies(
                        1,
                        20,
                        Utils.getMovieFields(),
                        latest_order,
                        null,
                        null,
                        null),
                "Latest on Muvi",
                movies -> {
                    loadedSections.add("latest");
                    isLoading = false;
                    checkIfMoreSectionsNeeded();
                }
        );
    }

    private void loadPopularMovies() {
        movieCallHandler.fetchMovieSection(
                movieService.getFilteredMovies(
                        1,
                        20,
                        Utils.getMovieFields(),
                        popular_order,
                        null,
                        null,
                        null),
                "Popular",
                movies -> {
                    loadedSections.add("popular");
                    isLoading = false;
                    Log.d(SEARCH_TAG, "Loaded popular section ...");
                    checkIfMoreSectionsNeeded();
                }
        );
    }

    private void loadTopRatedMovies() {
        movieCallHandler.fetchMovieSection(
                movieService.getFilteredMovies(
                        1,
                        20,
                        Utils.getMovieFields(),
                        top_order,
                        null,
                        null,
                        null),
                "Top Rated",
                movies -> {
                    loadedSections.add("toprated");
                    isLoading = false;
                    Log.d(SEARCH_TAG, "Loaded top rated section ...");
                }
        );
    }

    private void setUpSearch() {
        // Handle search query changes
        searchView.getEditText().addTextChangedListener(new TextWatcher() {
            private final Handler handler = new Handler();
            private Runnable searchRunnable;

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // cancel
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
                long DEBOUNCE_DELAY = 1000;
                handler.postDelayed(searchRunnable, DEBOUNCE_DELAY);
            }
        });

        searchView.addTransitionListener(((searchView1, previousState, newState) -> {
            if (newState == SearchView.TransitionState.HIDDEN) {

            }
        }));
    }

    private void performSearch(String query) {
        if (currentMovieResponseCall != null && !currentMovieResponseCall.isCanceled()) {
            currentMovieResponseCall.cancel();
            Log.e(SEARCH_TAG, "performSearch: Search cancelled");
        }

        Log.d(SEARCH_TAG, "performSearch: Search triggered starting ...");

        searchAdapter.showLoading();

        currentMovieResponseCall = movieService.searchMovies(
                Utils.getMovieFields(),
                query
        );
        currentMovieResponseCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<SupabaseMovie>> call, @NonNull Response<List<SupabaseMovie>> response) {
                Log.d(SEARCH_TAG, "performSearch: Call Search triggered and response got");
                List<SupabaseMovie> searchList = response.body();

                Log.d(SEARCH_TAG, "performSearch: Supabase Movie Response Body" + response.body());

                if (call == currentMovieResponseCall && response.isSuccessful() && response.body() != null) {

                    Log.d(SEARCH_TAG, "performSearch: Search successfully and results got for" + query);

                    searchAdapter.setSupabaseMovies(searchList);

                } else {
                    Log.d(SEARCH_TAG, "performSearch: Stale response ignored for " + query);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<SupabaseMovie>> call, @NonNull Throwable throwable) {
                if (call == currentMovieResponseCall && !call.isCanceled()) {
                    throwable.getMessage();
                } else {
                    Log.d(SEARCH_TAG, "Cancelled call ignored for " + query);
                }
            }
        });

    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    protected int getScrollViewId() {
        return R.id.movies_nested_scroll;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (movieCallHandler != null) {
            movieCallHandler.cleanup();
        }
    }

    private void navigateToMovieDetails(TMDBMovieDetails tmdbMovieDetails, SupabaseMovie supabaseMovie) {
        Intent intent = new Intent(context, MovieDetails.class);
        intent.putExtra("supabasemovie", supabaseMovie);
        intent.putExtra("tmdbdetails", tmdbMovieDetails);
        // show ad and navigate
        navigationManager.navigateWithAd(getActivity(), intent, preloader);
    }

    @Override
    public void onMoviePosterClick(SupabaseMovie supabaseMovie) {
        preloader.show();

        userManager.getOrCreateUser(mAuth.getCurrentUser(), new UserManager.UserCallback() {
            @Override
            public void onSuccess(User user) {
                TMDBApi tmdbApi = TMDBClient.getApi(context);
                Call<TMDBMovieDetails> tmdbMovieDetailsCall = tmdbApi.getMovieDetails(supabaseMovie.getId(), "images,videos,credits");
                tmdbMovieDetailsCall.enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<TMDBMovieDetails> call, @NonNull Response<TMDBMovieDetails> response) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            TMDBMovieDetails tmdbMovieDetails = response.body();
                            if (response.isSuccessful() && response.body() != null) {
                                navigateToMovieDetails(tmdbMovieDetails, supabaseMovie);
                            }
                        });
                    }

                    @Override
                    public void onFailure(@NonNull Call<TMDBMovieDetails> call, @NonNull Throwable throwable) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            preloader.dismiss();
                            Toast.makeText(context, "Error loading movie details", Toast.LENGTH_SHORT).show();
                            Log.e(SEARCH_TAG, "onFailure: " + throwable.getMessage());
                            throwable.printStackTrace();
                        });
                    }
                });
            }

            @Override
            public void onError(String error) {

            }
        });
    }
}