KTD 3D ANALYZER v3.9.62 — GER referansli yorum paleti ve 2D yumusak kontur

KAYNAK: kullanicinin paylastigi GER 3D VIEWER ekran goruntuleri; yazilim kopyasi degildir.

YAPILANLAR
- 13. palet: GER / KTD Anomali. Negatif sinyal mavi; dogal referans yesil;
  kucuk pozitif sapma sari; yuksek pozitif sapma turuncu/kirmizi.
- 2D yumusak harita, mevcut bilineer enterpolasyona ilave medyan ve yumusatma
  ile daha genis, surekli renk konturlari gosterir. HAM kayitlara dokunmaz.
- 2D, 3D ve 4D icin GER paletindeki delta esikleri aynidir. Goruntuler
  farkli interpolasyon veya kamera nedeniyle tam ayni piksel olmayabilir.
- Diger 12 palet ve eski ekranlar korunur. Son paleti secip deneyin.
- ESP32 ve Bluetooth komutlari degistirilmedi.

SINIRLAMALAR
- Esikler deneysel GORSEL sinyal siniflaridir; GER yaziliminin kapali
  kaynak algoritmasindan cikarilmis metal/bosluk kodu DEGILDIR.
- Yalniz EMS100 ile demir-demir disi, kazilmis toprak, bosluk, mezar,
  kup ve 3 m gercek derinligi kesin tanimlamak bilimsel olarak mumkun degildir.
- 4D, manyetik orneklerden uretildigi icin rekonstruksiyon gorunumudur;
  olculen derinlik katmanlari olarak degerlendirilmemelidir.
- Bu oturumda Android SDK/Gradle bulunmadigi icin APK derlemesi
  dogrulanamamistir. GitHub Actions ile test edilmelidir.
