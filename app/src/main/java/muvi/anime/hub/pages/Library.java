package muvi.anime.hub.pages;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;

import muvi.anime.hub.R;
import muvi.anime.hub.adapters.movie.CollectionAdapter;
import muvi.anime.hub.adapters.movie.DetailsPagerAdapter;
import muvi.anime.hub.adapters.movie.RecentAdapter;
import muvi.anime.hub.adapters.tv.CollectionAdapterTv;
import muvi.anime.hub.adapters.tv.RecentTvAdapter;
import muvi.anime.hub.api.TMDBApi;
import muvi.anime.hub.api.TMDBClient;
import muvi.anime.hub.data.movie.SupabaseMovie;
import muvi.anime.hub.data.movie.TMDBMovieDetails;
import muvi.anime.hub.data.tv.SupabaseTv;
import muvi.anime.hub.data.tv.TMDBTv;
import muvi.anime.hub.managers.NavigationManager;
import muvi.anime.hub.player.VideoPlaybackManager;
import muvi.anime.hub.player.tv.VideoTvPlaybackManager;
import muvi.anime.hub.tabs.movie.wishlist;
import muvi.anime.hub.tabs.tv.wishlist_tv;
import muvi.anime.hub.ui.FullScreenPreloader;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Library extends AppCompatActivity implements CollectionAdapter.onCollectionPosterClickedListener, CollectionAdapterTv.onCollectionTvPosterClickedListener {
    private RecyclerView recentMoviesRecycler;
    private MaterialToolbar toolbar;
    private FullScreenPreloader preloader;

    private RecyclerView recentEpisodesRecycler;

    private ViewPager2 libraryPager;
    private TabLayout tabLayout;
    private DetailsPagerAdapter detailsPagerAdapter;
    private NavigationManager navigationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_library);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, 0);
            return insets;
        });

        setUpViews();

        // initialize play back managers
        VideoPlaybackManager playbackManager = new VideoPlaybackManager(this);
        VideoTvPlaybackManager tvPlaybackManager = new VideoTvPlaybackManager(this);
        detailsPagerAdapter = new DetailsPagerAdapter(this);
        RecentAdapter recentAdapter = new RecentAdapter(this, playbackManager.getRecentlyPlayed(), this);
        RecentTvAdapter recentTvAdapter = new RecentTvAdapter(this, tvPlaybackManager.getRecentlyPlayed(), this);
        preloader = new FullScreenPreloader(this);
        navigationManager = NavigationManager.getInstance(this);

        toolbar.setNavigationOnClickListener(view -> finish());

        libraryPager.setAdapter(detailsPagerAdapter);
        recentMoviesRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recentMoviesRecycler.setAdapter(recentAdapter);
        recentEpisodesRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recentEpisodesRecycler.setAdapter(recentTvAdapter);

        setUpTabs();
    }

    private void setUpViews() {
        tabLayout = findViewById(R.id.library_tabs);
        libraryPager = findViewById(R.id.library_pager);
        toolbar = findViewById(R.id.library_toolbar);
        recentEpisodesRecycler = findViewById(R.id.recent_episodes_recycler);
        recentMoviesRecycler = findViewById(R.id.recent_movies_recycler);
    }

    private void setUpTabs() {
        List<Fragment> fragmentList = new ArrayList<>();
        List<String> fragmentTitles = new ArrayList<>();

        fragmentList.add(wishlist.newInstance());
        fragmentList.add(wishlist_tv.newInstance());

        fragmentTitles.add("Movies");
        fragmentTitles.add("Tv shows");

        detailsPagerAdapter.updateFragments(fragmentList, fragmentTitles);

        new TabLayoutMediator(tabLayout, libraryPager, (tab, position) ->
                tab.setText(fragmentTitles.get(position))
        ).attach();
    }

    private void navigateToMovieDetails(TMDBMovieDetails tmdbMovieDetails, SupabaseMovie supabaseMovie) {
        Intent intent = new Intent(this, MovieDetails.class);
        intent.putExtra("supabasemovie", supabaseMovie);
        intent.putExtra("tmdbdetails", tmdbMovieDetails);

        // show ad and navigate
        navigationManager.navigateWithAd(this, intent, preloader);
    }

    @Override
    public void onPosterClicked(SupabaseMovie supabaseMovie) {
        preloader.show();

        TMDBApi tmdbApi = TMDBClient.getApi(this);
        Call<TMDBMovieDetails> tmdbMovieDetailsCall = tmdbApi.getMovieDetails(supabaseMovie.getId(), "images,videos,credits");
        tmdbMovieDetailsCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<TMDBMovieDetails> call, @NonNull Response<TMDBMovieDetails> response) {
                TMDBMovieDetails tmdbMovieDetails = response.body();
                if (response.isSuccessful() && response.body() != null) {
                    navigateToMovieDetails(tmdbMovieDetails, supabaseMovie);
                }
            }

            @Override
            public void onFailure(@NonNull Call<TMDBMovieDetails> call, @NonNull Throwable throwable) {
                preloader.dismiss();
            }
        });
    }

    private void navigateToTvDetails(SupabaseTv supabaseTv, TMDBTv tmdbTv) {
        Intent intent = new Intent(this, TvDetails.class);
        intent.putExtra("supabasetv", supabaseTv);
        intent.putExtra("tmdbdetails", tmdbTv);

        // show ad ad navigate
        navigationManager.navigateWithAd(this, intent, preloader);
    }

    @Override
    public void onTvPosterClicked(SupabaseTv supabaseTv) {
        preloader.show();
        TMDBApi tmdbApi = TMDBClient.getApi(this);
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
}