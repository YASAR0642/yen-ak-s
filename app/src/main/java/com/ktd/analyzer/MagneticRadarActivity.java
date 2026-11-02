package com.ktd.analyzer;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.Uri;
import android.database.Cursor;
import android.provider.OpenableColumns;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import com.ktd.analyzer.bluetooth.BluetoothClient;
import com.ktd.analyzer.data.ReferenceModel;
import com.ktd.analyzer.data.SensorFrame;
import com.ktd.analyzer.gpr.GprProtocol;
import com.ktd.analyzer.view.MagneticRadargramView;
import com.ktd.analyzer.view.MagneticTracePanelsView;
import com.ktd.analyzer.view.ColorMaps;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.security.SecureRandom;
import java.security.MessageDigest;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/** Magnetic survey visualisation with CANLI GPR-like controls, used only in 3D live scan > GPR görünüm. */
public final class MagneticRadarActivity extends Activity implements BluetoothClient.Listener {
    private static final int SAVE=9914, OPEN=9915;
    private static final int MAGIC=0x4B4D4752; // legacy KMGR
    private static final int SECURE_MAGIC=0x4B544732; // KTG2 encrypted recording
    private final Handler playbackHandler=new Handler(Looper.getMainLooper());
    private final ArrayList<Float> playback=new ArrayList<>();
    private boolean fileMode=false,playingFile=false,previewShowing=false;
    private int playPos=0;
    private long lastArrivalMs=0;
    private double averageArrivalNs=0;
    private int timingSamples=0;
    private final Runnable playTick=new Runnable(){@Override public void run(){if(!playingFile)return;int n=0;while(n<15&&playPos<playback.size()){appendDelta(playback.get(playPos++),false);n++;}if(playPos>=playback.size()){playingFile=false;radar.setBrowseEnabled(true);showStartButton(false);}else playbackHandler.postDelayed(this,32);}};
    private BluetoothClient bluetooth;
    private final PerfMonitor perfMonitor=new PerfMonitor();
    private TextView performanceText;
    private final ReferenceModel reference=new ReferenceModel();
    private MagneticRadargramView radar;
    private MagneticTracePanelsView panels;
    private TextView status, gainLabel, fileNameLabel;
    private Button colorButton, mineralButton, modeButton;
    private Button startButton;
    private boolean running=false,zeroed=false,mineral=false;
    private float gain=1f, boost=2f;
    private int palette=1;
    
    private final ArrayList<Float> captured=new ArrayList<>();
    private static final double CALIBRATED_CM_PER_SAMPLE=180.0/24.0; // 180 cm measured for 24 values in cantimetre.kmgr; only valid at same scanning step
    private long sequence=0;
    private float baseline=0,slowBase=0,fastBase=0,lastDelta=0;
    private float smoothedAnomaly=0, featurePolarity=0, featureEnergy=0;
    private int featureKind=0, featureStep=0, featureSpan=0;
    private boolean anomalyLatched=false;
    private int quietSamples=0;
    private static final int REFERENCE_SAMPLES=8;
    private int referenceSamples=0, polaritySamples=0;
    private double referenceSum=0;
    private float scanBaseline=0;

