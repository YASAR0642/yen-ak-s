KTD 3D ANALYZER v3.9.17
- Dosya yeniden oynatma: 40 ms yerine 32 ms adım (yaklaşık %25 hız artışı). Bluetooth gerçek örnekleme frekansı değişmez.
- Ana radar kazancı: 0.0x-10.0x, 0.1x adım; 0.0x görünümü nötrleştirir, 10x yansımayı güçlendirir. Ham SGY ölçümleri korunur.
- G1-G4 kazanç: 0.0x-10.0x, her panel sağa/sola sürüklenerek değişir.
- Derinlik göstergesi: doğrulanabilir ns bilgisi varsa mevcut hesaplanan sayı; yoksa DERİNLİK --.
ÖNEMLİ: Eski kaynak kodun USER_GPR_DEPTH_DISPLAY_SCALE=10000 sayısal görüntüleme çarpanı bilimsel kalibrasyon değildir. Kalibre edilmeden gerçek metre iddiası yapılamaz.
Android Gradle derlemesi ve telefon testi burada yapılmadı.
