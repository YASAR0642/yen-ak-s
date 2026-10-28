package com.ktd.analyzer;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.Uri;
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
import java.io.InputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import android.os.Handler;
import android.os.Looper;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/** Magnetic survey visualisation with CANLI GPR-like controls, used only in 3D live scan > GPR görünüm. */
public final class MagneticRadarActivity extends Activity implements BluetoothClient.Listener {
    private static final int SAVE=9914, OPEN=9915;
    private static final int MAGIC=0x4B4D4752; // KMGR
    private final Handler playbackHandler=new Handler(Looper.getMainLooper());
    private final ArrayList<Float> playback=new ArrayList<>();
    private boolean fileMode=false,playingFile=false,previewShowing=false;
    private int playPos=0;
    private final Runnable playTick=new Runnable(){@Override public void run(){if(!playingFile)return;int n=0;while(n<15&&playPos<playback.size()){appendDelta(playback.get(playPos++),false);n++;}if(playPos>=playback.size()){playingFile=false;startButton.setText("BAŞLAT");}else playbackHandler.postDelayed(this,16);}};
    private BluetoothClient bluetooth;
    private final ReferenceModel reference=new ReferenceModel();
    private MagneticRadargramView radar;
    private MagneticTracePanelsView panels;
    private TextView status, gainLabel;
    private Button startButton;
    private boolean running=false,zeroed=false,mineral=false;
    private float gain=1f, boost=2f;
    private int palette=1;
    
    private final ArrayList<Float> captured=new ArrayList<>();
    private long sequence=0;
    private float baseline=0,slowBase=0,fastBase=0,lastDelta=0;
    private float smoothedAnomaly=0, featurePolarity=0, featureEnergy=0;
    private int featureKind=0, featureStep=0, featureSpan=0;

