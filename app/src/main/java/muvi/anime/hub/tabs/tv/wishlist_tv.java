package muvi.anime.hub.tabs.tv;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import muvi.anime.hub.R;

import muvi.anime.hub.adapters.tv.CollectionAdapterTv;
import muvi.anime.hub.storage.TvStorage;

public class wishlist_tv extends Fragment {
    private CollectionAdapterTv collectionAdapterTv;
    private TvStorage tvStorage;

    public wishlist_tv() {
        // Required empty public constructor
    }

    public static wishlist_tv newInstance() {
        wishlist_tv fragment = new wishlist_tv();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (!(context instanceof CollectionAdapterTv.onCollectionTvPosterClickedListener)) {
            throw new ClassCastException(context.toString() + " must implement OnEpisodeClickListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_wishlist_tv, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.wishlist_tv_recycler);
        tvStorage = new TvStorage(requireContext());

        collectionAdapterTv = new CollectionAdapterTv(tvStorage.getTvs(), requireContext(), (CollectionAdapterTv.onCollectionTvPosterClickedListener) getActivity(), requireActivity());
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(collectionAdapterTv);

        return view;
    }
}