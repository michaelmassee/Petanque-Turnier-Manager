# Architektur- & Robustheits-Analyse — Petanque-Turnier-Manager

## Context

Anfrage: Architektur analysieren, kritische Stellen identifizieren, Vorschläge zur Steigerung der Robustheit. Es ist (noch) keine Implementierung gefragt — dieses Dokument ist Analyse + priorisierter Maßnahmenkatalog. Konkrete Umsetzungen folgen erst nach Auswahl durch den Nutzer.

Projektstand: 520 Java-Dateien, 107 Test-Klassen, Java 25, ohne CI, ohne statische Analyse. Architektur ist überdurchschnittlich sauber, aber einige systemische Schwachstellen gefährden Robustheit unter ungewöhnlichen UNO-Zuständen oder bei Erweiterungen.

---

## 1. Architektur-Bewertung (kompakt)

**Stärken**
- **Saubere Schichtung**: Entry-Points → SheetRunner → Turniersystem-Logik (Delegates) → Helper → UNO-API. Helpers (besonders `Lo.qi()`) erzwingen die CLAUDE.md-Regel "kein Java-Cast auf UNO" — **0 Verstöße** im gesamten Code.
- **Vertikale Modularität**: Jedes Turniersystem (`supermelee`, `liga`, `schweizer`, `ko`, `forme`, `jedergegenjeden`, `kaskade`, `poule`, `formulex`, `maastrichter`) ist isoliert; keine Cross-Imports zwischen Systemen.
- **Threading-Disziplin**: `SheetRunnerKoordinator` (`AtomicBoolean laeuft`, `volatile aktuellerRunner`) garantiert serielle Sheet-Operationen; `koordinatorVorgekoppelt`/`silentBackground` schließen dokumentierte Race-Conditions.
- **i18n vollständig**: 694 Keys in 5 Sprachen, alle Files synchron.
- **Algorithmen testabgedeckt**: `SchweizerSystemTest`, `SuperMeleePaarungenV2Test`, `CadrageRechnerTest`, `Direktvergleich(Result)Test`.
- **Ressourcen-Management gut**: try-with-resources flächendeckend, keine offensichtlichen Lecks.

**Schwächen / Risiko-Quellen**
- **Monolithische Sheet-Klassen** (>700 LOC): `ProtocolHandler` (1460), `KoTurnierbaumSheet` (1427), `WebServerManager` (1007), `SheetHelper` (981). Hohe lokale Komplexität, Test-Aufwand wächst nichtlinear.
- **Helper-Paket sehr breit** (108 Dateien): geringe Kohäsion, Querverbindungen zwischen Sub-Helpers schwer zu durchschauen.
- **Helper-Sub-Pakete ungetestet**: `helper/border`, `helper/cellstyle`, `helper/print`, `helper/random`, `helper/rangliste`, `helper/sheet/blattschutz`, `helper/sheet/io`, `helper/sheet/numberformat`, `helper/sheet/search`, `webserver/*`, `kaskade/*`, `poule/*`.
- **Keine statische Analyse**, keine CI — Regressionen werden nur lokal sichtbar.

---

## 2. Kritische Stellen (priorisiert)

> **Status (Commit `b4175e59`):** P1 #1–#4, P2 #9, P2 #10 und P3 #12 sind umgesetzt. Verbleibend: P2 #5–#8, P3 #11.

### Kritisch (P1)

1. ~~**`SheetRunner.koordinator` ist `static` ohne `volatile`**~~ — **erledigt**: Feld als `volatile` markiert mit Limitierungs-Kommentar (`SheetRunner.java:39–43`).

2. ~~**`Throwable`-Catches in 43 Stellen**~~ — **teilweise erledigt**: in `SidebarPanelDelegator`, `GlobalEventListener` und `SheetMetadataHelper` migriert auf `catch (Exception) { LogUtil.error/warn(...) } catch (Error e) { throw e; }` mit operationsspezifischen Kontext-Strings. Neuer Helper `helper/LogUtil.java` (mit Test) verhindert Logverlust bei `null`-Message und leerem Kontext.
   → **Verbleibend** (~30 Stellen außerhalb der drei Schlüsseldateien): per Boy-Scout-Rule bei künftigen Touches migrieren.

3. ~~**`leseScoreText()`-Null-Kette**~~ — **erledigt**: intern auf zwei private `Optional`-Helper aufgeteilt (`aufloeseNamedRangeAddresse`, `zelleAusAdresse`). Öffentliche Signatur (`String`/`null`) bewusst unverändert.