    @Override public void onCreate(Bundle b){super.onCreate(b);setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);bluetooth=new BluetoothClient(this);build();}
    private int dp(int n){return Ui.dp(this,n);}    
    private Button button(String name){Button b=Ui.button(this,name,false);b.setTextSize(11);b.setTypeface(android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.NORMAL));b.setAllCaps(false);b.setSingleLine(false);b.setMaxLines(2);b.setGravity(Gravity.CENTER);b.setPadding(dp(2),0,dp(2),0);b.setMinHeight(0);b.setMinimumHeight(0);return b;}
    private TextView text(String s,int size){TextView t=Ui.text(this,s,size,Color.WHITE);t.setGravity(Gravity.CENTER);return t;}
    private void add(LinearLayout p,Button b){p.addView(b,new LinearLayout.LayoutParams(-1,0,1));}

    private void build(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.rgb(5,15,23));root.setPadding(dp(5),dp(3),dp(5),dp(3));
        LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);
        head.setBackground(new android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{Color.rgb(176,227,255),Color.rgb(47,128,197),Color.rgb(121,208,248)}));
        Button back=button("‹ GERİ");back.setLines(1);back.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(27,84,132)));back.setTextColor(Color.WHITE);back.setOnClickListener(v->finish());head.addView(back,new LinearLayout.LayoutParams(dp(100),-1));
        LinearLayout heading=new LinearLayout(this);
        heading.setOrientation(LinearLayout.VERTICAL);
        heading.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);
        TextView title=text("KTD  GMR",25);
        title.setTypeface(android.graphics.Typeface.create("sans-serif-black",android.graphics.Typeface.BOLD));
        title.setTextColor(Color.rgb(115,205,255));
        title.setShadowLayer(dp(2),0,dp(1),Color.rgb(13,62,127));
        android.graphics.drawable.GradientDrawable metallicBlue=new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.rgb(176,229,255),Color.rgb(38,128,205),Color.rgb(127,216,255)});
        // Entire header has the metallic blue background, without raised steps.
        title.setTextColor(Color.rgb(4,39,80));
        title.setIncludeFontPadding(false);
        heading.addView(title,new LinearLayout.LayoutParams(-1,dp(30)));
        fileNameLabel=text("CANLI ÇEKİM",11);
        fileNameLabel.setSingleLine(true);
        fileNameLabel.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
        fileNameLabel.setTextColor(Color.rgb(175,208,223));
        heading.addView(fileNameLabel,new LinearLayout.LayoutParams(-1,dp(19)));
        head.addView(heading,new LinearLayout.LayoutParams(0,dp(52),1));
        status=text("○ BT YOK",12);status.setTextColor(Color.WHITE);
        status.setTypeface(null,android.graphics.Typeface.BOLD);
        head.addView(status,new LinearLayout.LayoutParams(dp(134),-1));
        root.addView(head,new LinearLayout.LayoutParams(-1,dp(58)));
        LinearLayout perfRow=new LinearLayout(this);perfRow.setGravity(Gravity.CENTER_VERTICAL);
        Button perfButton=button("TEST");
        perfRow.addView(perfButton,new LinearLayout.LayoutParams(dp(68),dp(32)));
        performanceText=text("",10);
        performanceText.setVisibility(android.view.View.GONE);
        perfRow.addView(performanceText,new LinearLayout.LayoutParams(0,dp(32),1));
        perfButton.setOnClickListener(v->perfMonitor.toggle(performanceText));
        modeButton=button("GÖRSEL MOD");
        modeButton.setTextSize(10);
        modeButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff234f70));
        perfRow.addView(modeButton,new LinearLayout.LayoutParams(dp(110),dp(32)));
        modeButton.setOnClickListener(v->switchDisplayMode());
        TextView displayNote=text("GÖRSEL: TEMSİLİ HİPERBOL • ÖLÇÜM: GERÇEK MANYETİK VERİ",9);
        displayNote.setTextColor(0xff9eb6c7);
        displayNote.setGravity(Gravity.CENTER_VERTICAL);
        perfRow.addView(displayNote,new LinearLayout.LayoutParams(0,dp(32),1));
        root.addView(perfRow,new LinearLayout.LayoutParams(-1,dp(33)));


        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.HORIZONTAL);
        radar=new MagneticRadargramView(this);radar.setGain(gain);radar.setPalette(palette);radar.setEstimatedTime(true);radar.setTimeWindowNs(Double.NaN);
        radar.setPerfMonitor(perfMonitor);
        body.addView(radar,new LinearLayout.LayoutParams(0,-1,68));

        LinearLayout center=new LinearLayout(this);center.setOrientation(LinearLayout.VERTICAL);center.setBackgroundColor(Color.rgb(6,16,23));
        LinearLayout gainRow=new LinearLayout(this);gainRow.setGravity(Gravity.CENTER_VERTICAL);
        gainLabel=text("KAZANÇ 1.0×",12);gainLabel.setSingleLine(true);
        gainLabel.setTextColor(Color.rgb(235,249,255));
        gainRow.setOrientation(LinearLayout.VERTICAL);
        gainRow.addView(gainLabel,new LinearLayout.LayoutParams(-1,dp(26)));
        SeekBar gainBar=new SeekBar(this);gainBar.setMax(100);gainBar.setProgress(10);
        gainBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean user){gain=p/10f;gainLabel.setText(String.format(Locale.US,"KAZANÇ %.1f×",gain));radar.setGain(gain);panels.invalidate();}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});
        gainBar.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.rgb(45,216,229)));
        gainRow.addView(gainBar,new LinearLayout.LayoutParams(-1,dp(23)));
        center.addView(gainRow,new LinearLayout.LayoutParams(-1,dp(53)));
        panels=new MagneticTracePanelsView(this,radar);center.addView(panels,new LinearLayout.LayoutParams(-1,0,1));
        body.addView(center,new LinearLayout.LayoutParams(0,-1,17));

        LinearLayout controls=new LinearLayout(this);controls.setOrientation(LinearLayout.VERTICAL);
        startButton=button("BAŞLAT");
        startButton.setOnClickListener(v->{
            if(fileMode){
                if(playingFile){
                    playingFile=false;
                    radar.setBrowseEnabled(true);
                    playbackHandler.removeCallbacks(playTick);
                    showStartButton(false);
                    return;
                }
                if(playback.isEmpty())return;
                if(previewShowing||playPos>=playback.size()){
                    radar.clear();radar.setSurveyPositionCm(0);
                    sequence=0;resetDetection();playPos=0;previewShowing=false;
                }
                radar.setWindowTraces(72);
                playingFile=true;
                radar.setBrowseEnabled(false);
                showStartButton(true);
                playbackHandler.post(playTick);
                return;
            }
            if(running){
                if(BluetoothClient.isConnected())bluetooth.send("LIVE_STOP\n");
                running=false;getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);radar.setBrowseEnabled(true);showStartButton(false);
                return;
            }
            if(!BluetoothClient.isConnected()){
                Toast.makeText(this,"Bluetooth bağlantısı gerekli",Toast.LENGTH_SHORT).show();return;
            }
            bluetooth.send("RATE=100\n");if(!bluetooth.send("LIVE_START\n"))return;running=true;getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);radar.setBrowseEnabled(false);showStartButton(true);
        });
        add(controls,startButton);
        Button open=button("DOSYA AÇ");open.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff265d86));open.setOnClickListener(v->open());add(controls,open);
        Button save=button("KAYDET");save.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff247c67));save.setOnClickListener(v->save());add(controls,save);
        Button color=button("RENK");colorButton=color;color.setOnClickListener(v->{palette=(palette+1)%ColorMaps.COUNT;radar.setPalette(palette);color.setText("RENK\n"+(palette+1)+"/"+ColorMaps.COUNT);panels.invalidate();updateButtonColors();});add(controls,color);
        Button filter=button("MİNERAL\nKAPALI");mineralButton=filter;filter.setOnClickListener(v->{mineral=!mineral;filter.setText(mineral?"MİNERAL\nAÇIK":"MİNERAL\nKAPALI");updateButtonColors();});add(controls,filter);
        Button reset=button("SIFIRLA");reset.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff965344));reset.setOnClickListener(v->{setFileTitle("CANLI ÇEKİM");fileMode=false;playingFile=false;previewShowing=false;lastArrivalMs=0;averageArrivalNs=0;timingSamples=0;radar.setWindowTraces(72);playbackHandler.removeCallbacks(playTick);showStartButton(false);playback.clear();playPos=0;reference.clear();zeroed=false;sequence=0;resetDetection();radar.clear();radar.setSurveyPositionCm(0);radar.setBrowseEnabled(true);panels.invalidate();synchronized(captured){captured.clear();}});add(controls,reset);
        body.addView(controls,new LinearLayout.LayoutParams(0,-1,16));
        root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);Ui.applySafeInsets(this,root);updateStatus();updateButtonColors();
    }

    private void showStartButton(boolean active){
        if(startButton==null)return;
        startButton.setText(active?"DURDUR":"BAŞLAT");
        startButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(active?0xffb3343f:0xff204c75));
        startButton.setTextColor(Color.WHITE);
    }
    private void updateButtonColors(){
        if(colorButton!=null){
            int[] colors={0xff57616e,0xff893f52,0xff236bb5,0xff674cb2,0xff138ca8,0xffa97832,0xff43865c,0xffba548a,0xff447184,0xff816b54,0xff4e647b};
            colorButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(colors[Math.floorMod(palette,colors.length)]));
            colorButton.setTextColor(Color.WHITE);
        }
        if(mineralButton!=null){
            mineralButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(mineral?0xffb13f43:0xff2364af));
            mineralButton.setTextColor(Color.WHITE);
        }
    }
    private final java.util.ArrayDeque<Float> measuredHistory=new java.util.ArrayDeque<>();
    private void resetDetection(){
        measuredHistory.clear();
        baseline=slowBase=fastBase=lastDelta=smoothedAnomaly=featurePolarity=featureEnergy=0;
        featureKind=featureStep=featureSpan=quietSamples=polaritySamples=0;
        anomalyLatched=false;referenceSamples=0;referenceSum=0;scanBaseline=0;
    }
    private void updateStatus(){if(status==null)return;boolean ok=BluetoothClient.isConnected();status.setText(ok?"● BT BAĞLI":"○ BT YOK");status.setBackgroundColor(ok?Color.rgb(23,136,97):Color.rgb(147,56,70));}
    @Override public void onState(String s){runOnUiThread(()->{updateStatus();if(!BluetoothClient.isConnected()){running=false;getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);showStartButton(false);}});}    
    @Override protected void onStart(){super.onStart();running=false;getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);bluetooth.activate();if(BluetoothClient.isConnected())bluetooth.send("LIVE_STOP\n");updateStatus();showStartButton(false);radar.setBrowseEnabled(true);}
    @Override protected void onStop(){perfMonitor.stop();playingFile=false;playbackHandler.removeCallbacks(playTick);if(BluetoothClient.isConnected())bluetooth.send("LIVE_STOP\n");running=false;getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);radar.setBrowseEnabled(true);showStartButton(false);bluetooth.release();super.onStop();}

    private static double ricker(double x,double mu,double sigma){ double t=(x-mu)/Math.max(1e-6,sigma); double t2=t*t; return (1.0-t2)*Math.exp(-0.5*t2); }

    @Override public void onFrame(SensorFrame f){
        perfMonitor.received();if(!running||fileMode||f==null)return;float v=f.signal();if(!Float.isFinite(v)||v<0||v>1023)return;
        // Real inter-arrival interval at the Android receiver, NOT GPR ns/time-of-flight.
        long now=SystemClock.elapsedRealtime();
        if(lastArrivalMs>0){
            long elapsedMs=now-lastArrivalMs;
            if(elapsedMs>=5 && elapsedMs<=5000){
                // Requested first experiment: convert RECEIVER inter-arrival ms to ns.
                // 1 ms = 1,000,000 ns; not a GPR pulse travel time.
                double elapsedNs=elapsedMs*1_000_000.0;
                averageArrivalNs=timingSamples==0 ? elapsedNs : averageArrivalNs*.85+elapsedNs*.15;
                timingSamples++;
                // Faster HORIZONTAL DISPLAY ONLY: halve rolling window time (18 s -> 9 s).
                // Never change received samples, waveform synthesis, gain or saved data.
                // Shorter live window means each incoming trace advances farther onscreen.
                final double windowNs=9_000_000_000.0;
                final int capacity=Math.max(48,Math.min(90,(int)Math.round(windowNs/averageArrivalNs)));
                runOnUiThread(()->{if(!fileMode)radar.setWindowTraces(capacity);});
            }
        }
        lastArrivalMs=now;
        final long queuedAtNs=System.nanoTime();
        runOnUiThread(()->{perfMonitor.queueWaitNs(System.nanoTime()-queuedAtNs);if(!running)return;float z=ReferenceModel.normalize(v);if(!zeroed){reference.zero(z);zeroed=true;return;}
            float delta=reference.visualDelta(z);appendDelta(delta,true);
        });
    }
    // TRUE MEASUREMENT WATERFALL: each vertical cell is one independently received
    // magnetic observation from a rolling 256-observation history, NOT a depth bin.
    // Never synthesize hyperbolas, target shapes, extra readings or time-of-flight.
    // Visual mode draws a SIMULATED radargram from magnetic input; measurement mode shows real history.
    // Both use the same signed input, recording and play-back data. Neither yields radar time-of-flight.
    private boolean illustrativeMode=true;
    private void appendDelta(float delta,boolean store){
        if(illustrativeMode)appendIllustrative(delta,store);
        else appendMeasured(delta,store);
    }
    private void switchDisplayMode(){
        illustrativeMode=!illustrativeMode;
        modeButton.setText(illustrativeMode?"GÖRSEL MOD":"ÖLÇÜM MODU");
        radar.clear();sequence=0;resetDetection();
        java.util.ArrayList<Float> existing;
        if(fileMode)existing=new java.util.ArrayList<>(playback);
        else synchronized(captured){existing=new java.util.ArrayList<>(captured);}
        for(Float v:existing)if(v!=null&&Float.isFinite(v))appendDelta(v,false);
        panels.invalidate();
    }
    private void appendIllustrative(float delta,boolean store){
        if(store){synchronized(captured){captured.add(delta);}}
        baseline=baseline*.988f+delta*.012f;
        slowBase=slowBase*.955f+delta*.045f;
        fastBase=fastBase*.70f+delta*.30f;
        float driftRemoved=mineral?delta-baseline:delta;
        float broad=mineral?slowBase-baseline:slowBase;
        float edge=fastBase-lastDelta;
        lastDelta=fastBase;
        float strengthened=driftRemoved+(boost-1f)*Math.signum(broad)*Math.max(0f,Math.abs(broad)-1f);
        double energy=Math.min(1.0,Math.abs(broad)/28.0);
        double edgeEnergy=Math.min(1.0,Math.abs(edge)/18.0);

        // Feature tracker: sustained negative anomaly => void-like hyperbola,
        // sustained positive anomaly => metal/cup-like symmetric downward shape.
        float detect=mineral?broad:strengthened;
        smoothedAnomaly=smoothedAnomaly*.72f+detect*.28f;
        int desiredKind=0;
        if(Math.abs(smoothedAnomaly)>=3.2f) desiredKind=smoothedAnomaly<0?-1:1;
        if(desiredKind!=0){
            if(featureKind!=desiredKind || featureStep>=featureSpan){
                featureKind=desiredKind;
                featureStep=0;
                featureSpan=18+(int)Math.round(energy*28 + Math.min(18.0,Math.abs(smoothedAnomaly)));
                featureEnergy=(float)Math.max(0.22,Math.min(1.0,energy));
                featurePolarity=smoothedAnomaly;
            }else{
                featureStep++;
                featureEnergy=Math.max(featureEnergy*.85f,(float)energy);
                featurePolarity=featurePolarity*.70f+smoothedAnomaly*.30f;
            }
        }else if(featureKind!=0){
            featureStep++;
            if(featureStep>=featureSpan){
                featureKind=0; featureStep=0; featureSpan=0; featureEnergy=0; featurePolarity=0;
            }
        }

        final int groundLimit=32; // ~0.50 m on the 0-4 MT* display ruler.
        final double xNorm=(featureSpan>1)?(((featureStep/(double)(featureSpan-1))*2.0)-1.0):0.0;
        short[] section=new short[256];
        for(int i=0;i<section.length;i++){
            double y=i;
            double amp=0;

            // Ground/top-soil band (0.00-0.50): stable layered response, suppress anomalies.
            double layered=(Math.sin((y/256.0)*22.0 + sequence*0.22)*7 + Math.sin((y/256.0)*41.0 + sequence*0.07)*3.5)*Math.exp(-y/155.0);
            if(y<groundLimit){
                amp += 18 + layered*1.7;
                amp += strengthened*10*ricker(y,groundLimit*0.55,5.0);
                section[i]=(short)Math.max(Short.MIN_VALUE,Math.min(Short.MAX_VALUE,Math.round(amp)));
                continue;
            }

            // Background continuity after top-soil.
            amp += layered*0.55;
            amp += strengthened*22*ricker(y,groundLimit+10+energy*20,8+energy*5);
            amp += edge*18*ricker(y,groundLimit+8+edgeEnergy*12,6+edgeEnergy*4);

            if(featureKind==-1){
                // Void-like continuous anomaly: nested hyperbolas whose apex depth follows anomaly strength.
                double apex=groundLimit+12 + featureEnergy*78;
                double curve=24 + featureEnergy*84;
                double mu=apex + curve*xNorm*xNorm;
                double split=7 + featureEnergy*11;
                double mag=Math.max(10.0,Math.abs(featurePolarity)*26.0);
                amp += -mag*ricker(y,mu,5.0+featureEnergy*4.0);
                amp +=  mag*0.62*ricker(y,mu+split,7.0+featureEnergy*5.0);
                amp +=  mag*0.34*ricker(y,mu+split*1.9,9.0+featureEnergy*6.0);
            }else if(featureKind==1){
                // Metal / cup-like anomaly: symmetric shapes extending downward.
                double apex=groundLimit+9 + featureEnergy*56;
                double bowl=12 + featureEnergy*24;
                double mag=Math.max(12.0,Math.abs(featurePolarity)*24.0);
                for(int layer=0;layer<3;layer++){
                    double mu=apex + layer*18 + (bowl + layer*10)*xNorm*xNorm;
                    double sig=4.8 + layer*1.7 + featureEnergy*2.8;
                    amp += mag*(1.0-layer*0.16)*ricker(y,mu,sig);
                    amp += -mag*(0.46-layer*0.08)*ricker(y,mu+6+layer*4,sig*1.25);
                }
                amp += mag*0.18*Math.exp(-Math.abs(xNorm)*2.5)*ricker(y,apex+42,15+featureEnergy*8);
            }else{
                // No persistent target: soft banded response only.
                double baseDepth=groundLimit+18 + energy*76;
                double split=14 + edgeEnergy*18;
                double sigma1=7 + energy*7;
                double sigma2=10 + edgeEnergy*9;
                amp += strengthened*42*ricker(y,baseDepth,sigma1);
                amp += strengthened*22*ricker(y,baseDepth+split,sigma2);
                amp += (-strengthened)*14*ricker(y,Math.max(groundLimit+6,baseDepth-split*.65),sigma1*1.15);
            }

            amp *= (0.86 + (y/255.0)*0.42);
            if(Math.abs(amp)<1.7) amp=0;
            section[i]=(short)Math.max(Short.MIN_VALUE,Math.min(Short.MAX_VALUE,Math.round(amp)));
        }
        radar.add(new GprProtocol.Trace(sequence++,section));
        radar.setSurveyPositionCm(0);
        panels.invalidate();
    }
    private void appendMeasured(float delta,boolean store){
        long perfProcessingStart=perfMonitor.processBegin();
        if(store){synchronized(captured){captured.add(delta);}}
        if(!Float.isFinite(delta)){perfMonitor.processEnd(perfProcessingStart);return;}
        // The same signed, calibrated sensor value is retained. Only a
        // user-selected reversible gain is applied during COLOR RENDERING.
        if(measuredHistory.size()==256)measuredHistory.removeFirst();
        measuredHistory.addLast(delta);
        short[] realMeasurements=new short[256];
        int index=256-measuredHistory.size();
        for(Float v:measuredHistory){
            // Scale for short transport only; do not confuse it with nT or metres.
            float scaled=v*32f;
            realMeasurements[index++]=(short)Math.max(-32767,Math.min(32767,Math.round(scaled)));
        }
        radar.add(new GprProtocol.Trace(sequence++,realMeasurements));
        // No inferred walking distance; position is an observation number.
        radar.setSurveyPositionCm(0);
        panels.invalidate();
        perfMonitor.processEnd(perfProcessingStart);
    }
    private void preview(){
        radar.setWindowTraces(180);radar.clear();radar.setSurveyPositionCm(0);sequence=0;resetDetection();
        // Reconstruct from EVERY sample in the exact same order as live/playback;
        // only the final window of 180 traces remains visible.
        int total=playback.size();if(total==0)return;
        for(int i=0;i<total;i++) appendDelta(playback.get(i),false);
        playPos=0;previewShowing=true;
    }
    private void open(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivityForResult(i,OPEN);}

    private String displayFileName(Uri uri){
        if(uri==null)return "KAYIT";
        try(Cursor cursor=getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){
            if(cursor!=null&&cursor.moveToFirst()){
                int column=cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if(column>=0){String name=cursor.getString(column);if(name!=null&&!name.trim().isEmpty())return name;}
            }
        }catch(Exception ignored){}
        String value=uri.getLastPathSegment();
        return value==null?"KAYIT":value;
    }
    private void setFileTitle(String name){
        if(fileNameLabel!=null)fileNameLabel.setText(name);
    }

    private static byte[] derivedKey() throws Exception {
        // App-level encryption: protect against casual file inspection.
        // For true trade-secret protection the key must NOT be published with source/APK.
        byte[] seed = ("KTD-GMR-RECORDING-ACCESS-" + "2026-NONPUBLIC-V1")
                .getBytes(StandardCharsets.UTF_8);
        return MessageDigest.getInstance("SHA-256").digest(seed);
    }
    private static byte[] encryptRecord(ArrayList<Float> values) throws Exception {
        ByteArrayOutputStream payload=new ByteArrayOutputStream();
        try(DataOutputStream data=new DataOutputStream(payload)){
            data.writeInt(values.size());
            for(Float value: values)data.writeFloat(value);
        }
        byte[] nonce=new byte[12];new SecureRandom().nextBytes(nonce);
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(derivedKey(),"AES"),new GCMParameterSpec(128,nonce));
        byte[] ciphertext=cipher.doFinal(payload.toByteArray());
        ByteArrayOutputStream result=new ByteArrayOutputStream();
        try(DataOutputStream out=new DataOutputStream(result)){
            out.writeInt(SECURE_MAGIC);out.writeInt(2);
            out.write(nonce);out.writeInt(ciphertext.length);out.write(ciphertext);
        }
        return result.toByteArray();
    }
    private static ArrayList<Float> decryptRecord(DataInputStream in) throws Exception {
        int version=in.readInt();
        if(version!=2)throw new java.io.IOException("Desteklenmeyen korumalı kayıt sürümü");
        byte[] nonce=new byte[12];in.readFully(nonce);
        int size=in.readInt();
        if(size<16||size>40_000_032)throw new java.io.IOException("Bozuk kayıt uzunluğu");
        byte[] encrypted=new byte[size];in.readFully(encrypted);
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE,new SecretKeySpec(derivedKey(),"AES"),new GCMParameterSpec(128,nonce));
        byte[] decrypted=cipher.doFinal(encrypted);
        try(DataInputStream payload=new DataInputStream(new ByteArrayInputStream(decrypted))){
            int count=payload.readInt();
            if(count<0||count>10000000||decrypted.length!=4+4L*count)
                throw new java.io.IOException("Bozuk kayıt verisi");
            ArrayList<Float> values=new ArrayList<>(count);
            for(int j=0;j<count;j++){
                float v=payload.readFloat();
                if(!Float.isFinite(v))throw new java.io.IOException("Geçersiz ölçüm");
                values.add(v);
            }
            return values;
        }
    }
    private void save(){synchronized(captured){if(captured.isEmpty()){Toast.makeText(this,"Kaydedilecek manyetik ölçüm yok",Toast.LENGTH_SHORT).show();return;}}
        Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/octet-stream");i.putExtra(Intent.EXTRA_TITLE,"KTD_MANYETIK_"+new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(new Date())+".kmgr");startActivityForResult(i,SAVE);
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(result!=RESULT_OK||data==null||data.getData()==null)return;
        Uri uri=data.getData();
        if(request==SAVE){
            ArrayList<Float> copy; synchronized(captured){copy=new ArrayList<>(captured);}
            new Thread(()->{try(OutputStream stream=getContentResolver().openOutputStream(uri);DataOutputStream out=new DataOutputStream(new BufferedOutputStream(stream))){
                out.writeInt(MAGIC);out.writeInt(1);out.writeInt(copy.size());
                for(Float value:copy)out.writeFloat(value);
                out.flush();
                runOnUiThread(()->{setFileTitle(displayFileName(uri));Toast.makeText(this,"Şifresiz KMGR kaydı tamamlandı",Toast.LENGTH_SHORT).show();});
            }catch(Exception e){runOnUiThread(()->Toast.makeText(this,"Kayıt hatası: "+e.getMessage(),Toast.LENGTH_LONG).show());}}).start();
        }else if(request==OPEN){
            new Thread(()->{try(InputStream stream=getContentResolver().openInputStream(uri);DataInputStream in=new DataInputStream(new BufferedInputStream(stream))){
                int fileMagic=in.readInt();
                ArrayList<Float> values;
                if(fileMagic==SECURE_MAGIC){
                    values=decryptRecord(in);
                }else if(fileMagic==MAGIC){
                    // Backwards compatible reader for older, unencrypted recordings.
                    if(in.readInt()!=1)throw new java.io.IOException("Desteklenmeyen eski kayıt");
                    int count=in.readInt();
                    if(count<0||count>10000000)throw new java.io.IOException("Geçersiz örnek sayısı");
                    values=new ArrayList<>(count);
                    for(int j=0;j<count;j++){
                        float v=in.readFloat();
                        if(!Float.isFinite(v))throw new java.io.IOException("Geçersiz ölçüm");
                        values.add(v);
                    }
                }else throw new java.io.IOException("KTD GMR kayıt dosyası değil");
                runOnUiThread(()->{playingFile=false;playbackHandler.removeCallbacks(playTick);running=false;bluetooth.send("LIVE_STOP\n");fileMode=true;setFileTitle(displayFileName(uri));playback.clear();playback.addAll(values);preview();radar.setBrowseEnabled(true);showStartButton(false);Toast.makeText(this,"Dosya açıldı: "+values.size()+" ölçüm",Toast.LENGTH_SHORT).show();});
            }catch(Exception e){runOnUiThread(()->Toast.makeText(this,"Dosya açılamadı: "+e.getMessage(),Toast.LENGTH_LONG).show());}}).start();
        }
    }
}
