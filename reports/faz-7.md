# Faz 7 — Pro ve Billing

## Yapılanlar
- **`BillingManager`** (Bölüm 7.2, Play Billing 9.1.0): `@Singleton`, `enablePendingPurchases(PendingPurchasesParams…enableOneTimeProducts())`; `isPro: StateFlow<Boolean>` (başlangıç `pro_cached`, `DEBUG_FORCE_PRO` ise hep true); açılışta bağlanır, `queryPurchasesAsync(INAPP)` → `pro_lifetime` PURCHASED ise `pro_cached = true`, başarılı sorguda yoksa `false`, başarısız sorguda önbellek korunur; onaylanmamış satın almalar `acknowledgePurchase` ile onaylanır; `queryProductDetailsAsync` ile `formattedPrice`; `launchPurchase(activity)`; `onPurchasesUpdated`: PURCHASED → onay + Pro, PENDING → "Ödeme bekleniyor", ITEM_ALREADY_OWNED → yeniden sorgu; bağlantı koparsa/kurulum başarısızsa üstel geri çekilme 1-2-4-8 sn, en fazla 4 deneme. Sunucu doğrulaması yok.
- **`ProStatus`** tek okuma noktası (Faz 5'te eklendi), `BillingManager.isPro` onu kullanır.
- **Pro ekranı** (Bölüm 6.11): özellik listesi (Bölüm 7.1), fiyat (yüklenemezse buton pasif + "Fiyat yüklenemedi"), "Satın al", "Satın alımı geri yükle", altta "Hatırlatmalar ve ilaç sayısı her zaman ücretsizdir.", Pro etkinse teşekkür satırı; sonuç mesajları Snackbar ile.
- **Pro kontrolleri bağlandı:** 2.+ profil (`ProfilesViewModel` → Pro ekranı), detaylı PDF şablonu (`ExportViewModel` → `detailed = isPro`), vurgu rengi 1–3 (`SettingsViewModel`, `MainViewModel` Pro yoksa 0 uygular), dışa aktarımda "Tüm profiller" (`ExportViewModel`, kilitliyse Pro ekranı). Widget Faz 8'de bağlanır. Hatırlatma, ilaç sayısı, geçmiş, CSV ve yedekte hiçbir Pro kontrolü yok.
- `DozDefteriApp.onCreate` → `billingManager.start()`.

## Oluşturulan / değişen dosyalar
- `billing/BillingManager.kt` (yeni), `billing/ProStatus.kt`
- `ui/pro/{ProScreen,ProViewModel}.kt`
- `DozDefteriApp.kt`, `ui/navigation/AppNavHost.kt` (ikon import çakışması için takma adlar)
- `res/values{,-tr}/strings.xml`

## Komut sonuçları
- assembleDebug: ❌ çalıştırılamadı (dl.google.com engeli; Billing kütüphanesi de oradan iniyor)
- testDebugUnitTest: ❌ çalıştırılamadı; JVM doğrulama projesinde saf testler 39/39 ✅ (değişmedi)
- lintDebug: ❌ çalıştırılamadı
- **Birleşik manifest kontrolü (Bölüm 2.4):** yapılamadı — `app/build/intermediates/merged_manifest/release/...` derleme olmadan oluşmuyor. Bilinen durum: Play Billing kütüphanesi kendi manifest'iyle `com.android.vending.BILLING` iznini ekler; `INTERNET`/`ACCESS_NETWORK_STATE` ekleyip eklemediği ilk derlemede kontrol edilmeli (plan gereği eklenirse kaldırılmayacak, yalnızca raporlanacak).

## Kabul kriterleri
- [ ] `DEBUG_FORCE_PRO = true` ile tüm Pro özellikler açılır, `false` ile kapanır — tüm kapılar `ProStatus.isPro`'ya bağlı ve o da `DEBUG_FORCE_PRO`'yu okuyor; cihazda doğrulanmalı
- [ ] Billing bağlanamadığında uygulama çökmez ve önbellek kullanılır — `startConnection` istisnası yakalanır, başarısız sorgu önbelleği değiştirmez; cihazda (ör. Play Store'suz emülatör) doğrulanmalı
- [ ] Gerçek satın alma testi — insan tarafından (Play Console + lisans test hesabı)

## Verilen kararlar
- **Billing 9.1.0** kullanıldı (en güncel kararlı; Play 31 Ağustos 2026'dan itibaren en az 8 istiyor). Fiyat için `getOneTimePurchaseOfferDetails()` kullanıldı (8.0/9.0'da kaldırılmadı).
- **Etkinlik (Activity) ViewModel'de tutulmaz:** satın alma akışı için ekran `Activity`'yi bulup `ProViewModel.purchase(activity)` → `BillingManager.launchPurchase(activity)` çağrısına yalnızca o an geçirir.
- **"Satın alımı geri yükle"** Play'den yeniden sorgu yapar ve sonucu (geri yüklendi / bulunamadı / hata) Snackbar'da gösterir. İstemci hazır değilse yeniden bağlanmayı başlatır ve hata gösterir.
- **ITEM_ALREADY_OWNED** durumunda satın alım geri yüklenmiş sayılır.
- **İade/iptal:** başarılı bir sorguda `pro_lifetime` bulunmazsa `pro_cached = false` olur (plan kuralı); böylece iade edilen satın almada Pro kapanır.
- Billing akışında ağ erişimi Play Store uygulaması üzerinden olur; uygulama kodunda HTTP isteği yok.

## Çözülemeyen sorunlar
- Derleme ve birleşik manifest kontrolü yapılamadı (ortam engeli).

## Kullanıcının elle doğrulaması gerekenler
- Play Console'da `pro_lifetime` uygulama içi ürününü oluşturmak (TR ₺149, US $4,99), lisans test hesaplarını eklemek, uygulamayı kapalı test kanalına yüklemek (Billing yalnızca Play'den kurulan imzalı sürümde çalışır).
- Test hesabıyla satın alma, bekleyen ödeme (test kartı "yavaş"), iade sonrası Pro'nun kapanması, "Satın alımı geri yükle".
- `DEBUG_FORCE_PRO = true` yapıp: 2. profil, vurgu renkleri, "Tüm profiller", detaylı PDF; `false` ile kilitlerin geri gelmesi.
- İlk derlemeden sonra `app/build/intermediates/merged_manifest/release/.../AndroidManifest.xml` içinde `INTERNET`/`ACCESS_NETWORK_STATE` olup olmadığına ve hangi kütüphaneden geldiğine bakmak (Android Studio > Merged Manifest sekmesi).

## Önerilen commit mesajı
feat(billing): add one-time Pro purchase with Play Billing and feature gates
