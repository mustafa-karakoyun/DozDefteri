# Faz 2 — Alarm motoru

## Yapılanlar
- `DoseSync.run(reason)`: Bölüm 5.1 sırası (markMissed → ensureWindow → kaçırılanların bildirimini iptal → scheduleNext → widget). `Mutex` ile eşzamanlı çağrılar sıraya alınır.
- `AlarmScheduler.scheduleNext()`: tek `PendingIntent` (requestCode 1001, `FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE`), tetik = `max(scheduledAt, now + 1 sn)`, kesin alarm izni varsa `setExactAndAllowWhileIdle`, yoksa `setAndAllowWhileIdle`. `canScheduleExactAlarms()` UI için açık.
- `DoseAlarmReceiver`: `scheduledAt <= now + 30 sn` olan açık ve bildirilmemiş dozları gösterir, `notifiedAt = now`, sonra `DoseSync`.
- `DoseActionReceiver`: Aldım / Ertele (ayarlardaki süre) / Atla, bildirimi iptal eder, `DoseSync`.
- `RescheduleReceiver`: BOOT_COMPLETED, LOCKED_BOOT_COMPLETED, TIME_SET, TIMEZONE_CHANGED, MY_PACKAGE_REPLACED, SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED.
- Tüm receiver'lar `goAsync()` + `Dispatchers.IO` + 9 sn zaman aşımı (`runAsync` uzantısı), hata loglanır.
- `DoseNotifier`: 3 kanal (`dose_reminders` HIGH, `stock_alerts` DEFAULT, `backup_reminders` LOW), doz bildirimi (Bölüm 5.4: başlık, "miktar birim · saat", çok profilde profil adı öneki, kategori/öncelik, grup + 2+ bildirimde özet id 0, aksiyon requestCode = id*10+indeks), düşük stok bildirimi (`LowStockNotifier` uygulaması), yedek hatırlatma bildirimi. İzin yoksa sessizce hiçbir şey göstermez.
- `MaintenanceWorker` (`@HiltWorker`): 6 saatte bir, `KEEP`, ad `dose_maintenance`; `DoseSync` + 30 günlük yedek hatırlatması.
- `DozDefteriApp`: kanalları oluşturur, worker'ı planlar, HiltWorkerFactory ile WorkManager yapılandırması, ilk profil + açılış senkronu.
- `MainActivity.onResume` → `DoseSync.run("resume")`.
- Manifest: yalnızca 3 izin, receiver kayıtları (yalnız `RescheduleReceiver` exported), WorkManager varsayılan başlatıcısı kaldırıldı.
- Debug: Bugün placeholder'ında yalnızca `BuildConfig.DEBUG`'da görünen "Create test dose in 1 minute" butonu (`DebugDoseCreator`).
- Enstrümantasyon testi: `AlarmSchedulerTest` (`FLAG_NO_CREATE` ile `PendingIntent` varlığı).

## Oluşturulan / değişen dosyalar
- `app/src/main/java/app/dozdefteri/alarm/{AlarmScheduler,DoseSync,DoseAlarmReceiver,DoseActionReceiver,RescheduleReceiver,DoseNotifier,MaintenanceWorker,WidgetUpdater,ReceiverEntryPoint,ReceiverExt}.kt`
- `app/src/main/java/app/dozdefteri/core/format/Formats.kt`, `core/debug/DebugDoseCreator.kt`, `core/di/NotifierModule.kt`
- `app/src/main/java/app/dozdefteri/{DozDefteriApp,MainActivity}.kt`, `ui/today/DebugViewModel.kt`, `ui/navigation/AppNavHost.kt`
- `app/src/main/java/app/dozdefteri/data/repository/DoseRepository.kt`
- `app/src/main/AndroidManifest.xml`, `res/values{,-tr}/strings.xml`
- `app/src/androidTest/java/app/dozdefteri/alarm/AlarmSchedulerTest.kt`

## Komut sonuçları
- assembleDebug: ❌ çalıştırılamadı (dl.google.com engeli)
- testDebugUnitTest: ❌ çalıştırılamadı (aynı neden); saf Kotlin testleri JVM projesinde 16/16 ✅ (değişmedi)
- lintDebug: ❌ çalıştırılamadı
- connectedDebugAndroidTest: çalıştırılamadı (emülatör yok)

## Kabul kriterleri
- [ ] Debug butonuyla oluşturulan doz 1 dk sonra bildirim olarak gelir (uygulama kapalıyken de) — cihaz gerekli, doğrulanamadı
- [ ] Aldım/Ertele/Atla veritabanını doğru günceller, bildirim kapanır — repository mantığı testli (Faz 1), uçtan uca cihazda doğrulanmalı
- [ ] Ertele sonrası bildirim ayarlanan süre sonra tekrar gelir — cihazda doğrulanmalı
- [x] İnsan için elle test listesi raporda (aşağıda)

## Verilen kararlar
- **Receiver'larda Hilt alan enjeksiyonu yerine `EntryPointAccessors`** kullanıldı: `@AndroidEntryPoint` BroadcastReceiver'ın bytecode dönüşümüne bağımlılığı yok, derlenemeyen ortamda daha az risk.
- **Bildirim izni yokken de `notifiedAt` yazılır**; aksi hâlde alarm aynı doz için sürekli yeniden kurulup döngüye girerdi.
- **İlk yedek hatırlatması:** `last_backup_reminder_at = 0` iken (ilk kurulum) bildirim gösterilmez, sayaç başlatılır; böylece ilk hatırlatma kurulumdan ~30 gün sonra gelir.
- **Bildirim etiketleri:** Doz bildirimleri etiketsiz `id = event.id`; stok (`tag = "stock"`, id = ilaç id) ve yedek (`tag = "backup"`) farklı etiketle gönderilir, id çakışması olmaz.
- **Kesin alarm `SecurityException`** (kontrol ile çağrı arasında izin geri alınırsa) yakalanıp kesin olmayan alarma düşülür.
- **Debug test dozu** "Gerektiğinde" planlı geçici bir ilaç ve tek bir PENDING olay oluşturur: doz üretici başka doz üretmez, uyum yüzdesine girmez.
- Profil adı öneki için profil sayısı her alarmda okunur (`> 1` ise).

## Çözülemeyen sorunlar
- Derleme/çalıştırma yok (ortam engeli). Alarm davranışı yalnızca gerçek cihazda doğrulanabilir.

## Kullanıcının elle doğrulaması gerekenler
1. Debug APK'yı kur, bildirim ve kesin alarm iznini ver; Bugün ekranındaki "Create test dose in 1 minute" butonuna bas, uygulamayı kapat (son uygulamalardan kaydır) → ~1 dk sonra bildirim gelmeli.
2. Bildirimde **Aldım**, **Atla**, **Ertele**'yi ayrı test dozlarında dene; bildirim kapanmalı, Ertele sonrası 10 dk sonra tekrar gelmeli.
3. Cihazı yeniden başlat: önceden planlı bir doz zamanında gelmeli.
4. Doze: `adb shell dumpsys deviceidle force-idle` sonrası planlı doz bildirimi gelmeli; sonra `adb shell dumpsys deviceidle unforce`.
5. Saat dilimini değiştir (Ayarlar > Tarih/saat): yerel saatle planlı doz yeni dilimde doğru yerel saatte gelmeli.
6. Kesin alarm iznini kapat/aç (Android 12+): alarm yeniden kurulmalı.
7. Xiaomi/Samsung gibi agresif pil yönetimli cihazlarda test.

## Önerilen commit mesajı
feat(alarm): add exact alarm scheduling, dose notifications and maintenance worker
