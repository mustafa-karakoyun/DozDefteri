# DozDefteri — UI Uygulama Planı (Figma → Jetpack Compose)

> Bu dosya Claude Code için yazıldı. Kaynak: Figma dosyası **"DozDefteri — Android UI"**
> (`https://www.figma.com/design/ioGvgI4mGuVBY5g8FoVIK9/DozDefteri-%E2%80%94-Android-UI`).
> Planı fazlar halinde, kullanıcıya soru sormadan uygula; her fazın sonunda kısa bir **faz raporu** yaz.

---

## 0. Çalışma kuralları

1. **Commit atma.** Kullanıcı commit'leri kendisi atar. Her faz sonunda, faz raporunda önerilen commit mesaj(lar)ını ver.
2. **Önce repoyu oku.** Mevcut proje varsa (Room varlıkları, repository'ler, alarm/WorkManager altyapısı, navigation) onlara uy; bu plandaki adları mevcut modellere eşle, ikinci bir veri katmanı yaratma. Proje yoksa Faz 1'de iskeleti kur.
3. **Yığın:** Kotlin, Jetpack Compose, Material 3, Navigation Compose, Hilt (projede başka DI varsa onu kullan), Room, Glance (widget), NotificationCompat. minSdk 26, targetSdk güncel.
4. **Tüm metinler `strings.xml`'de**, Türkçe; çoğul ve sayı biçimleri `plurals`/`getString(…, args)` ile. Bu plandaki tırnak içi metinler tasarımdaki **birebir** metinlerdir — değiştirme.
5. **§1'deki token değerleri Figma Variables'tan birebir alındı** (Primitives 43, Color 74, Dimension 15 değişken + metin stilleri). Token adlarını Kotlin'de de aynı hiyerarşiyle koru; ekranda başka bir hex görürsen token'ı esas al.
6. Uygulama **tamamen çevrimdışı**; hesap yok, ağ izni yok.
7. Her bileşen ve ekran için `@Preview` yaz: açık tema, koyu tema, `fontScale = 2f`.

---

## 1. Tasarım tokenları

### 1.1 Primitives (`ui/theme/Palette.kt`)

```kotlin
object Teal   { val c50 = Color(0xFFE7F3F0); val c100 = Color(0xFFC6E5DE); val c200 = Color(0xFF93D0C4)
                val c300 = Color(0xFF5FB5A6); val c500 = Color(0xFF24806F); val c600 = Color(0xFF17685D)
                val c700 = Color(0xFF115249); val c800 = Color(0xFF0C3D36); val c900 = Color(0xFF072823) }
object Sand   { val c0 = Color(0xFFFFFFFF); val c25 = Color(0xFFFCFAF6); val c50 = Color(0xFFF6F2EA)
                val c100 = Color(0xFFEDE7DC); val c200 = Color(0xFFDCD3C5); val c300 = Color(0xFFC3B9A9)
                val c400 = Color(0xFFA39988); val c500 = Color(0xFF857B6D); val c600 = Color(0xFF665E53)
                val c700 = Color(0xFF4B453D); val c800 = Color(0xFF302C27); val c850 = Color(0xFF25221E)
                val c900 = Color(0xFF1C1A17); val c950 = Color(0xFF141210) }
object Amber  { val c100 = Color(0xFFFBEBC7); val c200 = Color(0xFFF5D58E); val c700 = Color(0xFF7A5000); val c900 = Color(0xFF3A2700) }
object Slate  { val c100 = Color(0xFFE4E8EE); val c200 = Color(0xFFC6CFDB); val c700 = Color(0xFF465263); val c900 = Color(0xFF232A35) }
object Clay   { val c100 = Color(0xFFF7E4DA); val c200 = Color(0xFFEEC3AD); val c700 = Color(0xFF87472A); val c900 = Color(0xFF3D1F12) }
object Red    { val c100 = Color(0xFFFBE2DF); val c300 = Color(0xFFF2B8B5); val c700 = Color(0xFFB3261E); val c900 = Color(0xFF410E0B) }
object Violet { val c100 = Color(0xFFECE4F7); val c200 = Color(0xFFD3C2EE); val c700 = Color(0xFF5B3F8C); val c900 = Color(0xFF2A1C42) }
```

### 1.2 Semantik renkler (Figma `Color` koleksiyonu: `light/…` ve `dark/…`)

| Token | Açık | Koyu | Kullanım |
|---|---|---|---|
| `bg.app` | sand/25 | sand/950 | Uygulama zemini |
| `surface.default` | sand/0 | sand/900 | Bekleyen/aktif kartlar, form alanları |
| `surface.container` | sand/50 | sand/850 | Tamamlanmış (Alındı/Atlandı) doz kartları — kenarlıksız |
| `surface.container-high` | sand/100 | sand/800 | "Bekliyor" çipi zemini, ilerleme çubuğu boş kısmı, tamamlanan kart ikon dairesi |
| `surface.inverse` | sand/900 | sand/50 | Snackbar |
| `text.primary` | sand/900 | sand/50 | |
| `text.secondary` | sand/600 | sand/300 | Doz satırı, alt yazılar |
| `text.on-primary` | sand/0 | teal/900 | Dolu buton metni |
| `text.inverse` | sand/50 | sand/900 | Snackbar metni |
| `primary.default` | teal/600 | teal/200 | "Aldım", "Kaydet", seçili durum, alındı ilerlemesi |
| `primary.container` | teal/50 | teal/800 | İlaç ikon dairesi, tonal butonlar ("Güncelle", "Stok ekle", "Şimdi al"), bilgi kutuları, FAB, seçili çip |
| `primary.on-container` | teal/800 | teal/100 | |
| `border.default` | sand/200 | sand/700 | Kart / alan kenarlığı (1 dp) |
| `border.strong` | sand/400 | sand/500 | Kesikli "+ Başka saat", odaklı alan |
| `status.taken.bg / fg` | teal/50 / teal/700 | teal/800 / teal/100 | "Alındı" |
| `status.skipped.bg / fg` | slate/100 / slate/700 | slate/900 / slate/200 | "Atlandı" |
| `status.snoozed.bg / fg` | amber/100 / amber/700 | amber/900 / amber/200 | "Ertelendi" |
| `status.missed.bg / fg` | clay/100 / clay/700 | clay/900 / clay/200 | "Kaçırıldı" çipi, kart kenarlığı (fg), kart içi açıklama kutusu (bg) |
| `status.pending.fg` | sand/600 | sand/300 | "Bekliyor" (zemin `surface.container-high`) |
| `warning.bg / fg` | amber/100 / amber/700 | amber/900 / amber/200 | İzin banner'ı, stok azalıyor kartı/metni, Hatırlatma sağlığı "1 sorun" |
| `danger.bg / fg` | red/100 / red/700 | red/900 / red/300 | Yıkıcı işlemler (geri yükleme onayı, arşivleme), form hataları |
| `pro.bg / fg` | violet/100 / violet/700 | violet/900 / violet/200 | "Pro" rozeti |

**Profil renkleri** (mod bağımsız, ad + `-on` metin rengi): blue `#1F5FA8`/beyaz · orange `#E69F00`/`#1C1A17` · green `#00805C`/beyaz · pink `#B4508E`/beyaz · sky `#56B4E9`/`#1C1A17` · vermillion `#C24E00`/beyaz · purple `#6A4C93`/beyaz · yellow `#E8D44D`/`#1C1A17`. İlk profil blue; diğer renkler Pro ("Profil renkleri").

Uygulama: Material `ColorScheme`'i bu tokenlardan kur (primary, onPrimary, primaryContainer, onPrimaryContainer, background, surface, surfaceContainer, surfaceContainerHigh, outline = border.default, outlineVariant, error = danger.fg, errorContainer = danger.bg, inverseSurface…). Durum/uyarı/pro/profil renkleri için `DozColors` `data class` + `LocalDozColors`. **Koyu tema Figma'da tasarlanmış** (02 sayfasında "Board / Koyu tema"); yukarıdaki koyu değerleri kullan. Dinamik renk (Material You) **kapalı**. Varsayılan "Sistem ayarını izle".

Not: Kaçırıldı (clay) ile danger (red) ayrı tonlar — kaçırılan doz suçlayıcı kırmızıyla değil, toprak tonuyla gösterilir; birleştirme.

### 1.3 Tipografi (Figma metin stilleri)

| Figma stili | Boyut / satır yüksekliği | Kullanım |
|---|---|---|
| Display | 36 / 44 | Pro başlığı "Bir kez öde, hep senin.", widget saati |
| Headline | 28 / 36 | Ekran başlıkları "Bugün", "İlaçlarım", "Geçmiş", "Ayarlar" |
| Title / Large | 22 / 28 | Üst bar başlıkları, "Bu ay uyum: %92", bölüm kart başlıkları |
| Title / Medium | 18 / 24 | İlaç adı, "5 dozdan 2'si alındı" |
| Body / Large | 18 / 26 | Form alanları, özet cümle |
| Body / Medium | 16 / 24 | Doz satırı ("500 mg · 1 tablet · tok karnına"), açıklamalar |
| Label / Large | 16 / 20 | Butonlar ("Aldım", "Kaydet") |
| Label / Medium | 14 / 20 | Çipler, sekmeler, bottom nav, bölüm başlıkları |
| Caption | 13 / 18 | Alt yazılar, legend, dipnotlar |
| Numeric / Time | 24 / 28 | Büyük saat gösterimleri (widget 4×2, saat seçici); **tabular rakam** (`fontFeatureSettings = "tnum"`) |

Font: Roboto (sistem). Ağırlık: Display/Headline/Title/Label = Medium (500), Body/Caption = Regular (400). Kart içindeki doz saati ("08:00", "08:04'te") Body/Medium + Medium ağırlık + `tnum`. Bunları `Typography` + `DozTextStyles` olarak tanımla.

Hiçbir yerde `maxLines = 1` + ellipsis ile ilaç adı kesme; metin satır kırsın.

### 1.4 Ölçüler (Figma `Dimension` koleksiyonu)

```kotlin
object Space  { val s4 = 4.dp; val s8 = 8.dp; val s12 = 12.dp; val s16 = 16.dp; val s20 = 20.dp; val s24 = 24.dp; val s32 = 32.dp; val s48 = 48.dp }
object Radius { val sm = 8.dp; val md = 12.dp; val lg = 16.dp; val xl = 28.dp; val full = 999.dp }
object Touch  { val min = 48.dp; val primary = 56.dp }
```

- Kart: `Radius.lg` (16); form alanı / radyo kartı: `Radius.md` (12); küçük öğeler (takvim hücresi, dosya yolu kutusu): `Radius.sm` (8); widget ve alt sayfa üst köşeleri: `Radius.xl` (28); buton ve çipler: `Radius.full`.
- "Aldım", "Kaydet" ve diğer birincil eylemler `Touch.primary` (56 dp) yükseklik; diğer tüm dokunma hedefleri ≥ `Touch.min` (48 dp).
- Ekran kenar boşluğu `s16`; kartlar arası `s12`; bölümler arası `s24`; kart iç boşluğu `s16`.
- İlaç ikon dairesi 40 dp (`primary.container` zemin, 20 dp ikon); tamamlanan kartta `surface.container-high` zemin + `text.secondary` ikon.
- Durum çipi: 28 dp yükseklik, 16 dp ikon, yatay `s8`–`s12` iç boşluk.
- Gölge: Figma'da tek bir "Elevation" efekt stili var; yalnızca FAB ve alt sayfada kullan, kartlar düz (kenarlık ile) kalır.

### 1.5 İkonlar

Material Symbols (Rounded). İlaç formuna göre: Tablet → `do_not_disturb`-benzeri bölünmüş daire (tablet), Kapsül → `pill`, Şurup → şişe, Damla → `water_drop`, İğne → `syringe`, Sprey → sprey şişesi, Krem → tüp. Bölüm ikonları: Sabah → gün doğumu, Öğle → güneş, Akşam → ay. Durumlar: Bekliyor → boş daire, Alındı → ✓, Ertelendi → saat, Atlandı → ileri-dönen ok (`redo`), Kaçırıldı → daire içinde ünlem.

---

## 2. Ortak bileşenler (`ui/components/`)

### 2.1 `DoseCard` — 5 durum (tasarımın çekirdeği)

Ortak düzen: sol 40 dp ikon dairesi · orta sütun (ilaç adı / doz satırı / saat + durum çipi) · sağ eylem.

| Durum | Zemin | Saat satırı | Çip | Sağ eylem | Ek |
|---|---|---|---|---|---|
| **Bekliyor** | `surface.default` + `border.default` kenarlık | "08:00" | "Bekliyor" (zemin `surface.container-high`, metin `status.pending.fg`) | Dolu "Aldım" butonu | |
| **Alındı** | `surface.container`, kenarlıksız | "08:04'te" | "Alındı" ✓ (`status.taken`) | ⋮ menü | |
| **Ertelendi** | `surface.default` + `border.default` | "08:00 → 08:10" | "Ertelendi" saat ikonu (`status.snoozed`), saatin **altında ayrı satır** | Dolu "Aldım" | |
| **Atlandı** | `surface.container`, kenarlıksız | "08:00" | "Atlandı" (`status.skipped`) | ⋮ menü | |
| **Kaçırıldı** | `surface.default` + **`status.missed.fg` kenarlık** | "08:00" | "Kaçırıldı" ünlem (`status.missed`) | ⋮ menü | Kart altında `status.missed.bg` kutu: "Bu dozu işaretlemedin. Aldıysan güncelleyebilirsin." + tonal "Güncelle" |

- ⋮ menü: "Alınmadı olarak işaretle / Saati düzelt / Geri al" (mevcut iş mantığına göre).
- "Güncelle" → saat seçicili alt sayfa (alındı zamanını gir) veya "Atlandı" seçeneği.
- "Aldım"a basınca kart animasyonla Alındı durumuna geçer; Snackbar: "Metformin alındı · Geri al".
- Durum **asla yalnızca renkle** anlatılmaz: her durumda ikon + metin var.
- Erişilebilirlik: kart tek `semantics(mergeDescendants)` — "Metformin, 500 miligram, 1 tablet, tok karnına, saat 08:00, bekliyor". "Aldım" ayrı odaklanabilir eylem.

### 2.2 Diğer bileşenler

- `StatusChip(status)` — yukarıdaki 5 durum + `LowStock` varyantı.
- `MedIcon(form, muted)` — 40 dp daire.
- `StockBar(current, total, daysLeft, low)` — 6 dp yuvarlak çubuk; dolu kısım normalde `primary.default`, azalıyorsa `warning.fg`; boş kısım `surface.container-high`. Metin: "38 tablet · ~19 gün yeter"; azalıyorsa önünde uyarı ikonu, `warning.fg` rengi.
- `DayProgressCard(taken, total, segments, next)` — "5 dozdan 2'si alındı"; doz başına bir segment (alındı `primary.default`, ertelendi `status.snoozed.fg`, kaçırıldı `status.missed.fg`, bekliyor `surface.container-high`); altında "Sıradaki: D vitamini, 13:10 (ertelendi)".
- `InlineBanner(kind = Warning|Info|Error, text, actionLabel, onAction, onDismiss)` — ör. "Hatırlatmalar birkaç dakika gecikebilir." + "Düzelt".
- `SectionHeader(icon, text)` — "Sabah", "Öğle", "Akşam", "Gerektiğinde".
- `ProfileChip(name, color)` — top bar sağında avatar harfi + ad + ▾; profil değiştirme menüsü açar. Aktif profil **her ana ekranda** görünür.
- `PrnCard` — "Parasetamol / 500 mg · son: dün 22:10" + tonal "Şimdi al".
- `LowStockCard` — `warning.bg` kart: "Atorvastatin azalıyor", `StockBar`, tonal "Stok ekle".
- `SettingsRow(icon?, title, subtitle, trailing)` — trailing: chevron / ✓ / switch / tonal buton / Pro rozeti.
- `DozExtendedFab("İlaç ekle")` — `primary.container` zeminli, Elevation gölgesi, sağ alt.
- `DozBottomBar` — Bugün · İlaçlarım · Geçmiş · Ayarlar; seçili öğede ikon arkasında pill gösterge.

---

## 3. Ekranlar

### 2 · Bugün (`today`)
**Amaç (UX gerekçesi):** Uygulama nadiren açılır; açıldığında tek soruyu yanıtlar: "şimdi ne almalıyım?".

Yukarıdan aşağı:
1. Başlık "Bugün", altında tarih ("Pazar, 1 Kasım" — `Locale("tr")`), sağda `ProfileChip`.
2. İzin sorunu varsa `InlineBanner(Warning)`: "Hatırlatmalar birkaç dakika gecikebilir." + "Düzelt" (tam alarm iznine yönlendirir). Kapatılabilir; sorun çözülünce kaybolur.
3. `DayProgressCard`.
4. Saat dilimine göre gruplu `DoseCard` listeleri: **Sabah** (< 12:00), **Öğle** (12:00–17:00), **Akşam** (≥ 17:00). Boş grup gösterilmez. Grup içi saat sırası.
5. Stok azalan ilaç varsa ilgili grubun sonunda `LowStockCard`.
6. **Gerektiğinde** bölümü: `PrnCard`'lar.
7. `DozExtendedFab` → İlaç ekle.

Boş durum (hiç ilaç yok): ortada açıklama + "İlaç ekle" butonu. Gün tamamlandığında üstte "Bugünkü dozlar tamam".

### 3 · İlaç ekle (`medication/edit?id=`)
**Amaç:** Sihirbaz yerine tek ekran + katlanır bölümler; 65+ kullanıcı nerede olduğunu görür, geri gitmesi gerekmez.

- Üst bar: X (kapat; değişiklik varsa "Değişiklikler kaydedilmesin mi?" diyaloğu), "İlaç ekle", sağda metin buton "Kaydet".
- **İlaç adı** (otomatik büyük harf, zorunlu).
- **Miktar** (sayısal) + **Birim** dropdown (mg, mcg, g, ml, IU, damla, …) yan yana.
- **Form**: tek seçimli ikonlu çipler — Tablet, Kapsül, Şurup, Damla, İğne, Sprey, Krem (FlowRow; seçili = `primary.container` + `primary.default` kenarlık).
- **Ne sıklıkla?**: radyo kartları (başlık + açıklama):
  - "Her gün" / "Günde bir veya birkaç kez"
  - "Haftanın belirli günleri" / "Ör. Pzt, Çar, Cum" → seçilince gün çipleri açılır
  - "Birkaç günde bir" / "Ör. 3 günde bir" → seçilince aralık sayısı alanı açılır
  - "Gerektiğinde" / "Hatırlatma yok, istediğinde işaretlersin" → saat bölümü gizlenir
- **Hangi saatlerde?**: saat çipleri (seçili olanlarda ✓), varsayılan öneriler 08:00 / 13:00 / 20:00, kesikli kenarlı "+ Başka saat" → TimePicker.
- **Özet kutusu** (`primary.container`, info ikonu): form durumundan canlı üretilen cümle — ör. "Her gün 08:00 ve 20:00'de, 1 tablet, 30 gün boyunca." Kaydetmeden önce "doğru mu?" kontrolü bu.
- Katlanır satırlar (kapalıyken özet alt yazı gösterir):
  - **Süre** — "Bugün başlar · 30 gün" (başlangıç tarihi, süre / süresiz)
  - **Stok ve azalınca uyar** — "Girilmedi · isteğe bağlı" (mevcut adet, uyarı eşiği gün olarak)
  - **Not** — "Ör. tok karnına, bol suyla" (doz satırında "· tok karnına" olarak görünür)
- Altta tam genişlik dolu "Kaydet" (56 dp).
- Akıllı varsayılanlar: Her gün, Tablet, 1 adet, 30 gün. Doğrulama hataları alanın altında, kaydırarak ilk hataya odaklan.
- Aynı ekran düzenleme modunda "İlacı düzenle" başlığıyla açılır; altta "Arşive taşı" metin butonu.

### 4 · İlaçlarım (`medications`)
**Amaç:** Stok üç biçimde okunur: çubuk + sayı + gün tahmini. Silme yok, Arşiv var: geçmiş ve raporlar bozulmaz.

- Başlık + `ProfileChip`; sekmeler "Aktif (5)" / "Arşiv (2)".
- Kart: `MedIcon`, ad, "500 mg · Her gün 08:00, 20:00" (PRN için "500 mg · Gerektiğinde"), chevron, altında `StockBar`.
  - Stok yoksa: info ikonu + "Stok girilmemiş · ekle" (dokunulabilir → stok alt sayfası).
  - PRN'de gün tahmini yok, yalnız "12 tablet".
- Karta dokun → İlaç detay/düzenle. FAB "İlaç ekle".
- Arşiv kartında "Geri al" (aktife taşı) eylemi.

### 5 · Geçmiş (`history`)
**Amaç:** Takvim "ay nasıl geçti" sorusunu yanıtlar; her gün renk + ikonla okunur. Uyum yüzdesinin yanında ham sayılar var. Geçmiş gün detayında düzeltilebilir.

- Başlık + `ProfileChip`; ay gezgini "‹ Ekim 2026 ›".
- Özet kartı (`primary.container`): "Bu ay uyum: %92" + "148 planlı dozun 136'sı alındı · 5 atlandı · 7 işaretlenmedi". Uyum = alındı / planlı.
- Takvim ızgarası (Pzt…Paz, Pazartesi başlangıçlı). Hücre: gün numarası + ikon:
  - Tamamı alındı → ✓, `status.taken.bg` zemin
  - Kısmi → yarım daire, `status.snoozed.bg`
  - İşaretlenmedi var → ünlem, `status.missed.bg`
  - Doz yok / gelecek → zeminsiz, "–"
  - Seçili gün → `primary.default` kenarlık (2 dp)
- Altta legend: "Tamamı alındı · Kısmi · İşaretlenmedi · Doz yok".
- Gün detayı ("14 Ekim, Çarşamba"): satır başına saat, ilaç adı, durum çipi + gerçekleşen saat ("07:41'de"), sağda kalem ikonu → durum/saat düzenleme alt sayfası.
- Hücrelerin `contentDescription`'ı: "14 Ekim, 5 dozdan 4'ü alındı, 1 işaretlenmedi".

### 6 · Rapor ve yedek (`reports`)
**Amaç:** Bakım verenin en sık işi "doktora götür": hazır aralıklar ve PDF varsayılan. Yedekte dosyanın nereye kaydedildiği açıkça yazar; geri yükleme ikincil butondur, onay diyaloğu üzerine yazma riskini söyler.

- Üst bar: ← "Rapor ve yedek".
- **Doktor raporu**: segmentli seçim "Son 1 ay" (varsayılan) / "Son 3 ay" / "Özel" (DateRangePicker). Profil + aralık dropdown "Ayşe · 1–31 Ekim 2026". Format kartları: **PDF** "Doktor için tek sayfa" (varsayılan) / **CSV** "Tablo programları için". PDF önizleme küçük resmi. Dolu buton "PDF oluştur ve paylaş" → `PdfDocument` ile üret, `FileProvider` + share sheet.
- **Yedek**: "Son yedek: 3 Ekim 2026, 10:12" + dosya yolu kutusu "İndirilenler / DozDefteri / yedek-2026-10-03.ddb"; tonal "Şimdi yedek oluştur" (MediaStore Downloads); outlined "Yedekten geri yükle" (SAF dosya seçici → onay diyaloğu: mevcut verilerin yedekteki verilerle değişeceğini açıkça yazar); switch "Ayda bir yedek hatırlat" + "Sonraki: 3 Kasım".

### 8 · Ayarlar (`settings`)
**Amaç:** "Hatırlatma sağlığı" en üstte: güven ilkesinin kalıcı adresi. Her izin ikon + metinle durumunu söyler, sorunlu olan tek dokunuşla düzelir. Pro özellikler ilgili ayarın yanında görünür. Sorumluluk reddi sade dille en altta.

- **Hatırlatma sağlığı** kartı (sorun varsa `warning.fg` kenarlık + `warning` rozeti "1 sorun"):
  - Bildirimler — "Açık" ✓ / değilse "Düzelt" (POST_NOTIFICATIONS / bildirim ayarları)
  - Tam vaktinde çalma — "Kapalı · birkaç dakika gecikebilir" + tonal "Düzelt" (`ACTION_REQUEST_SCHEDULE_EXACT_ALARM`)
  - Pil kısıtlaması — "Kısıtlanmamış" ✓ / değilse "Düzelt" (pil optimizasyonu muafiyeti)
  - Durumlar `ON_RESUME`'da yeniden okunur.
- **Görünüm**: Dil "Türkçe" (per-app language), Tema "Sistem ayarını izle", Büyük yazı "Metinler ve butonlar büyür" (switch — uygulama içi ek ölçek, bkz. §5).
- **Hatırlatmalar**: Erteleme süresi "10 dakika"; "İşaretlenmedi" sayılma süresi "Doz saatinden 60 dk sonra".
- **Veriler ve profiller**: Rapor ve yedek "PDF, CSV, yedek dosyası"; Profiller "Birden fazla kişinin ilaçları" + "Pro" rozeti; DozDefteri Pro "Tek seferlik · ₺149" (fiyatı Play Billing'den al, sabit yazma).
- **Hakkında**: Gizlilik "Verilerin yalnızca bu telefonda. Hesap yok."
- Alt not (info ikonu): "DozDefteri tıbbi tavsiye vermez, doktor talimatının yerini tutmaz."

### 8b · Pro (`pro`)
**Amaç:** Dürüst monetizasyon: ilk blok ücretsiz sürümün kalıcı olarak neleri kapsadığını söyler. Fiyat tek seferlik ve açık; abonelik, sayaç, "son fırsat" yok. "Satın alımı geri yükle" her zaman görünür.

- ← "DozDefteri Pro"; başlık "Bir kez öde, hep senin."; alt metin "Abonelik yok, reklam yok. Ücretsiz sürüm de kalıcı olarak ücretsiz kalır."
- `primary.container` kutu: "Ücretsizde sonsuza kadar" / "Sınırsız ilaç · tüm hatırlatmalar · geçmiş ve uyum · PDF/CSV rapor · yedekleme".
- Tablo (Özellik · Ücretsiz · Pro): Sınırsız ilaç ve hatırlatma ✓ ✓ · Bildirimden Aldım / Ertele / Atla ✓ ✓ · Geçmiş, rapor, yedek ✓ ✓ · Birden fazla profil – ✓ · Ana ekran widget'ı – ✓ · Profil renkleri – ✓. "–" hücrelerine `contentDescription = "yok"`.
- Dolu buton "₺149 · Tek seferlik satın al", metin buton "Satın alımı geri yükle", dipnot "Ödeme Google Play üzerinden. Hesap gerekmez."
- Satın alma mantığını `ProRepository` arkasına koy; bu fazda sahte (fake) uygulama yeterli, Play Billing bağlantısı ayrı iş.

---

## 4. Bildirimler ve widget

### 0 · Doz bildirimi
**Amaç:** Bildirim uygulamanın asıl ekranıdır. İlaç adı ve doz başlıkta, saat ve form ikincil satırda. Üç eylem metin buton (Android standardı), "Aldım" her zaman ilk sırada. Aynı dakikadaki dozlar tek bildirimde gruplanır.

- Kanal **"Doz hatırlatmaları"**: `IMPORTANCE_HIGH` (heads-up), kategori `CATEGORY_REMINDER`, küçük ikon tek renk hap ikonu, uygulama rengi `primary.default` (teal/600).
- Tek doz: başlık "Metformin 500 mg", metin "Saat 20:00 · 1 tablet, tok karnına", eylemler **Aldım · Ertele (10 dk) · Atla** (ertele süresi ayardan).
- Aynı dakikada birden çok doz → tek bildirim: başlık "20:00 · 2 ilaç", metin "Metformin, Atorvastatin", eylemler **Hepsini aldım · Ertele · Aç**. Genişletilmiş (`InboxStyle`): "Tek tek işaretlemek için uygulamayı aç." + satırlar "Metformin 500 mg · 1 tablet", "Atorvastatin 20 mg · 1 tablet".
- Eylemler `BroadcastReceiver` → repository; uygulama açılmadan çalışmalı.

### 0b · Stok ve eylem sonrası
**Amaç:** Stok uyarısı ayrı ve sessiz kanalda, doz bildirimiyle yarışmaz. "Aldım" sonrası bildirim kısa bir onayla güncellenip 4 sn sonra kapanır; düşen stok burada görünür, "Geri al" bildirimin içindedir.

- Kanal **"Stok uyarıları"**: `IMPORTANCE_LOW`, sessiz. "Atorvastatin azalıyor" / "4 tablet kaldı, yaklaşık 4 gün yeter." / eylem "Stok ekle" (uygulamada stok alt sayfasını açar). Günde en fazla bir kez.
- "Aldım" sonrası aynı bildirim ID'si güncellenir: "✓ Metformin alındı · 20:02", "Kalan: 37 tablet · ~18 gün", ilerleme çubuğu (stok oranı), eylem "Geri al"; `setTimeoutAfter(4000)`, sessiz güncelleme (`setOnlyAlertOnce`).
- "Ertele" sonrası: "⏱ Metformin 20:10'a ertelendi" / "Tekrar hatırlatacağız." / eylem "Şimdi aldım".

### 0c · %200 yazı boyutu
Büyük yazıda başlık ve metin satır kırar, kesilmez; eylemler alt satıra sarar ve her biri 48 dp kalır. Bu yüzden ilaç adı başlıkta, doz ikinci satırda: dar alanda önce en önemli bilgi görünür. → Bildirimde özel `RemoteViews` **kullanma**, standart şablon bunu sağlar.

### W · Ana ekran widget'ı (Pro, Glance)
**Amaç:** Widget yalnızca "sıradaki doz" sorusunu yanıtlar ve tek eylem sunar. Aktif profil adı küçük ama her zaman var.

- **2×2**: "Ayşe · sıradaki", büyük saat "20:00", "Metformin +1", dolu "Aldım" (sıradaki saatteki tüm dozları alır).
- **4×2**: "Ayşe · sıradaki · 20:00", o saatteki ilaçlar listesi (ikon + "Metformin 500 mg"), "Bugün 2/5 alındı", dolu "Hepsini aldım".
- **Gün tamam** durumu: ✓ daire + "Bugünkü dozlar tamam" / "Yarın ilk doz 07:30".
- `surface.default` kart, `Radius.xl` (28 dp) köşe, Android 12+ dinamik köşe yarıçapı. Eylem `ActionCallback` ile; widget her doz değişiminde `updateAll`.
- Pro değilse widget eklenebilir ama "Pro ile kullanılabilir" + "Aç" gösterir.

---

## 5. Erişilebilirlik ve büyük yazı (kabul ölçütleri)

- Tüm ekranlar `fontScale = 2f` previews'ta kırpılmadan, yatay taşma olmadan çalışır; satır içi öğeler `FlowRow` ile sarar.
- Ayarlardaki **Büyük yazı** switch'i: sistem ölçeğinin üstüne uygulama içi 1.15× ölçek + buton yüksekliklerini 64 dp yapar (`LocalDensity` sarmalayıcısı).
- Dokunma hedefleri ≥ 48 dp; "Aldım" 56 dp.
- Durum bilgisi her yerde ikon + metin; renk tek başına anlam taşımaz.
- TalkBack: kartlar birleşik açıklama, takvim hücreleri tam cümle, ikon butonlarda `contentDescription`, bölüm başlıklarında `heading()`.
- Kontrast: metin ≥ 4.5:1 (özellikle koyu temada `text.secondary` sand/300 ve tüm `status.*.fg`/`bg` çiftlerini ölç; sorun varsa raporla, token'ı kendin değiştirme).

---

## 6. Fazlar

Her fazın sonunda **derlenebilir** durumda bırak, `./gradlew lint testDebugUnitTest` çalıştır, faz raporu yaz.

| Faz | İçerik | Bitti sayılır |
|---|---|---|
| **1 — Tema** | `Color.kt`, `Type.kt`, `Shape.kt`, `DozColors` + `LocalDozColors`, açık/koyu tema, ikon eşlemesi | Token preview ekranı açık/koyu render oluyor |
| **2 — Bileşenler** | §2'deki tüm bileşenler + `DoseCard`'ın 5 durumu | Her bileşen için açık/koyu/%200 preview; screenshot testleri (Roborazzi veya Paparazzi) |
| **3 — İskelet** | Navigation, bottom bar, `ProfileChip` + profil menüsü, boş ekranlar | 4 sekme arası geçiş, geri davranışı doğru |
| **4 — Bugün + İlaç ekle** | Ekran 2 ve 3, ViewModel'ler, mevcut veri katmanına bağlama, özet cümle üretici (birim testli) | İlaç eklenince Bugün'de doğru grupta görünüyor; Aldım/Ertele/Geri al çalışıyor |
| **5 — İlaçlarım + Geçmiş** | Ekran 4 ve 5, arşiv, takvim hesapları, gün detayı düzenleme | Uyum yüzdesi ve gün durumları birim testli |
| **6 — Bildirimler** | 2 kanal, tekli/gruplu doz bildirimi, eylem receiver'ları, eylem sonrası güncelleme, stok bildirimi | Gerçek cihazda/emülatörde bildirimden Aldım uygulamayı açmadan kaydediyor |
| **7 — Rapor/yedek + Ayarlar + Pro** | Ekran 6, 8, 8b; izin durumu okuma; PDF/CSV; yedek/geri yükle; sahte `ProRepository` | Hatırlatma sağlığı kartı izinleri doğru gösteriyor; PDF paylaşılabiliyor; geri yükleme onay diyaloğu var |
| **8 — Widget + cila** | Glance widget (3 durum), animasyonlar, erişilebilirlik taraması, kontrast düzeltmeleri | §5 maddeleri tek tek işaretli |

### Faz raporu formatı

```
## Faz N raporu
- Yapılanlar: …
- Tasarımdan sapmalar ve nedeni: …
- Doğrulanamayanlar / tahmini değerler: …
- Testler: komut + sonuç
- Önerilen commit mesajı: feat(ui): …
```

---

## 7. Kapsam dışı / açık noktalar

- Play Billing gerçek entegrasyonu (bu planda sahte repository).
- Ekran 1 ve 7 Figma'da yok (numaralandırma 0, 2, 3, 4, 5, 6, 8, 8b, W). Figma dosyasında varsa onları da uygula; yoksa raporda not et.
- 03 sayfası ("Akışlar & Kenar durumlar") şu an boş; boş durumlar, onay diyalogları ve hata halleri için bu plandaki tanımları kullan.
- Koyu ekranlar için 02 sayfasındaki "Board / Koyu tema" referans alınır.
