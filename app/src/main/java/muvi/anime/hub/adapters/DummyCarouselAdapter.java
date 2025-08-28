package muvi.anime.hub.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

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

public class DummyCarouselAdapter extends RecyclerView.Adapter<DummyCarouselAdapter.DummyViewHolder> {
    private final Context context;
    private final List<DummyMedia> dummyMedia;
    private final Activity activity;
    private final NavigationManager navigationManager;

    public DummyCarouselAdapter(Context context, List<DummyMedia> dummyMedia, Activity activity) {
        this.context = context;
        this.dummyMedia = dummyMedia;
        this.activity = activity;
        this.navigationManager = NavigationManager.getInstance(context);
    }

    @NonNull
    @Override
    public DummyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.dummy_carousel_item, parent, false);
        return new DummyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DummyViewHolder holder, int position) {
        DummyMedia dummyMedia1 = dummyMedia.get(position);
        holder.bind(dummyMedia1, this);
    }

    @Override
    public int getItemCount() {
        return dummyMedia.size();
    }

    static class DummyViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public DummyViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.carousel_poster_dummy);
        }

        public void bind(DummyMedia dummyMedia, DummyCarouselAdapter dummyCarouselAdapter) {
            DrawableCrossFadeFactory fadeFactory = new DrawableCrossFadeFactory.Builder(500)
                    .setCrossFadeEnabled(true)
                    .build();

            // Load image
            Glide.with(imageView)
                    .load(dummyMedia.getPoster())
                    .transition(DrawableTransitionOptions.withCrossFade(fadeFactory))
                    .centerCrop()
                    .into(imageView);

            imageView.setOnClickListener(view -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    dummyCarouselAdapter.onPosterClicked(dummyMedia);
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
