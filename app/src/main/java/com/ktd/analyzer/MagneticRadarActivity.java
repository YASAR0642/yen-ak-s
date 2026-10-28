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
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/** Magnetic survey visualisation with CANLI GPR-like controls, used only in 3D live scan > GPR görünüm. */
public final class MagneticRadarActivity extends Activity implements BluetoothClient.Listener {
    private static final int SAVE=9914;
    private BluetoothClient bluetooth;
    private final ReferenceModel reference=new ReferenceModel();
    private MagneticRadargramView radar;
    private MagneticTracePanelsView panels;
    private TextView status, gainLabel;
    private Button startButton;
    private boolean running=false,zeroed=false,mineral=false;
    private float gain=1f, boost=2f;
    private int palette=1,antenna=300,timeIndex=3;
    private final double[] windows={30,60,100,150,200,300,500};
    private final ArrayList<Float> captured=new ArrayList<>();
    private long sequence=0;
    private float baseline=0,slowBase=0,fastBase=0,lastDelta=0;

    @Override public void onCreate(Bundle b){super.onCreate(b);setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);bluetooth=new BluetoothClient(this);build();}
    private int dp(int n){return Ui.dp(this,n);}    
    private Button button(String name){Button b=Ui.button(this,name,false);b.setTextSize(10);b.setAllCaps(false);b.setLines(2);b.setGravity(Gravity.CENTER);b.setPadding(dp(3),dp(3),dp(3),dp(3));return b;}
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
        radar=new MagneticRadargramView(this);radar.setGain(gain);radar.setPalette(palette);radar.setEstimatedTime(true);radar.setTimeWindowNs(windows[timeIndex]);
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
        startButton=button("BAŞLAT");startButton.setOnClickListener(v->{if(!BluetoothClient.isConnected()){Toast.makeText(this,"Bluetooth bağlantısı gerekli",Toast.LENGTH_SHORT).show();return;}bluetooth.send("LIVE_START\n");running=true;startButton.setText("ÇALIŞIYOR");});add(controls,startButton);
        Button stop=button("DUR");stop.setOnClickListener(v->{bluetooth.send("LIVE_STOP\n");running=false;startButton.setText("BAŞLAT");});add(controls,stop);
        Button save=button("KAYDET");save.setOnClickListener(v->save());add(controls,save);
        Button time=button("ZAMAN\n150 ns");time.setOnClickListener(v->{timeIndex=(timeIndex+1)%windows.length;radar.setTimeWindowNs(windows[timeIndex]);time.setText(String.format(Locale.US,"ZAMAN\n%.0f ns",windows[timeIndex]));panels.invalidate();});add(controls,time);
        Button color=button("RENK");color.setOnClickListener(v->{palette=(palette+1)%ColorMaps.COUNT;radar.setPalette(palette);color.setText("RENK\n"+(palette+1)+"/"+ColorMaps.COUNT);panels.invalidate();});add(controls,color);
        Button ant=button("ANTEN\n300");ant.setOnClickListener(v->{int[] arr={100,200,300,400,500,800};int i=0;while(i<arr.length&&arr[i]!=antenna)i++;antenna=arr[(i+1)%arr.length];radar.setAntennaMhz(antenna);ant.setText("ANTEN\n"+antenna);});add(controls,ant);
        Button filter=button("MİNERAL\nKAPALI");filter.setOnClickListener(v->{mineral=!mineral;filter.setText(mineral?"MİNERAL\nAÇIK":"MİNERAL\nKAPALI");});add(controls,filter);
        Button boostButton=button("ANOMALİ\n2.0×");boostButton.setOnClickListener(v->{boost=boost>=5f?1f:boost+.5f;boostButton.setText(String.format(Locale.US,"ANOMALİ\n%.1f×",boost));});add(controls,boostButton);
        Button reset=button("SIFIRLA");reset.setOnClickListener(v->{reference.clear();zeroed=false;baseline=0;slowBase=0;fastBase=0;lastDelta=0;sequence=0;radar.clear();panels.invalidate();synchronized(captured){captured.clear();}});add(controls,reset);
        body.addView(controls,new LinearLayout.LayoutParams(0,-1,15));
        root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);Ui.applySafeInsets(this,root);updateStatus();
    }

    private void updateStatus(){if(status==null)return;boolean ok=BluetoothClient.isConnected();status.setText(ok?"● BLUETOOTH\nBAĞLI":"○ BLUETOOTH\nYOK");status.setBackgroundColor(ok?Color.rgb(45,180,95):Color.rgb(104,50,54));}
    @Override public void onState(String s){runOnUiThread(this::updateStatus);}    
    @Override protected void onStart(){super.onStart();bluetooth.activate();if(BluetoothClient.isConnected()){bluetooth.send("RATE=20\n");bluetooth.send("LIVE_START\n");running=true;}updateStatus();}
    @Override protected void onStop(){if(BluetoothClient.isConnected())bluetooth.send("LIVE_STOP\n");running=false;bluetooth.release();super.onStop();}

    private static double ricker(double x,double mu,double sigma){ double t=(x-mu)/Math.max(1e-6,sigma); double t2=t*t; return (1.0-t2)*Math.exp(-0.5*t2); }

    @Override public void onFrame(SensorFrame f){if(!running||f==null)return;float v=f.signal();if(!Float.isFinite(v)||v<0||v>1023)return;
        runOnUiThread(()->{if(!running)return;float z=ReferenceModel.normalize(v);if(!zeroed){reference.zero(z);zeroed=true;return;}
            float delta=reference.visualDelta(z); synchronized(captured){captured.add(delta);}            
            baseline=baseline*.988f+delta*.012f; slowBase=slowBase*.955f+delta*.045f; fastBase=fastBase*.70f+delta*.30f;
            float driftRemoved=mineral?delta-baseline:delta; float broad=mineral?slowBase-baseline:slowBase; float edge=fastBase-lastDelta; lastDelta=fastBase;
            float strengthened=driftRemoved+(boost-1f)*Math.signum(broad)*Math.max(0f,Math.abs(broad)-1f);
            double energy=Math.min(1.0,Math.abs(broad)/28.0); double edgeEnergy=Math.min(1.0,Math.abs(edge)/18.0);
            // A GPR-like visual profile from magnetic data: multiple band-limited wavelets,
            // not a real electromagnetic travel-time trace.
            double baseDepth=70 + energy*90; double split=18 + edgeEnergy*28; double sigma1=7 + energy*10; double sigma2=10 + edgeEnergy*12;
            short[] section=new short[256];
            for(int i=0;i<section.length;i++){
                double y=i;
                double amp=0;
                amp += strengthened*180*ricker(y,baseDepth,sigma1);
                amp += strengthened*95*ricker(y,baseDepth+split,sigma2);
                amp += (-strengthened)*65*ricker(y,Math.max(24,baseDepth-split*.65),sigma1*1.15);
                amp += edge*120*ricker(y,40+edgeEnergy*45,6+edgeEnergy*7);
                amp += Math.sin((y/256.0)*28.0 + sequence*0.12)*10*Math.exp(-y/240.0); // subtle layered texture
                amp *= (0.92 + (y/255.0)*0.55);
                if(Math.abs(amp)<2.0) amp=0;
                section[i]=(short)Math.max(Short.MIN_VALUE,Math.min(Short.MAX_VALUE,Math.round(amp)));
            }
            radar.add(new GprProtocol.Trace(sequence++,section)); panels.invalidate();
        });
    }

    private void save(){synchronized(captured){if(captured.isEmpty()){Toast.makeText(this,"Kaydedilecek manyetik ölçüm yok",Toast.LENGTH_SHORT).show();return;}} Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("text/csv");i.putExtra(Intent.EXTRA_TITLE,"KTD_MANYETIK_"+new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(new Date())+".csv");startActivityForResult(i,SAVE);}    
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request!=SAVE||result!=RESULT_OK||data==null||data.getData()==null)return;Uri uri=data.getData();ArrayList<Float> copy; synchronized(captured){copy=new ArrayList<>(captured);} new Thread(()->{try(OutputStream out=getContentResolver().openOutputStream(uri)){ if(out==null)throw new java.io.IOException("Kayıt açılamadı"); out.write("index,magnetic_delta,source\n".getBytes(StandardCharsets.UTF_8)); for(int k=0;k<copy.size();k++) out.write(String.format(Locale.US,"%d,%.6f,GRADIOMETER\n",k,copy.get(k)).getBytes(StandardCharsets.UTF_8)); runOnUiThread(()->Toast.makeText(this,"Kayıt tamamlandı",Toast.LENGTH_SHORT).show()); }catch(Exception e){ runOnUiThread(()->Toast.makeText(this,"Kayıt hatası: "+e.getMessage(),Toast.LENGTH_LONG).show()); }}).start(); }
}
