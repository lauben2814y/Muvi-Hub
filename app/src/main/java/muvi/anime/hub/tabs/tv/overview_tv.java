package muvi.anime.hub.tabs.tv;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import muvi.anime.hub.R;

public class overview_tv extends Fragment {

    private static final String ARG_PARAM = "param1";

    public overview_tv() {
        // Required empty public constructor
    }

    public static overview_tv newInstance(String param1) {
        overview_tv fragment = new overview_tv();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM, param1);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_overview_tv, container, false);
        TextView overViewTxt = view.findViewById(R.id.tvOverview);
        String overview;

        if (getArguments() != null) {
            overview = (String) getArguments().get(ARG_PARAM);
            overViewTxt.setText(overview);
        }

        return view;
    }
}