package muvi.anime.hub.adapters.movie;

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
import muvi.anime.hub.data.movie.TMDBMovieCast;

public class CastAdapter extends RecyclerView.Adapter<CastAdapter.castViewHolder> {
    private final List<TMDBMovieCast> castList;
    private final Context context;

    public CastAdapter(List<TMDBMovieCast> castList, Context context) {
        this.castList = castList;
        this.context = context;
    }

    @NonNull
    @Override
    public castViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.movie_details_cast_item, parent, false);
        return new castViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull castViewHolder holder, int position) {
        TMDBMovieCast cast = castList.get(position);

        holder.castName.setText(cast.getName());
        holder.castAs.setText(cast.getCharacter());

        DrawableCrossFadeFactory fadeFactory = new DrawableCrossFadeFactory.Builder(500)
                .setCrossFadeEnabled(true)
                .build();

        Glide.with(holder.castProfile)
                .load(cast.getProfile_path())
                .transition(DrawableTransitionOptions.withCrossFade(fadeFactory))
                .centerCrop()
                .into(holder.castProfile);

    }

    @Override
    public int getItemCount() {
        return castList.size();
    }

    static class castViewHolder extends RecyclerView.ViewHolder {
        TextView castName, castAs;
        ImageView castProfile;

        public castViewHolder(@NonNull View itemView) {
            super(itemView);
            castName = itemView.findViewById(R.id.movieCastName);
            castAs = itemView.findViewById(R.id.movieCastAs);
            castProfile = itemView.findViewById(R.id.movieCastProfile);
        }
    }
}
