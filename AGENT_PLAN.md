# DozDefteri — Kodlama Ajanı İçin Uygulama Planı

> Bu dosya bir kodlama ajanının (Claude Code vb.) DozDefteri Android uygulamasını sıfırdan inşa etmesi için yazılmıştır.
> Tüm ürün ve teknik kararlar burada verilmiştir. Ajan bu planı uygular.
> **Ajan tamamen otonom çalışır: kullanıcıya hiçbir soru sormaz, hiçbir fazdan sonra durup onay beklemez.**
> Tüm fazları (0–9) baştan sona tamamlar, her fazın raporunu `reports/` klasörüne yazar ve en sonda `reports/OZET.md` oluşturur.
> Kullanıcı işi yalnızca en sonda, raporlar üzerinden inceleyecektir.
> Plan ile gerçeklik çelişirse (ör. bir API değişmişse) ajan Bölüm 0.4'teki karar kurallarına göre en güvenli seçeneği uygular, kararı raporlar ve devam eder.

---

## 0. Ajan için çalışma kuralları (ZORUNLU)

1. **Fazları sırayla ve kesintisiz uygula.** Bir faz bitmeden sonrakine geçme. Her fazın sonunda raporu `reports/faz-N.md` dosyasına yaz (bkz. 0.3) ve **durmadan bir sonraki faza geç**. Kullanıcıya soru sorma, onay bekleme, "devam edeyim mi?" deme. Faz 9 bitince `reports/OZET.md` dosyasını yaz (bkz. 0.6); iş ancak o zaman biter.
2. **Git commit YAPMA.** Commit'leri geliştirici kendisi yapar. Her faz raporunda önerilen commit mesajını yaz (Conventional Commits formatında, İngilizce). `git add`, `git commit`, `git push`, branch oluşturma da yapılmaz.
3. **Listede olmayan bağımlılık ekleme.** Bölüm 2.3'teki kütüphaneler dışında bir şey gerekiyorsa önce standart Android/Kotlin API'leriyle çöz. Mümkün değilse özelliği en sade hâliyle yap ve raporun "Verilen kararlar" bölümüne yaz.
4. **Ağ erişimi yok.** Uygulama kodunda hiçbir HTTP isteği, analitik, crash raporlama SDK'sı, reklam SDK'sı olmayacak.
5. **Hardcoded metin yok.** Kullanıcıya görünen her metin `strings.xml` içinde, hem İngilizce (varsayılan `values/`) hem Türkçe (`values-tr/`).
6. **Saat kullanımı:** Kodda `System.currentTimeMillis()`, `Instant.now()`, `LocalDate.now()` doğrudan çağrılmaz. Her yerde enjekte edilen `java.time.Clock` kullanılır (test edilebilirlik için).
7. **Her faz sonunda şu komutlar hatasız geçmeli:**
   ```bash
   ./gradlew assembleDebug testDebugUnitTest lintDebug
   ```
   Lint uyarıları kabul edilebilir; lint **hataları** kabul edilmez.
8. **Belirsizlik durumunda:** soru sorma, durma. Bölüm 0.4'teki karar kurallarına göre seç, uygula ve raporun "Verilen kararlar" bölümüne gerekçesiyle yaz.
9. **Tıbbi işlev ekleme.** İlaç etkileşimi, doz önerisi, teşhis, ilaç veritabanı gibi hiçbir özellik ekleme (Play sağlık politikası).
10. **Kapsam dışı özellikleri** (Bölüm 1.3) önermek dışında kodlama.

### 0.1 Kod stili

