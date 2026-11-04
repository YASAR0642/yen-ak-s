package com.ktd.analyzer.view;

import android.graphics.Color;

/**
 * KTD renk paletleri.
 * 0-7: uygulamanın yerleşik paletleri.
 * 8-13: kullanıcı tarafından sağlanan CLR paletlerinin uygulamaya aktarılmış karşılıkları.
 */
public final class ColorMaps {
    public static final int COUNT=11;
    public static final String[] NAMES={
        "KTD Spectrum", "Siyah / Beyaz", "Neon", "Termal", "Buz",
        "Bakır", "Viridis", "Boşluk İçi Metal", "KTD VOLENDER", "ODA", "Malzeme"
    };
    private ColorMaps(){}

    private static final PaletteData P8=new PaletteData(
        new float[]{0.00000000f,0.37800688f,0.40893471f,0.42268041f,0.55326461f,0.59106530f,0.66323024f,0.67697595f,0.75601375f,0.79111842f,0.81907895f,0.85855263f,0.88160056f,0.89690722f,0.92096220f,0.93658338f,0.94736842f,0.94799930f,0.97532895f,1.00000000f},
        new int[]{Color.argb(255,0,0,0),Color.argb(255,255,0,0),Color.argb(255,25,25,25),Color.argb(255,255,255,102),Color.argb(255,255,255,0),Color.argb(255,0,0,0),Color.argb(255,255,255,0),Color.argb(255,0,255,0),Color.argb(255,0,0,255),Color.argb(255,255,0,0),Color.argb(255,255,0,0),Color.argb(255,255,0,0),Color.argb(255,255,0,0),Color.argb(255,255,0,0),Color.argb(255,0,0,0),Color.argb(255,0,0,255),Color.argb(255,255,0,0),Color.argb(255,0,0,0),Color.argb(255,255,0,0),Color.argb(255,255,0,0)},
        new float[]{0.00000000f,0.37800688f,0.40893471f,0.94166230f,0.94799930f,1.00000000f},
        new int[]{61,61,235,235,89,89}
    );
    private static final PaletteData P9=new PaletteData(
        new float[]{0.00000000f,0.37800688f,0.40893471f,0.42268041f,0.44901316f,0.47697368f,0.49013158f,0.50164474f,0.51151316f,0.53289474f,0.55756579f,0.57401316f,0.59106530f,0.62828947f,0.66323024f,0.67697595f,0.69736842f,0.71381579f,0.72861842f,0.75601375f,0.80098684f,0.85827946f,0.87500000f,0.88486842f,0.89638158f,0.92096220f,0.93658338f,0.94166230f,0.94572368f,0.94799930f,0.94901316f,0.95723684f,0.97203947f,0.97697368f,0.98848684f,1.00000000f},
        new int[]{Color.argb(255,230,230,230),Color.argb(255,255,0,0),Color.argb(255,25,25,25),Color.argb(255,0,0,255),Color.argb(255,50,50,205),Color.argb(255,103,103,152),Color.argb(255,137,137,118),Color.argb(255,0,255,0),Color.argb(255,168,168,87),Color.argb(255,208,208,47),Color.argb(255,255,255,0),Color.argb(255,0,255,255),Color.argb(255,0,0,0),Color.argb(255,0,0,255),Color.argb(255,255,0,0),Color.argb(255,0,0,0),Color.argb(255,255,255,255),Color.argb(255,64,180,165),Color.argb(255,102,51,51),Color.argb(255,0,0,255),Color.argb(255,255,0,0),Color.argb(255,255,0,0),Color.argb(255,255,0,0),Color.argb(255,255,0,0),Color.argb(255,255,0,0),Color.argb(255,255,0,0),Color.argb(255,204,102,51),Color.argb(255,153,51,102),Color.argb(255,186,89,77),Color.argb(255,204,110,63),Color.argb(255,195,116,60),Color.argb(255,126,166,39),Color.argb(255,0,255,0),Color.argb(255,204,162,142),Color.argb(255,204,183,173),Color.argb(255,204,204,204)},
        new float[]{0.00000000f,0.37800688f,0.40893471f,0.94166230f,0.94799930f,1.00000000f},
        new int[]{61,255,235,235,79,89}
    );
    private static final PaletteData P10=new PaletteData(
        new float[]{0.00000000f,0.00000000f,0.00000000f,0.00000000f,0.00000000f,0.00000000f,0.00764818f,0.03059273f,0.03059273f,0.03059273f,0.27061856f,0.32130584f,0.43403442f,0.43403442f,0.43403442f,0.43403442f,0.43785851f,0.54684512f,0.54684512f,0.62130584f,0.62130584f,0.62130584f,0.84512428f,0.84703633f,0.84703633f,0.87954111f,0.87954111f,0.89866157f,0.89866157f,0.97131931f,0.98470363f,1.00000000f},
        new int[]{Color.argb(255,255,255,255),Color.argb(255,255,255,255),Color.argb(255,255,255,255),Color.argb(255,255,255,255),Color.argb(255,255,255,255),Color.argb(255,255,255,255),Color.argb(255,204,51,0),Color.argb(255,255,255,255),Color.argb(255,255,255,0),Color.argb(255,255,255,255),Color.argb(255,0,51,51),Color.argb(255,102,51,255),Color.argb(255,255,255,255),Color.argb(255,255,255,255),Color.argb(255,255,255,255),Color.argb(255,255,255,255),Color.argb(255,229,229,229),Color.argb(255,0,0,0),Color.argb(255,0,255,0),Color.argb(255,0,0,0),Color.argb(255,0,0,0),Color.argb(255,0,0,0),Color.argb(255,221,221,221),Color.argb(255,255,255,255),Color.argb(255,30,255,30),Color.argb(255,0,255,0),Color.argb(255,147,147,96),Color.argb(255,0,255,0),Color.argb(255,255,0,0),Color.argb(255,166,0,0),Color.argb(255,153,0,0),Color.argb(255,255,0,0)},
        new float[]{0.00000000f,0.24812030f,0.31766917f,0.75563910f,1.00000000f},
        new int[]{20,20,13,228,228}
    );
    private static final PaletteData P11=new PaletteData(
        new float[]{0.00000000f,1.00000000f},
        new int[]{Color.argb(255,0,0,0),Color.argb(255,255,255,255)},
        new float[]{0.00000000f,1.00000000f},
        new int[]{255,255}
    );
    private static final PaletteData P12=new PaletteData(
        new float[]{0.00000000f,0.00000000f,0.00000000f,0.00000000f,0.00000000f,0.00000000f,0.00764818f,0.03059273f,0.03059273f,0.03059273f,0.03059273f,0.06309751f,0.07648184f,0.10325048f,0.12810707f,0.14340344f,0.17590822f,0.20650096f,0.54684512f,0.62130584f,0.62130584f,0.62130584f,0.84512428f,0.84703633f,0.84703633f,0.87954111f,0.87954111f,0.89866157f,0.89866157f,0.97131931f,0.98470363f,1.00000000f},
        new int[]{Color.argb(255,255,255,255),Color.argb(255,255,255,255),Color.argb(255,255,255,255),Color.argb(255,255,255,255),Color.argb(255,255,255,255),Color.argb(255,255,255,255),Color.argb(255,204,51,0),Color.argb(255,255,255,255),Color.argb(255,255,255,0),Color.argb(255,255,255,255),Color.argb(255,0,51,51),Color.argb(255,102,51,255),Color.argb(255,255,255,255),Color.argb(255,255,255,255),Color.argb(255,255,255,255),Color.argb(255,255,255,255),Color.argb(255,229,229,229),Color.argb(255,0,0,0),Color.argb(255,0,255,0),Color.argb(255,0,0,0),Color.argb(255,0,0,0),Color.argb(255,0,0,0),Color.argb(255,221,221,221),Color.argb(255,255,255,255),Color.argb(255,30,255,30),Color.argb(255,0,255,0),Color.argb(255,147,147,96),Color.argb(255,0,255,0),Color.argb(255,255,0,0),Color.argb(255,166,0,0),Color.argb(255,153,0,0),Color.argb(255,255,0,0)},
        new float[]{0.00000000f,0.20300752f,0.33082707f,0.75563910f,1.00000000f},
        new int[]{255,255,255,228,228}
    );
    private static final PaletteData P13=new PaletteData(
        new float[]{0.00000000f,0.37800688f,0.40893471f,0.42268041f,0.55326461f,0.59106530f,0.66323024f,0.67697595f,0.75601375f,0.85827946f,0.88160056f,0.89690722f,0.92096220f,0.93658338f,0.94166230f,0.94799930f,1.00000000f},
        new int[]{Color.argb(61,0,0,0),Color.argb(255,255,0,0),Color.argb(235,25,25,25),Color.argb(235,255,255,102),Color.argb(235,255,255,0),Color.argb(235,0,0,0),Color.argb(235,255,255,0),Color.argb(235,0,255,0),Color.argb(235,0,0,255),Color.argb(235,204,204,51),Color.argb(235,0,0,0),Color.argb(235,153,102,51),Color.argb(235,0,0,0),Color.argb(235,204,102,51),Color.argb(235,153,51,102),Color.argb(79,204,110,63),Color.argb(89,204,204,204)},
        null,
        null
    );

