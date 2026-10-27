KTD 3D ANALYZER v3.9.0 — 4D SAYDAM DOĞAL ZEMİN + TELEFON CSV ERİŞİMİ
============================================================================

YENİLİKLER

1. 4D sahne: doğal zeminin dış sınırı saydam bir kabuk olarak çizilir.
   Opak/anlaşılır anomali çekirdeği + yarı saydam dış anomali katmanı
   kullanılır. X/Y yatay ve Z derinlik yönündeki iç kesit kılavuz çizgileri
   4D kutuda gösterilir (Izgara açıkken).

2. Görünüm (ana ayarlar veya analiz ekranı):
   - Doğal zemin görünürlüğü: %2–35 (düşük sayı daha saydam)
   - Anomali keskinliği: 0,7–2,5x
   - Yumuşatma ve iç kılavuz çizgileri
   - Mevcut anomali kazancı, hacim yayılımı, katman sayısı, eşik
   Bu parametreler yalnız çizim katmanlarını değiştirir. Ölçüm/derinlik
   hesabını, Bluetooth, C, Z, CSV içerik modelini değiştirmez.

3. Android telefon/tablet CSV erişimi:
   - ANALİZ > CSV / V3D Aç > Telefonda/tabletteki CSV veya V3D'yi seç
   - KAYITLAR > TELEFONDAN / TABLETTEN CSV AÇ
   Android'in belge seçicisinde İndirilenler, Belgeler, kullanılabilir
   diğer depolama klasörleri ve desteklenen bulut sağlayıcıları açılabilir.
   - KAYITLAR üzerinden birden çok dosya seçilir, tamamı ayrı isimlerle
     KTD kayıtlarına içe aktarılır; tek seçimde dosya analize de açılır.
   - Eski kayıtların üzerine yazılmaz; başarısız dosyalar bildirilir.
   - CSV içeriğinin X/Y/Z(C) veya tanınabilir benzer sayısal bir biçim
     kullanması gerekir; üreticiye özel şifreli/ikili dosyaların tamamı
     garanti edilmez.

4. Referans, ters derinlik cetveli, anomali sınıflandırması:
   v3.8.9 ile BİREBİR AYNI KOD. Bu güncellemede dokunulmadı.

ÖNEMLİ SINIRLAR
- Hacim renklendirmesi ham manyetik/ADC anomalisine göredir.
  ADC çıkışını kalibre edilmiş mV sanmayın. Fiziksel gerilim çevrimi için
  kartın Vref ve yükselteç transfer fonksiyonu gerekir.
- Hacim şekli 4D görselleştirme/ters modeldir. Kaya içi mağara, yeniden
  doldurulmuş kazı sınırları veya m³ değeri ek doğrulama olmadan kesin
  sonuç olarak kabul edilemez.
- Android'in özel uygulama dizinleri/korumalı klasörleri tümüyle taranmaz;
  dosyaları Android sistem dosya seçicisi ile kullanıcı seçer.

DERLEME
ZIP'i açıp içeriğini GitHub deposunun köküne yükleyin; .github/workflows/main.yml
APK kaynak derlemesini çalıştırmak içindir. GitHub Actions > Run workflow
-> Artifacts -> APK.
Bu ortamda Android SDK olmadığından derlenmiş APK/telefon testi yapılamadı.
Sözdizimi kontrolü ve paket bütünlüğü yapılmıştır.
