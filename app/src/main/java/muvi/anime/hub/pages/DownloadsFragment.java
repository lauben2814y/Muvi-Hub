package muvi.anime.hub.pages;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.appbar.MaterialToolbar;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchListener;
import com.tonyodev.fetch2.Status;
import com.tonyodev.fetch2core.DownloadBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import muvi.anime.hub.MainActivity;
import muvi.anime.hub.managers.FetchSingleton;
import muvi.anime.hub.R;
import muvi.anime.hub.adapters.DownloadAdapter;
import muvi.anime.hub.data.DownloadItem;

public class DownloadsFragment extends Fragment {
    private RecyclerView downloadsRecycler;
    private Fetch fetch;
    private DownloadAdapter downloadAdapter;
    private final List<DownloadItem> currentDownloads = new ArrayList<>();
    private final static String TAG = "Muvi-Hub";
    private MaterialToolbar toolbar;

    public DownloadsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_downloads, container, false);

        toolbar = view.findViewById(R.id.downloads_toolbar);
        fetch = FetchSingleton.getFetchInstance(requireContext());
        downloadsRecycler = view.findViewById(R.id.downloadsRecycler);
        downloadsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));

        downloadAdapter = new DownloadAdapter(new ArrayList<>(), fetch, requireContext());
        downloadsRecycler.setAdapter(downloadAdapter);

        setUpSideNavigation();

        // only get non completed DownloadsFragment
        updateDownloadsList();
        return view;
    }

    private void setUpSideNavigation() {
        toolbar.setNavigationOnClickListener(view -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openDrawer();
            }
        });
    }

    private void updateDownloadsList() {
        fetch.getDownloads(downloads -> {
            List<DownloadItem> items = downloads.stream()
                    .filter(download -> download.getStatus() != Status.CANCELLED) // filter ot completed DownloadsFragment
                    .map(this::mapDownloadToDownloadItem)
                    .collect(Collectors.toList());

            currentDownloads.clear();
            currentDownloads.addAll(items);

            // Use getActivity() with null check instead of requireActivity()
            Activity activity = getActivity();
            if (activity != null && !activity.isFinishing()) {
                activity.runOnUiThread(() -> {
                    // Another check in case the fragment gets detached during UI thread scheduling
                    if (isAdded()) {
                        downloadAdapter.updateItems(currentDownloads);
                    }
                });
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        fetch.removeListener(fetchListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        fetch.addListener(fetchListener);
        updateDownloadsList();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private DownloadItem mapDownloadToDownloadItem(Download download) {
        long total = download.getTotal();
        long downloaded = download.getDownloaded();
        int progress = 0;

        if (total > 0) {
            progress = (int) ((downloaded * 100) / total);
        }

        return new DownloadItem(
                download.getRequest().getHeaders().getOrDefault("fileName", "Unknown File"),
                download.getUrl(),
                download.getRequest().getHeaders().getOrDefault("fileImage", ""),
                progress,
                download.getStatus() == Status.PAUSED,
                downloaded,
                total,
                "0 bytes",
                download.getId(),
                download.getStatus()
        );
    }


    private void updateDownloadItem(Download download) {
        // Check if fragment is still attached
        if (!isAdded()) {
            return;
        }

        Activity activity = getActivity();
        if (activity != null && !activity.isFinishing()) {
            activity.runOnUiThread(() -> {
                // Double-check if fragment is still attached
                if (isAdded()) {
                    DownloadItem item = mapDownloadToDownloadItem(download);
                    downloadAdapter.updateItem(item, download.getId());
                }
            });
        }
    }

    private String formatSpeed(long bytesPerSecond) {
        if (bytesPerSecond < 0) return "N/A";
        double speed = bytesPerSecond;
        String[] units = {"B/s", "KB/s", "MB/s", "GB/s"};
        int unitIndex = 0;

        while (speed >= 1024 && unitIndex < units.length - 1) {
            speed /= 1024;
            unitIndex++;
        }

        return String.format(Locale.US, "%.1f %s", speed, units[unitIndex]);
    }

    private final FetchListener fetchListener = new FetchListener() {
        @Override
        public void onAdded(@NonNull Download download) {
            DownloadItem item = mapDownloadToDownloadItem(download);
            requireActivity().runOnUiThread(() -> {
                downloadAdapter.updateItem(item, download.getId());
            });
        }

        @Override
        public void onQueued(@NonNull Download download, boolean b) {
            updateDownloadItem(download);
        }

        @Override
        public void onWaitingNetwork(@NonNull Download download) {
            DownloadItem item = mapDownloadToDownloadItem(download);
            requireActivity().runOnUiThread(() -> {
                downloadAdapter.updateItem(item, download.getId());
            });
        }

        @Override
        public void onCompleted(@NonNull Download download) {
            requireActivity().runOnUiThread(() -> {
                downloadAdapter.removeItem(download.getId(), download);
                currentDownloads.removeIf(downloadItem -> downloadItem.getDownloadID() == download.getId());
            });
        }

        @Override
        public void onError(@NonNull Download download, @NonNull Error error, @Nullable Throwable throwable) {
            DownloadItem item = mapDownloadToDownloadItem(download);
            Activity activity = getActivity();
            if (activity != null && !activity.isFinishing()) {
                activity.runOnUiThread(() -> {
                    // Double-check if fragment is still attached
                    if (isAdded()) {
                        downloadAdapter.updateItem(item, download.getId());
                    }
                });
            }
            Log.e(TAG, "onError: Error occurred " + error);
        }

        @Override
        public void onDownloadBlockUpdated(@NonNull Download download, @NonNull DownloadBlock downloadBlock, int i) {

        }

        @Override
        public void onStarted(@NonNull Download download, @NonNull List<? extends DownloadBlock> list, int i) {
            // Check if fragment is still attached before updating
            if (isAdded()) {
                updateDownloadItem(download);
            }
        }

        @Override
        public void onProgress(@NonNull Download download, long l, long l1) {
            // Check if fragment is still attached
            if (!isAdded()) {
                return;
            }

            DownloadItem item = mapDownloadToDownloadItem(download);
            item.setNetworkSpeed(formatSpeed(l1));

            // Use getActivity() with null check instead of requireActivity()
            Activity activity = getActivity();
            if (activity != null && !activity.isFinishing()) {
                activity.runOnUiThread(() -> {
                    // Double-check if fragment is still attached
                    if (isAdded()) {
                        int id = download.getId();
                        downloadAdapter.updateItem(item, id);
                    }
                });
            }
        }

        @Override
        public void onPaused(@NonNull Download download) {
            updateDownloadItem(download);
        }

        @Override
        public void onResumed(@NonNull Download download) {
            updateDownloadItem(download);
        }

        @Override
        public void onCancelled(@NonNull Download download) {
            downloadAdapter.removeItem(download.getId(), download);
            downloadAdapter.deleteFile(download);
        }

        @Override
        public void onRemoved(@NonNull Download download) {
            downloadAdapter.removeItem(download.getId(), download);
            downloadAdapter.deleteFile(download);
        }

        @Override
        public void onDeleted(@NonNull Download download) {
            downloadAdapter.removeItem(download.getId(), download);
            downloadAdapter.deleteFile(download);
        }
    };
}