    public static int color(int palette,float value,float maxAbs){
        if(maxAbs<1f)maxAbs=1f;
        float t=Math.max(-1f,Math.min(1f,value/maxAbs));
        float u=(t+1f)*.5f;
        switch(palette){
            case 1: return opaque(gray(t));
            case 2: return opaque(diverging(t,Color.rgb(90,0,160),Color.rgb(0,235,255),Color.rgb(40,255,95),Color.rgb(255,245,0),Color.rgb(255,0,145)));
            case 3: return opaque(diverging(t,Color.rgb(10,0,40),Color.rgb(80,0,140),Color.rgb(225,35,20),Color.rgb(255,145,0),Color.rgb(255,250,210)));
            case 4: return opaque(diverging(t,Color.rgb(0,20,90),Color.rgb(0,80,220),Color.rgb(0,220,255),Color.rgb(170,250,255),Color.WHITE));
            case 5: return opaque(diverging(t,Color.rgb(45,15,5),Color.rgb(120,50,15),Color.rgb(205,105,25),Color.rgb(250,185,70),Color.rgb(255,242,195)));
            case 6: return opaque(diverging(t,Color.rgb(68,1,84),Color.rgb(59,82,139),Color.rgb(33,145,140),Color.rgb(94,201,98),Color.rgb(253,231,37)));
            case 7: return opaque(P8.color(u));
            case 8: return opaque(P9.color(u));
            case 9: return opaque(P10.color(u));
            case 10:return opaque(P12.color(u));
            default:return opaque(diverging(t,Color.rgb(20,55,170),Color.rgb(10,145,210),Color.rgb(75,215,55),Color.rgb(245,225,35),Color.rgb(255,45,20)));
        }
    }

