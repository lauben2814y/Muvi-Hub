package muvi.anime.hub.pages;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.tonyodev.fetch2.Fetch;

import java.util.ArrayList;
import java.util.List;

import muvi.anime.hub.adapters.tv.CollectionAdapterTv;
import muvi.anime.hub.api.TMDBApi;
import muvi.anime.hub.api.TMDBClient;
import muvi.anime.hub.managers.DownloadManager;
import muvi.anime.hub.managers.FetchSingleton;
import muvi.anime.hub.R;
import muvi.anime.hub.data.tv.Episode;
import muvi.anime.hub.data.tv.SupabaseTv;
import muvi.anime.hub.adapters.tv.DetailsPagerAdapter;
import muvi.anime.hub.adapters.tv.SeasonSheetBtnsAdapter;
import muvi.anime.hub.data.tv.TMDBTv;
import muvi.anime.hub.managers.NavigationManager;
import muvi.anime.hub.player.tv.VideoMetaData;
import muvi.anime.hub.storage.TvStorage;
import muvi.anime.hub.tabs.tv.cast_tv;
import muvi.anime.hub.tabs.tv.episodes;
import muvi.anime.hub.tabs.tv.overview_tv;
import muvi.anime.hub.tabs.tv.collection_tv;
import muvi.anime.hub.adapters.tv.EpisodesAdapter;
import muvi.anime.hub.ui.FullScreenPreloader;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TvDetails extends AppCompatActivity implements episodes.onSeasonUpdatedListener, EpisodesAdapter.onPlayEpisodeBtnClickedListener, EpisodesAdapter.onDownloadEpisodeBtnClicked, CollectionAdapterTv.onCollectionTvPosterClickedListener {
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private DetailsPagerAdapter detailsPagerAdapter;
    private Button seasonButton;
    private TextView seasonSheetTitle;
    private final Context context = this;
    private Fetch fetch;
    private DownloadManager downloadManager;
    private SupabaseTv supabaseTv;
    private TMDBTv tmdbTv;
    private TvStorage tvStorage;
    private FullScreenPreloader preloader;
    private NavigationManager navigationManager;

    private ImageButton wishListBtn, shareBtn, reportBtn;

    private TextView wishListTxt, tvDetailsName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tv_details);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, 0);
            return insets;
        });

        ImageView headerImage = findViewById(R.id.tvDetailsHeaderImg);
        ImageView posterImage = findViewById(R.id.tvDetailsPoster);
        ImageView logoImage = findViewById(R.id.tvDetailsLogo);
        TextView genres = findViewById(R.id.tvDetailsGenres);
        TextView vote = findViewById(R.id.tvDetailsVote);
        TextView date = findViewById(R.id.tvDetailsDate);
        seasonButton = findViewById(R.id.seasonBtn);

        tabLayout = findViewById(R.id.tvDetailsTabLayout);
        detailsPagerAdapter = new DetailsPagerAdapter(this);
        viewPager = findViewById(R.id.tvDetailsPager);
        viewPager.setAdapter(detailsPagerAdapter);
        wishListBtn = findViewById(R.id.tv_wishlist_img);
        shareBtn = findViewById(R.id.tv_share_img);
        reportBtn = findViewById(R.id.tv_report_img);
        wishListTxt = findViewById(R.id.tv_wishlist_txt);
        tvDetailsName = findViewById(R.id.tvDetailsName);


        // Data config
        preloader = new FullScreenPreloader(context);
        supabaseTv = (SupabaseTv) getIntent().getSerializableExtra("supabasetv");
        tmdbTv = (TMDBTv) getIntent().getSerializableExtra("tmdbdetails");

        // download and fetch config
        navigationManager = NavigationManager.getInstance(context);
        fetch = FetchSingleton.getFetchInstance(context);
        downloadManager = new DownloadManager(this, fetch, context);
        tvStorage = new TvStorage(this);

        if (supabaseTv != null) {
            loadImage(tmdbTv.getBackdrop_path(), headerImage, false);

            loadImage(supabaseTv.getPoster_path(), posterImage, true);

            if (supabaseTv.getLogo_path() != null) {
                loadImage(supabaseTv.getLogo_path(), logoImage, false);
            } else {
                logoImage.setVisibility(View.GONE);
                tvDetailsName.setVisibility(View.VISIBLE);
                tvDetailsName.setText(supabaseTv.getName());
            }

            // set actions
            setActionClicks();

            genres.setText(String.join(", ", supabaseTv.getGenres()));
            vote.setText(String.valueOf(tmdbTv.getVote_average()));
            date.setText(supabaseTv.getYearFromDate(supabaseTv.getFirst_air_date()));


            if (!supabaseTv.getSeasons().isEmpty()) {
                seasonButton.setText(supabaseTv.getSeasons().get(0).getName());
                setUpTabs(supabaseTv, tmdbTv);
                seasonButton.setOnClickListener(view -> showSeasonSheet(supabaseTv));
            }

        }
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

    private void setActionClicks() {
        boolean wishListed = tvStorage.tvExists(supabaseTv.getId());
        if (wishListed) {
            wishListBtn.setImageResource(R.drawable.bookmark_added_24px);
            wishListTxt.setText("Added");
        } else {
            wishListBtn.setOnClickListener(view -> {
                tvStorage.addTv(supabaseTv);
                wishListBtn.setImageResource(R.drawable.bookmark_added_24px);
                wishListTxt.setText("Added");
            });
        }

        // share btn
        shareBtn.setOnClickListener(view -> {
            shareTvURL("https://kamumedia.online/media/tv/" + supabaseTv.getId());
        });
    }

    private void shareTvURL(String url) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out " +
                supabaseTv.getName() + ":\n" + url);

        // Show chooser dialog
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }

    private void showSeasonSheet(SupabaseTv supabaseTv) {
        BottomSheetDialog seasonBottomSheet = new BottomSheetDialog(this);
        View seasonBottomView = getLayoutInflater()
                .inflate(R.layout.tv_season_sheet, null);
        seasonBottomSheet.setContentView(seasonBottomView);
        RecyclerView seasonSheetBtnsRecycler = seasonBottomView.findViewById(R.id.tvSeasonBtnsRecycler);

        seasonSheetTitle = seasonBottomView.findViewById(R.id.tvSeasonSheetTitle);
        seasonSheetTitle.setText(supabaseTv.getName() + " seasons");
        seasonSheetBtnsRecycler.setLayoutManager(new LinearLayoutManager(this));
        SeasonSheetBtnsAdapter seasonSheetBtnsAdapter = new SeasonSheetBtnsAdapter(supabaseTv.getSeasons(), this, Season -> {
            episodes episodesFrag = (episodes) detailsPagerAdapter.getFragmentAtPosition(0);
            if (episodesFrag != null) {
                episodesFrag.onSeasonBtnClicked(Season);
                Log.e("Debug", "episodesFragment is defined and not null");
            } else {
                Log.e("Debug", "episodesFragment is null");
            }
            seasonBottomSheet.dismiss();
        });
        seasonSheetBtnsRecycler.setAdapter(seasonSheetBtnsAdapter);
        seasonBottomSheet.show();
    }

    public void setUpTabs(SupabaseTv supabaseTv, TMDBTv tmdbTv) {
        List<Fragment> fragmentList = new ArrayList<>();
        List<String> fragmentTitles = new ArrayList<>();

        fragmentList.add(episodes.newInstance(supabaseTv));
        fragmentList.add(overview_tv.newInstance(tmdbTv.getOverview()));
        fragmentList.add(cast_tv.newInstance(tmdbTv));
        fragmentList.add(collection_tv.newInstance(supabaseTv));

        fragmentTitles.add("Episodes");
        fragmentTitles.add("Overview");
        fragmentTitles.add("Cast");
        fragmentTitles.add("Related");

        detailsPagerAdapter.updateFragments(fragmentList, fragmentTitles);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) ->
                tab.setText(fragmentTitles.get(position))
        ).attach();

    }

    @Override
    public void onSeasonUpdated(String seasonName) {
        seasonButton.setText(seasonName);
    }

    private void navigateToPlayer(VideoMetaData videoMetaData) {
        Intent intent = new Intent(context, PlayerTv.class);
        intent.putExtra("metadata", videoMetaData);
        intent.putExtra("supabasetv", supabaseTv);
        this.startActivity(intent);
    }

    private void streamOptions(Episode episode) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
        View sheetView = getLayoutInflater()
                .inflate(R.layout.tv_details_stream_sheet, null);
        bottomSheetDialog.setContentView(sheetView);

        // Buttons
        Button btnTranslated = sheetView.findViewById(R.id.btn_stream_translated);
        Button btnNonTranslated = sheetView.findViewById(R.id.btn_stream_non_translated);

        // set visibility
        btnTranslated.setVisibility(View.VISIBLE);
        btnNonTranslated.setVisibility(View.GONE);

        // handle clicks
        btnTranslated.setOnClickListener(view -> {
            episode.setCurrentStreamUrl(episode.getTranslated_url());
            navigateToPlayer(new VideoMetaData(episode, 0, System.currentTimeMillis(), supabaseTv));
            bottomSheetDialog.dismiss();
        });

        btnTranslated.setOnClickListener(view -> {
            episode.setCurrentStreamUrl(episode.getNon_translated_url());
            navigateToPlayer(new VideoMetaData(episode, 0, System.currentTimeMillis(), supabaseTv));
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    private void downloadOptions(Episode episode) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
        View bottomSheetView = getLayoutInflater()
                .inflate(R.layout.tv_details_download_sheet, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        // Buttons
        Button btnTranslated = bottomSheetView.findViewById(R.id.btn_download_translated);
        Button btnNonTranslated = bottomSheetView.findViewById(R.id.btn_download_non_translated);

        // visibility
        btnTranslated.setVisibility(View.VISIBLE);
        btnNonTranslated.setVisibility(View.VISIBLE);

        // listeners
        btnTranslated.setOnClickListener(view -> {
            String fileName = supabaseTv.getName().replaceAll("[:,\\-\\_\\s?!'\"@()]", "") + "S" + episode.getSeason_number() + "E" + episode.getEpisode_number();
            downloadManager.startDownload(fileName, episode.getTranslated_url(), episode.getStill_path());
        });

        btnNonTranslated.setOnClickListener(view -> {
            String fileName = supabaseTv.getName().replaceAll("[:,\\-\\_\\s?!'\"@()]", "") + "S" + episode.getSeason_number() + "E" + episode.getEpisode_number();
            downloadManager.startDownload(fileName, episode.getNon_translated_url(), episode.getStill_path());
        });

        bottomSheetDialog.show();
    }

    public void onPlayEpisodeClicked(Episode episode) {
        String translatedUrl = episode.getTranslated_url();
        String nonTranslatedUrl = episode.getNon_translated_url();

        if (translatedUrl != null && !translatedUrl.isEmpty() && nonTranslatedUrl != null && !nonTranslatedUrl.isEmpty()) {
            streamOptions(episode);
        } else {
            if (translatedUrl != null && !translatedUrl.isEmpty()) {
                episode.setCurrentStreamUrl(translatedUrl);
                navigateToPlayer(new VideoMetaData(episode, 0, System.currentTimeMillis(), supabaseTv));
            } else if (nonTranslatedUrl != null && !nonTranslatedUrl.isEmpty()) {
                episode.setCurrentStreamUrl(nonTranslatedUrl);
                navigateToPlayer(new VideoMetaData(episode, 0, System.currentTimeMillis(), supabaseTv));
            }
        }
    }


    @Override
    public void onDownloadEpisodeClicked(Episode episode) {
        String translatedUrl = episode.getTranslated_url();
        String nonTranslatedUrl = episode.getNon_translated_url();

        if (translatedUrl != null && !translatedUrl.isEmpty() && nonTranslatedUrl != null && !nonTranslatedUrl.isEmpty()) {
            downloadOptions(episode);
        } else {
            if (translatedUrl != null && !translatedUrl.isEmpty()) {
                String fileName = supabaseTv.getName().replaceAll("[:,\\-\\_\\s?!'\"@()]", "") + "S" + episode.getSeason_number() + "E" + episode.getEpisode_number();
                downloadManager.startDownload(fileName, episode.getTranslated_url(), episode.getStill_path());
            } else if (nonTranslatedUrl != null && !nonTranslatedUrl.isEmpty()) {
                String fileName = supabaseTv.getName().replaceAll("[:,\\-\\_\\s?!'\"@()]", "") + "S" + episode.getSeason_number() + "E" + episode.getEpisode_number();
                downloadManager.startDownload(fileName, episode.getNon_translated_url(), episode.getStill_path());
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        downloadManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void navigateToTvDetails(SupabaseTv supabaseTv, TMDBTv tmdbTv) {
        Intent intent = new Intent(context, TvDetails.class);
        intent.putExtra("supabasetv", supabaseTv);
        intent.putExtra("tmdbdetails", tmdbTv);

        // navigate with  ad
        navigationManager.navigateWithAd(this, intent, preloader);
    }

    @Override
    public void onTvPosterClicked(SupabaseTv supabaseTv1) {
        if (supabaseTv.getId() != supabaseTv1.getId()) {
            preloader.show();
            TMDBApi tmdbApi = TMDBClient.getApi(context);
            Call<TMDBTv> tmdbTvDetailsCall = tmdbApi.getTvDetails(supabaseTv1.getId(), "images,videos,credits");

            tmdbTvDetailsCall.enqueue(new Callback<TMDBTv>() {
                @Override
                public void onResponse(@NonNull Call<TMDBTv> call, @NonNull Response<TMDBTv> response) {
                    TMDBTv tmdbTv = response.body();
                    if (response.isSuccessful() && response.body() != null) {
                        navigateToTvDetails(supabaseTv1, tmdbTv);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<TMDBTv> call, @NonNull Throwable throwable) {

                }
            });
        } else {
            Toast.makeText(context, "This TV " + supabaseTv.getName() + " is selected", Toast.LENGTH_SHORT).show();
        }
    }
}