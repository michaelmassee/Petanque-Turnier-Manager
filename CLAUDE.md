# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Petanque-Turnier-Manager is a **LibreOffice Calc Extension** (.oxt) for managing Petanque/Boule tournaments. It is written in Java and uses the LibreOffice UNO component model. The codebase and comments are primarily in German.

## LibreOffice Source Reference

The complete LibreOffice / LibreOffice Calc source code is available locally at **`/home/michael/devel/projects_massee/libreoffice`**.

```bash
git clone https://gerrit.libreoffice.org/core libreoffice
```

Use this reference to:
- Understand how LO processes XCU configuration files (e.g. `framework/source/fwe/classes/addonsoptions.cxx`)
- Look up internal UNO API behaviour (e.g. toolbar icon loading in `framework/source/uielement/toolbarmanager.cxx`)
- Investigate Calc-specific internals under `sc/`
- Verify how Add-on menus, toolbars and images are handled by the framework (`framework/`)

When debugging LO extension behaviour, **always check the LO source first** before guessing — the source is authoritative.

## Code Quality & Refactoring Rules

- **Strict Clean Code:** All code modifications and additions MUST strictly adhere to Clean Code principles. No exceptions.
- **Zero Warnings:** The resulting code must compile and run without ANY warnings. Proactively fix any warnings you encounter.
- **Replace Deprecated Code:** Any deprecated (`@Deprecated`) methods, classes, or APIs encountered in the modified context MUST be proactively migrated to their current, recommended alternatives.
- **Boy Scout Rule:** If you touch or modify an existing class, you MUST clean it up and refactor it to meet current quality standards (remove code smells, improve readability and structure).
- **Test Classes Included:** The strict Clean Code, zero warnings, and refactoring rules apply equally to all Test classes. Treat test code with the same care as production code.
- **Kein zellenweises Schreiben in Schleifen:** Schleifen, die einzelne Zellen nacheinander beschreiben, sind zu vermeiden. Stattdessen MUSS `RangeHelper` (zusammen mit `RangeData`/`RowData`) verwendet werden, um Daten als Block in einen Zellbereich zu schreiben.

## Build Commands

The project uses Gradle
Direct Gradle commands (when no conflicting global init.gradle exists):

```bash
./gradlew buildPlugin            # Build OXT and show output path
./gradlew buildOXT               # Create OXT extension
./gradlew test                   # Run all tests (unit only, UI tests need LibreOffice)
./gradlew test --tests "de.petanqueturniermanager.algorithmen.SchweizerSystemTest"  # Single test class
./gradlew reinstallExtension     # Uninstall + install extension in LibreOffice
./gradlew debugLibreOffice       # Reinstall and start LibreOffice with JDWP on port 5005
```

**Build output**: `build/distributions/PetanqueTurnierManager-1.0.0.oxt`

**Requirements**: Java 25 (configured via SDKMAN in gradle.properties), LibreOffice SDK jars from `/usr/lib/libreoffice/program/classes/`.

## Architecture

### This is a LibreOffice Extension, not a standalone app

The project produces an `.oxt` file (a ZIP containing JARs, XCU config files, images, IDL interfaces). It runs inside LibreOffice Calc, not as a standalone process. All UI interaction goes through the LibreOffice UNO API.

### Entry Points

- **`comp/PetanqueTurnierManagerImpl`** — XJobExecutor: handles menu actions from the "PétTurnMngr" menu in Calc
- **`comp/RegistrationHandler`** — UNO component registration (referenced in JAR manifest)
- **`addins/GlobalImpl`** — Calc add-in implementing `addin/XGlobal`: custom spreadsheet functions (`PTM.ALG.*`)
- **`SheetRunner`** — Abstract base for long-running tournament operations (one thread at a time)

### Tournament Systems

Each tournament type has its own package under `src/main/java/de/petanqueturniermanager/`:

| Package | Tournament Type |
|---------|----------------|
| `supermelee/` | Super Melee |
| `liga/` | League |
| `jedergegenjeden/` | Round-robin (Jeder gegen Jeden) |
| `schweizer/` | Swiss system |
| `forme/` | KO rounds / elimination |

