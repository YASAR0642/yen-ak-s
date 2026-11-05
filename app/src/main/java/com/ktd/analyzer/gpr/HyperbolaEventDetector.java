package com.ktd.analyzer.gpr;

import java.util.ArrayDeque;

/**
 * Cautious geometric hyperbola-like reflector heuristic, NOT a void classifier.
 * Uses only measured GPR traces. A window with a shallow central apex and
 * two deeper symmetric arms can trigger a single event. A trace cooldown
 * prevents announcements while the same shape continues through the display.
 */
public final class HyperbolaEventDetector {
    private static final int WINDOW=17;
    private static final int COOLDOWN_TRACES=100;
    private final ArrayDeque<short[]> recent=new ArrayDeque<>();
    private int samples=0;
    private int cooldown=0;
    private boolean armed=true;
    private int quietTraces=0;
    public void reset(){recent.clear();samples=0;cooldown=0;armed=true;quietTraces=0;}
    public boolean accept(GprProtocol.Trace trace){
        if(trace==null||trace.samples==null||trace.samples.length<80)return false;
        if(samples!=0&&samples!=trace.samples.length)reset();
        samples=trace.samples.length;
        recent.addLast(trace.samples);
        if(recent.size()>WINDOW)recent.removeFirst();
        if(cooldown>0)cooldown--;
        if(recent.size()!=WINDOW)return false;
        short[][] a=recent.toArray(new short[WINDOW][]);
        // Estimate background from all samples so uniform horizontal layers
        // cannot be classified solely by having a strong return.
        double total=0;int n=0;
        for(int x=0;x<WINDOW;x++)for(int y=0;y<samples;y+=6){total+=Math.abs((int)a[x][y]);n++;}
        double baseline=Math.max(8,total/Math.max(1,n));
        int shiftMin=Math.max(3,samples/100),shiftMax=Math.max(shiftMin+3,samples/17);
        shiftMax=Math.min(shiftMax,samples/7);
        // The apex is at the middle trace, both arms are on either side.
        boolean candidate=false;
        for(int y=Math.max(12,samples/12);y<samples*4/5-shiftMax;y+=2){
            double center=peak(a[8],y,2);
            if(center<baseline*4.1 || center<60)continue;
            double sideAtApex=(peak(a[2],y,2)+peak(a[14],y,2))*0.5;
            for(int dy=shiftMin;dy<=shiftMax;dy+=Math.max(1,shiftMin/2)){
                double left=peak(a[2],y+dy,3),right=peak(a[14],y+dy,3);
                // Arms must be comparably strong, and displaced downward.
                if(left<baseline*2.8||right<baseline*2.8)continue;
                if(Math.min(left,right)<Math.max(left,right)*.42)continue;
                if((left+right)*.5<sideAtApex*1.55)continue;
                // Additional intermediate shoulders must connect into the two arms.
                int half=Math.max(2,dy/2);
                double shoulderL=peak(a[5],y+half,3),shoulderR=peak(a[11],y+half,3);
                if(shoulderL<baseline*2.4||shoulderR<baseline*2.4)continue;
                // Ordinary horizontal bands should not win merely by high amplitude.
                if(center<sideAtApex*1.35)continue;
                candidate=true;break;
            }
            if(candidate)break;
        }
        if(!candidate){
            if(++quietTraces>=WINDOW*3 && cooldown==0)armed=true;
            return false;
        }
        quietTraces=0;
        if(!armed||cooldown>0)return false;
        armed=false;cooldown=COOLDOWN_TRACES;
        return true;
    }
    private static double peak(short[] values,int center,int radius){
        int best=0;
        for(int i=Math.max(0,center-radius);i<=Math.min(values.length-1,center+radius);i++)
            best=Math.max(best,Math.abs((int)values[i]));
        return best;
    }
}
