## UI Faz 1 raporu — Tema

- **Yapılanlar:**
  - `ui/theme/Palette.kt`: Figma Primitives (Teal, Sand, Amber, Slate, Clay, Red, Violet) birebir.
  - `ui/theme/Color.kt`: semantik token'lar `DozColors` (+ `ColorPair`) açık/koyu (`LightDozColors`, `DarkDozColors`), `LocalDozColors`; Material `ColorScheme` token'lardan kuruluyor (`toColorScheme`); 8 profil rengi `ProfilePalette` (ad yerine sıra: blue, orange, green, pink, sky, vermillion, purple, yellow + `-on` metin renkleri).
  - `ui/theme/Type.kt`: Figma metin stilleri `DozTextStyles` (Display…Caption, Numeric/Time ve kart saati `tnum` ile) + Material `Typography` eşlemesi.
  - `ui/theme/Shape.kt`: `Space`, `Radius`, `Touch` (+ `LocalTouch`), `DozShapes`, Material `Shapes`.
  - `ui/theme/Theme.kt`: `DozDefteriTheme(themeMode, largeText)`; dinamik renk kapalı; "Büyük yazı" açıkken font ölçeği ×1.15 ve birincil buton yüksekliği 64 dp. `Doz.colors / Doz.type / Doz.touch` kısayolları.
  - `ui/theme/DozIcons.kt`: form, durum ve gün bölümü ikon eşlemesi; Material setinde olmayan tablet, kapsül ve krem tüpü 24 dp vektör olarak çizildi.
  - `ui/theme/TokenPreview.kt` + `@DozPreviews` (açık / koyu / %200) çoklu preview anotasyonu.
  - Eski vurgu rengi (accent) sistemi kaldırıldı; `SettingsRepository`'ye `large_text`, `backup_reminder_enabled`, `last_backup_at`, `last_backup_location` eklendi; "işaretlenmedi" varsayılanı 60 dk, seçenekler 30/60/120/180/360 dk.
  - Pencere arka planı ve uygulama ikonu zemini yeni palete (`#FCFAF6` / `#141210`, teal/600) çekildi.
- **Tasarımdan sapmalar ve nedeni:**
  - İkonlar Material Symbols yerine Material Icons Extended (Rounded) — projede zaten olan kütüphane; eksik üç şekil elle çizildi.
  - Geçiş süresince eski ekranlar için `LegacyTokens.kt` geçici takma adlar içeriyor; ekranlar yenilenince silinecek.
- **Doğrulanamayanlar / tahmini değerler:** Token preview'ları çalıştırılamadı (derleme ortamı yok). Kontrast ölçümü Faz 8'de.
- **Testler:** `./gradlew lint testDebugUnitTest` çalıştırılamadı (dl.google.com engeli sürüyor). Saf Kotlin testleri JVM doğrulama projesinde koşturuluyor (bkz. sonraki fazlar).
- **Önerilen commit mesajı:** `feat(ui): add Figma design tokens, typography and theme`
