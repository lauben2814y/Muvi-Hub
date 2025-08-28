package muvi.anime.hub.managers.update;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;

public class UpdateSecurityUtils {

    private static final String TAG = "UpdateSecurity";

    /**
     * Verify that the downloaded APK has the same signature as the installed app
     */
    public static boolean verifyApkSignature(Context context, File apkFile) {
        try {
            PackageManager pm = context.getPackageManager();

            // Get current app signature
            PackageInfo installedApp = pm.getPackageInfo(context.getPackageName(),
                    PackageManager.GET_SIGNATURES);
            Signature[] installedSignatures = installedApp.signatures;

            // Get downloaded APK signature
            PackageInfo downloadedApp = pm.getPackageArchiveInfo(apkFile.getAbsolutePath(),
                    PackageManager.GET_SIGNATURES);

            if (downloadedApp == null || downloadedApp.signatures == null) {
                Log.e(TAG, "Could not get signatures from downloaded APK");
                return false;
            }

            Signature[] downloadedSignatures = downloadedApp.signatures;

            // Compare signatures
            if (installedSignatures.length != downloadedSignatures.length) {
                Log.e(TAG, "Signature count mismatch: installed=" + installedSignatures.length +
                        ", downloaded=" + downloadedSignatures.length);
                return false;
            }

            for (int i = 0; i < installedSignatures.length; i++) {
                if (!installedSignatures[i].equals(downloadedSignatures[i])) {
                    Log.e(TAG, "Signature mismatch detected at index " + i);
                    return false;
                }
            }

            if (UpdateConfig.DEBUG_UPDATES) {
                Log.d(TAG, "APK signature verification passed");
            }
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error verifying APK signature", e);
            return false;
        }
    }

    /**
     * Calculate SHA-256 checksum of a file
     */
    public static String calculateSHA256(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            FileInputStream fis = new FileInputStream(file);
            byte[] byteArray = new byte[1024];
            int bytesCount;

            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }
            fis.close();

            byte[] bytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();

        } catch (Exception e) {
            Log.e(TAG, "Error calculating SHA-256", e);
            return null;
        }
    }

    /**
     * Calculate MD5 checksum of a file (alternative to SHA-256)
     */
    public static String calculateMD5(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            FileInputStream fis = new FileInputStream(file);
            byte[] byteArray = new byte[1024];
            int bytesCount;

            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }
            fis.close();

            byte[] bytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();

        } catch (Exception e) {
            Log.e(TAG, "Error calculating MD5", e);
            return null;
        }
    }

    /**
     * Verify file integrity using checksum
     */
    public static boolean verifyFileIntegrity(File file, String expectedChecksum) {
        if (expectedChecksum == null || expectedChecksum.isEmpty()) {
            if (UpdateConfig.DEBUG_UPDATES) {
                Log.d(TAG, "No checksum provided, skipping integrity verification");
            }
            return true; // Skip verification if no checksum provided
        }

        String actualChecksum;

        // Determine checksum type based on length
        if (expectedChecksum.length() == 32) {
            // MD5 checksum
            actualChecksum = calculateMD5(file);
            if (UpdateConfig.DEBUG_UPDATES) {
                Log.d(TAG, "Verifying MD5 checksum");
            }
        } else if (expectedChecksum.length() == 64) {
            // SHA-256 checksum
            actualChecksum = calculateSHA256(file);
            if (UpdateConfig.DEBUG_UPDATES) {
                Log.d(TAG, "Verifying SHA-256 checksum");
            }
        } else {
            Log.w(TAG, "Unknown checksum format, length: " + expectedChecksum.length());
            return true; // Skip verification for unknown formats
        }

        if (actualChecksum == null) {
            Log.e(TAG, "Failed to calculate file checksum");
            return false;
        }

        boolean matches = expectedChecksum.equalsIgnoreCase(actualChecksum);

        if (UpdateConfig.DEBUG_UPDATES) {
            Log.d(TAG, "Checksum verification: " + (matches ? "PASSED" : "FAILED"));
            if (!matches) {
                Log.d(TAG, "Expected: " + expectedChecksum);
                Log.d(TAG, "Actual:   " + actualChecksum);
            }
        }

        return matches;
    }

    /**
     * Verify APK file size matches expected size
     */
    public static boolean verifyFileSize(File file, long expectedSize) {
        if (expectedSize <= 0) {
            if (UpdateConfig.DEBUG_UPDATES) {
                Log.d(TAG, "No expected file size provided, skipping size verification");
            }
            return true; // Skip verification if no size provided
        }

        long actualSize = file.length();
        boolean matches = actualSize == expectedSize;

        if (UpdateConfig.DEBUG_UPDATES) {
            Log.d(TAG, "File size verification: " + (matches ? "PASSED" : "FAILED"));
            Log.d(TAG, "Expected: " + formatFileSize(expectedSize));
            Log.d(TAG, "Actual:   " + formatFileSize(actualSize));
        }

        return matches;
    }

    /**
     * Comprehensive security verification
     */
    public static SecurityVerificationResult verifyApkSecurity(Context context, File apkFile,
                                                               String expectedChecksum, long expectedFileSize) {

        SecurityVerificationResult result = new SecurityVerificationResult();

        // Check if file exists
        if (!apkFile.exists()) {
            result.error = "APK file does not exist";
            return result;
        }

        // Verify file size
        result.fileSizeValid = verifyFileSize(apkFile, expectedFileSize);
        if (!result.fileSizeValid) {
            result.error = "File size verification failed";
        }

        // Verify checksum
        result.checksumValid = verifyFileIntegrity(apkFile, expectedChecksum);
        if (!result.checksumValid) {
            result.error = "File integrity verification failed";
        }

        // Verify APK signature
        result.signatureValid = verifyApkSignature(context, apkFile);
        if (!result.signatureValid) {
            result.error = "APK signature verification failed";
        }

        result.overallValid = result.fileSizeValid && result.checksumValid && result.signatureValid;

        if (UpdateConfig.DEBUG_UPDATES) {
            Log.d(TAG, "Security verification result: " + result);
        }

        return result;
    }

    @SuppressLint("DefaultLocale")
    private static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    public static class SecurityVerificationResult {
        public boolean overallValid = false;
        public boolean signatureValid = false;
        public boolean checksumValid = false;
        public boolean fileSizeValid = false;
        public String error = null;

        @NonNull
        @Override
        public String toString() {
            return "SecurityVerificationResult{" +
                    "overallValid=" + overallValid +
                    ", signatureValid=" + signatureValid +
                    ", checksumValid=" + checksumValid +
                    ", fileSizeValid=" + fileSizeValid +
                    ", error='" + error + '\'' +
                    '}';
        }
    }
}
