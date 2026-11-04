package com.ktd.analyzer.data;

public final class SensorFrame {
    public final float x,y,z,c;
    public final float value;
    public final boolean hasXYZC;
    /** Explicit REF:NNN packet starts a NEW scan, never a grid measurement. */
    public final boolean referencePacket;

    /** Eski tek kanal veri için geriye uyumluluk. */
    public SensorFrame(float value){ this(Float.NaN,Float.NaN,Float.NaN,value,false); }
    public SensorFrame(float x,float y,float z,float c){ this(x,y,z,c,true); }
    private SensorFrame(float x,float y,float z,float c,boolean has){
        this(x,y,z,c,has,false);
    }
    private SensorFrame(float x,float y,float z,float c,boolean has,boolean ref){
        this.x=x; this.y=y; this.z=z; this.c=c; this.hasXYZC=has;
        this.referencePacket=ref;
        // KTD native protokolü: X/Y saha koordinatı, Z ham sensör, C derinlik (m).
        this.value = has && Float.isFinite(z) ? z : c;
    }

    public static SensorFrame reference(float newReference){
        return new SensorFrame(Float.NaN,Float.NaN,Float.NaN,newReference,false,true);
    }
    /** Ham sensör/genlik değeri. */
    public float signal(){ return hasXYZC && Float.isFinite(z) ? z : c; }

    /** Cihazın gönderdiği C derinlik alanı (metre). */
    public float channelC(){ return hasXYZC && Float.isFinite(c) ? c : Float.NaN; }
}
