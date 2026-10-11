# UUID statt Teamnummer als Team-Identität

## Arbeitsstand (2026-10-11)

Branch `ptm-online-2`, Haupt-Checkout (Worktree entfernt).

| Schritt | Stand | Commit |
|---|---|---|
| 1. `PTM.ALG.TNRUUID` / `PTM.ALG.UUIDZEILE` | erledigt | `d42ad54f` |
| 1. `PTM.ALG.KOSIEGER` | offen (mit KO-Umbau) | |
| 2. UUID in der Nr-Zelle, Zusatzspalte entfernt | erledigt | `82c9d1cd`, `c9190752` |
| 2. Nr-Spalte sperren + Menüpunkt „Teamnummer ändern“ | **offen** | |
| 3. Formelbauer über `TeamSchluessel` (`Uuid` / `Nr` als Übergang) | erledigt in `MeldeListeHelper`, `TeamAnzeigeHelper` | `7bba57a8` (WIP) |
| 4. Schweizer/Maastrichter-Vorrunde | Runden schreiben UUID-Formeln, keine technischen Spalten mehr; `SchweizerTeamNrAufloeser` (UUID → Zahl → technische Spalte → Anzeige-Index), `SchweizerRundenLeser`, `LiveZellbereich` lesen die UUID | `7bba57a8` (WIP) |
| 4. Schweizer-UITests | `SchweizerSpielrundeNamenLiveUpdateUITest` (neu: Nummerntausch in der Meldeliste ändert nur die Anzeige; Fallback technische Spalten) und `MaastrichterVorrundeNamensanzeigeUITest` umgestellt; Pakete `schweizer`, `maastrichter`, `ptmonline`: 154/155 grün | siehe Log |
| 4. JGJ, Liga, TripTete, FormuleX, Kaskade, KO, Poule, Supermelee, PTM-Online-Leser | offen | |
| 5. Migration bestehender Dokumente | offen | |

Neue Klassen: `TeamUuidFormel` (Parser), `MeldelisteUuidIndex` (UUID ↔ Nr), `TeamSchluessel` (Formelausdruck je Schlüsselart), `NrUuidSpalte`, `TeamNummernPlan`/`TeamNummernVergabe`.

Offen, nicht UUID-bezogen: `PtmOnlineAbgleichSheetRunnerUITest.rundenstartMeldetLokaleSetzpositionStattTeamNr` scheitert reproduzierbar mit „Style/CF-Operation außerhalb eines aktiven BlattschutzScopes“ (`PtmOnlineSyncSheet.schreibeKopf` aus der Testhilfe ohne SheetRunner bei aktivem Turnier-Modus). Vermutlich seit Master-Merge `792bfa3a` (Turnier-Modus pro Dokumentfenster).

Nächste Schritte: „Teamnummer ändern“ (Schritt 2), danach die übrigen Systeme.

Entscheidungen „Teamnummer ändern“ (2026-10-11):
- **Ein** systemübergreifender Menüeintrag (Bereich Spieler-DB/Meldeliste, `Addons_A0_SpielerDb.xcu`), erkennt das aktive Turniersystem selbst; kein Eintrag je Systemmenü, kein Toolbar-Button.
- Nr-Spalte nur im Turnier-Modus gesperrt (bestehendes Blattschutz-Konzept). Außerhalb davon bleibt sie offen; eine überschriebene Nr bekommt beim nächsten Meldeliste-Update eine neue UUID, bestehende Runden verlieren dann den Bezug zu dieser Meldung. Keine Heilung über gesicherte Zuordnung.

## Kontext

Heute ist die Teamnummer (Spalte A der Meldeliste, bei Supermelee die Spielernummer) überall die Identität:
- im Java-Modell (`NrComparable.equals/hashCode`),
- in den Zellen von Spielrunden, Spielplänen, KO-Bäumen und Ranglisten,
- in allen Namensformeln (`MATCH(nr; Meldeliste.$A$1:$A$999)`).

Ändert man eine Nr, zeigen bestehende Runden still auf ein anderes Team.

Die lokale PTM-Online-UUID steht zusätzlich in einer ausgeblendeten Zusatzspalte. Diese Spalte verursacht viel Aufwand:
- Sie muss in jeden Sortier- und Leerbereich (`MeldelisteSortBereich`).
- Der Adapter muss sie per Spaltensuche finden.
- Beim Supermelee-Spieltagwechsel wandert sie; das erfordert Sichern und Wiederherstellen in `MeldelisteZielFactory`.

