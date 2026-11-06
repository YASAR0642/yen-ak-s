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

    /**
     * Lisans dogrulama nedenini ayri donderir. Basarili sonuc yalnizca
     * RSA imzasi ve BU uygulamanin cihaz kodu tam eslesirse verilir.
     */
    public static String validationError(Context c, String rawToken) {
        if (rawToken == null || rawToken.trim().isEmpty())
            return "Aktivasyon kodunu girin veya panodan yapistirin.";
        String token = cleanToken(rawToken);
        // Mevcut KTD 3D aktivasyon ureticisinin 4-4-4-4 bicimli
        // cihaz-bagli lisans kodlari desteklenir. RSA destegi de korunur.
        String compact = token.replace("-", "");
        if (compact.matches("[0-9]{16}")) {
            if (Activation16Code.verify(deviceCode(c), token)) return null;
            return "16 haneli aktivasyon kodu bu GPR cihaz koduyla eslesmiyor. "
                    + "GPR uygulamasinda gorunen cihaz koduyla yeni kod uretin. "
                    + "Surum: v1.0.13";
        }
        if (!token.startsWith(PREFIX + "-"))
            return "Kod bicimi gecersiz. 16 haneli (XXXX-XXXX-XXXX-XXXX) "
                    + "veya KTD1- ile baslayan lisans kodu girin.";
        String[] parts = token.split("-", 3);
        if (parts.length != 3 || !parts[1].matches("[0-9A-F]{16}") || parts[2].isEmpty())
            return "Kod bicimi eksik veya bozulmus. Yoneticide KOPYALA secenegini kullanin.";
        String localDevice = deviceCode(c);
        if (!localDevice.equals(parts[1]))
            return "Farkli cihaz kodu: Lisans " + parts[1]
                    + " icin uretilmis; bu GPR uygulamasinin kodu " + localDevice
                    + ". GPR ekranindaki cihaz koduyla YENI lisans uretin.";
        try {
            byte[] pubBytes = Base64.decode(PUBLIC_KEY_B64, Base64.DEFAULT);
            PublicKey pub = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(pubBytes));
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(pub);
            sig.update(payload(c).getBytes(StandardCharsets.UTF_8));
            byte[] signature = Base64.decode(parts[2], Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
            if (sig.verify(signature)) return null;
            return "Dijital imza dogrulanamadi. Kod eksik kopyalanmis ya da baska bir yonetici surumuyle uretilmis olabilir.";
        } catch (Exception ex) {
            return "Dijital imza okunamadi. Kodu yeniden kopyalayip yapistirin.";
        }
    }

    private static String cleanToken(String raw) {
        // WhatsApp / metin paylasimindan gelen bosluk ve satir sonlarini kaldir.
        // Imza icindeki ASCII '-' karakterlerine asla dokunma.
        String value = raw.trim().replace('\u2011','-').replace('\u2010','-')
                .replace('\u2013','-').replace('\u2014','-');
        return value.replaceAll("\\s+", "").replace("\u00A0", "")
                .replace("\u200B", "").replace("\uFEFF", "");
    }

    public static boolean verify(Context c, String token) {
        return validationError(c, token) == null;
    }

    public static boolean isActivated(Context c) {
        String token = c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY_TOKEN, null);
        return verify(c, token);
    }

    public static boolean activate(Context c, String token) {
        if (!verify(c, token)) return false;
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY_TOKEN, cleanToken(token)).apply();
        return true;
    }

    public static void clear(Context c) {
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().remove(KEY_TOKEN).apply();
    }
}
