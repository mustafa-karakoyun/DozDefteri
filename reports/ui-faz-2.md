## UI Faz 2 raporu — Bileşenler

- **Yapılanlar** (`ui/components/`):
  - `DoseCard` — 5 durum (Bekliyor, Alındı, Ertelendi, Atlandı, Kaçırıldı) plan 2.1 tablosundaki zemin/kenarlık/saat/çip/eylem kurallarıyla; Kaçırıldı'da açıklama kutusu + "Güncelle"; ⋮ menüsü duruma göre ("Alınmadı olarak işaretle", "Saati düzelt", "Aldım olarak işaretle", "Atlandı olarak işaretle", "Geri al"); durum değişiminde renk ve boyut animasyonu; tek birleşik TalkBack açıklaması, "Aldım" ayrı odaklanabilir.
  - `StatusChip` (5 durum + Stok azalıyor), `MedIcon` (40 dp, soluk varyant), `StockBar` (6 dp çubuk + metin, azalırken uyarı rengi ve ikonu), `DayProgressCard` (doz başına segment + "Sıradaki"), `InlineBanner` (Warning/Info/Error, eylem ve kapatma), `SectionHeader`, `ProfileAvatar` + `ProfileChip`, `ProBadge`, `PrnCard`, `LowStockCard`, `SettingsRow` (chevron / ✓ / switch / tonal buton / Pro rozeti), `DozExtendedFab`, `DozBottomBar` (seçili öğede pill gösterge), `ScreenHeader` (Headline + tarih + profil çipi), `DozTopBar` (← / ✕ + başlık + sağ alan), `PrimaryButton` / `TonalButton` / `OutlinedPillButton` / `TextAction`, düz `DozCard`.
  - `core/format/TurkishSuffix`: tasarımdaki Türkçe ekler kurala bağlandı — "08:04'te", "07:32'de", "20:10'a", "Mehmet'in", "5 dozdan 2'si", "136'sı". İngilizcede ek eklenmez.
  - Her bileşenin `@DozPreviews` (açık / koyu / %200) preview'ı var.
- **Tasarımdan sapmalar ve nedeni:**
  - Screenshot testleri (Roborazzi/Paparazzi) eklenmedi: ilk planın "listede olmayan bağımlılık ekleme" kuralı ve derleme ortamı olmaması. Preview'lar tanımlı; istersen ayrı iş olarak eklenebilir.
  - "Atlandı" segmenti ilerleme çubuğunda nötr (boş) renkte; plan atlanan için renk tanımlamıyor.
- **Doğrulanamayanlar / tahmini değerler:** Preview render'ları ve gerçek boyutlar (derleme yok). Çip iç boşluğu 8/12 dp, ilerleme çubuğu 8 dp yükseklik tahmini.
- **Testler:** JVM doğrulama projesinde `TurkishSuffixTest` 5/5 ✅ (toplam saf testler 44/44 ✅). `./gradlew lint testDebugUnitTest` çalıştırılamadı (dl.google.com engeli).
- **Önerilen commit mesajı:** `feat(ui): add design system components and dose card states`
