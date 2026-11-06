package com.ktd.analyzer;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.ScrollView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public final class ActivationActivity extends Activity {
    private EditText codeInput;
    private TextView status;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(3, 8, 12));
        getWindow().setNavigationBarColor(Color.BLACK);

        // Keep the activation action visible on displays with large font/display scaling.
        // The form above it scrolls independently instead of pushing the button off-screen.
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(3, 8, 12));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(true);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setGravity(Gravity.CENTER_HORIZONTAL);
        form.setPadding(Ui.dp(this, 24), Ui.dp(this, 20), Ui.dp(this, 24), Ui.dp(this, 8));
        scroll.addView(form, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ktd_logo);
        logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        form.addView(logo, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 150)));

        TextView title = Ui.text(this, "KTD GPR SYSTEM", 24, Color.WHITE);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        form.addView(title, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 48)));

        TextView sub = Ui.text(this, "UYGULAMA AKTİVASYONU • v1.0.13", 15, Color.rgb(72, 214, 235));
        sub.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        sub.setGravity(Gravity.CENTER);
        form.addView(sub, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 38)));

        TextView info = Ui.text(this, "Bu cihaz için aktivasyon gereklidir. Cihaz kodunu kopyalayıp yetkili aktivasyon kodunu girin.", 13, Color.rgb(170, 195, 208));
        info.setGravity(Gravity.CENTER);
        info.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 14));
        form.addView(info, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView devLabel = Ui.text(this, "CİHAZ KODU", 12, Color.rgb(150, 180, 198));
        devLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        form.addView(devLabel);

        String device = LicenseManager.deviceCode(this);
        TextView deviceBox = Ui.text(this, device, 20, Color.WHITE);
        deviceBox.setGravity(Gravity.CENTER);
        deviceBox.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        deviceBox.setTextIsSelectable(true);
        deviceBox.setBackground(Ui.bg(Color.rgb(13, 30, 39), 14, this));
        LinearLayout.LayoutParams dbp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 58));
        dbp.setMargins(0, Ui.dp(this, 6), 0, Ui.dp(this, 8));
        form.addView(deviceBox, dbp);

        TextView copy = Ui.text(this, "CİHAZ KODUNU KOPYALA", 13, Color.rgb(3, 20, 18));
        copy.setGravity(Gravity.CENTER);
        copy.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        copy.setBackground(Ui.bg(Color.rgb(44, 220, 117), 13, this));
        copy.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("KTD Cihaz Kodu", device));
            Toast.makeText(this, "Cihaz kodu kopyalandı", Toast.LENGTH_SHORT).show();
        });
        form.addView(copy, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 48)));

        TextView paste = Ui.text(this, "AKTİVASYON KODUNU PANODAN YAPIŞTIR", 13, Color.rgb(3, 20, 18));
        paste.setGravity(Gravity.CENTER);
        paste.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        paste.setBackground(Ui.bg(Color.rgb(245, 193, 74), 13, this));
        LinearLayout.LayoutParams pasteLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 46));
        pasteLp.setMargins(0, Ui.dp(this, 10), 0, 0);
        form.addView(paste, pasteLp);
        paste.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip() != null
                    && cm.getPrimaryClip().getItemCount() > 0) {
                CharSequence content = cm.getPrimaryClip().getItemAt(0).coerceToText(this);
                if (content != null) {
                    codeInput.setText(content.toString());
                    codeInput.setSelection(codeInput.length());
                    Toast.makeText(this, "Kod panodan alındı", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            Toast.makeText(this, "Panoda kod bulunamadı", Toast.LENGTH_SHORT).show();
        });

        TextView actLabel = Ui.text(this, "AKTİVASYON KODU (16 HANELİ)", 12, Color.rgb(150, 180, 198));
        actLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 34));
        alp.setMargins(0, Ui.dp(this, 16), 0, 0);
        form.addView(actLabel, alp);

        codeInput = new EditText(this);
        codeInput.setTextColor(Color.WHITE);
        codeInput.setHintTextColor(Color.rgb(100, 128, 145));
        codeInput.setHint("Örn: 4522-5677-1851-2439");
        codeInput.setTextSize(13);
        codeInput.setSingleLine(false);
        codeInput.setMinLines(3);
        codeInput.setMaxLines(5);
        codeInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        codeInput.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10));
        codeInput.setBackground(Ui.bg(Color.rgb(13, 30, 39), 14, this));
        form.addView(codeInput, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 100)));

        status = Ui.text(this, "", 12, Color.rgb(255, 95, 95));
        status.setGravity(Gravity.CENTER);
        form.addView(status, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 34)));

        TextView activate = Ui.text(this, "AKTİVE ET", 17, Color.rgb(2, 18, 16));
        activate.setGravity(Gravity.CENTER);
        activate.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        activate.setBackground(Ui.bg(Color.rgb(36, 211, 183), 15, this));
        activate.setOnClickListener(v -> doActivate());
        // Fixed bottom action, outside the scrollable form.
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 56));
        actionParams.setMargins(Ui.dp(this, 24), Ui.dp(this, 8), Ui.dp(this, 24), Ui.dp(this, 16));
        root.addView(activate, actionParams);

        setContentView(root);
        Ui.applySafeInsets(this, root);
    }

    private void doActivate() {
        String token = codeInput.getText().toString().replace("\n", "").replace("\r", "").trim();
        // Önce cihaz-bagli 16 haneli aktivasyon, sonra eski imzali lisans destegi.
        if (LicenseManager.activate(this, token)) {
            status.setTextColor(Color.rgb(44, 220, 117));
            status.setText("AKTİVASYON BAŞARILI");
            Intent next = new Intent(this, LiveGprActivity.class);
            if (getIntent() != null && getIntent().getData() != null) {
                next.setData(getIntent().getData());
                next.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            startActivity(next);
            finish();
        } else {
            status.setTextColor(Color.rgb(255, 90, 90));
            String error = LicenseManager.validationError(this, token);
            status.setText(error == null ? "Aktivasyon kaydedilemedi" : error);
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Aktivasyon neden kabul edilmedi?")
                    .setMessage(error == null ? "Kod doğrulandı, ancak aktivasyon kaydedilemedi." : error)
                    .setPositiveButton("TAMAM", null).show();
        }
    }
}
