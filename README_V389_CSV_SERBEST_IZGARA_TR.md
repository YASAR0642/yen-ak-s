# KTD 3D ANALYZER v3.8.9 — CSV, Görünüm, Serbest Izgara

## Kullanım

- **CSV / V3D Aç** → **KTD kayıtlarım**: uygulamanın `files/scans` klasörüne kaydedilen tüm CSV'ler listelenir.
- **CSV / V3D Aç** → **Telefondan CSV / V3D seç**: dosya seçiciden başka telefondan alınan CSV dahil, desteklenen standart CSV dosyası açılır. Başka telefondan dosyayı önce Bluetooth, WhatsApp, USB veya dosya aktarımı ile bu telefona kopyalayın.
- **CSV Kaydet** → **KTD kayıtlarıma kaydet** veya **Telefon klasörüne kaydet**. Aynı isimli kayıtlara numara eklenir, eski dosya üzerine yazılmaz.
- **GÖRÜNÜM**: analiz ekranında yumuşatma, ızgara, kazanç, 4D katmanları; 4D ekranında ek anomali eşiği ve hacim yayılımı.
- **CANLI TARAMA**: her yeni başlatma/sıfırlamada yeni referans; deneysel çift yönlü ters cetvel; kaynağa dokunmadan görsel kazanç.

## Desteklenen CSV

1. Başlıklı `X,Y,Z,C`, `X,Y,Z` ve `row,col,...,Z` dosyaları
2. Noktalı virgül / TAB / virgül ayırıcı ve Türkçe ondalık virgül
3. Başlıksız 3-4 sayısal sütunlu kayıtlar, başlıksız sayısal matrisler
4. Tek `Z` sütunlu sinyal dizisi (1D ölçüm). Bu dosyada X/Y saha koordinatları bulunmadığından 3D/4D fiziksel haritası doğru oluşturulamaz.

**Sınır:** Her üreticinin özel CSV'sini otomatik çözmek mümkün değildir. Tanınmayan sütun adları / kodlanmış yapı hata verir; kayıp koordinatlar uydurulmaz. Desteklenmeyen örneği paylaşırsanız eşleme eklenebilir. 50.000 satır/hücre üstü koruma nedeniyle reddedilir.

## Izgara boyutu

- 5x5 şartı yoktur. Tarama ızgarası 2–60 X / 2–60 Y seçilebilir.
- 2D ve CSV açma aşamasında sabit 5x5 veya 25 örnek şartı yoktur.
- 4D en az 2x2 konumsal yerleşim ve 4 ölçüm bekler; birkaç noktadan oluşturulan hacim fiziksel doğrulama sayılmaz.
- Ayrı tek-dipol derinlik modeli için asgari 3x3 / 9 örnek, ayrıca yeterli saha açıklığı, doyumsuzluk ve model uyumu gerekir. Bu yalnız derinlik modelinin kalite kontrolüdür, tarama kısıtlaması değildir.

## Derinlik uyarısı

Çift yönlü `s=(|Z-R|)/(R veya 1023-R)` ve `D=0.50/s` hesabı **deneysel bir görsel ölçek** olup kazıyla doğrulanmış fiziksel metre değildir. Canlı REF her yeni taramada yenilenir. CSV yalnız X,Y,Z,C içerdiğinden yeniden açılan başka cihaz kayıtlarında ilk REF yoksa program bir tahmin kullanır ve bunu gerçek cihaz referansı diye sunmaz. Saha kazı kaydının 8,50–8,95 m olması, tüm hedeflerin otomatik derinlik hesabını doğrulamaz.

## Doğrulama

CSV ayrıştırıcı Java testleri yapıldı: noktalı virgül ve Türkçe ondalık, TAB, başlıklı/başlıksız, eski KTD satır/sütun dosyası, ham matris, 6×6 36 ölçüm ve Ereğli 20×15 300 ölçüm; kayıt–yeniden aç testleri geçti. Android APK ve telefon testi bu ortamda çalıştırılmadı; GitHub Actions derlemesi hâlâ gereklidir.
