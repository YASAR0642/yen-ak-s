KTD 3D ANALYZER v3.9.16 - Android GPR düzenleme

1. Dosya önizlemesi ve dosya oynatma aynı yatay iz örneklemesini kullanır (en fazla 180 gösterim sütunu). Böylece SGY tamamının önizlemesi ile oynatma sonunda görünmesi gereken geometri eşleşir. Canlı Bluetooth izleri ise kendi kayan penceresinde kalır.
2. G1-G4 grafiklerinin her birinde parmakla sağa/sola sürükleyerek bağımsız kazanç 0.2x - 8x ayarlanır. Grafikler farklı kaydedilmiş iz noktalarını gösterir; tek kanallı SGY dosyasından dört bağımsız sensör ölçümü oluşturulmaz.
3. Yükseklik ve kenar boşlukları ekran yoğunluğuna (dp) bağlı ayarlanır; eksen rakamları seyrekleştirildi.
4. Yatay mesafe ancak DX_M bilgisi varsa metre olarak çıkar; yoksa MESAFE BİLİNMİYOR gösterilir. NS bilgisi ölçüm olarak doğrulanmadıysa derinlik uydurulmaz.
5. Dosya okuyucudaki 1600 iz sabit sınırı kaldırıldı. Yüksek sayıda dosya izi hâlâ cihaz belleğiyle sınırlıdır.

NOT: Java/Android tam derleme bu ortamda yapılmadı. GPR kayıtlarının tek kanallı olduğu durumda G1–G4 aynı ölçümün farklı izlerinden elde edilir, gerçek dört kanallı sistem değildir.
