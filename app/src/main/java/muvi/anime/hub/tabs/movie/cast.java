package muvi.anime.hub.tabs.movie;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import muvi.anime.hub.R;
import muvi.anime.hub.data.movie.TMDBMovieCast;
import muvi.anime.hub.data.movie.TMDBMovieCredit;
import muvi.anime.hub.adapters.movie.CastAdapter;

public class cast extends Fragment {
    private static final String ARG_DATA  = "cast";
    private Context context;

    public cast() {
        // Required empty public constructor
    }

    public static cast newInstance(TMDBMovieCredit tmdbMovieCredit) {
        cast fragment = new cast();
        Bundle args = new Bundle();
        args.putSerializable(ARG_DATA, tmdbMovieCredit);
        fragment.setArguments(args);
        return fragment;
    }

    public static <T> List<T> safeSubList(List<T> list, int fromIndex, int toIndex) {
        if (list == null || fromIndex > list.size() || fromIndex < 0) {
            return new ArrayList<>();
        }
        toIndex = Math.min(toIndex, list.size());
        return list.subList(fromIndex, toIndex);
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_cast, container, false);
        context = requireContext();

        TMDBMovieCredit tmdbMovieCredit = null;
        RecyclerView castRecycler = view.findViewById(R.id.movieCastRecycler);

        if (getArguments() != null) {
            tmdbMovieCredit = (TMDBMovieCredit) getArguments().getSerializable(ARG_DATA);
        }

        if (tmdbMovieCredit != null) {
            castRecycler.setLayoutManager(new LinearLayoutManager(context));
            CastAdapter castAdapter = new CastAdapter(safeSubList(tmdbMovieCredit.getCast(), 0, 11), context);
            castRecycler.setAdapter(castAdapter);
        }
        return view;
    }
}