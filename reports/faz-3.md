# Faz 3 — Onboarding, ilaç ekleme, İlaçlar ekranı

## Yapılanlar
- **Onboarding** (`HorizontalPager`, 3 sayfa): değer sayfası (3 madde + sorumluluk reddi küçük yazı), bildirim izni (Android 13+ `POST_NOTIFICATIONS`; reddedilirse "Ayarları aç" + "Şimdilik geç"; 13 altında sayfa listeden çıkarılır), tam zamanlı hatırlatma (`ACTION_REQUEST_SCHEDULE_EXACT_ALARM` + `package:` URI, izin varsa ✅, pil rehberi linki, "Başla" → `onboarding_done = true` → Bugün).
- İzin durumu her `ON_RESUME`'da yeniden okunur (`rememberPermissionSnapshot`), sistem ayar ekranlarına geçişler `SystemSettings` içinde, bulunamazsa uygulama detay sayfasına düşer.
- **İlaç ekle/düzenle** (Bölüm 6.4): tüm alanlar ve doğrulamalar (`MedicationFormValidator`, saf Kotlin), birim öneri çipleri, form açılır menüsü, plan türü radyo listesi, saat çipleri + `TimePicker`, gün çipleri, 2–30 aralık seçici, başlangıç/bitiş `DatePicker`, stok switch'i + miktar/eşik, not (sayaçlı). Kaydet geçersiz formda pasif; kaydet → `MedicationRepository.save` (içinde `regenerateForSchedule`) → `DoseSync.run("edit")` → geri dön + Snackbar "Kaydedildi". Düzenlemede menüde "Arşivle" (onay diyaloğu), gösterilen bildirimleri iptal eder.
- **İlaçlar** (Bölüm 6.5): Aktif/Arşiv sekmeleri, satırda ad, doz, plan özeti ("Her gün · 08:00, 20:00"), stok rozeti (ikon + metin, düşükse uyarı ikonu, kalan gün tahmini), arşivde "Geri yükle" (+ DoseSync), boş durumlar, FAB.
- `MainViewModel` + `MainActivity`: başlangıç hedefi (`onboarding_done`), tema ayarı, bildirim/widget'tan gelen hedef (`EXTRA_DESTINATION`) yönlendirmesi.
- Ortak bileşenler: `EmptyState`, `ConfirmDialog`, `TimeSelectDialog`, `DateSelectDialog`, `ScheduleFormatter` (plan özeti, form etiketleri), `Formats`.
- TR/EN tüm metinler; preview'lar `fontScale = 2f` ile de tanımlı.
- Testler: `MedicationFormValidatorTest` (10, JVM), `MedicationEditContentTest` (Compose UI, enstrümantasyon).

## Oluşturulan / değişen dosyalar
- `ui/onboarding/{OnboardingScreen,OnboardingViewModel}.kt`
- `ui/medications/{MedicationForm,MedicationEditViewModel,MedicationEditScreen,MedicationsViewModel,MedicationsScreen}.kt`
- `ui/components/{Permissions,Pickers,ConfirmDialog,EmptyState}.kt`, `core/format/ScheduleFormatter.kt`
- `ui/MainViewModel.kt`, `MainActivity.kt`, `ui/navigation/AppNavHost.kt`
- `res/values{,-tr}/strings.xml`
- `test/.../ui/medications/MedicationFormValidatorTest.kt`, `androidTest/.../ui/MedicationEditContentTest.kt`

## Komut sonuçları
- assembleDebug: ❌ çalıştırılamadı (dl.google.com engeli)
- testDebugUnitTest: ❌ çalıştırılamadı; JVM doğrulama projesinde saf testler 26/26 ✅ (DoseGenerator 12, Adherence 4, FormValidator 10)
- lintDebug: ❌ çalıştırılamadı
- Kaynak kontrolü: tüm XML dosyaları ayrıştırıldı, EN ve TR string anahtarları birebir eşleşiyor.

## Kabul kriterleri
- [x] Bölüm 6.4 doğrulama kurallarının tamamı — `MedicationFormValidatorTest` ile JVM'de doğrulandı
- [ ] Kayıt sonrası dozlar üretilir ve alarm kurulur — kod yolu hazır (save → regenerate → DoseSync), cihazda doğrulanmalı
- [ ] Arşivleme gelecekteki dozları kaldırır — repository testi var (`archive_removesFutureOpenEvents`), Robolectric çalıştırılamadı

## Verilen kararlar
- **Hata metinleri:** Ad ve birim hataları alan bir kez değiştirildikten sonra gösterilir (boş formda kırmızı alan görünmesin); diğer hatalar anında gösterilir. Kaydet butonu her durumda geçersiz formda pasif.
- **Yeni ilaç varsayılanları:** doz "1", plan "Her gün", saat 08:00, başlangıç bugün.
- **Uyarı eşiği isteğe bağlı:** boş bırakılırsa `lowStockThreshold = null` (düşük stok bildirimi yok). Plan eşiğin zorunlu olup olmadığını belirtmiyor; en az kısıtlayıcı seçenek seçildi.
- **Ondalık ayırıcı:** hem "." hem "," kabul edilir (TR klavye).
- **Form açılır menüsü** `ExposedDropdownMenuBox` yerine `OutlinedButton + DropdownMenu` ile yapıldı: `menuAnchor` API'si Material3 1.3–1.4 arasında değişti; derleme doğrulanamadığı için kararlı API seçildi.
- **Plan türü seçimi** segment yerine radyo listesi: 4 uzun seçenek `fontScale 2.0`'da segmentte kesilirdi.
- **Başlangıç hedefi** NavHost ömrü boyunca sabit tutulur; onboarding bitince geri yığını temizlenip Bugün'e geçilir. Sekme geçişleri Bugün'e kadar pop eder.
- **Pil rehberi, Dışa aktar, Pro** route'ları bu fazda placeholder; ilgili fazlarda doldurulur.

## Çözülemeyen sorunlar
- Derleme/çalıştırma yok (ortam engeli).

## Kullanıcının elle doğrulaması gerekenler
- İlk açılışta onboarding'in 3 sayfası (Android 12 cihazda 2 sayfa), izin reddi ve "Ayarları aç" akışı.
- İlaç ekleme formunda her hatanın görünmesi, kaydedince İlaçlar listesinde görünmesi ve planlı saatte bildirim gelmesi.
- Arşivle → bildirimlerin gelmemesi; Arşiv sekmesinden geri yükleme.

## Önerilen commit mesajı
feat(ui): add onboarding, medication editor and medication list
