# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Petanque-Turnier-Manager is a **LibreOffice Calc Extension** (.oxt) for managing Petanque/Boule tournaments. It is written in Java and uses the LibreOffice UNO component model. The codebase and comments are primarily in German.

## LibreOffice Source Reference

LO source: **`/home/michael/devel/projects_massee/libreoffice`** — always check first before guessing. Key areas: `sc/` (Calc internals), `framework/` (menus/toolbars/Add-on), `framework/source/fwe/classes/addonsoptions.cxx` (XCU processing), `framework/source/uielement/toolbarmanager.cxx` (toolbar icons).

## Code Quality & Refactoring Rules

- **Strict Clean Code:** All code modifications and additions MUST strictly adhere to Clean Code principles. No exceptions.
- **Zero Warnings:** The resulting code must compile and run without ANY warnings. Proactively fix any warnings you encounter.
- **Replace Deprecated Code:** Any deprecated (`@Deprecated`) methods, classes, or APIs encountered in the modified context MUST be proactively migrated to their current, recommended alternatives.
- **Boy Scout Rule:** If you touch or modify an existing class, you MUST clean it up and refactor it to meet current quality standards (remove code smells, improve readability and structure).
- **Test Classes Included:** The strict Clean Code, zero warnings, and refactoring rules apply equally to all Test classes. Treat test code with the same care as production code.
- **Kein zellenweises Schreiben in Schleifen:** Schleifen, die einzelne Zellen nacheinander beschreiben, sind zu vermeiden. Stattdessen MUSS `RangeHelper` (zusammen mit `RangeData`/`RowData`) verwendet werden, um Daten als Block in einen Zellbereich zu schreiben.

## SpotBugs

Lauf: `./gradlew spotbugsMain` — Reports: `build/reports/spotbugs/main.{html,xml}`. Konfiguration in `build.gradle`, Filter in `config/spotbugs/exclude.xml`.

**Aktueller Stand: 0 Findings.** Alle Kategorien (CORRECTNESS, MT_CORRECTNESS, PERFORMANCE, BAD_PRACTICE, STYLE, I18N, MALICIOUS_CODE) sind triagiert — entweder per Fix oder per dokumentiertem Exclude in `exclude.xml`.

**Neue Findings:** Beim Hinzufügen/Ändern von Code MUSS `spotbugsMain` clean bleiben. Echte Bugs werden gefixt; nur bei begründeten False Positives wird ein neuer `<Match>`-Block in `exclude.xml` ergänzt mit klarer Begründung (warum FP, was die Pattern-Annahme verletzt). Niemals stillschweigend per `@SuppressFBWarnings` im Code suppress'en.

**Architekturweite Excludes** (akzeptierte Kategorie-/Klassen-Pauschalen, alle in `exclude.xml` begründet):
- **`MALICIOUS_CODE`** (komplette Kategorie): Defensive-Copy-Pflicht greift nicht für UNO-Remote-Proxies, geteiltes Domain-Modell und statische Property-Registries.
- **`CT_CONSTRUCTOR_THROW`** (7 Klassen): Kein Finalizer-Attack-Risiko in plugin-internem Code.
- **`EQ_COMPARETO_USE_OBJECT_EQUALS`** (`Spieler*Ergebnis`): `compareTo` definiert Sortierung, `equals` bleibt bewusst identity-basiert.
- **`UG_SYNC_SET_UNSYNC_GET`** + **`SING_SINGLETON_GETTER_NOT_SYNCHRONIZED`**: Felder sind `volatile`, Reads brauchen kein `synchronized`.
- **`ProtocolHandler.SHARED_CONTEXT`** + **`ProtocolHandler.isEnabled` (`REC_CATCH_EXCEPTION`)**: Beabsichtigter Cross-Instance-State bzw. defensiver Catch-all für Toolbar-Stabilität.

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
- **`sidebar/`** — Sidebar info panels (zeigt Plugin-Version als Label)

### Menu Configuration

Menu items are defined in XCU files under `registry/org/openoffice/Office/` (one per tournament system + config/download/stop).

