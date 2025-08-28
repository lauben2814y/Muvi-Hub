package muvi.anime.hub.tabs.tv;

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
import muvi.anime.hub.data.tv.TMDBTv;
import muvi.anime.hub.adapters.tv.CastAdapter;

public class cast_tv extends Fragment {
    private static final String ARG_PARAM = "tmdbDetails";
    private Context context;
    private RecyclerView castTvRecycler;

    public cast_tv() {

    }

    public static cast_tv newInstance(TMDBTv tmdbTv) {
        cast_tv fragment = new cast_tv();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PARAM, tmdbTv);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
    }

    public static <T> List<T> safeSubList(List<T> list, int fromIndex, int toIndex) {
        if (list == null || fromIndex > list.size() || fromIndex < 0) {
            return new ArrayList<>();
        }
        toIndex = Math.min(toIndex, list.size());
        return list.subList(fromIndex, toIndex);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cast_tv, container, false);
        context = requireContext();
        castTvRecycler = view.findViewById(R.id.tvCastRecycler);
        TMDBTv tmdbTv = null;

        if (getArguments() != null) {
            tmdbTv = (TMDBTv) getArguments().getSerializable(ARG_PARAM);
        }

        if (tmdbTv != null) {
            castTvRecycler.setLayoutManager(new LinearLayoutManager(context));
            CastAdapter castAdapter = new CastAdapter(safeSubList(tmdbTv.getCredits().getCast(), 0, 11), context);
            castTvRecycler.setAdapter(castAdapter);
        }
        return view;
    }
}