4. ~~**`MainKonfigDialog.configPanelList`**~~ — **erledigt**: `static` → `final` Instanz-Feld; behebt nebenbei den Akkumulations-Bug bei mehrfachem `initBox()` (vorher per Test-Workaround `.clear()` behandelt).

### Mittel (P2)

5. **Größe von `ProtocolHandler` (1460 LOC)** — vermutlich riesiger if-Kette über Menü-URLs. Jedes neue Menü-Item erhöht Risiko von Tippfehlern und unbehandelten Pfaden.
   → Dispatch-Map (URL → `Runnable`-Strategie); kann inkrementell erfolgen.

6. **Größe von `KoTurnierbaumSheet` (1427 LOC) + `SpielrundeDelegate` (717)** — Kombination aus Layout, Datenfluss, Formel-Erzeugung in einer Klasse. Tests dafür lückenhaft.

7. **`WebServerManager` (1007 LOC) ohne Tests** — exponiert REST-Endpoints aus dem Calc-Prozess. Sicherheits-/Stabilitätslücken hier wirken nach außen sichtbar (Listening-Port).
   → Mindestens Smoke-Test (Server hochfahren, `/health` o.ä. abfragen) + Input-Validation auf Endpunkten prüfen.

8. **`return null` an 138 Stellen, keine `@Nullable`-Annotationen** — Caller können Optionalität nicht erkennen.
   → Schrittweise JSpecify- oder JetBrains-`@Nullable` einführen, beginnend bei Helper-APIs.

9. ~~**`System.out.println` in Produktionscode**~~ — **erledigt**: `OfficeDocumentHelper.java:201` → `logger.warn(...)`.

10. ~~**`@Disabled`-Tests ohne klare Wieder-Aktivierungs-Strategie**~~ — **erledigt**: `EndranglisteSheetUITest` Capture-Helper umbenannt zu `captureJsonReferenceFiles` mit dokumentierter Begründung; `KoGruppeABSheetUITest` gelöscht (TODO-only mit blockierendem `waitEnter()`).

### Kosmetisch (P3)

11. **15+ TODO/FIXME-Marker** ohne Tracker-Verlinkung.

12. ~~**Auskommentierter Code**~~ — **erledigt**: `GlobalEventListener.onCreate`-Block (12 Zeilen) entfernt.

---

## 3. Maßnahmen-Katalog (vom kleinsten Hebel zum größten)

### A. Sofort, niedriger Aufwand, hoher Wert — **abgeschlossen** (Commit `b4175e59`)
- ~~**A1** `SheetRunner.koordinator` → `volatile`~~
- ~~**A2** `OfficeDocumentHelper.java:201` `System.out.println` → Logger~~
- ~~**A3** Auskommentierten Code in `GlobalEventListener` löschen~~
- ~~**A4** `MainKonfigDialog.configPanelList` → Instanz-Feld~~
- ~~**A5** `@Disabled`-Tests entweder mit Begründung versehen oder löschen~~

