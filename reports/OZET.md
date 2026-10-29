# DozDefteri — Özet rapor

> **En önemli not:** Bu bulut ortamı `dl.google.com`'a erişemediği için (ağ politikası 403) Android Gradle Plugin,
> AndroidX, Play Billing ve Android SDK indirilemedi. **Hiçbir Gradle komutu (derleme, test, lint) çalıştırılamadı.**
> Kodun tamamı yazıldı; saf Kotlin kısımları (alan mantığı, form doğrulama, CSV, yedek) ayrı bir Kotlin/JVM projesinde
> derlenip **39/39 test geçti**. Android'e bağlı kısımlar ilk kez senin derlemende derlenecek; küçük derleme hataları
> çıkarsa bu rapordaki "İlk derleme" bölümüne bak.

## 1. Faz tablosu

| Faz | Konu | Durum | Kabul kriterleri (karşılanan/toplam) | Rapor |
|---|---|---|---|---|
| 0 | Proje iskeleti | Kısmi (kod tamam, derlenmedi) | 0/4 | [faz-0.md](faz-0.md) |
| 1 | Veri katmanı | Kısmi | 0/2 (saf testler ✅, Robolectric ve şema dosyası derleme gerektiriyor) | [faz-1.md](faz-1.md) |
| 2 | Alarm motoru | Kısmi | 1/4 | [faz-2.md](faz-2.md) |
| 3 | Onboarding, ilaç ekleme, İlaçlar | Kısmi | 1/3 | [faz-3.md](faz-3.md) |
| 4 | Bugün ve Geçmiş | Kısmi | 1/2 | [faz-4.md](faz-4.md) |
| 5 | Dışa aktarım ve yedek | Kısmi | 0/3 (mantık testli, insan doğrulaması gerekli) | [faz-5.md](faz-5.md) |
| 6 | Ayarlar, profiller, pil rehberi | Kısmi | 0/3 | [faz-6.md](faz-6.md) |
| 7 | Pro ve Billing | Kısmi | 0/3 | [faz-7.md](faz-7.md) |
| 8 | Widget | Kısmi | 0/4 | [faz-8.md](faz-8.md) |
| 9 | Cila, erişilebilirlik, release | Kısmi | 1/3 | [faz-9.md](faz-9.md) |

"Kısmi" = özelliklerin kodu yazıldı, ancak kabul kriterlerinin çoğu derleme ya da cihaz gerektirdiği için doğrulanamadı.

## 2. Verilen kararların birleşik listesi

**Ortam ve süreç**
- Kullanıcının onayıyla plan kuralı değişti: her faz sonunda `ccr-d6d292a7-87sutq` branch'ine commit + push yapıldı (konteyner geçici olduğu için). `main`'e dokunulmadı, PR açılmadı.
- Derleme ortam yüzünden imkânsız olduğu için 0.5'teki "derleme hatalıyken sonraki faza geçme" kuralı uygulanamadı; kullanıcının "hepsini bitir" talimatıyla devam edildi. Saf Kotlin kodu ayrı JVM projesinde derlenip test edildi.

**Sürümler ve yapı**
- AGP 9.4.0 (yerleşik Kotlin; `kotlin-android` eklentisi uygulanmaz), Gradle 9.6.0, Kotlin 2.3.21, KSP 2.3.12, Hilt 2.60.1, Compose BOM 2026.09.00, Billing 9.1.0 (tam liste: faz-0.md).
- `compileSdk = 37`, `targetSdk = 36` (Compose/AndroidX kütüphaneleri API 37 ile derleniyor; Play API 36 istiyor).
- `hilt-navigation-compose` yerine resmi halefi `hilt-lifecycle-viewmodel-compose` (`hiltViewModel()` artık orada).
- Bulut yedeklemesi kapalı (`allowBackup=false`), cihazdan cihaza aktarım açık.
- `androidx.core` ayrıca eklenmedi (activity-compose üzerinden geliyor).

**Veri ve alarm**
- Düşük stok bildirimi `LowStockNotifier` arayüzüyle soyutlandı.
- `take/skip/snooze` yalnızca PENDING/SNOOZED dozlarda çalışır; `undo` TAKEN ve SKIPPED için 24 saat içinde; AS_NEEDED kaydı geri alınırsa silinir; iade dozun tam miktarıdır.
- Stok iadesi/artışı eşiğin üstüne çıkarsa düşük stok bayrağı sıfırlanır.
- Gün aralıkları `originalScheduledAt`'e, Bugün'deki saat grupları `scheduledAt`'e göre.
- Receiver'larda Hilt `EntryPointAccessors`; bildirim izni yokken de `notifiedAt` yazılır (alarm döngüsü olmasın).
- İlk yedek hatırlatması kurulumdan ~30 gün sonra.
- Bildirim etiketleri: doz (etiketsiz, id = doz id), stok (`stock`), yedek (`backup`).

