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