Each tournament system typically has sheet classes for: Meldeliste (entry list), Spielrunde (game round), and Rangliste (ranking).

### Key Packages

- **`comp/`** — LibreOffice integration, startup, component lifecycle, `WorkingSpreadsheet`, `OfficeStarter`
- **`helper/`** — UNO API abstraction layer (SheetHelper, ColorHelper, MessageBox, Lo)
- **`model/`** — Data classes: Team, Spieler, TeamPaarung, TeamRangliste, SpielErgebnis
- **`basesheet/`** — Base classes for sheet types (meldeliste, spielrunde, konfiguration)
- **`algorithmen/`** — Core algorithms: SchweizerSystem, SuperMeleePaarungen, Direktvergleich, CadrageRechner
- **`addin/`** — Generated Java interface (`XGlobal`) from IDL; **`addins/`** contains the implementations (`GlobalImpl`, `AbstractAddInImpl`)
- **`konfigdialog/`** — Configuration UI dialogs
- **`sidebar/`** — Sidebar info panels (**DEAKTIVIERT** – buggy, muss komplett überarbeitet werden, siehe unten)

### Menu Configuration

Menu items are defined in XCU files under `registry/org/openoffice/Office/` (one per tournament system + config/download/stop).

**Node-Namen in XCU-Menüs müssen strikt sequenziell nummeriert sein.** Die dritte Stelle (der Zähler) darf **niemals** alphanumerische Suffixe enthalten (z.B. `B2A`, `A5A2S`). LibreOffice sortiert Nodes nach dem Namen — ein Suffix wie `A` in `B2A` führt zu falscher Anzeigereihenfolge. Bei jeder Änderung (Einfügen, Löschen) alle betroffenen Nummern neu durchnummerieren.

Separator-Nodes belegen einfach den nächsten freien Slot:
```xml
<node oor:name="A5" oor:op="replace">
    <prop oor:name="URL" oor:type="xs:string">
        <value>private:separator</value>
    </prop>
</node>
```
Falsch: `B2A`, `A5A2S`, `A5A4S`

### IDL / Add-in Interfaces

IDL files in `idl/` define the XGlobal interface for Calc functions. The `addin/` package contains the generated Java interface from IDL. IDL compilation is not automated in Gradle (see BUILD_ISSUES.md) — interfaces must be generated manually or the project should be migrated to a modern IDL-free approach.

## Testing

- **Framework**: JUnit 4, AssertJ, Mockito, PowerMock
- **Unit tests**: Algorithm and model tests run standalone with `./gradlew test`
- **UI tests**: Classes ending in `UITest` extend `BaseCalcUITest`, which launches a headless LibreOffice instance via `OfficeStarter` using the user's installed extension from `~/.config/libreoffice/4`. These tests require the extension to be installed first via `./gradlew reinstallExtension`.

## Known Build Issues

See `BUILD_ISSUES.md` for details on:
1. IDL-to-Java interface generation not automated in Gradle

## Sidebar – DEAKTIVIERT (buggy, funktioniert nicht)

Das `sidebar/`-Package wurde neu geschrieben (2026-03), funktioniert aber in LibreOffice **noch nicht korrekt** und ist deshalb deaktiviert:

- `PetanqueTurnierManager.components` → Factory-Eintrag auskommentiert → LibreOffice lädt die Sidebar nicht
- `registry/org/openoffice/Office/UI/UIElementFactoryManager.xcu` und `Sidebar.xcu` sind vorhanden, werden aber von LibreOffice 25.8 nicht korrekt verarbeitet → Panel-Inhalt wird nicht angezeigt
- `SidebarUITest` ist mit `@Disabled` markiert

**Bekannte Symptome:** Deck-Icon erscheint, Panel-Überschrift erscheint, aber kein Inhalt im Panel.

**Offene Ursache:** Die Factory-Registrierung via `UIElementFactoryManager.xcu` scheint in LO 25.8 nicht zu greifen. `createUIElement()` wird nicht aufgerufen.

