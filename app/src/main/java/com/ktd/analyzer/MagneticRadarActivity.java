package com.ktd.analyzer;

import android.app.Activity;
import android.graphics.Color;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import com.ktd.analyzer.bluetooth.BluetoothClient;
import com.ktd.analyzer.data.ReferenceModel;
import com.ktd.analyzer.data.SensorFrame;
import com.ktd.analyzer.view.MagneticRadarView;
import java.util.Locale;

/** Landscape magnetic display styled like the app's live GPR screen. */
public final class MagneticRadarActivity extends Activity implements BluetoothClient.Listener {
    private BluetoothClient bluetooth;
    private final ReferenceModel reference = new ReferenceModel();
    private MagneticRadarView radar;
    private TextView status, boostLabel;
    private boolean running = false, zeroed = false, mineral = false;
    private float boost = 2f;
    private int paletteIndex = 0;
    private final TextView[] traceLabels = new TextView[4];
    private final float[] traceGain = {1f, 1f, 1f, 1f};
    private final float[] traceLevel = {0, 0, 0, 0};

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        bluetooth = new BluetoothClient(this);
        build();
    }

    private int dp(int a) { return Ui.dp(this, a); }
    private Button button(String t) { Button b = Ui.button(this, t, false); b.setTextSize(11); return b; }
    private TextView txt(String s, int size) { TextView t = Ui.text(this, s, size, Color.WHITE); t.setGravity(Gravity.CENTER); return t; }

    private void build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(5, 15, 23));
        root.setPadding(dp(5), dp(3), dp(5), dp(3));

        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        Button back = button("‹ NORMAL");
        back.setOnClickListener(v -> finish());
        head.addView(back, new LinearLayout.LayoutParams(dp(105), dp(38)));

        TextView title = txt("KTD 3D ANALYZER GPR MOD", 18);
        head.addView(title, new LinearLayout.LayoutParams(0, dp(40), 1));

        status = txt("○ BLUETOOTH", 12);
        status.setPadding(dp(10), 0, dp(10), 0);
        status.setBackgroundColor(Color.rgb(36, 48, 60));
        head.addView(status, new LinearLayout.LayoutParams(dp(170), dp(38)));
        root.addView(head, new LinearLayout.LayoutParams(-1, dp(43)));

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.HORIZONTAL);

        radar = new MagneticRadarView(this);
        radar.setBoost(boost);
        body.addView(radar, new LinearLayout.LayoutParams(0, -1, 67));

        LinearLayout midWrap = new LinearLayout(this);
        midWrap.setOrientation(LinearLayout.HORIZONTAL);
        midWrap.setBackgroundColor(Color.rgb(8, 23, 32));

        LinearLayout meterCol = new LinearLayout(this);
        meterCol.setOrientation(LinearLayout.VERTICAL);
        meterCol.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView meterTitle = txt("MT", 11);
        meterTitle.setTextColor(Color.rgb(142, 205, 250));
        meterCol.addView(meterTitle, new LinearLayout.LayoutParams(-1, dp(28)));
        for (int i = 0; i <= 4; i++) {
            TextView mt = txt(String.format(Locale.US, "%d", i), 11);
            mt.setTextColor(Color.rgb(210, 230, 238));
            mt.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
            meterCol.addView(mt, new LinearLayout.LayoutParams(-1, 0, 1));
        }
        midWrap.addView(meterCol, new LinearLayout.LayoutParams(dp(28), -1));

        LinearLayout middle = new LinearLayout(this);
        middle.setOrientation(LinearLayout.VERTICAL);
        boostLabel = txt("KAZANÇ 2.0×", 11);
        middle.addView(boostLabel, new LinearLayout.LayoutParams(-1, dp(28)));
        SeekBar boostBar = new SeekBar(this);
        boostBar.setMax(100);
        boostBar.setProgress(20);
        boostBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean user) {
                boost = Math.max(0f, p / 10f);
                boostLabel.setText(String.format(Locale.US, "KAZANÇ %.1f×", boost));
                radar.setBoost(boost);
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        middle.addView(boostBar, new LinearLayout.LayoutParams(-1, dp(30)));

        for (int i = 0; i < 4; i++) {
            final int id = i;
            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            traceLabels[i] = txt("G" + (i + 1) + " 1.0× • 0", 11);
            cell.addView(traceLabels[i], new LinearLayout.LayoutParams(-1, dp(22)));
            SeekBar bar = new SeekBar(this);
            bar.setMax(100);
            bar.setProgress(10);
            bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar s, int p, boolean user) {
                    traceGain[id] = p / 10f;
                    refreshTrace(id);
                }
                public void onStartTrackingTouch(SeekBar s) {}
                public void onStopTrackingTouch(SeekBar s) {}
            });
            cell.addView(bar, new LinearLayout.LayoutParams(-1, dp(28)));
            middle.addView(cell, new LinearLayout.LayoutParams(-1, 0, 1));
        }
        midWrap.addView(middle, new LinearLayout.LayoutParams(0, -1, 1));
        body.addView(midWrap, new LinearLayout.LayoutParams(0, -1, 22));

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.VERTICAL);
        Button start = button("BAŞLAT");
        start.setOnClickListener(v -> {
            if (!BluetoothClient.isConnected()) return;
            bluetooth.send("LIVE_START\n");
            running = true;
            start.setText("DEVAM");
        });
        controls.addView(start, new LinearLayout.LayoutParams(-1, 0, 1));

        Button stop = button("DURDUR");
        stop.setOnClickListener(v -> {
            bluetooth.send("LIVE_STOP\n");
            running = false;
        });
        controls.addView(stop, new LinearLayout.LayoutParams(-1, 0, 1));

        Button reset = button("SIFIRLA");
        reset.setOnClickListener(v -> {
            zeroed = false;
            reference.clear();
            radar.clear();
        });
        controls.addView(reset, new LinearLayout.LayoutParams(-1, 0, 1));

        Button mineralBtn = button("MİNERAL: KAPALI");
        mineralBtn.setOnClickListener(v -> {
            mineral = !mineral;
            radar.setMineral(mineral);
            mineralBtn.setText(mineral ? "MİNERAL: AÇIK" : "MİNERAL: KAPALI");
        });
        controls.addView(mineralBtn, new LinearLayout.LayoutParams(-1, 0, 1));

        Button colorBtn = button("RENK");
        colorBtn.setOnClickListener(v -> {
            paletteIndex = (paletteIndex + 1) % 4;
            radar.setPalette(paletteIndex);
        });
        controls.addView(colorBtn, new LinearLayout.LayoutParams(-1, 0, 1));

        body.addView(controls, new LinearLayout.LayoutParams(0, -1, 11));
        root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        Ui.applySafeInsets(this, root);
        updateStatusUi();
    }

    private void refreshTrace(int i) {
        if (traceLabels[i] != null) {
            traceLabels[i].setText(String.format(Locale.US, "G%d %.1f× • %.1f", i + 1, traceGain[i], traceLevel[i] * traceGain[i]));
        }
    }

    @Override protected void onStart() {
        super.onStart();
        bluetooth.activate();
        if (BluetoothClient.isConnected()) {
            bluetooth.send("RATE=20\n");
            bluetooth.send("LIVE_START\n");
            running = true;
        }
        updateStatusUi();
    }

    @Override protected void onStop() {
        if (BluetoothClient.isConnected()) bluetooth.send("LIVE_STOP\n");
        running = false;
        bluetooth.release();
        super.onStop();
    }

    private void updateStatusUi() {
        if (status == null) return;
        boolean connected = BluetoothClient.isConnected();
        status.setText(connected ? "● BLUETOOTH BAĞLI" : "○ BLUETOOTH YOK");
        status.setTextColor(Color.WHITE);
        status.setBackgroundColor(connected ? Color.rgb(48, 196, 104) : Color.rgb(120, 48, 48));
    }

    @Override public void onState(String state) { runOnUiThread(this::updateStatusUi); }

    @Override public void onFrame(SensorFrame f) {
        if (!running || f == null) return;
        float v = f.signal();
        if (!Float.isFinite(v) || v < 0 || v > 1023) return;
        runOnUiThread(() -> {
            if (!running) return;
            float z = ReferenceModel.normalize(v);
            if (!zeroed) {
                reference.zero(z);
                zeroed = true;
                return;
            }
            float delta = reference.visualDelta(z);
            radar.append(delta);
            for (int i = 0; i < 4; i++) {
                traceLevel[i] = traceLevel[i] * (.45f + i * .13f) + delta * (.55f - i * .13f);
                refreshTrace(i);
            }
        });
    }
}