**Jede neue oder umbenannte XCU MUSS in `META-INF/manifest.xml` als `<manifest:file-entry>` eingetragen werden.** Das Manifest ist statisch und wird nicht von Gradle generiert. Fehlt der Eintrag, wird die Datei zwar in die OXT gepackt und beim Install ins LO-Profil entpackt, aber LO ignoriert sie stillschweigend — der Menüpunkt erscheint einfach nicht. Symptom ist sehr verwirrend, weil Build, `reinstallExtension` und Datei-Inhalte alle korrekt aussehen. Bei `git mv` einer XCU also **immer** auch den `manifest:full-path`-Pfad anpassen; bei neuer Datei einen Block ergänzen; bei Löschung den Block entfernen.

**Node-Namen in XCU-Menüs müssen strikt sequenziell nummeriert sein.** Die dritte Stelle (der Zähler) darf **niemals** alphanumerische Suffixe enthalten (z.B. `B2A`, `A5A2S`). LibreOffice sortiert Nodes nach dem Namen — ein Suffix wie `A` in `B2A` führt zu falscher Anzeigereihenfolge. Bei jeder Änderung (Einfügen, Löschen) alle betroffenen Nummern neu durchnummerieren.

**Hintergrund:** Die Sortierung erfolgt in `configmgr/source/config_map.hxx` über `LengthContentsCompare` — primär nach **Stringlänge**, sekundär lexikographisch. Daher sortiert „B2A" (Länge 3) hinter alle Length-2-Slots wie „A1", „W1" etc., unabhängig von der lexikographischen Stelle. Konsequenz: einen Trenner zwischen `A0` und `A1` einzufügen ist nicht möglich, ohne *alle* anderen Top-Level-Slots derselben Submenu-Ebene auf gleiche Länge zu bringen — und selbst dann ist das Verhalten nicht stabil reproduzierbar (Versuch mit dreistelliger Normalisierung A00..A10/W10/X10..Y80/Z10/Z20 ließ in einem Test nur die `Y*`/`Z*`-Einträge sichtbar; Ursache nicht abschließend geklärt). **Pragmatik:** beim Original-Schema (zweistellig, A0..A9, W1, X1..) bleiben und auf Trenner zwischen den Top-Level-Slots verzichten.

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

### Reproduzierbare Zufallsdaten – `RandomSource`

Alle `Random`-/`ThreadLocalRandom.current()`-/`Collections.shuffle(list)`-Aufrufe im Produktivcode laufen über `de.petanqueturniermanager.helper.random.RandomSource`. Default-Pfad ist verhaltensgleich zu `ThreadLocalRandom`; im Test kann per Thread-lokal gesetztem Seed exakte Reproduzierbarkeit erzwungen werden.

**Regeln für neuen Code:**
- Niemals `new Random()` oder `ThreadLocalRandom.current()` direkt verwenden – immer `RandomSource.nextInt(...)` bzw. `RandomSource.asJavaRandom()`.
- `Collections.shuffle(list)` ist verboten – stattdessen `Collections.shuffle(list, RandomSource.asJavaRandom())`.

**Test-Pattern für E2E-`*TurnierTestDaten`-UI-Tests:**

```java
@BeforeEach @Override
public void beforeTest() {
    super.beforeTest();
    RandomSource.setSeed(42L);
}

@AfterEach
public void resetRandom() {
    RandomSource.reset();
}
```

Anschließend Sheets per `validateWithJson(rangeData, jsonFile)` (aus `BaseCalcUITest`) gegen JSON-Referenzdateien unter `src/test/resources/de/petanqueturniermanager/<system>/` validieren.

**JSON-Referenzen erfassen / aktualisieren:**
1. Im Test temporär `writeToJson(name, range, sheet, doc)` vor dem `validateWithJson(...)`-Aufruf aktivieren.
2. Test laufen lassen – die Datei wird in `$HOME` geschrieben.
3. Datei nach `src/test/resources/.../<package>/` kopieren.
4. `writeToJson(...)`-Aufruf wieder auskommentieren.
5. Bei mehreren JSONs pro Test: iterativ vorgehen, nach jedem Capture die JSON nach Resources kopieren und Test erneut laufen lassen (jeder Lauf erfasst eine weitere Datei, da der Test beim ersten fehlenden Reference-File abbricht).

Beispiele: `KoTurnierTestDatenUITest`, `MaastrichterTurnierTestDatenUITest`, `JGJTurnierTestDatenUITest`, `FormuleXTurnierTestDatenUITest`.

## Known Build Issues

See `BUILD_ISSUES.md` for details on:
1. IDL-to-Java interface generation not automated in Gradle

## Sidebar