**Regeln:**
- **Keine neuen Features in `sidebar/`** bis das grundlegende Problem gelöst ist.
- Bei Änderungen an globalen Komponenten (`NewReleaseChecker`, Events, etc.): **kein** Code in `sidebar/` einbeziehen.
- Sidebar erst wieder aktivieren (Factory in `.components` einkommentieren) wenn `createUIElement()` nachweislich aufgerufen wird.

## RanglisteRefreshListener – Architekturregeln

### Problem: Race Condition bei forceCreate()

`NewSheet.forceCreate().create()` **löscht** das bestehende Sheet und legt es neu an. Das triggert LibreOffice-interne Events (u.a. `selectionChanged`). Wenn der `RanglisteRefreshListener` dabei das Rangliste-Sheet als aktiv erkennt **und** `SheetRunner.isRunning() == false` ist (was bei direkten `doRun()`-Aufrufen immer der Fall ist, da diese den `SheetRunnerKoordinator` umgehen), startet der Listener sofort einen zweiten parallelen Thread → beide Threads schreiben gleichzeitig auf dasselbe Sheet → Datenverlust/Korruption.

### Regel: Listener müssen `*SheetUpdate`-Klassen verwenden

**NIEMALS** eine `*RanglisteSheet`-Klasse (Vollaufbau) direkt im `RanglisteRefreshListener` registrieren.
**IMMER** eine `*RanglisteSheetUpdate`-Klasse (nur Datenbereich) verwenden.

| Listener-Registrierung in `PetanqueTurnierMngrSingleton` | Klasse |
|---|---|
| Schweizer Rangliste | `SchweizerRanglisteSheetUpdate` |
| Maastrichter Vorrunden-Rangliste | `MaastrichterVorrundenRanglisteSheetUpdate` |

### Muster für neue Turniersysteme

Jedes neue Turniersystem, das einen `RanglisteRefreshListener` bekommt, benötigt eine `*SheetUpdate`-Klasse:

1. `FooRanglisteSheet` – vollständiger Aufbau (Menüaktion, Erstaufbau): verwendet `NewSheet.forceCreate()`
2. `FooRanglisteSheetUpdate extends FooRanglisteSheet` – Update-Pfad (Listener):
   - Überschreibt `doRun()`: **kein** `forceCreate`, kein Sheet-Event, keine Race Condition
   - Prüft ob Sheet existiert; falls nicht → Fallback auf `FooRanglisteSheet.doRun()` (Erstaufbau)
   - Schreibt nur den Datenbereich neu via `berechnungUndSchreiben()` (shared protected method)
   - Löscht überzählige Zeilen wenn Teamanzahl gesunken ist (`loeSchalteDatenzeilen`)
   - Registrierung in `PetanqueTurnierMngrSingleton` via `RanglisteRefreshListener.fuerSchluessel(..., (ws, ignored) -> new FooRanglisteSheetUpdate(ws))`

### `setActiveSheet()` – nur im SheetRunner-Kontext

`getSheetHelper().setActiveSheet(sheet)` **nur aufrufen wenn `SheetRunner.isRunning() == true`** (d.h. der Aufruf kommt vom Menü über `SheetRunner.run()`). Bei direktem `doRun()`-Aufruf (Listener, Test) darf `setActiveSheet` **nicht** aufgerufen werden – das würde erneut `selectionChanged` feuern.

## Business Logic & Rules
- **Schweizer System:** The complete ruleset for the Swiss tournament system in Petanque (including Buchholz, Feinbuchholz, Point Difference, and pairings) is documented in `turniersysteme/03_Schweizer.md`.
- **Supermelee System:** The complete ruleset for the tournament system in Petanque is documented in `turniersysteme/07_Supermelee.md`.
- **KO System:**  The complete ruleset for the tournament system in Petanque is documented in `turniersysteme/05_KO.md`.
- **Maastrichter System:**  The complete ruleset for the tournament system in Petanque is documented in `turniersysteme/04_Maastrichter.md`.
- **Kaskaden oder Erweitertes A-B-C-D System:**  The complete ruleset for the tournament system in Petanque is documented in `turniersysteme/06_Kaskaden-KO.md`.
- **Arena-Pétanque:** Documented in `turniersysteme/16_ArenaPetanque.md`.
- **Crazy-Mêlée:** Documented in `turniersysteme/15_CrazyMelee.md`.
- **Dänisches System:** Documented in `turniersysteme/11_DaenischesSystem.md`.
- **Formule 4:** Documented in `turniersysteme/13_Formule4.md`.
- **Kölner Sextet:** Documented in `turniersysteme/10_KoelnerSextet.md`.
- **Monrad-System:** Documented in `turniersysteme/12_MonradSystem.md`.
- **Tête-Series:** Documented in `turniersysteme/14_TeteSeries.md`.
- **Trip-Tête / Trio-System:** Documented in `turniersysteme/09_TripTete.md`.
- **Poule-AB:** Documented in `turniersysteme/02_Poule-AB.md`.
- **Formule_X:** Documented in `turniersysteme/08_Formule_X.md`.