**Ziel:**
- Die UUID steckt in der Nr-Zelle selbst: `=PTM.ALG.TNRUUID(7;"<uuid>")` zeigt 7 an.
- Die Zusatzspalte entfällt.
- **Schlüssel ist überall die UUID.** Runden, Ranglisten und der PTM-Online-Sync speichern UUIDs; die Nr ist nur Anzeige.
- Die Nr ist nur über einen Menüpunkt änderbar, danach stimmt alles weiter.

Entscheidungen des Users:
- Die Liste UUID → Nr → Zeile → Namen baut Java bei Bedarf (kein Seiteneffekt-Cache in der Formel).
- Spielrunden und Ranglisten werden gleich mit umgestellt.
- Umnummerieren nur per Menüpunkt; die Nr-Spalte bleibt gesperrt.

## Kernmechanik

1. **`PTM.ALG.TNRUUID(nr; uuid)` → nr.** Eine reine Anzeigefunktion. Die Zeile ist kein Parameter, weil die Position der Zelle sie ohnehin bestimmt und Sortieren sie mitnimmt.
2. **`PTM.ALG.UUIDZEILE(uuid; Meldeliste.$A$1:$A$999)` → Zeilenindex im Bereich, sonst `#N/A`.**
   - Der Parameter ist vom Typ `com.sun.star.table.XCellRange`. Belegt im LO-Quellcode: `sc/source/core/tool/interpr4.cxx:3051`; der Bereich wird aus dem aufrufenden Dokument erzeugt, das löst die Mehr-Dokument-Frage.
   - Die Funktion liest per `XCellRangeFormula.getFormulaArray()` die Nr-Formeln und sucht die UUID.
   - Calc verfolgt den Bezug als Abhängigkeit: Umnummerieren oder Sortieren berechnet die abhängigen Formeln neu.
   - Der Bereichsparameter ist Pflicht und wird von den Formelbauern automatisch geschrieben. Gründe:
     - Ohne Bezug kennt Calc die Abhängigkeit nicht. Es gäbe keine Neuberechnung nach Umnummerieren; die Alternative wäre eine volatile Funktion, die bei jeder Änderung rechnet.
     - Ohne Bereich hätte die Funktion keinen Dokumentbezug (fokus-basiert = verboten).
     - Ohne Bereich müsste der lokalisierte Blattname fest im Code stehen.
   - Damit wird in allen Formelbauern `MATCH(nr;$A$1:$A$999;0)` zu `PTM.ALG.UUIDZEILE("<uuid>";$A$1:$A$999)`. Im Anzeigemodus NR gilt `INDEX($A$1:$A$999; UUIDZEILE(…))`.
3. **Keine versteckten Spalten, weder in der Meldeliste noch in Runden oder Ranglisten.** Jede Teamzelle trägt ihre UUID als Literal in der eigenen Formel:
   - Anzeige Nr: `=INDEX(Meldeliste.$A$1:$A$999; PTM.ALG.UUIDZEILE("<uuid>"; Meldeliste.$A$1:$A$999))`.
   - Anzeige Name: dieselbe Zeile über die Formelbauer (`PTM.ALG.TEAMANZEIGE`).
   - Java liest die UUID per `getFormulaArray` und UUID-Regex aus der Zelle. Gemeinsamer Parser: `TeamUuidFormel.findeUuid(String formel)`.
   - **Weitergabe ohne Hilfsspalte** (KO-Sieger, Poule-Sieger/Verlierer): `PTM.ALG.KOSIEGER(teamA; teamB; punkteA; punkteB; verlierer)` ersetzt `IF(scoreA>scoreB; …)` und liefert die UUID des Siegers (bzw. mit `verlierer=1` des Verlierers), `#N/A` ohne Ergebnis.
     - Alle vier Zellen kommen als `XCellRange`; die Abhängigkeiten werden also verfolgt.
     - Die Teamzellen werden **rekursiv** aufgelöst: Enthält ihre Formel ein UUID-Literal, ist das die UUID. Ist sie selbst eine Siegerzelle, liest die Funktion deren `KOSIEGER`-Argumente (Zellbezüge im Formeltext, über `XSheetCellRange.getSpreadsheet()` aufgelöst) und steigt ab. Die Tiefe entspricht der Rundenzahl (max. ca. 7).
     - Beispiel Siegerzelle: `TEAMANZEIGE(…; INDEX(…; UUIDZEILE(KOSIEGER(A5;A6;K5;L5;0); …)))`.
     - Java nutzt für Leser dieselbe Auflösung (gemeinsame UNO-freie Logik, Zugriff auf Zellen über eine schmale Schnittstelle).
