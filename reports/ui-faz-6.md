## UI Faz 6 raporu — Bildirimler

- **Yapılanlar:**
  - Kanallar: **"Doz hatırlatmaları"** (`IMPORTANCE_HIGH`, heads-up) ve **"Stok uyarıları"** (`IMPORTANCE_LOW`, sessiz; önemi var olan bir kanalda düşürülemediği için yeni kanal kimliği `stock_alerts_quiet`, eski kanal siliniyor). Yedek hatırlatmaları kanalı (LOW) korunuyor. Tüm bildirimlerde uygulama rengi teal/600 ve tek renk hap ikonu.
  - **Tek doz:** başlık "Metformin 500 mg", metin "Saat 20:00 · 1 tablet, tok karnına", eylemler **Aldım · Ertele (10 dk) · Atla** (erteleme süresi ayardan).
  - **Aynı dakikada birden çok doz → tek bildirim:** başlık "20:00 · 2 ilaç", metin "Metformin, Atorvastatin", eylemler **Hepsini aldım · Ertele · Aç**; genişletilmiş `InboxStyle`: "Tek tek işaretlemek için uygulamayı aç." + "Metformin 500 mg · 1 tablet" satırları. Uygulamadan dozlardan biri işaretlenince grup kalan dozlarla sessizce yeniden çizilir (tek doz kalırsa tek doz bildirimine döner).
  - **"Aldım" sonrası:** aynı bildirim "✓ Metformin alındı · 20:02" + "Kalan: 37 tablet · ~18 gün" + stok oranı ilerleme çubuğu + "Geri al" ile güncellenir; `setTimeoutAfter(4000)`, sessiz (`setOnlyAlertOnce`, `setSilent`).
  - **"Ertele" sonrası:** "⏱ Metformin 20:10'a ertelendi" / "Tekrar hatırlatacağız." / "Şimdi aldım"; ertelenen saat gelince hatırlatma aynı bildirimin yerini alır.
  - **Stok bildirimi:** "Atorvastatin azalıyor" / "4 tablet kaldı, yaklaşık 4 gün yeter." / "Stok ekle" (uygulamada İlaçlarım'ı açıp o ilacın stok alt sayfasını gösterir). İlaç başına stok yeniden artırılana kadar yalnızca bir kez (günde en fazla bir kez şartını karşılar).
  - Eylemler `DoseActionReceiver` üzerinden, uygulama açılmadan çalışır; bir bildirimdeki tüm doz kimlikleri tek seferde işlenir. Özel `RemoteViews` kullanılmadı (büyük yazıda standart şablon satır kırar).
  - `LowStockNotifier` kalan gün tahminini de alıyor.
- **Tasarımdan sapmalar ve nedeni:** Grup için "Geri al" onayı da tüm dozları birlikte geri alır ("✓ 2 ilaç alındı").
- **Doğrulanamayanlar / tahmini değerler:** Kabul kriteri "bildirimden Aldım uygulamayı açmadan kaydediyor" gerçek cihaz/emülatör gerektirir; doğrulanamadı.
- **Testler:** JVM saf testleri değişmedi (10 sınıf ✅); `DoseRepositoryTest`/`BackupManagerTest`/`AlarmSchedulerTest` yeni `LowStockNotifier` imzasına güncellendi (çalıştırılamadı). `./gradlew lint testDebugUnitTest` çalıştırılamadı (dl.google.com engeli).
- **Önerilen commit mesajı:** `feat(notifications): group same-minute doses and add post-action confirmations`