### B. Build/CI-Härtung (mittel)
- ~~**B1** `build.gradle`: `-Xlint:all -Werror` für Produktions-Source~~ — **erledigt**: aktiviert mit dokumentierten Suppressions für `classfile` (externe Lib), `dangling-doc-comments` (~60 Datei-Header), `this-escape` (case-by-case-Refactoring) und `deprecation` (Apache Commons StringUtils-Migration). `unchecked` und `serial` initial mitfixiert (`AbstractStore`, Properties-Klassen, Webserver-Exceptions). Test-Source nur Warnings, kein `-Werror`.
- ~~**B2** **SpotBugs** als Gradle-Plugin~~ — **erledigt**: `com.github.spotbugs:6.0.26` + SpotBugs-Tool 4.9.6 (Java-25-Bytecode-Support). Konfiguration in `build.gradle`, leerer Excludefilter unter `config/spotbugs/exclude.xml`. CI lädt Report als Artifact hoch (14 Tage). Aktuell `ignoreFailures = true` als Bestandsaufnahme. **Initial-Findings: 265** (161 MALICIOUS_CODE — überwiegend `EI_EXPOSE_REP/REP2`-Noise; 49 MT_CORRECTNESS; 30 STYLE; 27 PERFORMANCE; 21 BAD_PRACTICE; **9 CORRECTNESS**; 4 I18N). **Backlog**: CORRECTNESS-Findings einzeln triagieren, dann Excludefilter mit Baseline füllen und `ignoreFailures = false` setzen ("keine neuen Findings"). `spotbugsTest` deaktiviert (Mock-Noise).
- **B2-ErrorProne** **offen**: ErrorProne-Plugin (`net.ltgt.errorprone`) auf Java 25 aktuell wackelig — error-prone-core-Releases hinken JDK-Versionen meist 6–12 Monate hinterher. Geplant einführen, sobald stable Java-25-Support vermeldet (Tracking via [error-prone Issues](https://github.com/google/error-prone/issues)).
- ~~**B3** Minimale **GitHub-Action**~~ — **erledigt**: `.github/workflows/ci.yml` läuft `./gradlew test` auf Push & PR (master/main), inkl. JDK 25 (Temurin), Node 20, LibreOffice-SDK via apt und Gradle-Cache. Build-Status-Badge in allen 5 README-Sprachen ergänzt.
- **B3a** **UI-Tests in eigener GitHub-Action** (separater Workflow `ui-tests.yml`, getrennt vom Push-CI). Pragmatischer Mix aus drei Triggern:
   1. `workflow_dispatch` — manuell auslösbar
   2. `pull_request` mit `paths:` (`src/**`, `registry/**`, `idl/**`, `frontend/**`, `build.gradle`) — Gate vor dem Merge
   3. `schedule: '0 4 * * 1'` — wöchentlich Montagnacht; fängt Umgebungs-Drift (LO-apt-Update, Java-25-Patch) ohne tägliches Budget
   Setup zusätzlich nötig: `apt-get install libreoffice-dev xvfb`, `xvfb-run --auto-servernum -- ./gradlew uiTests -Dorg.gradle.java.home=$JAVA_HOME`. Risiken: Bridge-Timing fragiler als lokal; `BeispielturnierUITest`-NPE-Baustelle wäre laufender Roter; kompletter `runAllTests` ~30–60 min Runner-Zeit. Für Beispielturnier-Suite optional `continue-on-error: true` im nightly-Lauf.
- ~~**B4** Optional: NullAway mit Annotated-Default für `helper`-Paket~~ — **erledigt mit Pilot-Scope**: `net.ltgt.errorprone:4.1.0` + `error_prone_core:2.36.0` + `nullaway:0.12.1` + `jspecify:1.0.0` (compileOnly) eingebaut. Java 25 läuft trotz junger ErrorProne-Stack (kein Crash). Alle Standard-ErrorProne-Checks deaktiviert, nur **NullAway** scharf — `NullAway:JSpecifyMode=true`. Notausgang `-PdisableErrorProne=true`. **AnnotatedPackages = `de.petanqueturniermanager.helper.random`** als Pilot (1 Klasse, 0 Findings). **Backlog**: Wurzelpaket `helper` produzierte initial **~100 Findings** — schrittweise weitere Subpakete in die Annotated-List aufnehmen (komma-separiert), Findings dort jeweils per `@org.jspecify.annotations.Nullable` oder Refactoring auflösen.

### C. Robustheits-Refaktor (gezielt)
- ~~**C1** Helper `LogUtil` einführen + `catch (Throwable)` → `catch (Exception)`~~ — **teilweise erledigt** (`SidebarPanelDelegator`, `GlobalEventListener`, `SheetMetadataHelper`); restliche ~30 Stellen per Boy-Scout-Rule.
- ~~**C2** `SheetMetadataHelper.leseScoreText` Refactor~~ — **erledigt** (intern Optional, API unverändert). `Lo.requireQI` nicht eingeführt — durch Optional-Chain obsolet.
- ~~**C3** Mindest-Smoke-Test für `WebServerManager`~~ — **erledigt**: `WebServerInstanzSmokeTest` mit 6 Tests deckt Start/Stop, HTTP-Status-Codes (200/404/405) und JSON-Schema des `/debug/sse`-Diagnose-Endpunkts ab. Läuft ohne LibreOffice (StubResolver, freier OS-Port). `WebServerManager` selbst bleibt UNO-abhängig — getestet wird die innere `WebServerInstanz`, die das HTTP- und SSE-Handling kapselt.
- ~~**C4** Algorithmen-Tests ausweiten auf `forme/`, `kaskade/`, `poule/`~~ — **erledigt**: Bestandsaufnahme zeigte, dass die Kern-Algorithmen (`CadrageRechner`, `KoRundeTeamPaarungen`, `KaskadenKoFeldRechner`, `KaskadenKoRundenPlaner`, `PouleGruppenRechner`, `PouleRanglisteRechner`, `FormuleX`, `FormeSpielrunde`) bereits Tests haben. Lücken in den Daten-Records geschlossen mit drei neuen Test-Klassen: `PouleTeamErgebnisTest` (defensive Kopie, `gegnerNrn`-Ableitung), `KaskadenKoFeldInfoTest` (Cadrage-Factory, abgeleitete Methoden, 7-Teams-Edge-Case), `FormuleXErgebnisTest` (`istSieger`-Logik inkl. Freilos und Unentschieden, `punktedifferenz`).

### D. Strukturelle Maßnahmen (groß)
- **D1** `ProtocolHandler` aufteilen in URL-Strategie-Map; pro Turniersystem eine `*MenuStrategie`-Klasse. Reduziert die Datei auf ~200 LOC.
- **D2** `KoTurnierbaumSheet` in Layout-Renderer + Daten-Builder + Formel-Generator zerlegen.
- **D3** Helper-Paket nach Sub-Domänen schneiden: stabile API in `helper/api/`, Detail in `helper/internal/`. Nur explizite Pakete dürfen `internal` importieren (per ArchUnit-Test prüfbar).

---

## 4. Empfohlene Reihenfolge

1. ~~**Block A** (alles, ein Vormittag)~~ — **erledigt** (Commit `b4175e59`).
2. ~~**Block B1**~~ und ~~**Block B3**~~ erledigt; **Block B2** (SpotBugs/ErrorProne) als nächster Schritt.
3. **Block B2** (SpotBugs/ErrorProne): weitere Findings → in Backlog.
4. **Block C** in dieser Reihenfolge: ~~C1~~ → ~~C2~~ → C4 → C3.
5. **Block D**: nur wenn Erweiterungen anstehen, die die jeweilige Klasse ohnehin berühren (Boy-Scout-Rule).

---

## 5. Kritische Dateien für nachfolgende Arbeiten

- `src/main/java/de/petanqueturniermanager/SheetRunner.java` (P1: A1)
- `src/main/java/de/petanqueturniermanager/SheetRunnerKoordinator.java` (Kontext)
- `src/main/java/de/petanqueturniermanager/helper/sheet/SheetMetadataHelper.java` (C2)
- `src/main/java/de/petanqueturniermanager/helper/document/OfficeDocumentHelper.java` (A2)
- `src/main/java/de/petanqueturniermanager/comp/GlobalEventListener.java` (A3)
- `src/main/java/de/petanqueturniermanager/konfigdialog/MainKonfigDialog.java` (A4)
- `src/main/java/de/petanqueturniermanager/helper/Lo.java` (C2: neue Helper)
- `build.gradle` (B1–B4)
- *neu*: `.github/workflows/ci.yml` (B3)

---

## 6. Verifikation der Maßnahmen

- **Block A**: `./gradlew test` muss grün bleiben; manuelle Stichprobe `./gradlew reinstallExtension` + Calc starten + ein Turnier-System öffnen.
- **Block B**: `./gradlew check` läuft mit aktivierten Lints/SpotBugs lokal grün; CI-Job grün auf Probe-Push.
- **Block C1/C2**: bestehende Tests grün; neue Unit-Tests für `LogUtil` und `Lo.requireQI` (Failure-Pfad).
- **Block C3**: neuer Test `WebServerManagerSmokeTest` startet Server auf zufälligem Port und macht einen HTTP-Roundtrip.
- **Block D**: Vor/Nach-Vergleich per `./gradlew test`; zusätzlich UI-Test für betroffenes Menü-Item bzw. KO-Baum.

---

## 7. Offene Fragen an den Nutzer

Bevor wir implementieren, brauchen wir Richtung in folgenden Punkten:

1. Welcher Block soll als Erstes umgesetzt werden — **A** (Quick-Wins), **B** (CI/Lint-Setup) oder direkt ein gezielter **C**-Refactor?
2. Soll **GitHub Actions** angelegt werden, oder bleibt CI bewusst lokal?
3. ~~**NullAway/JSpecify** zustimmungspflichtig?~~ — **entschieden**: JSpecify (`org.jspecify.annotations.*`) ist verbindlicher Standard. Andere Annotation-Familien (JSR-305, JetBrains, Checker Framework) sind für neuen Code verboten. Konvention `@NullMarked` auf Paket-Ebene + `@Nullable` für Ausnahmen. Festgeschrieben in `CLAUDE.md` → "Code Style & Language".
4. Bei **D1/D2** (große Refactorings): Soll dies "nur wenn Klasse sowieso angefasst wird" gelten, oder als eigene Aufgabe geplant werden?
