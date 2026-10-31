## UI Faz 5 raporu — İlaçlarım + Geçmiş

- **Yapılanlar:**
  - **İlaçlarım** (ekran 4): başlık + profil çipi; "Aktif (5)" / "Arşiv (2)" sekmeleri; kartta `MedIcon`, ad, "500 mg · Her gün 08:00, 20:00" (PRN için "500 mg · Gerektiğinde"), chevron, altında `StockBar` ("38 tablet · ~19 gün yeter", azalıyorsa uyarı rengi ve ikonu). Stok yoksa "ⓘ Stok girilmemiş · ekle" → stok alt sayfası (stok takibini 7 gün uyarı eşiğiyle başlatır). PRN'de gün tahmini yok. Stok çubuğuna dokununca "Stok ekle". Arşiv kartında "Geri al". FAB "İlaç ekle".
  - **Geçmiş** (ekran 5): başlık + profil çipi; "‹ Ekim 2026 ›"; özet kartı "Bu ay uyum: %92" + "148 planlı dozun 136'sı alındı · 5 atlandı · 7 işaretlenmedi" (uyum = alındı / planlı); Pazartesi başlangıçlı takvim, hücrede gün + ikon (✓ tamamı alındı / yarım daire kısmi / ünlem işaretlenmedi / "–" doz yok veya gelecek), seçili gün 2 dp primary çerçeve; lejant; gün detay kartı ("14 Ekim, Çarşamba"): saat, ilaç adı, durum çipi + gerçekleşen saat ("07:41'de" / "İşaretlenmedi"), kalem → saat/durum düzenleme alt sayfası (alındı saati veya Atlandı). Hücre TalkBack açıklaması: "14 Ekim, 5 dozdan 4'ü alındı, 1 işaretlenmedi".
  - `HistoryStats` (saf Kotlin, birim testli): ay ve gün sayımları, uyum yüzdesi; gün renkleri mevcut `DayStatusCalculator` ile.
  - `MedicationRepository.startStockTracking`.
- **Tasarımdan sapmalar ve nedeni:**
  - Gün detayı plan metnine göre takvimin altında kart olarak (alt sayfa değil); ekran görüntüsü de böyle.
  - Bugünden sonraki günler her zaman "–" (tasarım: "Doz yok / gelecek").
- **Doğrulanamayanlar / tahmini değerler:** Takvim hücre oranı (0.85) ve renkleri cihazda kontrol edilmeli.
- **Testler:** JVM doğrulama projesinde `HistoryStatsTest` 3/3 (tasarımdaki 136/148 → %92 dahil) ✅, toplam 10 sınıf geçti. `./gradlew lint testDebugUnitTest` çalıştırılamadı (dl.google.com engeli).
- **Önerilen commit mesajı:** `feat(ui): rebuild medications list and history calendar`
