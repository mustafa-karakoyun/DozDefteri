# Faz 5 — Dışa aktarım ve yedek

## Yapılanlar
- **CSV** (`CsvExporter`, saf Kotlin): UTF-8 BOM, virgül ayırıcı, RFC 4180 tırnaklama, CRLF; sütunlar `date, planned_time, medication, dose, unit, status, action_time, profile` (başlıklar ve durum metinleri uygulama diline göre); planlanan zamana göre artan sıralama. Dosya adı `dozdefteri_{profil}_{baslangic}_{bitis}.csv`, profil adı ASCII'ye sadeleştirilir (`FileNames`).
- **PDF** (`PdfExporter`, `PdfDocument`): A4 595×842 pt, 40 pt kenar, sans-serif, gövde 10 pt, başlık 16 pt. Basit şablon: başlık, profil, tarih aralığı, oluşturulma tarihi, ilaç tablosu (ad, doz, plan özeti), doz tablosu (tarih, saat, ilaç, durum). Detaylı şablon (Pro): ek olarak ilaç bazlı uyum tablosu ve genel uyum. Sayfa taşınca tablo başlığı yeni sayfada tekrarlanır; her sayfada sorumluluk reddi altbilgisi ve "1/3" sayfa numarası. İçerik metinleri `ExportContentBuilder`'da yerelleştirilir.
- **JSON yedek** (`BackupCodec` + `BackupManager`): `kotlinx.serialization` ile ayrı `Backup*` DTO'ları, `format`/`schemaVersion`/`appVersionCode`/`exportedAt` alanları, dosya adı `dozdefteri_yedek_{yyyy-MM-dd}.json`. Geri yüklemede format, sürüm ve iç referanslar (profil→ilaç→plan→doz) doğrulanır; onay diyaloğundan sonra tek transaction'da tüm tablolar silinip yedek ID'leriyle eklenir; ardından aktif profil = ilk profil, tüm bildirimler iptal, `DoseSync.run("restore")`.
- **Dışa aktar ekranı** (Bölüm 6.7): profil seçimi (birden fazla profil varsa; "Tüm profiller" Pro, değilse kilitli ve Pro ekranına yönlendirir), tarih aralığı (7/30/90/Özel + tarih seçiciler), "CSV olarak kaydet", "PDF rapor oluştur", "Yedekle", "Yedekten geri yükle". Dosyalar `CreateDocument`/`OpenDocument` (SAF) ile, depolama izni yok. Kaydettikten sonra Snackbar'da "Paylaş" (ACTION_SEND).
- `ProStatus` (okuma tarafı): `pro_cached || DEBUG_FORCE_PRO`. Faz 7'de Billing bu önbelleği güncelleyecek.
- Yedek alındığında `last_backup_reminder_at` güncellenir (30 günlük hatırlatma sıfırlanır).
- Testler: `CsvExporterTest` (BOM, tırnaklama, sütun sırası, dosya adı), `BackupCodecTest` (gidiş-dönüş, yanlış format, yeni sürüm, bozuk JSON, kopuk referans), `BackupManagerTest` (Robolectric: dışa aktar → başka veritabanına içe aktar → eşit; yanlış format reddi ve verinin korunması).

## Oluşturulan / değişen dosyalar
- `export/{CsvExporter,PdfExporter,PdfReport,BackupModels,BackupManager,DocumentIo,ExportContentBuilder,FileNames}.kt`
- `billing/ProStatus.kt`
- `ui/export/{ExportViewModel,ExportScreen}.kt`, `ui/navigation/AppNavHost.kt`
- `data/repository/MedicationRepository.kt` (`getActiveWithSchedules`)
- `res/values{,-tr}/strings.xml`
- `test/.../export/{CsvExporterTest,BackupCodecTest,BackupManagerTest}.kt`

## Komut sonuçları
- assembleDebug: ❌ çalıştırılamadı (dl.google.com engeli)
- testDebugUnitTest: ❌ çalıştırılamadı; JVM doğrulama projesinde saf testler 39/39 ✅ (yeni: CsvExporter 4, BackupCodec 5). `BackupManagerTest` Robolectric gerektirdiği için koşturulamadı.
- lintDebug: ❌ çalıştırılamadı

## Kabul kriterleri
- [ ] CSV Excel'de Türkçe karakterlerle doğru açılır — BOM testli; Excel'de insan doğrulaması gerekli
- [ ] PDF birden fazla sayfaya taşar ve başlık tekrarlanır — sayfalama mantığı yazıldı; cihazda 90 günlük raporla doğrulanmalı
- [ ] Yedek geri yükleme sonrası alarmlar yeniden kurulur — `confirmRestore` → `DoseSync.run("restore")`; cihazda doğrulanmalı

## Verilen kararlar
- **Yedek iki adımlı:** dosya önce okunup doğrulanır; yalnızca geçerliyse onay diyaloğu gösterilir, onaydan sonra yazılır. Geçersiz dosyada mevcut veriye hiç dokunulmaz (Kural 0.4-2).
- **Referans doğrulaması** eklendi (plan istemiyordu): kopuk referanslı bir yedek foreign-key hatasıyla yarıda kalmasın diye baştan reddedilir.
- **Yedek uyumluluğu:** `ignoreUnknownKeys = true`; aynı `schemaVersion` içinde gelecekte eklenecek alanlar eski sürümü bozmaz. Farklı `schemaVersion` reddedilir.
- **CSV'de doz miktarı** uygulama diline göre biçimlenir (TR'de "0,5"); virgül içerdiğinde RFC 4180 tırnaklamasıyla korunur, Excel TR'de sayı olarak okunur.
- **CSV başlıkları** TR'de `tarih, planlanan_saat, ilaç, ...` (plan "başlıklar uygulama diline göre" diyor); sütun sırası sabit.
- **PDF metin kırma:** tablo hücreleri sütun genişliğine sığmazsa "…" ile kısaltılır (satır yüksekliği sabit kalır).
- **"Tüm profiller" dosya adı** profil kısmı `all` olur.
- **Paylaş** butonu, kullanıcının seçtiği SAF URI'sini okuma izniyle paylaşır; ayrı önbellek dosyası oluşturulmaz.

## Çözülemeyen sorunlar
- Derleme/çalıştırma yok (ortam engeli).

## Kullanıcının elle doğrulaması gerekenler
- CSV'yi Excel'de (TR bölgesel ayarlarla) açıp Türkçe karakterleri ve sütunları kontrol etmek.
- 90 günlük, çok ilaçlı bir PDF raporunda sayfa geçişi, tekrar eden tablo başlığı, sayfa numarası ve altbilgi.
- Yedekle → uygulamayı sil/kur → geri yükle; ilaçların, geçmişin ve bildirimlerin geri gelmesi.
- Başka bir JSON dosyasıyla geri yüklemenin hata mesajı verip veriyi korumak.

## Önerilen commit mesajı
feat(export): add CSV and PDF export with JSON backup and restore
