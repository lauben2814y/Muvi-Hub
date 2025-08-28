package muvi.anime.hub.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.interstitial.InterstitialAd;

import java.util.ArrayList;
import java.util.List;

import muvi.anime.hub.R;
import muvi.anime.hub.data.movie.SupabaseMovie;
import muvi.anime.hub.interfaces.OnMoviePosterClicked;

public class MovieCarouselAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<SupabaseMovie> supabaseMovieList;
    private boolean isLoading = true;
    private final OnMoviePosterClicked onMoviePosterClicked;
    private Fragment fragment; // Store fragment reference

    private static final int VIEW_TYPE_SHIMMER = 0;
    private static final int VIEW_TYPE_ITEM = 1;

    private InterstitialAd mInterstitialAd;

    public MovieCarouselAdapter(Fragment fragment, OnMoviePosterClicked onMoviePosterClicked) {
        this.fragment = fragment;
        this.onMoviePosterClicked = onMoviePosterClicked;
        this.supabaseMovieList = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            supabaseMovieList.add(new SupabaseMovie(true));
        }
    }

    @Override
    public int getItemViewType(int position) {
        return supabaseMovieList.get(position).isPlaceHolder() ? VIEW_TYPE_SHIMMER : VIEW_TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SHIMMER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.carousel_item_shimmer, parent, false);
            return new ShimmerViewHolder(view);
        }

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.carousel_item, parent, false);
        return new CarouselViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        
        if (holder instanceof ShimmerViewHolder) {
            ShimmerViewHolder shimmerViewHolder = (ShimmerViewHolder) holder;
            shimmerViewHolder.shimmerFrameLayout.startShimmer();
            
        } else if (holder instanceof CarouselViewHolder) {

            if (fragment == null || !fragment.isAdded() || fragment.getActivity() == null
                    || fragment.getActivity().isDestroyed() || fragment.getActivity().isFinishing()) {
                return;
            }

            SupabaseMovie supabaseMovie = supabaseMovieList.get(position);
            ((CarouselViewHolder) holder).bind(supabaseMovie, onMoviePosterClicked);
        }

    }

    @Override
    public int getItemCount() {
        return supabaseMovieList.size();
    }

    public void setSupabaseMovieList(List<SupabaseMovie> newSupabaseMovieList) {
        this.supabaseMovieList.clear();
        this.supabaseMovieList.addAll(newSupabaseMovieList);
        notifyDataSetChanged();
    }

    public List<SupabaseMovie> getSupabaseMovieList() {
        return supabaseMovieList;
    }

    public class CarouselViewHolder extends RecyclerView.ViewHolder {
        ImageView carouselPoster;
        TextView carouselPosterText;

        public CarouselViewHolder(View itemView) {
            super(itemView);
            carouselPoster = itemView.findViewById(R.id.carousel_poster);
            carouselPosterText = itemView.findViewById(R.id.carousel_poster_vj);
        }

        public void bind(SupabaseMovie supabaseMovie, OnMoviePosterClicked onMoviePosterClicked) {
            carouselPosterText.setText(supabaseMovie.getVj());

            DrawableCrossFadeFactory fadeFactory = new DrawableCrossFadeFactory.Builder(500)
                    .setCrossFadeEnabled(true)
                    .build();

            Glide.with(carouselPoster)
                    .load(supabaseMovie.getPoster_path())
                    .transition(DrawableTransitionOptions.withCrossFade(fadeFactory))
                    .centerCrop()
                    .into(carouselPoster);

            carouselPoster.setOnClickListener( view -> {
                int position = getBindingAdapterPosition();

                if (onMoviePosterClicked != null && position != RecyclerView.NO_POSITION) {
                    onMoviePosterClicked.onMoviePosterClick(supabaseMovieList.get(position));
                }
            });
        }
    }

    static class ShimmerViewHolder extends RecyclerView.ViewHolder {
        ShimmerFrameLayout shimmerFrameLayout;

        public ShimmerViewHolder(@NonNull View itemView) {
            super(itemView);
            shimmerFrameLayout = itemView.findViewById(R.id.shimmerContainer);
        }
    }
}
