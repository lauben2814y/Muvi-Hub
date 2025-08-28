package muvi.anime.hub.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory;
import com.facebook.shimmer.ShimmerFrameLayout;
import muvi.anime.hub.interfaces.OnTvPosterClicked;

import java.util.ArrayList;
import java.util.List;

import muvi.anime.hub.R;
import muvi.anime.hub.data.tv.SupabaseTv;
import muvi.anime.hub.ui.FullScreenPreloader;

public class TvCarouselAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<SupabaseTv> supabaseTvList;
    private final Context context;
    private final OnTvPosterClicked onTvPosterClicked;
    private FullScreenPreloader preloader;

    private static final int VIEW_TYPE_SHIMMER = 0;
    private static final int VIEW_TYPE_ITEM = 1;

    public TvCarouselAdapter(Context context, OnTvPosterClicked onTvPosterClicked) {
        this.onTvPosterClicked = onTvPosterClicked;
        this.supabaseTvList = new ArrayList<>();
        this.context = context;

        for (int i = 0; i < 20; i++) {
            supabaseTvList.add(new SupabaseTv(true));
        }
    }

    @Override
    public int getItemViewType(int position) {
        return supabaseTvList.get(position).isPlaceHolder() ? VIEW_TYPE_SHIMMER : VIEW_TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SHIMMER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.carousel_item_shimmer_tv, parent, false);
            return new ShimmerViewHolder(view);
        }

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.carousel_item_tv, parent, false);
        return new TvCarouselViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ShimmerViewHolder) {
            ShimmerViewHolder shimmerViewHolder = (ShimmerViewHolder) holder;
            shimmerViewHolder.shimmerFrameLayout.startShimmer();
        } else if (holder instanceof TvCarouselViewHolder) {
            SupabaseTv supabaseTv = supabaseTvList.get(position);
            ((TvCarouselViewHolder) holder).bind(supabaseTv, onTvPosterClicked);
        }
    }

    @Override
    public int getItemCount() {
        return supabaseTvList.size();
    }

    public void setSupabaseTvList(List<SupabaseTv> newSupabaseTvs) {
        this.supabaseTvList.clear();
        this.supabaseTvList.addAll(newSupabaseTvs);
        notifyDataSetChanged();
    }

    public List<SupabaseTv> getSupabaseTvList() {
        return supabaseTvList;
    }

    public class TvCarouselViewHolder extends RecyclerView.ViewHolder {
        private TextView tvVj;
        private ImageView tvPoster;

        public TvCarouselViewHolder(@NonNull View itemView) {
            super(itemView);
            tvVj = itemView.findViewById(R.id.tv_carousel_poster_vj);
            tvPoster = itemView.findViewById(R.id.tv_carousel_poster);
        }

        public void bind(SupabaseTv supabaseTv, OnTvPosterClicked onTvPosterClicked) {
            tvVj.setText(supabaseTv.getVj());

            DrawableCrossFadeFactory fadeFactory = new DrawableCrossFadeFactory.Builder(500)
                    .setCrossFadeEnabled(true)
                    .build();

            Glide.with(tvPoster)
                    .load(supabaseTv.getPoster_path())
                    .transition(DrawableTransitionOptions.withCrossFade(fadeFactory))
                    .centerCrop()
                    .into(tvPoster);

            tvPoster.setOnClickListener( view -> {
                int position = getBindingAdapterPosition();

                if (onTvPosterClicked != null && position != RecyclerView.NO_POSITION) {
                    onTvPosterClicked.onTvPosterClick(supabaseTvList.get(position));
                }
            });
        }
    }

    static class ShimmerViewHolder extends RecyclerView.ViewHolder {
        ShimmerFrameLayout shimmerFrameLayout;

        public ShimmerViewHolder(@NonNull View itemView) {
            super(itemView);
            shimmerFrameLayout = itemView.findViewById(R.id.shimmerContainerTv);
        }
    }
}
