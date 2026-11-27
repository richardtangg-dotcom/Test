# Reservdelssökning – Yamaha RX-A3020 Aventage, digitalt kretskort

Sammanställt 2026-11-27. Alla priser i SEK är uppskattningar med kurserna
**1 EUR ≈ 11,05 kr, 1 USD ≈ 10,0 kr, 1 CAD ≈ 7,2 kr, 1 PLN ≈ 2,6 kr, 1 JPY ≈ 0,067 kr**
(Riksbankens och ECB:s referenskurser i mitten av november 2026, avrundade).

## Viktigt om källorna – läs detta först

- Den här miljön kunde **inte öppna annonssidor direkt**. eBay, Kleinanzeigen,
  Tradera, Blocket, badcaps.net och manualmachine.com blockerades av nätverkspolicyn.
  Allt nedan bygger på sökmotorernas index och utdrag ur sidorna.
- **Ingen annons har kunnat verifieras som aktiv i dag.** Datum och priser kommer
  från sökindexet och kan vara inaktuella. Kontrollera varje länk innan du gör något.
- Tradera och Blocket gick inte att söka i alls (Blocket blockerar sökverktyget).
  Där får du söka själv: `RX-A3020`, `RX-V3073`, `Yamaha 3020`.
- Fraktkostnaderna är mina uppskattningar och inga offerter. De är markerade med *(est.)*.

---

## Etapp 1 – Identifiering av kretskortet

### 1.1 Vad som är bekräftat

