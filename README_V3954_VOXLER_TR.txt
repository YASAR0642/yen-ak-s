KTD 3D ANALYZER v3.9.54 — VOXLER BENZERI GORSELL ESTIRME

- Kullanici secimi kare duzeni aynen korunur (3x4, 4x4, 20x5 vb.).
- X/Y tarama adimi 30 cm; CSV X/Y koordinatlari cm cinsindedir.
- DenseField gridding: inverse distance weighting (IDW) power=2.
- Eksik taramada 2.35 grid hucre yaricapinin disina deger uydurulmaz.
- Varsayilan interpolasyon yumusatma kapali (Voxler Smooth=0). UI filtre acilirsa uygulanir.
- 4D C derinlik kanali bulunmadiginda modelin tahmini degeri metre cetveli olarak gosterilmez.
- C yoksa 4D yalniz goreli hacim/izoyuzey temsilidir; kesit derinligi fiziksel metre degildir.
- CSV kayit / ESP32 protokolu degistirilmedi. Yeni ESP firmware gerektirmez.
- IDW gercek 3B yeralti hacmi olcmez; XY anomalilerinin interpolasyonudur.

Derleme/donanim testi bu ortamda yapilamadi. Android kaynaklarini GitHub'a yukleyip Actions ile derleyin.
