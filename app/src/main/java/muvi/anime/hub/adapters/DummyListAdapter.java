package muvi.anime.hub.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory;

import java.util.List;

import muvi.anime.hub.BuildConfig;
import muvi.anime.hub.R;
import muvi.anime.hub.data.DummyMedia;
import muvi.anime.hub.managers.NavigationManager;
import muvi.anime.hub.pages.DummyDetails;
import muvi.anime.hub.ui.FullScreenPreloader;

public class DummyListAdapter extends RecyclerView.Adapter<DummyListAdapter.DummyListViewHolder> {
    private final Context context;
    private final List<DummyMedia> dummyMedia;
    private final Activity activity;
    private final NavigationManager navigationManager;

    public DummyListAdapter(Context context, List<DummyMedia> dummyMedia, Activity activity) {
        this.context = context;
        this.dummyMedia = dummyMedia;
        this.activity = activity;
        this.navigationManager = NavigationManager.getInstance(context);
    }

    @NonNull
    @Override
    public DummyListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.movie_details_collection_item, parent, false);
        return new DummyListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DummyListViewHolder holder, int position) {
        DummyMedia dummyMedia1 = dummyMedia.get(position);
        holder.bind(dummyMedia1, this);
    }

    @Override
    public int getItemCount() {
        return dummyMedia.size();
    }

    static class DummyListViewHolder extends RecyclerView.ViewHolder {
        ImageView poster;
        TextView title, overview, type;

        public DummyListViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.movieCollectionTitle);
            overview = itemView.findViewById(R.id.movieCollectionOverview);
            poster = itemView.findViewById(R.id.movieCollectionPoster);
            type = itemView.findViewById(R.id.movieCollectionType);
        }

        public void bind(DummyMedia dummyMedia, DummyListAdapter dummyListAdapter) {
            title.setText(dummyMedia.getTitle());
            overview.setText(dummyMedia.getOverview());

            DrawableCrossFadeFactory fadeFactory = new DrawableCrossFadeFactory.Builder(500)
                    .setCrossFadeEnabled(true)
                    .build();

            // Load image
            Glide.with(poster)
                    .load(dummyMedia.getPoster())
                    .transition(DrawableTransitionOptions.withCrossFade(fadeFactory))
                    .centerCrop()
                    .into(poster);

            poster.setOnClickListener( view -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    dummyListAdapter.onPosterClicked(dummyMedia);
                }
            });

            type.setOnClickListener( view -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    dummyListAdapter.onPosterClicked(dummyMedia);
                }
            });

            title.setOnClickListener( view -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    dummyListAdapter.onPosterClicked(dummyMedia);
                }
            });

            overview.setOnClickListener( view -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    dummyListAdapter.onPosterClicked(dummyMedia);
                }
            });
        }
    }

    private void onPosterClicked(DummyMedia dummyMedia) {
        Intent intent = new Intent(context, DummyDetails.class);
        intent.putExtra("dummymedia", dummyMedia);

        // show ad and navigate
        navigationManager.navigateWithAd(activity, intent, new FullScreenPreloader(context));
    }
}
