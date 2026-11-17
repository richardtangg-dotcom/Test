# Bambu Live

Android-app med hemskärmswidget som visar din **Bambu Lab P2S** live via **Bambu Cloud**.

## Funktioner

- **Inloggning med Bambu-kontot** (inkl. e-postkod och tvåstegsverifiering).
- **Status var du än är** via Bambu Clouds MQTT: status, procent, tid kvar, lager, temperaturer.
- **Live-kamera** i appen (RTSPS från skrivaren, port 322).
- **Widget** med kamerabild/omslagsbild, förlopp, tid kvar och temperaturer.
  - *Live-widget på*: en förgrundstjänst håller anslutningen öppen och uppdaterar widgeten var 5:e sekund.
  - *Av*: widgeten uppdateras var 15:e minut, eller när du trycker på ⟳.

## Viktigt om kameran

Bambu skickar **inte** kamerabilden via sitt moln till tredjepartsappar (deras molnvideo
använder ett stängt P2P-protokoll). Därför gäller:

- **Status och widget** fungerar överallt via molnet.
- **Live-video** fungerar när mobilen är på **samma WiFi** som skrivaren. Slå på
  **LAN Mode Liveview** på skrivaren (Inställningar → Nätverk). Åtkomstkoden hämtas från kontot
  och IP-adressen från skrivarens egna rapporter; båda går att ändra under *Kamerainställningar*.
- Medan videon visas sparas en bild var 10:e sekund som widgeten visar. Utan färsk kamerabild
  visar widgeten omslagsbilden för den aktuella utskriften från Bambu Cloud.

## Installera

Varje push bygger en APK med GitHub Actions:

1. Gå till **Releases** i repot (eller fliken **Actions** → senaste körningen → *BambuLive-apk*).
2. Ladda ner `BambuLive.apk` på mobilen och installera (tillåt "installera okända appar").
3. Öppna appen, logga in och välj skrivare.
4. Håll in på hemskärmen → **Widgetar** → **Bambu Live**.

Bygga själv: `./gradlew assembleDebug` (kräver Android SDK, JDK 17).

## Teknik

- Kotlin, Jetpack Compose, Glance (widget), Media3/ExoPlayer (RTSP), Eclipse Paho (MQTT), OkHttp.
- `RtspsProxy` är en liten lokal proxy som sköter TLS mot skrivarens självsignerade certifikat,
  eftersom ExoPlayer bara kan vanlig RTSP.
- Bambu Clouds API är inofficiellt och kan ändras av Bambu Lab.
