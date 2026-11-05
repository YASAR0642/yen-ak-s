package com.ktd.analyzer;

import android.content.Context;

/** User-measured survey-line length, stored per imported filename (not a global multiplier). */
public final class GprDistanceCalibration {
    private GprDistanceCalibration(){}
    private static String key(String file){return "line_m_"+file.trim().toLowerCase(java.util.Locale.ROOT);}
    public static double get(Context c,String file){
        if(file==null||file.trim().isEmpty())return Double.NaN;
        long bits=c.getSharedPreferences("gpr_distance",0).getLong(key(file),Long.MIN_VALUE);
        return bits==Long.MIN_VALUE?Double.NaN:Double.longBitsToDouble(bits);
    }
    public static void set(Context c,String file,double meters){
        if(!Double.isFinite(meters)||meters<=0||file==null)return;
        c.getSharedPreferences("gpr_distance",0).edit().putLong(key(file),Double.doubleToRawLongBits(meters)).apply();
    }
    public static void clear(Context c,String file){
        if(file!=null)c.getSharedPreferences("gpr_distance",0).edit().remove(key(file)).apply();
    }
}
