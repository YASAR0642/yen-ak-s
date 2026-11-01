KTD 3D ANALYZER v3.9.52 - GERCEK EMS100 256 ORNEK GOSTERIMI

Bu yazilim sadece MagneticRadarActivity / MagneticRadargramView (KTD GMR) bolumunun
veri-goruntu modelini degistirir; NORMAL / ayri GPR modunun sentez algoritmasina dokunmaz.

Her ESP32 sensor paketi, Android tarafinda mevcut ReferenceModel uzerinden normalize edilir.
Her gelen manyetik delta degeri en fazla 256 orneklik kayan pencereye eklenir.
Her radargram sutunu bu gercek son 256 manyetik olcumun zaman gecmisidir.
Eksik baslangic ornekleri 0 ile doldurulur; bunlar sensor olcumu degildir.
Renkler goreli sensör degerleridir; metre, ns, nT veya fiziksel hedef turu belirtmez.
Yapay hiperbol/Ricker/sinus katmanlari artik uretilmez.
Dikey cetvel ORNEK 0..255, yatay cetvel gelen olcum sayisidir.
Kazanc gosterdigi kontrasti etkiler, fiziksel sensor hassasiyetini artirmaz.

KISIT:
Bu gercek yeraltı radargrami DEGIL, gercek manyetik olcumlerin kayan
zaman gecmisi haritasidir. ADS1115 ile EMS100 sensorleri farkli derinliklerden
bagimsiz GPR zaman ornekleri kaydedemez.

ESP32 V7 firmware bu Android degisikligi icin yeniden yuklenmek zorunda degildir.
APK derlemesi burada yapilamadi; Github Actions ile test ediniz.
