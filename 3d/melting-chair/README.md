# Smältande stol – 3D-utskrift för Bambu Studio

![preview](preview.png)

| Fil | Innehåll |
|---|---|
| `melting_chair.3mf` | Öppna direkt i Bambu Studio (mm, Z upp, står på byggplattan) |
| `melting_chair.stl` | Samma modell som STL |
| `generate.py` | Parametrisk generator (SDF → marching cubes), kör om för att ändra detaljer |

**Mått:** ca 134 × 122 × 132 mm (B × D × H) · ca 170 cm³ solid volym · 500 k trianglar · vattentät, en enda kropp.

**Form:** sits och rygg är ett sammanhängande böjt skal (som en riktig plaststol) med 4 långa solfjäderformade ryggspår och 4 sitsspår. Ryggens överkant är bågformad och rullar mjukt ner i sluttande armstöd. Ryggribborna rinner ut över sitsen, framkanten är en tjock, hängande kant med få men tunga droppar (den längsta ca 3 cm), höger axel har sjunkit ihop, en smält sträng går mellan höger armstöd och sitsen och benen flyter ut i stora flikiga pölar.

## Rekommenderade inställningar (PLA, 0,4 mm munstycke)
- **Lagerhöjd:** 0,12 mm (Fine) – droppar och rundningar blir mjuka. 0,16 mm går också.
- **Väggar:** 3 · **Infill:** 10–15 % gyroid.
- **Stöd:** *Tree (auto)*, tröskelvinkel 30°. Behövs under sitsen, armstöden, ryggstödets bakkant och under varje dropps runda spets. Tillåt stöd på modellen (inte bara på plattan): dropparna under höger armstöd och axeln hänger ovanför sitsen.
- **Platta:** fötterna är platta ”pölar” med stor kontaktyta – ingen brim behövs normalt.
- **Glans som på bilden:** vit silk-PLA, eller slipa + spraya med blank klarlack.

Uppskalning: modellen har ≥ 3 mm tjocka detaljer, så den kan skalas ned till ~60 % utan problem.
