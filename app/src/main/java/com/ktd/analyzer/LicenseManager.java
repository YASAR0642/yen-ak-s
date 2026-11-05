package com.ktd.analyzer;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Locale;

public final class LicenseManager {
    private LicenseManager() {}

    private static final String PREF = "ktd_gpr_license";
    private static final String KEY_TOKEN = "activation_token";
    private static final String PREFIX = "KTD1";
    private static final String PUBLIC_KEY_B64 =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAr6q8UzcE9Wwj+g+zuNF7pYN4BSVJleOA8NTHjYviAjJvMNUZ/GzU1mIfBOY9GWXTrpaUfPcFyhTKiwYgRR04RlM/RAkqnS56Uz9h7u3Nu6gTC1AC+vurCSV8OqQsN5TjLKidsJoL809/4iQBADWcF7pNtWWBssn8uN2oBo2sK6zRsyIC43lYGzNf3b77bCJjm6ADkM/BL1N5lx7CfGhSQQ/xkUzMZo1uxIZ6Lya1w6+Dqrv9iQNio4LkLMEcwIOhmOS2jgJRX3ocnisPu+MAEtHFUP6N+HnQ7brhLaKFOGncfckZ4SO00tMZiLzprxmawCyyxogMyYD2/wCzBjgpewIDAQAB";

    public static String deviceCode(Context c) {
        try {
            String androidId = Settings.Secure.getString(c.getContentResolver(), Settings.Secure.ANDROID_ID);
            if (androidId == null) androidId = "NO_ANDROID_ID";
            String src = "KTDGPR|" + c.getPackageName() + "|" + androidId;
            byte[] h = MessageDigest.getInstance("SHA-256").digest(src.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) sb.append(String.format(Locale.US, "%02X", h[i]));
            return sb.toString();
        } catch (Exception e) {
            return "0000000000000000";
        }
    }

    private static String payload(Context c) {
        return "KTD|" + deviceCode(c) + "|LIFETIME";
    }

    public static boolean verify(Context c, String token) {
        if (token == null) return false;
        // Only RSA licenses from the existing KTD activation manager are accepted.
        // Device code includes KTDGPR and the distinct package name so KTD 3D licenses cannot match.
        return verifyLegacy(c, token);
    }

    private static boolean verifyLegacy(Context c, String token) {
        if (token == null) return false;
        token = token.trim();
        String[] p = token.split("-", 3);
        if (p.length != 3 || !PREFIX.equals(p[0])) return false;
        String device = p[1].trim().toUpperCase(Locale.US);
        if (!device.equals(deviceCode(c))) return false;
        try {
            byte[] pubBytes = Base64.decode(PUBLIC_KEY_B64, Base64.DEFAULT);
            PublicKey pub = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(pubBytes));
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(pub);
            sig.update(payload(c).getBytes(StandardCharsets.UTF_8));
            byte[] signature = Base64.decode(p[2], Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
            return sig.verify(signature);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isActivated(Context c) {
        String token = c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY_TOKEN, null);
        return verify(c, token);
    }

    public static boolean activate(Context c, String token) {
        if (!verify(c, token)) return false;
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY_TOKEN, token.trim()).apply();
        return true;
    }

    public static void clear(Context c) {
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().remove(KEY_TOKEN).apply();
    }
}