4. **Java-Leser `MeldelisteUuidIndex`** (neu, `basesheet/meldeliste/`):
   - Ein Block-Lesezugriff auf Formeln und Werte der Nr-Spalte ergibt UUID ↔ Nr ↔ Zeile.
   - Die Namen kommen über den vorhandenen `TeilnehmerNamenLeser`.
   - Dieser Leser ist die einzige Stelle, die `PTM.ALG.TNRUUID` parst. `GlobalImpl` nutzt denselben Parser.
5. **Laufzeitmodell unverändert, gespeicherte Daten per UUID:**
   - Jeder SheetRunner-Lauf bildet zu Beginn über `MeldelisteUuidIndex` UUID → aktuelle Nr ab.
   - Die Algorithmen (`Team`, `TeamMeldungen`, `NrComparable`, Schweizer, KO, …) rechnen wie bisher mit der in diesem Moment eindeutigen Nr.
   - Geschrieben wird immer die UUID.
   - Vorteil: Kein Umbau der Algorithmen und Modelle; die UUID-Pflicht gilt an der Sheet-Grenze.

## Schritte

### 1. Add-in-Funktionen
- In `idl/de/petanqueturniermanager/AddInGlobal.idl` die Indizes 34–36 ergänzen: `ptmtnruuid(long nr, string uuid)`, `ptmuuidzeile(string uuid, XCellRange bereich)` (erledigt, `d42ad54f`) und `ptmkosieger(XCellRange teamA, XCellRange teamB, XCellRange punkteA, XCellRange punkteB, long verlierer)` → `any` (mit dem KO-Umbau).
- `addin/XGlobal.java` um `MethodTypeInfo` und Signaturen ergänzen. Muster: `ptmteamanzeige`, Index 29.
- `addins/GlobalImpl.java` implementieren. Der UUID-Parser liegt in einer UNO-freien Klasse `TeamUuidFormel` (bauen/parsen) und ist unit-getestet.
- `registry/org/openoffice/Office/GlobalAddIn.xcu` (5 Sprachen) ergänzen und `./gradlew compileIdl` ausführen.
- Risiko Add-in-Metadaten-Cache (vgl. Err:504-Memory): im Release-Hinweis erwähnen.

### 2. Meldeliste
- `teamnummernVergeben` in allen `*MeldeListeSheetUpdate`/`*Delegate` (Schweizer, KO, Poule, FormuleX, Kaskade, JGJ, TripTete) sowie `MeldeListeHelper.updateMeldungenNr` (Supermelee, Liga):
  - Neue Nummern werden als `PTM.ALG.TNRUUID`-Formel mit neuer UUID geschrieben, als Block (`RangeHelper`/`setFormulaArray`).
  - Bestehende reine Zahlen ohne Formel bekommen beim Update eine UUID (Migration).
- Die Nr-Spalte ist dauerhaft gesperrt. Neuer Menüpunkt „Teamnummer ändern“ (Dialog alte → neue Nr, schreibt nur die erste Konstante der Formel neu):
  - neue XCU in `META-INF/manifest.xml`,
  - `ProtocolHandler`-Konstante,
  - `ProtocolHandlerCommandsTest` mitlaufen lassen.
- Entfernen:
  - `MeldelisteSortBereich` samt aller Aufrufer (Sortierbereich wieder nur die Datenspalten),
  - `MeldeListeHelper.datenBereichMitLokalerUuid`,
  - die UUID-Spaltenlogik im `SheetMeldelisteAdapter` (`uuidSpalte`, `ermittleUuidSpalte`, `supermeleeUuidZielSpalte`, `verschiebeUuids`, `leseKopfzeile`),
  - `LokaleUuidSpalte`,
  - `MeldelisteZielFactory.sichereUndEntferne…/stelleLokalePtmOnlineUuidsWiederher` und deren Aufrufe im Supermelee-Spieltagwechsel,
  - den i18n-Key `ptmonline.meldeliste.header.lokaleuuid` (alle 5 Sprachen).