### UNO API Strict Rules
- **NEVER use standard Java casts** for UNO interfaces (e.g., `(XSpreadsheetDocument) doc` is forbidden).
- **ALWAYS use `UnoRuntime.queryInterface()`**: `XSpreadsheetDocument doc = UnoRuntime.queryInterface(XSpreadsheetDocument.class, obj);`
- **Helper Usage:** Whenever possible, use the abstractions in `de.petanqueturniermanager.helper.Lo` instead of writing raw UNO boilerplate.

### Calc-Formeln in `SheetHelper.setFormulaInCell()` – immer englische ODF-Funktionsnamen

`SheetHelper.setFormulaInCell()` ruft intern `xCell.setFormula()` auf – das ist die **ODF-Formelsprache**, die **sprachunabhängig** und immer englisch ist. Das gilt für **alle Locales** (Deutsch, Französisch, Niederländisch, Spanisch, …) – lokalisierte Funktionsnamen führen in jeder Spracheinstellung zu `#NAME?`.

Eine kleine Übersetzungsliste (`FORMULA_GERMAN_SEARCH_LIST` in `SheetHelper`) übersetzt einige deutsche Namen automatisch – aber diese Liste ist unvollständig:

| Java-Code (deutsch, übersetzt) | ODF-Name |
|-------------------------------|---------|
| WENN | IF |
| ISTZAHL | ISNUMBER |
| ISTNV | ISNA |
| WENNNV | IFNA |
| ANZAHL | COUNT |
| ANZAHL2 | COUNTA |
| ZÄHLENWENN | COUNTIF |
| ISOKALENDERWOCHE | ISOWEEKNUM |

**Alle anderen Funktionen MÜSSEN direkt mit ODF-Namen geschrieben werden** – insbesondere:
- `VLOOKUP` (nicht `SVERWEIS`, nicht `RECHERCHEV`, nicht `VERT.ZOEKEN` o.ä.)
- Referenz-Syntax für Fremdblätter: `$'Sheetname'.$A$1:$B$999` (mit `$`-Prefix und Single Quotes)

Beispiel korrekt: `"VLOOKUP(" + nr + ";$'" + SheetNamen.meldeliste() + "'.$A$1:$B$999;2;0)"`

## Code Style & Language
- **Language:** All new class names, methods, variable names, JavaDoc, and inline comments MUST be in German to match the existing codebase.
- **UI Strings & i18n:** **EVERY string visible to the user** (sheet column headers, cell contents, comments, MessageBox texts, processbox messages, error messages, labels,menus, titles – anything the user can read) **MUST be managed via the i18n framework** using `I18n.get("key")`. This applies to all new code AND any previously hardcoded strings found during work. Translations must be added to all existing language files: `messages.properties` (DE/default), `messages_en.properties`, `messages_fr.properties`, `messages_nl.properties`, `messages_es.properties` in `src/main/resources/de/petanqueturniermanager/i18n/`. Never write a user-visible string literal directly into Java code.
- **Java Features:** The project uses Java 25. Feel free to use modern features like `var`, records, switch expressions, and text blocks where appropriate.

## Error Handling & Logging
- **Do not swallow exceptions:** Never write empty `catch` blocks.
- **User Feedback:** If an error occurs during a user action (e.g., in a menu command), catch the exception and display it to the user using the `de.petanqueturniermanager.helper.MessageBox` class.
- **Stacktraces:** Always log the stacktrace to the logger
