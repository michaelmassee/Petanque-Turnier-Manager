# Blattschutz im Turnier-Modus – Architektur

Der Turnier-Modus (Kiosk-Modus, `TurnierModus`) ist **nicht für die Darstellung** gedacht, sondern
für die vereinfachte **Ergebnis-Erfassung durch Laien**. Beim Aktivieren werden alle nicht dafür
benötigten Calc-Bedienelemente ausgeblendet – Menüleiste, Symbolleisten, Statusleiste und
Rechenleiste (`TurnierModus.STANDARD_ELEMENTE` / `setzeRechnerleiste`); nur die PTM-Toolbar bleibt
sichtbar. Zusätzlich werden die Sheets des aktiven Turniersystems tab-geschützt; ausschließlich die
für die Erfassung nötigen editierbaren Zellen (Name, SP, Spieltage, Ergebnisse) bleiben über
`CellProtection.IsLocked = false` bedienbar. Beim Deaktivieren werden UI-Elemente und Sheets wieder
freigegeben.

## Zentrale Klassen

| Klasse | Paket | Zweck |
|---|---|---|
| `TurnierModus` | `toolbar/` | Orchestriert das Aus-/Einblenden der UI-Elemente (Kiosk-Modus) und stößt den Blattschutz an |
| `IBlattschutzKonfiguration` | `helper/sheet/blattschutz/` | Interface – eine Impl. pro Turniersystem |
| `SheetSchutzInfo` | `helper/sheet/blattschutz/` | Record: Sheet + editierbare Bereiche |
| `BlattschutzManager` | `helper/sheet/blattschutz/` | Singleton-Orchestrator |
| `BlattschutzRegistry` | `helper/sheet/blattschutz/` | Registry (Open/Closed Principle) |
| `SupermeleeBlattschutzKonfiguration` | `supermelee/blattschutz/` | Supermelee-Implementierung |

`TurnierModus.aktivierenIntern()` / `deaktivierenIntern()` delegieren per Registry –
kein `if (SUPERMELEE)` nötig, neue Systeme nur per `BlattschutzRegistry.register()` eintragen.

## Pflicht-Reihenfolge beim Sperren (kritisch!)

1. `zelleStylesAktualisieren(ws)` – **vor** jedem `protect()`, sonst LO-RuntimeException
2. Sheet ggf. entsperren (`BlattschutzManager.entsperreSheet`, idempotent via `XProtectable.isProtected()`)
3. Editierbare Bereiche mit `CellProtection.IsLocked = false` freigeben
4. `XProtectable.protect("")`

## UNO-Hinweis: `CellProtection`

- Klasse: `com.sun.star.util.CellProtection` (nicht `sheet`!)
- Editierbar-Flag: **`IsLocked`** (nicht `IsProtected`)
- Immer **alten Wert lesen**, neues Objekt schreiben, alle Flags (`IsHidden`, `IsFormulaHidden`, `IsPrintHidden`) übernehmen

## Command-Scope (Lazy-Unprotect)

`BlattschutzManager` bündelt die Pflicht-Reihenfolge pro `SheetRunner`-Kommando in einem
thread-lokalen, referenzgezählten Scope, damit ein Kommando höchstens **ein** physisches Entsperren
und garantiert **ein** abschließendes Schützen auslöst:

| Methode | Zweck |
|---|---|
| `beginCommandScope(konfig, ws)` | Öffnet den Scope; entsperrt noch **nicht** |
| `ensureUnprotectedInScope()` | Entsperrt lazy beim ersten Bedarf (z. B. aus `ConditionalFormatHelper`, `RangeHelper.clearRange`/`setDataInRange`); weitere Aufrufe im selben Scope sind No-Ops |
| `endCommandScope()` | Schließt den äußersten Scope; führt **immer** ein abschließendes `doSchuetzen()` aus – auch wenn `ensureUnprotectedInScope()` nie gefeuert hat (deckt Doc-Struktur-Mutationen wie `NewSheet.forceCreate()` ab) |
| `scopeFuer(TurnierSystem, WorkingSpreadsheet)` | Convenience-`AutoCloseable` für Aufrufer außerhalb eines `SheetRunner` (z. B. modale Dialoge im `ProtocolHandler`-Pfad); No-Op wenn Turnier-Modus inaktiv oder kein Mapping registriert |
| `mitFallbackEntsperrt(sheet, Runnable)` | Physische Absicherung direkt am Schreibpunkt, falls der globale `TurnierModus.istAktiv()`-Flag (pro Prozess, nicht pro Dokument) vom tatsächlichen Sheet-Zustand abweicht |

