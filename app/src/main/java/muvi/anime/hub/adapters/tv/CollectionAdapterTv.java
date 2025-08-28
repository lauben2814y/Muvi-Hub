package muvi.anime.hub.adapters.tv;

import android.app.Activity;
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

import java.util.List;

import muvi.anime.hub.R;
import muvi.anime.hub.data.tv.SupabaseTv;

public class CollectionAdapterTv extends RecyclerView.Adapter<CollectionAdapterTv.CollectionViewHolderTv> {
    private final List<SupabaseTv> supabaseTvs;
    private final Context context;
    private final onCollectionTvPosterClickedListener onCollectionTvPosterClickedListener;
    private final Activity activity;

    public CollectionAdapterTv(List<SupabaseTv> supabaseTvs, Context context, onCollectionTvPosterClickedListener onCollectionTvPosterClickedListener, Activity activity) {
        this.supabaseTvs = supabaseTvs;
        this.context = context;
        this.onCollectionTvPosterClickedListener = onCollectionTvPosterClickedListener;
        this.activity = activity;
    }

    @NonNull
    @Override
    public CollectionViewHolderTv onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.tv_details_collection_item, parent, false);
        return new CollectionViewHolderTv(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CollectionViewHolderTv holder, int position) {
        SupabaseTv supabaseTv = supabaseTvs.get(position);
        holder.bind(supabaseTv);
    }

    @Override
    public int getItemCount() {
        return supabaseTvs.size();
    }

    public class CollectionViewHolderTv extends RecyclerView.ViewHolder {
        TextView name, vj, overview;
        ImageView poster;

        public CollectionViewHolderTv(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.collectionTitle);
            vj = itemView.findViewById(R.id.collectionVj);
            overview = itemView.findViewById(R.id.collectionOverview);
            poster = itemView.findViewById(R.id.collectionPoster);
        }

        public void bind(SupabaseTv supabaseTv) {
            name.setText(supabaseTv.getName());
            vj.setText(supabaseTv.getVj());
            overview.setText(supabaseTv.getOverview());

            DrawableCrossFadeFactory fadeFactory = new DrawableCrossFadeFactory.Builder(500)
                    .setCrossFadeEnabled(true)
                    .build();

            Glide.with(poster)
                    .load(supabaseTv.getPoster_path())
                    .transition(DrawableTransitionOptions.withCrossFade(fadeFactory))
                    .centerCrop()
                    .into(poster);

            poster.setOnClickListener(view -> {
                int position = getBindingAdapterPosition();

                if (onCollectionTvPosterClickedListener != null && position != RecyclerView.NO_POSITION) {
                    onCollectionTvPosterClickedListener.onTvPosterClicked(supabaseTvs.get(position));
                }
            });
        }

    }

    public interface onCollectionTvPosterClickedListener {
        void onTvPosterClicked(SupabaseTv supabaseTv);
    }
}
