# Takkåpa med plastskruv

Tunn kåpa (Ø120 mm vid taket, 78 mm hög, 1,8 mm vägg). Formen ligger mellan U och V:
raka sidor upptill som går mjukt över i en 45°-avsmalning mot en liten platt botten.
Kåpan låses på kabeln med en ihålig plastskruv med väldigt grov trapetsgänga
(Ø26, stigning 8 mm). När skruven dras åt pressas dess slitsade topp ihop runt kabeln.

| Fil | Innehåll |
|---|---|
| `takkapa.scad` | Parametrisk OpenSCAD-modell |
| `takkapa.3mf` | Färdig utskriftsplatta med båda delarna (220×220 mm), rätt vända |
| `kapa.stl` | Kåpan, redan vänd för utskrift: **takkanten på bädden**, ingen support |
| `skruv.stl` | Plastskruven: skallen på bädden, ingen support |
| `snitt.png`, `render.png` | Snitt i monterat läge / 3D-vy |

## Montering
1. Trä kabeln genom skruven (skallen nedåt mot lampan) och sedan genom kåpan, **innan** DCL-kontakten kopplas in.
2. Häng upp lampan i kroken och koppla in kontakten.
3. Håll kåpan mot taket och skruva in skruven tills kabeln sitter fast. Låset tar på ungefär det sista halva varvet.

## Utskrift
PETG eller PLA, 0,2 mm lager, 3–4 perimetrar. Kåpan ~95 g, skruven ~14 g.

## Anpassa
- `u_v` – 0 = mer U, 1 = mer V.
- `cable_d` – kabelns diameter (standard 7 mm, greppar ca 5,5–8 mm).
- `height` – total höjd.
- `pitch`, `thr_d` – gängans stigning/diameter. `clear` – öka om gängan går trögt.