Innerhalb eines aktiven Scopes sind die öffentlichen `schuetzen()`/`entsperren()`-Aufrufe No-Ops.
Außerhalb eines Scopes wirft `ensureUnprotectedInScope()` eine `IllegalStateException`, wenn der
Turnier-Modus aktiv ist – Style-/CF-Mutationen **müssen** also innerhalb eines `SheetRunner.run()`
bzw. eines `scopeFuer(...)`-Blocks laufen.

## Neues Turniersystem anschließen

1. `FooBlattschutzKonfiguration implements IBlattschutzKonfiguration` in `foo/blattschutz/` anlegen
   – Vorbild: `supermelee/blattschutz/SupermeleeBlattschutzKonfiguration.java`
2. In `BlattschutzRegistry` static-Block: `REGISTRY.put(TurnierSystem.FOO, FooBlattschutzKonfiguration.get())`
3. Editierbare Bereiche per `SheetMetadataHelper.findeSheet()` + `getSchluesselMitPrefix()` ermitteln
4. Zeilengrenzen: `MeldungenSpalte.MAX_ANZ_MELDUNGEN = 999` – keine Magic Numbers
5. Das Teilnehmer-Sheet muss **nicht** separat behandelt werden: `BlattschutzManager.mitGlobalenSchutzInfos()`
   sperrt es systemübergreifend automatisch vollständig, unabhängig von der eigenen `IBlattschutzKonfiguration`.

## Named Ranges – Pflichtregeln für Schlüssel

`XNamedRanges` sind dokumentweit – ein Schlüssel existiert genau einmal, unabhängig vom Sheet. **Zwingend für alle `__PTM_…__`-Schlüssel:** eindeutig im Dokument, sprachunabhängig (`__PTM_<SYSTEM>_<TYP>[_SUFFIX]__`-Namespace), sheet-namen-unabhängig (nicht vom angezeigten Titel ableiten). Schlüssel-Inhalt (`$'SheetName'.$A$1`) darf Sheet-Namen enthalten – LO aktualisiert automatisch bei Umbenennung.

### Lebenszyklus-Verhalten (an LO-Quelle und UITests verifiziert)

Die Verbindung Schlüssel→Blatt ist intern **index-basiert** (Tab-Index im Referenz-Token, nicht der Blattname). Daraus folgt, je Anwender-Aktion:

| Aktion | Verhalten | Begründung |
|---|---|---|
| **Rename** | Schlüssel bleibt korrekt am selben Blatt; Lookup über neuen Namen greift | Nur der Anzeige-Name wechselt, der Tab-Index bleibt. `sheetIndexAusNamedRangeObj` löst rein über die Zellreferenz auf, nie über den Namen. |
| **Verschieben** (Reihenfolge) | bleibt korrekt | LO zieht den Index via `UpdateInsertTab`/`UpdateMoveTab` nach (`sc/.../documen2.cxx` `CopyTab`, `rangenam.cxx`). |
| **Kopieren** | Kopie ist **schlüssellos**; Original behält den Schlüssel | `copyByName` dupliziert nur blatt-lokale Named Ranges, **nicht** die dokument-globalen. So entsteht kein Sidebar-Doppeleintrag. Achtung: ein fehlgeschlagener Duplikat-Namen-Copy lässt LO-intern (`MoveTable`) trotzdem ein auto-benanntes Blatt zurück. |
| **Blatt löschen** | **kein `#REF!`-Waise** – LO entfernt den abhängigen globalen Named Range selbst | empirisch im UITest bestätigt. Deshalb ist `bereinigeVerwaisteMetadaten` primär **defensiv** für Alt-Dokumente / Fremd-Manipulation, nicht für den normalen Lösch-Pfad. |
| **A1-Anker zerstören** (Spalte A / Zeile 1 löschen) | Name überlebt, Referenz wird `#REF!` → von `findeSheet`/Cleanup erkannt | einziger regulär reproduzierbarer Weg zu einem `#REF!`-Waise bei überlebendem Namen. |

