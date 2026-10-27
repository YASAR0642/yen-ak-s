# KTD 3D ANALYZER v3.9.1 – CANLI GPR / ESP32 Bluetooth prototipi

## Mimari

- **3D ZEMIN TARAMA** ve **3D CANLI TARAMA** mevcut kaynakları ve derinlik formülü değişmeden bırakıldı.
- Ana menüye **CANLI GPR** eklendi. Kendi `LiveGprActivity`, `GprRadargramView`, `gpr/GprProtocol.java` modülleri vardır.
- `BluetoothClient` aynı ESP32 Classic Bluetooth SPP bağlantısını kullanır. GPR ekranı aktifken GPR ASCII satırları okunur, diğer ekranlarda eski sensör okuyucu çalışır.
- USB gradyometre bağlantısı kullanılmaz.

## Gerçek GPR paketi

```text
KTDGPR,META,NS_PER_SAMPLE,0.5,DX_M,0.05,SOURCE,FRONTEND
KTDGPR,TRACE,1,16,2,-1,0,3,-2,4,-5,2,1,0,-1,0,3,-2,1,0
KTDGPR,TRACE,2,16,1,0,-2,2,-1,3,-4,3,0,1,-1,0,2,-3,2,0
```

Gerçek `ns/örnek` (nano saniye/örnek) **harici GPR alıcısı/örnekleme donanımı tarafından ölçülüp raporlanmalı**. Android seçilen zemin `εr` parametresi ile `d = 0.299792458 × (sampleIndex × dtNs − t0Ns) / (2√εr)` hesabını yapar. Bu, yayın hızı modelidir; gerçek derinlik doğrulaması için zaman-sıfır ve εr kalibrasyonu gerekir. `SOURCE=FRONTEND` yazmak tek başına doğrulama değildir.

**Yalnızca tek manyetik ölçüm (`Z=654`, `KTD1,ADC`) varsa gerçek radar izi / nanosaniye elde edilemez.** Böyle bir değer GPR izine dönüştürülüp gerçek gibi çizilmez. İsterseniz manyetik veriyi ayrı renkli zaman-serisi modunda göstermek mümkündür, ama buna GPR radargramı denmez.

## Firmware

`firmware/KTD_ESP32_BT_GPR_391/KTD_ESP32_BT_GPR_391.ino` Arduino IDE'de ESP32 Dev Module (klasik ESP32-WROOM-32 veya ESP32U) için tasarlandı. ESP32-C3 / ESP32-S3 Classic Bluetooth SPP desteklemediği için aynı yazılım onlarda çalışmaz.

- Classic BT adı: **KTD_GPR_V1**
- GPR harici hızlı örnekleyici çıkışı: ESP32 `UART2 RX=GPIO16`, `TX=GPIO17` / 115200 8N1.
- `GPR_START` / `GPR_STOP`: gerçekte alınan izleri aktarır; harici radar verisi yoksa gerçek radar izi çizilmez.
- `GPR_DEMO_START`: yalnızca ekranı denemek için sentetik paket üretir. Kaynak `DEMO` olarak işaretlenir ve gerçek radar sonucu sayılmaz.
- `LIVE_START` / `LIVE_STOP`: sadece basit **GPIO34 analog test** okumasını eski `Z:...` biçiminde gönderir. Mevcut EMS100 sensör donanımının bütün elektronik özelliklerine göre kalibre edilmiş firmware olduğu iddia edilmez.
- `REF`: GPIO34 test referansını yollar.

## Android test

1. ZIP içeriğini GitHub deposuna yükle; GitHub Actions v3.9.1 derlemesini çalıştır.
2. ESP32'ye `KTD_ESP32_BT_GPR_391.ino` yükle, Android Bluetooth ayarlarında `KTD_GPR_V1` ile eşleş.
3. Uygulamada **Bluetooth** bağlantısını kur, **CANLI GPR** aç.
4. Harici gerçek GPR örnekleme kartı henüz yoksa önce **DEMO**'ya basarak renkli radargramın aktığını kontrol et.
5. Gerçek `TRACE` + `META` veren radar ön-uç bağlandığında uygulama örnek zamanını (ns) ve εr üzerinden hesaplanmış mesafeyi gösterir.

## Sınırlamalar

- Bu ilk sürüm veri protokolü + radargram ekranı + ESP32 Bluetooth taşıma prototipidir; RF ön-uç/yüksek hızlı örnekleyici donanımı içermez.
- Bu ortamda Android SDK / fiziksel ESP32 bulunmadığından gerçek APK derlemesi ve cihaz testi yapılmadı.
- PC uygulamasının orijinal `581 bayt/557 örnek` kapalı protokolünü doğrudan kopyalamak yerine PC kodundaki açık `KTDGPR,TRACE` formatı kullanıldı.
- Önceki 3D derinlik modeline dokunulmadı; GPR'deki ns/metre hesaplaması bilimsel olarak farklı bir kanaldır.
