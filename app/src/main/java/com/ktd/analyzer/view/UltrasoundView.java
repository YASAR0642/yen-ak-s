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
    private boolean gprStyle=false;
    public void setGprStyle(boolean value){gprStyle=value;invalidate();}
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
        final int rows=Math.min(count,150), bands=160;
        if(rows==0)return;
        // Her piksel komşu örneklerin düzgün ağırlıklı ortalamasıyla hesaplanır;
        // dikdörtgen bloklardan kaynaklanan keskin basamakları kaldırır.
        android.graphics.Bitmap bm=android.graphics.Bitmap.createBitmap(bands,rows,android.graphics.Bitmap.Config.ARGB_8888);
        for(int y=0;y<rows;y++){
            float pos=y*Math.max(0,count-1f)/Math.max(1,rows-1);
            int j=(int)pos;float frac=pos-j;
            float a=d[(head-1-j+N)%N];
            float b=d[(head-1-Math.min(count-1,j+1)+N)%N];
            float amp=a+(b-a)*frac;
            for(int x=0;x<bands;x++){
                float nx=(x+.5f)/bands;
                float magnitude=Math.min(1f,Math.abs(amp)*displayGain/Math.max(1f,maxAbs));
                float breadth=.14f+.16f*magnitude;
                float q=(nx-.5f)/Math.max(.05f,breadth);
                float envelope=(float)Math.exp(-2.6f*q*q);
                float local=amp*envelope*displayGain;
                int col;
                if(gprStyle){
                    // Radargram benzeri dikey iz: aynı manyetik örnek; radar/SGY ölçümü değildir.
                    float intensity=Math.min(1f,Math.abs(local)/Math.max(1f,maxAbs));
                    int base=ColorMaps.color(palette,local,maxAbs);
                    col=intensity<.025f?Color.rgb(5,14,18):base;
                }else{
                    col=ColorMaps.color(palette,local,maxAbs);
                    if(envelope<.02f)col=Color.rgb(5,14,18);
                }
                bm.setPixel(x,y,col);
            }
        }
        p.setFilterBitmap(true);
        c.drawBitmap(bm,null,new android.graphics.Rect(0,0,w,h),p);
        bm.recycle();
    }
}
