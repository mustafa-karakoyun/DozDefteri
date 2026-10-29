# Faz 1 — Veri katmanı

## Yapılanlar
- Enum'lar (`ScheduleType`, `DoseStatus`, `MedicationForm`, `StockChangeReason`) ve `OPEN_DOSE_STATUSES`.
- Entity'ler (Bölüm 3.2 birebir), `Converters` (enum → `name`, `LocalDate` → ISO, `List<LocalTime>` → `"08:00,20:00"` sıralı/tekrarsız).
- DAO'lar: `ProfileDao`, `MedicationDao`, `ScheduleDao` (`ActiveSchedule` join'i), `DoseEventDao` (`DoseEventDetails` join'i: ilaç adı, birim, plan türü, profil adı), `StockLogDao`.
- `AppDatabase` (`dozdefteri.db`, sürüm 1, `exportSchema = true`).
- `SettingsRepository` (DataStore, Bölüm 3.4'teki tüm anahtarlar; izin verilmeyen değerler varsayılana düşer).
- `DoseRepository`: `ensureWindow`, `regenerateForSchedule`, `markMissed`, `take`, `takeAsNeeded`, `skip`, `snooze`, `undo`, `archiveMedication` (Bölüm 4.3) + düşük stok kuralı (4.4).
- `MedicationRepository` (tek plan kaydetme, elle stok değişimi `MANUAL_SET`/`MANUAL_ADD` log'u, arşiv/geri yükleme), `ProfileRepository` (aktif profil Flow'u, ilk açılış profili, silme).
- `DoseGenerator`, `AdherenceCalculator`, `StockEstimator` (kalan gün tahmini) — saf Kotlin.
- İlk açılış: `DozDefteriApp.onCreate` içinde veritabanı boşsa "Ben"/"Me" profili oluşturulur ve `active_profile_id` yazılır.
- Hilt modülleri: `DatabaseModule`, `DataStoreModule`, `CoroutinesModule` (`@ApplicationScope`), geçici `NotifierModule`.
- Testler: `DoseGeneratorTest` (12), `AdherenceCalculatorTest` (4), `DoseRepositoryTest` (10, Robolectric + in-memory Room).

## Oluşturulan / değişen dosyalar
- `app/src/main/java/app/dozdefteri/domain/{DoseGenerator,AdherenceCalculator,StockEstimator}.kt`, `domain/model/Enums.kt`
- `app/src/main/java/app/dozdefteri/data/db/{AppDatabase,Converters}.kt`, `data/db/entity/*`, `data/db/dao/*`
- `app/src/main/java/app/dozdefteri/data/prefs/SettingsRepository.kt`
- `app/src/main/java/app/dozdefteri/data/repository/{DoseRepository,MedicationRepository,ProfileRepository,LowStockNotifier}.kt`
- `app/src/main/java/app/dozdefteri/core/di/{DatabaseModule,DataStoreModule,CoroutinesModule,NotifierModule}.kt`
- `app/src/main/java/app/dozdefteri/DozDefteriApp.kt`
- `app/src/main/res/values{,-tr}/strings.xml`
- `app/src/test/java/app/dozdefteri/domain/{DoseGeneratorTest,AdherenceCalculatorTest}.kt`
- `app/src/test/java/app/dozdefteri/data/{DoseRepositoryTest,TestData}.kt`

## Komut sonuçları
- assembleDebug: ❌ çalıştırılamadı (dl.google.com engeli, bkz. Faz 0)
- testDebugUnitTest: ❌ çalıştırılamadı (aynı neden)
- lintDebug: ❌ çalıştırılamadı (aynı neden)
- **Yedek doğrulama:** Saf Kotlin dosyaları (domain, entity'ler, Converters) ayrı bir Kotlin/JVM projesinde (Kotlin 2.3.21, Room anotasyonları için stub) derlendi ve testler koşturuldu: `DoseGeneratorTest` 12/12 ✅, `AdherenceCalculatorTest` 4/4 ✅. `DoseRepositoryTest` Robolectric + Room gerektirdiği için koşturulamadı.

## Kabul kriterleri
- [~] Tüm birim testleri geçer — saf Kotlin testleri 16/16 geçti; `DoseRepositoryTest` (10 test) çalıştırılamadı.
- [ ] `app/schemas/.../1.json` oluşur — oluşmadı: şema dosyasını Room'un KSP işlemcisi ilk derlemede üretir. Elle yazılmadı çünkü `identityHash` Room'un iç algoritmasıyla üretiliyor; elle yazılmış hatalı bir şema migration testlerini bozardı. İlk `./gradlew assembleDebug` sonrası commit edilmeli.

## Verilen kararlar
- **Düşük stok bildirimi soyutlaması:** Repository'ler Android bildirim sınıflarına dokunmasın diye `LowStockNotifier` arayüzü eklendi; Faz 2'de `DoseNotifier` ile bağlanır.
- **`take`/`skip`/`snooze` yalnızca PENDING/SNOOZED olaylarda çalışır**, aksi hâlde `false` döner (çift dokunma, bildirim + uygulama yarışı güvenli).
- **`undo`:** TAKEN ve SKIPPED için, `actionAt` son 24 saatteyse. Doz PENDING'e döner (`actionAt = null`, `notifiedAt` korunur → aynı doz için ikinci bildirim çıkmaz). AS_NEEDED kaydı geri alınırsa olay silinir (planlı bir doz olmadığı için PENDING'e dönmesi anlamsız). Stok iadesi dozun tam miktarıdır; stok 0'da kırpıldıysa iade kırpılan miktardan fazla olabilir (olay başına düşülen miktar şemada tutulmuyor).
- **Stok iadesi/elle artırma sonrası** stok eşiğin üstüne çıkarsa `lowStockNotified = false` (4.4 kuralı iade için de uygulandı).
- **Gün aralığı sorguları** (`observeDetailsRange`) `originalScheduledAt` üzerinden yapılır; ertelenen doz gece yarısını geçse de ait olduğu günde kalır.
- **`markMissed`** önce etkilenen id'leri döner, sonra günceller; `DoseSync` bu id'lerin bildirimlerini iptal eder.
- **Profil silme** foreign-key `CASCADE` ile tüm ilaç/plan/doz/stok kayıtlarını siler; son profil silinemez.

## Çözülemeyen sorunlar
- Android derlemesi ve Robolectric testleri çalıştırılamıyor — ortam engeli (Faz 0). Geçici çözüm: saf Kotlin testleri ayrı JVM projesinde koşturuldu.

## Kullanıcının elle doğrulaması gerekenler
- `./gradlew testDebugUnitTest` ile `DoseRepositoryTest`'in geçtiğini görmek.
- İlk derlemeden sonra oluşan `app/schemas/app.dozdefteri.data.db.AppDatabase/1.json` dosyasını commit etmek.

## Önerilen commit mesajı
feat(data): add Room data layer, dose generation and repositories
