# Blattschutz im Turnier-Modus – Architektur

Beim Aktivieren der Turnieransicht (`TurnierModus`) werden die Sheets des aktiven Turniersystems
tab-geschützt; editierbare Zellen (Name, SP, Spieltage, Ergebnisse) bleiben über
`CellProtection.IsLocked = false` bedienbar. Beim Deaktivieren werden die Sheets entsperrt.

## Zentrale Klassen

| Klasse | Paket | Zweck |
|---|---|---|
| `IBlattschutzKonfiguration` | `helper/sheet/blattschutz/` | Interface – eine Impl. pro Turniersystem |
| `SheetSchutzInfo` | `helper/sheet/blattschutz/` | Record: Sheet + editierbare Bereiche |
| `BlattschutzManager` | `helper/sheet/blattschutz/` | Singleton-Orchestrator |
| `BlattschutzRegistry` | `helper/sheet/blattschutz/` | Registry (Open/Closed Principle) |
| `SupermeleeBlattschutzKonfiguration` | `supermelee/blattschutz/` | Supermelee-Implementierung |

`TurnierModus.aktivierenIntern()` / `deaktivierenIntern()` delegieren per Registry –
kein `if (SUPERMELEE)` nötig, neue Systeme nur per `BlattschutzRegistry.register()` eintragen.

## Pflicht-Reihenfolge beim Sperren (kritisch!)

1. `zelleStylesAktualisieren(ws)` – **vor** jedem `protect()`, sonst LO-RuntimeException
2. Sheet ggf. entsperren (`entsperreSheetFallsNoetig`) – Idempotenz
3. Editierbare Bereiche mit `CellProtection.IsLocked = false` freigeben
4. `XProtectable.protect("")`

## UNO-Hinweis: `CellProtection`

- Klasse: `com.sun.star.util.CellProtection` (nicht `sheet`!)
- Editierbar-Flag: **`IsLocked`** (nicht `IsProtected`)
- Immer **alten Wert lesen**, neues Objekt schreiben, alle Flags (`IsHidden`, `IsFormulaHidden`, `IsPrintHidden`) übernehmen

## Neues Turniersystem anschließen

1. `FooBlattschutzKonfiguration implements IBlattschutzKonfiguration` in `foo/blattschutz/` anlegen
   – Vorbild: `supermelee/blattschutz/SupermeleeBlattschutzKonfiguration.java`
2. In `BlattschutzRegistry` static-Block: `REGISTRY.put(TurnierSystem.FOO, FooBlattschutzKonfiguration.get())`
3. Editierbare Bereiche per `SheetMetadataHelper.findeSheet()` + `getSchluesselMitPrefix()` ermitteln
4. Zeilengrenzen: `MeldungenSpalte.MAX_ANZ_MELDUNGEN = 999` – keine Magic Numbers

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

**Konsequenz für alle `doRun()`-Methoden die `formatDaten()` aufrufen:**
Ist TurnierModus aktiv (Sheets sind geschützt), MUSS vor jedem `upDateSheet()`-Aufruf der Blattschutz entfernt werden. `formatDaten()` ruft am Ende `schuetzen()` und stellt den Schutz selbst wieder her.

```java
// Pflichtmuster in doRun() / naechsteSpieltag() etc. wenn TurnierModus aktiv sein kann:
if (TurnierModus.get().istAktiv()) {
    BlattschutzRegistry.fuer(TurnierSystem.SUPERMELEE)
            .ifPresent(k -> BlattschutzManager.get().entsperren(k, getWorkingSpreadsheet()));
}
// ... clearRange(), upDateSheet() etc.
// formatDaten() → schuetzen() stellt Schutz am Ende wieder her
```

## `CellStyleHelper` – Überladung ohne ISheet

```java
// Für Kontexte ohne ISheet (z. B. BlattschutzManager):
CellStyleHelper.from(XSpreadsheetDocument doc, AbstractCellStyleDef def).apply();
```
