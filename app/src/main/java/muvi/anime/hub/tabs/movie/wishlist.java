package muvi.anime.hub.tabs.movie;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import muvi.anime.hub.R;
import muvi.anime.hub.adapters.movie.CollectionAdapter;
import muvi.anime.hub.data.tv.SupabaseTv;
import muvi.anime.hub.storage.MovieStorage;

public class wishlist extends Fragment {
    private CollectionAdapter collectionAdapter;
    private MovieStorage movieStorage;

    public wishlist() {
        // Required empty public constructor
    }

    public static wishlist newInstance() {
        wishlist fragment = new wishlist();
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
        if (!(context instanceof CollectionAdapter.onCollectionPosterClickedListener)) {
            throw new ClassCastException(context.toString() + " must implement OnEpisodeClickListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_wishlist, container, false);
        RecyclerView collectionRecycler = view.findViewById(R.id.wishlist_movie_recycler);
        movieStorage = new MovieStorage(requireContext());

        collectionAdapter = new CollectionAdapter(movieStorage.getMovies(), requireContext(), (CollectionAdapter.onCollectionPosterClickedListener) getActivity(), requireActivity());
        collectionRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        collectionRecycler.setAdapter(collectionAdapter);

        return view;
    }
}