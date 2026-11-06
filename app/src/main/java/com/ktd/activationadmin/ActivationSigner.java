package com.ktd.activationadmin;

import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Locale;

final class ActivationSigner {
    private ActivationSigner() {}

    private static final String[] KEY_PARTS = new String[]{
            "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCvqrxTNwT1bCP6D7O40Xulg3gFJUmV44Dw1MeNi+ICMm8w1Rn8bNTWYh8E5j0ZZdOulpR89wXKFMqLBiBFHThGUz9ECSqdLnpTP2Hu7c27qBMLUAL6+6sJJXw6pCw3lOMs",
            "qJ2wmgvzT3/iJAEANZwXuk21ZYGyyfy43agGjawrrNGzIgLjeVgbM1/dvvtsImOboAOQz8EvU3mXHsJ8aFJBD/GRTMxmjW7EhnovJrXDr4Oqu/2JA2KjguQswRzAg6GY5LaOAlFfehyeKw+74wAS0cVQ/o34edDtuuEtooU4adx9yRnhI7TS",
            "0xmIvOmvGZrALLLGiAzJgPb/ALMGOCl7AgMBAAECggEAI6M4HIuzalS7gb4QwGRjOMNQpH6ZjJqAbJtGNp2Txs1TiG0bHf44OZvjXhuVdqMVTDyJOEbm+kW4h3bBMjRI19+InLVdW1D0CRJ5PD8rNzBUWL2tNRy8aJkduEnZHJeMdOFqSEsg",
            "NIbC025pc5Dsr1Wg8TV8F/jpV9nD/jxcmulY7SdCSQUPGnb9N6fPjbUsouI82h3QxdTVWB6aYWrbIIENQkZtQMCZ1pzRYQZahKjXFHvRel7hO4QVtbzLgi+ZV+62gOFjCjXJ8bep49YSNS+2tlJ7/knHoE6VY5STUGdNrZXWYcCFm3tveU9d",
            "9tHKIt9GSffGc09ra3bA6l1akQKBgQDdF4M75akShOS1PBEPpq3zCX3bpG4cGsoWavMSr4MVWIcxft64pnezEsOMZAE89voTnYVxGCuTp0D+lSC6idlifESQJpHfQDvbAPquqdPQAmRRvZhd6xPVmti6YEMsKkDXlHhGrZB3FG5wbieupqL1",
            "fu06nhkG4Te2zv0GcP1bsQKBgQDLZyhLi2y+A8mQuzXiMRfvECNfw73FUbxInJcAnw4kvxhCd7wTvzO1/at6Ffmb5o0asIhpEP8Ew/0/7cgBRE4LjpLMVD6WYuNVO//HRi6v/yNt0WUzG8uqx6sZSr10+ZWaqCXqq9AFagmu6fy/dAZLq2j6",
            "sKizsD0HfCJb1hpe6wKBgDCri2U1ImoEPUC0T2EtpR9aWI1wfhjLwO78bjQP1QBu85vuvOLTKoiOOeP+49Li35xP0d9ancUsTX99nq3RyLwIdSNyiiwmPMbyST0H/6vLyv+/h/os1LnyE4G7zS28w3dxpKSScrn0EnfHmA63eiom3dxIj2PX",
            "Umd423R+jReRAoGBALMAjqKlCcbTOHEJgCsWP1Tg4/CkJ37eJo/UVsNyNpU6IxRjoP3T16NCocToT+nWZLlnQDUOgw9jl5zKGqiavwBGsYPmY8ofLqdVDQ0HA5FZPImprKDkxkD+ROIWN1MZToJzliLGn2QUZH8tZoPqjJvNNq7gBydTYvIS",
            "WX5ulniPAoGAJBcTfh/kNhm9CmqIAN1Iq8aK3XNMg0t6OD3Ram+aRY8SGxZvyXHTnO+SdDoX7tnvnOZ39FbDisnDHo9HB+qVv7/pXN5s4uuzqG1sIGSr4XCzrUN+AcHS1M3W6D/cSnS0ALdyEXtvYEaeGFpu4zS17D18bfXr5YNxoU2Gzbv+",
            "dE8="
    };

    static String normalizeDevice(String raw) {
        if (raw == null) return "";
        return raw.toUpperCase(Locale.US).replaceAll("[^0-9A-F]", "");
    }

    static boolean isValidDevice(String device) {
        return device != null && device.matches("[0-9A-F]{16}");
    }

    static String generate(String rawDevice) throws Exception {
        String device = normalizeDevice(rawDevice);
        if (!isValidDevice(device)) throw new IllegalArgumentException("16 haneli cihaz kodu gerekli");
        StringBuilder sb = new StringBuilder();
        for (String s : KEY_PARTS) sb.append(s);
        byte[] der = Base64.decode(sb.toString(), Base64.DEFAULT);
        PrivateKey key = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(key);
        sig.update(("KTD|" + device + "|LIFETIME").getBytes(StandardCharsets.UTF_8));
        byte[] signature = sig.sign();
        String enc = Base64.encodeToString(signature, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
        return "KTD1-" + device + "-" + enc;
    }
}
