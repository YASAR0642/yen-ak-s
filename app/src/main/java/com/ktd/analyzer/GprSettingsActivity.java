package com.ktd.analyzer;

import android.app.Activity;
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
        Button smart=Ui.button(this,"AKILLI ANOMALİ RENKLERİ",true);
        smart.setOnClickListener(v->SmartColorDialog.show(this,()->ColorMaps.configure(this)));
        body.addView(smart,new LinearLayout.LayoutParams(-1,dp(56)));
        TextView note=Ui.text(this,"Kayıtlı paletlerini GPR ekranındaki RENK menüsünden seçebilirsin. Renk ve anomali eşikleri görsel yorumdur; ölçüm verisini değiştirmez.",14,Color.rgb(181,207,220));
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
}
