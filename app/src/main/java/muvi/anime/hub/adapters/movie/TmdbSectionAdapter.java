package muvi.anime.hub.adapters.movie;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import muvi.anime.hub.R;
import muvi.anime.hub.api.TMDBApi;
import muvi.anime.hub.api.TMDBClient;
import muvi.anime.hub.data.movie.TMDBMovie;
import muvi.anime.hub.data.movie.TMDBMovieResponse;
import muvi.anime.hub.data.movie.TMDBMovieSection;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TmdbSectionAdapter extends RecyclerView.Adapter<TmdbSectionAdapter.tmdbSectionViewHolder> {
    private final List<TMDBMovieSection> tmdbMovieSections;
    private final Context context;
    private int popularPage = 2;
    private boolean isLoading = false;

    public TmdbSectionAdapter(List<TMDBMovieSection> tmdbMovieSections, Context context) {
        this.tmdbMovieSections = tmdbMovieSections;
        this.context = context;
    }

    @NonNull
    @Override
    public tmdbSectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.tmdb_movie_header, parent, false);
        return new tmdbSectionViewHolder(view, this);
    }

    @Override
    public void onBindViewHolder(@NonNull tmdbSectionViewHolder holder, int position) {
        TMDBMovieSection tmdbMovieSection = tmdbMovieSections.get(position);
        holder.headerText.setText(tmdbMovieSection.getHeader());

        TmdbSectionItemsAdapter movieTMDBSectionItemsAdapter = new TmdbSectionItemsAdapter(tmdbMovieSection.getTmdbMovieList(), context);
        holder.innerRecyclerView.setLayoutManager(
                new LinearLayoutManager(holder.itemView.getContext(), LinearLayoutManager.HORIZONTAL, false)
        );
        holder.innerRecyclerView.setAdapter(movieTMDBSectionItemsAdapter);

        holder.innerRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

//                    check is list end is reached
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount && firstVisibleItemPosition >= 0) {
                        Toast.makeText(context, "Starting to fetch", Toast.LENGTH_SHORT).show();

                        recyclerView.post(() -> {
                            loadMoreMovies(movieTMDBSectionItemsAdapter, tmdbMovieSection);
                        });
                    }
                }
            }
        });
    }

    private void loadMoreMovies(TmdbSectionItemsAdapter movieTMDBSectionItemsAdapter, TMDBMovieSection tmdbMovieSection) {
        if (isLoading) return;
        isLoading = true;

        movieTMDBSectionItemsAdapter.showLoading();

        if (tmdbMovieSection.getHeader() == "Popular") {
            TMDBApi tmdbApi = TMDBClient.getApi(context);
            tmdbApi.getPopularMovies("en-us", tmdbMovieSection.getNextPage()).enqueue(new Callback<TMDBMovieResponse>() {
                @Override
                public void onResponse(Call<TMDBMovieResponse> call, Response<TMDBMovieResponse> response) {
                    isLoading = false;

                    if (response.isSuccessful() && response.body() != null) {
                        TMDBMovieResponse popularMovieResponse = response.body();
                        List<TMDBMovie> newPopularMovies = popularMovieResponse.getResults();

                        int previousSize = tmdbMovieSection.getTmdbMovieList().size();
                        tmdbMovieSection.getTmdbMovieList().addAll(newPopularMovies);

                        movieTMDBSectionItemsAdapter.addItems(newPopularMovies);

                        movieTMDBSectionItemsAdapter.notifyItemRangeInserted(previousSize, newPopularMovies.size());

                        tmdbMovieSection.incrementNextPage();

                    } else {
                        movieTMDBSectionItemsAdapter.hideLoading();
                        System.err.println("Error: " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<TMDBMovieResponse> call, Throwable t) {
                    isLoading = false;

                    movieTMDBSectionItemsAdapter.hideLoading();
                    System.err.println("Error: " + t.getMessage());
                }
            });
        } else {

        }
    }


    public void addTMDBMovieSection(TMDBMovieSection tmdbMovieSection) {
        tmdbMovieSections.add(tmdbMovieSection);
        notifyItemInserted(tmdbMovieSections.size() - 1);
    }

    @Override
    public int getItemCount() {
        return tmdbMovieSections.size();
    }

    static class tmdbSectionViewHolder extends RecyclerView.ViewHolder {
        TextView headerText, viewMoreBtn;
        RecyclerView innerRecyclerView;

        public tmdbSectionViewHolder(@NonNull View itemView, TmdbSectionAdapter movieTMDBHeaderAdapter) {
            super(itemView);
            headerText = itemView.findViewById(R.id.movieTMDBSectionHeader);
            viewMoreBtn = itemView.findViewById(R.id.movieTMDBViewMoreBtn);
            innerRecyclerView = itemView.findViewById(R.id.movieTMDBInnerRecyclerView);
        }
    }
}
