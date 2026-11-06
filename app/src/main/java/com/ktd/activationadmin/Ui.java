package com.ktd.activationadmin;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.TextView;

final class Ui {
    private Ui() {}
    static int dp(Context c, int v) { return Math.round(v * c.getResources().getDisplayMetrics().density); }
    static TextView text(Context c, String s, float sp, int color) {
        TextView v = new TextView(c); v.setText(s); v.setTextSize(sp); v.setTextColor(color); v.setGravity(Gravity.CENTER_VERTICAL); return v;
    }
    static GradientDrawable bg(int fill, int stroke, int radiusDp, Context c) {
        GradientDrawable g = new GradientDrawable(); g.setColor(fill); g.setCornerRadius(dp(c,radiusDp)); if(stroke!=0)g.setStroke(dp(c,1),stroke); return g;
    }
    static void bold(TextView v){v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);}
    static int rgb(String hex){return Color.parseColor(hex);}
}