Factory-Eintrag in `.components` aktiv. `InfoSidebarContent` zeigt nur die installierte Plugin-Version als einzelnes Label (via `ExtensionsHelper.getVersionNummer()`). `SidebarUITest` ist aktiv – setzt `reinstallExtension` voraus.

**Regeln:** Keine globalen Komponenten (`NewReleaseChecker` o.ä.) in der Sidebar verwenden. Sidebar-Inhalt minimal halten.


### Zellstile (CellStyles) und Sheet-Schutz – kritische LO-Einschränkung

**Regel**: `CellStyleHelper.apply()` wirft `RuntimeException` sobald **irgendein** Sheet tab-geschützt ist (LO: `sc/source/ui/unoobj/styleuno.cxx` `ScStyleObj::setPropertyValue_Impl`). Absichtliche LO-Einschränkung, kein Bug.


## Blattschutz im Turnier-Modus
Details und Implementierungsmuster: `turniersysteme/BLATTSCHUTZ.md`

**Kritische Regeln:**
- `zelleStylesAktualisieren(ws)` **vor** jedem `protect()` – sonst LO-RuntimeException
- `setPropertyValue("ConditionalFormat", ...)` bei aktivem Sheet-Schutz: **lautlos** gelöscht → vor `upDateSheet()` entsperren
- `CellProtection.IsLocked = false` (nicht IsProtected) für editierbare Bereiche; Klasse `com.sun.star.util.CellProtection`
- Neues System: `FooBlattschutzKonfiguration implements IBlattschutzKonfiguration` + `BlattschutzRegistry.register()`
- `CellStyleHelper.from(XSpreadsheetDocument, AbstractCellStyleDef).apply()` für Kontexte ohne ISheet

## RanglisteRefreshListener – Architekturregeln
Details und Muster: `turniersysteme/RANGLISTE_LISTENER.md`

**Kritische Regeln:**
- **NIEMALS** `*RanglisteSheet` direkt im Listener registrieren → Race Condition mit `forceCreate()`
- **IMMER** `*RanglisteSheetUpdate`-Klasse verwenden (nur Datenbereich, kein `forceCreate`)
- `setActiveSheet()` nur wenn `SheetRunner.isRunning() == true`

## Business Logic & Rules

Regeln für jedes Turniersystem sind in `turniersysteme/` dokumentiert:

| System | Datei |
|---|---|
| Schweizer System | `03_Schweizer.md` |
| Supermelee | `07_Supermelee.md` |
| KO | `05_KO.md` |
| Maastrichter | `04_Maastrichter.md` |
| Kaskaden / A-B-C-D | `06_Kaskaden-KO.md` |
| Arena-Pétanque | `16_ArenaPetanque.md` |
| Crazy-Mêlée | `15_CrazyMelee.md` |
| Dänisches System | `11_DaenischesSystem.md` |
| Kölner Sextet | `10_KoelnerSextet.md` |
| Monrad-System | `12_MonradSystem.md` |
| Tête-Series | `14_TeteSeries.md` |
| Trip-Tête / Trio | `09_TripTete.md` |
| Poule-AB | `02_Poule-AB.md` |
| Formule_X | `08_Formule_X.md` |

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
- **Nullness-Annotationen — Standard ist JSpecify** (`org.jspecify.annotations.*`). Andere Annotationen (`javax.annotation.Nullable` aus JSR-305, `org.jetbrains.annotations.*`, `org.checkerframework.*`) sind **verboten** für neuen Code. Konvention:
  - `@NullMarked` an `package-info.java` setzen, sobald ein (Sub-)Paket NullAway-clean ist; dann gilt dort *non-null by default*.
  - Innerhalb annotierter Pakete nur `@org.jspecify.annotations.Nullable` für Felder/Parameter/Rückgaben, die `null` sein dürfen.
  - Außerhalb annotierter Pakete: keine Annotationen — alter Code bleibt unangetastet, NullAway ignoriert ihn (`AnnotatedPackages` in `build.gradle` ist die Allowlist).
  - Aktuell annotiert: `de.petanqueturniermanager.helper.random`. Weitere Pakete iterativ.

## Error Handling & Logging
- **Do not swallow exceptions:** Never write empty `catch` blocks.
- **User Feedback:** If an error occurs during a user action (e.g., in a menu command), catch the exception and display it to the user using the `de.petanqueturniermanager.helper.MessageBox` class.
- **Stacktraces:** Always log the stacktrace to the logger
