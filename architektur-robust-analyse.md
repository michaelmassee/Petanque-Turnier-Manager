# Architektur- & Robustheits-Analyse — Petanque-Turnier-Manager

## Kontext

Dieses Dokument beschreibt den aktuellen Architektur- und Robustheitsstand des Projekts und dient als kompakter Maßnahmenkatalog. Erledigte Arbeiten sind nur zusammengefasst; Detailhistorie früherer Umsetzungsblöcke ist bewusst entfernt.

Aktueller Projektstand: ca. 676 Java-Dateien in `src/main/java`, ca. 142 Test-/UITest-Klassen, Java 25, GitHub Actions CI, strikte Compiler-Lints für Produktivcode, SpotBugs-Bestandsaufnahme und NullAway/JSpecify-Pilot.

---

## 1. Architektur-Bewertung

**Stärken**
- **Saubere Schichtung**: Entry-Points -> SheetRunner -> Turniersystem-Logik -> Helper -> UNO-API.
- **Vertikale Modularität**: Die Turniersysteme sind weitgehend isoliert; Cross-Imports zwischen Systemen bleiben gering.
- **Threading-Disziplin**: `SheetRunnerKoordinator` serialisiert Sheet-Operationen und reduziert bekannte Race-Conditions.
- **Gute Basis-Absicherung**: Algorithmus-Tests, i18n-Vollständigkeitstests, Compiler-Lints, CI und erste statische Analyse sind vorhanden.
- **JSpecify als Nullness-Standard**: Neue Nullness-Annotationen sind vereinheitlicht; NullAway läuft bereits auf ausgewählten Paketen.

**Schwächen / Risiko-Quellen**
- **Sehr große Klassen**: `ProtocolHandler` (2141 LOC), `KoTurnierbaumSheet` (1632), `WebServerManager` (1188), `SheetHelper` (1076), `SpielrundeDelegate` (816). Lokale Komplexität und Änderungsrisiko bleiben hoch.
- **Breites Helper-Paket**: ca. 118 Helper-Java-Dateien; Kohäsion und interne Abhängigkeiten sind schwer überschaubar.
- **Null-Rückgaben bleiben verbreitet**: ca. 176 `return null`-Treffer in Produktivcode. Nicht alle optionalen APIs sind per JSpecify sichtbar.
- **SpotBugs ist noch Bestandsaufnahme**: `ignoreFailures = true`; Findings verhindern aktuell keine neuen Regressionen.
- **NullAway ist noch Pilot/Teilausbau**: aktiv für `helper.random`, `spielerdb.webview` und `comp.newrelease`, aber noch nicht für breite Helper- oder UI-Flächen.

---

## 2. Offene kritische Stellen

### Kritisch (P1)

Aktuell keine offenen P1-Punkte aus dieser Analyse bekannt.

### Mittel (P2)

1. **`ProtocolHandler` verkleinern**
   - Problem: 2141 LOC, viel Routing-/Dispatch-Logik in einer Datei.
   - Ziel: URL-/Command-Dispatch in eine Strategie- oder Registry-Struktur auslagern; pro Turniersystem bzw. Funktionsgruppe klar abgegrenzte Handler.

2. **`KoTurnierbaumSheet` entflechten**
   - Problem: 1632 LOC mit Mischung aus Layout, Datenaufbau und Formel-Erzeugung.
   - Ziel: Layout-Renderer, Daten-Builder und Formel-Generator trennen; Tests um die extrahierten Einheiten legen.

3. **SpotBugs verschärfen**
   - Problem: SpotBugs läuft, aber als Bestandsaufnahme mit `ignoreFailures = true`.
   - Ziel: CORRECTNESS-Findings zuerst triagieren, Baseline-Excludefilter aufbauen, danach `ignoreFailures = false` für "keine neuen Findings".

4. **NullAway/JSpecify ausweiten**
   - Problem: Nullness ist nur in Teilpaketen erzwungen.
   - Ziel: weitere Pakete schrittweise in `NullAway:AnnotatedPackages` aufnehmen, jeweils mit `package-info.java`/`@NullMarked` und gezielten `@Nullable`-Annotationen.

5. **UI-Tests in eigene GitHub Action heben**
   - Problem: Unit-CI existiert; UI-Tests laufen noch nicht als separater Workflow.
   - Ziel: `ui-tests.yml` mit `workflow_dispatch`, pfadbasiertem `pull_request`-Trigger und wöchentlichem Nightly-Lauf.

