package com.ktd.analyzer.data;

import java.util.Locale;

/**
 * 2D ızgaraya yazılan derinliğin KAYNAĞINI görünür tutar.
 * C/tekil saha noktası hücreye aittir. Dipol modeli tek hedefi tahmin eder;
 * sadece hedefin yakınındaki güçlü anomali kutularına ≈ işaretiyle yazılır.
 * 1024/Z, deneysel indeks olarak ≈ işaretiyle sunulur; gerçek metre değildir.
 */
public final class CellDepthDisplay {
    private CellDepthDisplay() {}
    public enum Source { VERIFIED, DEVICE_C, MODEL, INVERSE_INDEX, RULER, UNKNOWN }
    public static final class Value {
        public final Source source;
        public final float meters;
        private Value(Source s,float m){source=s;meters=m;}
        public boolean known(){return source!=Source.UNKNOWN;}
        public String label(){
            if(!known())return "—";
            String m=String.format(Locale.forLanguageTag("tr-TR"),"%.2f",meters);
            // 0,00 ortam yüzeyinin (anomali yok) işaretidir.
            return source==Source.MODEL||source==Source.INVERSE_INDEX||source==Source.RULER?(meters==0f?"0,00":"≈"+m):source==Source.VERIFIED?"✓"+m:m;
        }
    }
    private static final Value UNKNOWN=new Value(Source.UNKNOWN,Float.NaN);
    public static Value at(ScanData d,MagneticDepthEstimator.Result fit,int row,int col){
        if(d==null||row<0||row>=d.rows||col<0||col>=d.cols||!d.filled[row][col])return UNKNOWN;
        float site=d.fieldVerifiedDepth[row][col];
        if(Float.isFinite(site)&&site>=0)return new Value(Source.VERIFIED,site);
        // Kullanıcı Z ham sensörünün ilk ortam referansına göre ters ölçeğini istiyor.
        // Gerçekten saha tarafından doğrulanmış nokta bunun üstünde tutulur.
        ReferenceInverseDepth.Reading reading=ReferenceInverseDepth.at(d,row,col);
        if(Float.isFinite(reading.meters))return new Value(Source.RULER,reading.meters);
        // Referans bulunamazsa yalnız bağımsız cihaz C alanını gösterebiliriz.
        if(!d.cIsDerivedFromRaw){
            float c=d.depth[row][col];
            if(Float.isFinite(c)&&c>=0)return new Value(Source.DEVICE_C,c);
        }
        if(fit==null||!fit.hasEstimate()||!Double.isFinite(fit.depthMeters)||fit.depthMeters<=0)return UNKNOWN;
        // Dipol derinliği tek bir hedefin parametresidir. Bütün hücrelere yaymak yanlış olur.
        double x=Float.isFinite(d.x[row][col])?d.x[row][col]:(d.rows-1-row)*d.stepCm/100.0;
        double y=Float.isFinite(d.y[row][col])?d.y[row][col]:col*d.stepCm/100.0;
        double radius=Math.max(.20,d.stepCm/100.0*.95);
        double dx=x-fit.xMeters,dy=y-fit.yMeters;
        if(dx*dx+dy*dy>radius*radius)return UNKNOWN;
        if(Math.abs(d.delta[row][col])<Math.max(10f,d.maxAbs()*.16f))return UNKNOWN;
        return new Value(Source.MODEL,(float)fit.depthMeters);
    }
}
