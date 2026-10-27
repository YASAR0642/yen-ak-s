package com.ktd.analyzer.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import com.ktd.analyzer.data.ScanData;
import com.ktd.analyzer.data.CellDepthDisplay;
import com.ktd.analyzer.data.MagneticDepthEstimator;

import java.util.Locale;

/**
 * Çekim sırasında gerçek ölçüm hücrelerini, belirgin ızgarayı ve aynı veriden
 * üretilen canlı kontur katmanını birlikte gösterir.
 */
public final class ScanGridContourView extends View {
    public static final int OVERLAY_NONE=0;
    public static final int OVERLAY_VALUE=1;
    public static final int OVERLAY_DEPTH=2;

    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private ScanData data;
    private int palette=0,activeRow=-1,activeCol=-1,startCorner=0,overlayMode=OVERLAY_NONE;
    private boolean serpentine=true;
    private MagneticDepthEstimator.Result depthModel;

    public ScanGridContourView(Context c){super(c);setBackgroundColor(Color.rgb(3,10,15));}
    public void setData(ScanData d){data=d;invalidate();}
    public void setPalette(int v){palette=v;invalidate();}
    public void setActiveCell(int r,int c){activeRow=r;activeCol=c;invalidate();}
    public void setTraversal(int corner,boolean zigzag){startCorner=corner;serpentine=zigzag;invalidate();}
    public void setOverlayMode(int mode){overlayMode=mode;invalidate();}
    public void setDepthModel(MagneticDepthEstimator.Result result){depthModel=result;invalidate();}

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);if(data==null)return;

        // v3.5.2: saha hücreleri fiziksel olarak 30 x 30 cm olduğundan ekranda da
        // her hücre kare çizilir. Görüntü artık telefon ekranına zorla esnetilmez.
        float leftMargin=dp(8f),rightMargin=dp(8f),topMargin=dp(8f),bottomMargin=dp(8f);
        float availW=getWidth()-leftMargin-rightMargin;
        float availH=getHeight()-topMargin-bottomMargin;
        if(availW<=0||availH<=0||data.cols<=0||data.rows<=0)return;
        float cell=Math.min(availW/data.cols,availH/data.rows);
        if(cell<=1f)return;
        float plotW=cell*data.cols,plotH=cell*data.rows;
        float left=leftMargin+(availW-plotW)*.5f;
        float top=topMargin+(availH-plotH)*.5f;
        RectF area=new RectF(left,top,left+plotW,top+plotH);

        drawContour(c,area);
        drawGrid(c,area);
        // Koordinat aralığı ve X/Y yazıları istek gereği tamamen gizli.
        drawStart(c,area);
    }

    private void drawContour(Canvas c,RectF a){
        if(data.count==0){p.setColor(Color.rgb(8,22,30));c.drawRect(a,p);return;}
        DenseField d=DenseField.from(data,56);Bitmap b=Bitmap.createBitmap(d.w,d.h,Bitmap.Config.ARGB_8888);
        float scale=Math.max(10f,d.robustAbs);
        for(int y=0;y<d.h;y++)for(int x=0;x<d.w;x++){
            int i=y*d.w+x;
            int col=d.valid[i]?ColorMaps.color(palette,d.value[i],scale):Color.rgb(7,18,25);
            b.setPixel(x,y,col);
        }
        p.setFilterBitmap(true);p.setAlpha(255);c.drawBitmap(b,null,a,p);p.setAlpha(255);b.recycle();
    }

    private void drawGrid(Canvas c,RectF a){
        float cw=a.width()/data.cols,ch=a.height()/data.rows;

        // Kısmi çekimde DenseField enterpolasyonu yalnız ölçülmüş karelerde görünür.
        // Böylece renk henüz örnek alınmamış komşu kutulara taşmaz.
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.rgb(3,10,15));
        for(int r=0;r<data.rows;r++)for(int co=0;co<data.cols;co++)if(!data.filled[r][co]){
            float l=a.left+co*cw,t=a.top+r*ch;
            c.drawRect(l,t,l+cw,t+ch,p);
        }

        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(1.15f));p.setColor(Color.argb(220,225,241,248));
        for(int i=0;i<=data.cols;i++){float x=a.left+i*cw;c.drawLine(x,a.top,x,a.bottom,p);}
        for(int i=0;i<=data.rows;i++){float y=a.top+i*ch;c.drawLine(a.left,y,a.right,y,p);}

        // v3.4.4: Hücre merkezindeki beyaz ölçüm noktaları kaldırıldı.
        // Okunabilirlik yalnız ızgara + isteğe bağlı DEĞER/DERİNLİK yazısı ile sağlanır.
        drawCellNumbers(c,a,cw,ch);

        if(activeRow>=0&&activeCol>=0&&activeRow<data.rows&&activeCol<data.cols){
            float l=a.left+activeCol*cw,t=a.top+activeRow*ch;
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(5f);p.setColor(Color.rgb(0,255,220));
            c.drawRect(l+2,t+2,l+cw-2,t+ch-2,p);
            p.setStyle(Paint.Style.FILL);p.setColor(Color.argb(85,0,255,220));c.drawRect(l+4,t+4,l+cw-4,t+ch-4,p);
        }
        p.setStyle(Paint.Style.FILL);
    }

    private void drawCellNumbers(Canvas c,RectF a,float cw,float ch){
        if(overlayMode==OVERLAY_NONE)return;
        p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        // Her palette otomatik açık/koyu kontrast + karşı renk kontur kullanılır.
        // Böylece siyah sayılar koyu yeşil / mavi hücrede kaybolmaz.
        p.clearShadowLayer();
        p.setAlpha(255);
        for(int r=0;r<data.rows;r++)for(int co=0;co<data.cols;co++)if(data.filled[r][co]){
            String text=overlayMode==OVERLAY_VALUE?formatSignal(data.raw[r][co]):
                    CellDepthDisplay.at(data,depthModel,r,co).label();
            float size=fitTextSize(text,cw,ch);
            p.setTextSize(size);
            float x=a.left+(co+.5f)*cw;
            float y=a.top+(r+.5f)*ch-(p.ascent()+p.descent())*.5f;
            int bg=ColorMaps.color(palette,data.delta[r][co],Math.max(10f,data.maxAbs()));
            double lum=.2126*Color.red(bg)+.7152*Color.green(bg)+.0722*Color.blue(bg);
            int front=lum<135?Color.WHITE:Color.BLACK;
            int back=lum<135?Color.BLACK:Color.WHITE;
            p.setStrokeJoin(Paint.Join.ROUND);
            p.setStrokeWidth(dp(2.6f));
            p.setStyle(Paint.Style.STROKE);p.setColor(back);
            c.drawText(text,x,y,p);
            p.setStyle(Paint.Style.FILL);p.setColor(front);
            c.drawText(text,x,y,p);
        }
        p.clearShadowLayer();
        p.setAlpha(255);
        p.setStyle(Paint.Style.FILL);
        p.setTypeface(android.graphics.Typeface.DEFAULT);
        p.setTextAlign(Paint.Align.LEFT);
    }

    /** Hücre boyutuna göre yazıyı mümkün olduğunca büyük, ama taşmayacak şekilde ayarlar. */
    private float fitTextSize(String text,float cw,float ch){
        float maxByHeight=Math.max(sp(12f),ch*.40f);
        float size=Math.min(sp(30f),Math.min(cw*.46f,maxByHeight));
        size=Math.max(sp(11f),size);
        p.setTextSize(size);
        float maxWidth=cw*.88f;
        float w=p.measureText(text);
        if(w>maxWidth&&w>0)size*=maxWidth/w;
        return Math.max(sp(9.5f),size);
    }

    /** Sensör ham değeri 0..1023 ADC aralığındadır; ekranda .0 gösterilmez. */
    private String formatSignal(float v){
        int n=Math.max(0,Math.round(v)); // cihaz 1023'u az aşıyorsa 1023 diye kırpma
        return Integer.toString(n);
    }

    /** Çekim ekranında birim yazısı kullanılmaz; yalnız sayısal derinlik gösterilir. */
    private String formatDepth(float v){
        float d=Math.abs(v);
        if(Math.abs(d-Math.round(d))<0.005f)return String.format(Locale.US,"%.0f",d);
        if(d>=10f)return String.format(Locale.US,"%.1f",d);
        return String.format(Locale.US,"%.2f",d);
    }

    private float sp(float v){return v*getResources().getDisplayMetrics().scaledDensity;}
    private float dp(float v){return v*getResources().getDisplayMetrics().density;}

    private void drawStart(Canvas c,RectF a){
        float cw=a.width()/data.cols,ch=a.height()/data.rows;
        int co=startCorner==1?data.cols-1:0;
        // Hücre rakamlarını örten BAŞLA metni yerine küçük köşe işareti.
        float x=a.left+co*cw+(startCorner==1?cw-dp(7):dp(7));
        float y=a.bottom-dp(7);
        p.setStyle(Paint.Style.FILL);p.setColor(Color.BLACK);c.drawCircle(x,y,dp(5.3f),p);
        p.setColor(Color.YELLOW);c.drawCircle(x,y,dp(3.0f),p);
        if(!serpentine){p.setColor(Color.argb(180,255,255,255));p.setTextSize(sp(8f));c.drawText("PARALEL",a.right-dp(48),a.top+dp(12),p);}
    }
}
