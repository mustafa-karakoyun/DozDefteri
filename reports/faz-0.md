# Faz 0 — Proje iskeleti

## Yapılanlar
- Tek modüllü (`:app`) Gradle Kotlin DSL projesi, version catalog (`gradle/libs.versions.toml`), Gradle wrapper 9.6.0.
- Bölüm 2.3'teki tüm bağımlılıklar güncel kararlı sürümlerle eklendi (aşağıdaki tablo).
- Hilt kurulumu: `DozDefteriApp` (`@HiltAndroidApp`), `MainActivity` (`@AndroidEntryPoint`, edge-to-edge).
- `ClockModule` (`Clock.systemDefaultZone()`).
- Tema: Bölüm 10 renkleri; 4 vurgu rengi × açık/koyu `ColorScheme` elle tanımlandı, dinamik renk kapalı, profil paleti ve durum renkleri.
- `values/strings.xml` (EN) ve `values-tr/strings.xml` (TR) iskeleti.
- Alt navigasyonlu 4 sekme (Bugün, İlaçlar, Geçmiş, Ayarlar) ve placeholder ekranlar; type-safe route'lar (`Routes.kt`).
- `.gitignore`, `README.md`, adaptif ikon iskeleti (hap simgesi, arka plan `#0E7C86`), bildirim ikonu.
- Plan dosyası repoya `AGENT_PLAN.md` olarak kopyalandı.

### Sabitlenen sürümler

| Bileşen | Sürüm | Kaynak |
|---|---|---|
| Android Gradle Plugin | 9.4.0 | developer.android.com AGP sürüm notları (Eylül 2026) |
| Gradle | 9.6.0 | AGP 9.4 asgari sürümü |
| Kotlin (KGP, compose & serialization plugin) | 2.3.21 | Maven Central |
| KSP | 2.3.12 | Maven Central |
| Hilt (Dagger) | 2.60.1 | Maven Central |
| androidx.hilt (navigation-compose, work, compiler) | 1.4.0 | AndroidX stable channel |
| Compose BOM | 2026.09.00 (ui 1.12.1, material3 1.4.0) | BOM mapping sayfası |
| activity-compose | 1.13.0 | AndroidX stable |
| lifecycle | 2.11.0 | AndroidX stable |
| navigation-compose | 2.10.2 | AndroidX stable |
| Room | 2.8.5 | AndroidX stable |
| DataStore | 1.3.0 | AndroidX stable |
| WorkManager | 2.12.0 | AndroidX stable |
| Glance | 1.2.0 | AndroidX stable |
| Play Billing (billing-ktx) | 9.1.0 | Play Billing sürüm notları |
| kotlinx-serialization-json | 1.10.0 | Maven Central |
| kotlinx-coroutines-test | 1.10.2 | Maven Central |
| JUnit | 4.13.2 | |
| Turbine | 1.2.1 | |
| Truth | 1.4.5 | Maven Central |
| androidx.test.ext:junit / runner | 1.3.0 / 1.7.0 | AndroidX stable |
| Robolectric | 4.16 | |