    @Override public void onCreate(Bundle b){super.onCreate(b);setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);bluetooth=new BluetoothClient(this);build();}
    private int dp(int n){return Ui.dp(this,n);}    
    private Button button(String name){Button b=Ui.button(this,name,false);b.setTextSize(11);b.setAllCaps(false);b.setSingleLine(false);b.setMaxLines(2);b.setGravity(Gravity.CENTER);b.setPadding(dp(2),0,dp(2),0);b.setMinHeight(0);b.setMinimumHeight(0);return b;}
    private TextView text(String s,int size){TextView t=Ui.text(this,s,size,Color.WHITE);t.setGravity(Gravity.CENTER);return t;}
    private void add(LinearLayout p,Button b){p.addView(b,new LinearLayout.LayoutParams(-1,0,1));}

    private void build(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.rgb(5,15,23));root.setPadding(dp(5),dp(3),dp(5),dp(3));
        LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);
        Button back=button("‹ GERİ");back.setLines(1);back.setOnClickListener(v->finish());head.addView(back,new LinearLayout.LayoutParams(dp(100),dp(40)));
        TextView title=text("KTD 3D ANALYZER GPR MOD",17);head.addView(title,new LinearLayout.LayoutParams(0,dp(42),1));
        status=text("○ BLUETOOTH",12);head.addView(status,new LinearLayout.LayoutParams(dp(164),dp(40)));
        root.addView(head,new LinearLayout.LayoutParams(-1,dp(44)));

        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.HORIZONTAL);
        radar=new MagneticRadargramView(this);radar.setGain(gain);radar.setPalette(palette);radar.setEstimatedTime(true);radar.setTimeWindowNs(60);
        body.addView(radar,new LinearLayout.LayoutParams(0,-1,68));

        LinearLayout center=new LinearLayout(this);center.setOrientation(LinearLayout.VERTICAL);center.setBackgroundColor(Color.rgb(6,16,23));
        LinearLayout gainRow=new LinearLayout(this);gainRow.setGravity(Gravity.CENTER_VERTICAL);
        gainLabel=text("KAZANÇ 1.0×",11);gainRow.addView(gainLabel,new LinearLayout.LayoutParams(0,dp(30),1));
        SeekBar gainBar=new SeekBar(this);gainBar.setMax(100);gainBar.setProgress(10);
        gainBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean user){gain=p/10f;gainLabel.setText(String.format(Locale.US,"KAZANÇ %.1f×",gain));radar.setGain(gain);panels.invalidate();}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});
        gainRow.addView(gainBar,new LinearLayout.LayoutParams(0,dp(36),1));center.addView(gainRow,new LinearLayout.LayoutParams(-1,dp(39)));
        panels=new MagneticTracePanelsView(this,radar);center.addView(panels,new LinearLayout.LayoutParams(-1,0,1));
        body.addView(center,new LinearLayout.LayoutParams(0,-1,17));

        LinearLayout controls=new LinearLayout(this);controls.setOrientation(LinearLayout.VERTICAL);
        startButton=button("BAŞLAT");startButton.setOnClickListener(v->{if(fileMode){if(playback.isEmpty())return;if(previewShowing||playPos>=playback.size()){radar.clear();sequence=0;baseline=slowBase=fastBase=lastDelta=0;smoothedAnomaly=featurePolarity=featureEnergy=0;featureKind=featureStep=featureSpan=0;playPos=0;previewShowing=false;}playingFile=!playingFile;if(playingFile){startButton.setText("DURAKLAT");playbackHandler.post(playTick);}else{playbackHandler.removeCallbacks(playTick);startButton.setText("DEVAM");}return;}if(!BluetoothClient.isConnected()){Toast.makeText(this,"Bluetooth bağlantısı gerekli",Toast.LENGTH_SHORT).show();return;}bluetooth.send("LIVE_START\n");running=true;startButton.setText("ÇALIŞIYOR");});add(controls,startButton);
        Button stop=button("DURDUR");stop.setOnClickListener(v->{if(fileMode){playingFile=false;playbackHandler.removeCallbacks(playTick);playPos=0;preview();}else{bluetooth.send("LIVE_STOP\n");running=false;}startButton.setText("BAŞLAT");});add(controls,stop);
        Button open=button("DOSYA AÇ");open.setOnClickListener(v->open());add(controls,open);
        Button save=button("KAYDET");save.setOnClickListener(v->save());add(controls,save);
        Button color=button("RENK");color.setOnClickListener(v->{palette=(palette+1)%ColorMaps.COUNT;radar.setPalette(palette);color.setText("RENK\n"+(palette+1)+"/"+ColorMaps.COUNT);panels.invalidate();});add(controls,color);
        Button filter=button("MİNERAL\nKAPALI");filter.setOnClickListener(v->{mineral=!mineral;filter.setText(mineral?"MİNERAL\nAÇIK":"MİNERAL\nKAPALI");});add(controls,filter);
        Button reset=button("SIFIRLA");reset.setOnClickListener(v->{fileMode=false;playingFile=false;previewShowing=false;playbackHandler.removeCallbacks(playTick);playback.clear();playPos=0;reference.clear();zeroed=false;baseline=0;slowBase=0;fastBase=0;lastDelta=0;smoothedAnomaly=featurePolarity=featureEnergy=0;featureKind=featureStep=featureSpan=0;sequence=0;radar.clear();panels.invalidate();synchronized(captured){captured.clear();}});add(controls,reset);
        body.addView(controls,new LinearLayout.LayoutParams(0,-1,16));
        root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);Ui.applySafeInsets(this,root);updateStatus();
    }

    private void updateStatus(){if(status==null)return;boolean ok=BluetoothClient.isConnected();status.setText(ok?"● BLUETOOTH\nBAĞLI":"○ BLUETOOTH\nYOK");status.setBackgroundColor(ok?Color.rgb(45,180,95):Color.rgb(104,50,54));}
    @Override public void onState(String s){runOnUiThread(this::updateStatus);}    
    @Override protected void onStart(){super.onStart();bluetooth.activate();if(!fileMode&&BluetoothClient.isConnected()){bluetooth.send("RATE=100\n");bluetooth.send("LIVE_START\n");running=true;}updateStatus();}
    @Override protected void onStop(){playingFile=false;playbackHandler.removeCallbacks(playTick);if(BluetoothClient.isConnected())bluetooth.send("LIVE_STOP\n");running=false;bluetooth.release();super.onStop();}

    private static double ricker(double x,double mu,double sigma){ double t=(x-mu)/Math.max(1e-6,sigma); double t2=t*t; return (1.0-t2)*Math.exp(-0.5*t2); }

    @Override public void onFrame(SensorFrame f){if(!running||f==null)return;float v=f.signal();if(!Float.isFinite(v)||v<0||v>1023)return;
        runOnUiThread(()->{if(!running)return;float z=ReferenceModel.normalize(v);if(!zeroed){reference.zero(z);zeroed=true;return;}
            float delta=reference.visualDelta(z);appendDelta(delta,true);
        });
    }
    private void appendDelta(float delta,boolean store){
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
        panels.invalidate();
    }
    private void preview(){
        radar.clear();sequence=0;baseline=slowBase=fastBase=lastDelta=0;smoothedAnomaly=featurePolarity=featureEnergy=0;featureKind=featureStep=featureSpan=0;
        // Preview the full recording at no more than 180 representative traces.
        int total=playback.size();if(total==0)return;
        int shown=Math.min(180,total);
        for(int i=0;i<shown;i++){int at=(int)((long)i*total/shown);appendDelta(playback.get(at),false);}
        playPos=0;previewShowing=true;
    }
    private void open(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivityForResult(i,OPEN);}

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
                out.writeInt(MAGIC);out.writeInt(1);out.writeInt(copy.size());for(Float v:copy)out.writeFloat(v);out.flush();
                runOnUiThread(()->Toast.makeText(this,"KMGR kaydı tamamlandı",Toast.LENGTH_SHORT).show());
            }catch(Exception e){runOnUiThread(()->Toast.makeText(this,"Kayıt hatası: "+e.getMessage(),Toast.LENGTH_LONG).show());}}).start();
        }else if(request==OPEN){
            new Thread(()->{try(InputStream stream=getContentResolver().openInputStream(uri);DataInputStream in=new DataInputStream(new BufferedInputStream(stream))){
                if(in.readInt()!=MAGIC)throw new java.io.IOException("KMGR dosyası değil");
                if(in.readInt()!=1)throw new java.io.IOException("Desteklenmeyen sürüm");
                int count=in.readInt();if(count<0||count>10000000)throw new java.io.IOException("Geçersiz örnek sayısı");
                ArrayList<Float> values=new ArrayList<>(count);for(int j=0;j<count;j++){float v=in.readFloat();if(!Float.isFinite(v))throw new java.io.IOException("Geçersiz ölçüm");values.add(v);}
                runOnUiThread(()->{playingFile=false;playbackHandler.removeCallbacks(playTick);running=false;bluetooth.send("LIVE_STOP\n");fileMode=true;playback.clear();playback.addAll(values);preview();startButton.setText("BAŞLAT");Toast.makeText(this,"Dosya açıldı: "+values.size()+" ölçüm",Toast.LENGTH_SHORT).show();});
            }catch(Exception e){runOnUiThread(()->Toast.makeText(this,"Dosya açılamadı: "+e.getMessage(),Toast.LENGTH_LONG).show());}}).start();
        }
    }
}
