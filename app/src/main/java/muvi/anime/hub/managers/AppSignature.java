package muvi.anime.hub.managers;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class AppSignature {
    // Get your app's basic signature hash (run this once to get the hash)
    public static String getAppSignatureHash(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);

            for (Signature signature : packageInfo.signatures) {
                // Get certificate from signature
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                X509Certificate cert = (X509Certificate) cf.generateCertificate(
                        new ByteArrayInputStream(signature.toByteArray())
                );

                // Get certificate fingerprint
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] fingerprint = md.digest(cert.getEncoded());

                return bytesToHex(fingerprint);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // Generate dynamic signature for secure requests
    public static String generateDynamicSignature(Context context) {
        try {
            String baseSignature = getAppSignatureHash(context);
            String packageName = context.getPackageName();
            long timestamp = System.currentTimeMillis();

            // Round timestamp to nearest hour (prevents replay attacks but allows some time drift)
            long roundedTimestamp = (timestamp / (1000 * 60 * 60)) * (1000 * 60 * 60);

            String combined = baseSignature + packageName + roundedTimestamp;

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(combined.getBytes());

            return bytesToHex(hash);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
