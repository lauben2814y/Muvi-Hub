package muvi.anime.hub.tabs.tv;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import muvi.anime.hub.R;
import muvi.anime.hub.data.tv.Season;
import muvi.anime.hub.data.tv.SupabaseTv;
import muvi.anime.hub.adapters.tv.EpisodesAdapter;
import muvi.anime.hub.interfaces.OnSeasonBtnClickListener;

public class episodes extends Fragment implements OnSeasonBtnClickListener {
    private static final String ARG_PARAM = "episodes";
    private SupabaseTv supabaseTv;
    private RecyclerView episodesRecycler;
    private EpisodesAdapter episodesAdapter;
    private onSeasonUpdatedListener listener;

    public episodes() {
        // Required empty public constructor
    }

    public interface onSeasonUpdatedListener {
        void onSeasonUpdated(String seasonName);
    }

    public static episodes newInstance(SupabaseTv supabaseTv) {
        episodes fragment = new episodes();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PARAM, supabaseTv);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            listener = (onSeasonUpdatedListener) context;
        } catch (ClassCastException e) {
            throw new RuntimeException(context + " must implement OnSeasonUpdatedListener", e);
        }

        // Check for other required interfaces
        if (!(context instanceof EpisodesAdapter.onPlayEpisodeBtnClickedListener)) {
            throw new ClassCastException(context + " must implement onPlayEpisodeBtnClickedListener");
        } else if (!(context instanceof EpisodesAdapter.onDownloadEpisodeBtnClicked)) {
            throw new ClassCastException(context + " must implement onDownloadEpisodeBtnClickedListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            supabaseTv = (SupabaseTv) getArguments().getSerializable(ARG_PARAM);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_episodes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize UI components
        episodesRecycler = view.findViewById(R.id.tvEpisodesRecycler);
        episodesRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Initialize and set adapter
        initializeAdapter();

        // Set initial data if available
        if (supabaseTv != null && supabaseTv.getSeasons() != null && !supabaseTv.getSeasons().isEmpty()) {
            episodesAdapter.setEpisodeList(supabaseTv.getSeasons().get(0).getEpisodes());
        }
    }

    private void initializeAdapter() {
        if (isAdded() && getActivity() != null) {
            episodesAdapter = new EpisodesAdapter(
                    requireContext(),
                    (EpisodesAdapter.onPlayEpisodeBtnClickedListener) requireActivity(),
                    (EpisodesAdapter.onDownloadEpisodeBtnClicked) requireActivity()
            );

            if (episodesRecycler != null) {
                episodesRecycler.setAdapter(episodesAdapter);
            }
        }
    }

    @Override
    public void onSeasonBtnClicked(Season season) {
        if (!isAdded()) {
            return; // Fragment not attached to activity
        }

        // Safety check and reinitialize if needed
        if (episodesAdapter == null) {
            initializeAdapter();
        }

        if (episodesAdapter != null && season != null && season.getEpisodes() != null) {
            episodesAdapter.setEpisodeList(season.getEpisodes());

            if (listener != null) {
                listener.onSeasonUpdated(season.getName());
            }
        }
    }

    @Override
    public void onDestroyView() {
        // Clean up references to avoid memory leaks
        episodesRecycler = null;
        episodesAdapter = null;
        super.onDestroyView();
    }

    @Override
    public void onDetach() {
        listener = null;
        super.onDetach();
    }
}