# DozDefteri — Yeni oturum için devir notu

> Bu dosya, bu projede çalışacak bir sonraki ajan için yazıldı. Önce bunu, sonra `reports/` altındaki raporları oku.
> Kullanıcıyla **Türkçe** konuş. Kullanıcı çoğunlukla telefondan yazıyor: kısa ve sade cevap ver, teknik jargonu azalt.

## 1. Durum özeti (1 Kasım 2026)

- Android uygulaması **DozDefteri**: reklamsız, hesapsız, çevrimdışı ilaç hatırlatıcısı. Kotlin + Jetpack Compose + Material 3 + Hilt + Room + WorkManager + Glance + Play Billing.
- İki plan uygulandı ve tamamlandı:
  1. `AGENT_PLAN.md` — uygulamanın kendisi (Faz 0–9). Raporlar: `reports/faz-0.md` … `faz-9.md`, özet `reports/OZET.md`.
  2. `docs/UI_PLAN.md` — Figma tasarımının uygulanması (UI Faz 1–8). Raporlar: `reports/ui-faz-1.md` … `ui-faz-8.md`.
- Tasarım ekran görüntüleri: `screens/light/*.png`, `screens/dark/*.png` (kullanıcı ekledi). Figma dosyası: `https://www.figma.com/design/ioGvgI4mGuVBY5g8FoVIK9/` — Figma MCP "Starter plan çağrı limiti" hatası veriyor; kullanıcı "Figma'ya bağlanmana gerek yok" dedi. **Ekran görüntülerini kullan.**
- **Kod hiç derlenmedi** (aşağıdaki ortam engeli). Kullanıcı ilk planın kodunu kendi bilgisayarında derleyebildi (Room şeması `app/schemas/.../1.json`'u o commit etti). UI planından sonraki kod henüz kullanıcı tarafından derlenmedi; **ilk derlemede küçük hatalar beklenmeli**.

## 2. Git

- Çalışma branch'i: **`ccr-d6d292a7-87sutq`** (uzakta tek branch bu; `main` yok/kullanılmıyor). PR açılmadı; kullanıcı istemeden açma.
- Kullanıcının kararı: **her faz/iş sonunda commit + push** (planlardaki "commit atma" kuralı kullanıcı onayıyla değişti; konteyner geçici).
- Commit mesajları Conventional Commits, İngilizce. Sona şu satırlar eklenir:
  ```
  Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
  Claude-Session: https://claude.ai/code/session_012HQqa9dpf4ka7kggpS8CMG
  ```
  (Yeni oturumda sistem farklı bir attribution verirse onu kullan.) Commit mesajına model adı/kimliği yazma.
- Kullanıcı da bu branch'e push ediyor (ör. `screens/` klasörü). İşe başlarken `git pull --rebase origin ccr-d6d292a7-87sutq`.

## 3. Ortam engelleri (bulut konteyneri)

- **`dl.google.com` ağ politikasıyla engelli (403)**. AGP, AndroidX, Play Billing ve Android SDK oradan geldiği için **`./gradlew` hiçbir Android görevi çalışmaz**. Kullanıcıya çözüm anlatıldı (ortam ayarları → Network access → Allowed domains'e `dl.google.com` eklemek) ama yapılmadı. Ağı tekrar kontrol et: `curl -sS -o /dev/null -w '%{http_code}' https://dl.google.com/dl/android/maven2/master-index.xml`.
- Maven Central (`repo1/repo.maven.apache.org`) sık sık **429 rate limit** veriyor; Google'ın aynası çalışıyor: `https://maven-central.storage-download.googleapis.com/maven2`.
- İndirilen ikili dosyaları çalıştırma (ör. ktlint) otomatik izin sistemi tarafından **reddedildi**; biçim elle korunuyor.
- **Doğrulama yöntemi (saf Kotlin testleri):** scratchpad'de ayrı bir Kotlin/JVM Gradle projesi kuruldu; Room anotasyonları için stub'lar var, uygulamadan saf dosyalar kopyalanıp testler koşturuluyor. Scratchpad yeni oturumda yok olabilir; gerekirse yeniden kur:
  - `settings.gradle.kts`: pluginManagement ve dependencyResolutionManagement repo'su olarak yukarıdaki Google Maven Central aynası + gradlePluginPortal.
  - `build.gradle.kts`: `kotlin("jvm") version "2.3.21"`, `kotlin("plugin.serialization")`, bağımlılıklar `javax.inject:javax.inject:1`, `kotlinx-serialization-json:1.10.0`, `kotlinx-coroutines-core:1.10.2`, test için `junit:4.13.2`, `truth:1.4.5`; `sourceSets main = stubs + extra`, `test = tests`; `jvmToolchain(21)`.
  - `stubs/androidx/room/Room.kt`: `Entity, PrimaryKey, ForeignKey(+CASCADE), Index(vararg value, unique), TypeConverter, Embedded, Dao, Insert(onConflict), Update, Query(value)` ve `object OnConflictStrategy { const val IGNORE = 5 }`.
  - Kopyalanan saf dosyalar (`app/src/main/java/app/dozdefteri/` altından): `domain/*.kt`, `domain/model/*.kt`, `data/db/entity/*.kt`, `data/db/Converters.kt`, `data/db/dao/DoseEventDao.kt`, `export/{CsvExporter,FileNames,BackupModels}.kt`, `core/format/{Formats,TurkishSuffix,DoseText}.kt`, `ui/medications/{MedicationForm,MedicationSummary}.kt`, `ui/today/TodayModels.kt`.
  - Koşturulan testler (`app/src/test/...`): `domain/{DoseGenerator,AdherenceCalculator,DayStatusCalculator,HistoryStats}Test`, `ui/medications/{MedicationFormValidator,MedicationSummary}Test`, `ui/today/TodayGroupingTest`, `export/{CsvExporter,BackupCodec}Test`, `core/TurkishSuffixTest` → son durum **10 sınıf, hepsi geçiyor**.
  - Robolectric/Room testleri (`DoseRepositoryTest`, `BackupManagerTest`) ve enstrümantasyon testleri burada koşturulamıyor.

## 4. Kullanıcının verdiği kararlar (bunları yeniden sorma)

- Faz başına commit + push.
- Gerçek **Play Billing** korunur (UI planındaki sahte `ProRepository` yerine). **TR + EN** dil desteği korunur.
- Veritabanı **sürüm 2'ye** migration ile taşındı (veri kaybı yok).
- "İşaretlenmedi" sayılma süresi varsayılan **60 dk**; yedek doğrudan **İndirilenler/DozDefteri** (Android 10+ MediaStore; 8–9'da SAF).
- Pro kapsamı (tasarımdaki tablo): birden fazla profil, ana ekran widget'ı, profil renkleri. PDF/CSV, yedek, sınırsız ilaç her zaman ücretsiz.

## 5. Önemli teknik bilgiler

- Sürümler (`gradle/libs.versions.toml`): AGP 9.4.0 (yerleşik Kotlin; `kotlin-android` eklentisi yalnızca kökte `apply false`), Gradle 9.6.0, Kotlin 2.3.21, KSP 2.3.12, Hilt 2.60.1, androidx.hilt 1.4.0 (`hiltViewModel` → `androidx.hilt.lifecycle.viewmodel.compose`), Compose BOM 2026.09.00, Room 2.8.5, Navigation 2.10.2, Glance 1.2.0, Billing 9.1.0. `compileSdk = 37`, `targetSdk = 36`, `minSdk = 26`.
- applicationId / kök paket: `app.dozdefteri`.
- Mimari: MVVM; `Screen` (stateful) + `Content` (stateless, `@DozPreviews` = açık/koyu/%200). Hilt; ViewModel'ler Context'e dokunmaz (`alarm/`, `export/`, `billing/` soyutlamaları).
- Kurallar: kodda `now()` yok, enjekte `java.time.Clock` kullan (`LocalDate.now(clock)` olur); `!!` yok; kullanıcı metinleri `values/strings.xml` (EN) + `values-tr/strings.xml` (TR) — ikisi her zaman aynı anahtarlara sahip olmalı; satır ≤ 120 karakter; ağ izni/HTTP yok.
- Tasarım sistemi: `ui/theme/` (`Palette`, `Color.kt` → `DozColors`/`LocalDozColors`, `Type.kt` → `DozTextStyles`, `Shape.kt` → `Space/Radius/Touch`, `Theme.kt` → `Doz.colors/type/touch`, `DozIcons`). Bileşenler `ui/components/` (DoseCard, StatusChip, StockBar, DayProgressCard, InlineBanner, SettingsRow, FormControls, DoseSheets, ScreenHeader/DozTopBar, DozBottomBar…).
- Türkçe ekler: `core/format/TurkishSuffix` ("08:04'te", "20:10'a", "Mehmet'in", "2'si"); İngilizcede ek yok.
- Veritabanı v2 (`data/db/Migrations.kt`): `medications.strengthAmount/strengthUnit/lowStockDays`, `profiles.note`. Yedek formatı `schemaVersion = 2`, sürüm 1 de kabul.
- Alarm: tek giriş noktası `alarm/DoseSync.run()`; tek `AlarmManager` alarmı; aynı dakikadaki dozlar tek bildirimde (`DoseNotifier`, tag `dose_group`); receiver'lar `EntryPointAccessors` kullanır.
- Navigasyon: `ui/navigation/AppNavHost.kt` (type-safe route'lar `Routes.kt`), aktif profil `LocalHeaderProfile` ile başlıklara veriliyor, profil geçişi `ui/shell/`.
- Debug: Ayarlar'da yalnız debug'da "Developer → Create test dose in 1 minute". `DEBUG_FORCE_PRO` `app/build.gradle.kts` içinde.

## 6. Açık işler / sıradaki adımlar

1. **Kullanıcının derlemesi:** Kullanıcıdan `git pull` + Android Studio'da derleme istendi. Derleme hatalarını gönderirse düzelt, push et. (Ortam açılırsa kendin `./gradlew assembleDebug testDebugUnitTest lintDebug` çalıştır.)
2. Derleme sonrası oluşacak `app/schemas/app.dozdefteri.data.db.AppDatabase/2.json` commit edilmeli (`MigrationTest` buna dayanıyor).
3. Cihazda doğrulanmamış her şey (bildirim eylemleri, toplu bildirim, widget, PDF paylaşma, MediaStore yedeği, onboarding, %200 yazı) — ayrıntılı listeler `reports/OZET.md` ve `reports/ui-faz-*.md` içinde.
4. İsteğe bağlı: UI planı için birleşik özet raporu (`reports/UI_OZET.md`) henüz yazılmadı; kullanıcıya önerildi.
5. Kullanıcının yapacakları (Play Console, `pro_lifetime` ürünü, gizlilik politikası URL'si → `strings.xml` `privacy_policy_url`, imzalama anahtarı) `reports/OZET.md` §4'te.

## 7. Bilinen tasarım sapmaları (raporlarda gerekçeli)

- İlaç formuna "Her seferinde: – 1 tablet +" adımlayıcısı ve profil formuna "Not" alanı eklendi (tasarımda kart metinleri var, giriş alanı yok).
- Dil seçimi yalnız Android 13+ (AppCompat eklenmedi). Pil "Düzelt" doğrudan muafiyet istemez, pil rehberini açar.
- Screenshot testleri (Roborazzi/Paparazzi) eklenmedi (yeni bağımlılık + derleme yok).
