package com.ktd.activationadmin;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

final class SecurityStore {
    private SecurityStore() {}
    private static final String PREF = "ktd_admin_security";
    private static final String K_SALT = "pin_salt";
    private static final String K_HASH = "pin_hash";
    private static final String K_BIO = "bio_enabled";

    static boolean hasPin(Context c) {
        return prefs(c).contains(K_HASH);
    }

    static boolean setPin(Context c, String pin) {
        if (!validPin(pin)) return false;
        try {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            byte[] hash = derive(pin, salt);
            prefs(c).edit()
                    .putString(K_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
                    .putString(K_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
                    .apply();
            return true;
        } catch (Exception e) { return false; }
    }

    static boolean verifyPin(Context c, String pin) {
        try {
            String s = prefs(c).getString(K_SALT, null);
            String h = prefs(c).getString(K_HASH, null);
            if (s == null || h == null) return false;
            byte[] salt = Base64.decode(s, Base64.NO_WRAP);
            byte[] expected = Base64.decode(h, Base64.NO_WRAP);
            byte[] actual = derive(pin, salt);
            return MessageDigest.isEqual(expected, actual);
        } catch (Exception e) { return false; }
    }

    static boolean validPin(String pin) {
        return pin != null && pin.matches("[0-9]{6,10}");
    }

    static boolean biometricEnabled(Context c) { return prefs(c).getBoolean(K_BIO, true); }
    static void setBiometricEnabled(Context c, boolean v) { prefs(c).edit().putBoolean(K_BIO, v).apply(); }

    private static byte[] derive(String pin, byte[] salt) throws Exception {
        KeySpec spec = new PBEKeySpec(pin.toCharArray(), salt, 120000, 256);
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }
}
