package com.ktd.analyzer.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

import com.ktd.analyzer.data.SensorFrame;

import java.util.List;

/**
 * FINAL hızlı canlı tarama.
 * Her gerçek Bluetooth paketi alttan girer ve yukarı akar. Anomali merkezi sabittir;
 * yalnız yatayda sağa/sola genişler. Böylece dikey boy sahte olarak büyütülmez.
 * Renkler 3D yüzeyle aynı ColorMaps paletinden, opak ve canlı çizilir.
 */
public final class UltrasoundView extends View {
    private static final int N = 220;
    private final float[] d = new float[N];
    private final boolean[] used = new boolean[N];
    private int head = 0, count = 0, palette = 0;
    private float maxAbs = 18f;
    private float displayGain=1f;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    public UltrasoundView(Context c) {
        super(c);
        setBackgroundColor(Color.rgb(3,9,14));
    }

    public void setGain(float v){displayGain=Math.max(.2f,Math.min(3f,v));invalidate();}
    public void setPalette(int v) { palette = v; invalidate(); }

    public void clear() {
        for (int i = 0; i < N; i++) used[i] = false;
        head = 0; count = 0; maxAbs = 18f; invalidate();
    }

    public void add(SensorFrame f,float delta){append(delta);postInvalidateOnAnimation();}
    public void addBatch(List<SensorFrame> frames,List<Float> deltas){
        if(deltas==null)return;
        int n=frames==null?deltas.size():Math.min(frames.size(),deltas.size());
        for(int i=0;i<n;i++)append(deltas.get(i));
        if(n>0)postInvalidateOnAnimation();
    }

    private void append(float delta){
        d[head]=delta; used[head]=true;
        head=(head+1)%N; count=Math.min(N,count+1);
        // Hızlı tepki: eski tepe değeri çabuk sönsün, yeni anomali renk skalasını hemen kullansın.
        maxAbs=Math.max(18f,maxAbs*.975f);
        maxAbs=Math.max(maxAbs,Math.abs(delta));
    }

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        int w=getWidth(),h=getHeight(); if(w<=0||h<=0)return;

        // Dikey ölçek sabit tutulur; kullanıcı yalnız yatay genişleme istedi.
        float rowH=Math.max(10f,h/46f);
        int rows=Math.min(count,(int)(h/rowH)+2);
        int bands=192;
        float bandW=w/(float)bands;

        for(int j=0;j<rows;j++){
            int idx=(head-1-j+N)%N; if(!used[idx])continue;
            float y2=h-j*rowH, y1=y2-rowH-1f;
            float amp=d[idx];
            float strength=Math.min(1f,Math.abs(amp)*displayGain/Math.max(1f,maxAbs));

            // Zemin koyu kalır; 3D gibi canlı renk yalnız gerçek anomali alanında görünür.
            p.setColor(Color.rgb(5,14,18)); p.setAlpha(255); c.drawRect(0,y1,w,y2,p);

            // Hedef güçlü oldukça yalnız SAĞ/SOL yönde genişler. Güçlü anomali yaklaşık
            // ekran genişliğinin %50-58'ine kadar çıkabilir; merkez ekranda sabittir.
            float spread=.15f+.12f*strength;
            final float center=.5f;
            for(int xb=0;xb<bands;xb++){
                float xn=(xb+.5f)/bands;
                float q=(xn-center)/Math.max(.045f,spread);
                float envelope=(float)Math.exp(-q*q*3.15f);
                if(envelope<.012f)continue;

                float local=amp*envelope*displayGain;
                int col=ColorMaps.color(palette,local,maxAbs);
                p.setColor(Color.rgb(Color.red(col),Color.green(col),Color.blue(col)));
                p.setAlpha(255);
                c.drawRect(xb*bandW,y1,(xb+1)*bandW+1f,y2,p);
            }
        }
        // FINAL: canlı ultrason ekranında grid çizgileri yok.
    }
}