    public static int recommendedAlpha(int palette){ return 255; }

    private static int gray(float t){int v=(int)(255f*(t+1f)*.5f);v=Math.max(0,Math.min(255,v));return Color.rgb(v,v,v);}
    private static int opaque(int c){return Color.rgb(Color.red(c),Color.green(c),Color.blue(c));}
    private static int withAlpha(int c,int a){return Color.argb(a,Color.red(c),Color.green(c),Color.blue(c));}
    private static int diverging(float t,int c0,int c1,int c2,int c3,int c4){float u=(t+1f)*2f;int i=Math.min(3,(int)Math.floor(u));float f=u-i;int a=i==0?c0:i==1?c1:i==2?c2:c3;int b=i==0?c1:i==1?c2:i==2?c3:c4;return lerp(a,b,f);}
    private static int lerp(int a,int b,float t){t=Math.max(0f,Math.min(1f,t));int r=(int)(Color.red(a)+(Color.red(b)-Color.red(a))*t),g=(int)(Color.green(a)+(Color.green(b)-Color.green(a))*t),bl=(int)(Color.blue(a)+(Color.blue(b)-Color.blue(a))*t),al=(int)(Color.alpha(a)+(Color.alpha(b)-Color.alpha(a))*t);return Color.argb(al,r,g,bl);}

    private static final class PaletteData {
        final float[] x; final int[] c; final float[] ax; final int[] av;
        PaletteData(float[] x,int[] c,float[] ax,int[] av){this.x=x;this.c=c;this.ax=ax;this.av=av;}
        int color(float u){
            int col=sampleColor(x,c,u);
            int alpha=Color.alpha(col);
            if(ax!=null&&av!=null&&ax.length>0&&av.length>0) alpha=(alpha*sampleAlpha(ax,av,u))/255;
            return Color.argb(alpha,Color.red(col),Color.green(col),Color.blue(col));
        }
    }
    private static int sampleColor(float[] x,int[] c,float u){
        if(x==null||c==null||x.length==0||c.length==0) return Color.WHITE;
        if(u<=x[0]) return c[0];
        int n=Math.min(x.length,c.length);
        for(int i=1;i<n;i++){if(u<=x[i]){float d=x[i]-x[i-1];if(Math.abs(d)<1e-7f)return c[i];return lerp(c[i-1],c[i],(u-x[i-1])/d);}}
        return c[n-1];
    }
    private static int sampleAlpha(float[] x,int[] a,float u){
        if(x==null||a==null||x.length==0||a.length==0)return 255;
        if(u<=x[0])return a[0];
        int n=Math.min(x.length,a.length);
        for(int i=1;i<n;i++){if(u<=x[i]){float d=x[i]-x[i-1];if(Math.abs(d)<1e-7f)return a[i];float f=(u-x[i-1])/d;return Math.max(0,Math.min(255,(int)(a[i-1]+(a[i]-a[i-1])*f)));}}
        return a[n-1];
    }
}
