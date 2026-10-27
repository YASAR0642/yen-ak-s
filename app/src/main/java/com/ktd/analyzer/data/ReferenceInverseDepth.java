package com.ktd.analyzer.data;

import java.util.ArrayList;
import java.util.Collections;

/**
 * V3.8.8 - iki yönlü, zemine göre NORMALİZE ters sinyal cetveli.
 * Bu sonuç µT veya fiziksel hedef uzaklığı ölçümü değildir. Kalibrasyon
 * yapılmadıkça deneysel bir görsel mesafe indeksidir.
 *
 * Referans R her taramanın ilk REF/ÖRNEK AL ölçümünden gelir.
 *   Z > R : s=(Z-R)/(1023-R)
 *   Z < R : s=(R-Z)/R
 *   D=0.05/s [deneysel gösterim; fiziksel derinlik doğruluğu kanıtlanmamıştır]
 *
 * Doğal zemindeki gürültü bandı (|Z-R|<=10) 0 m yüzey kabul edilir;
 * bu, o noktada yeraltı hedefi 0 m'dedir demek değildir.
 */
public final class ReferenceInverseDepth {
    private ReferenceInverseDepth(){}
    public static final float ADC_MAX=1023f;
    public static final float GROUND_BAND=10f;
    public static final float SATURATION_ASSUMED_METERS=.05f;
    public static final class Reading {
        public final float meters,relativeStrength;
        public final boolean anomaly,positive,saturated,approxReference;
        Reading(float m,float s,boolean a,boolean p,boolean sat,boolean approx){
            meters=m;relativeStrength=s;anomaly=a;positive=p;saturated=sat;approxReference=approx;
        }
    }
    public static float referenceOf(ScanData scan){
        if(scan==null)return Float.NaN;
        if(Float.isFinite(scan.reference)&&scan.reference>0f&&scan.reference<ADC_MAX)
            return scan.reference;
        // Dört sütunlu CSV, ilk REF değerini içermez. Sadece ön izleme için
        // sağlam bir medyan tahmini; gerçek REFERANS diye sunulmaz.
        ArrayList<Float> vals=new ArrayList<>();
        for(int r=0;r<scan.rows;r++)for(int c=0;c<scan.cols;c++)if(scan.filled[r][c]){
            float z=scan.raw[r][c];
            if(Float.isFinite(z)&&z>1f&&z<1022f)vals.add(z);
        }
        if(vals.size()<3)return Float.NaN;
        Collections.sort(vals);
        return vals.get(vals.size()/2);
    }
    public static Reading measure(float raw,float ref){
        return measure(raw,ref,1f);
    }
    public static Reading measure(float raw,float ref,float calibrationFactor){
        if(!Float.isFinite(raw)||!Float.isFinite(ref)||ref<=0f||ref>=ADC_MAX)
            return new Reading(Float.NaN,Float.NaN,false,false,false,false);
        float z=Math.max(0f,Math.min(ADC_MAX,raw));
        float diff=z-ref;
        boolean positive=diff>=0f;
        boolean saturated=z<=1f||z>=1022f;
        if(Math.abs(diff)<=GROUND_BAND)return new Reading(0f,0f,false,positive,saturated,false);
        float side=positive?ADC_MAX-ref:ref;
        if(side<=0f)return new Reading(Float.NaN,Float.NaN,false,positive,saturated,false);
        float strength=Math.max(0f,Math.min(1f,Math.abs(diff)/side));
        if(strength<=0f)return new Reading(0f,0f,false,positive,saturated,false);
        // Üst sınır YOK; sinyal küçüldükçe deneysel ölçek büyür.
        float f=1f; // Önceki cihaz-kalibrasyon çarpanı devre dışı.
        float meters=(SATURATION_ASSUMED_METERS/strength)*f;
        return new Reading(Float.isFinite(meters)?meters:Float.NaN,strength,true,positive,saturated,false);
    }
    public static Reading at(ScanData d,int row,int col){
        if(d==null||row<0||col<0||row>=d.rows||col>=d.cols||!d.filled[row][col])
            return new Reading(Float.NaN,Float.NaN,false,false,false,false);
        float ref=referenceOf(d);
        Reading base=measure(d.raw[row][col],ref,d.depthCalibrationFactor);
        if(!d.referenceEstimated)return base;
        return new Reading(base.meters,base.relativeStrength,base.anomaly,base.positive,base.saturated,true);
    }
    /** Orijinal X/Y/Z/C ve CSV saklanır, 3D/4D yalnız kopyayı kullanır. */
    public static ScanData createVisualDepth(ScanData original){
        if(original==null)return null;
        ScanData out=new ScanData(original.rows,original.cols);
        out.reference=referenceOf(original);
        out.depthCalibrationFactor=original.depthCalibrationFactor;
        out.referenceEstimated=original.referenceEstimated;
        out.stepCm=original.stepCm;out.startCorner=original.startCorner;
        out.serpentine=original.serpentine;out.soilProfile=original.soilProfile;
        out.name=original.name;out.count=original.count;
        out.cIsDerivedFromRaw=original.cIsDerivedFromRaw;
        out.derivedCDivisor=original.derivedCDivisor;
        out.verifiedSiteMinMeters=original.verifiedSiteMinMeters;
        out.verifiedSiteMaxMeters=original.verifiedSiteMaxMeters;
        out.verifiedSiteLabel=original.verifiedSiteLabel;
        out.rulerDerivedDepth=true;
        for(int r=0;r<original.rows;r++)for(int c=0;c<original.cols;c++){
            out.filled[r][c]=original.filled[r][c];
            out.x[r][c]=original.x[r][c];out.y[r][c]=original.y[r][c];
            out.z[r][c]=original.z[r][c];out.raw[r][c]=original.raw[r][c];
            out.c[r][c]=original.c[r][c];out.delta[r][c]=original.delta[r][c];
            out.fieldVerifiedDepth[r][c]=original.fieldVerifiedDepth[r][c];
            if(!original.filled[r][c])continue;
            Reading reading=measure(original.raw[r][c],out.reference,out.depthCalibrationFactor);
            // Orijinal C'de bağımsız cihaz derinliği varsa ayrı saklanır.
            // Z temelli deneysel görünüm her iki tarafta aynı cetveldir.
            out.depth[r][c]=Float.isFinite(reading.meters)?reading.meters:Float.NaN;
        }
        return out;
    }
}
