package com.ktd.analyzer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.text.InputType;
import android.widget.EditText;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import com.ktd.analyzer.view.ColorMaps;

/** GPR-only visible settings; not the gradiometer settings screen. */
public final class GprSettingsActivity extends Activity {
    private int dp(int v){return Ui.dp(this,v);}
    @Override public void onCreate(Bundle state){super.onCreate(state);
        ColorMaps.configure(this);
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(20),dp(14),dp(20),dp(20));body.setBackgroundColor(Color.rgb(5,15,23));
        TextView title=Ui.text(this,"KTD GPR SYSTEM · AYARLAR",22,Color.WHITE);title.setGravity(Gravity.CENTER);
        body.addView(title,new LinearLayout.LayoutParams(-1,dp(58)));
        Button create=Ui.button(this,"+ RENK OLUŞTUR · 10 NOKTA",true);
        create.setOnClickListener(v->CustomPaletteEditor.show(this,()->ColorMaps.configure(this)));
        body.addView(create,new LinearLayout.LayoutParams(-1,dp(58)));
        Button saved=Ui.button(this,"KAYITLI RENKLER · DÜZENLE / KAYDET / SİL",true);
        saved.setOnClickListener(v->CustomPaletteEditor.show(this,()->ColorMaps.configure(this)));
        body.addView(saved,new LinearLayout.LayoutParams(-1,dp(56)));
        Button signal=Ui.button(this,"GPR SİNYAL / FİLTRE AYARLARI",true);
        signal.setOnClickListener(v->signalDialog());
        body.addView(signal,new LinearLayout.LayoutParams(-1,dp(56)));
        Button distance=Ui.button(this,"MESAFE KALİBRASYONU · GERÇEK HAT (m)",true);
        distance.setOnClickListener(v->distanceDialog());
        body.addView(distance,new LinearLayout.LayoutParams(-1,dp(56)));
        Button smart=Ui.button(this,"AKILLI ANOMALİ RENKLERİ",true);
        smart.setOnClickListener(v->SmartColorDialog.show(this,()->ColorMaps.configure(this)));
        body.addView(smart,new LinearLayout.LayoutParams(-1,dp(56)));
        TextView note=Ui.text(this,"Kayıtlı paletlerini GPR ekranındaki RENK düğmesine basarak sırayla seçebilirsin. Renk ve anomali eşikleri görsel yorumdur; ölçüm verisini değiştirmez.",14,Color.rgb(181,207,220));
        body.addView(note,new LinearLayout.LayoutParams(-1,dp(95)));
        Switch voice=new Switch(this);voice.setText("TÜRKÇE SESLİ YÖNLENDİRME");voice.setTextColor(Color.WHITE);
        voice.setChecked(getSharedPreferences("gpr_options",0).getBoolean("voice",true));
        voice.setOnCheckedChangeListener((v,on)->getSharedPreferences("gpr_options",0).edit().putBoolean("voice",on).apply());
        body.addView(voice,new LinearLayout.LayoutParams(-1,dp(65)));
        TextView caveat=Ui.text(this,"GPR derinliği yalnızca doğrulanmış ns/örnek bilgisi ve zemin dielektrik katsayısı ile hesaplanabilir. Mavi/kırmızı renk malzeme tespiti değildir.",13,Color.rgb(159,194,209));
        body.addView(caveat);
        Button back=Ui.button(this,"GPR EKRANINA DÖN",false);back.setOnClickListener(v->finish());
        body.addView(back,new LinearLayout.LayoutParams(-1,dp(58)));
        ScrollView scroll=new ScrollView(this);scroll.addView(body);setContentView(scroll);
    }
    private void signalDialog(){
        LinearLayout layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20),dp(8),dp(20),dp(12));
        TextView desc=Ui.text(this,"TORK GPR benzeri görünüm filtreleri. Yalnız ekrandaki görünümü değiştirir; SGY kayıt verisine dokunmaz.",14,Color.WHITE);
        layout.addView(desc);
        Spinner filter=new Spinner(this);
        filter.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,
            new String[]{"FİLTRE YOK · ORİJİNAL","DC DÜZELTME","ZEMİN / ARKA PLAN ÇIKAR","DİKEY KENAR VURGUSU"}));
        filter.setSelection(getSharedPreferences("gpr_options",0).getInt("display_filter",0));
        layout.addView(filter);
        Switch contrast=new Switch(this);contrast.setText("OTOMATİK KONTRAST / AMPLİFİKATÖR");contrast.setTextColor(Color.WHITE);
        contrast.setChecked(getSharedPreferences("gpr_options",0).getBoolean("auto_contrast",true));
        layout.addView(contrast);
        new AlertDialog.Builder(this).setTitle("GPR GÖRÜNÜM AYARLARI").setView(layout)
            .setPositiveButton("KAYDET",(d,w)->getSharedPreferences("gpr_options",0).edit()
                .putInt("display_filter",filter.getSelectedItemPosition())
                .putBoolean("auto_contrast",contrast.isChecked()).apply())
            .setNegativeButton("İPTAL",null).show();
    }
    private void distanceDialog(){
        String file=getIntent().getStringExtra("gpr_file_key");
        double source=getIntent().getDoubleExtra("gpr_header_distance",Double.NaN);
        int traceCount=getIntent().getIntExtra("gpr_trace_count",0);
        if(file==null||file.trim().isEmpty()||traceCount<2){
            new AlertDialog.Builder(this).setTitle("Mesafe kalibrasyonu")
                .setMessage("Önce SGY/CSV dosyasını açın. Bu ayar yalnız seçili dosyada uygulanır; canlı cihazın yürüme metresi için tekerlek/konum verisi gerekir.")
                .setPositiveButton("TAMAM",null).show();return;
        }
        double saved=GprDistanceCalibration.get(this,file);
        LinearLayout layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(22),dp(12),dp(22),dp(8));
        TextView info=Ui.text(this,"Dosya: "+file+"\nSEG-Y başlık mesafesi: "+
            (Double.isFinite(source)?String.format(java.util.Locale.forLanguageTag("tr-TR"),"%.3f m",source):"Bilinmiyor")+
            "\nSahada ölçtüğünüz gerçek hat uzunluğunu girin. Diğer dosyaları etkilemez.",14,Color.WHITE);
        layout.addView(info);
        EditText length=new EditText(this);length.setSingleLine(true);length.setTextColor(Color.WHITE);
        length.setHint("Gerçek mesafe, örn. 14,00");length.setHintTextColor(Color.LTGRAY);
        length.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if(Double.isFinite(saved))length.setText(String.format(java.util.Locale.US,"%.3f",saved));
        layout.addView(length);
        new AlertDialog.Builder(this).setTitle("DOSYAYA ÖZEL MESAFE KALİBRASYONU")
            .setView(layout).setPositiveButton("KAYDET",(d,w)->{
                try{double meters=Double.parseDouble(length.getText().toString().replace(',','.'));
                    if(meters<=0||meters>100000)throw new IllegalArgumentException();
                    GprDistanceCalibration.set(this,file,meters);
                    Toast.makeText(this,"Mesafe kaydedildi: "+meters+" m",Toast.LENGTH_LONG).show();
                }catch(Exception e){Toast.makeText(this,"Geçerli bir mesafe girin",Toast.LENGTH_LONG).show();}
            }).setNeutralButton("SIFIRLA",(d,w)->{
                GprDistanceCalibration.clear(this,file);
                Toast.makeText(this,"Dosya mesafesi başlık değerine döndü",Toast.LENGTH_SHORT).show();
            }).setNegativeButton("İPTAL",null).show();
    }
}
