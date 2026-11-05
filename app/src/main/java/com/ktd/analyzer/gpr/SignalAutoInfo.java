package com.ktd.analyzer.gpr;

/** Measured trace waveform only; never interprets arrival cadence as nanoseconds. */
public final class SignalAutoInfo {
    private SignalAutoInfo(){}
    public static final class Estimate {
        public final double dominantMhz, receivedWidthNs;
        public Estimate(double f,double width){dominantMhz=f;receivedWidthNs=width;}
    }
    public static Estimate estimate(short[] samples,double nsPerSample){
        if(samples==null||samples.length<80||!Double.isFinite(nsPerSample)||nsPerSample<=0)return new Estimate(Double.NaN,Double.NaN);
        final int n=Math.min(samples.length,256);
        double mean=0;for(int i=0;i<n;i++)mean+=samples[i];mean/=n;
        double variance=0;for(int i=0;i<n;i++){double d=samples[i]-mean;variance+=d*d;}
        if(variance/n<16)return new Estimate(Double.NaN,Double.NaN);
        double nyquistMhz=500.0/nsPerSample;
        // Hann taper and DFT: peak of received waveform, not transmitter antenna certification.
        double best=0,bestMhz=Double.NaN,second=0;
        for(int k=1;k<n/2;k++){
            double mhz=1000.0*k/(n*nsPerSample);
            if(mhz<50||mhz>1200||mhz>=nyquistMhz)continue;
            double re=0,im=0;
            for(int j=0;j<n;j++){
                double v=(samples[j]-mean)*(.5-.5*Math.cos(2*Math.PI*j/(n-1)));
                double phase=2*Math.PI*k*j/n;
                re+=v*Math.cos(phase);im-=v*Math.sin(phase);
            }
            double energy=re*re+im*im;
            if(energy>best){second=best;best=energy;bestMhz=mhz;}
            else if(energy>second)second=energy;
        }
        if(best<=0 || best<variance*.02)bestMhz=Double.NaN;
        // Width of the first robust received transient, at 50% peak envelope.
        int window=Math.min(n,Math.max(24,n/3));double max=0;int apex=-1;
        for(int i=0;i<window;i++){
            double val=Math.abs(samples[i]-mean);
            if(val>max){max=val;apex=i;}
        }
        double width=Double.NaN;
        if(apex>=0 && max>Math.sqrt(variance/n)*2){
            int l=apex,r=apex;
            while(l>0 && Math.abs(samples[l-1]-mean)>max*.5)l--;
            while(r+1<window && Math.abs(samples[r+1]-mean)>max*.5)r++;
            if(l>0&&r<window-1)width=(r-l+1)*nsPerSample;
        }
        return new Estimate(bestMhz,width);
    }
}
