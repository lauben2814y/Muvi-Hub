package muvi.anime.hub.pages;

import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import muvi.anime.hub.R;

public abstract class ShortsBaseFragment extends Fragment {
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Find the NestedScrollView in the fragment
        RelativeLayout relativeView = view.findViewById(getScrollViewId());
        if (relativeView != null) {
            // Get the bottom navigation from the activity
            BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottom_navigation);
            if (bottomNav != null) {
                // Apply the padding once the bottom nav is laid out
                bottomNav.post(() -> {
                    int bottomNavHeight = bottomNav.getHeight();
                    relativeView.setPadding(
                            relativeView.getPaddingLeft(),
                            relativeView.getPaddingTop(),
                            relativeView.getPaddingRight(),
                            bottomNavHeight
                    );
                    relativeView.setClipToPadding(false);
                });
            }
        }
    }

    // Each fragment must implement this to return its own scroll view ID
    protected abstract int getScrollViewId();
}
