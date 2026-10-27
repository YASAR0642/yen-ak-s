package com.ktd.analyzer.view;

import android.content.Context;import android.graphics.*;import android.view.*;import com.ktd.analyzer.Prefs;import com.ktd.analyzer.Ui;

public final class PaletteStripView extends View{
    public interface Listener{void onPalette(int p);}private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private int selected;private Listener listener;
    public PaletteStripView(Context c){super(c);selected=Prefs.palette(c);setBackgroundColor(Color.rgb(7,16,24));setMinimumWidth(Ui.dp(c,ColorMaps.COUNT*92));}
    public void setListener(Listener l){listener=l;}public int selected(){return selected;}
    protected void onDraw(Canvas c){super.onDraw(c);float gap=8f;float getW=(getWidth()-gap*(ColorMaps.COUNT+1))/ColorMaps.COUNT;float top=7,bottom=getHeight()-7;for(int k=0;k<ColorMaps.COUNT;k++){float l=gap+(getW+gap)*k;for(int i=0;i<72;i++){float t=i/71f;int col=ColorMaps.color(k,(t*2f-1f)*100,100);p.setColor(col);float x1=l+getW*i/72f,x2=l+getW*(i+1)/72f+1;c.drawRect(x1,top,x2,bottom,p);}p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(k==selected?4:1);p.setColor(k==selected?Color.WHITE:Color.argb(130,255,255,255));c.drawRoundRect(l,top,l+getW,bottom,7,7,p);p.setStyle(Paint.Style.FILL);}}
    public boolean onTouchEvent(MotionEvent e){if(e.getAction()!=MotionEvent.ACTION_DOWN)return true;float gap=8f,getW=(getWidth()-gap*(ColorMaps.COUNT+1))/ColorMaps.COUNT;for(int k=0;k<ColorMaps.COUNT;k++){float l=gap+(getW+gap)*k;if(e.getX()>=l&&e.getX()<=l+getW){selected=k;Prefs.setPalette(getContext(),k);invalidate();if(listener!=null)listener.onPalette(k);break;}}return true;}
}
