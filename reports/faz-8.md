# Faz 8 — Widget

## Yapılanlar
- **`NextDoseWidget`** (Glance, `SizeMode.Responsive`: 2×1 ≈ 110×40 dp ve 3×2 ≈ 180×110 dp): aktif profilin sıradaki açık dozu (ilaç adı; saat · doz miktarı), büyük boyutta bugünün özeti ("3/5 alındı", AS_NEEDED hariç). Bugün başka açık doz yoksa "Bugün başka doz yok".
- Dokununca uygulama Bugün ekranında açılır (`EXTRA_DESTINATION = today`); widget'tan aksiyon yok.
- Pro değilse: "Widget Pro ile kullanılabilir…" metni, dokununca Pro ekranı.
- `WidgetUpdater.updateAll()` → `NextDoseWidget().updateAll(context)`; `DoseSync.run` her çalışmanın sonunda çağırır (Bölüm 5.1 adım 5). Dozun bildirimden, Bugün ekranından veya alarmdan işaretlenmesi DoseSync'i tetiklediği için widget güncellenir.
- Manifest'e `NextDoseWidgetReceiver` ve `res/xml/next_dose_widget_info.xml` (Glance varsayılan yükleme düzeni, yeniden boyutlandırılabilir, saatlik yenileme).
- Veriler Hilt `WidgetEntryPoint` ile okunur; Pro durumu widget sürecinde akış henüz yüklenmemiş olabileceği için doğrudan DataStore'dan (`ProStatus.current()`) okunur.

## Oluşturulan / değişen dosyalar
- `widget/NextDoseWidget.kt` (widget + receiver + entry point)
- `alarm/WidgetUpdater.kt` (artık gerçek güncelleme yapıyor), `billing/ProStatus.kt` (`current()`)
- `AndroidManifest.xml`, `res/xml/next_dose_widget_info.xml`, `res/values{,-tr}/strings.xml`

## Komut sonuçları
- assembleDebug: ❌ çalıştırılamadı (dl.google.com engeli)
- testDebugUnitTest: ❌ çalıştırılamadı; JVM doğrulama projesinde saf testler 39/39 ✅ (değişmedi)
- lintDebug: ❌ çalıştırılamadı

## Kabul kriterleri
- [ ] Widget eklenir — cihazda doğrulanmalı
- [ ] Bir sonraki dozu gösterir — cihazda doğrulanmalı
- [ ] Doz işaretlenince güncellenir — `DoseSync` sonunda `updateAll`; cihazda doğrulanmalı
- [ ] Pro değilken kilit mesajı gösterir — cihazda doğrulanmalı

## Verilen kararlar
- **Widget receiver `exported="true"`:** Bölüm 2.4 yalnızca `RescheduleReceiver`'ı exported istiyor; ancak widget sağlayıcısı da sistem (widget host) yayınlarını almak zorunda ve Glance dokümantasyonu `exported="true"` kullanıyor. Aynı gerekçeyle istisna yapıldı; alıcı yalnızca widget'ı yeniden çizer, dışarıdan tetiklenmesi zararsızdır.
- **"Sıradaki doz"** aktif profilin en erken açık (PENDING/SNOOZED) dozudur; gecikmiş bir doz varsa o gösterilir. Bugüne ait değilse "Bugün başka doz yok".
- **Saatlik `updatePeriodMillis`**: gece yarısından sonra özetin sıfırlanması gibi DoseSync dışı değişimler için; asıl güncelleme DoseSync'ten gelir.

## Çözülemeyen sorunlar
- Derleme/çalıştırma yok (ortam engeli).

## Kullanıcının elle doğrulaması gerekenler
- Ana ekrana widget ekleme (2×1 ve 3×2'ye büyütme), içerik ve dokunma davranışı.
- Bildirimden "Aldım" sonrası widget'ın güncellenmesi.
- `DEBUG_FORCE_PRO = false` iken kilit mesajı ve Pro ekranına yönlendirme.
- Koyu temada okunabilirlik.

## Önerilen commit mesajı
feat(widget): add Glance next-dose widget for Pro users