| Uppgift | Värde | Källa / säkerhet |
|---|---|---|
| Servicemanual | *RX-A3020 / RX-V3073 Service Manual*, 216 sidor (täcker även RX-A2020/RX-V2073 med noter som "A2020 A3020 only") | [elektrotanya](https://elektrotanya.com/yamaha_rx-a3020_rx-v3073.pdf/download.html), [manualmachine](https://manualmachine.com/yamaha/rxa3020/3237109-service-manual/), [manuali.com (köp)](https://manuali.com/products/yamaha-rx-a3020-rx-v3073-av-receiver-service-manual). **Säker** |
| Kortets namn i manualen | **DIGITAL P.C.B.** med schemablad "DIGITAL 1/10 … 10/10". Forumet kallar huvuddelen **DIGITAL (1)** | Manualens index + [badcaps-tråd](https://www.badcaps.net/forum/troubleshooting-hardware-devices-and-electronics-theory/troubleshooting-audio-equipment/89701-yamaha-rx-a3020). **Säker** |
| DSP-kretsar på kortet | **IC911 och IC920: D80YK113CPTP400** (Texas Instruments, Yamaha-specifik). Kapsel **QFP/TQFP-176**, alltså **inte BGA** | Manualens index (via sökutdrag), kretshandlarnas kapselangivelse. **Säker** |
| Andra kretsar på samma kort | HDMI-sändare IC61/62 Sii9136CTU-3, logik IC70–73 TC74VHC273FT, IC76/77 SN74LV4051APWR | Manualens index. **Säker** |
| Yamahas reservdelsnummer för kortet | **ZC735700 – "P.C.B. ASSEMBLY DIGITAL RX-A30 20ML EX"** ("RX-A30 20" = RX-A3020 i Yamahas avkortade benämning) | Yamaha Electronics Corp. USA:s reservdelskatalog via [UED.net](https://www.ued.net/catalog/cat_item_idx_8970.html), och en eBay-annons som säljer kortet under samma nummer. **Mycket trolig**, verifiera mot etiketten |
| Tillgänglighet ny | ZC735700 uppges **inte längre levereras** från Yamaha | [JustAnswer](https://www.justanswer.com/home-theater-stereo/bn40h-no-sound-yamaha-rx-a3020-receiver-awhile-ago-just.html) (ca 8 år gammal uppgift). **Trolig** |
| Firmware | Gemensam firmwarefamilj för RX-A2020/RX-A3020/RX-V2073/RX-V3073, senaste version **1.98** | [Yamaha USA](https://usa.yamaha.com/support/updates/rx-a2020-3020-en.html), [Yamaha AU](https://au.yamaha.com/en/support/updates/rx-a2020-3020_en.html). **Säker** |

**Slutsats:** Din benämning "DIGITAL (1) P.C.B." stämmer. Det är på det kortet DSP1
(IC911) och DSP2 (IC920) sitter. Kortet säljs som en sammansatt enhet,
**ZC735700 "P.C.B. ASSEMBLY DIGITAL"**. Kortet bär också HDMI-sändarna, och därför
säljs det på eBay som "DIGITAL MAIN HDMI PCB".

### 1.2 Felaktiga eller osäkra artikelnummer – köp inte på dessa

| Nummer | Varför det inte ska användas |
|---|---|
| **ZK647000**, "P.C.B. ASSEMBLY DIGITAL RX-A30" ([instrumentalparts](https://instrumentalparts.com/p-c-b-assembly-digital-rx-a30/), ca 625–657 USD) | "RX-A30" är avkortat och kan betyda vilken RX-A30x0 som helst. Nummerserien ZK… hör till en senare generation än ZC… (2012). **Ej verifierat för RX-A3020** |
| **ZX240000** ("ASSEMBLY DIGITAL RX-A30 70") | Gäller RX-A3070 |
| **ZQ781200** ("Digital RX-A3050ML"), **ZU763600** (RX-A3060) | Andra modeller |
| **ZC735500** | Ett digitalkort i samma nummerserie, men forumet kopplar det till RX-A2020. **Inte RX-A3020** |
| **ZC736000** | Ett FUNCTION-kort till RX-A1020 |
| **WY506200** | Ett DIGITAL-2-kort till RX-A3010 (annan generation) |
| **"HDMI BOARD YE540"** (eBay, sålt för 39,95 USD, "for parts") | Det är oklart om YE540 är kortets tryckta beteckning för samma digitalkort eller ett separat HDMI-kort. Jämför med märkningen på ditt kort |

### 1.3 Kompatibilitet med andra modeller

| Modell | Bedömning |
|---|---|
| **RX-V3073** | **Samma plattform och samma servicemanual** som RX-A3020, och firmwaren är gemensam. Digitalkortet är troligen identiskt eller nästan identiskt, men jag har inte hittat något artikelnummer för RX-V3073:s digitalkort som bekräftar att det är ZC735700. **Möjlig donator, men overifierad** |
| **RX-A2020 / RX-V2073** | Samma manual, men manualen har avvikelsenoter och katalogen listar ett **eget** digitalkort för RX-A2020. **Inte kompatibelt utan vidare** |
| **RX-A3010 / RX-V3071** (2011) | Annan generation med andra kortnummer (WY…-serien). **Inte kompatibelt** |
| **RX-A3030 / RX-A3040 / RX-A3050** | Senare generationer med andra kort. De använder dock samma DSP-familj (D80YK113…-varianter). **Kortet är inte kompatibelt, men DSP-kretsen är det troligen** |
| **Japansk RX-A3020 (J-version)** | Samma namn men 100 V och japansk destination och firmware. Digitalkortet kan ha destinationsspecifik konfiguration. **Undvik** |
| **USA-version (U) / Kanada (C)** | Annan destination, regionsfirmware och tunersteg. Ett U-kort kan behöva få destinationen omskriven. **Föredra europeisk (G/B) donator** |

### 1.4 Krävs programmering efter byte?

- Syskonmodellernas servicemanualer säger uttryckligen följande:
  - **RX-A3010:** "When the DIGITAL1 P.C.B. or IC83 on DIGITAL1 P.C.B. is replaced,
    this unit will not operate properly without additional setting."
    ([manualmachine RX-A3010](https://manualmachine.com/yamaha/rxa3010/8215079-service-manual))
  - **RX-A3070:** modellnamn och serienummer måste skrivas till backup-EEPROM på
    DIGITAL-kortet, annars visas "Internal Error".
    ([manualmachine RX-A3070](https://manualmachine.com/yamaha/rxa3070/14506074-service-manual/))
- För **RX-A3020** har jag inte kunnat läsa den exakta texten, men samma princip gäller med
  mycket stor sannolikhet. Ett **nytt** kort kräver då att modell, destination och
  serienummer skrivs via servicemenyn. Ett **begagnat kort ur en RX-A3020 med samma
  destination** bär redan rätt modellidentitet i sin EEPROM och fungerar troligen direkt.
  Serienumret blir då donatorns.
- Uppdatera alltid till **firmware 1.98** för din region (via USB eller nätverk) efter bytet.
- Ett kort från U-, C- eller J-destination kan kräva att destinationen skrivs om enligt
  servicemanualen. Det är **inte bekräftat att det går** med kortet ur en annan region.
  En RX-A1050-ägare misslyckades med motsvarande omskrivning.

### 1.5 Märkning du behöver fotografera

Fotografera följande för att kunna bekräfta kompatibiliteten exakt:

1. **Etiketten på digitalkortet**. Det är oftast en vit eller gul streckkodsetikett med
   ett nummer av typen `ZC735700` eller liknande, och ibland en revisionsbokstav eller ett lotnummer.
2. **Den tryckta kortbeteckningen** i kopparlagret eller silkscreen, till exempel
   "DIGITAL (1)", ett kortnummer i stil med `1xxxxx` eller `YExxx`, och eventuell revision (A/B/C…).
3. **DSP-kretsarnas toppmärkning**, IC911 och IC920. Är det `D80YK113CPTP4` eller den
   reviderade `D80YK113DPTP4`? Ta med datumkoden.
4. **Typskylten på baksidan**: modell, serienummer, spänning (230 V) och destinationskod.
5. **Firmwareversionen** i menyn (Setup → Information eller ADVANCED SETUP).
6. Om du kan: **självdiagnosen** i servicemenyn (proceduren finns i servicemanualen), där
   testet "DSP BUS" (C1-7 i syskonmodellerna) visar OK/NG för respektive DSP när kortet
   är kallt respektive varmt. Det avgör om felet sitter i IC911 eller IC920.

### 1.6 Teknisk kommentar om felbilden

Felbilden är dokumenterad hos andra RX-A3020-ägare. På badcaps beskrivs en RX-A3020 där
självdiagnosen gav **"BUS DSP1 NG", alltså IC911 på DIGITAL (1)**. Enheten fungerade vid
cirka 50 °C och slutade fungera vid cirka 45 °C. Lokal värme fick ljudet att komma tillbaka,
precis som hos dig. Samma mönster finns på RX-A2020, RX-A2010, RX-A3010 och RX-A3050.

Eftersom DSP-kretsen är en **QFP-176 och inte en BGA** är ett kretsbyte en vanlig
varmluftsreparation och inte ett BGA-jobb. Det är avgörande för ekonomin, se etapp 4.
Andra temperaturberoende orsaker som en tekniker bör mäta innan något byts:

- DSP:ernas kärn- och I/O-spänningar, kallt jämfört med varmt
- Kristall/oscillator till DSP:erna (kallstartsproblem)
- Torra elektrolytkondensatorer vid regulatorerna

---

## Etapp 2–3 – Marknadsgenomgång och jämförelsetabell

**Antaganden för importavgifter:**

- **Inom EU** (Tyskland, Polen): ingen tull. Privatsäljare tar ingen moms.
- **Utanför EU** (USA, Japan):
  - Vid **≤ 150 €** tar eBay eller AliExpress ut svensk moms (25 %) vid köpet, och ingen tull tillkommer.
  - Vid **> 150 €** tas 25 % moms på varuvärde plus frakt, plus eventuell tull (för reservdelar
    till ljud- och AV-utrustning normalt cirka 0–4 %, beroende på tulltaxenummer), plus
    transportörens hanteringsavgift (cirka 75–150 kr, *(est.)*).
- Frakt för en hel receiver (cirka 18–20 kg) från Tyskland med DHL Paket: cirka 500–660 kr *(est.)*.

### 3.1 Köpbara alternativ (status: ej verifierad i dag), sorterade efter total kostnad

| # | Produkt / artikelnr | Säljare | Skick | Testat? | Kompatibilitet | Pris | Frakt till Trollhättan | Moms/tull/avgift | **Totalt (SEK)** | Garanti/retur | Länk | Risk |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | **2 st DSP-kretsar D80YK113DPTP4** (QFP-176) till IC911 och IC920, *ingen kortreparation ingår* | Alibaba-säljare | Ny ("original" enligt säljaren) | Nej | Rätt krets enligt manualen (DPTP4 är en reviderad CPTP4) | 10–18 USD/st ≈ 200–360 kr | ca 100–250 kr *(est.)* | moms 25 % ≈ 75–150 kr | **≈ 400–750** *(enbart delar)* | Alibaba Trade Assurance | [Alibaba 1](https://www.alibaba.com/product-detail/D80YK113DPTP4-New-and-Original-Integrated-Circuit_1600835006019.html), [Alibaba 2](https://www.alibaba.com/product-detail/New-original-D80YK113DPTP400-D80YK113DPTP4-D80YK113CPTP4-integrated_1601056432979.html) | Medel: risk för förfalskade eller återvunna kretsar |
| 2 | Samma krets, D80YK113DPTP4 | eBay-säljare | Ny enligt säljaren | Nej | Som ovan | 29,70 USD/st ≈ 600 kr för 2 st (eller 56 USD för 2-pack ≈ 560 kr) | ingår ofta eller är låg *(est.)* | moms tas vid köp ≈ 140–150 kr | **≈ 700–850** *(enbart delar)* | eBay Money Back Guarantee | [eBay 358333384141](https://www.ebay.com/itm/358333384141), [eBay 177775571582 (2 st)](https://www.ebay.com/itm/177775571582), [eBay 358588752109](https://www.ebay.com/itm/358588752109) | Låg–medel |
| 3 | **Donator: RX-A3020, "lässt sich nicht einschalten"** | Privat, Kleinanzeigen (Dachau), indexerad 2026-09-15 | Defekt | Nej | Exakt modell, sannolikt EU-version | 80 € ≈ 884 kr | 500–660 kr *(est.)*, om säljaren skickar | Ingen (EU, privat) | **≈ 1 400–1 550** | Ingen (privatköp). Använd "Sicher bezahlen" | [Kleinanzeigen "yamaha 3020"](https://www.kleinanzeigen.de/s-yamaha-3020/k0) | **Hög**: digitalkortet är otestat. Ett nätdelsfel slår ofta inte ut digitalkortet, men det går inte att veta |
| 4 | **Donator: RX-A3020 "defekt / für Bastler oder Reparatur"** | Privat, Kleinanzeigen (Reutlingen), indexerad 2026-07-26/27 | Defekt, okänt fel | Nej | Exakt modell | 200 € ≈ 2 210 kr | 500–660 kr *(est.)* | Ingen | **≈ 2 700–2 900** | Ingen | [hifishark RX-A3020](https://www.hifishark.com/model/yamaha-rx-a-3020), [Kleinanzeigen RX-A3020](https://www.kleinanzeigen.de/s-yamaha-rx-a3020/k0) | **Hög**: felet kan vara samma DSP-fel som ditt (vanligt på modellen) |
| 5 | **ZC735700 "DIGITAL MAIN HDMI PCB FOR Yamaha RX-A3020"** | eBay **reddrag0n25** (USA, cirka 2 730 omdömen, 100 % positiva) | Begagnat, "Parts Only" | Inte angivet. Fråga säljaren | **Rätt artikelnummer**. Destination (U/C?) okänd, fråga säljaren | 163,93 € (ebay.de) / 198,10 CAD (ebay.ca) ≈ 1 810 kr | ca 250–450 kr *(est.)* | > 150 € ⇒ moms 25 % ≈ 520–570 kr, tull 0–4 % ≈ 0–90 kr, avgift 75–150 kr | **≈ 2 650–3 050** | eBay MBG täcker "not as described". "Parts only" ger svagt skydd mot funktionsfel | [eBay-sök ZC735700](https://www.ebay.com/sch/i.html?_nkw=ZC735700), [säljare](https://www.ebay.com/usr/reddrag0n25) | Medel: rätt kort men otestat, och en US-destination kan kräva omskrivning |
| 6 | **Fungerande RX-A3020** (hela enheten som donator) | Privat, Kleinanzeigen, indexerad 2026-10-11 | Begagnad, fungerande enligt annonsen | Säljarens uppgift | Exakt modell, EU | 370 € ≈ 4 090 kr | 500–660 kr *(est.)* | Ingen | **≈ 4 600–4 750** | Ingen (privat) | [hifishark RX-A3020](https://www.hifishark.com/model/yamaha-rx-a-3020) | Låg–medel: kortet är bevisat fungerande. Du får en hel reservenhet |
| 7 | Defekt RX-A3020 från USA ("For PART" / "AS/IS") | eBay USA (t.ex. 127323090363) | Defekt | Nej | US-version | 120–245 USD (begärda priser för liknande) ≈ 1 200–2 450 kr | 20 kg USA→SE ca 1 500–3 000 kr *(est.)* | moms 25 %, tull, avgift | **≈ 4 000–6 500** | Ingen retur | [eBay 127323090363](https://www.ebay.com/itm/127323090363) | Hög, och dyr frakt |
| 8 | RX-A3020 "ジャンク" (Japan) | Yahoo! Auctions / Hard Off | Defekt | Nej | **J-version, 100 V** | 12 000–24 000 ¥ ≈ 800–1 600 kr (Hard Off 33 000 ¥ ≈ 2 200 kr) | proxy plus EMS 20 kg ca 1 800–3 000 kr *(est.)* | moms 25 %, tull, avgift | **≈ 3 600–6 500** | Ingen | [Yahoo! avslutade](https://auctions.yahoo.co.jp/closedsearch/closedsearch/rx-a3020/0), [Hard Off](https://netmall.hardoff.co.jp/brand/1442/0001000100070001/) | **Mycket hög**: fel destination. Avråds |
| 9 | Fungerande RX-A3020 (Polen) | Privat, OLX.pl (Warszawa, Toruń m.fl.) | Begagnad | Säljarens uppgift | EU | 2 300–4 000 zł ≈ 6 000–10 400 kr | ca 400–700 kr *(est.)* | Ingen | **≈ 6 400–11 100** | Ingen (privat) | [OLX.pl RX-A3020](https://www.olx.pl/elektronika/sprzet-audio/q-Yamaha-RX-A3020/) | Låg risk, men dyrt |
| 10 | Fungerande RX-A3020 (Sverige, butik) | **Ljudshopen** | Begagnad | Butiken | Svensk/EU-version | 7 000 kr | inhämtas/okänd | Moms ingår | **≈ 7 000–7 500** | **Konsumentköplagen: 3 års reklamationsrätt, 14 dagars ångerrätt vid distansköp** | [Ljudshopen](https://www.ljudshopen.se/produkt/yamaha-rx-a3020-beg) | Lägst risk, men högst pris |
| – | **Nytt ZC735700 från Yamaha** | Yamaha-kanaler | – | – | – | – | – | – | **Ej köpbart (troligen utgånget)** | – | [UED-katalog](https://www.ued.net/catalog/cat_item_idx_8970.html) | – |

Rad 1–2 är **reparationsdelar och inte kretskort**. De finns med eftersom de sannolikt löser
ditt problem billigast, se etapp 4.

### 3.2 Avslutade annonser och historiska priser (endast referens)

| Objekt | Pris | Var/när | Kommentar |
|---|---|---|---|
| RX-A3020 AS/IS "does not produce any sound" | **155,98 USD** (sålt, tidigare 194,98) | eBay [335930743406](https://www.ebay.com/itm/335930743406), 29 juni (år okänt) | Prisnivå för en ljudlös enhet i USA |
| RX-A3020 "HDMI board YE540", for parts | **39,95 USD** (sålt) | [eBay.ie 192647281747](https://www.ebay.ie/itm/YAMAHA-RX-A3020-HDMI-BOARD-YE540/192647281747) | Oklart vilket kort det är, se 1.2 |
| RX-A3020 Kleinanzeigen, "HDMI-Platine defekt, Klang OK" | 150 € VB | Liebenburg, okt 2025 | Troligen såld. Var intressant som donator om "HDMI-Platine" var ett annat kort |
| Fungerande RX-A3020 Kleinanzeigen | 350–650 € | 2022–2025 | Ganderkesee, Bad Salzuflen, Konstanz, Böblingen m.fl. |
| RX-A3020 Blocket (via hifishark) | 4 000 kr (2019–20), 6 000 kr (2015–16) | Sverige | Gamla priser |
| RX-A3020 ジャンク Yahoo! Auctions | 6 750–24 000 ¥ | Japan | Medelpris för alla skick under 180 dagar: 34 640 ¥ |
| Nytt DIGITAL-2-kort (syskonmodellen RX-V2067) | ca 100 € | badcaps, cirka 2021 | Visar att Yamaha tidigare sålde digitalkort billigt. Gäller **inte** RX-A3020 |

---

## Etapp 4 – Rekommendation

### 1. Billigaste kompatibla kretskortet

- **Som kort:** ZC735700 från eBay-säljaren reddrag0n25, totalt cirka **2 650–3 050 kr**.
  Det är det enda kortet med verifierat artikelnummer. Be säljaren bekräfta **destination**
  (U eller C), om kortet är **testat**, och vilka **returvillkor** som gäller innan du köper.
- **Som donator:** Kleinanzeigen Dachau (80 €), cirka **1 400–1 550 kr**, men kortet är helt otestat.

### 2. Mest prisvärda funktionstestade kretskortet

Jag hittade **inget löst kort som uttryckligen är funktionstestat**. Det mest prisvärda
sättet att få ett *bevisat* fungerande digitalkort är en **fungerande RX-A3020 från
Kleinanzeigen för cirka 370 €**, totalt **≈ 4 600–4 750 kr**. Kontrollera den
vid kallstart före köp. Du kan sedan sälja din egen enhet, eller den andra enhetens
övriga delar, för att få tillbaka en del av kostnaden.

### 3. Säkraste köpet med hänsyn till garanti och returrätt

- **Ljudshopen, 7 000 kr** för en fungerande begagnad enhet med svensk konsumentlag.
  Det är tryggast men dyrt. Det är i praktiken en ny receiver och ingen reservdel.
- För själva kortet är **eBay (reddrag0n25) med eBay Money Back Guarantee** det säkraste
  alternativet, med en säljare som har 100 % positiva omdömen.

### 4. Billigaste lämpliga donatorreceivern

**Kleinanzeigen Dachau, 80 €** (startar inte, alltså troligen ett nätdelsfel) för totalt
cirka 1 400–1 550 kr. Nästa alternativ är Reutlingen för 200 €. Fråga säljaren:

- Startar den överhuvudtaget? Har den rökt eller luktat bränt?
- Kan den skickas med DHL?
- Har den visat ljud före felet?

En enhet som bara har tappat ljudet har sannolikt **samma DSP-fel som din** och är en
dålig donator.

### 5. Reparera ditt befintliga kort – troligen billigast och bäst

Felet är termiskt och har lokaliserats till DSP-området, och DSP:erna är QFP-176-kretsar.
Därför är **byte av IC911 och IC920 mot nya D80YK113DPTP4** sannolikt den bästa
vägen. En reflow ensam ger enligt flera forumrapporter ofta bara en tillfällig lösning.

| Verkstad | Ort | Pris enligt deras sida | Kommentar |
|---|---|---|---|
| **HiFi-Punkten** | Storgatan 39, **Trollhättan**, tel 0520-147 40 | Felsökning 400 kr, 600 kr/tim | Lokal. Reparerar "när tid finns" eftersom försäljningen går först. Fråga om de har varmluftsstation och vana vid QFP-byte. [Länk](https://www.hifi-punkten.se/sidor/laes-mer/service-och-installation) |
| **Current Repair Shop** | Mölndal | 550 kr/halvtimme exkl. moms, kostnadsförslag 700 kr exkl. moms | Kompetent men dyr (≈ 1 375 kr/tim inkl. moms). [Länk](https://www.currentrepairshop.se/) |
| **Vatansever Ljud** | Södra Sverige | Ej publicerat | Reparerar på komponentnivå, även modern hifi, med 3 månaders garanti. [Länk](https://vatanseverljud.se/) |
| **STT Media Service** | Göteborg, tel 031-491080 | Ej publicerat | Auktoriserad för flera AV-märken |
| Yamaha Sveriges servicepartners | – | – | [Yamaha servicecenter-lista](https://se.yamaha.com/sv/support/service-centers/). Auktoriserad verkstad har tillgång till servicemanualen och EEPROM-proceduren, men är ofta dyrare |

**Uppskattad kostnad för DSP-byte:** kretsar 400–850 kr, plus 2–3 timmars arbete på
HiFi-Punkten (400 kr felsökning plus 1 200–1 800 kr), totalt **≈ 2 000–3 100 kr**.

Detta förutsätter att teknikern först mäter matningsspänningar och klocka, så att det
inte är en regulator eller kondensator som är felet. Den reparationen är i så fall ännu billigare.

---

## Ekonomisk bedömning

| Läge | Värde |
|---|---|
| Fungerande RX-A3020, begagnat Sverige/Tyskland | ca **4 000–7 000 kr** (Kleinanzeigen 350–550 €, Ljudshopen 7 000 kr) |
| Defekt RX-A3020 | ca **900–2 200 kr** (80–200 €) |
| DSP-byte på eget kort | **≈ 2 000–3 100 kr** |
| Begagnat ZC735700-kort plus montering och eventuell EEPROM-inställning | **≈ 2 650–3 050 kr** plus 600–1 200 kr arbete ≈ **3 300–4 300 kr** |
| Köpa en fungerande RX-A3020 i stället | **≈ 4 600–4 750 kr**, minus andrahandsvärdet för din defekta enhet (≈ 900–2 000 kr) ≈ **netto 2 600–3 850 kr** |

**Bedömning:**

- Det är ekonomiskt försvarbart att reparera, så länge kostnaden håller sig **under cirka 3 000 kr**.
- **Förstahandsvalet** är ett DSP-byte, och eventuellt bara mätning och regulator- eller
  kondensatorbyte, på ditt befintliga kort hos en lokal verkstad.
- **Reservplanen** är att köpa en fungerande RX-A3020 i Tyskland och sälja den defekta enheten.
  Nettokostnaden blir ungefär densamma som för ett löst begagnat kort från USA, men risken är
  lägre eftersom du kan provlyssna och undviker frågan om destination och EEPROM.
- Ett löst begagnat ZC735700 från USA är bara vettigt om säljaren kan bekräfta att kortet är
  testat och vilken destination det har.

---

## Källor (urval)

- Badcaps – RX-A3020-tråd (IC911/DSP1, temperaturberoende): https://www.badcaps.net/forum/troubleshooting-hardware-devices-and-electronics-theory/troubleshooting-audio-equipment/89701-yamaha-rx-a3020
- Servicemanual RX-A3020/RX-V3073 (index): https://manualmachine.com/yamaha/rxa3020/3237109-service-manual/
- Servicemanual RX-A3010 (krav på extra inställning efter byte av DIGITAL1): https://manualmachine.com/yamaha/rxa3010/8215079-service-manual
- Servicemanual RX-A3070 (EEPROM, modell och serienummer): https://manualmachine.com/yamaha/rxa3070/14506074-service-manual/
- UED.net, Yamaha-katalog (ZC735700): https://www.ued.net/catalog/cat_item_idx_8970.html
- JustAnswer (ZC735700 ej tillgängligt): https://www.justanswer.com/home-theater-stereo/bn40h-no-sound-yamaha-rx-a3020-receiver-awhile-ago-just.html
- Fixya (D80YK113DPTP4 som reviderad krets): https://www.fixya.com/support/p6171343-yamaha_rx_a3020_rx_a3020_home_theater_re
- AVS Forum, RX-A1020/2020/3020-tråden s. 105 (DSP från TI, säljs inte av Yamaha): https://www.avsforum.com/threads/the-official-yamaha-aventage-rx-a1020-rx-a2020-and-rx-a3020-thread.1415108/page-105
- Yamaha firmware 1.98: https://usa.yamaha.com/support/updates/rx-a2020-3020-en.html
- hifishark RX-A3020: https://www.hifishark.com/model/yamaha-rx-a-3020
