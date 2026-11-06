# KTD Aktivasyon Yönetici — Android v1.0.4

GitHub üzerinde kaynak kodu düzenlenebilen ve **Actions** üzerinden **Debug APK** oluşturulabilen Android projesi.

## GitHub'a yükleme

1. GitHub'da **New repository** oluşturun; görünürlüğü **Private** seçin.
2. ZIP arşivini bilgisayarınızda açın. Arşivin **içindeki** `.github`, `app`, `build.gradle`, `settings.gradle` gibi dosya ve klasörleri deponun **köküne** yükleyin. ZIP'in kendisini tek dosya olarak yüklemeyin.
3. **Commit changes** yapın. Varsayılan dal `main` veya `master` olabilir.
4. GitHub'da **Actions** sekmesinde **Build KTD Aktivasyon Yonetici APK v1.0.4** iş akışını açıp **Run workflow** seçin. `main`/`master` dalına her gönderim de otomatik derleme başlatır.
5. Başarılı çalıştırmanın **Artifacts** bölümünden `KTD-AKTIVASYON-YONETICI-v1.0.4` arşivini indirin; içinde kurulabilir **debug APK** bulunur.

## GitHub'da kodu değiştirmek

`app/src/main/java/com/ktd/activationadmin/MainActivity.java` arayüz kodunu içerir. Dosyayı GitHub'da açıp kalem simgesiyle düzenleyin, commit edin. Derleme yeniden başlar.

## v1.0.4

Kopyala / Paylaş butonları, alt bilgilendirme alanı ve üst yazı okunurluğu için arayüz güncellemesi. Uygulama sürüm numarası ve Actions çıktı dosya adı eşitlendi.

## Önemli güvenlik uyarısı

**Depoyu herkese açık yapmayın.** Projede aktivasyon imzalama özel anahtarı uygulama kaynak kodunda gömülü olarak tutuluyor. Bu bir güvenlik riskidir: APK'yı edinen birisi de anahtarı analiz yoluyla çıkarabilir. Güvenli bir lisans altyapısı için imzalama işlemini yetkili bir sunucuda yapmanız ve mevcut anahtarı değiştirmeniz önerilir.

## Derleme ortamı

GitHub Actions Ubuntu + Java 17 + Android SDK 35 + Gradle 8.9. Bu ZIP içinde Gradle wrapper bulunmaz; iş akışı Gradle'ı kurar. Yerel geliştirme için Android Studio Gradle senkronizasyonu kullanabilirsiniz.

*Not: GitHub Actions derlemesi bu ortamda fiilen çalıştırılarak doğrulanmadı.*
