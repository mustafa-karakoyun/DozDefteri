# Faz 6 — Ayarlar, profiller, pil rehberi

## Yapılanlar
- **Ayarlar** (Bölüm 6.9): Görünüm (tema modu: sistem/açık/koyu; vurgu rengi 4 seçenek, 1–3 Pro kilitli → Pro ekranı), Hatırlatmalar (erteleme 5/10/15/30/60 dk; kaçırıldı eşiği 1/2/3/6 saat — değişince `DoseSync`; kesin alarm izni durumu + düzelt; bildirim ayarları; pil rehberi), Veri (Dışa aktar ve yedekle, Profiller), Pro (durum → Pro ekranı), Hakkında (sürüm, gizlilik politikası, sorumluluk reddi), yalnızca debug'da Geliştirici bölümü.
- DataStore ayarlarının etkisi: tema modu ve vurgu rengi `MainViewModel` üzerinden anında uygulanır (Pro değilse vurgu 0'a düşer), erteleme süresi bildirim ve Bugün ekranındaki ertelemede, kaçırıldı eşiği `DoseSync.markMissed`'de kullanılır. Hepsi DataStore'da kalıcıdır.
- **Profiller** (Bölüm 6.8): liste, aktif profil işareti (satıra dokununca aktif olur), ekle/düzenle diyaloğu (ad + 8 renk paleti), sil (onayda tüm verinin silineceği yazılır; son profil silinemez — sil butonu gösterilmez). Ücretsizde ikinci profil eklenmeye çalışılınca Pro ekranına yönlendirir. Silme: gösterilen bildirimler iptal, `DoseSync.run("profile_delete")`, aktif profil silindiyse ilk profile geçilir.
- **Pil rehberi** (Bölüm 6.10): genel açıklama, "Pil optimizasyonu ayarlarını aç" (`ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`), `Build.MANUFACTURER`'a göre (xiaomi/redmi/poco, samsung, huawei/honor, oppo/realme/oneplus, vivo) adımlar; eşleşme yoksa genel metin. TR/EN.
- **Sorumluluk reddi** (Bölüm 6.12): Ayarlar > Hakkında ve Onboarding 1. sayfa altında (Faz 3).
- `privacy_policy_url` boş bırakıldı (`translatable="false"`); boşken Ayarlar'da gizlilik politikası satırı gösterilmez.

## Oluşturulan / değişen dosyalar
- `ui/settings/{SettingsScreen,SettingsViewModel}.kt`
- `ui/profiles/{ProfilesScreen,ProfilesViewModel}.kt`
- `ui/battery/BatteryGuideScreen.kt`
- `ui/MainViewModel.kt` (Pro'ya bağlı vurgu), `ui/navigation/AppNavHost.kt`
- `res/values{,-tr}/strings.xml`

## Komut sonuçları
- assembleDebug: ❌ çalıştırılamadı (dl.google.com engeli)
- testDebugUnitTest: ❌ çalıştırılamadı; JVM doğrulama projesinde saf testler 39/39 ✅ (değişmedi)
- lintDebug: ❌ çalıştırılamadı

## Kabul kriterleri
- [ ] Profil değişince Bugün/Geçmiş o profili gösterir — her iki ekran `activeProfile` Flow'una bağlı; cihazda doğrulanmalı
- [ ] Profil silme o profilin bildirimlerini iptal eder — kod yolu hazır; cihazda doğrulanmalı
- [ ] Ayarlar uygulama yeniden başlatıldığında korunur — DataStore; cihazda doğrulanmalı

## Verilen kararlar
- **Profil adı** en fazla 30 karakter (plan sınır vermiyor; başlıkta ve bildirim önekinde okunur kalsın diye).
- **Yeni profil rengi** varsayılan olarak sıradaki palet rengi.
- **Kaçırıldı eşiği** seçenekleri saat olarak gösterilir (60 dk → "1 saat").
- **Gizlilik politikası satırı** URL boşken gizlenir (boş bir linke dokunmak hata olurdu).
- **Vurgu rengi** Pro bitip ücretsize dönülürse kayıtlı seçim korunur ama uygulanmaz (yeniden Pro olunca geri gelir).
- Ayarlar seçimleri tek seçimli diyaloglarla (radyo) yapılır; 48 dp satır yüksekliği.

## Çözülemeyen sorunlar
- Derleme/çalıştırma yok (ortam engeli).

## Kullanıcının elle doğrulaması gerekenler
- Gizlilik politikası sayfasını yayınlayıp URL'yi `app/src/main/res/values/strings.xml` içindeki `privacy_policy_url`'ye yazmak.
- Tema ve vurgu değişikliğinin anında uygulanması, uygulama kapatılıp açılınca korunması.
- Profil ekleme (Pro'da), profil değiştirince Bugün/Geçmiş'in değişmesi, profil silince o profilin bildirimlerinin kaybolması.
- Xiaomi/Samsung cihazda pil rehberinde doğru marka metninin görünmesi.

## Önerilen commit mesajı
feat(settings): add settings, profiles management and battery guide
