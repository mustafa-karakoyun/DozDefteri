# Faz 4 — Bugün ve Geçmiş

## Yapılanlar
- **Bugün** (Bölüm 6.3): aktif profil adı başlıkta (birden fazla profil varsa renk noktası + açılır menüyle profil değiştirme); uyarı bantları (kesin alarm izni yok → "Hatırlatmalar gecikebilir" + "Düzelt"; bildirim kapalı → "Bildirimler kapalı" + "Aç"), her `ON_RESUME`'da yeniden okunur; bugünkü dozlar saate göre gruplu; satırda ad, doz, durum ikonu + metni; PENDING/SNOOZED'da "Aldım"/"Atla", uzun basınca "Ertele" (TalkBack için özel erişilebilirlik aksiyonu da var); geçmiş saatte bekleyen dozda "Gecikti"; TAKEN/SKIPPED'da 24 saat içinde "Geri al"; "Gerektiğinde" bölümü ve "Şimdi aldım"; düşük stok kartı (kalan gün tahminiyle); boş durumlar ("Henüz ilaç yok" + "İlaç ekle", "Bugün için doz yok"); FAB "İlaç ekle".
- Bugün ekranı Room `Flow`'larına bağlı: bildirimden yapılan işlem uygulamaya dönünce anında görünür. Dakikalık `minuteTicker` gecikme etiketini, geri alma penceresini ve gece yarısı gün değişimini günceller.
- **Geçmiş** (Bölüm 6.6): kendi yazılmış ay ızgarası (yerel haftanın ilk günüyle), ay seçici, ayın uyum yüzdesi (`AdherenceCalculator`, AS_NEEDED hariç; payda 0 → "—"), gün hücrelerinde ikon + renk (✓ yeşil tümü alındı, turuncu kısmi, kırmızı kaçırılan var, boş), bugün çerçeveli, açıklamalı lejant; güne dokununca `ModalBottomSheet`'te o günün dozları ve durumları. Hücreler TalkBack'te "tarih, durum" olarak okunur.
- `DayStatusCalculator` (saf Kotlin) + `DayStatusCalculatorTest`.
- Debug test dozu butonu Bugün'den kaldırıldı; Ayarlar'da yalnızca debug'da görünen "Developer" bölümüne taşındı (Ayarlar ekranının geri kalanı Faz 6'da).
- Enstrümantasyon: `TodayContentTest` ("Aldım" sonrası durum değişimi ve "Geri al").

## Oluşturulan / değişen dosyalar
- `ui/today/{TodayViewModel,TodayScreen}.kt`
- `ui/history/{HistoryViewModel,HistoryScreen}.kt`
- `ui/settings/{SettingsScreen,DeveloperSection,DebugViewModel}.kt` (DebugViewModel `ui/today`'den taşındı)
- `ui/components/{StatusVisuals,WarningBanner}.kt`, `core/time/Ticker.kt`, `domain/DayStatus.kt`
- `ui/navigation/AppNavHost.kt`, `res/values{,-tr}/strings.xml`
- `test/.../domain/DayStatusCalculatorTest.kt`, `androidTest/.../ui/TodayContentTest.kt`

## Komut sonuçları
- assembleDebug: ❌ çalıştırılamadı (dl.google.com engeli)
- testDebugUnitTest: ❌ çalıştırılamadı; JVM doğrulama projesinde saf testler 30/30 ✅
- lintDebug: ❌ çalıştırılamadı

## Kabul kriterleri
- [ ] Bugün ekranı bildirimden yapılan değişikliği anında gösterir — `Flow` tabanlı; cihazda doğrulanmalı
- [x] Geçmiş takvim renkleri ve uyum yüzdesi testlerdeki hesaplamayla tutarlı — ekran doğrudan `AdherenceCalculator` ve `DayStatusCalculator`'ı kullanıyor; ikisi de JVM'de testli

## Verilen kararlar
- **Gün durumu kuralları:** kaçırılan varsa kırmızı; hepsi alındıysa yeşil; yalnızca bekleyen (gelecek) dozlar varsa boş; diğer her karışım (atlanan, alındı + bekleyen) turuncu "kısmi". AS_NEEDED kayıtları gün rengine ve yüzdeye girmez, ama alt sayfada listelenir.
- **Gruplama** dozun güncel `scheduledAt` saatine göre (ertelenen doz yeni saatinde görünür); hangi güne ait olduğu `originalScheduledAt` ile belirlenir.
- **Erteleme** satıra uzun basınca doğrudan Ayarlar'daki süreyle yapılır (ek menü yok); satırda "Ertelemek için uzun bas" ipucu gösterilir.
- **Gerektiğinde ilaçları** "Şimdi aldım" ile alındığında o anki saatte TAKEN satırı olarak listeye de düşer (geri alınabilir).
- **Ay başlığı** bağımsız ay adı (`LLLL yyyy`) ile, ilk harf büyük.

## Çözülemeyen sorunlar
- Derleme/çalıştırma yok (ortam engeli).

## Kullanıcının elle doğrulaması gerekenler
- Bildirimden "Aldım" deyip uygulamaya dönünce Bugün'de durumun "Alındı" olması.
- Kesin alarm iznini kapatınca Bugün'de uyarı bandı, "Düzelt" ile ayar ekranının açılması.
- Geçmiş ekranında renklerin ve ay yüzdesinin elle sayımla tutarlılığı; TalkBack ile takvim hücrelerinin okunuşu.

## Önerilen commit mesajı
feat(ui): add today screen with dose actions and history calendar
