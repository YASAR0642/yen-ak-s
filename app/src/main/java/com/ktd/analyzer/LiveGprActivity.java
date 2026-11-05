package com.ktd.analyzer;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ktd.analyzer.bluetooth.BluetoothClient;
import com.ktd.analyzer.data.SensorFrame;
import com.ktd.analyzer.gpr.GprFileIo;
import com.ktd.analyzer.gpr.GprProtocol;
import com.ktd.analyzer.gpr.SignalAutoInfo;
import com.ktd.analyzer.gpr.HyperbolaEventDetector;
import com.ktd.analyzer.view.GprRadargramView;
import com.ktd.analyzer.view.GprTracePanelsView;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.util.Date;

/** Landscape GPR console. Bluetooth GPR traces are never synthesized from gradiometer ADC. */
public final class LiveGprActivity extends Activity implements BluetoothClient.Listener,BluetoothClient.GprLineListener {
    private static final int OPEN_FILE=1103, SAVE_FILE=1104;
    private static final int NAVY=Color.rgb(11,27,39),TEAL=Color.rgb(31,204,182);
    private final Handler ui=new Handler(Looper.getMainLooper());
    private final Object guard=new Object();
    private final ArrayDeque<GprProtocol.Message> queue=new ArrayDeque<>();
    private boolean flushScheduled=false, running=false, wasDemo=false, sound=true, hyperbola=true, auto=true;
    private boolean wanted=false, lastBtConnected=false;
    private BluetoothClient bluetooth;
    private GprRadargramView radar;
    private GprTracePanelsView panels;
    private TextView connection, gainLabel, footer;
    private Button startButton,pauseButton,restartButton,speedButton,filterButton,timeButton,antennaButton,colorButton,soundButton,hyperbolaButton,autoButton,voiceButton;
    private float gain=1f;
    private int antennaMhz=300,filter=0,timeIndex=0,soundTone=0;
    private long count=0,lastAudio=0;
    private final HyperbolaEventDetector hyperbolaDetector=new HyperbolaEventDetector();
    private final ArrayList<GprProtocol.Trace> loadedTraces=new ArrayList<>();
    private boolean fileMode=false,playingFile=false;
    private int replayPosition=0, replaySpeed=1;
    private double loadedDxMeters=Double.NaN;
    private String loadedFileKey="";
    private boolean fileHasSequentialTraceNumbers=false;
    private double calibratedDxMeters(){
        if(!fileMode||loadedTraces.size()<2)return loadedDxMeters;
        double actual=GprDistanceCalibration.get(this,loadedFileKey);
        return Double.isFinite(actual)&&actual>0?actual/(loadedTraces.size()-1.0):loadedDxMeters;
    }
    private void applyImportedDistance(){
        if(!fileMode||radar==null)return;
        radar.setOriginalSequenceDistance(fileHasSequentialTraceNumbers);
        radar.setImportedMeta(Double.isFinite(timeOverrideNs)?timeOverrideNs:lastDeviceNs,calibratedDxMeters(),false);
    }
    private double reportedAntennaMhz=Double.NaN,reportedPulseWidthNs=Double.NaN;
    private double estimatedAntennaMhz=Double.NaN,estimatedPulseWidthNs=Double.NaN;
    private double detectedWindowNs=Double.NaN;
    private int estimateCount=0;
    private boolean antennaManual=false;
    private int replayPreviewColumns=180;
    private int selectedPalette=1;
    private static final int REPLAY_FRAME_MS=14; // Approx. 2x the previous 29 ms file playback cadence
    // Bluetooth input is drained in short batches; display memory stays bounded.
    private static final int MAX_PENDING_MESSAGES=4096;
    private long droppedGprMessages=0;
    private final Runnable replayTick=new Runnable(){@Override public void run(){
        if(!fileMode||!playingFile)return;
        int batch=replaySpeed;
        while(batch-->0&&replayPosition<loadedTraces.size()){
            GprProtocol.Trace trace=loadedTraces.get(replayPosition++);
            // Same horizontal sampling grid as full-record preview: no stretched hyperbolas.
            final int total=loadedTraces.size();
            final int columns=Math.min(180,total);
            final int playedIndex=replayPosition-1;
            // Preview chooses floor(k*total/columns); replay chooses the identical indices.
            final int column=(int)(((long)playedIndex*columns+total-1)/total);
            if((column<columns && (int)((long)column*total/columns)==playedIndex) || replayPosition==total) radar.add(trace);
            count++;
            alertSignal(trace);
        }
        panels.invalidate();updateFooter();
        if(replayPosition>=loadedTraces.size()){
            playingFile=false;refreshReplayButtons();
        }else ui.postDelayed(this,REPLAY_FRAME_MS);
    }};
    private double eps=9.0, timeOverrideNs=Double.NaN, lastDeviceNs=Double.NaN;
    private ToneGenerator tone;
    private VoiceGuide voiceGuide;
    private final double[] timeOptions={Double.NaN,30,60,100,150,200,300,500};
    private final String[] filters={"FİLTRE YOK","DC DÜZELT","ZEMİN FİLTRE","KENAR VURGU"};

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        getWindow().setStatusBarColor(Color.rgb(2,14,24));
        getWindow().setNavigationBarColor(Color.rgb(2,14,24));
        bluetooth=new BluetoothClient(this);
        try{tone=new ToneGenerator(AudioManager.STREAM_MUSIC,30);}catch(Exception ignored){}
        voiceGuide=new VoiceGuide(this);
        sound=getSharedPreferences("gpr_options",0).getBoolean("voice",true);
        voiceGuide.setEnabled(sound);
        com.ktd.analyzer.view.ColorMaps.configure(this);
        selectedPalette=Prefs.palette(this);
        build();
        updateAutoLabels();
        if(getIntent()!=null && getIntent().getData()!=null)loadUri(getIntent().getData());
    }
    @Override protected void onStart(){
        super.onStart();BluetoothClient.startGprLines(this);bluetooth.activate();refresh();
    }
    @Override protected void onStop(){
        wanted=false;
        if(running&&BluetoothClient.isConnected())bluetooth.send("GPR_STOP\n");
        running=false;BluetoothClient.stopGprLines(this);bluetooth.release();
        ui.removeCallbacks(flush);ui.removeCallbacks(replayTick);playingFile=false;hyperbolaDetector.reset();synchronized(guard){queue.clear();flushScheduled=false;}
        super.onStop();
    }
    @Override protected void onResume(){
        super.onResume();com.ktd.analyzer.view.ColorMaps.configure(this);
        selectedPalette=Prefs.palette(this);
        if(radar!=null){
            radar.setPalette(selectedPalette);
            radar.setFilterMode(getSharedPreferences("gpr_options",0).getInt("display_filter",0));
            radar.setAutoContrast(getSharedPreferences("gpr_options",0).getBoolean("auto_contrast",true));
            applyImportedDistance();
        }
    }
    @Override protected void onDestroy(){if(tone!=null){tone.release();tone=null;}if(voiceGuide!=null)voiceGuide.shutdown();super.onDestroy();}
    private int dp(float v){return Ui.dp(this,v);}
    private TextView text(String value,int size){
        TextView tv=Ui.text(this,value,size,Color.WHITE);tv.setGravity(Gravity.CENTER);
        return tv;
    }
    private Button action(String label){
        Button b=Ui.button(this,label,false);b.setTextSize(10);
        b.setPadding(dp(3),0,dp(3),0);Ui.fitText(b,8,12,2);
        return b;
    }
    private void margin(View v,LinearLayout.LayoutParams params,int all){params.setMargins(dp(all),dp(all),dp(all),dp(all));v.setLayoutParams(params);}
    private LinearLayout.LayoutParams weighted(float value){return new LinearLayout.LayoutParams(0,-1,value);}
    private void build(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(5,15,23));root.setPadding(dp(4),dp(3),dp(4),dp(3));
        LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=text("KTD GPR SYSTEM",20);title.setGravity(Gravity.CENTER_VERTICAL);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        head.addView(title,new LinearLayout.LayoutParams(0,dp(40),1));
        connection=text("○ BLUETOOTH",11);connection.setBackground(Ui.bg(NAVY,6,this));
        connection.setOnClickListener(v->bluetooth.pickPaired(this));
        head.addView(connection,new LinearLayout.LayoutParams(dp(124),dp(34)));
        root.addView(head,new LinearLayout.LayoutParams(-1,dp(43)));

        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.HORIZONTAL);
        radar=new GprRadargramView(this);
        radar.setFastLive(true);
        radar.setPalette(selectedPalette);
        radar.setHyperbolaEnhance(hyperbola);
        radar.setEpsilon(eps);
        body.addView(radar,weighted(66));
        panels=new GprTracePanelsView(this,radar);
        LinearLayout middle=new LinearLayout(this);middle.setOrientation(LinearLayout.VERTICAL);
        middle.setBackgroundColor(Color.rgb(7,16,25));
        LinearLayout lineGain=new LinearLayout(this);lineGain.setGravity(Gravity.CENTER_VERTICAL);
        gainLabel=text("KAZANÇ 1.0×",10);
        lineGain.addView(gainLabel,new LinearLayout.LayoutParams(dp(91),-1));
        SeekBar slider=new SeekBar(this);slider.setMax(100);slider.setProgress(10);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar b,int n,boolean fromUser){
                gain=n*.1f;radar.setGain(gain);gainLabel.setText(String.format(Locale.US,"KAZANÇ %.1f×",gain));
            }
            public void onStartTrackingTouch(SeekBar b){}
            public void onStopTrackingTouch(SeekBar b){}
        });
        lineGain.addView(slider,new LinearLayout.LayoutParams(0,-1,1));
        // G1..G5 cover exactly the radargram depth plot from 28dp to height-8dp.
        middle.addView(lineGain,new LinearLayout.LayoutParams(-1,dp(28)));
        middle.addView(panels,new LinearLayout.LayoutParams(-1,0,1));
        View gainBottomInset=new View(this);
        middle.addView(gainBottomInset,new LinearLayout.LayoutParams(-1,dp(8)));
        body.addView(middle,weighted(20));

        LinearLayout control=new LinearLayout(this);control.setPadding(dp(3),dp(3),dp(3),dp(3));
        control.setOrientation(LinearLayout.VERTICAL);
        // AYARLAR lives in the first right-hand button, never over the radar title.
        // The Settings screen includes creation, editing, saving and deleting 10-stop palettes.
        Button createColor=action("AYARLAR");
        createColor.setOnClickListener(v->{
            Intent settings=new Intent(this,GprSettingsActivity.class);
            if(fileMode&&!loadedTraces.isEmpty()){
                settings.putExtra("gpr_file_key",loadedFileKey);
                settings.putExtra("gpr_trace_count",loadedTraces.size());
                settings.putExtra("gpr_header_distance",Double.isFinite(loadedDxMeters)?
                    loadedDxMeters*(loadedTraces.size()-1):Double.NaN);
            }
            startActivity(settings);
        });
        Button open=action("DOSYA AÇ");
        open.setOnClickListener(v->openFile());
        addRow(control,createColor,open);
        Button save=action("KAYDET");
        save.setOnClickListener(v->save());
        startButton=action("BAŞLAT");
        startButton.setOnClickListener(v->toggle());
        addRow(control,save,startButton);
        Button stop=action("DURDUR");
        stop.setOnClickListener(v->stopGprFromButton());
        timeButton=action("ZAMAN\n-- ns");
        timeButton.setOnClickListener(v->chooseTime());
        addRow(control,stop,timeButton);
        Button colors=action("RENK");
        colors.setOnClickListener(v->{
            com.ktd.analyzer.view.ColorMaps.configure(this);
            int n=Math.max(1,com.ktd.analyzer.view.ColorMaps.count());
            selectedPalette=(selectedPalette+1)%n;
            Prefs.setPalette(this,selectedPalette);
            radar.setPalette(selectedPalette);
            Toast.makeText(this,com.ktd.analyzer.view.ColorMaps.name(selectedPalette),Toast.LENGTH_SHORT).show();
        });
        LinearLayout paletteRow=new LinearLayout(this);
        paletteRow.addView(colors,new LinearLayout.LayoutParams(-1,dp(36)));
        control.addView(paletteRow,new LinearLayout.LayoutParams(-1,dp(37)));
        antennaButton=action("ANTEN\n-- MHz");
        antennaButton.setOnClickListener(v->chooseAntenna());
        LinearLayout antennaRow=new LinearLayout(this);
        antennaRow.addView(antennaButton,new LinearLayout.LayoutParams(-1,dp(36)));
        control.addView(antennaRow,new LinearLayout.LayoutParams(-1,dp(37)));
        soundButton=action(sound?"SES AÇIK":"SES KAPALI");
        soundButton.setOnClickListener(v->{
            sound=!sound;
            getSharedPreferences("gpr_options",0).edit().putBoolean("voice",sound).apply();
            if(voiceGuide!=null)voiceGuide.setEnabled(sound);
            soundButton.setText(sound?"SES AÇIK":"SES KAPALI");
        });
        control.addView(soundButton,new LinearLayout.LayoutParams(-1,dp(37)));
        body.addView(control,weighted(17));
        root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        footer=text("COM • 0 İZ",10);footer.setGravity(Gravity.CENTER_VERTICAL);
        footer.setTextColor(Color.rgb(132,191,215));
        // Bottom status details deliberately hidden; keep footer for internal updates.
        setContentView(root);Ui.applySafeInsets(this,root);
    }
    private void addRow(LinearLayout parent,Button one,Button two){
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams p1=new LinearLayout.LayoutParams(0,-1,1);p1.setMargins(dp(1),dp(1),dp(1),dp(1));
        LinearLayout.LayoutParams p2=new LinearLayout.LayoutParams(0,-1,1);p2.setMargins(dp(1),dp(1),dp(1),dp(1));
        row.addView(one,p1);row.addView(two,p2);
        parent.addView(row,new LinearLayout.LayoutParams(-1,0,1));
    }
    private void refresh(){
        if(connection==null)return;
        boolean connected=BluetoothClient.isConnected();
        connection.setText(connected?"● BLUETOOTH":"○ BLUETOOTH");
        connection.setTextColor(connected?TEAL:Color.rgb(247,129,111));
        if(connected&&!lastBtConnected)Toast.makeText(this,"Bluetooth bağlandı",Toast.LENGTH_SHORT).show();
        lastBtConnected=connected;
        updateFooter();
    }
    private void updateFooter(){
        if(footer==null)return;
        String mode=fileMode?(playingFile?"DOSYA OYNATILIYOR":"DOSYA DURDU"):(radar.isDemo()?"DEMO":radar.hasTimeAxis()?"ns":"ÖRNEK");
        footer.setText(String.format(Locale.forLanguageTag("tr-TR"),"%s  •  %d İZ  •  %s  •  εr=%.0f",fileMode?(replayPosition+"/"+loadedTraces.size()):BluetoothClient.isConnected()?"BLUETOOTH BAĞLI":"BLUETOOTH KAPALI",count,mode,eps)+"  •  ANTEN "+antennaMhz+" MHz");
    }
    private void stopPlayback(){playingFile=false;ui.removeCallbacks(replayTick);}
    private void refreshReplayButtons(){
        if(startButton!=null)startButton.setText(fileMode?(playingFile?"DURAKLAT":"BAŞLAT"):(running?"ÇALIŞIYOR":"BAŞLAT"));
        if(pauseButton!=null)pauseButton.setText(playingFile?"Ⅱ\nDURAKLAT":"▶\nDEVAM");
    }
    private void startReplay(boolean fromBeginning){
        if(!fileMode||loadedTraces.isEmpty()){Toast.makeText(this,"Önce bir SGY/CSV dosyası açın",Toast.LENGTH_SHORT).show();return;}
        stopPlayback();
        if(fromBeginning||replayPosition==0||replayPosition>=loadedTraces.size()){
            hyperbolaDetector.reset();
            replayPosition=0;count=0;radar.resetSession();panels.invalidate();
            applyImportedDistance();
            radar.setFastLive(false);
            radar.setTraceStride(loadedTraces.size()/(double)Math.max(1,Math.min(180,loadedTraces.size())));
        }
        playingFile=true;refreshReplayButtons();ui.post(replayTick);updateFooter();
    }
    private void restartReplay(){
        if(!fileMode){Toast.makeText(this,"Yeniden oynatmak için dosya açın",Toast.LENGTH_SHORT).show();return;}
        startReplay(true);
    }
    private void pauseOrResume(){
        if(!fileMode){Toast.makeText(this,"Önce SGY veya CSV dosyası açın",Toast.LENGTH_SHORT).show();return;}
        if(playingFile){stopPlayback();refreshReplayButtons();updateFooter();}
        else startReplay(false);
    }
    private void showReplayInfo(){
        new AlertDialog.Builder(this).setTitle("GPR DOSYA / OYNATMA")
            .setMessage(fileMode?("İz: "+replayPosition+" / "+loadedTraces.size()+"\nOynatma: "+replaySpeed+"×\nOynatma gerçek zamanlı ölçüm değil, dosyadaki izlerin sırayla gösterimidir."):
              "SGY/SEGY veya CSV dosyası açıp tekrar oynatabilirsiniz. Canlı GPR için Bluetooth bağlantısı gerekir.")
            .setPositiveButton("TAMAM",null).show();
    }
    private void start(){
        if(!BluetoothClient.isConnected()){bluetooth.pickPaired(this);return;}
        stopPlayback();fileMode=false;loadedTraces.clear();
        radar.resetSession();radar.setOriginalSequenceDistance(false);radar.setFastLive(true);radar.setTraceStride(1);hyperbolaDetector.reset();count=0;panels.invalidate();
        resetDeviceAuto();
        wanted=true;
        if(bluetooth.send("GPR_START\n")){
            running=true;wasDemo=false;
            startButton.setText("ÇALIŞIYOR");
        }
        updateFooter();
    }
    /** Stops live acquisition or pauses a file replay and resets its cursor for restart. */
    private void stopGprFromButton(){
        if(fileMode){
            stopPlayback();
            replayPosition=0;
            count=0;
            showFullPreview();
            refreshReplayButtons();
            updateFooter();
            return;
        }
        if(running){bluetooth.send("GPR_STOP\n");}
        wanted=false;running=false;hyperbolaDetector.reset();
        refreshReplayButtons();
        updateFooter();
    }
    private void toggle(){
        if(fileMode){if(playingFile){stopPlayback();refreshReplayButtons();updateFooter();}else startReplay(false);return;}
        if(running){wanted=false;running=false;hyperbolaDetector.reset();bluetooth.send("GPR_STOP\n");startButton.setText("BAŞLAT");}
        else start();
    }
    private void extraMenu(){
        String[] items={"DEMO", "ZEMİN εr", "ZAMAN PENCERESİ", "NS/ÖRNEK KALİBRASYONU", "DOSYA AÇ"};
        new AlertDialog.Builder(this).setItems(items,(d,i)->{
            if(i==0)demo();else if(i==1)chooseSoil();else if(i==2)chooseTime();else if(i==3)calibrateSampleTime();else openFile();
        }).show();
    }
    private void demo(){
        if(!BluetoothClient.isConnected()){bluetooth.pickPaired(this);return;}
        radar.resetSession();hyperbolaDetector.reset();count=0;
        if(bluetooth.send("GPR_DEMO_START\n")){
            wanted=true;running=true;wasDemo=true;startButton.setText("ÇALIŞIYOR");
        }
    }
    private void choosePalette(){
        com.ktd.analyzer.view.ColorMaps.configure(this);
        int n=com.ktd.analyzer.view.ColorMaps.count();
        String[] choices=new String[n];for(int i=0;i<n;i++)choices[i]=com.ktd.analyzer.view.ColorMaps.name(i);
        new AlertDialog.Builder(this).setTitle("RADARGRAM RENKLERİ")
            .setSingleChoiceItems(choices,Math.max(0,Math.min(n-1,selectedPalette)),(d,choice)->{
                selectedPalette=choice;Prefs.setPalette(this,choice);radar.setPalette(choice);d.dismiss();
            }).setNegativeButton("İPTAL",null).show();
    }
    private void showFullPreview(){
        radar.resetSession();radar.setFastLive(false);radar.setTraceStride(loadedTraces.size()/(double)Math.max(1,Math.min(180,loadedTraces.size())));count=0;
        applyImportedDistance();
        // Complete recording is represented in the fixed-width preview without dropping its timeline.
        int total=loadedTraces.size();
        final int columns=Math.min(180,total);
        for(int k=0;k<columns;k++){
            int idx=(k==columns-1)?total-1:(int)((long)k*total/columns);
            radar.add(loadedTraces.get(idx));
        }
        panels.invalidate();updateFooter();
    }
    private void chooseTime(){
        String[] choices={"Tümü", "30 ns", "60 ns", "100 ns", "150 ns", "200 ns", "300 ns", "500 ns"};
        new AlertDialog.Builder(this).setTitle("ZAMAN").setSingleChoiceItems(choices,timeIndex,(d,selected)->{
            timeIndex=selected;radar.setTimeWindowNs(timeOptions[timeIndex]);
            updateAutoLabels();
            panels.invalidate();d.dismiss();
        }).setNegativeButton("İPTAL",null).show();
    }
    private void calibrateSampleTime(){
        EditText field=new EditText(this);
        field.setHint("ns / örnek; boş = cihaz verisi");
        field.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if(Double.isFinite(timeOverrideNs))field.setText(Double.toString(timeOverrideNs));
        new AlertDialog.Builder(this).setTitle("NS/ÖRNEK KALİBRASYONU")
            .setView(field)
            .setPositiveButton("UYGULA",(d,which)->{
                String value=field.getText().toString().trim().replace(',','.');
                if(value.isEmpty()){
                    timeOverrideNs=Double.NaN;
                    radar.setImportedMeta(lastDeviceNs,radar.getStepMeters(),radar.isDemo());
                    updateFooter();updateAutoLabels();return;
                }
                try{
                    double ns=Double.parseDouble(value);
                    if(!Double.isFinite(ns)||ns<=0||ns>10000)throw new NumberFormatException();
                    timeOverrideNs=ns;
                    radar.setImportedMeta(ns,radar.getStepMeters(),radar.isDemo());
                    updateFooter();updateAutoLabels();
                }catch(NumberFormatException e){Toast.makeText(this,"Geçerli ns/örnek değeri gir",Toast.LENGTH_SHORT).show();}
            }).setNegativeButton("İPTAL",null).show();
    }
    private void chooseSoil(){
        double[] vals={1,4,6,9,12,16,25};
        String[] labels={"Hava  εr=1","Kuru zemin  εr=4","Zemin  εr=6","Zemin  εr=9","Nemli zemin  εr=12","Nemli zemin  εr=16","Yüksek dielektrik  εr=25"};
        new AlertDialog.Builder(this).setTitle("ZEMİN SEÇ").setItems(labels,(d,which)->{
            eps=vals[which];radar.setEpsilon(eps);updateFooter();
        }).show();
    }
    private void chooseAntenna(){
        String[] options={"OTOMATİK","100 MHz","200 MHz","300 MHz","400 MHz","500 MHz","800 MHz","BİLGİ"};
        final int[] values={0,100,200,300,400,500,800,0};
        new AlertDialog.Builder(this).setTitle("ANTEN / OTOMATİK ANALİZ").setItems(options,(d,n)->{
            if(n==7){showAutoDiagnostics();return;}
            if(n==0){antennaManual=false;updateAutoLabels();return;}
            antennaMhz=values[n];antennaManual=true;radar.setAntennaMhz(antennaMhz);updateAutoLabels();
            // It is a display preset; RF hardware cannot be retuned without a supported command.
        }).show();
    }
    private void openFile(){
        Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent,OPEN_FILE);
    }
    private void loadUri(final Uri uri){
        new Thread(()->{
            try{
                String filename="";
                try(android.database.Cursor c=getContentResolver().query(uri,new String[]{android.provider.OpenableColumns.DISPLAY_NAME},null,null,null)){
                    if(c!=null&&c.moveToFirst())filename=c.getString(0);
                }catch(Exception ignored){}
                boolean csv=filename!=null&&filename.toLowerCase(Locale.ROOT).endsWith(".csv");
                GprFileIo.Record rec;
                try(InputStream input=getContentResolver().openInputStream(uri)){
                    if(input==null)throw new java.io.IOException("Dosya okunamadı");
                    rec=csv?GprFileIo.readCsv(input):GprFileIo.readSegy(input);
                }
                final String importedName=(filename==null||filename.trim().isEmpty())?
                    String.valueOf(uri.getLastPathSegment()):filename;
                runOnUiThread(()->{
                    if(running)bluetooth.send("GPR_STOP\n");
                    running=false;wanted=false;startButton.setText("BAŞLAT");
                    stopPlayback();
                    loadedTraces.clear();loadedTraces.addAll(rec.traces);
                    fileMode=true;replayPosition=0;count=0;lastDeviceNs=rec.dtNs;loadedDxMeters=rec.dxMeters;loadedFileKey=importedName;
                    fileHasSequentialTraceNumbers=!"CSV".equals(rec.source);
                    // Survey-specific field measurement supplied by the user: this
                    // recording was walked over 14 m although its SEG-Y coordinates
                    // imply ~1.4 m. Never apply the correction to other recordings.
                    double fileHeaderLength=Double.isFinite(rec.dxMeters)?
                        rec.dxMeters*Math.max(0,rec.traces.size()-1):Double.NaN;
                    if(importedName.toUpperCase(Locale.ROOT).startsWith("20130623_144600")
                        && Double.isFinite(fileHeaderLength)
                        && fileHeaderLength>=1.2 && fileHeaderLength<=1.6
                        && !Double.isFinite(GprDistanceCalibration.get(this,importedName))){
                        GprDistanceCalibration.set(this,importedName,14.0);
                    }
                    if(Double.isFinite(rec.dielectric) && rec.dielectric>=1 && rec.dielectric<=100){
                        eps=rec.dielectric;radar.setEpsilon(eps);
                    }
                    reportedAntennaMhz=rec.antennaMhz;reportedPulseWidthNs=rec.pulseWidthNs;
                    estimatedAntennaMhz=Double.NaN;estimatedPulseWidthNs=Double.NaN;estimateCount=0;antennaManual=false;
                    if(Double.isFinite(rec.dtNs) && !rec.traces.isEmpty()){
                        SignalAutoInfo.Estimate e=SignalAutoInfo.estimate(rec.traces.get(0).samples,rec.dtNs);
                        estimatedAntennaMhz=e.dominantMhz;estimatedPulseWidthNs=e.receivedWidthNs;
                    }
                    detectedWindowNs=rec.traces.isEmpty()||!Double.isFinite(rec.dtNs)?Double.NaN:rec.traces.get(0).samples.length*rec.dtNs;
                    updateAutoLabels();
                    radar.resetSession();
                    applyImportedDistance();
                    radar.setFastLive(false);
                    hyperbolaDetector.reset();
                    // Opening a file must only preview the whole recording. PLAY begins acquisition-style replay.
                    showFullPreview();refreshReplayButtons();
                    Toast.makeText(this,"Dosya hazır: "+loadedTraces.size()+" iz. BAŞLAT ile oynat.",Toast.LENGTH_LONG).show();
                });
            }catch(Exception ex){runOnUiThread(()->Toast.makeText(this,"GPR dosyası açılamadı: "+ex.getMessage(),Toast.LENGTH_LONG).show());}
        },"ktd-gpr-import").start();
    }
    private void save(){
        if(radar.traceCount()==0 && (!fileMode||loadedTraces.isEmpty())){Toast.makeText(this,"İz bulunamadı",Toast.LENGTH_SHORT).show();return;}
        Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("text/csv");
        String date=new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(new Date());
        intent.putExtra(Intent.EXTRA_TITLE,"KTD_GPR_"+date+".csv");
        startActivityForResult(intent,SAVE_FILE);
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(result!=RESULT_OK||data==null||data.getData()==null)return;
        final Uri uri=data.getData();
        if(request==OPEN_FILE){
            loadUri(uri);
        } else if(request==SAVE_FILE){
            final List<GprProtocol.Trace> current=fileMode?new ArrayList<>(loadedTraces):radar.snapshot();
            final double time=radar.getSampleIntervalNs(),dx=radar.getStepMeters();
            new Thread(()->{
                try(OutputStream o=getContentResolver().openOutputStream(uri)){
                    if(o==null)throw new java.io.IOException("Dosya açılamadı");
                    GprFileIo.writeCsv(o,current,time,dx);
                    runOnUiThread(()->Toast.makeText(this,"KAYDEDİLDİ",Toast.LENGTH_SHORT).show());
                }catch(Exception ex){runOnUiThread(()->Toast.makeText(this,"Kayıt hatası: "+ex.getMessage(),Toast.LENGTH_LONG).show());}
            },"ktd-gpr-save").start();
        }
    }
    @Override public void onState(String s){runOnUiThread(()->{
        boolean connected=BluetoothClient.isConnected();
        refresh();if(!connected){running=false;startButton.setText("BAŞLAT");}
    });}
    @Override public void onFrame(SensorFrame f){/* Magnetometer is never converted to radar traces. */}
    @Override public void onGprLine(String line){
        if(fileMode)return;
        GprProtocol.Message m=GprProtocol.parse(line);if(m==null)return;
        synchronized(guard){
            while(queue.size()>=MAX_PENDING_MESSAGES){
                queue.removeFirst();droppedGprMessages++;
            }
            queue.addLast(m);
            if(!flushScheduled){flushScheduled=true;ui.postDelayed(flush,30);}
        }
    }
    private final Runnable flush=new Runnable(){@Override public void run(){
        ArrayDeque<GprProtocol.Message> packet=new ArrayDeque<>();
        synchronized(guard){while(!queue.isEmpty())packet.addLast(queue.removeFirst());flushScheduled=false;}
        for(GprProtocol.Message m:packet){
            if(m.meta!=null){
                lastDeviceNs=m.meta.nsPerSample;
                reportedAntennaMhz=m.meta.antennaMhz;
                reportedPulseWidthNs=m.meta.pulseWidthNs;
                detectedWindowNs=m.meta.timeWindowNs;
                radar.setMeta(m.meta);
                if(Double.isFinite(timeOverrideNs))radar.setImportedMeta(timeOverrideNs,m.meta.dxMeters,m.meta.isDemo());
                wasDemo=m.meta.isDemo();updateAutoLabels();
            }
            if(m.trace!=null){
                if(!wasDemo && estimateCount<3 && Double.isFinite(radar.getSampleIntervalNs())){
                    SignalAutoInfo.Estimate e=SignalAutoInfo.estimate(m.trace.samples,radar.getSampleIntervalNs());
                    if(Double.isFinite(e.dominantMhz))estimatedAntennaMhz=e.dominantMhz;
                    if(Double.isFinite(e.receivedWidthNs))estimatedPulseWidthNs=e.receivedWidthNs;
                    estimateCount++;
                    if(!Double.isFinite(detectedWindowNs))detectedWindowNs=radar.getSampleIntervalNs()*m.trace.samples.length;
                    updateAutoLabels();
                }
                radar.add(m.trace);count++;alertSignal(m.trace);
            }
        }
        panels.invalidate();updateFooter();
    }};

    private void showAutoDiagnostics(){
        String dt=Double.isFinite(lastDeviceNs)?String.format(Locale.US,"%.6g ns/örnek",lastDeviceNs):"Cihaz bildirmedi";
        String antenna=Double.isFinite(reportedAntennaMhz)?String.format(Locale.US,"%.0f MHz (cihaz)",reportedAntennaMhz):
            Double.isFinite(estimatedAntennaMhz)?String.format(Locale.US,"~%.0f MHz (alınan sinyalin spektrumu)",estimatedAntennaMhz):"Bilinmiyor";
        double pulse=Double.isFinite(reportedPulseWidthNs)?reportedPulseWidthNs:estimatedPulseWidthNs;
        String width=Double.isFinite(pulse)?String.format(Locale.US,"~%.3f ns (%s)",pulse,
                Double.isFinite(reportedPulseWidthNs)?"cihaz bildirimi":"alıcı sinyalinin genişliği"):"Bilinmiyor";
        String dx=Double.isFinite(radar.getStepMeters())?String.format(Locale.US,"%.4f m/iz",radar.getStepMeters()):"Bilinmiyor";
        new AlertDialog.Builder(this).setTitle("OTOMATİK GPR BİLGİLERİ")
            .setMessage("Örnekleme: "+dt+"\nAnten: "+antenna+"\nDarbe/sinyal genişliği: "+width+
                "\nYatay iz aralığı: "+dx+"\n\nBluetooth aktarım hızı ns değildir. " +
                "Sinyalden bulunan frekans, verici anten frekansının kesin ölçümü değildir. " +
                "Yatay metre için gerçek iz mesafesi; derinlik için zaman ve zemin kalibrasyonu gerekir.")
            .setPositiveButton("TAMAM",null).show();
    }
    private void resetDeviceAuto(){
        lastDeviceNs=Double.NaN;reportedAntennaMhz=Double.NaN;reportedPulseWidthNs=Double.NaN;
        estimatedAntennaMhz=Double.NaN;estimatedPulseWidthNs=Double.NaN;
        detectedWindowNs=Double.NaN;estimateCount=0;antennaManual=false;
        updateAutoLabels();
    }
    private void updateAutoLabels(){
        if(timeButton!=null){
            double dt=Double.isFinite(timeOverrideNs)?timeOverrideNs:lastDeviceNs;
            if(timeIndex!=0 && Double.isFinite(timeOptions[timeIndex]))
                timeButton.setText(String.format(Locale.US,"ZAMAN\n%.1f ns",timeOptions[timeIndex]));
            else if(Double.isFinite(detectedWindowNs))
                timeButton.setText(String.format(Locale.US,"ZAMAN\n%.2f ns",detectedWindowNs));
            else if(Double.isFinite(dt))timeButton.setText(String.format(Locale.US,"ZAMAN\n%.4g ns/ör",dt));
            else timeButton.setText("ZAMAN\n-- ns");
        }
        if(antennaButton!=null){
            if(antennaManual)antennaButton.setText("ANTEN\n"+antennaMhz+" MHz");
            else if(Double.isFinite(reportedAntennaMhz)){
                antennaButton.setText(String.format(Locale.US,"ANTEN\n%.0f MHz",reportedAntennaMhz));
                radar.setAntennaMhz((int)Math.round(reportedAntennaMhz));
            }else if(Double.isFinite(estimatedAntennaMhz))
                antennaButton.setText(String.format(Locale.US,"ANTEN ~%.0f MHz",estimatedAntennaMhz));
            else antennaButton.setText("ANTEN\n-- MHz");
        }
    }

    private void alertSignal(GprProtocol.Trace t){
        if(t==null||t.samples.length<80)return;
        // A GPR echo hyperbola is not proof of a void or a particular material.
        if(!hyperbolaDetector.accept(t))return;
        // Only real traces or genuine file records, never device demo modes.
        if(!fileMode && (wasDemo||radar.isDemo()))return;
        long now=SystemClock.elapsedRealtime();
        if(now-lastAudio<4000)return;
        lastAudio=now;
        if(!sound)return;
        if(voiceGuide!=null)voiceGuide.say("Olası boşluk anomalisi");
        if(tone!=null)try{tone.startTone(ToneGenerator.TONE_PROP_BEEP,95);}catch(Exception ignored){}
    }

}