**`#REF!` ist locale-unabhängig.** `XNamedRange.getContent()` rendert immer mit `GRAM_API` (feste, nicht lokalisierte Grammatik – LO `ScNamedRangeObj::getContent`, Symbol-Tabelle `RID_STRLIST_FUNCTION_NAMES_ENGLISH_API` mit hartkodiertem `"#REF!"`). In **keiner** Locale (DE/EN/FR/NL/ES) erscheint ein lokalisiertes `#BEZUG!` – das existiert LO-weit nur in Hilfe-Übersetzungen, nie als Formel-Symbol. Daher prüft `SheetMetadataHelper.istKaputteReferenz` ausschließlich gegen `"#REF!"`.

**Eindeutigkeit ist nur app-seitig erzwungen, nicht strukturell:** „höchstens ein Identitäts-Schlüssel pro Blatt" lebt im Schreib-Pfad (`entferneFremdeIdentitaetsSchluessel`) und in der Anzeige-Heilung (`SheetBaumOrganisierer`). Externe Mutationen (Alt-Dokumente, Fremd-Tools, manuelle Eingriffe im Named-Ranges-Dialog) können die Invariante verletzen.

Regressions-Tests: `SupermeleeTurnierTestDatenUITest` – `kopiertesBlattErzeugtKeinenDoppeltenIdentitaetsSchluessel`, `umbenanntesBlattBehaeltSeinenIdentitaetsSchluessel`, `kaputteReferenzLiefertRefFehlerUndWirdBereinigt`.

## Bedingte Formatierung (ConditionalFormat) und Sheet-Schutz – kritische LO-Einschränkung

**Regel**: `xPropSet.setPropertyValue("ConditionalFormat", xEntries)` ruft intern `ReplaceConditionalFormat` auf (`sc/source/ui/docshell/docfunc.cxx`). Bei tab-geschütztem Sheet kehrt die Methode **lautlos ohne Exception zurück** – aber LO hat zuvor bereits alle CF-Daten gelöscht → **alle bedingten Formatierungen verschwinden spurlos**.

**Vergleich mit der CellStyle-Einschränkung:**

| Operation | Verhalten bei Sheet-Schutz |
|---|---|
| `CellStyleHelper.apply()` (Styles) | Wirft `RuntimeException` → von `applyAufDokument` gefangen → WARN |
| `setPropertyValue("ConditionalFormat", ...)` | `return;` ohne Exception → lautlos, kein Log-Eintrag |

**Konsequenz für alle Methoden, die `ConditionalFormat` setzen (`ConditionalFormatHelper`, `RangeHelper.clearRange`/`setDataInRange`):**
Ist TurnierModus aktiv (Sheets sind geschützt), MUSS vor der Mutation der Blattschutz entfernt werden.
In der Praxis passiert das nicht mehr durch direktes `entsperren()`/`schuetzen()`, sondern über den
[Command-Scope](#command-scope-lazy-unprotect): Ein `SheetRunner`-Lauf öffnet den Scope einmalig,
die einzelnen Style-/CF-Operationen lösen darin nur noch das lazy `ensureUnprotectedInScope()` aus,
das abschließende Schützen passiert automatisch bei `endCommandScope()`.

```java
// Aktuelles Muster – z. B. ConditionalFormatHelper.setzeConditionalFormat() / RangeHelper.clearRange():
BlattschutzManager.get().ensureUnprotectedInScope();
// ... Style-/ConditionalFormat-Mutation ...
// Schützen NICHT hier – passiert automatisch am Ende des umschließenden
// BlattschutzManager.endCommandScope() (bzw. scopeFuer(...)-try-with-resources)
```

## `CellStyleHelper` – Überladung ohne ISheet

```java
// Für Kontexte ohne ISheet (z. B. BlattschutzManager):
CellStyleHelper.from(XSpreadsheetDocument doc, AbstractCellStyleDef def).apply();
```
