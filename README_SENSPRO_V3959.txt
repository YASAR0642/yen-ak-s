KTD 3D ANALYZER v3.9.59 Sens Pro uyumluluk gözlem ekranı

- Temel 3D grid, 2D/3D/4D, NORMAL ve GMR görüntüleri korunur.
- Yeni 2D bilgi satırında, ESP32 tarafından verilen ADC10= alanı ve Nano firmware karar aralığı görünür.
- Eski firmware eşikleri: A<180; B: 200..260; C>=366. Diğerleri tanımsız.
- Eşiklerden metal/boşluk kimliği doğrulanmamıştır.
- 800/Z sonucu hesaplanmış C indeksidir; doğrulanmış fiziksel derinlik değildir.
- Android sensör işleme ve mevcut gradyan eşiği değiştirilmemiştir; bu sürüm uyumluluk ölçümünü ekler.
- Arduino: A2 lower OUT voltajından (bölücü düzeltildikten sonra) 5V varsayımıyla 10-bit ADC10 hesaplanır.
- Analog giriş/hardware farklılığı nedeniyle sonuç eski Nano'nun aynısı olduğunu kanıtlamaz.
