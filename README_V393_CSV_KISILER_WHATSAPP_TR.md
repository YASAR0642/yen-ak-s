# KTD 3D ANALYZER v3.9.3 — CSV / WHATSAPP / KİŞİLER

## Bu sürümde

1. **WhatsApp'ta CSV'ye basınca KTD 3D ile aç:** Android ACTION_VIEW, ACTION_SEND ve ACTION_SEND_MULTIPLE niyetleri tanımlı. WhatsApp'ın kullandığı yaygın CSV MIME türleri desteklenir. Uygulama gelen URI'yi okur; ölçüm içeriği analiz için uygunsa KTD Kayıtlarım'a özgün veriyle kopyalar.
2. **Dosya adı ile sınıflandırma yapılmaz:** `Ali.csv`, `kazı.csv`, `1.csv` gibi her ad kabul edilir; tanıma dosya *içeriğine* bakar. Orijinal dosya adı saklanır. İsim çakışmasında `_2` gibi ek verilir.
3. **KAYITLAR > CSV'LERİ TARA:** Android MediaStore üzerinde görünür CSV/V3D belgelerini listeler. **KLASÖR EKLE** ile Android tarafından erişim verilen bir klasörün alt klasörleri de taranır. **TELEFONDAN/TABLETTEN CSV AÇ** sistem dosya seçicisidir. Android'in korumalı uygulama dizinlerinin tamamını izinsiz taramak mümkün değildir. Bulut sağlayıcılarda da erişim sağlayıcının iznine bağlıdır.
4. **Dosya paylaşımı:** CSV gönderimi yalnız CSV URI ve `text/csv` MIME türü kullanır. PNG görsel paylaşımı gerekiyorsa ayrı menüden gönderilir.
5. **KTD KİŞİLERİ:** Android rehberinden kullanıcı tarafından seçilen telefon numaraları KTD'de özel yerel hafızada saklanır; silinebilir ve kayıtlı kişiyle WhatsApp sohbeti açılabilir. Geniş `READ_CONTACTS` izni veya arka planda rehber taraması yoktur. KTD kişi verilerini otomatik olarak sunucuya göndermez.
6. **WHATSAPP GRUP GÖRÜŞMESİNE GİT:** WhatsApp'ı açar ve WhatsApp'ın grup araması adımlarını açıklar. WhatsApp'ın grup aramasını Android üçüncü taraf uygulamalarına otomatik başlatma/katılımcı atama yetkisi bulunmadığından KTD içinden otomatik konferans başlatmaz.
7. **Gradyometre derinliği / referans / 3D / 4D / CANLI GPR:** Hesap motorları değiştirilmedi.

## Önemli kurulum notları

- ZIP'i açıp projeyi GitHub'a yükleyin; içindeki `.github/workflows/main.yml` GitHub Actions'ta Android debug APK derlemesi içindir. Projede 100'den az dosya vardır.
- Mevcut APK'ya yüklerken imza uyumsuzluğu oluşabilir. Yerel CSV'leri önce dışarı paylaşarak yedekleyin; uygulamayı silmek özel KTD kayıtlarının silinmesine yol açabilir. Kalıcı güvenli güncellemeler için aynı imzalama anahtarı kullanılmalıdır.
- Yeni APK kurulmadan Android'in açma menüsünde KTD görünmeye başlamaz. WhatsApp'ın kendi sürümüne göre paylaşım/açma menüsü farklı olabilir.
- Başka cihaz CSV'sinin açılabilmesi sütun ve ölçüm yapısının okuyucu tarafından tanınmasına bağlıdır. CSV adında `Tarama` kelimesi zorunlu değildir.
- Android 13+ korumalı dosya dizinleri doğrudan listelenemeyebilir. Sistem dosya seçicisinden açma her zaman birincil yedek yöntemdir.
- Bluetooth ile kullanılan ESP32 firmware ve GPR protokolü aynı kalır.

## Derleme doğrulaması

Kaynak paketinde Android manifest, CSV veri işleme ve gezinme kodları bulunmaktadır. Gerçek APK Android SDK ile GitHub Actions üzerinde derlenmeli ve telefonlarda test edilmelidir. Yerel çalışma alanında Android SDK olmadığı için burada APK derlemesi yapılmadı.
