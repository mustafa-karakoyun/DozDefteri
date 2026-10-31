## UI Faz 3 raporu — İskelet

- **Yapılanlar:**
  - `AppNavHost`: yeni `DozBottomBar` (Bugün · İlaçlarım · Geçmiş · Ayarlar; ev, kapsül, takvim, kaydırıcı ikonları; seçili öğede pill gösterge), uygulama zemini `bg.app`.
  - `ShellViewModel`: aktif profil ve profil geçiş satırları ("Sıradaki: 20:00 · 2 doz", "Bugünkü dozlar tamam").
  - `ProfileSwitcherSheet` (ekran 7b, "Kimin ilaçları?"): avatar + ad + sıradaki doz, aktif satır `primary.container` zemin ve ✓, "Profilleri yönet". Geçişten sonra Snackbar "Mehmet'in ilaçları gösteriliyor" (Türkçe iyelik eki kurala göre).
  - `LocalHeaderProfile`: aktif profil çipi her ana ekran başlığına nav host'tan sağlanıyor; çipe dokununca alt sayfa açılıyor.
  - Sekme etiketi "İlaçlarım" / "My medications" oldu.
  - Geri davranışı: sekmeler Bugün'e kadar pop eder, durum kaydedilip geri yüklenir (önceki fazdan).
- **Tasarımdan sapmalar ve nedeni:** Ana ekranların başlıkları bir sonraki fazlarda yeni `ScreenHeader` ile değiştirilecek; bu fazda iskelet ve sayfa bağlandı.
- **Doğrulanamayanlar / tahmini değerler:** Sekme geçişi ve geri davranışı cihazda doğrulanmalı.
- **Testler:** `./gradlew lint testDebugUnitTest` çalıştırılamadı (dl.google.com engeli). JVM saf testleri değişmedi (44/44 ✅).
- **Önerilen commit mesajı:** `feat(ui): add app shell with new bottom bar and profile switcher`