- `MeldelisteZiel`-UUID-Methoden (`leseLokaleUuids`, `getOderErzeugeLokaleUuids`, `setzeLokaleUuids`, `formelTeamNrAusLokalerUuid`) laufen über `MeldelisteUuidIndex` bzw. das Schreiben der Nr-Formel. Die Schnittstelle bleibt, damit `ptmonline/*` kaum angefasst wird.
- `MeleeAnmeldungZiel` entsprechend.

### 3. Formelbauer zentral umstellen
- `MeldeListeHelper.teamNameFormel`, `spielerNamenFormel` und `formulaSverweisSpielernamen` sowie `TeamAnzeigeHelper.formel` bekommen einen **UUID-Ausdruck** (Literal oder Zellbezug auf eine UUID-Zelle) statt der Nr-Adresse.
- Intern `UUIDZEILE` statt `MATCH`/`VLOOKUP`.
- Damit sind die meisten Anzeigeformeln mit einer Änderung erledigt.

### 4. Turniersysteme (gespeicherte Referenzen → UUID)
Muster je System: Wo heute die Nr als Zahl steht (sichtbar oder versteckt), steht künftig **eine** sichtbare Teamzelle mit UUID-Literal in der Formel. Sie zeigt je nach `TeamAnzeige` die Nr oder den Namen. Versteckte Nr-Spalten entfallen. Leser (Rangliste, Live, Sync) parsen die UUID und mappen sie über `MeldelisteUuidIndex` auf die aktuelle Nr.

- **Schweizer/Maastrichter:**
  - `TECHNISCHE_TEAM_A/B_NR_SPALTE` in `SchweizerAbstractSpielrundeSheet` entfällt; `TEAM_A/B_SPALTE` trägt die UUID-Formel.
  - `SchweizerTeamNrAufloeser` liest die UUID aus der Teamzelle; der Altbestand (Zahl oder technische Spalte, `TeamAnzeigeIndex`) bleibt bis zur Migration als Fallback.
  - Legacy `maastrichter/korunde/*` prüfen.
- **JGJ, Liga, TripTete:**
  - Die Nr-Spalten der Spielpläne (14/15 bzw. 19/20) entfallen; die Namensspalten tragen die UUID-Formel.
  - Leser: `JGJRanglisteRechner`, `LigaRanglisteSheet`, `TripTeteSpielPlanLeser`.
  - Direktvergleich-Matrizen (`JGJRanglisteDirektvergleichSheet`, Liga) positionieren nicht mehr nach Nr, sondern nach Meldelisten-Reihenfolge, mit Nr-Literalen → UUID.
- **FormuleX, Kaskade:**
  - Die sichtbare Teamspalte (heute Zahl) wird zur UUID-Formelzelle.
  - Leser: `FormuleXRanglisteSheet`, `KaskadeRundenErgebnisLeser`.
- **KO (`KoTurnierbaumSheet`, genutzt von KO, Maastrichter-Finale, Poule-KO, Kaskade-Felder, JGJ-Finale):**
  - Runde 1 trägt das UUID-Literal in der Formel statt der Nr.
  - Die Sieger-Propagation läuft über `KOSIEGER` (siehe Kernmechanik), ohne Hilfszellen.
  - Die zusätzliche Namensspalte im Modus NR (`:1584`, `:1927`) prüfen; vermutlich entfällt sie.
  - `TurnierbaumLiveLeser` liest die UUIDs aus den Formeln bzw. wertet bei Sieger-Zellen die Ergebnisse aus.
- **Poule-Vorrunde:**
  - `SPALTE_TEAM_A/B_NR` entfallen; die Namenszellen tragen die UUID-Formel, Sieger/Verlierer über `KOSIEGER`.
  - `PouleVorrundenRanglisteSheet` liest sie.
- **Supermelee:**
  - Die Spieler-Nr-Zellen in `SpielrundeDelegate` werden UUID-Formelzellen; die separaten VLOOKUP-Namensspalten werden geprüft und entfallen ggf. (Meldeliste gleiches `PTM.ALG.TNRUUID`-Schema).
  - Leser: `SpielrundeErgebnisLeser`, `EndranglisteSheet`.