6. **Explizite Optionalität bei `return null`-APIs**
   - Problem: Caller erkennen Nullbarkeit oft nur durch Lesen der Implementierung.
   - Ziel: Bei Touches JSpecify-Annotationen ergänzen oder APIs auf `Optional`/nicht-null-Verträge härten, ohne große API-Flächen unnötig umzubauen.

### Kosmetisch (P3)

1. **Restlichen echten `catch (Throwable)` prüfen**
   - Aktueller bekannter Produktivtreffer: `InfoModal.java`.
   - Ziel: Wenn fachlich möglich auf `Exception` + `Error`-Rethrow umstellen; sonst bewusst dokumentieren.

2. **TODO/FIXME-Marker aufräumen**
   - Aktuell ca. 12 Treffer in Produktiv- und Testcode.
   - Ziel: Entfernen, konkretisieren oder mit Tracker-/Kontext-Verweis versehen.

---

## 3. Bereits umgesetzt

- `SheetRunner.koordinator` ist `volatile`; die Limitierung ist dokumentiert.
- Wichtige `catch (Throwable)`-Stellen wurden auf `Exception` + `Error`-Rethrow migriert; `LogUtil` verhindert Kontextverlust bei Exceptions ohne Message.
- `SheetMetadataHelper.leseScoreText` wurde intern auf Optional-Helper umgebaut; die öffentliche Signatur blieb kompatibel.
- `MainKonfigDialog.configPanelList` ist ein Instanzfeld und akkumuliert nicht mehr über mehrere Dialoginitialisierungen.
- Produktionscode kompiliert mit `-Xlint:all,-classfile,-this-escape -Werror`; Testcode mit Lints ohne `-Werror`.
- GitHub Actions CI läuft für Unit-Tests und SpotBugs-Report-Upload.
- SpotBugs ist integriert; `spotbugsTest` ist bewusst deaktiviert.
- NullAway/ErrorProne ist integriert, Standard-ErrorProne-Checks sind deaktiviert, NullAway läuft im JSpecify-Modus.
- Webserver-HTTP-/SSE-Verhalten ist mit `WebServerInstanzSmokeTest` abgesichert.
- Algorithmus-/Record-Lücken in FormuleX, Kaskade und Poule wurden durch zusätzliche Tests geschlossen.
- JSpecify (`org.jspecify.annotations.*`) ist als verbindlicher Nullness-Standard in `CLAUDE.md` festgelegt.

---

## 4. Empfohlene Reihenfolge

1. **SpotBugs-CORRECTNESS triagieren** und Baseline-Strategie festlegen.
2. **NullAway schrittweise ausweiten**, beginnend mit kleinen, gut testbaren Paketen.
3. **UI-Test-Workflow ergänzen**, getrennt von der schnellen Push-CI.
4. **`ProtocolHandler` refactoren**, sobald Menü-/Command-Routing ohnehin angefasst wird.
5. **`KoTurnierbaumSheet` refactoren**, sobald KO-Baum-Layout, Datenfluss oder Formeln geändert werden.
6. **Rest-`Throwable` und TODO/FIXME** per Boy-Scout-Rule bereinigen.

---

## 5. Kritische Dateien für nachfolgende Arbeiten

- `src/main/java/de/petanqueturniermanager/comp/ProtocolHandler.java`
- `src/main/java/de/petanqueturniermanager/ko/KoTurnierbaumSheet.java`
- `src/main/java/de/petanqueturniermanager/webserver/WebServerManager.java`
- `src/main/java/de/petanqueturniermanager/helper/sheet/SheetHelper.java`
- `src/main/java/de/petanqueturniermanager/helper/msgbox/InfoModal.java`
- `build.gradle`
- `.github/workflows/ci.yml`
- `config/spotbugs/exclude.xml`

---

## 6. Verifikation bei weiteren Maßnahmen

- Markdown-only-Änderungen: Datei komplett lesen und auf widersprüchliche Statusangaben prüfen.
- Build-/Analyse-Änderungen: `./gradlew test spotbugsMain -Dorg.gradle.java.home="$JAVA_HOME" --no-daemon`.
- NullAway-Ausweitung: `./gradlew compileJava -Dorg.gradle.java.home="$JAVA_HOME" --no-daemon` und gezielte Tests für berührte Pakete.
- UI-Workflow: lokal `./gradlew uiTests` bzw. in CI mit `xvfb-run --auto-servernum`.
- Struktur-Refaktoren: Vor/Nach-Vergleich mit `./gradlew test`; bei UI-nahen Änderungen zusätzlich passende UITests.
