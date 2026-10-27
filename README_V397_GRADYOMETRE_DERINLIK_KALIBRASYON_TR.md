# KTD 3D ANALYZER 3.9.7 - GRADYOMETRE DERINLIK KALIBRASYONU

## Neden?

Saha raporu: Gradyometre 4 m bilinen noktayi yaklasik 40 m olarak gosteriyor.
Bu, GPR ya da SEG-Y / ns problemi degildir.

Kaynak formulu `ReferenceInverseDepth.measure`:
  s = |Z-REF| / (1023-REF) [pozitif] ya da |Z-REF|/REF [negatif]
  D0 = 0.50/s  (deneysel, dogrudan fiziksel metre degil)

Ornek REF=880, Z=869 icin s=11/880=0.0125 => D0=40.0.
Bu bir 10x unit parsing hatasi oldugunu kanitlamaz: ters oranti kucuk sapmalarda buyuk metre onerir.

## Duzeltme

Yeni GRADYOMETRE METRE KALIBRASYONU ekrani. Bilinen hedefin uygulamada
GORULEN degeri ve sahadaki gercek, dogrulanmis derinligini gir.
Ornek: Gosterilen = 40; bilinen = 4; mevcut carpan = 1 -> yeni carpan 0.10.
Yeni gorunum degeri = D0 * carpan; hem referans ustu hem alti icin ortaktir.

Kalibrasyon alanlari:
- Ayarlar -> GRADYOMETRE DERINLIK
- 2D tarama -> DERINLIK dugmesine UZUN BAS
- 3D/4D -> GORUNUM -> GRADYOMETRE METRE KALIBRASYONU

Katsayi Android telefonda kaydedilir ve sonraki taramalarda da kullanilir.
SIFIRLA (1x) ile varsayilana donulebilir. Saha/zemin degisirse yeniden kalibre et.

DOGRULANDI: 40.00 -> 4.00 ornek verisi javac hesap testini gecti.
Kaynak kodu fiziksel laboratuvar / telefon APK kurulumu ile henüz test edilmedi.

## Korunanlar

- GPR yatay ekran, SEG-Y ve ns/metre hesabi DEGISMEDI.
- ESP32 Bluetooth paketleri, REF yenileme, Z ham degeri DEGISMEDI.
- GPR ve gercek cihazin C alani DEGISMEDI.
- CSV'de yalniz X,Y,Z,C bulunur; orijinal 4 sutun DISINDA meta veri eklenmedi.
- 2D/3D/4D gradyometre deneysel derinlikleri ayni yeni carpanla gosterilir.
- Canli gradyometre deneysel metre yazisi ayni carpanla gosterilir.
- 16 haneli aktivasyon ve WhatsApp CSV / kisiler kodlari DEGISMEDI.

## Bilimsel sinir

Tek bir noktada 40 metreyi 4 metreye getirmek tum hedeflerde dogrulugu kanitlamaz.
Fluxgate alan siddetinden kesin hedef derinligi ve hacmi tek basina cikarilamaz.
Birden fazla bilinen derinlik ve turde hedefle saha kalibrasyonu onerilir.
CSV yalniz 4 sutun tasidigi icin uygulama ayari bir baska telefona CSV ile
aktarilmaz. Onun da kendi kalibrasyon ayari yapilmalidir.

## GitHub

Acilan ZIP'deki tum dosyalari depoya yukle; .github/workflows/main.yml
ile Actions uzerinden APK olustur.
