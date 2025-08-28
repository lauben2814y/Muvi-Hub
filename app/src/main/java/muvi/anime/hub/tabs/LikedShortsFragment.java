package muvi.anime.hub.tabs;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import muvi.anime.hub.R;

public class LikedShortsFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";

    public LikedShortsFragment() {
        // Required empty public constructor
    }

    public static LikedShortsFragment newInstance(String param1) {
        LikedShortsFragment fragment = new LikedShortsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_liked_shorts, container, false);
    }
}