- Kotlin resmi stil kuralları, 4 boşluk girinti, maksimum satır 120 karakter.
- Paket adları küçük harf, sınıf adları PascalCase, fonksiyonlar camelCase.
- Composable fonksiyonlar PascalCase, her ekranın `XxxScreen` (stateful, ViewModel alır) ve `XxxContent` (stateless, preview'lanabilir) olmak üzere iki katmanı olur.
- `!!` operatörü kullanılmaz.
- Public fonksiyonlarda KDoc yalnızca davranış açık değilse yazılır; alarm ve zaman hesaplama sınıflarında KDoc zorunlu.

### 0.2 Mimari kurallar

- MVVM: `Screen` → `ViewModel` (StateFlow<UiState>) → `Repository` → `DAO`.
- ViewModel'ler Android framework sınıflarına (Context, AlarmManager) doğrudan dokunmaz; bunlar `alarm` ve `export` paketlerindeki sınıflarla soyutlanır.
- Tüm veritabanı işlemleri `suspend` veya `Flow` döner.
- Hilt ile bağımlılık enjeksiyonu.

### 0.3 Faz raporu şablonu (`reports/faz-N.md`)

Her faz sonunda bu dosyayı oluştur. Sohbete rapor yazmak yetmez; rapor dosyada olmalı. Sohbette yalnızca tek satır yaz ("Faz N tamamlandı, rapor: reports/faz-N.md") ve hemen sonraki faza geç.

```markdown
# Faz N — <faz adı>

## Yapılanlar
- (madde madde)

## Oluşturulan / değişen dosyalar
- (liste)

## Komut sonuçları
- assembleDebug: ✅/❌
- testDebugUnitTest: ✅/❌ (geçen X / toplam Y)
- lintDebug: ✅/❌ (hata sayısı, uyarı sayısı)

## Kabul kriterleri
- [x] / [ ] her kriter, karşılanmadıysa nedeni

## Verilen kararlar
- Karar — gerekçe (planın belirsiz kaldığı veya gerçeklikle çeliştiği her nokta)

## Çözülemeyen sorunlar
- Sorun — denenenler — geçici çözüm (yoksa "Yok")

## Kullanıcının elle doğrulaması gerekenler
- (cihazda test, Play Console işi vb.)

## Önerilen commit mesajı
feat(scope): ...
```

### 0.4 Karar kuralları (soru sormak yerine)

Plan bir konuda belirsizse veya uygulanamıyorsa şu sırayla karar ver:

1. Planın açık metni ve Bölüm 1'deki ürün ilkeleri (gizlilik, çevrimdışı, abonelik yok, ücretsiz hatırlatma) her şeyden önce gelir.
2. Kullanıcının verisini kaybetme ihtimali olan seçenek asla seçilmez.
3. Daha az kod, daha az bağımlılık, daha az izin gerektiren seçenek tercih edilir.
4. Android'in resmi dokümantasyonundaki önerilen yöntem tercih edilir.
5. Bir özellik tamamlanamıyorsa yarım ve hatalı bırakılmaz: ya basitleştirilmiş çalışan bir sürümü yapılır ya da özellik kapalı bırakılıp (UI'da gösterilmez) raporlanır.

### 0.5 Derleme veya test hatası

- Hata önce düzeltilir; aynı hata için en az 3 farklı çözüm denenir.
- Derleme (`assembleDebug`) hatalıyken bir sonraki faza **geçilmez**; derleme mutlaka düzeltilir.
- Tek bir test 3 denemeden sonra da düzeltilemiyorsa `@Ignore("BLOCKED: <kısa neden>")` ile işaretlenir, "Çözülemeyen sorunlar"a yazılır ve devam edilir. Bu yöntem testin kendisi geçmesin diye değiştirilerek **değil**, yalnızca bu şekilde uygulanır.
- Emülatör veya fiziksel cihaz gerektiren adımlar (enstrümantasyon testleri, bildirim gözlemi) çalıştırılamıyorsa atlanır ve "Kullanıcının elle doğrulaması gerekenler"e yazılır.

### 0.6 Özet raporu (`reports/OZET.md`)

Faz 9'dan sonra yazılır. İçerik:

1. Faz tablosu: faz, durum (tamam / kısmi), kabul kriterleri karşılanan/toplam, rapor dosyası bağlantısı.
2. Tüm fazlardaki "Verilen kararlar"ın birleşik listesi.
3. Tüm "Çözülemeyen sorunlar" ve `@Ignore` edilmiş testler.
4. Kullanıcının elle doğrulaması gerekenlerin tek, sıralı kontrol listesi (Bölüm 13 dahil).
5. Son komut sonuçları: `assembleDebug`, `testDebugUnitTest`, `lintDebug`, `assembleRelease`.
6. Önerilen commit planı: değişiklikleri mantıklı commit'lere bölmek için faz faz dosya grupları ve commit mesajları (ajan commit yapmaz, kullanıcı yapar).

---

## 1. Ürün tanımı

**DozDefteri**: reklamsız, hesapsız, tamamen çevrimdışı çalışan ilaç ve takviye hatırlatıcısı. Konumlandırma: "Abonelik yok · Hesap yok · Sınırsız ilaç."

### 1.1 Sabit değerler

| Ad | Değer |
|---|---|
| Uygulama adı (EN) | DozDefteri |
| Uygulama adı (TR) | DozDefteri |
| applicationId | `app.dozdefteri` (bu değeri kullan; Play'e ilk yüklemeden sonra değiştirilemeyeceğini OZET.md'de kullanıcıya hatırlat) |
| Kök paket | `app.dozdefteri` |
| minSdk | 26 |
| compileSdk / targetSdk | 36 (Faz 0'da Play'in güncel zorunlu hedef API seviyesiyle uyumsuzsa raporla) |
| Varsayılan erteleme süresi | 10 dakika |
| Varsayılan "kaçırıldı" eşiği | 180 dakika |
| Doz üretim penceresi | şimdi + 48 saat |
| Pro ürün ID | `pro_lifetime` |
| Ücretsiz profil limiti | 1 |

### 1.2 MVP özellikleri

1. Sınırsız ilaç ekleme/düzenleme/arşivleme.
2. Plan türleri: günlük (her gün belirli saatler), haftanın belirli günleri, X günde bir, gerektiğinde (hatırlatmasız).
3. Kesin zamanlı bildirim; bildirimden **Aldım / Ertele / Atla**.
4. Stok takibi ve düşük stok bildirimi.
5. Bugün ekranı, ilaç listesi, geçmiş takvimi ve uyum yüzdesi.
6. CSV dışa aktarım (ücretsiz), PDF rapor (ücretsiz: basit; Pro: detaylı).
7. JSON yedekleme ve geri yükleme (ücretsiz).
8. Profiller (ücretsiz 1, Pro sınırsız).
9. Ana ekran widget'ı (Pro).
10. Tema renk seçenekleri (Pro).
11. TR/EN dil desteği; sistem diline göre otomatik.
12. Tek seferlik Pro satın alma ve geri yükleme.

### 1.3 Kapsam dışı (kodlanmayacak)

İlaç etkileşimi, ilaç veritabanı/arama, barkod okuma, bulut senkronizasyonu, hesap/giriş, Wear OS, iOS, aile bildirimi, uygulama kilidi, analitik, reklam, tam ekran bildirim (full-screen intent).

---

## 2. Teknik yapı

### 2.1 Proje yapısı

Tek modül (`:app`), Gradle Kotlin DSL, version catalog (`gradle/libs.versions.toml`).

```
app/src/main/java/app/dozdefteri/
├── DozDefteriApp.kt                 # @HiltAndroidApp, bildirim kanallarını oluşturur, WorkManager'ı planlar
├── MainActivity.kt                  # Tek Activity, edge-to-edge, NavHost
├── core/
│   ├── time/ClockModule.kt          # Clock.systemDefaultZone() sağlar
│   └── di/                          # Hilt modülleri (DatabaseModule, DataStoreModule)
├── data/
│   ├── db/
│   │   ├── AppDatabase.kt
│   │   ├── Converters.kt
│   │   ├── entity/                  # ProfileEntity, MedicationEntity, ScheduleEntity, DoseEventEntity, StockLogEntity
│   │   └── dao/                     # ProfileDao, MedicationDao, ScheduleDao, DoseEventDao, StockLogDao
│   ├── prefs/SettingsRepository.kt  # DataStore
│   └── repository/                  # ProfileRepository, MedicationRepository, DoseRepository
├── domain/
│   ├── DoseGenerator.kt             # Saf Kotlin, Android bağımlılığı YOK
│   ├── AdherenceCalculator.kt       # Saf Kotlin
│   └── model/                       # Enum'lar ve UI'a giden modeller
├── alarm/
│   ├── AlarmScheduler.kt
│   ├── DoseAlarmReceiver.kt
│   ├── DoseActionReceiver.kt
│   ├── RescheduleReceiver.kt        # boot, saat, saat dilimi, paket güncelleme, izin değişimi
│   ├── DoseNotifier.kt              # Bildirim oluşturma
│   ├── MaintenanceWorker.kt         # WorkManager periyodik iş
│   └── DoseSync.kt                  # Tek giriş noktası: pencereyi doldur, kaçırılanları işaretle, alarmı kur
├── billing/
│   ├── BillingManager.kt
│   └── ProStatus.kt
├── export/
│   ├── CsvExporter.kt
│   ├── PdfExporter.kt
│   └── BackupManager.kt
├── widget/
│   └── NextDoseWidget.kt            # Glance
└── ui/
    ├── navigation/AppNavHost.kt, Routes.kt
    ├── theme/                       # Color.kt, Theme.kt, Type.kt
    ├── components/                  # Ortak composable'lar
    ├── onboarding/
    ├── today/
    ├── medications/                 # liste + ekle/düzenle
    ├── history/
    ├── export/
    ├── profiles/
    └── settings/
```

### 2.2 Gradle ayarları

- Java/Kotlin hedefi: 17.
- `buildFeatures { compose = true; buildConfig = true }`.
- Room şeması dışa aktarımı: `ksp { arg("room.schemaLocation", "$projectDir/schemas") }`, `exportSchema = true`.
- Release: `isMinifyEnabled = true`, `isShrinkResources = true`, `proguard-android-optimize.txt`.
- Debug'a özel `BuildConfig.DEBUG_FORCE_PRO` alanı: debug için `false` (elle `true` yapılabilir), release için her zaman `false`.
- İmzalama yapılandırması **eklenmez** (insan yapacak).

### 2.3 İzin verilen bağımlılıklar

Faz 0'da her biri için **güncel kararlı sürümü** belirle, `libs.versions.toml`'a sabitle ve raporda sürümleri listele. Alfa/beta sürüm kullanma.

| Amaç | Kütüphane |
|---|---|
| Compose | `androidx.compose:compose-bom`, `ui`, `ui-tooling-preview`, `material3`, `material-icons-extended` |
| Activity / Lifecycle | `activity-compose`, `lifecycle-runtime-compose`, `lifecycle-viewmodel-compose` |
| Navigasyon | `navigation-compose` (type-safe route'lar için `kotlinx-serialization`) |
| Veritabanı | `room-runtime`, `room-ktx`, `room-compiler` (KSP) |
| Ayarlar | `datastore-preferences` |
| DI | `hilt-android`, `hilt-compiler` (KSP), `hilt-navigation-compose`, `hilt-work`, `androidx.hilt:hilt-compiler` |
| Arka plan | `work-runtime-ktx` |
| Satın alma | `com.android.billingclient:billing-ktx` |
| JSON | `kotlinx-serialization-json` |
| Widget | `androidx.glance:glance-appwidget`, `glance-material3` |
| Test | `junit` 4, `kotlinx-coroutines-test`, `app.cash.turbine:turbine`, `com.google.truth:truth`, `room-testing`, `androidx.test.ext:junit`, `androidx.test:runner`, `work-testing`, `compose-ui-test-junit4`, `robolectric` |

### 2.4 Manifest

İzinler (yalnızca bunlar):

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```

- `USE_EXACT_ALARM`, `USE_FULL_SCREEN_INTENT`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, `INTERNET`, depolama izinleri **eklenmez**.
- Faz 7'de birleşik manifest'i (`app/build/intermediates/merged_manifest/release/...`) kontrol et. Bir kütüphane `INTERNET` veya `ACCESS_NETWORK_STATE` ekliyorsa **kaldırma**, yalnızca hangi kütüphanenin eklediğini raporla (kullanıcı sonda karar verecek).
- Receiver'lar `android:exported="false"`, yalnızca `RescheduleReceiver` sistem yayınları için `exported="true"` (sistem yayınları gerektirdiği için).

`RescheduleReceiver` intent filtreleri:

```
android.intent.action.BOOT_COMPLETED
android.intent.action.LOCKED_BOOT_COMPLETED
android.intent.action.TIME_SET
android.intent.action.TIMEZONE_CHANGED
android.intent.action.MY_PACKAGE_REPLACED
android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
```

`LOCKED_BOOT_COMPLETED` için receiver `android:directBootAware="false"` kalır; kilit açılmadan veritabanına erişilemeyeceği için bu yayında yalnızca `BOOT_COMPLETED` beklenir (iki filtre de bulunur, işlem aynıdır, hata yakalanır).

---

## 3. Veri modeli (Room)

Veritabanı adı: `dozdefteri.db`, sürüm 1.

### 3.1 Enum'lar (`domain/model`)

```kotlin
enum class ScheduleType { DAILY, WEEKDAYS, EVERY_N_DAYS, AS_NEEDED }
enum class DoseStatus { PENDING, TAKEN, SNOOZED, SKIPPED, MISSED }
enum class MedicationForm { TABLET, CAPSULE, LIQUID, DROPS, INJECTION, INHALER, CREAM, OTHER }
enum class StockChangeReason { DOSE_TAKEN, MANUAL_ADD, MANUAL_SET, DOSE_UNDONE }
```

Room'da enum'lar `name` olarak String saklanır (TypeConverter).

### 3.2 Entity'ler

```kotlin
@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorIndex: Int,          // 0..7, temadaki profil renk paletinden
    val createdAt: Long           // epoch millis
)

@Entity(
    tableName = "medications",
    foreignKeys = [ForeignKey(ProfileEntity::class, ["id"], ["profileId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("profileId")]
)
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val name: String,                 // 1..60 karakter, zorunlu
    val doseAmount: Double,           // > 0, ör. 1.0, 0.5
    val doseUnit: String,             // serbest metin, 1..20 karakter, ör. "tablet", "mg", "ml"
    val form: MedicationForm,
    val note: String?,                // en fazla 200 karakter
    val stockQuantity: Double?,       // null = stok takibi kapalı
    val lowStockThreshold: Double?,   // stockQuantity null ise null
    val lowStockNotified: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "schedules",
    foreignKeys = [ForeignKey(MedicationEntity::class, ["id"], ["medicationId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("medicationId")]
)
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationId: Long,
    val type: ScheduleType,
    val times: List<LocalTime>,       // TypeConverter: "08:00,20:00" (sıralı, tekrarsız). AS_NEEDED için boş liste
    val daysOfWeekMask: Int,          // WEEKDAYS için bit maskesi: Pazartesi=1 shl 0 ... Pazar=1 shl 6. Diğer türlerde 0
    val intervalDays: Int,            // EVERY_N_DAYS için 2..30. Diğer türlerde 1
    val startDate: LocalDate,         // TypeConverter: ISO "2026-11-02"
    val endDate: LocalDate?           // dahil; null = süresiz
)

@Entity(
    tableName = "dose_events",
    foreignKeys = [ForeignKey(ScheduleEntity::class, ["id"], ["scheduleId"], onDelete = ForeignKey.CASCADE)],
    indices = [
        Index(value = ["scheduleId", "originalScheduledAt"], unique = true),
        Index("scheduledAt"),
        Index("status")
    ]
)
data class DoseEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scheduleId: Long,
    val medicationId: Long,           // sorgu kolaylığı için denormalize
    val profileId: Long,              // sorgu kolaylığı için denormalize
    val originalScheduledAt: Long,    // planın ürettiği zaman, asla değişmez (tekilleştirme anahtarı)
    val scheduledAt: Long,            // erteleme ile değişebilir
    val status: DoseStatus,
    val notifiedAt: Long?,            // bildirim gösterildiği an; tekrar bildirim engeli
    val actionAt: Long?,              // TAKEN/SKIPPED/MISSED olduğu an
    val quantity: Double              // o dozdaki miktar (medication.doseAmount kopyası)
)

@Entity(
    tableName = "stock_logs",
    foreignKeys = [ForeignKey(MedicationEntity::class, ["id"], ["medicationId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("medicationId")]
)
data class StockLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationId: Long,
    val delta: Double,                // negatif = düşüş
    val reason: StockChangeReason,
    val createdAt: Long
)
```

**Gerektiğinde (AS_NEEDED) ilaçlar:** hatırlatma üretilmez. Bugün ekranındaki "Şimdi aldım" düğmesi `scheduleId`'si o AS_NEEDED planı olan, `originalScheduledAt = scheduledAt = actionAt = şimdi`, `status = TAKEN` bir DoseEvent ekler.

### 3.3 İlk açılış

Veritabanı boşsa ilk açılışta adı `strings.profile_default_name` ("Ben" / "Me") olan, `colorIndex = 0` bir profil oluşturulur. Aktif profil ID'si DataStore'da `active_profile_id` anahtarında tutulur.

### 3.4 DataStore anahtarları (`SettingsRepository`)

| Anahtar | Tip | Varsayılan |
|---|---|---|
| `onboarding_done` | Boolean | false |
| `active_profile_id` | Long | ilk profilin id'si |
| `snooze_minutes` | Int | 10 (izinli: 5, 10, 15, 30, 60) |
| `missed_after_minutes` | Int | 180 (izinli: 60, 120, 180, 360) |
| `theme_mode` | String | "SYSTEM" (SYSTEM / LIGHT / DARK) |
| `accent_index` | Int | 0 (0 ücretsiz; 1..3 Pro) |
| `pro_cached` | Boolean | false |
| `last_backup_reminder_at` | Long | 0 |

---

## 4. Alan mantığı (saf Kotlin, `domain/`)

### 4.1 `DoseGenerator`

```kotlin
class DoseGenerator {
    /**
     * [from, to) aralığında, planın ürettiği doz anlarını artan sırada döner.
     * Yerel tarih + LocalTime, ZonedDateTime.of(date, time, zone) ile çözülür.
     */
    fun generate(schedule: ScheduleEntity, from: Instant, to: Instant, zone: ZoneId): List<Instant>
}
```

Kurallar (her biri birim testli olacak):

1. `AS_NEEDED` → her zaman boş liste.
2. Gün aralığı: `from` ve `to`'nun yerel tarihleri arasındaki her `LocalDate` için, `startDate <= tarih <= endDate (varsa)` koşulu.
3. `DAILY`: her uygun gün.
4. `WEEKDAYS`: `tarih.dayOfWeek` (Pazartesi=0 … Pazar=6) biti maskede açıksa.
5. `EVERY_N_DAYS`: `ChronoUnit.DAYS.between(startDate, tarih) % intervalDays == 0`.
6. Her uygun gün için her `times` öğesi: `ZonedDateTime.of(tarih, saat, zone).toInstant()`. Sonuç `from <= an < to` ise listeye eklenir.
7. **Yaz saati:** `ZonedDateTime.of` varsayılan davranışı kullanılır (ileri geçişte boşluktaki saat boşluk kadar ileri kayar; geri geçişte erken ofset seçilir). Böylece her doz tam bir kez üretilir. Bunu `Europe/Berlin` ve `America/New_York` ile test et.
8. Sonuç tekrarsız ve sıralı.

### 4.2 `AdherenceCalculator`

- Girdi: belirli tarih aralığındaki DoseEvent listesi.
- `uyum % = TAKEN / (TAKEN + SKIPPED + MISSED) * 100`, tam sayıya yuvarlanır (`roundToInt`).
- AS_NEEDED planlarına ait olaylar hesaba katılmaz.
- Payda 0 ise `null` döner (UI "—" gösterir).

### 4.3 `DoseRepository` işlemleri (tek doğruluk kaynağı)

| Fonksiyon | Davranış |
|---|---|
| `ensureWindow(now)` | Aktif (arşivlenmemiş) ilaçların tüm planları için `[now - 1 dk, now + 48 saat)` doz anlarını üretir; `INSERT OR IGNORE` ile ekler (unique index sayesinde tekrar oluşmaz). |
| `regenerateForSchedule(scheduleId, now)` | Plan değişince: o plana ait `scheduledAt > now` ve `status IN (PENDING, SNOOZED)` olayları siler, sonra pencereyi yeniden üretir. Geçmiş olaylara dokunmaz. |
| `markMissed(now, missedAfterMinutes)` | `status IN (PENDING, SNOOZED)` ve `scheduledAt < now - missedAfter` olanları `MISSED`, `actionAt = now` yapar. |
| `take(eventId, now)` | `TAKEN`, `actionAt = now`. Stok takibi açıksa stoktan `quantity` düşer (0'ın altına inmez), StockLog `DOSE_TAKEN` ekler. Düşük stok kontrolü yapar (4.4). Tek transaction. |
| `skip(eventId, now)` | `SKIPPED`, `actionAt = now`. Stok değişmez. |
| `snooze(eventId, now, minutes)` | `SNOOZED`, `scheduledAt = now + minutes`, `notifiedAt = null`. |
| `undo(eventId, now)` | Yalnızca Bugün ekranından, `actionAt` son 24 saat içindeyse: TAKEN geri alınırsa stok iade edilir (`DOSE_UNDONE` log), durum `scheduledAt <= now` ise PENDING olur. |
| `archiveMedication(id, now)` | `isArchived = true`; geleceğe ait PENDING/SNOOZED olayları siler. |

### 4.4 Düşük stok kuralı

- `take` sonrası `stockQuantity <= lowStockThreshold` ve `lowStockNotified == false` ise: düşük stok bildirimi göster, `lowStockNotified = true`.
- Stok elle artırılıp eşiğin üstüne çıkarsa `lowStockNotified = false`.
- Kalan gün tahmini (UI'da): `stock / (günlük ortalama doz miktarı)`; günlük ortalama, DAILY/WEEKDAYS/EVERY_N_DAYS planlarından hesaplanır, AS_NEEDED dahil edilmez. Tahmin yoksa gösterilmez.

---

## 5. Alarm sistemi (`alarm/`)

### 5.1 `DoseSync.run(reason: String)` — tek giriş noktası

Sıra kesindir:

1. `doseRepository.markMissed(now, missedAfter)`
2. `doseRepository.ensureWindow(now)`
3. Kaçırıldı olarak işaretlenen olayların bildirimlerini iptal et.
4. `alarmScheduler.scheduleNext()`
5. Widget'ı güncelle (Faz 8'den sonra).

Çağrıldığı yerler: uygulama açılışı (`MainActivity.onResume`), her ilaç/plan kaydı/arşivi, `RescheduleReceiver`, `DoseAlarmReceiver`, `DoseActionReceiver`, `MaintenanceWorker`, profil silme, yedek geri yükleme.

Receiver içinde `goAsync()` + `CoroutineScope(Dispatchers.IO)` kullanılır; iş 10 saniye içinde bitmeli.

### 5.2 `AlarmScheduler.scheduleNext()`

1. `status IN (PENDING, SNOOZED)` ve `notifiedAt IS NULL` olan en küçük `scheduledAt` değerini al.
2. Yoksa mevcut alarmı iptal et ve çık.
3. Tetik zamanı = `max(scheduledAt, now + 1 saniye)`.
4. Tek bir `PendingIntent`: `requestCode = 1001`, hedef `DoseAlarmReceiver`, `FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE`. Her kurulum öncekinin yerine geçer.
5. `alarmManager.canScheduleExactAlarms()` (API 31+; altında hep true):
   - true → `setExactAndAllowWhileIdle(RTC_WAKEUP, trigger, pi)`
   - false → `setAndAllowWhileIdle(RTC_WAKEUP, trigger, pi)` ve `SettingsRepository`'de UI uyarısı için durum güncellenir (UI zaten `canScheduleExactAlarms()`'ı her `onResume`'da okur; ayrıca saklamaya gerek yok).

### 5.3 `DoseAlarmReceiver`

1. `status IN (PENDING, SNOOZED)`, `notifiedAt IS NULL`, `scheduledAt <= now + 30 saniye` olan tüm olayları al.
2. Her biri için `DoseNotifier.showDose(event)`; `notifiedAt = now`.
3. `DoseSync.run("alarm")`.

### 5.4 `DoseNotifier`

Bildirim kanalları (`DozDefteriApp.onCreate` içinde oluşturulur):

| Kanal ID | Ad (strings) | Önem |
|---|---|---|
| `dose_reminders` | Doz hatırlatmaları / Dose reminders | `IMPORTANCE_HIGH` |
| `stock_alerts` | Stok uyarıları / Stock alerts | `IMPORTANCE_DEFAULT` |
| `backup_reminders` | Yedek hatırlatmaları / Backup reminders | `IMPORTANCE_LOW` |

Doz bildirimi:

- `notificationId = event.id.toInt()`
- Başlık: ilaç adı. Metin: `"{doseAmount} {doseUnit} · {saat}"` (saat sistem 12/24 ayarına göre). Birden fazla profil varsa metnin başına `"{profil adı}: "`.
- `category = CATEGORY_REMINDER`, `priority = PRIORITY_HIGH`, `setAutoCancel(false)`, `setOngoing(false)`.
- Grup anahtarı `dose_group`; aynı anda 2+ bildirim varsa özet bildirim (`id = 0`).
- İçerik intent'i: `MainActivity` → Bugün ekranı.
- Aksiyonlar: **Aldım** (`ACTION_TAKE`), **Ertele** (`ACTION_SNOOZE`), **Atla** (`ACTION_SKIP`). Her biri `DoseActionReceiver`'a, `requestCode = event.id * 10 + aksiyonIndeksi`, extra `event_id`.
- Android 13+ bildirim izni yoksa bildirim gösterilmez, hata atılmaz.

### 5.5 `DoseActionReceiver`

Aksiyona göre `take` / `snooze(settings.snoozeMinutes)` / `skip`, ardından bildirimi iptal et ve `DoseSync.run("action")`. Olay zaten PENDING/SNOOZED değilse yalnızca bildirimi iptal et.

### 5.6 `MaintenanceWorker`

- `PeriodicWorkRequest` 6 saatte bir, `ExistingPeriodicWorkPolicy.KEEP`, benzersiz ad `dose_maintenance`. Kısıt yok.
- İşi: `DoseSync.run("worker")`.
- Ayrıca: son yedek hatırlatmasından 30 gün geçtiyse ve en az 1 ilaç varsa `backup_reminders` kanalına bir bildirim, `last_backup_reminder_at = now`.

---

## 6. Ekranlar ve davranışlar (`ui/`)

Material 3, dinamik renk **kapalı** (marka tutarlılığı), açık/koyu tema. Minimum dokunma alanı 48dp. Tüm ikonlu durumlar ikon + metin ile gösterilir (yalnız renk değil). Her ikon `contentDescription` taşır.

### 6.1 Navigasyon

Alt navigasyon çubuğu 4 sekme: **Bugün**, **İlaçlar**, **Geçmiş**, **Ayarlar**. Diğer ekranlar push edilir. Başlangıç: `onboarding_done == false` ise Onboarding, değilse Bugün.

Route'lar (type-safe, `@Serializable`): `Onboarding`, `Today`, `Medications`, `MedicationEdit(medicationId: Long?)`, `History`, `Settings`, `Export`, `Profiles`, `Pro`, `BatteryGuide`.

### 6.2 Onboarding (3 sayfa, `HorizontalPager`)

1. **Değer:** başlık "İlaçlarını unutma" / "Never miss a dose"; 3 madde: abonelik yok, hesap yok, verilerin telefonunda. Buton: "Devam".
2. **Bildirim izni:** açıklama + "İzin ver" (Android 13+ `POST_NOTIFICATIONS` istenir; 13 altında bu sayfa otomatik geçilir). Reddedilirse "Ayarları aç" butonu ve "Şimdilik geç".
3. **Tam zamanlı hatırlatma:** açıklama + "Ayarları aç" (`Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM`, `package:` URI ile; API 31+). İzin zaten varsa ✅ gösterilir. Altında "Pil ayarları rehberi" linki (`BatteryGuide`). Buton: "Başla" → `onboarding_done = true` → Bugün.

### 6.3 Bugün

- Üstte aktif profil adı (birden fazla profil varsa tıklanınca profil seçme menüsü).
- **Uyarı bantları** (varsa, sırayla): kesin alarm izni yok → "Hatırlatmalar gecikebilir" + "Düzelt"; bildirim izni yok → "Bildirimler kapalı" + "Aç".
- Bugünkü dozlar saate göre gruplu liste. Her satır: ilaç adı, doz, saat, durum ikonu. PENDING/SNOOZED satırda: "Aldım", "Atla" butonları; uzun basınca "Ertele".
- TAKEN/SKIPPED satırda 24 saat içinde "Geri al".
- Geçmiş saatte bekleyen doz "Gecikti" etiketi gösterir.
- Bölüm: "Gerektiğinde" ilaçları, her birinde "Şimdi aldım".
- Düşük stok kartı: eşiğin altındaki ilaçlar ve tahmini kalan gün.
- Boş durum: illüstrasyon yerine ikon + "Henüz ilaç yok" + "İlaç ekle" butonu.
- FAB: "İlaç ekle".
- Doz yoksa ama ilaç varsa: "Bugün için doz yok".

### 6.4 İlaç ekle / düzenle

Alanlar ve doğrulama:

| Alan | Bileşen | Doğrulama |
|---|---|---|
| Ad | TextField | 1–60 karakter, boşlukları kırp |
| Doz miktarı | Sayısal TextField | > 0, en fazla 2 ondalık |
| Birim | TextField + öneri çipleri (tablet, kapsül, mg, ml, damla, puf) | 1–20 karakter |
| Form | Dropdown (MedicationForm) | zorunlu |
| Plan türü | Segment/radio: Her gün, Belirli günler, X günde bir, Gerektiğinde | zorunlu |
| Saatler | Saat çipleri + "Saat ekle" (TimePicker) | DAILY/WEEKDAYS/EVERY_N_DAYS için en az 1, en fazla 12, tekrarsız |
| Günler | 7 gün çipi | WEEKDAYS için en az 1 |
| Aralık | Sayı seçici 2–30 | EVERY_N_DAYS için |
| Başlangıç | DatePicker | varsayılan bugün |
| Bitiş | Opsiyonel DatePicker | başlangıçtan önce olamaz |
| Stok takibi | Switch; açıkken mevcut miktar ve uyarı eşiği | miktar >= 0, eşik >= 0 |
| Not | TextField | en fazla 200 karakter |

- Bir ilaç MVP'de **tek plan** taşır (veri modeli birden fazlasını destekler; UI tek plan yönetir).
- Kaydet → repository → `regenerateForSchedule` → `DoseSync.run("edit")` → geri dön, Snackbar "Kaydedildi".
- Düzenleme modunda menüde "Arşivle" (onay diyaloğu). Kalıcı silme yok.
- Hatalı alanlar altında hata metni; Kaydet butonu form geçersizse pasif.

### 6.5 İlaçlar

İki sekme: Aktif / Arşiv. Satır: ad, doz, plan özeti ("Her gün 08:00, 20:00"), stok rozeti. Arşivde "Geri yükle" (isArchived=false + DoseSync).

### 6.6 Geçmiş

- Ay görünümü takvim (kendi yazdığın basit grid; ek kütüphane yok). Her gün hücresinde nokta: tümü alındı (yeşil + ✓), kısmi (turuncu), kaçırılan var (kırmızı), doz yok (boş).
- Üstte ay seçici ve seçili ayın uyum yüzdesi.
- Güne dokununca alt sayfada o günün dozları ve durumları.

### 6.7 Dışa aktar (`Export`)

- Seçimler: profil (Pro'da "Tümü" seçeneği), tarih aralığı (Son 7 gün / Son 30 gün / Son 90 gün / Özel).
- Butonlar: "CSV olarak kaydet", "PDF rapor oluştur". Dosya `ActivityResultContracts.CreateDocument` ile kullanıcının seçtiği yere yazılır, sonra paylaş seçeneği sunulur.
- "Yedekle" ve "Yedekten geri yükle" de bu ekranda.

### 6.8 Profiller

Liste, ekle, yeniden adlandır, renk seç, sil (son profil silinemez; silme onayında o profilin tüm verisinin silineceği yazılır). Ücretsizde ikinci profil eklenmeye çalışılırsa `Pro` ekranına yönlendir.

### 6.9 Ayarlar

Bölümler: Görünüm (tema modu, vurgu rengi—Pro), Hatırlatmalar (erteleme süresi, kaçırıldı eşiği, kesin alarm izni durumu + düzelt, bildirim ayarları, pil rehberi), Veri (Dışa aktar ve yedekle, Profiller), Pro (durum, satın al, satın alımı geri yükle), Hakkında (sürüm, gizlilik politikası—URL insan tarafından verilecek, `strings.privacy_policy_url` boş bırakılır ve rapor edilir; sorumluluk reddi metni).

### 6.10 Pil rehberi (`BatteryGuide`)

- Genel açıklama + "Pil optimizasyonu ayarlarını aç" (`Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`).
- `Build.MANUFACTURER`'a göre (küçük harfe çevrilmiş: xiaomi/redmi/poco, samsung, huawei/honor, oppo/realme/oneplus, vivo) ilgili kısa adımlar metni; eşleşme yoksa genel metin. Metinler strings'te.

### 6.11 Pro ekranı

Özellik listesi (Bölüm 7.1), fiyat (Billing'den gelen `formattedPrice`; yüklenemezse buton pasif ve "Fiyat yüklenemedi"), "Satın al", "Satın alımı geri yükle". Altında: "Hatırlatmalar ve ilaç sayısı her zaman ücretsizdir."

### 6.12 Sorumluluk reddi (Ayarlar > Hakkında ve Onboarding 1. sayfa altı, küçük yazı)

- TR: "DozDefteri tıbbi tavsiye vermez ve doktorunuzun talimatlarının yerini tutmaz. İlaç kullanımınızla ilgili kararlar için doktorunuza veya eczacınıza danışın."
- EN: "DozDefteri does not provide medical advice and does not replace your doctor's instructions. Consult your doctor or pharmacist about your medication."

---

## 7. Pro ve satın alma (`billing/`)

### 7.1 Pro'ya ait özellikler (yalnızca bunlar)

1. 2. ve sonraki profiller.
2. Ana ekran widget'ı (ücretsizde widget eklenebilir ama "Pro gerekli" mesajı gösterir ve Pro ekranına açılır).
3. Detaylı PDF şablonu (uyum yüzdesi tablosu ve ilaç bazlı özet).
4. Vurgu rengi seçenekleri 1–3.
5. Dışa aktarımda "Tüm profiller" seçeneği.

Hatırlatmalar, ilaç sayısı, geçmiş, CSV, yedekleme hiçbir koşulda kısıtlanmaz.

### 7.2 `BillingManager`

- `@Singleton`, `BillingClient` `enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())` ile.
- `isPro: StateFlow<Boolean>` — başlangıç değeri DataStore `pro_cached`; `BuildConfig.DEBUG_FORCE_PRO` true ise her zaman true.
- Uygulama açılışında bağlan, `queryPurchasesAsync(INAPP)`; `pro_lifetime` için `PURCHASED` durumunda satın alma varsa `isPro = true`, `pro_cached = true`; yoksa ve sorgu başarılıysa `false`. Sorgu başarısızsa önbellek korunur.
- `queryProductDetails` ile fiyat.
- `launchPurchase(activity)`.
- `onPurchasesUpdated`: `PURCHASED` ise ve `isAcknowledged == false` ise `acknowledgePurchase`; sonra Pro'yu aç. `PENDING` ise "Ödeme bekleniyor" mesajı.
- Bağlantı koparsa üstel geri çekilmeyle (1, 2, 4, 8 sn; en fazla 4 deneme) yeniden bağlan.
- Sunucu doğrulaması yapılmaz.

---

## 8. Dışa aktarım ve yedek (`export/`)

### 8.1 CSV

- Kodlama UTF-8 **BOM'lu** (Excel'de Türkçe karakterler için), ayırıcı virgül, RFC 4180 tırnaklama.
- Dosya adı: `dozdefteri_{profil}_{baslangic}_{bitis}.csv` (tarih `yyyy-MM-dd`, profil adı ASCII'ye sadeleştirilmiş).
- Sütunlar (başlıklar uygulama diline göre):

| date | planned_time | medication | dose | unit | status | action_time | profile |

- `status` metni yerelleştirilmiş (Alındı/Taken, Atlandı/Skipped, Kaçırıldı/Missed, Bekliyor/Pending, Ertelendi/Snoozed).
- Sıralama: planlanan zamana göre artan.

### 8.2 PDF (`android.graphics.pdf.PdfDocument`)

- A4: 595 × 842 pt, kenar boşluğu 40 pt, yazı tipi sistem sans-serif, gövde 10 pt, başlık 16 pt.
- **Basit şablon (ücretsiz):** başlık "İlaç kullanım raporu", profil adı, tarih aralığı, oluşturulma tarihi; ilaç listesi (ad, doz, plan özeti); doz tablosu (tarih, saat, ilaç, durum). Sayfa taşarsa yeni sayfa, tablo başlığı tekrarlanır. Altta sayfa numarası "1/3".
- **Detaylı şablon (Pro):** ek olarak ilaç bazlı uyum yüzdesi tablosu ve genel uyum yüzdesi.
- Altbilgi her sayfada: sorumluluk reddi kısa metni.

### 8.3 JSON yedek

```json
{
  "format": "dozdefteri-backup",
  "schemaVersion": 1,
  "appVersionCode": 1,
  "exportedAt": "2026-11-20T10:15:00Z",
  "profiles": [],
  "medications": [],
  "schedules": [],
  "doseEvents": [],
  "stockLogs": []
}
```

- `kotlinx.serialization` ile ayrı `Backup*` DTO sınıfları (entity'ler doğrudan serileştirilmez).
- Dosya adı: `dozdefteri_yedek_{yyyy-MM-dd}.json`. `CreateDocument("application/json")` ile kaydedilir.
- Geri yükleme (`OpenDocument`): `format` ve `schemaVersion` doğrula; uyumsuzsa hata mesajı. Onay diyaloğu: "Mevcut tüm veriler silinip yedekteki verilerle değiştirilecek." Onaylanırsa tek transaction'da tüm tablolar temizlenir ve yedek eklenir (ID'ler korunur). Sonra aktif profil = ilk profil, `DoseSync.run("restore")`, tüm bildirimler iptal edilir.
- Dışa aktarım ve yedekleme hiçbir depolama izni istemez (Storage Access Framework).

---

## 9. Widget (`widget/`, Glance) — Pro

- Boyut: 2×1 ve 3×2 (`SizeMode.Responsive`).
- İçerik: aktif profilin bir sonraki dozu (ilaç adı, saat) ve bugünün özeti ("3/5 alındı"). Doz yoksa "Bugün başka doz yok".
- Dokununca uygulamayı Bugün ekranında açar. Widget'tan aksiyon yok (MVP).
- Pro değilse: "Widget Pro ile kullanılabilir" metni, dokununca Pro ekranı.
- `DoseSync` sonunda `NextDoseWidget().updateAll(context)`.

---

## 10. Tema

- Ana renk (accent 0): petrol mavisi `#0E7C86`. Pro accent'ler: 1 = `#5B5BD6` (indigo), 2 = `#C2410C` (turuncu), 3 = `#15803D` (yeşil). Her biri için Material 3 açık ve koyu `ColorScheme` üret (Material Theme Builder mantığıyla, elle tanımla).
- Profil renk paleti (8 renk): `#0E7C86, #5B5BD6, #C2410C, #15803D, #B91C1C, #A16207, #7E22CE, #475569`.
- Durum renkleri: alındı `#15803D`, kaçırıldı `#B91C1C`, atlandı/beklemede `onSurfaceVariant`. Her zaman ikonla birlikte.
- Tipografi: Material 3 varsayılan; `fontScale` 2.0'a kadar düzen bozulmamalı (metinler kırılır, kesilmez).

---

## 11. Testler

### 11.1 Zorunlu birim testleri (`src/test`)

`DoseGeneratorTest` (en az şu vakalar):
- DAILY, iki saat, 3 günlük aralık → 6 doz.
- WEEKDAYS Pzt/Çar/Cum → doğru günler.
- EVERY_N_DAYS, intervalDays=3, startDate'ten itibaren doğru günler; startDate'ten önceki günler hariç.
- endDate dahil, endDate sonrası hariç.
- AS_NEEDED → boş.
- `from`/`to` sınırları (from dahil, to hariç).
- Ay sonu ve 29 Şubat 2028.
- Europe/Berlin ileri yaz saati geçişi (02:30 dozu → 03:30, bir kez).
- Europe/Berlin geri geçiş (02:30 dozu bir kez).
- Europe/Istanbul (yaz saati yok) basit vaka.

`AdherenceCalculatorTest`: tümü alındı, karışık, payda 0, AS_NEEDED hariç tutma.

`DoseRepositoryTest` (Robolectric + in-memory Room): ensureWindow tekrar çağrıda kopya üretmez; regenerate geçmişe dokunmaz; markMissed eşik sınırı; take stok düşer ve 0'ın altına inmez; düşük stok bayrağı bir kez; undo stok iadesi; snooze `scheduledAt` ve `notifiedAt` sıfırlama.

`CsvExporterTest`: BOM, tırnaklama (virgül ve tırnak içeren ilaç adı), sütun sırası.

`BackupManagerTest`: dışa aktar → içe aktar gidiş-dönüşte veri eşitliği; yanlış `format` reddi.

### 11.2 Enstrümantasyon testleri (`src/androidTest`)

- Room migration test altyapısı (sürüm 1 şema dosyası mevcut olmalı).
- `AlarmScheduler`: alarm kurulduktan sonra `alarmManager.nextAlarmClock` değil, `PendingIntent` varlığı `FLAG_NO_CREATE` ile doğrulanır.
- Compose UI: ilaç ekleme formu geçerli/geçersiz durumlar; Bugün ekranında "Aldım" sonrası durum değişimi.

Enstrümantasyon testleri faz komutlarına dahil değildir; ajan `./gradlew connectedDebugAndroidTest` komutunu yalnızca emülatör erişimi varsa çalıştırır, yoksa raporda belirtir.

---

## 12. Fazlar

Her faz: hedef → görevler → kabul kriterleri. Faz sonunda Bölüm 0.3 raporunu `reports/faz-N.md` olarak yaz ve **durmadan** sonraki faza geç. Faz 9'dan sonra `reports/OZET.md` (Bölüm 0.6).

### Faz 0 — Proje iskeleti

Görevler:
1. Bölüm 2.1 yapısıyla Android projesini oluştur (Compose, Kotlin DSL, version catalog).
2. Bölüm 2.3 bağımlılıklarını güncel kararlı sürümlerle ekle.
3. Hilt kurulumu (`DozDefteriApp`, `MainActivity` `@AndroidEntryPoint`).
4. `ClockModule`, boş tema (Bölüm 10 renkleri), `strings.xml` (EN) ve `values-tr/strings.xml` iskeleti.
5. Alt navigasyonlu boş 4 sekme ve placeholder ekranlar.
6. `.gitignore` (Android standart + `local.properties`, `*.jks`, `*.keystore`).
7. `README.md`: proje adı, derleme komutları, bu planın adı.

Kabul: uygulama emülatörde açılır, 4 sekme arasında geçilir, koyu/açık tema çalışır, TR cihazda Türkçe metinler görünür.

### Faz 1 — Veri katmanı

Görevler: Bölüm 3'teki enum, entity, converter, DAO, `AppDatabase`, `SettingsRepository`, repository'ler (DoseRepository dahil, Bölüm 4.3), `DoseGenerator`, `AdherenceCalculator`, ilk açılış profili. Bölüm 11.1'deki `DoseGeneratorTest`, `AdherenceCalculatorTest`, `DoseRepositoryTest`.

Kabul: tüm birim testleri geçer; `app/schemas/.../1.json` oluşur.

### Faz 2 — Alarm motoru

Görevler: Bölüm 5'in tamamı, manifest kayıtları (Bölüm 2.4), bildirim kanalları, `MaintenanceWorker` planlaması. Geçici bir debug ekranı veya Bugün placeholder'ında "1 dakika sonra test dozu oluştur" butonu (yalnızca `BuildConfig.DEBUG`'da görünür).

Kabul:
- Debug butonuyla oluşturulan doz 1 dakika sonra bildirim olarak gelir; uygulama kapatılmış olsa da.
- Aldım/Ertele/Atla aksiyonları veritabanını doğru günceller, bildirim kapanır.
- Ertele sonrası bildirim ayarlanan süre sonra tekrar gelir.
- Raporda insan için elle test listesi: cihaz yeniden başlatma, `adb shell dumpsys deviceidle force-idle` sonrası alarm, saat dilimi değişimi.

### Faz 3 — Onboarding, ilaç ekleme, İlaçlar ekranı

Görevler: Bölüm 6.2, 6.4, 6.5. İzin akışları.

Kabul: Bölüm 6.4 doğrulama kurallarının tamamı çalışır; kayıt sonrası dozlar üretilir ve alarm kurulur; arşivleme gelecekteki dozları kaldırır.

### Faz 4 — Bugün ve Geçmiş

Görevler: Bölüm 6.3, 6.6, uyarı bantları, geri alma, gerektiğinde ilaçlar, düşük stok kartı. Debug test butonunu Bugün'den kaldır, Ayarlar altında yalnızca debug'da görünen "Geliştirici" bölümüne taşı.

Kabul: Bugün ekranı bildirimden yapılan değişikliği uygulamaya dönünce anında gösterir (Flow); geçmiş takvim renkleri ve uyum yüzdesi testlerdeki hesaplamayla tutarlı.

### Faz 5 — Dışa aktarım ve yedek

Görevler: Bölüm 8 (Pro'ya bağlı kısımlar `isPro` false varsayımıyla çalışır, Faz 7'de bağlanır), Bölüm 6.7. `CsvExporterTest`, `BackupManagerTest`.

Kabul: CSV Excel'de Türkçe karakterlerle doğru açılır (insan doğrulaması raporda istenir); PDF birden fazla sayfaya taşar ve başlık tekrarlanır; yedek geri yükleme sonrası alarmlar yeniden kurulur.

### Faz 6 — Ayarlar, profiller, pil rehberi

Görevler: Bölüm 6.8, 6.9, 6.10, 6.12; DataStore ayarlarının etkisi (erteleme süresi, kaçırıldı eşiği, tema modu).

Kabul: profil değişince Bugün/Geçmiş o profili gösterir; profil silme o profilin bildirimlerini iptal eder; ayarlar uygulama yeniden başlatıldığında korunur.

### Faz 7 — Pro ve Billing

Görevler: Bölüm 7, 6.11; Pro kontrollerini ilgili yerlere bağla (profil limiti, PDF şablonu, vurgu rengi, "Tüm profiller"); birleşik manifest kontrolü (Bölüm 2.4).

Kabul: `DEBUG_FORCE_PRO = true` ile tüm Pro özellikler açılır, `false` ile kapanır; Billing bağlanamadığında uygulama çökmez ve önbellek kullanılır. Gerçek satın alma testi insan tarafından yapılacak (raporda belirt).

### Faz 8 — Widget

Görevler: Bölüm 9.

Kabul: widget eklenir, bir sonraki dozu gösterir, doz işaretlenince güncellenir, Pro değilken kilit mesajı gösterir.

### Faz 9 — Cila, erişilebilirlik, release hazırlığı

Görevler:
1. Tüm metinlerin TR/EN çevirisinin eksiksiz olduğunu kontrol et (eksik çeviri lint'i hata değil ama raporla).
2. TalkBack etiketleri, 48dp alanlar, `fontScale` 2.0 kontrolü (Compose preview'ları `fontScale = 2f` ile).
3. Uygulama ikonu için adaptive icon iskeleti (ön plan: basit hap simgesi vektör, arka plan: `#0E7C86`). İnsan daha sonra değiştirebilir.
4. Release derlemesi: `./gradlew bundleRelease` imzasız olarak derlenebilmeli; R8 sonrası Room, Hilt, serialization, Billing için gerekli keep kuralları.
5. `versionCode = 1`, `versionName = "1.0.0"`.
6. `CHANGELOG.md` oluştur.

Kabul: `./gradlew assembleRelease` (imzasız) başarılı; release APK'da uygulama açılır, ilaç eklenir, bildirim gelir (insan doğrulaması); raporda Bölüm 13 listesi.

---

## 13. İnsanın yapması gerekenler (ajan YAPMAZ, beklemez; yalnızca OZET.md'de listeler)

| Ne zaman | Görev |
|---|---|
| İş bittiğinde | applicationId'yi (`app.dozdefteri`) kontrol et; Play Console geliştirici hesabını aç ($25) |
| Faz 1'den itibaren | Kapalı test için 15–20 kişilik liste topla (en az 12 kişi 14 gün kesintisiz katılmalı) |
| Faz 2 sonrası | Gerçek cihazda yeniden başlatma, Doze, Xiaomi/Samsung testleri |
| Faz 6 sonrası | Gizlilik politikası sayfasını yayınla, URL'yi `strings.privacy_policy_url`'ye yaz |
| Faz 7 öncesi | Play Console'da `pro_lifetime` ürününü oluştur (TR ₺149, US $4,99), lisans test hesaplarını ekle |
| Faz 9 sonrası | Upload keystore oluştur ve imzalama yapılandırmasını ekle; Play App Signing'i etkinleştir |
| Yayın öncesi | Veri güvenliği formu (veri toplanmıyor), Sağlık uygulamaları beyanı, içerik derecelendirmesi, hedef kitle, mağaza metni ve ekran görüntüleri; kapalı test kanalına yükleme |
| İş bittiğinde | `reports/OZET.md`'yi ve kodu gözden geçir, OZET.md'deki commit planına göre commit et |

---

## 14. Ajan için başlangıç talimatı (kopyala-yapıştır)

```
Repo kökündeki AGENT_PLAN.md dosyasını baştan sona oku ve Faz 0'dan Faz 9'a kadar
tamamını kesintisiz uygula.

- Bana hiçbir soru sorma ve hiçbir fazdan sonra onay bekleme.
- Belirsiz durumlarda Bölüm 0.4'teki karar kurallarına göre karar ver ve raporla.
- Her fazın sonunda reports/faz-N.md raporunu yaz, sonra hemen sonraki faza geç.
- Git commit, add, push yapma.
- Faz 9'dan sonra reports/OZET.md dosyasını yaz. İş ancak o zaman bitmiş sayılır.

Ben her şeyi en sonda raporlar üzerinden inceleyeceğim.
```
