package muvi.anime.hub.pages;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import muvi.anime.hub.R;

public abstract class BaseFragment extends Fragment {

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Find the NestedScrollView in the fragment
        NestedScrollView scrollView = view.findViewById(getScrollViewId());
        if (scrollView != null) {
            // Get the bottom navigation from the activity
            BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottom_navigation);
            if (bottomNav != null) {
                // Apply the padding once the bottom nav is laid out
                bottomNav.post(() -> {
                    int bottomNavHeight = bottomNav.getHeight();
                    scrollView.setPadding(
                            scrollView.getPaddingLeft(),
                            scrollView.getPaddingTop(),
                            scrollView.getPaddingRight(),
                            bottomNavHeight
                    );
                    scrollView.setClipToPadding(false);
                });
            }
        }
    }

    // Each fragment must implement this to return its own scroll view ID
    protected abstract int getScrollViewId();
}
