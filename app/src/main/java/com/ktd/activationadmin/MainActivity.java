package com.ktd.activationadmin;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.biometric.BiometricManager;
import androidx.fragment.app.FragmentActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.util.concurrent.Executor;

public final class MainActivity extends FragmentActivity {
    private EditText deviceInput;
    private TextView tokenBox;
    private TextView status;
    private String currentToken = "";
    private boolean unlocked = false;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        getWindow().setStatusBarColor(Color.rgb(3,16,23));
        getWindow().setNavigationBarColor(Color.BLACK);
        if (!SecurityStore.hasPin(this)) showCreatePin(); else showUnlock();
    }

    @Override protected void onPause(){ super.onPause(); if(!isChangingConfigurations()) unlocked=false; }
    @Override protected void onResume(){ super.onResume(); }

    private void showCreatePin() {
        LinearLayout root = baseRoot();
        root.addView(header("İLK KURULUM", "Bu yönetici uygulaması yalnızca sizin kullanımınız içindir."));
        EditText p1 = pinBox("6-10 haneli yönetici PIN'i");
        EditText p2 = pinBox("PIN'i tekrar girin");
        root.addView(p1, lp(LinearLayout.LayoutParams.MATCH_PARENT, 56, 8,8));
        root.addView(p2, lp(LinearLayout.LayoutParams.MATCH_PARENT, 56, 8,8));
        TextView warn = Ui.text(this,"PIN'inizi unutursanız uygulama verilerini temizleyip yeniden kurmanız gerekir.",13,Ui.rgb("#F7C968"));
        warn.setGravity(Gravity.CENTER); root.addView(warn, lp(-1,-2,10,12));
        TextView save = button("PIN OLUŞTUR VE DEVAM ET", Ui.rgb("#18D9C1"), Ui.rgb("#031315"));
        save.setOnClickListener(v->{
            String a=p1.getText().toString(), c=p2.getText().toString();
            if(!SecurityStore.validPin(a)){toast("PIN 6-10 rakam olmalı");return;}
            if(!a.equals(c)){toast("PIN'ler aynı değil");return;}
            if(SecurityStore.setPin(this,a)){unlocked=true;showMain();}else toast("PIN kaydedilemedi");
        });
        root.addView(save, lp(-1,56,10,0));
        setContentView(wrap(root));
    }

    private void showUnlock() {
        LinearLayout root=baseRoot();
        root.addView(header("KTD AKTİVASYON YÖNETİCİ", "Yetkili erişim"));
        EditText pin=pinBox("Yönetici PIN'i"); root.addView(pin, lp(-1,58,16,8));
        TextView open=button("KİLİDİ AÇ", Ui.rgb("#18D9C1"), Ui.rgb("#031315"));
        open.setOnClickListener(v->{ if(SecurityStore.verifyPin(this,pin.getText().toString())){unlocked=true;showMain();}else toast("PIN yanlış"); });
        root.addView(open,lp(-1,56,8,8));

        BiometricManager bm=BiometricManager.from(this);
        if(SecurityStore.biometricEnabled(this) && bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL)==BiometricManager.BIOMETRIC_SUCCESS){
            TextView bio=button("PARMAK İZİ / EKRAN KİLİDİ İLE AÇ", Ui.rgb("#173847"), Color.WHITE);
            bio.setOnClickListener(v->startBiometric()); root.addView(bio,lp(-1,54,8,0));
            root.postDelayed(this::startBiometric,250);
        }
        setContentView(wrap(root));
    }

    private void startBiometric(){
        Executor ex=ContextCompat.getMainExecutor(this);
        BiometricPrompt prompt=new BiometricPrompt(this,ex,new BiometricPrompt.AuthenticationCallback(){
            @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult r){super.onAuthenticationSucceeded(r);unlocked=true;showMain();}
        });
        BiometricPrompt.PromptInfo info=new BiometricPrompt.PromptInfo.Builder()
                .setTitle("KTD Aktivasyon Yönetici")
                .setSubtitle("Yetkili erişimi doğrulayın")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();
        prompt.authenticate(info);
    }

    private void showMain(){
        if(!unlocked){showUnlock();return;}
        LinearLayout root=baseRoot();
        root.addView(header("KTD AKTİVASYON YÖNETİCİ", "KTD 3D ANALYZER • Çevrimdışı lisans üretimi"));

        TextView lab=Ui.text(this,"CİHAZ KODU",14,Ui.rgb("#DBEFF6"));Ui.bold(lab);root.addView(lab,lp(-1,28,12,0));
        deviceInput=new EditText(this); deviceInput.setSingleLine(true); deviceInput.setAllCaps(true); deviceInput.setTextColor(Color.WHITE);deviceInput.setHintTextColor(Ui.rgb("#637E8A"));
        deviceInput.setHint("Örn: 3A1B9C2D4E5F6789");deviceInput.setTextSize(19);deviceInput.setTypeface(Typeface.MONOSPACE,Typeface.BOLD);deviceInput.setGravity(Gravity.CENTER);deviceInput.setPadding(Ui.dp(this,10),0,Ui.dp(this,10),0);deviceInput.setBackground(Ui.bg(Ui.rgb("#0D202A"),Ui.rgb("#26505F"),14,this));
        root.addView(deviceInput,lp(-1,60,5,10));

        TextView gen=button("AKTİVASYON KODU ÜRET",Ui.rgb("#FFC94A"),Ui.rgb("#151006"));
        gen.setOnClickListener(v->generate());root.addView(gen,lp(-1,58,8,12));

        TextView tl=Ui.text(this,"AKTİVASYON KODU",14,Ui.rgb("#DBEFF6"));Ui.bold(tl);root.addView(tl,lp(-1,28,8,0));
        tokenBox=Ui.text(this,"Henüz kod üretilmedi.",12,Ui.rgb("#DDF8FF"));tokenBox.setTypeface(Typeface.MONOSPACE);tokenBox.setTextIsSelectable(true);tokenBox.setGravity(Gravity.TOP|Gravity.START);tokenBox.setPadding(Ui.dp(this,12),Ui.dp(this,10),Ui.dp(this,12),Ui.dp(this,10));tokenBox.setBackground(Ui.bg(Ui.rgb("#081820"),Ui.rgb("#244A59"),14,this));
        root.addView(tokenBox,lp(-1,150,5,10));

        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
        TextView copy=button("KOPYALA",Ui.rgb("#18D9C1"),Ui.rgb("#031315"));copy.setOnClickListener(v->copyToken());
        TextView share=button("PAYLAŞ",Ui.rgb("#173847"),Color.WHITE);share.setOnClickListener(v->shareToken());
        LinearLayout.LayoutParams a=new LinearLayout.LayoutParams(0,Ui.dp(this,58),1f);a.setMargins(0,0,Ui.dp(this,5),0);row.addView(copy,a);
        LinearLayout.LayoutParams b=new LinearLayout.LayoutParams(0,Ui.dp(this,58),1f);b.setMargins(Ui.dp(this,5),0,0,0);row.addView(share,b);root.addView(row,lp(-1,58,8,10));

        status=Ui.text(this,"Hazır",13,Ui.rgb("#5FE3B0"));status.setGravity(Gravity.CENTER);root.addView(status,lp(-1,34,4,8));

        TextView lock=button("UYGULAMAYI KİLİTLE",Ui.rgb("#132A35"),Color.WHITE);lock.setOnClickListener(v->{unlocked=false;showUnlock();});root.addView(lock,lp(-1,54,10,0));
        setContentView(wrap(root));
    }

    private void generate(){
        try{
            String d=ActivationSigner.normalizeDevice(deviceInput.getText().toString());deviceInput.setText(d);
            currentToken=ActivationSigner.generate(d);tokenBox.setText(currentToken);status.setTextColor(Ui.rgb("#5FE3B0"));status.setText("Aktivasyon kodu hazır • Bu kod yalnızca "+d+" cihazında geçerlidir.");
        }catch(Exception e){currentToken="";tokenBox.setText("Kod üretilemedi.");status.setTextColor(Ui.rgb("#FF7777"));status.setText("16 haneli cihaz kodunu kontrol edin.");}
    }

    private void copyToken(){if(currentToken.isEmpty()){toast("Önce aktivasyon kodu üretin");return;}ClipboardManager cm=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);cm.setPrimaryClip(ClipData.newPlainText("KTD Aktivasyon Kodu",currentToken));toast("Aktivasyon kodu kopyalandı");}
    private void shareToken(){if(currentToken.isEmpty()){toast("Önce aktivasyon kodu üretin");return;}Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_SUBJECT,"KTD 3D ANALYZER Aktivasyon Kodu");i.putExtra(Intent.EXTRA_TEXT,currentToken);startActivity(Intent.createChooser(i,"Aktivasyon kodunu paylaş"));}

    private LinearLayout baseRoot(){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(Ui.dp(this,18),Ui.dp(this,12),Ui.dp(this,18),Ui.dp(this,20));r.setBackgroundColor(Ui.rgb("#031017"));return r;}
    private ScrollView wrap(LinearLayout r){ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setClipToPadding(false);s.addView(r);return s;}
    private LinearLayout header(String title,String sub){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setGravity(Gravity.CENTER_HORIZONTAL);
        ImageView icon=new ImageView(this);icon.setImageResource(com.ktd.activationadmin.R.drawable.ic_launcher_foreground);icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);box.addView(icon,new LinearLayout.LayoutParams(-1,Ui.dp(this,76)));
        TextView t=Ui.text(this,title,21,Color.WHITE);t.setGravity(Gravity.CENTER);t.setPadding(Ui.dp(this,4),Ui.dp(this,6),Ui.dp(this,4),Ui.dp(this,6));Ui.bold(t);box.addView(t,new LinearLayout.LayoutParams(-1,LinearLayout.LayoutParams.WRAP_CONTENT));
        TextView st=Ui.text(this,sub,15,Ui.rgb("#B5F5F4"));st.setGravity(Gravity.CENTER);st.setPadding(Ui.dp(this,5),Ui.dp(this,5),Ui.dp(this,5),Ui.dp(this,9));box.addView(st,new LinearLayout.LayoutParams(-1,LinearLayout.LayoutParams.WRAP_CONTENT));
        return box;
    }
    private EditText pinBox(String hint){EditText e=new EditText(this);e.setSingleLine(true);e.setTextColor(Color.WHITE);e.setHintTextColor(Ui.rgb("#6E8996"));e.setHint(hint);e.setTextSize(18);e.setGravity(Gravity.CENTER);e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_PASSWORD);e.setBackground(Ui.bg(Ui.rgb("#0D202A"),Ui.rgb("#26505F"),14,this));return e;}
    private TextView button(String s,int bg,int fg){TextView v=Ui.text(this,s,15,fg);v.setGravity(Gravity.CENTER);Ui.bold(v);v.setBackground(Ui.bg(bg,0,14,this));v.setClickable(true);v.setFocusable(true);return v;}
    private LinearLayout.LayoutParams lp(int w,int h,int top,int bottom){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h<0?h:Ui.dp(this,h));p.setMargins(0,Ui.dp(this,top),0,Ui.dp(this,bottom));return p;}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}
