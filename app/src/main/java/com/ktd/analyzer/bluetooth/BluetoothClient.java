package com.ktd.analyzer.bluetooth;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Build;

import com.ktd.analyzer.data.SensorFrame;
import com.ktd.analyzer.data.SensorStreamDecoder;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Uygulama genelinde TEK Classic Bluetooth SPP bağlantısı.
 * Activity değişince soket kapanmaz; yeni ekran sadece dinleyici olur.
 */
public final class BluetoothClient {
    public interface Listener { void onState(String s); void onFrame(SensorFrame f); }
    /** CANLI GPR için yalnız etkin modda dağıtılan çerçevelenmiş ASCII paketleri. */
    public interface GprLineListener { void onGprLine(String line); }
    private static final UUID SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final CopyOnWriteArraySet<Listener> LISTENERS=new CopyOnWriteArraySet<>();
    private static final CopyOnWriteArraySet<GprLineListener> GPR_LISTENERS=new CopyOnWriteArraySet<>();
    private static volatile boolean gprLineMode=false;
    private static final ExecutorService EXEC=Executors.newSingleThreadExecutor();
    private static final Object LOCK=new Object();
    private static volatile BluetoothSocket socket;
    private static volatile boolean reading=false,connecting=false;
    private static volatile String state="Bluetooth bağlı değil";
    private static volatile String deviceName="";
    private static volatile SensorFrame lastFrame;

    private final Listener listener;
    private boolean active=false;
    public BluetoothClient(Listener l){listener=l;}

    public void activate(){
        if(active)return; active=true; LISTENERS.add(listener);
        listener.onState(state);
        if(isConnected()&&lastFrame!=null)listener.onFrame(lastFrame);
    }
    public void release(){active=false;LISTENERS.remove(listener);}
    /** Eski çağrılar için: artık global bağlantıyı kapatmaz. */
    public void close(){release();}

    public static boolean isConnected(){BluetoothSocket s=socket;return s!=null&&s.isConnected()&&reading;}
    public static String connectedName(){return deviceName==null?"":deviceName;}
    public static SensorFrame lastFrame(){return lastFrame;}

    public static void startGprLines(GprLineListener listener){
        if(listener!=null)GPR_LISTENERS.add(listener);
        gprLineMode=true;
    }
    public static void stopGprLines(GprLineListener listener){
        if(listener!=null)GPR_LISTENERS.remove(listener);
        if(GPR_LISTENERS.isEmpty())gprLineMode=false;
    }

