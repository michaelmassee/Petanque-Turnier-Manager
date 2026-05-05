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

### Kritisch (P1)

1. **`SheetRunner.koordinator` ist `static` ohne `volatile`** — `SheetRunner.java:39`. Kommentar erlaubt Test-Austausch. Zwischen Tests und Produktivlauf besteht kein Memory-Barrier; in Edge-Cases kann ein zweiter Runner-Thread eine veraltete Referenz sehen. Mindestens `volatile` setzen, idealerweise auf Setter-Methode mit `synchronized` umstellen oder Test-Hook über Subklasse statt Feld-Mutation.

2. **`Throwable`-Catches in 43 Stellen** — Pattern: `catch (Throwable t) { logger.error(t.getMessage(), t); }`. Bei `t.getMessage() == null` (z.B. NPE) verliert die Logmeldung den Kontext. Beispiele: `SidebarPanelDelegator.java:49–51`, `SheetMetadataHelper.java:227`, `GlobalEventListener.java:91`. Außerdem: `Throwable` fängt `OutOfMemoryError` und `StackOverflowError` mit ab — meist unbeabsichtigt.
   → Empfehlung: helper `LogUtil.error(logger, "Kontext", t)` einführen, der `t.toString()` als Fallback nimmt; Catches bevorzugt auf `Exception` einengen.

3. **`leseScoreText()`-Null-Kette** — `SheetMetadataHelper.java:240–266`: 9× `if (x == null) return null` hintereinander. Schwer zu warten, Caller können Fehlerursachen nicht unterscheiden.
   → Refactoring zu `Optional<String>` oder eigenem Result-Typ; alternativ Helper `Lo.chain(...)`.

4. **`MainKonfigDialog.configPanelList`** — `static List<ConfigPanel>` als plain `ArrayList`. Wenn der Konfig-Dialog je aus einem zweiten Kontext geöffnet würde (Listener-Re-Entry), entstehen ConcurrentModificationExceptions. Heute selten, aber latent.
   → `CopyOnWriteArrayList` oder Instanz-Feld.

### Mittel (P2)

5. **Größe von `ProtocolHandler` (1460 LOC)** — vermutlich riesiger if-Kette über Menü-URLs. Jedes neue Menü-Item erhöht Risiko von Tippfehlern und unbehandelten Pfaden.
   → Dispatch-Map (URL → `Runnable`-Strategie); kann inkrementell erfolgen.

6. **Größe von `KoTurnierbaumSheet` (1427 LOC) + `SpielrundeDelegate` (717)** — Kombination aus Layout, Datenfluss, Formel-Erzeugung in einer Klasse. Tests dafür lückenhaft.

7. **`WebServerManager` (1007 LOC) ohne Tests** — exponiert REST-Endpoints aus dem Calc-Prozess. Sicherheits-/Stabilitätslücken hier wirken nach außen sichtbar (Listening-Port).
   → Mindestens Smoke-Test (Server hochfahren, `/health` o.ä. abfragen) + Input-Validation auf Endpunkten prüfen.

8. **`return null` an 138 Stellen, keine `@Nullable`-Annotationen** — Caller können Optionalität nicht erkennen.
   → Schrittweise JSpecify- oder JetBrains-`@Nullable` einführen, beginnend bei Helper-APIs.

9. **`System.out.println` in Produktionscode** — `OfficeDocumentHelper.java:201`. Kosmetisch, aber Symptom: kein Lint blockt das.

10. **3 `@Disabled`-Tests ohne klare Wieder-Aktivierungs-Strategie** — `EndranglisteSheetUITest:154`, `KoGruppeABSheetUITest:21`. Tendenz: weiterer Test-Verfall.

### Kosmetisch (P3)

11. **15+ TODO/FIXME-Marker** ohne Tracker-Verlinkung.
12. **Auskommentierter Code** (`GlobalEventListener.java:174–183`).

---

## 3. Maßnahmen-Katalog (vom kleinsten Hebel zum größten)

### A. Sofort, niedriger Aufwand, hoher Wert
- **A1** `SheetRunner.koordinator` → `volatile` (1 Zeile). **P1.**
- **A2** `OfficeDocumentHelper.java:201` `System.out.println` → Logger.
- **A3** Auskommentierten Code in `GlobalEventListener` löschen.
- **A4** `MainKonfigDialog.configPanelList` → Instanz-Feld oder `CopyOnWriteArrayList`.
- **A5** `@Disabled`-Tests entweder mit Begründung versehen oder löschen.

### B. Build/CI-Härtung (mittel)
- **B1** `gradle.properties` / `build.gradle`: Compiler-Flags `-Xlint:all -Werror` für Produktions-Source.
- **B2** **SpotBugs** + **ErrorProne** als Gradle-Plugins. Initial-Lauf zur Bestandsaufnahme; Schwelle einfrieren ("keine neuen Findings").
- **B3** Minimale **GitHub-Action**: `./gradlew test` auf jeden Push. Build-Status-Badge in README.
- **B4** Optional: NullAway mit Annotated-Default für `helper`-Paket.

### C. Robustheits-Refaktor (gezielt)
- **C1** Helper `LogUtil.errorMitKontext(logger, kontextMsg, throwable)` der `t.toString()` als Fallback nimmt; `catch (Throwable)` → `catch (Exception)` wo möglich. Iterativ einführen, beginnend bei `helper/sheet/*`.
- **C2** `SheetMetadataHelper.leseScoreText` zu `Optional<String>` migrieren; Helper `Lo.requireQI(class, obj)` für Aufrufer-Code, der eine UNO-Schnittstelle erwartet (wirft mit aussagekräftiger Meldung, wenn `null`).
- **C3** Mindest-Smoke-Test für `WebServerManager` (Start/Stop, eine Anfrage, JSON-Schema-Check).
- **C4** Algorithmen-Tests ausweiten auf `forme/`, `kaskade/`, `poule/` (analog zu `SchweizerSystemTest`).

### D. Strukturelle Maßnahmen (groß)
- **D1** `ProtocolHandler` aufteilen in URL-Strategie-Map; pro Turniersystem eine `*MenuStrategie`-Klasse. Reduziert die Datei auf ~200 LOC.
- **D2** `KoTurnierbaumSheet` in Layout-Renderer + Daten-Builder + Formel-Generator zerlegen.
- **D3** Helper-Paket nach Sub-Domänen schneiden: stabile API in `helper/api/`, Detail in `helper/internal/`. Nur explizite Pakete dürfen `internal` importieren (per ArchUnit-Test prüfbar).

---

## 4. Empfohlene Reihenfolge

1. **Block A** (alles, ein Vormittag): unmittelbare Risiken weg, niedriges Regressions-Risiko.
2. **Block B1+B3** (CI + Lint): liefert Sicherheitsnetz für alles Weitere.
3. **Block B2** (SpotBugs/ErrorProne): weitere Findings → in Backlog.
4. **Block C** in dieser Reihenfolge: C1 → C2 → C4 → C3.
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
3. **NullAway/JSpecify** zustimmungspflichtig? (Annotation-Style legt Stil-Standard für lange Zeit fest.)
4. Bei **D1/D2** (große Refactorings): Soll dies "nur wenn Klasse sowieso angefasst wird" gelten, oder als eigene Aufgabe geplant werden?
