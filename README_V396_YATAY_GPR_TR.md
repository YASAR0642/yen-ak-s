# KTD 3D ANALYZER v3.9.6 — YATAY CANLI GPR

v3.9.5 16-haneli aktivasyonlu Android projesinden türetilmiştir.

## Değişiklikler

- CANLI GPR açıldığında etkinlik **sensorLandscape** (yatay) açılır. Diğer ekranlar dikey kalabilir.
- Solda geniş B-scan radargram, ortada dört ayrı örnek A-scan profil paneli, sağda iki sütunlu GPR kontrol menüsü.
- Siyah-beyaz, mavi/kırmızı ve sepya renk haritaları; kazanç, otomatik kontrast, DC/arka plan/kenar görsel filtreleri, hiperbol vurgusu (kenar kontrastı), sesli uyarı, zaman penceresi ve eps-r zemin seçimi.
- Bluetooth COM, GPR_START/GPR_STOP, açılır menüden GPR_DEMO_START.
- SGY AÇ: SEG-Y dosya okuyucu (IBM Float 1, int32 2, int16 3, IEEE float 5, int8 8). Ayrıca KTD GPR CSV yeniden açma.
- KAYDET: gerçek radar örneklerinin tüm değerlerini ve META varsa ns/örnek, dx ile CSV kaydı.
- PC KTD GPR v8.5 tarafından üretilen SGY dosyalarında `SAMPLE INTERVAL HEADER=1 US PLACEHOLDER` veya `REAL ACQUISITION DT UNVERIFIED` başlığı algılanır. **Yanlış metre hesabı yapılmasın diye ns aralığı bilinmiyor kabul edilir**.
- ⋮ > NS/ÖRNEK KALİBRASYONU: ancak gerçek donanımdan veya doğrulanmış ölçümden bilinen ns/örnek değeri girilebilir. Boş bırakınca dosya/cihaz bilgisine dönülür.
- 16-haneli aktivasyon, KTD Kişileri, CSV/WhatsApp entegrasyonu ve mevcut gradyometre derinlik algoritması **değiştirilmedi**.
- Ekrandaki uzun uyarı/açıklama alanları kaldırıldı.

## Doğruluk notları

- Radargram gerçek KTDGPR,TRACE ölçüm örneklerini çizer; tek gradyometre ADC değeri radar izine çevrilmez.
- Soldaki eksen **ns** olarak yalnız donanımdan veya güvenilir dosya bilgisinden gerçek ns/örnek geliyorsa gösterilir. Aksi hâlde **ÖRNEK** ekseni görünür, metre ekseni çizilmez.
- Metre cetveli standart çift yönlü GPR uçuş süresi `d(m) = 0.299792458 * t(ns)/(2 * sqrt(epsilonR))` kullanır. Zemin dielektrik sabitine, zaman sıfırına ve kalibrasyona duyarlıdır; kesin derinlik değildir.
- ZAMAN düğmesi **görüntülenen pencereyi** değiştirir, gerçek örnekleme zamanını üretmez.
- TX/RX anten seçimi bu sürümde **arayüz etiketi/presetidir**; ESP32 donanım frekansı ayarlanmaz.
- G1–G4, mevcut radar izlerinden seçilen dört A-scan profilidir; dört ayrı fiziksel alıcı kanalı değildir.
- HİPERBOL bir görsel keskinlik/vurgu filtresidir; doğrulanmış hedef veya otomatik tespit değildir.
- Enkoder bulunmadığı sürece X ekseni **iz sayısıdır**. DX_M ancak cihazın gerçek mesafe bilgisi varsa kullanılmalıdır.

## Kaynak ve APK

`app` + `.github/workflows/main.yml` dahil ZIP kökünü GitHub deposuna yükleyin; GitHub Actions `KTD 3D ANALYZER 3.9.6 APK Derle` iş akışını çalıştırın.

Testler: GprProtocol/GprFileIo Java derlemesi; KTD GPR CSV kaydet-yeniden aç roundtrip; 16-bit SEG-Y okuma; PC SGY yer tutucu zaman bilgisi kontrolü; standart ns->m denklemi. Tam Android SDK bu çalışma ortamında bulunmadığından APK cihaz testi yapılmadı.
