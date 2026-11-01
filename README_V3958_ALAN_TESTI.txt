KTD 3D ANALYZER v3.9.58 - 2,00 x 2,80 m AYNI ALAN TARAMA DUZELTMESI

1. Her ORNEK AL komutu yalniz bir koordinatli SAMPLE paketini kabul eder.
2. Pakette X/Y yoksa ya da istenen hucrenin X/Y noktasi ile uyusmuyorsa kareye yazilmaz.
3. Bir sonraki kareye gecmek icin dogru ve yeni cevap gerekli; 6 saniye cevap gelmezse kullanici ayni kareyi yeniden isteyebilir.
4. Cekimde onceki karelerin RAW/Z degerleri degistirilmez; renkler sabit ortak olcekte saklanir.
5. Ilk ORNEK AL: ortam referansi; sonraki her basma tek kare.
6. Mevcut 800/Z degeri C indeksidir, derinlik kalibrasyonu sayilmaz.
7. 2,00 x 2,80 m olcuyu adim 30 cm ile tam esit nokta araliklarina bolmek mumkun olmayabilir. Her yon icin (N-1)*30 cm nokta araliklarini ve kare kenarlarini ayri not edin.
8. Ayni alan tarama testinde sensor yonu, yerden yukseklik ve zemin referansi ayni kalmalidir.

DEGISEN: app/src/main/java/com/ktd/analyzer/ScanActivity.java
ANDROIDE YENI KAYNAKLARI YUKLEYIN; ESP32 V9 KODUNU TEKRAR YUKLEMEYE GEREK YOK.
SAMPLE paketi X=... Y=... Z=... C=... biciminde olmalidir.

Bu surum paket/hucresel kayit tutarliligini iyilestirir; tum fiziksel manyetik sapmalari veya 4D derinlik modelini dogrulamaz.
