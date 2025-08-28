package muvi.anime.hub.adapters.movie;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import muvi.anime.hub.R;
import muvi.anime.hub.api.SecureClient;
import muvi.anime.hub.api.SecureService;
import muvi.anime.hub.data.FilterData;
import muvi.anime.hub.data.Utils;
import muvi.anime.hub.data.movie.MovieGenre;
import muvi.anime.hub.data.movie.SupabaseMovie;
import muvi.anime.hub.data.movie.SupabaseMovieSection;
import muvi.anime.hub.interfaces.OnMoviePosterClicked;
import muvi.anime.hub.pages.MovieMore;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DbSectionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<SupabaseMovieSection> supabaseMovieSections;
    private final Context context;
    private boolean isLoading = false;
    private final OnMoviePosterClicked onMoviePosterClicked;
    private FilterData filterData;
    private String order;
    private int page = 2;
    private final Map<Integer, List<MovieGenre>> sectionGenres = new HashMap<>();

    private static final int VIEW_TYPE_PRELOADER = 0;
    private static final int VIEW_TYPE_SECTION = 1;

    public DbSectionAdapter(List<SupabaseMovieSection> supabaseMovieSections, Context context, OnMoviePosterClicked onMoviePosterClicked) {
        this.supabaseMovieSections = supabaseMovieSections;
        this.context = context;
        this.onMoviePosterClicked = onMoviePosterClicked;
    }

    private void initializeOrder(String header) {
        switch (header) {
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

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_PRELOADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.loading_main_row, parent, false);
            return new dbSectionPreloader(view);
        }

        View view = LayoutInflater.from(context)
                .inflate(R.layout.supabase_movie_header, parent, false);
        return new dbSectionViewHolder(view, this);
    }

    @Override
    public int getItemViewType(int position) {
        return supabaseMovieSections.get(position).isPlaceHolder() ? VIEW_TYPE_PRELOADER : VIEW_TYPE_SECTION;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        if (holder instanceof dbSectionPreloader) {

        } else if (holder instanceof dbSectionViewHolder) {
            SupabaseMovieSection supabaseMovieSection = supabaseMovieSections.get(position);
            filterData = new FilterData(supabaseMovieSection.getHeader(), 2, null, null, null);
            ((dbSectionViewHolder) holder).bind(position, supabaseMovieSection, onMoviePosterClicked);
        }

    }

    private void fetchMoreMovies(DbSectionItemsAdapter dbSectionItemsAdapter, SupabaseMovieSection supabaseMovieSection) {
        filterData.setOrderBy(supabaseMovieSection.getHeader());

        Log.d(Utils.getTag(), "MoviesFragment loaded used page: " + page);

        SecureService movieService = SecureClient.getApi(context);
        Call<List<SupabaseMovie>> call = movieService.getFilteredMovies(
                page,
                20,
                Utils.getMovieFields(),
                Utils.getOrder(supabaseMovieSection.getHeader()),
                null,
                null,
                null
        );
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<SupabaseMovie>> call, @NonNull Response<List<SupabaseMovie>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(Utils.getTag(), "Title " + supabaseMovieSection.getHeader());
                    Log.d(Utils.getTag(), "Response got for " + Utils.getOrder(supabaseMovieSection.getHeader()));

                    List<SupabaseMovie> newMovies = response.body();
                    new Handler(Looper.getMainLooper()).post(() -> {
                        new Handler().postDelayed(() -> {
                            dbSectionItemsAdapter.hideLoading();
                            dbSectionItemsAdapter.addSupabaseMovies(newMovies);
                            page = page + 1;
                            isLoading = false;
                        }, 2000);
                    });
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<SupabaseMovie>> call, @NonNull Throwable throwable) {
                dbSectionItemsAdapter.hideLoading();
            }
        });
    }

    public void addPreloader() {
        supabaseMovieSections.add(new SupabaseMovieSection(true));
        notifyItemInserted(supabaseMovieSections.size() - 1);
    }

    public void addSupabaseMovieSection(SupabaseMovieSection supabaseMovieSection) {
        for (int i = 0; i < supabaseMovieSections.size(); i++) {
            if (supabaseMovieSections.get(i).isPlaceHolder()) {
                supabaseMovieSections.set(i, supabaseMovieSection);
                notifyItemChanged(i);
                break;
            }
        }
    }

    @Override
    public int getItemCount() {
        return supabaseMovieSections.size();
    }

    public String getGenreQuery(String genreNames) {
        return genreNames != null ? "cs.{" + genreNames + "}" : null;
    }

    static class dbSectionPreloader extends RecyclerView.ViewHolder {
        public dbSectionPreloader(@NonNull View itemView) {
            super(itemView);
        }
    }

    public List<MovieGenre> getMovieGenres(Context context) {
        try {
            InputStream inputStream = context.getResources().openRawResource(R.raw.movie_genres);
            InputStreamReader reader = new InputStreamReader(inputStream);

            Gson gson = new Gson();
            Type typeList = new TypeToken<List<MovieGenre>>() {
            }.getType();
            return gson.fromJson(reader, typeList);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void viewMoreBtnClicklistener(int position) {
        if (position != RecyclerView.NO_POSITION) {
            SupabaseMovieSection supabaseMovieSection = supabaseMovieSections.get(position);

            Intent intent = new Intent(context, MovieMore.class);
            intent.putExtra("sectiondata", supabaseMovieSection);
            context.startActivity(intent);
        }
    }

    public class dbSectionViewHolder extends RecyclerView.ViewHolder {
        TextView headerText;
        ImageButton viewMoreBtn;
        RecyclerView innerRecyclerView, genreRecyclerView;

        public dbSectionViewHolder(@NonNull View itemView, DbSectionAdapter dbSectionAdapter) {
            super(itemView);
            headerText = itemView.findViewById(R.id.sectionHeader);
            viewMoreBtn = itemView.findViewById(R.id.viewMoreBtn);
            innerRecyclerView = itemView.findViewById(R.id.innerRecyclerView);
            genreRecyclerView = itemView.findViewById(R.id.genreRecycler);

            viewMoreBtn.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    dbSectionAdapter.viewMoreBtnClicklistener(position);
                }
            });
        }

        public void bind(int position, SupabaseMovieSection supabaseMovieSection, OnMoviePosterClicked onMoviePosterClicked) {
            headerText.setText(supabaseMovieSection.getHeader());

            // Set up inner recyclerview for movies
            DbSectionItemsAdapter dbSectionItemsAdapter = new DbSectionItemsAdapter(
                    supabaseMovieSection.getSupabaseMovieList(),
                    context,
                    onMoviePosterClicked);

            innerRecyclerView.setLayoutManager(
                    new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
            innerRecyclerView.setAdapter(dbSectionItemsAdapter);

            initializeOrder(supabaseMovieSection.getHeader());

            innerRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();

                    if (layoutManager != null) {
                        int visibleItemCount = layoutManager.getChildCount();
                        int totalItemCount = layoutManager.getItemCount();
                        int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                        if (!isLoading && (visibleItemCount + firstVisibleItemPosition) >= totalItemCount && firstVisibleItemPosition >= 0) {
                            isLoading = true;
                            // show loading indicator
                            dbSectionItemsAdapter.showLoading();
                            fetchMoreMovies(dbSectionItemsAdapter, supabaseMovieSection);
                        }
                    }
                }
            });
        }
    }
}