    public static boolean hasConnectPermission(Activity a){
        return Build.VERSION.SDK_INT<31 || a.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED;
    }
    public static void requestConnectPermission(Activity a,int code){
        if(Build.VERSION.SDK_INT>=31 && !hasConnectPermission(a))a.requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},code);
    }

    public void pickPaired(Activity a){
        if(isConnected()){broadcastState("Bağlı: "+connectedName());return;}
        if(!hasConnectPermission(a)){requestConnectPermission(a,4001);broadcastState("Bluetooth izni gerekli");return;}
        BluetoothAdapter ad=BluetoothAdapter.getDefaultAdapter();
        if(ad==null){broadcastState("Bluetooth yok");return;}
        if(!ad.isEnabled()){broadcastState("Bluetooth kapalı");return;}
        Set<BluetoothDevice> set=ad.getBondedDevices();
        if(set==null||set.isEmpty()){broadcastState("Eşleşmiş cihaz yok");return;}
        ArrayList<BluetoothDevice> devs=new ArrayList<>(set);String[] names=new String[devs.size()];
        for(int i=0;i<devs.size();i++){BluetoothDevice d=devs.get(i);names[i]=safeName(d);}
        new AlertDialog.Builder(a).setTitle("Bluetooth cihazı").setItems(names,(d,w)->connect(devs.get(w))).setNegativeButton("İptal",null).show();
    }

    public void connect(BluetoothDevice d){
        synchronized(LOCK){if(connecting)return;connecting=true;}
        disconnectInternal(false); broadcastState("Bağlanıyor: "+safeName(d));
        EXEC.submit(()->{
            try{
                BluetoothSocket s=null;
                try{ s=d.createRfcommSocketToServiceRecord(SPP);s.connect(); }
                catch(Exception first){ try{ if(s!=null)s.close(); }catch(Exception ignored){} s=d.createInsecureRfcommSocketToServiceRecord(SPP);s.connect(); }
                socket=s;deviceName=safeName(d);reading=true;connecting=false;
                broadcastState("Bağlı: "+deviceName+" • X/Y/Z/C bekleniyor");
                readLoop(s.getInputStream());
            }catch(Exception e){connecting=false;broadcastState("Bağlantı kurulamadı: "+shortMsg(e));}
            finally{if(reading)disconnectInternal(true);}
        });
    }

    public void disconnect(){disconnectInternal(false);broadcastState("Bluetooth bağlı değil");}

    /** Telefonda ÖRNEK AL gibi komutları bağlı SPP cihazına yollar. */
    public boolean send(String text){
        BluetoothSocket s=socket;
        if(s==null||!s.isConnected()||!reading||text==null)return false;
        try{
            OutputStream out=s.getOutputStream();
            out.write(text.getBytes(StandardCharsets.UTF_8));
            out.flush();
            return true;
        }catch(Exception e){broadcastState("Komut gönderilemedi: "+shortMsg(e));return false;}
    }

    private static void readLoop(InputStream in)throws Exception{
        byte[] buf=new byte[2048];SensorStreamDecoder decoder=new SensorStreamDecoder();
        StringBuilder lineBuffer=new StringBuilder();
        boolean lastWasGpr=false;
        while(reading){
            int n=in.read(buf);if(n<0)break;if(n==0)continue;
            boolean nowGpr=gprLineMode;
            if(nowGpr!=lastWasGpr){lineBuffer.setLength(0);decoder=new SensorStreamDecoder();lastWasGpr=nowGpr;}
            String chunk=new String(buf,0,n,StandardCharsets.UTF_8);
            if(nowGpr){
                for(int k=0;k<chunk.length();k++){
                    char ch=chunk.charAt(k);
                    if(ch=='\n' || ch=='\r'){
                        if(lineBuffer.length()>0){
                            String line=lineBuffer.toString().trim();lineBuffer.setLength(0);
                            if(line.startsWith("KTDGPR,")||line.startsWith("KTD1,TRACE,")){
                                for(GprLineListener l:GPR_LISTENERS)try{l.onGprLine(line);}catch(Exception ignored){}
                            }
                        }
                    }else if(lineBuffer.length()<20000){lineBuffer.append(ch);}
                    else{lineBuffer.setLength(0);}
                }
            }else{
                for(SensorFrame f:decoder.feed(chunk))broadcastFrame(f);
            }
        }
    }

    private static void broadcastFrame(SensorFrame f){lastFrame=f;for(Listener l:LISTENERS)try{l.onFrame(f);}catch(Exception ignored){}}
    private static void broadcastState(String s){state=s;for(Listener l:LISTENERS)try{l.onState(s);}catch(Exception ignored){}}
    private static void disconnectInternal(boolean fromReader){
        reading=false;BluetoothSocket s=socket;socket=null;deviceName="";lastFrame=null;
        try{if(s!=null)s.close();}catch(Exception ignored){}
        if(fromReader)broadcastState("Bağlantı kesildi");
    }
    private static String safeName(BluetoothDevice d){try{String n=d.getName();return n==null||n.trim().isEmpty()?d.getAddress():n;}catch(Exception e){return "Cihaz";}}
    private static String shortMsg(Exception e){String m=e.getMessage();return m==null?e.getClass().getSimpleName():m;}
}
