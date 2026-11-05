package com.ktd.analyzer.gpr;

import java.util.ArrayDeque;
import java.util.Arrays;

/**
 * On-line, colour-independent detector of a *completed* two-armed GPR echo.
 * It does not identify the material or establish the existence of a void.
 * A delayed event is emitted after both arms have been acquired and a local
 * match maximum has passed. All thresholds apply to raw samples, not to gain
 * or rendered RGB pixels.
 */
public final class HyperbolaEventDetector {
    private static final int WINDOW=171, MID=85;
    private static final int[] X={-75,-55,-35,-17,0,17,35,55,75};
    private static final int[] CURVATURE={50,62,74,86,98,110,122,134};
    private static final int[] BASE_X={-60,-50,-40,-30,-20,-10,0,10,20,30,40,50,60};
    private static final int MIN_SEPARATION=95;
    private final ArrayDeque<short[]> window=new ArrayDeque<>();
    private int samples;
    private long total, previousPeak=-100000, pendingCenter=-1;
    private float pendingBest;
    private int fallingSteps;

    public void reset(){
        window.clear();samples=0;total=0;previousPeak=-100000;
        pendingCenter=-1;pendingBest=0;fallingSteps=0;
    }

    public boolean accept(GprProtocol.Trace trace){
        if(trace==null||trace.samples==null||trace.samples.length<160)return false;
        if(samples!=0&&samples!=trace.samples.length)reset();
        samples=trace.samples.length;total++;
        window.addLast(trace.samples);
        if(window.size()>WINDOW)window.removeFirst();
        if(window.size()<WINDOW || total%3!=0)return false;
        // The center is 85 traces behind the newest trace: right arm is present.
        long center=total-MID-1;
        short[][] raw=window.toArray(new short[WINDOW][]);
        float score=bestMatch(raw);
        if(score>0){
            if(pendingCenter<0 || score>pendingBest){
                pendingBest=score;pendingCenter=center;fallingSteps=0;
            }else if(score<pendingBest*.78f){
                fallingSteps++;
            }else fallingSteps=0;
        }else if(pendingCenter>=0){
            fallingSteps++;
        }
        // Do not announce during first matching frame. Wait for matching
        // scores to fall, which marks the end of the candidate apex region.
        if(pendingCenter>=0 && fallingSteps>=3){
            long matched=pendingCenter;
            pendingCenter=-1;pendingBest=0;fallingSteps=0;
            if(matched-previousPeak<MIN_SEPARATION)return false;
            previousPeak=matched;
            return true;
        }
        // If the candidate has remained high for too many traces, it is more
        // likely a layer than a localized target; discard without speech.
        if(pendingCenter>=0 && center-pendingCenter>75){
            pendingCenter=-1;pendingBest=0;fallingSteps=0;
        }
        return false;
    }

    private float bestMatch(short[][] raw){
        int n=samples;
        int first=Math.max(70,(int)(n*.45f));
        int last=Math.min(n-140,(int)(n*.77f));
        if(last<=first)return 0;
        int[] base=new int[n];
        int[][] contrast=new int[X.length][n];
        int max=1;
        for(short sample:raw[MID])max=Math.max(max,Math.abs((int)sample));
        // Five-point vertical envelope on raw echo, horizontal robust
        // background median to suppress constant flat reflectors.
        for(int y=first;y<Math.min(n-4,last+135);y++){
            int[] values=new int[BASE_X.length];
            for(int j=0;j<BASE_X.length;j++){
                int m=0;short[] t=raw[MID+BASE_X[j]];
                for(int dy=-4;dy<=4;dy++)m=Math.max(m,Math.abs((int)t[y+dy]));
                values[j]=m;
            }
            Arrays.sort(values);
            base[y]=values[values.length/2];
            for(int j=0;j<X.length;j++){
                int m=0;short[] t=raw[MID+X[j]];
                for(int dy=-4;dy<=4;dy++)m=Math.max(m,Math.abs((int)t[y+dy]));
                contrast[j][y]=Math.max(0,m-base[y]);
                max=Math.max(max,m);
            }
        }
        float best=0;
        int[] v=new int[9];
        for(int bend:CURVATURE){
            for(int y=first;y<=last;y+=5){
                for(int i=0;i<X.length;i++){
                    int yy=y+(int)(bend*(X[i]/75.0)*(X[i]/75.0));
                    v[i]=contrast[i][yy];
                }
                // Apex, left/right arms, three widths: at least 7/9 ridge
                // samples must be strong; the weakest point also matters.
                int min=Integer.MAX_VALUE;
                for(int a:v)min=Math.min(min,a);
                int[] sorted=v.clone();Arrays.sort(sorted);
                float score=.65f*sorted[2]+.35f*min;
                if(score>best)best=score;
            }
        }
        // A conservative within-window amplitude ratio. This is an
        // empirical visual-event threshold, not a calibrated void classifier.
        return best>=Math.max(45f,max*.035f)?best:0f;
    }
}
