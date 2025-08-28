package muvi.anime.hub.gestures;

import android.content.Context;
import android.media.AudioManager;

import muvi.anime.hub.R;

public class VolumeController {
    private final AudioManager audioManager;
    private final int maxVolume;
    private int currentVolume;

    public static class VolumeInfo {
        public final int percentage;
        public final int iconRes;
        public final boolean isMuted;

        public VolumeInfo(int percentage, int iconRes, boolean isMuted) {
            this.percentage = percentage;
            this.iconRes = iconRes;
            this.isMuted = isMuted;
        }
    }

    public VolumeController(Context context) {
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        this.currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    public VolumeInfo adjustVolume(float delta) {
        int volumeChange = (int) (delta * maxVolume);
        currentVolume = Math.max(0, Math.min(maxVolume, currentVolume + volumeChange));

        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0);

        int volumePercent = (int) ((float) currentVolume / maxVolume * 100);
        int iconRes = getVolumeIcon(currentVolume);
        boolean isMuted = currentVolume == 0;

        return new VolumeInfo(volumePercent, iconRes, isMuted);
    }

    private int getVolumeIcon(int volume) {
        if (volume == 0) {
            return R.drawable.volume_off_24px;
        } else if (volume < maxVolume * 0.5f) {
            return R.drawable.volume_down_24px;
        } else {
            return R.drawable.volume_up_24px;
        }
    }

    public int getCurrentVolume() {
        return currentVolume;
    }

    public int getMaxVolume() {
        return maxVolume;
    }

    public VolumeInfo getCurrentVolumeInfo() {
        int volumePercent = (int) ((float) currentVolume / maxVolume * 100);
        int iconRes = getVolumeIcon(currentVolume);
        boolean isMuted = currentVolume == 0;
        return new VolumeInfo(volumePercent, iconRes, isMuted);
    }
}