**Arayüz**
- Ad/birim hataları alan değişince gösterilir; Kaydet geçersiz formda her zaman pasif.
- Uyarı eşiği isteğe bağlı; ondalıkta "." ve "," kabul.
- Form seçimi `OutlinedButton + DropdownMenu`, plan türü radyo listesi (fontScale 2.0 için).
- Erteleme: doz kartına uzun basınca (TalkBack özel aksiyonu da var).
- Gün durumu: kaçırılan → kırmızı; hepsi alındı → yeşil; yalnız bekleyen → boş; diğer → turuncu.
- Profil adı en fazla 30 karakter; gizlilik politikası satırı URL boşken gizli; Pro bitince vurgu rengi korunur ama uygulanmaz.

**Dışa aktarım**
- Yedek iki adımlı (önce doğrula, sonra onayla, sonra tek transaction'da yaz); kopuk referanslı yedek reddedilir; `ignoreUnknownKeys`.
- CSV'de miktar uygulama diline göre biçimlenir ve gerekirse tırnaklanır; başlıklar dile göre.
- PDF hücreleri sığmazsa "…" ile kısaltılır; "Tüm profiller" dosya adında `all`.

**Pro / Widget**
- Activity ViewModel'de tutulmaz, yalnızca satın alma çağrısına geçirilir.
- ITEM_ALREADY_OWNED → geri yüklendi; başarılı sorguda ürün yoksa Pro kapanır (iade).
- Widget receiver `exported="true"` (sistem widget yayınları için; Bölüm 2.4'e gerekçeli istisna); saatlik yenileme + her DoseSync sonrası güncelleme.

## 3. Çözülemeyen sorunlar ve `@Ignore` edilen testler
- **dl.google.com erişimi yok** → derleme, Android birim testleri (Robolectric), lint, release derlemesi, birleşik manifest kontrolü ve Room şema dosyası (`app/schemas/.../1.json`) üretilemedi.
- `@Ignore` edilen test **yok**.
- Koşturulamayan testler: `DoseRepositoryTest` (10), `BackupManagerTest` (2) — Robolectric; enstrümantasyon: `AlarmSchedulerTest`, `MedicationEditContentTest`, `TodayContentTest`, `MigrationTest`.
- Koşturulan ve geçen testler (JVM): `DoseGeneratorTest` 12, `AdherenceCalculatorTest` 4, `DayStatusCalculatorTest` 4, `MedicationFormValidatorTest` 10, `CsvExporterTest` 4, `BackupCodecTest` 5 = **39/39**.

## 4. Elle doğrulama kontrol listesi (sıralı)

**A. Derleme (önce bu)**
1. Bu ortamda tekrar çalıştırmak istersen: claude.ai'de ortam ayarlarından **Network access → Allowed domains**'e `dl.google.com` ekle (veya daha geniş erişim seç), yeni oturum başlat. Ayrıntı: https://code.claude.com/docs/en/cloud-environments#network-access . Ya da projeyi Android Studio'da kendi bilgisayarında aç.
2. `./gradlew assembleDebug testDebugUnitTest lintDebug` — derleme hatası çıkarsa: hata büyük olasılıkla bir kütüphane sürümünün API farkıdır (ör. Compose/Glance/Billing imzası). Hata mesajını yeni bir oturumda bana ver, düzelteyim.
3. Oluşan `app/schemas/app.dozdefteri.data.db.AppDatabase/1.json` dosyasını commit et.
4. Android Studio > `AndroidManifest.xml` > Merged Manifest: `INTERNET`/`ACCESS_NETWORK_STATE` hangi kütüphaneden geliyor, not al (plan gereği kaldırmadım).
5. `./gradlew assembleRelease bundleRelease` (imzasız) ve `./gradlew connectedDebugAndroidTest` (emülatörle).

**B. Cihazda işlev**
6. Onboarding (Android 13+ 3 sayfa, 12'de 2), izin reddi ve "Ayarları aç".
7. İlaç ekle (tüm plan türleri), planlı saatte bildirim; Aldım / Ertele (10 dk sonra tekrar) / Atla.
8. Ayarlar > Geliştirici > "Create test dose in 1 minute" ile uygulama kapalıyken bildirim.
9. Yeniden başlatma, `adb shell dumpsys deviceidle force-idle`, saat dilimi değişimi, kesin alarm iznini kapat/aç.
10. Xiaomi ve Samsung cihazda test, pil rehberi metinleri.
11. Bugün: bildirimden işlem sonrası anında güncelleme, gecikti etiketi, geri al, gerektiğinde ilaçlar, düşük stok kartı.
12. Geçmiş: takvim renkleri ve ay yüzdesi.
13. CSV'yi Excel'de (TR) açma; 90 günlük çok sayfalı PDF; yedekle → kaldır/kur → geri yükle; yanlış dosyayla geri yükleme hatası.
14. Profil ekle/değiştir/sil, tema/vurgu, ayarların yeniden açılışta korunması.
15. `DEBUG_FORCE_PRO = true/false` ile tüm Pro kilitleri.
16. Widget (2×1, 3×2), güncellenme, Pro değilken kilit.
17. TalkBack ve en büyük yazı boyutu.
18. Release APK: açılış, ilaç ekleme, bildirim.

**C. Bölüm 13 — insan görevleri**
19. **applicationId `app.dozdefteri`'yi kontrol et — Play'e ilk yüklemeden sonra değiştirilemez.** Play Console geliştirici hesabını aç ($25).
20. Kapalı test için 15–20 kişilik liste (en az 12 kişi 14 gün kesintisiz).
21. Gizlilik politikası sayfasını yayınla, URL'yi `app/src/main/res/values/strings.xml` > `privacy_policy_url`'ye yaz.
22. Play Console'da `pro_lifetime` ürününü oluştur (TR ₺149, US $4,99), lisans test hesaplarını ekle; test satın alma, bekleyen ödeme, iade, geri yükleme.
23. Upload keystore oluştur, imzalama yapılandırmasını ekle (keystore'u commit etme; `.gitignore` `*.jks`/`*.keystore` içeriyor), Play App Signing'i etkinleştir.
24. Veri güvenliği formu (veri toplanmıyor), Sağlık uygulamaları beyanı, içerik derecelendirmesi, hedef kitle, mağaza metni ve ekran görüntüleri; kapalı test kanalına yükleme.
25. `reports/OZET.md` ve kodu gözden geçir.

## 5. Son komut sonuçları

| Komut | Sonuç |
|---|---|
| `assembleDebug` | ❌ çalıştırılamadı — `Plugin com.android.application:9.4.0 was not found` (dl.google.com 403) |
| `testDebugUnitTest` | ❌ çalıştırılamadı (aynı neden). Yerine: saf Kotlin testleri JVM'de 39/39 ✅ |
| `lintDebug` | ❌ çalıştırılamadı |
| `assembleRelease` | ❌ çalıştırılamadı |

## 6. Commit planı

Kullanıcı onayıyla commit'ler faz faz zaten yapıldı ve `ccr-d6d292a7-87sutq` branch'ine push edildi:

| Commit | Faz | Dosya grupları |
|---|---|---|
| `build: scaffold DozDefteri Android project with Compose, Hilt and version catalog` | 0 | Gradle dosyaları, manifest, tema, navigasyon iskeleti, kaynaklar, README, AGENT_PLAN |
| `feat(data): add Room data layer, dose generation and repositories` | 1 | `data/**`, `domain/**`, `core/di/**`, testler |
| `feat(alarm): add exact alarm scheduling, dose notifications and maintenance worker` | 2 | `alarm/**`, manifest receiver'ları, debug test dozu |
| `feat(ui): add onboarding, medication editor and medication list` | 3 | `ui/onboarding`, `ui/medications`, `ui/components`, `MainActivity` |
| `feat(ui): add today screen with dose actions and history calendar` | 4 | `ui/today`, `ui/history`, `domain/DayStatus.kt` |
| `feat(export): add CSV and PDF export with JSON backup and restore` | 5 | `export/**`, `ui/export`, `billing/ProStatus.kt` |
| `feat(settings): add settings, profiles management and battery guide` | 6 | `ui/settings`, `ui/profiles`, `ui/battery` |
| `feat(billing): add one-time Pro purchase with Play Billing and feature gates` | 7 | `billing/BillingManager.kt`, `ui/pro` |
| `feat(widget): add Glance next-dose widget for Pro users` | 8 | `widget/**`, widget XML |
| `chore(release): polish accessibility, align library APIs and prepare release build` | 9 | biçim düzeltmeleri, sürüm uyumluluğu, CHANGELOG, MigrationTest |
| `docs: add summary report` | — | `reports/OZET.md` |

İstersen bunları tek bir PR'da `main`'e birleştirebilirsin; ilk başarılı derlemeden sonra şema dosyası için ayrı bir commit ekle.
