package com.ktd.analyzer.data;

/**
 * Shared reference-relative PROJECT depth index (2D, 3D, 4D, NORMAL, GMR,
 * analysis). Not physical distance without target-specific field calibration.
 *
 * R: baseline measured at the start of the SAME scan (not nominal 512).
 * delta=raw-R. Positive and negative headroom are normalized separately:
 * Zrelative = 1023 * abs(delta) / (delta >= 0 ? 1023-R : R).
 * depthIndex = 800 / Zrelative.
 *
 * 15 raw counts is the GMR display threshold; below it there is no anomaly.
 * A null reading is NaN, NEVER a fabricated 0 m depth.
 */
public final class ReferenceMeterModel {
    private ReferenceMeterModel(){}
    public static final float NUMERATOR=800f;
    public static final float ADC_RANGE=1023f;
    public static final float ANOMALY_THRESHOLD=15f;
    public static float normalizedSignal(float raw,float reference){
        if(!Float.isFinite(raw)||!Float.isFinite(reference)||reference<=0f||reference>=ADC_RANGE)return Float.NaN;
        float delta=raw-reference;
        if(Math.abs(delta)<ANOMALY_THRESHOLD)return Float.NaN;
        float headroom=delta>=0f?ADC_RANGE-reference:reference;
        if(headroom<=0f)return Float.NaN;
        float normalized=ADC_RANGE*Math.abs(delta)/headroom;
        return normalized>0f?normalized:Float.NaN;
    }
    public static float meters(float raw,float reference){
        float z=normalizedSignal(raw,reference);
        return Float.isFinite(z)&&z>0f?NUMERATOR/z:Float.NaN;
    }
    public static float metersFromDelta(float delta,float reference){
        return Float.isFinite(delta)?meters(reference+delta,reference):Float.NaN;
    }
}
