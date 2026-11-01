## UI Faz 8 raporu — Widget + cila

- **Yapılanlar:**
  - **Widget (Glance)**: 2×2 ve 4×2 boyutları. 2×2: "Ayşe · sıradaki", büyük saat "20:00", "Metformin +1", dolu "Aldım" (o saatteki tüm dozları alır). 4×2: "Ayşe · sıradaki · 20:00", o saatteki ilaçlar listesi, "Bugün 2/5 alındı", "Hepsini aldım". **Gün tamam** durumu: ✓ daire + "Bugünkü dozlar tamam" / "Yarın ilk doz 07:30". Pro değilse "Widget Pro ile kullanılabilir" + "Aç". 28 dp köşe, renkler tasarım token'larından (açık/koyu). Eylem `ActionCallback` ile; her doz değişiminde `updateAll`.
  - **Onboarding (ekran 1a/1b/1c, Figma'da var)**: 1a karşılama (sağ üstte "English"/"Türkçe", hap ikonu, üç güvence, sorumluluk reddi, "Başla"); 1b bildirim izni (örnek bildirim kartı, "Bildirimlere izin ver" / "Şimdi değil"; reddetmek suçlanmaz, Bugün'de bant kalır); 1c tam vaktinde & pil (izin durumu + "İzin ver", marka çipleri Xiaomi/Samsung/Huawei/Oppo/Diğer, 3 adımlı görsel rehber, "Pil ayarını aç", "İlk ilacını ekle" → doğrudan İlaç ekle ekranı). Sayfa noktaları.
  - **Profiller (ekran 7, Figma'da var)**: başlıkta Pro rozeti; kartlarda avatar harfi + ad + "Babam · 3 ilaç"; aktif profil 2 dp çerçeve + "✓ Aktif"; kalemle düzenleme; "+ Profil ekle"; satır içi "Yeni profil" formu (Ad, Not, harfli renk daireleri, "Profili kaydet"); ilk renk dışındakiler Pro ("Profil renkleri"); profil silme onaylı.
  - **Pil rehberi** aynı marka çipli adım bileşenine taşındı.
  - **Temizlik**: geçici renk takma adları, eski bileşenler ve kullanılmayan 127 metin silindi; kodda geçen her metin/çizim anahtarının tanımlı olduğu betikle doğrulandı; tüm satırlar ≤ 120 karakter.
- **§5 erişilebilirlik kontrol listesi:**
  - [x] Durum bilgisi her yerde ikon + metin (çipler, takvim, kartlar, sağlık kartı).
  - [x] Dokunma hedefleri ≥ 48 dp; "Aldım"/"Kaydet" 56 dp (Büyük yazıda 64 dp).
  - [x] TalkBack: doz kartları birleşik açıklama, takvim hücreleri tam cümle, ikon butonlarda açıklama, başlıklarda `heading()`, Pro tablosunda "yok".
  - [x] Büyük yazı switch'i: ×1.15 ölçek + 64 dp buton.
  - [x] Her ekran ve bileşen için açık / koyu / %200 preview tanımlı — render'ları doğrulanamadı.
  - [x] Kontrast (WCAG, metin ≥ 4.5:1) — token'lar değiştirilmeden ölçüldü, hepsi geçiyor:

| Mod | Çift | Oran | |
|---|---|---|---|
| light | text.primary / bg.app | 16.65:1 | ✅ |
| light | text.secondary / bg.app | 6.12:1 | ✅ |
| light | text.secondary / surface.container | 5.71:1 | ✅ |
| light | text.on-primary / primary | 6.61:1 | ✅ |
| light | primary.on-container / primary.container | 10.64:1 | ✅ |
| light | primary (text) / surface.default | 6.61:1 | ✅ |
| light | taken fg/bg | 7.94:1 | ✅ |
| light | skipped fg/bg | 6.45:1 | ✅ |
| light | snoozed fg/bg | 5.99:1 | ✅ |
| light | missed fg/bg | 5.76:1 | ✅ |
| light | pending fg / container-high | 5.19:1 | ✅ |
| light | warning fg/bg | 5.99:1 | ✅ |
| light | danger fg/bg | 5.31:1 | ✅ |
| light | pro fg/bg | 6.71:1 | ✅ |
| dark | text.primary / bg.app | 16.74:1 | ✅ |
| dark | text.secondary / bg.app | 9.64:1 | ✅ |
| dark | text.secondary / surface.default | 8.96:1 | ✅ |
| dark | text.secondary / surface.container | 8.17:1 | ✅ |
| dark | text.on-primary / primary | 9.02:1 | ✅ |
| dark | primary.on-container / primary.container | 9.02:1 | ✅ |
| dark | primary (text) / surface.default | 9.97:1 | ✅ |
| dark | taken fg/bg | 9.02:1 | ✅ |
| dark | skipped fg/bg | 9.18:1 | ✅ |
| dark | snoozed fg/bg | 10.06:1 | ✅ |
| dark | missed fg/bg | 9.30:1 | ✅ |
| dark | pending fg / container-high | 7.15:1 | ✅ |
| dark | warning fg/bg | 10.06:1 | ✅ |
| dark | danger fg/bg | 9.52:1 | ✅ |
| dark | pro fg/bg | 9.47:1 | ✅ |
- **Tasarımdan sapmalar ve nedeni:** Profil formuna isteğe bağlı "Not" alanı eklendi (kartlarda "Babam" gösteriliyor ama formda girecek yer yoktu). Dil düğmesi Android 13 altında gizli.
- **Doğrulanamayanlar:** Widget görünümü, onboarding akışı ve %200 yazıda düzen cihazda doğrulanmalı.
- **Testler:** JVM saf testleri 10 sınıf ✅. `./gradlew lint testDebugUnitTest` çalıştırılamadı (dl.google.com engeli).
- **Önerilen commit mesajı:** `feat(ui): add widget states, new onboarding and profiles screens, accessibility pass`
