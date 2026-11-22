# Takkåpa med kabellås

Tunn kåpa (Ø120 mm invändigt, 70 mm djup, 1,8 mm vägg) som döljer takuttaget.
Kåpan hålls uppe mot taket av ett skruvbart kabellås (spännhylsa) i botten.

| Fil | Innehåll |
|---|---|
| `takkapa.scad` | Parametrisk OpenSCAD-modell (alla mått i början av filen) |
| `kapa.stl` | Kåpan – skrivs ut med botten på bädden, utan support |
| `kabellas.stl` | Kabellåset – skrivs ut med greppskivan på bädden, utan support |
| `snitt.png` | Snitt i monterat läge |

## Montering
1. Trä kabeln genom kabellåset (greppskivan nedåt mot lampan) och sedan genom kåpan underifrån – **innan** DCL-kontakten kopplas in.
2. Häng upp lampan i kroken och koppla in kontakten.
3. Skjut upp kåpan mot taket och skruva in kabellåset (medurs) tills kabeln sitter fast.

## Utskrift
PETG eller PLA, 0,2 mm lager, 3–4 perimetrar. Kåpan ~95 g, låset ~10 g.

## Anpassa
- `cable_d` – kabelns diameter (standard 7 mm, greppar ca 5,5–8 mm).
- `depth` – djup (standard 70, min 60).
- `wall` – väggtjocklek.
- `clear` – öka om gängan går trögt (t.ex. 0.45).