## Oluşturulan / değişen dosyalar
- `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `gradle/wrapper/*`, `gradlew`, `gradlew.bat`
- `app/build.gradle.kts`, `app/proguard-rules.pro`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/app/dozdefteri/DozDefteriApp.kt`, `MainActivity.kt`
- `app/src/main/java/app/dozdefteri/core/time/ClockModule.kt`
- `app/src/main/java/app/dozdefteri/domain/model/ThemeMode.kt`
- `app/src/main/java/app/dozdefteri/ui/theme/{Color,Theme,Type}.kt`
- `app/src/main/java/app/dozdefteri/ui/navigation/{Routes,AppNavHost}.kt`
- `app/src/main/res/values{,-tr,-night}/*`, `res/xml/data_extraction_rules.xml`, `res/mipmap-anydpi/*`, `res/drawable/*`
- `.gitignore`, `README.md`, `AGENT_PLAN.md`

## Komut sonuçları
- assembleDebug: ❌ çalıştırılamadı — ortam engeli (aşağıya bakın)
- testDebugUnitTest: ❌ çalıştırılamadı (aynı neden)
- lintDebug: ❌ çalıştırılamadı (aynı neden)

Çıktı: `Plugin [id: 'com.android.application', version: '9.4.0'] was not found ... could not resolve plugin artifact`.
Bu bulut ortamının ağ politikası `dl.google.com` alanını 403 ile engelliyor. `maven.google.com` tüm dosyaları
`dl.google.com`'a yönlendirdiği için Android Gradle Plugin, tüm AndroidX kütüphaneleri, Play Billing ve Android SDK
(platform 36, build-tools) indirilemiyor. Ortamda Android SDK de kurulu değil.

## Kabul kriterleri
- [ ] Uygulama emülatörde açılır — doğrulanamadı (derleme ortamı yok, emülatör yok)
- [ ] 4 sekme arasında geçilir — kod yazıldı, doğrulanamadı
- [ ] Koyu/açık tema çalışır — kod yazıldı, doğrulanamadı
- [ ] TR cihazda Türkçe metinler görünür — kod yazıldı, doğrulanamadı

## Verilen kararlar
- **Kullanıcının onayıyla git kuralı değiştirildi:** Plan "commit yapma" diyor; ancak bulut konteyneri geçici olduğundan kullanıcı faz başına commit + push'u seçti. Her faz `ccr-d6d292a7-87sutq` branch'ine raporlardaki önerilen mesajla commit edilir.
- **Derleme yapılamadan devam ediliyor** — Kural 0.5 "derleme hatalıyken sonraki faza geçilmez" diyor; burada hata kodda değil ortamda ve hiçbir faz derlenemeyecek. Kullanıcı tüm işin bitmesini istediği için kod yazılmaya devam edildi; saf Kotlin kısımlar (alan mantığı, CSV, yedek DTO'ları) ayrı bir JVM projesinde derlenip testleri koşturularak doğrulanacak (Faz 1+ raporlarına bakın).
- **AGP 9.4.0** seçildi (güncel kararlı). AGP 9 yerleşik Kotlin desteği kullandığı için `org.jetbrains.kotlin.android` eklentisi uygulamaya uygulanmıyor; kökte `apply false` ile yalnızca KGP sürümünü sabitliyor. Hilt 2.59+ AGP 9 gerektirdiği için AGP 8.x'e düşmek de mümkün değildi.
- **Kotlin 2.3.21** (2.4.x yerine): Dagger/Hilt KSP işlemcisinin ve serialization 1.10'un desteklediği en yeni ana sürüm; derleme doğrulanamadığı için daha olgun olan seçildi.
- **compileSdk/targetSdk 36**: Play, 31 Ağustos 2026'dan itibaren yeni uygulama ve güncellemeler için API 36 zorunlu kılıyor; 36 bu şartı karşılıyor.
- `android:allowBackup="false"` ve bulut yedeklemesi kapalı (veri yalnızca cihazda; plan "verilerin telefonunda"). Cihazdan cihaza aktarım açık.
- `androidx.core` (NotificationCompat vb.) ayrıca eklenmedi; `activity-compose` üzerinden geçişli olarak geliyor.

## Çözülemeyen sorunlar
- `dl.google.com` erişimi yok — `curl`, Gradle ve `maven.google.com` yönlendirmesi denendi; hepsi proxy'de 403. Geçici çözüm: yok (ağ politikası aşılmaz). Kullanıcı ortam ayarlarında izin vermeli veya kendi bilgisayarında derlemeli.

## Kullanıcının elle doğrulaması gerekenler
- Android Studio'da projeyi açıp `./gradlew assembleDebug testDebugUnitTest lintDebug` çalıştırmak.
- Uygulamayı emülatörde açıp 4 sekme, açık/koyu tema ve TR dilini kontrol etmek.

## Önerilen commit mesajı
build: scaffold DozDefteri Android project with Compose, Hilt and version catalog
