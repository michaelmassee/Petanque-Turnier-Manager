# Native Datengültigkeit (Calc-Gültigkeitsregeln)

Manuelle Eingabezellen bekommen zusätzlich zur roten Fehlerfarbe (bedingte Formatierung) eine
native Calc-Datengültigkeit. Sie zeigt beim Anwählen der Zelle eine Eingabehilfe und prüft die
Eingabe direkt beim Tippen. Zentrale Implementierung: `helper/sheet/DatengueltigkeitHelper`
(Zahlenbereiche, Auswahllisten, Bahn). Fachliche Aufrufer wie `MeldeListeHelper` übergeben nur
Werte und I18n-Schlüssel (`DatengueltigkeitHelper.Meldungen`).

## Übersicht

| Spalte | Erlaubt | Bei anderen Werten | Implementierung |
|---|---|---|---|
| Aktiv (Meldeliste) | leer, `1`, `2` (JGJ: leer, `1`) | **abgelehnt** (Stopp) | `MeldeListeHelper.setzeAktivDatengueltigkeit` → `setzeZahlenListe` |
| Spieltage (Supermelee) | leer, `1` (gespielt), `2` (ausgesetzt) | **abgelehnt** | `MeldeListeHelper.insertFormulaSpieltageSpaltenGeradeUngradeFarbe` |
| Setzposition (SP) | leer, ganze Zahl ≥ 0 | **abgelehnt** | `DatengueltigkeitHelper.setzeNichtNegativeGanzzahl` |
| Mêlée-Check-in | leer, `X` | **abgelehnt** | `MeldeListeHelper.setzeMeleeEingechecktDatengueltigkeit` → `setzeTextListe` |
| Spielergebnisse (Spielrunden, JGJ, KO-Turnierbaum, Trip-Tête) | leer, ganze Zahl 0–13 (Trip-Tête: 0–Spielziel) | **bleibt stehen**, rot markiert – **keine** Gültigkeitsregel, keine Eingabehilfe | nur bedingte Formatierung; `DatengueltigkeitHelper.entfernePruefungFuerSpielpunkte` räumt alte Regeln ab |
| Spielbahn (manuell, Modus „L“) | leer, positive ganze Zahl, **beliebiger Text** | nur **Warnung**, bestätigbar | `DatengueltigkeitHelper.setzeBahnDatengueltigkeit` |

Leere Zellen sind überall erlaubt (`IgnoreBlankCells`): leer bedeutet „nicht dabei“ bzw. „noch
kein Ergebnis“.

## Spielergebnisse: stehen lassen und rot markieren

Ergebniszellen haben bewusst **keine** Datengültigkeit. Ein ungültiges Ergebnis (z. B. `15`, `-1`,
Text) bleibt stehen, damit die Turnierleitung sieht, was eingetippt wurde; die bedingte Formatierung
(`styleIsFehler`: außerhalb 0–Spielziel, Text, Gleichstand) färbt die Zelle rot. Auch keine
Eingabehilfe: der Tooltip verdeckte beim schnellen Eintragen die Nachbarzeilen.
`DatengueltigkeitHelper.entfernePruefungFuerSpielpunkte` setzt die Regel auf `ValidationType.ANY`
zurück, damit Dokumente mit der früheren STOP-Regel beim Neuaufbau bereinigt werden. Jeder Aufrufer
MUSS selbst die Fehler-Formatierung setzen.

## Spielbahn: bewusst locker

Die Bahnspalte ist nur dann eine Eingabespalte, wenn die Bahnvergabe auf „leer, manuell“
(`SpielrundeSpielbahn.L`) steht. Die Turnierleitung trägt die Bahnen dann während des Turniers
selbst ein. Die Regel ist absichtlich großzügig:

- **Text ist erlaubt.** Viele Anlagen bezeichnen Bahnen mit Buchstaben oder Kombinationen
  („A3“, „1a“, „Platz 2“). Kein Code liest die manuell eingetragene Bahn als Zahl aus, Text ist
  daher gefahrlos.
- **Keine Eindeutigkeitsprüfung in der Gültigkeitsregel.** Eine harte Prüfung würde jeden Tausch
  zweier Bahnen blockieren: Beim Überschreiben der ersten Zelle wäre der Wert kurzzeitig doppelt.
  Doppelte Bahnen markiert stattdessen weiterhin die bedingte Formatierung rot.
- **Nur Warnung statt Ablehnung.** Werte wie `0`, negative Zahlen oder Kommazahlen sind
  ungewöhnlich, werden aber nach Bestätigung übernommen (`ValidationAlertStyle.WARNING`).

Formel (aktuelle Zelle per `INDIRECT(ADDRESS(ROW();COLUMN()))`):
`OR(ISBLANK(z); ISTEXT(z); AND(ISNUMBER(z); z=INT(z); z>0))`

**Nicht wieder verschärfen**, ohne die obigen Anwendungsfälle zu berücksichtigen. Abgesichert durch
`SchweizerLeereBahnSpalteUITest` (Warnstil, `ISTEXT`, kein `COUNTIF`).

## Technische Hinweise

- Programmatische Schreibvorgänge (`setDataArray`, `setValue`, Testdaten) umgehen die
  Datengültigkeit. Ältere oder per Code geschriebene ungültige Werte bleiben stehen und werden
  nur über die Fehlerfarbe sichtbar bzw. beim nächsten Aufbau bereinigt.
- Kann eine Regel nicht gesetzt werden, meldet `error.datengueltigkeit` die betroffene Spalte über
  einen übersetzten Namen (`datengueltigkeit.spalte.*`).
- Die Mêlée-Check-in-Spalte wird beim Blattaufbau normalisiert: jeder nicht leere Wert gilt wie
  bisher als eingecheckt und wird zu `X`. Geschrieben wird nur, wenn sich etwas ändert, und dann
  als Block.
- Eine Listen-Gültigkeit braucht eine Array-Konstante (`{1;2}`, `{"X"}`). `1;2` allein liest
  Calc als unvollständige Formel (Err:509).
- Auf geschützten Blättern verwirft LibreOffice Attributänderungen an **gesperrten** Zellen
  stillschweigend (`ApplyAttributesOperation`, `IsSelectionEditable`). Die Gültigkeitsregeln
  liegen auf entsperrten Eingabezellen und sind davon nicht betroffen.
