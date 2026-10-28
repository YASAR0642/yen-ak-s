package com.ktd.analyzer.view;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.util.ArrayList;

/** Magnetic field waterfall styled like a live radargram panel. */
public final class MagneticRadarView extends View {
    private final ArrayList<Float> samples = new ArrayList<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float boost = 2f, gain = 1f;
    private boolean mineral = false;
    private int palette = 0;
    private static final int MAX = 700;
    public MagneticRadarView(Context c) { super(c); setBackgroundColor(Color.rgb(3,10,17)); }
    public void append(float d) { if (!Float.isFinite(d)) return; samples.add(d); if (samples.size() > MAX) samples.remove(0); postInvalidateOnAnimation(); }
    public void clear() { samples.clear(); invalidate(); }
    public void setBoost(float f) { boost = Math.max(0f, Math.min(10f, f)); invalidate(); }
    public void setMineral(boolean value) { mineral = value; invalidate(); }
    public void setGain(float f) { gain = Math.max(0f, Math.min(10f, f)); invalidate(); }
    public void setPalette(int value) { palette = value; invalidate(); }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        int w = getWidth(), h = getHeight();
        if (w < 16 || h < 16) return;
        c.drawColor(Color.rgb(3,10,17));
        int left = 12, top = 18, right = w - 8, bottom = h - 8;
        if (right <= left || bottom <= top) return;

        int visible = Math.min(240, Math.max(1, samples.size()));
        int width = right - left, height = bottom - top;
        int cols = Math.min(240, Math.max(1, width));
        int rows = Math.min(260, Math.max(1, height));
        Bitmap bitmap = Bitmap.createBitmap(cols, rows, Bitmap.Config.RGB_565);
        int start = Math.max(0, samples.size() - visible);

        for (int x = 0; x < cols; x++) {
            // left-to-right flow: newest traces appear on the left and move toward the right.
            int reverseX = cols - 1 - x;
            int index = start + (int)((long)reverseX * visible / cols);
            if (index >= samples.size()) index = samples.size() - 1;
            float value = index < 0 ? 0 : samples.get(index);
            float base = 0; int n = 0;
            for (int j = Math.max(0, index - 30); j <= index && index >= 0; j++) { base += samples.get(j); n++; }
            if (n > 0) base /= n;
            float broad = 0; int count = 0;
            for (int j = Math.max(0, index - 8); j <= index && index >= 0; j++) { broad += samples.get(j); count++; }
            if (count > 0) broad /= count;
            float source = mineral ? value - .8f * base : value;
            float center = mineral ? broad - .8f * base : broad;
            float threshold = 1.0f;
            float boosted = source + (boost - 1f) * Math.signum(center) * Math.max(0f, Math.abs(center) - threshold);

            for (int y = 0; y < rows; y++) {
                float yy = (float) y / Math.max(1, rows - 1);
                float window = (float) Math.exp(-Math.pow((yy - .5f) / .30f, 2));
                float v = boosted * window * gain;
                int color = (index < 0 || gain == 0f || Math.abs(v) < .55f) ? Color.rgb(5,15,23) : ColorMaps.color(palette, v, 32f);
                bitmap.setPixel(x, y, color);
            }
        }

        paint.setFilterBitmap(true);
        c.drawBitmap(bitmap, null, new Rect(left, top, right, bottom), paint);
        bitmap.recycle();
    }
}