- **PTM-Online:**
  - `PtmOnlineSyncSheet` behält `SPALTE_LOKALE_UUID`; `SPALTE_NR` wird `INDEX(…UUIDZEILE…)`.
  - `LiveNummernAufloesung`, `PtmOnlineSpielrundeSync`, `TeamSpielrundenLeser` lesen die UUID direkt aus den Runden, ohne den Umweg Nr → Zeile.
- **Signatur-Whitelists (`SignaturQuellen`):** Teamzellen sind jetzt Formeln. Die Signatur darf nicht ihren berechneten Wert hashen (Hash-Flapping, siehe Memory), sondern den Formeltext bzw. die UUID.

### 5. Migration bestehender Dokumente
Einmalig beim ersten Meldeliste-Update bzw. Rundenaufbau (idempotent, als `SheetRunner`-Schritt):
- UUID aus der alten Zusatzspalte in die Nr-Formel übernehmen, dann die Spalte löschen.
- Rundenblätter: Nr-Zahlen bzw. technische Nr-Spalten per aktuellem Mapping Nr → UUID in UUID-Formeln umschreiben und die versteckten Spalten löschen.
- Leser behalten bis dahin den Zahl-Fallback.

## Wiederverwendung
- `TeilnehmerNamenLeser` (Namen je Zeile, Block-Read)
- `TeamAnzeigeFormatierer` / `PTM.ALG.TEAMANZEIGE`
- `SchweizerTeamNrAufloeser` (Vorlage für UUID → Nr mit Altbestand-Fallback)
- `SheetHelper.getFormulaArrayFromRange` / `setFormulaArray`
- `RangeHelper`
- `BlattschutzManager.schreibeEntsperrt`

## Risiken
- **Siegerzellen ohne Literal:** `KOSIEGER` löst rekursiv auf; Bezüge aus dem Formeltext parsen (relativ/absolut, gleiches Blatt) unit-testen.
- **Performance:** `UUIDZEILE` liest je Aufruf ca. 1000 Formeln. Bei vielen Formelzellen (KO-Baum, Spielplan) früh messen. Bei Bedarf einen Cache je Bereich mit Invalidierung über den Inhalt der Nr-Spalte.
- **Ohne installierte Extension** zeigen Nr-Spalten `#NAME?`. Das ist schon heute bei allen `PTM.ALG.*` so.
- **Größe:** Sehr viele UITest-JSON-Referenzen ändern sich, weil Teamzellen zu Formeln werden und versteckte Nr-Spalten entfallen. Neu erfasste JSONs inhaltlich prüfen (Memory-Regel).
- **Logik im Online-Projekt:** Paarung und Rangliste sind dort dupliziert; Auswirkung auf `Petanque-Turnier-Manager-Online` prüfen (Memory-Regel).

## Verifikation
- **Unit:** `TeamUuidFormel` (bauen/parsen, Sonderzeichen, Altformat Zahl), `GlobalImpl.ptmtnruuid/ptmuuidzeile` mit gemocktem `XCellRange`.
- **UITests je System:**
  - Turnier mit 2 Runden aufbauen, „Teamnummer ändern“ (z. B. 3 ↔ 7) ausführen.
  - Prüfen: Runden zeigen dieselben Paarungen mit neuen Nummern, Ranglisten unverändert bis auf die Nr.
- **Mehr-Dokument-Test** nach `GlobalImplMehrDokumenteUITest`: zwei Dokumente mit gleicher Nr und verschiedener UUID, Fokus auf dem falschen Dokument; `UUIDZEILE` liefert das richtige.
- **Migration:** Altdokument mit Zusatzspalte und Nr-Runden öffnen und aktualisieren. Danach Spalte weg, Runden mit UUID, PTM-Online-Sync unverändert zugeordnet.
- **Gates und Suites:**
  - `./gradlew test spotbugsMain` (inkl. `ProtocolHandlerCommandsTest`, `LibreOfficeOptionsPackagingTest`),
  - `./gradlew reinstallExtension uiTests`,
  - die PTM-Online-Zusammenspiel-Suite gegen den lokalen Worker.
