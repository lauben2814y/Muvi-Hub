package muvi.anime.hub.managers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;

import java.io.File;
import java.security.MessageDigest;

// Android: Device Fingerprinting
public class DeviceFingerprint {

    public static String generateFingerprint(Context context) {
        StringBuilder fingerprint = new StringBuilder();

        try {
            // 1. Android ID (most stable)
            @SuppressLint("HardwareIds")
            String androidId = Settings.Secure.getString(
                    context.getContentResolver(),
                    Settings.Secure.ANDROID_ID
            );
            fingerprint.append(androidId != null ? androidId : "unknown_android_id");

            // 2. Device Hardware Info
            fingerprint.append("|").append(Build.MANUFACTURER);
            fingerprint.append("|").append(Build.MODEL);
            fingerprint.append("|").append(Build.DEVICE);
            fingerprint.append("|").append(Build.PRODUCT);

            // 3. Build Information
            fingerprint.append("|").append(Build.FINGERPRINT);
            fingerprint.append("|").append(Build.HARDWARE);
            fingerprint.append("|").append(Build.BOARD);

            // 4. Display Metrics
            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            fingerprint.append("|").append(displayMetrics.widthPixels);
            fingerprint.append("|").append(displayMetrics.heightPixels);
            fingerprint.append("|").append(displayMetrics.densityDpi);

            // Hash the complete fingerprint
            return sha256(fingerprint.toString());

        } catch (Exception e) {
            Log.e("DeviceFingerprint", "Error generating fingerprint", e);
            return "fingerprint_generation_failed";
        }
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (Exception e) {
            return input; // Fallback
        }
    }

    // Security check methods
    public static boolean isAppTampered(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);

            for (Signature signature : packageInfo.signatures) {
                String signatureHash = sha256(signature.toCharsString());
                // You'll need to add your expected signature hash here
                Log.d("APP_SIGNATURE", "Current signature hash: " + signatureHash);
                // For now, return false. Add your validation logic here.
                return false;
            }
        } catch (Exception e) {
            return true; // Assume tampered if can't verify
        }
        return false;
    }

    public static boolean isDeviceRooted() {
        // Check for common root indicators
        String[] rootIndicators = {
                "/system/app/Superuser.apk",
                "/system/xbin/su",
                "/system/bin/su",
                "/sbin/su",
                "/data/local/xbin/su",
                "/data/local/bin/su"
        };

        for (String indicator : rootIndicators) {
            if (new File(indicator).exists()) {
                return true;
            }
        }
        return false;
    }

    public static boolean isDebuggable(Context context) {
        return (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }
}
