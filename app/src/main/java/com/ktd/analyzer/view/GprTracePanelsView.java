package com.ktd.analyzer.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import com.ktd.analyzer.gpr.GprProtocol;
import java.util.List;

/** Four A-scan trace profiles, sampled from the actual visible radargram. */
public final class GprTracePanelsView extends View {
    private final GprRadargramView radar;
    private final Paint pen=new Paint(Paint.ANTI_ALIAS_FLAG);
    public GprTracePanelsView(Context c,GprRadargramView r){super(c);radar=r;}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        float dp=getResources().getDisplayMetrics().density;
        int w=getWidth(),h=getHeight();
        pen.setStyle(Paint.Style.FILL);pen.setColor(Color.rgb(1,6,9));canvas.drawRect(0,0,w,h,pen);
        List<GprProtocol.Trace> tr=radar.snapshot();
        for(int box=0;box<4;box++){
            float y0=box*h/4f,y1=(box+1)*h/4f;
            pen.setStyle(Paint.Style.STROKE);pen.setStrokeWidth(dp*.7f);pen.setColor(Color.rgb(60,94,110));
            canvas.drawRect(1,y0+1,w-1,y1-1,pen);
            float centerX=w*.5f;
            pen.setColor(Color.rgb(138,50,49));canvas.drawLine(centerX,y0+6*dp,centerX,y1-8*dp,pen);
            pen.setStyle(Paint.Style.FILL);pen.setTypeface(android.graphics.Typeface.MONOSPACE);
            pen.setTextSize(dp*10f);pen.setColor(Color.rgb(185,215,230));
            canvas.drawText("G"+(box+1),4*dp,y0+12*dp,pen);
            if(tr.isEmpty())continue;
            int at=Math.min(tr.size()-1,Math.round((box+.5f)*tr.size()/4f));
            short[] samples=tr.get(at).samples;
            int count=samples.length;
            if(radar.hasTimeAxis()&&Double.isFinite(radar.getTimeWindowNs()))
                count=Math.max(2,Math.min(count,1+(int)(radar.getTimeWindowNs()/radar.getSampleIntervalNs())));
            int max=1;for(int i=0;i<count;i++)max=Math.max(max,Math.abs((int)samples[i]));
            float lim=Math.max(2,(y1-y0)-24*dp);
            float halfW=(w-16*dp)*.43f;
            float oldX=centerX,oldY=y0+18*dp;
            pen.setStyle(Paint.Style.STROKE);pen.setColor(Color.WHITE);pen.setStrokeWidth(dp*.9f);
            for(int i=0;i<count;i++){
                float xx=centerX+samples[i]*halfW/max;
                float yy=y0+17*dp+lim*i/Math.max(1,count-1);
                if(i>0)canvas.drawLine(oldX,oldY,xx,yy,pen);
                oldX=xx;oldY=yy;
            }
        }
    }
}
