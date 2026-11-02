package com.ktd.analyzer;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.Color;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Button;
import android.widget.TextView;
import com.ktd.analyzer.data.ScanData;
import com.ktd.analyzer.data.ScanSessionStore;
import java.util.Locale;

/** Ölçüm matrisinden otomatik hesaplanan denetlenebilir anomali özeti. */
public final class AutoAnalysisActivity extends Activity {
 private void line(LinearLayout host,String text,int size){TextView t=Ui.text(this,text,size,Color.rgb(224,236,242));t.setPadding(Ui.dp(this,12),Ui.dp(this,9),Ui.dp(this,12),Ui.dp(this,9));host.addView(t);}
 private static String meters(float v){return String.format(Locale.forLanguageTag("tr-TR"),"%.2f",v);}
 @Override public void onCreate(Bundle b){super.onCreate(b);
  LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(Ui.dp(this,12),Ui.dp(this,12),Ui.dp(this,12),Ui.dp(this,12));root.setBackgroundColor(Color.rgb(7,16,24));
  Button back=Ui.button(this,"‹  OTOMATİK ANALİZ RAPORU",false);back.setOnClickListener(v->finish());root.addView(back,new LinearLayout.LayoutParams(-1,Ui.dp(this,54)));
  ScrollView sc=new ScrollView(this);LinearLayout content=new LinearLayout(this);content.setOrientation(1);ScanData d=ScanSessionStore.current;
  if(d==null||d.count==0){line(content,"Ölçülmüş tarama verisi bulunamadı. Önce 3D Zemin Tarama bölümünde örnek alıp ardından ANALİZ ET düğmesine basın.",16);}
  else {
   int positive=0,negative=0,neutral=0; float strongestP=0,strongestN=0;int pr=-1,pc=-1,nr=-1,nc=-1; float ref=Float.isFinite(d.reference)?d.reference:512f;
   for(int row=0;row<d.rows;row++)for(int col=0;col<d.cols;col++)if(d.filled[row][col]){
    float z=d.raw[row][col];if(!Float.isFinite(z))continue;
    float delta=z-ref;
    if(delta>10){positive++;if(delta>strongestP){strongestP=delta;pr=row;pc=col;}}
    else if(delta< -10){negative++;if(-delta>strongestN){strongestN=-delta;nr=row;nc=col;}}
    else neutral++;
   }
   line(content,"TARAMA ÖZETİ",21);
   line(content,"Kayıt: "+d.name+"\nÖlçüm noktaları: "+d.count+" / "+(d.rows*d.cols)+"\nIzgara: "+d.cols+" sütun × "+d.rows+" satır\nAdım: "+d.stepCm+" cm\nAlan (merkezler arası): "+meters(Math.max(0,d.cols-1)*d.stepCm/100f)+" × "+meters(Math.max(0,d.rows-1)*d.stepCm/100f)+" m",15);
   line(content,"MANYETİK ANOMALİ DAĞILIMI",20);
   line(content,"Pozitif sapma: "+positive+" hücre\nNegatif sapma: "+negative+" hücre\nReferansa yakın: "+neutral+" hücre\nReferans (Z): "+meters(ref),16);
   if(pr>=0){float raw=d.raw[pr][pc];line(content,"En güçlü pozitif anomali: "+meters(strongestP)+" birim\nKoordinat: X="+meters(d.x[pr][pc])+" m, Y="+meters(d.y[pr][pc])+" m\n800/Z proje karşılığı: "+(raw>0?meters(800f/raw):"—"),16);}
   if(nr>=0){float raw=d.raw[nr][nc];line(content,"En güçlü negatif anomali: −"+meters(strongestN)+" birim\nKoordinat: X="+meters(d.x[nr][nc])+" m, Y="+meters(d.y[nr][nc])+" m\n800/Z proje karşılığı: "+(raw>0?meters(800f/raw):"—"),16);}
   line(content,"YORUM VE SINIRLAR",20);
   line(content,"Pozitif/negatif hücre oranları, metal veya boşluk bulunma olasılığı değildir. Bu cihazın mevcut manyetik ölçümleriyle metalin değerli/değersiz oluşu, demir dışı metalin kesin türü, mezar, dolgu veya boşluk doğrulanamaz. 800/Z proje hesabı ayrıca saha kalibrasyonu gerektirir. 2D, 3D ve 4D aynı örneklemeye ait farklı görselleştirmelerdir.",15);
   line(content,"ÖNERİLEN DOĞRULAMA",20);
   line(content,"Aynı ızgarayı tekrar tarayın. Sensör yüksekliğini ve yönelimini sabit tutun. Şüpheli noktayı farklı yönden ölçün. Bilinen hedefin gerçek derinliği ile proje değerini karşılaştırın. Veri dosyasını saklayın.",15);
  }
  sc.addView(content);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);Ui.applySafeInsets(this,root);
 }
}
