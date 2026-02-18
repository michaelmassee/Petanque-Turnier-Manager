# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Petanque-Turnier-Manager is a **LibreOffice Calc Extension** (.oxt) for managing Petanque/Boule tournaments. It is written in Java and uses the LibreOffice UNO component model. The codebase and comments are primarily in German.

## Build Commands

The project uses Gradle with a helper script (`build-oxt.sh`) that temporarily disables a global `~/.gradle/init.gradle` to prevent Artifactory conflicts.

```bash
# Build OXT only (build-oxt.sh hardcodes buildOXT, does not pass arguments)
./build-oxt.sh
```

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
- **`sidebar/`** — Sidebar info panels

### Menu Configuration

Menu items are defined in XCU files under `registry/org/openoffice/Office/` (one per tournament system + config/download/stop).

### IDL / Add-in Interfaces

IDL files in `idl/` define the XGlobal interface for Calc functions. The `addin/` package contains the generated Java interface from IDL. IDL compilation is not automated in Gradle (see BUILD_ISSUES.md) — interfaces must be generated manually or the project should be migrated to a modern IDL-free approach.

## Testing

- **Framework**: JUnit 4, AssertJ, Mockito, PowerMock
- **Unit tests**: Algorithm and model tests run standalone with `./gradlew test`
- **UI tests**: Classes ending in `UITest` extend `BaseCalcUITest`, which launches a headless LibreOffice instance via `OfficeStarter` using the user's installed extension from `~/.config/libreoffice/4`. These tests require the extension to be installed first via `./gradlew reinstallExtension`.

## Known Build Issues

See `BUILD_ISSUES.md` for details on:
1. Global init.gradle blocking Maven access (use `build-oxt.sh`)
2. IDL-to-Java interface generation not automated in Gradle
