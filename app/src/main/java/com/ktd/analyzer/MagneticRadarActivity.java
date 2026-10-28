package com.ktd.analyzer;

import android.app.Activity;
import android.graphics.Color;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import com.ktd.analyzer.bluetooth.BluetoothClient;
import com.ktd.analyzer.data.ReferenceModel;
import com.ktd.analyzer.data.SensorFrame;
import com.ktd.analyzer.view.MagneticRadarView;
import java.util.Locale;

/** Landscape magnetic display intentionally resembles GPR UI, not GPR physics. */
public final class MagneticRadarActivity extends Activity implements BluetoothClient.Listener {
    private BluetoothClient bluetooth;
    private final ReferenceModel reference=new ReferenceModel();
    private MagneticRadarView radar;
    private TextView status,boostLabel;
    private boolean running=false,zeroed=false,mineral=false;
    private float boost=2f;
    private final TextView[] traceLabels=new TextView[4];
    private final float[] traceGain={1f,1f,1f,1f};
    private final float[] traceLevel={0,0,0,0};
    private int incoming=0;
    @Override public void onCreate(Bundle b){super.onCreate(b);setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);bluetooth=new BluetoothClient(this);build();}
    private int dp(int a){return Ui.dp(this,a);}
    private Button button(String t){Button b=Ui.button(this,t,false);b.setTextSize(11);return b;}
    private TextView txt(String s,int size){TextView t=Ui.text(this,s,size,Color.WHITE);t.setGravity(Gravity.CENTER);return t;}
    private void build(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.rgb(5,15,23));root.setPadding(dp(5),dp(3),dp(5),dp(3));
        LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);
        Button back=button("‹ NORMAL");back.setOnClickListener(v->finish());head.addView(back,new LinearLayout.LayoutParams(dp(105),dp(38)));
        TextView title=txt("KTD 3D ANALYZER • MANYETİK GPR GÖRÜNÜM",17);head.addView(title,new LinearLayout.LayoutParams(0,dp(40),1));
        status=txt("○ COM",12);head.addView(status,new LinearLayout.LayoutParams(dp(120),dp(38)));root.addView(head,new LinearLayout.LayoutParams(-1,dp(43)));
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.HORIZONTAL);
        radar=new MagneticRadarView(this);radar.setBoost(boost);body.addView(radar,new LinearLayout.LayoutParams(0,-1,65));
        LinearLayout middle=new LinearLayout(this);middle.setOrientation(LinearLayout.VERTICAL);middle.setBackgroundColor(Color.rgb(8,23,32));
        boostLabel=txt("GENİŞ ANOMALİ 2.0×",11);middle.addView(boostLabel,new LinearLayout.LayoutParams(-1,dp(28)));
        SeekBar boostBar=new SeekBar(this);boostBar.setMax(8);boostBar.setProgress(2);boostBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int p,boolean user){boost=1f+p*.5f;boostLabel.setText(String.format(Locale.US,"ANOMALİ %.1f×",boost));radar.setBoost(boost);}
            public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}
        });middle.addView(boostBar,new LinearLayout.LayoutParams(-1,dp(30)));
        for(int i=0;i<4;i++){
            final int id=i;LinearLayout cell=new LinearLayout(this);cell.setOrientation(LinearLayout.VERTICAL);
            traceLabels[i]=txt("G"+(i+1)+" 1.0× • 0",11);cell.addView(traceLabels[i],new LinearLayout.LayoutParams(-1,dp(22)));
            SeekBar bar=new SeekBar(this);bar.setMax(100);bar.setProgress(10);
            bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
                public void onProgressChanged(SeekBar s,int p,boolean user){traceGain[id]=p/10f;refreshTrace(id);}
                public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}
            });cell.addView(bar,new LinearLayout.LayoutParams(-1,dp(28)));
            middle.addView(cell,new LinearLayout.LayoutParams(-1,0,1));
        }
        body.addView(middle,new LinearLayout.LayoutParams(0,-1,21));
        LinearLayout controls=new LinearLayout(this);controls.setOrientation(LinearLayout.VERTICAL);
        Button com=button("COM");com.setOnClickListener(v->bluetooth.pickPaired(this));controls.addView(com,new LinearLayout.LayoutParams(-1,0,1));
        Button start=button("BAŞLAT");start.setOnClickListener(v->{if(!BluetoothClient.isConnected()){bluetooth.pickPaired(this);return;}bluetooth.send("LIVE_START\n");running=true;start.setText("DEVAM");});controls.addView(start,new LinearLayout.LayoutParams(-1,0,1));
        Button stop=button("DURDUR");stop.setOnClickListener(v->{bluetooth.send("LIVE_STOP\n");running=false;});controls.addView(stop,new LinearLayout.LayoutParams(-1,0,1));
        Button reset=button("SIFIRLA");reset.setOnClickListener(v->{zeroed=false;reference.clear();radar.clear();});controls.addView(reset,new LinearLayout.LayoutParams(-1,0,1));
        Button mineralBtn=button("MİNERAL: KAPALI");mineralBtn.setOnClickListener(v->{mineral=!mineral;radar.setMineral(mineral);mineralBtn.setText(mineral?"MİNERAL: AÇIK":"MİNERAL: KAPALI");});controls.addView(mineralBtn,new LinearLayout.LayoutParams(-1,0,1));
        Button gain=button("RENK / KAZANÇ");gain.setOnClickListener(v->showPalette());controls.addView(gain,new LinearLayout.LayoutParams(-1,0,1));
        body.addView(controls,new LinearLayout.LayoutParams(0,-1,14));root.addView(body,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);Ui.applySafeInsets(this,root);
    }
    private void showPalette(){String[] items={"Renkli","Siyah-beyaz","Alternatif 1","Alternatif 2"};new android.app.AlertDialog.Builder(this).setTitle("Görüntü renkleri").setItems(items,(d,n)->radar.setPalette(n)).show();}
    private void refreshTrace(int i){if(traceLabels[i]!=null)traceLabels[i].setText(String.format(Locale.US,"G%d %.1f× • %.1f",i+1,traceGain[i],traceLevel[i]*traceGain[i]));}
    @Override protected void onStart(){super.onStart();bluetooth.activate();if(BluetoothClient.isConnected()){bluetooth.send("RATE=20\n");bluetooth.send("LIVE_START\n");running=true;}}
    @Override protected void onStop(){if(BluetoothClient.isConnected())bluetooth.send("LIVE_STOP\n");running=false;bluetooth.release();super.onStop();}
    @Override public void onState(String state){runOnUiThread(()->{if(status!=null)status.setText(BluetoothClient.isConnected()?"● COM BAĞLI":"○ COM");});}
    @Override public void onFrame(SensorFrame f){if(!running||f==null)return;float v=f.signal();if(!Float.isFinite(v)||v<0||v>1023)return;
        runOnUiThread(()->{if(!running)return;float z=ReferenceModel.normalize(v);if(!zeroed){reference.zero(z);zeroed=true;return;}float delta=reference.visualDelta(z);radar.append(delta);incoming++;
            // Each strip shows the measured magnetic profile at a different smoothing span,
            // NOT four independent sensor channels or physical depth bins.
            for(int i=0;i<4;i++){traceLevel[i]=traceLevel[i]*(.45f+i*.13f)+delta*(.55f-i*.13f);refreshTrace(i);}
        });
    }
}
