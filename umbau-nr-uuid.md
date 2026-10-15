# UUID statt Teamnummer als Team-Identität

## Alte Dokumente

Alte Dokumente werden nicht auf UUIDs umgestellt. PTM bricht die Verarbeitung ab und zeigt: „Dieses Dokument muss mit dem ursprünglichen Plugin geöffnet werden."


## Grundregel

**Die UUID bleibt fest am Team.** Nummer, Teamname und Teammitglieder (Namen, Austausch einzelner Spieler) dürfen sich ändern; die UUID nie. Eine neue UUID entsteht nur für ein neues Team (neue Meldung, Kopie einer Zeile); mit dem Löschen der Meldung verschwindet sie. Jeder Schreibpfad auf die Meldeliste muss das einhalten (Audit 2026-10-11: Namensänderung, Import, Sortieren, Lücken schließen, Spieltagwechsel, Nummernvergabe, Teamnummer ändern – alle konform). Einzige bewusste Lücke: direktes Überschreiben der Nr-Zelle außerhalb des Turnier-Modus.

Zwei getrennte Identitäten (Klarstellung 2026-10-11):
- **Team-UUID** – Nr-Zelle der Meldeliste, Identität der gebildeten Meldung/des Teams.
- **Mêlée-UUID** – Nr-Zelle der Mêlée-Anmeldung (eigene Liste, Einzelspieler), losgelöst von der Team-UUID. Die Mêlée-Anmeldung wird mit PTM-Online synchron gehalten (Mapping Mêlée-UUID ↔ Online-Meldungs-ID).
- **Umgesetzt (`65dc1e4e`), UITests `*Melee*` + `ptmonline`: 62/64 grün** (bekannter Blattschutz-Fehler in `PtmOnlineAbgleichSheetRunnerUITest`; `PtmOnlineSpieltagAbgleichSupermeleeUITest` im Gesamtlauf einmal rot durch asynchrones Fehler-Log, einzeln grün): „Übernommen“ = `IFNA(INDEX(Meldeliste.$A$1:$A$999; UUIDZEILE("<team-uuid>"; …)); "")` – zeigt die aktuelle Teamnummer, leer (= Spieler wieder frei), wenn das Team gelöscht wird. Übernahme schreibt je Team `TNRUUID(0;"<team-uuid>")` in die Meldeliste. `MeleeAnmeldungZeile.teamNr` (nur aus Formel mit UUID), `MeleeTeilnahme`/`LiveNummernAufloesung` ordnen über den Verweis zu, Namen nur Rückfall bei einer mit `X` markierten Zeile. Online-Team-UUID (`PUT melee-teams`) ist jetzt die Team-UUID der Meldeliste statt aus der Nummer abgeleitet (Server ersetzt die Zuordnungen komplett, keine Leichen). Der Server speichert die lokale UUID je Anmeldung (`local_registration_uuid`, `GET …/mapping`).

## PTM-Online-ID in der Nr-Zelle (Entscheidung 2026-10-12)

Alle Identitäten einer Meldung stehen an **einer** Stelle, der Nr-Zelle; das separate Mapping im Blatt „PTMOnline Sync“ entfällt.
- Meldeliste (Team): `PTM.ALG.TNRUUID(nr;"lokale-uuid";"online-id")`, Mêlée-Anmeldung (Spieler): `PTM.ALG.SPLRUUID(nr;"lokale-uuid";"online-id")`. Der dritte Parameter ist optional (`any`), ohne Online-Verbindung fehlt er.
- `NrZellFormel` liest/baut die Zelle (erstes Text-Literal = lokale UUID, zweites = Online-ID). Jeder Umnummerierer behält beide; eine kopierte Zeile wird eine neue Meldung ohne Online-ID.
- Das Sync-Blatt führt je lokaler UUID nur noch Anzeige, Revision, Rohstatus, Besetzung und Vermerke (Spalte „Online-ID“ entfernt). Seit 2026-10-15 gibt es dort auch keine Spalte „Lokale UUID“ mehr: Schlüssel einer Zeile ist die UUID in ihrer Nr-Formel `IFNA(PTM.ALG.UUIDTEAMNR("<uuid>";…);"")`. `addMappings` und `aktualisiereAnzeigeFormeln` weisen eine Nr-Formel ohne die UUID ihrer Zeile ab. `PtmOnlineRegistrationMapping` behält seine Schnittstelle und liest/schreibt die IDs über `MeldelisteZiel.leseOnlineIdsProUuid/setzeOnlineIds`. Es zählen nur Meldungen mit Zeile im Sync-Blatt dieser Verbindung.
- Lokal gelöschte Meldungen: Die Zuordnung kommt je Lauf vom Server (`registration.localRegistrationUuid`, `ergaenzeAusServer` nach jedem Abruf). Damit der Server die lokale UUID auch bei *importierten* Online-Anmeldungen kennt (früher erst nach einer lokalen Besetzungsänderung), legt jeder Abgleich für alle Zuordnungen, deren UUID online fehlt oder abweicht, einen Auftrag `ANMELDUNG_VERKNUEPFEN` an (`RegistrationImportTask.fehlendeVerknuepfungen`, `POST …/registration-links`, höchstens 200 je Auftrag) und sendet ihn sofort. Er ändert online weder Daten noch Ausführungsrevision; eine UUID, die schon eine andere Anmeldung trägt, bleibt dort. Das deckt auch ältere Dokumente ab. Bis der Auftrag angenommen ist, liefert `ergaenzeAusOffenenAuftraegen` die Zuordnung aus dem Puffer (Verknüpfungs- und Änderungsaufträge). E2E: `PtmOnlineSchweizerZusammenspielTest.geloeschteImportierteMeldungWirdNichtErneutImportiert` (ohne Verknüpfung rot).
- Supermelee: Die Nr-Zelle trägt nur die Online-ID des aktuellen Spieltags; beim Archivieren der vorherigen Spieltage (`PtmOnlineSpieltagWechsel`) werden die IDs geleert.
- Schreiben der Online-ID berechnet geänderte Zellen sofort (`calculate()`): Bei abgeschalteter automatischer Berechnung zeigten neue Nr-Formeln sonst 0.

## Arbeitsstand (2026-10-15)

Branch `ptm-online-2`, Haupt-Checkout (Worktree entfernt).

| Schritt | Stand | Commit |
|---|---|---|
| 1. `PTM.ALG.TNRUUID` / `PTM.ALG.UUIDZEILE` | erledigt | `d42ad54f` |
| 1. `PTM.ALG.UUIDTEAMNR` (Teamnummer direkt zur UUID, ersetzt `INDEX(…UUIDZEILE…)`) | erledigt; liest seit `295a9f41` nur noch den Wert der gefundenen Zelle | `cbe3438e` |
| 1. `PTM.ALG.KOSIEGER` | **entfällt** (Entscheidung 2026-10-11, siehe unten) | |
| 2. UUID in der Nr-Zelle, Zusatzspalte entfernt | erledigt | `82c9d1cd`, `c9190752` |
| 2. Menüpunkt „Teamnummer ändern“ | erledigt: `ptm:teamnummer_aendern` in `Addons_A0_SpielerDb.xcu` (A0A02); `TeamnummerAenderung` (UNO-frei, Tausch bei vergebener Nr), `TeamnummerAendernDialog`, `TeamnummerAendernSheet` (freigeschaltet nur für `UMGESTELLTE_SYSTEME`, Stand: alle Systeme). Nr-Spalte ist im Turnier-Modus bereits gesperrt (Blattschutz-Konfiguration). UITest: Tausch über den Runner, Runde + Rangliste ordnen richtig zu | siehe Log |
| 3. Formelbauer über `TeamSchluessel` | erledigt in `MeldeListeHelper`, `TeamAnzeigeHelper`. Gespeicherte Referenzen nutzen immer `Uuid` (`MeldelisteUuidIndex.uuidSchluessel`, bricht ohne UUID ab; der stille Rückfall auf ein Nummernliteral ist entfernt). `AngezeigteNr` (vormals `Nr`) verweist nur noch auf Zellen, die selbst die aktuelle Nr anzeigen: KO-/Poule-Sieger, Teilnehmerlisten, Ranglisten | `7bba57a8`, `d40345cd`, `46559309` |
| 4. Schweizer/Maastrichter-Vorrunde | erledigt: Runden schreiben UUID-Formeln, keine technischen Spalten mehr; `SchweizerTeamNrAufloeser` (UUID → sichtbare Zahl → Anzeige-Index; die technische Spalte ist seit `7ca94614` entfernt), `SchweizerRundenLeser`, `LiveZellbereich` lesen die UUID. Pakete `schweizer`, `maastrichter`, `ptmonline`: 154/155 UITests grün | `7bba57a8`, `d40345cd` |
| 4. KO-Turnierbaum (`KoTurnierbaumSheet`: KO, Maastrichter-Finale, Poule-KO, Kaskaden-Felder, JGJ-Finale) | erledigt: Runde-1- und Cadrage-Zellen schreiben immer eine UUID-Formel (auch im Nr-Modus, dort `INDEX(…UUIDZEILE)`); Sieger-/Verlierer-Formeln unverändert. KO in `UMGESTELLTE_SYSTEME`; UITest `KoTurnierbaumTeamnummerAendernUITest` | siehe Log |
| 4. FormuleX | erledigt: Teamzellen der Spielrunde sind Formeln `INDEX(…UUIDZEILE("uuid"))` (Anzeige = aktuelle Nr), Leser unverändert (lesen die Anzeige). FORMULEX in `UMGESTELLTE_SYSTEME`; UITest `FormuleXTeamnummerAendernUITest`; formulex-UITests 31/31 | siehe Log |
| 4. Kaskade | erledigt: Teamzellen der Kaskadenrunden aufgelöster Teams sind Formeln `INDEX(…UUIDZEILE)`, Platzhalter bleiben Text; KO-Felder über den Turnierbaum; Gruppenübersicht/Gruppenrangliste sind Anzeigen (neu aufgebaut). KASKADE in `UMGESTELLTE_SYSTEME`; UITest `KaskadeTeamnummerAendernUITest` (Folgerunde führt den Sieger mit neuer Nr); kaskade-UITests 19/19 | siehe Log |
| 4. TripTete | erledigt: ausgeblendete Nr-Spalten des Spielplans (19/20) sind Formeln `INDEX(…UUIDZEILE)` (Freilos 0) statt Zahlen; Namensformeln und `TripTeteSpielPlanLeser` unverändert (lesen die Anzeige). Abweichung vom Plan: Spalten bleiben, tragen aber die UUID. TRIPTETE in `UMGESTELLTE_SYSTEME`; UITest `TripTeteTeamnummerAendernUITest`; triptete-UITests 14/14 | siehe Log |
| 4. JGJ | erledigt: Nr-Spalten des Spielplans (14/15) sind Formeln `INDEX(…UUIDZEILE)` (Freilos 0); Gruppen-Aushang verweist per Formel auf diese Nr-Zellen (Nr-Anzeige ebenfalls per Formel); Direktvergleich positioniert nach Reihenfolge statt Nr (Lücken erlaubt), Nr-Literale werden beim Abruf neu erzeugt; Finale über den Turnierbaum. JGJ in `UMGESTELLTE_SYSTEME`; UITest `JGJTeamnummerAendernUITest`; jedergegenjeden-UITests 37/37 | siehe Log |
| 4. Maastrichter | erledigt: Vorrunden = Schweizer-Runden, Finale = KO-Baum. Die KO-Gruppe hängt in der Vorrunden-Rangliste an der Nr (Werte) – Gruppenübersicht, Siegergeld und Rangliste-Neuaufbau lesen sie darüber. „Teamnummer ändern“ zieht deshalb die Nr-Werte der Vorrunden-Rangliste sofort nach (`MaastrichterGruppenSpalteHelper.nummerAendern`, bei Tausch in beide Richtungen). MAASTRICHTER in `UMGESTELLTE_SYSTEME` (jetzt alle Systeme); UITest `MaastrichterTeamnummerAendernUITest` (Tausch über Gruppengrenzen, Gegenprobe ohne Nachziehen rot); Maastrichter-UITests 31/31 | siehe Log |
| 4. Liga | erledigt: Nr-Arbeitsspalten des Spielplans (14/15) sind Formeln `INDEX(…UUIDZEILE)` (Freispiel 0); Namensformeln, Rangliste, Termine, Status-/Blattschutz-Leser unverändert (lesen die Anzeige); Direktvergleich positioniert nach Reihenfolge statt Nr. LIGA in `UMGESTELLTE_SYSTEME`; UITest `LigaTeamnummerAendernUITest`; liga-UITests 23/23 | siehe Log |
| 4. Poule-Vorrunde | erledigt: Nr-Zellen der ersten Runde (Vorrunde und Gruppen-Spielpläne) sind Formeln `INDEX(…UUIDZEILE)`; Sieger-/Verlierer-/Barrage-Zeilen reichen die Anzeige weiter (wie KO), Namensformeln und Leser unverändert; Poule-KO über den Turnierbaum. POULE in `UMGESTELLTE_SYSTEME`; UITest `PouleTeamnummerAendernUITest`; Poule-UITests 18/18 | siehe Log |
| 4. Supermelee | erledigt: Spielernummern der Spielrunden sind Formeln `INDEX(…UUIDZEILE)`; Namensformeln, Ranglisten, Validatoren und Live-Leser unverändert (lesen die Anzeige). Da die Endrangliste die Spieltag-Ranglisten über die Nummer liest, baut „Teamnummer ändern“ bei Supermelee alle Spieltag-Ranglisten sofort neu auf. SUPERMELEE in `UMGESTELLTE_SYSTEME`; UITest `SupermeleeTeamnummerAendernUITest` (100 Spieler, 5 Spieltage); Supermelee-/Mêlée-UITests 90/90 (`PtmOnlineSpieltagAbgleichSupermeleeUITest` wie bekannt im Gesamtlauf rot, einzeln grün) | siehe Log |
| 5. Alte Dokumente | erledigt: zentral in `comp/kompatibilitaet/DokumentKompatibilitaet` (ersetzt `AltesDokumentErkennung`), Schritt `TeamUuidUmstellung` ab `5.40.00`, nicht migrierbar. Mit gespeicherter Version entscheidet die Version; ohne Version gilt ein Dokument als alt, wenn die Meldeliste nummerierte Meldungen, aber keine UUID hat (Entscheidung User 2026-10-12). Prüfung in `SheetRunner.run()` vor jeder Änderung, Abbruch mit `AltesDokumentException`, beim Öffnen einmal Hinweis (`DokumentKompatibilitaetsHinweis`). UITest `AltesDokumentUITest`, Details in CLAUDE.md | `fb19ae21`, `7e38be22` |

Neue Klassen: `TeamUuidFormel` (Parser), `MeldelisteUuidIndex` (UUID ↔ Nr, `leseNrSpalte`), `TeamSchluessel` (Formelausdruck je Schlüsselart), `NrUuidSpalte`, `TeamNummernPlan`/`TeamNummernVergabe`, `TeamnummerAenderung`/`TeamnummerAendernDialog`/`TeamnummerAendernSheet`. Dialog-Bausteine (`label`, `numericField`, `button`, `Rahmen`) liegen jetzt in `AbstractUnoDialog`.

Ranglisten nach „Teamnummer ändern“ (behoben 2026-10-15, `e5526e2f`): Ranglisten führen Nummern als Werte. `TeamnummerAendernSheet.nachziehen` führt jetzt je System das `*SheetUpdate` der vorhandenen Ranglisten synchron aus (fehlende werden nicht angelegt, Supermelee zusätzlich die Endrangliste, Maastrichter wie bisher `MaastrichterGruppenSpalteHelper`). Damit sehen auch Leser ohne Tabwechsel (Webserver, Export, PTM-Online-Live) sofort die neue Nummer; die `*TeamnummerAendernUITest` prüfen die Rangliste ohne eigenes Update.

Behoben (2026-10-12), nicht UUID-bezogen: Tests mit „Style/CF-Operation außerhalb eines aktiven BlattschutzScopes“. Ursache: Ein erfolgreicher `start()`-Lauf schaltet den Turnier-Modus automatisch ein (`SheetRunner.aktiviereTurnierModusAutomatisch`); folgende direkte Aufrufe im Test (`doRun()`, `uebernehmen()`, Auftragsversand) liefen ohne Scope. Fix: `BaseCalcUITest.wieImSheetRunner(...)` öffnet den Scope wie `SheetRunner.run()`. `PtmOnlineAbgleichSheetRunnerUITest` 10/10, `./gradlew ptmOnlineTests` 19/19.

Nächste Schritte (Stand 2026-10-15):
1. ~~Rückfallwege für Altbestand~~ erledigt (2026-10-12): Schweizer-technische Spalten (`TECHNISCHE_TEAM_A/B_NR_SPALTE`) und der Namens-Rückfall in `MeleeTeilnahme`/`LiveNummernAufloesung` entfernt. Bewusst behalten (auch für von Hand korrigierte Zellen in neuen Dokumenten): sichtbare Zahl ohne UUID und Name über `TeamAnzeigeIndex` im `SchweizerTeamNrAufloeser`, abgeleitete Team-UUID in `meleeTeams`.
2. ~~Gesamtlauf, Online-Projekt auf duplizierte Logik prüfen~~ erledigt (2026-10-15):
   - Gesamtlauf `./gradlew uiTests`: 674 Tests in 160 Klassen, rot nur 2 in `PtmOnlineAbgleichSheetRunnerUITest` (Test-Server kannte `/registration-links` nicht, `962e2370`). `PtmOnlineSpieltagAbgleichSupermeleeUITest` war diesmal grün.
   - Online-Projekt: **kein Anpassungsbedarf.** Der Umbau ändert, woran das Plugin ein Team erkennt, keine Turnierregel. Im Zeitraum des Umbaus gab es keine Änderung an Paarung, Rangliste, Freilos, Cadrage oder Wertung. Der Fix `69fda3a1` betrifft Kaskaden, die online nicht ausgelost werden.
   - Live-Sync: Das Plugin übersetzt lokale Nummern beim Senden in Online-Anmelde-IDs (`LiveUebertragungsDaten.onlineIdsVon`). Der Server erhält und speichert nur `registrationIds`, nie Teamnummern. „Teamnummer ändern“ ändert online daher nichts.
   - `seeding_position` ist die Setzpositions-Spalte, nicht die Teamnummer. Mêlée-Teams (`PUT …/melee-teams`) gehen schon mit `teamUuid`.
   - Die Online-Auslosung (`src/lib/pairing/*`) gilt nur für reine Online-Turniere und arbeitet mit eigenen Team-IDs (`tournament_teams.id`). Sie hat keine Berührung mit Nummern aus dem Dokument.
3. ~~PTM-Online-Lücke bei gelöschten importierten Meldungen~~ erledigt (2026-10-15), braucht das Deploy des Online-Projekts mit `POST …/registration-links`.

Release-Hinweis (erstes Release mit UUID-Teams):
- Dokumente älterer Versionen lassen sich öffnen, aber nicht mehr weiterführen („Dieses Dokument muss mit dem ursprünglichen Plugin geöffnet werden.“).
- Zeigen neue `PTM.ALG.*`-Formeln (z.&nbsp;B. in der Nr-Spalte) nach dem Update `Err:504` oder `#NAME?`, obwohl die Extension aktuell ist: LO hat alte Add-in-Metadaten im Benutzerprofil zwischengespeichert. LibreOffice schließen, `extensions` und `uno_packages/cache` im Benutzerprofil umbenennen, Extension neu installieren.

Entscheidung KO-Sieger (2026-10-11): **kein `PTM.ALG.KOSIEGER`.** Nur Setzlisten-Zellen (Runde 1, Cadrage) tragen die UUID; die Sieger-/Verlierer-Formeln reichen wie bisher die *Anzeige* weiter (`IF(scoreA>scoreB; teamA; …)`). Da die Setzlisten-Zellen immer die aktuelle Nr bzw. den aktuellen Namen zeigen, stimmt nach „Teamnummer ändern“ der ganze Baum. Leser (`LiveZellbereich`, Siegergeld, `KoStatusLeser`) lesen wie bisher die Anzeige. Bewusst in Kauf genommen: Sieger-Zellen tragen keine UUID; bei doppelten Teamnamen in der Namensanzeige bleiben sie für Java-Leser mehrdeutig (wie vorher). Gilt sinngemäß auch für Poule-Sieger/Verlierer.

Teilnehmer- und Checkin-Listen: Nummern dort sind Werte, ihre Signatur enthält die Nr-Spalte der Meldeliste (`MELDELISTE_BREIT_SPALTEN`) → nach „Teamnummer ändern“ beim nächsten Tab-Wechsel neu aufgebaut.

Entscheidungen „Teamnummer ändern“ (2026-10-11):
- **Ein** systemübergreifender Menüeintrag (Bereich Spieler-DB/Meldeliste, `Addons_A0_SpielerDb.xcu`), erkennt das aktive Turniersystem selbst; kein Eintrag je Systemmenü, kein Toolbar-Button.
- Nr-Spalte nur im Turnier-Modus gesperrt (bestehendes Blattschutz-Konzept). Außerhalb davon bleibt sie offen; beim Ändern der Nummer bleibt die dem Team zugehörige UUID unverändert. Bestehende Runden behalten dadurch ihren Bezug zu diesem Team.

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
   - Damit wird in allen Formelbauern `MATCH(nr;$A$1:$A$999;0)` zu `PTM.ALG.UUIDZEILE("<uuid>";$A$1:$A$999)`. Im Anzeigemodus NR gilt `PTM.ALG.UUIDTEAMNR("<uuid>";$A$1:$A$999)` (Index 37, ersetzt `INDEX($A$1:$A$999; UUIDZEILE(…))`; Altformeln bleiben gültig).
3. **Keine zusätzlichen UUID-Spalten, weder in der Meldeliste noch in Runden oder Ranglisten.** Jede Teamzelle trägt ihre UUID als Literal in der eigenen Formel. Bereits vorhandene, ausgeblendete Nr-Spalten (JGJ, Liga, TripTete) sind geblieben und tragen jetzt diese Formel:
   - Anzeige Nr: `=PTM.ALG.UUIDTEAMNR("<uuid>"; Meldeliste.$A$1:$A$999)` (früher `INDEX(…; UUIDZEILE(…))`).
   - Anzeige Name: dieselbe Zeile über die Formelbauer (`PTM.ALG.TEAMANZEIGE`).
   - Java liest die UUID per `getFormulaArray` und UUID-Regex aus der Zelle. Gemeinsamer Parser: `TeamUuidFormel.findeUuid(String formel)`.
   - ~~**Weitergabe ohne Hilfsspalte**~~ (verworfen, siehe Entscheidung KO-Sieger oben) (KO-Sieger, Poule-Sieger/Verlierer): `PTM.ALG.KOSIEGER(teamA; teamB; punkteA; punkteB; verlierer)` ersetzt `IF(scoreA>scoreB; …)` und liefert die UUID des Siegers (bzw. mit `verlierer=1` des Verlierers), `#N/A` ohne Ergebnis.
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

## Schritte (ursprünglicher Plan)

Umgesetzt ist alles; wo die Umsetzung abweicht, ist es unten vermerkt. Den tatsächlichen Stand je System zeigt die Tabelle unter „Arbeitsstand“.

### 1. Add-in-Funktionen
- In `idl/de/petanqueturniermanager/AddInGlobal.idl` die Indizes 34–36 ergänzen: `ptmtnruuid(long nr, string uuid)`, `ptmuuidzeile(string uuid, XCellRange bereich)` (erledigt, `d42ad54f`). `ptmkosieger` entfällt (Entscheidung KO-Sieger); hinzugekommen ist `ptmuuidteamnr` (`cbe3438e`).
- `addin/XGlobal.java` um `MethodTypeInfo` und Signaturen ergänzen. Muster: `ptmteamanzeige`, Index 29.
- `addins/GlobalImpl.java` implementieren. Der UUID-Parser liegt in einer UNO-freien Klasse `TeamUuidFormel` (bauen/parsen) und ist unit-getestet.
- `registry/org/openoffice/Office/GlobalAddIn.xcu` (5 Sprachen) ergänzen und `./gradlew compileIdl` ausführen.
- Risiko Add-in-Metadaten-Cache (vgl. Err:504-Memory): steht im Release-Hinweis unter „Arbeitsstand“.

### 2. Meldeliste
- `teamnummernVergeben` in allen `*MeldeListeSheetUpdate`/`*Delegate` (Schweizer, KO, Poule, FormuleX, Kaskade, JGJ, TripTete) sowie `MeldeListeHelper.updateMeldungenNr` (Supermelee, Liga):
  - Neue Nummern werden als `PTM.ALG.TNRUUID`-Formel mit neuer UUID geschrieben, als Block (`RangeHelper`/`setFormulaArray`).
  - Dokumente mit bestehenden reinen Zahlen ohne Formel gelten als alte Dokumente und werden mit der Fehlermeldung zum ursprünglichen Plugin abgewiesen.
- Die Nr-Spalte ist im Turnier-Modus gesperrt (Entscheidung 2026-10-11, siehe oben). Neuer Menüpunkt „Teamnummer ändern“ (Dialog alte → neue Nr, schreibt nur die erste Konstante der Formel neu):
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
Muster je System: Wo die Nr früher als Zahl stand (sichtbar oder versteckt), steht jetzt eine Formel mit UUID-Literal, die die aktuelle Nr anzeigt. Die meisten Leser (Rangliste, Live) lesen weiter diese Anzeige; die Schweizer-Leser (`SchweizerRundenLeser`, `LiveZellbereich`) parsen die UUID und bilden sie über `MeldelisteUuidIndex` auf die aktuelle Nr ab.

- **Schweizer/Maastrichter:**
  - `TECHNISCHE_TEAM_A/B_NR_SPALTE` in `SchweizerAbstractSpielrundeSheet` entfällt; `TEAM_A/B_SPALTE` trägt die UUID-Formel.
  - `SchweizerTeamNrAufloeser` liest die UUID aus der Teamzelle.
  - Legacy `maastrichter/korunde/*` prüfen.
- **JGJ, Liga, TripTete:**
  - Geplant war, die Nr-Spalten der Spielpläne (14/15 bzw. 19/20) zu entfernen. **Umgesetzt:** Die Spalten bleiben und tragen `INDEX(…UUIDZEILE)` (Freilos/Freispiel 0); die Namensformeln sind unverändert.
  - Leser `JGJRanglisteRechner`, `LigaRanglisteSheet`, `TripTeteSpielPlanLeser` unverändert (lesen die Anzeige).
  - Direktvergleich-Matrizen (`JGJRanglisteDirektvergleichSheet`, Liga) positionieren nicht mehr nach Nr, sondern nach Meldelisten-Reihenfolge, mit Nr-Literalen → UUID.
- **FormuleX, Kaskade:**
  - Die sichtbare Teamspalte (früher Zahl) ist eine UUID-Formelzelle; bei Kaskaden bleiben Platzhalter noch nicht feststehender Teams Text.
  - Leser `FormuleXRanglisteSheet`, `KaskadeRundenErgebnisLeser` unverändert (lesen die Anzeige).
- **KO (`KoTurnierbaumSheet`, genutzt von KO, Maastrichter-Finale, Poule-KO, Kaskade-Felder, JGJ-Finale):**
  - Runde 1 und Cadrage tragen das UUID-Literal in der Formel statt der Nr (erledigt).
  - Sieger-Propagation unverändert über die Anzeige (Entscheidung KO-Sieger); die Namensspalte im Modus NR bleibt (`MATCH` auf die aktuelle Nr).
  - `TurnierbaumLiveLeser` unverändert (liest die Anzeige, UUID-Formeln über `LiveZellbereich`).
- **Poule-Vorrunde:**
  - **Umgesetzt:** Die Nr-Zellen der ersten Runde (Vorrunde und Gruppen-Spielpläne) tragen `INDEX(…UUIDZEILE)`; Sieger-/Verlierer-/Barrage-Zeilen reichen die Anzeige weiter (wie KO).
  - `PouleVorrundenRanglisteSheet` unverändert (liest die Anzeige).
- **Supermelee:**
  - Die Spieler-Nr-Zellen in `SpielrundeDelegate` sind UUID-Formelzellen (Meldeliste gleiches `PTM.ALG.TNRUUID`-Schema); die Namensformeln sind geblieben.
  - Leser `SpielrundeErgebnisLeser`, `EndranglisteSheet` unverändert (lesen die Anzeige). Weil die Endrangliste die Spieltag-Ranglisten über die Nummer liest, baut „Teamnummer ändern“ die Spieltag-Ranglisten neu auf.
- **PTM-Online:**
  - `SPALTE_NR` des `PtmOnlineSyncSheet` ist eine Formel auf die Meldeliste und zugleich Schlüssel der Zeile (UUID-Literal); die geplante Spalte `SPALTE_LOKALE_UUID` ist entfernt.
  - **Abweichung (Entscheidung 2026-10-12):** Die Online-ID steht nicht mehr im Sync-Blatt, sondern als drittes Argument in der Nr-Zelle (siehe „PTM-Online-ID in der Nr-Zelle“).
  - Die Live-Übertragung liest die Anzeige der Runden und übersetzt die Nummern beim Senden in Online-IDs (`LiveUebertragungsDaten`).
- **Signatur-Whitelists (`SignaturQuellen`):** Teamzellen sind jetzt Formeln. Die Signatur darf nicht ihren berechneten Wert hashen (Hash-Flapping, siehe Memory), sondern den Formeltext bzw. die UUID.

## Wiederverwendung
- `TeilnehmerNamenLeser` (Namen je Zeile, Block-Read)
- `TeamAnzeigeFormatierer` / `PTM.ALG.TEAMANZEIGE`
- `SchweizerTeamNrAufloeser` (Vorlage für UUID → Nr)
- `SheetHelper.getFormulaArrayFromRange` / `setFormulaArray`
- `RangeHelper`
- `BlattschutzManager.schreibeEntsperrt`

## Risiken
- **Siegerzellen ohne Literal:** tragen keine UUID (Entscheidung KO-Sieger); Leser arbeiten mit der Anzeige.
- **Performance (gemessen 2026-10-15):** Supermelee-Beispielturnier (100 Spieler, 5 Spieltage, 37 Blätter) mit 1399 `UUIDTEAMNR`- und 1399 `UUIDTEAMANZEIGE`-Zellen: `calculateAll` 2,3 s, nach `295a9f41` (UUIDTEAMNR liest nur noch den Wert der gefundenen Zelle statt aller Werte) 1,7 s. Die reine Java-Suche (Regex über 999 Zeilen) macht davon ~130 ms aus, der Rest ist `getFormulaArray` und Calc selbst. Volle Neuberechnung nur bei Änderungen an der Nr-Spalte oder hartem Recalc, nicht bei Ergebniseingabe. **Kein Cache je Bereich:** Das Add-in-Objekt ist dokumentübergreifend, aus dem übergebenen `XCellRange` lässt sich das Dokument nicht stabil bestimmen – ein Cache könnte Werte zwischen zwei offenen Dokumenten vertauschen.
- **Ohne installierte Extension** zeigen Nr-Spalten `#NAME?`. Das ist schon heute bei allen `PTM.ALG.*` so.
- **Größe (eingetreten, abgearbeitet):** Sehr viele UITest-JSON-Referenzen ändern sich, weil Teamzellen zu Formeln werden und versteckte Nr-Spalten entfallen. Neu erfasste JSONs inhaltlich prüfen (Memory-Regel).
- ~~**Logik im Online-Projekt**~~ geprüft (2026-10-15): kein Anpassungsbedarf, siehe „Nächste Schritte“ Nr. 2.

## Verifikation
- **Unit:** `TeamUuidFormel` (bauen/parsen, Sonderzeichen, Altformat Zahl), `GlobalImpl.ptmtnruuid/ptmuuidzeile` mit gemocktem `XCellRange`.
- **UITests je System:**
  - Turnier mit 2 Runden aufbauen, „Teamnummer ändern“ (z. B. 3 ↔ 7) ausführen.
  - Prüfen: Runden zeigen dieselben Paarungen mit neuen Nummern, Ranglisten unverändert bis auf die Nr.
- **Mehr-Dokument-Test** (erledigt, `UuidFunktionenMehrDokumenteUITest`): zwei Dokumente mit gleicher Nr für verschiedene Teams und gleicher UUID unter anderer Nr/Zeile, Fokus auf dem jeweils anderen Dokument; `UUIDZEILE`, `UUIDTEAMNR` und `UUID.TEAMANZEIGE` liefern den Wert des eigenen Dokuments, eine Änderung im zweiten wirkt nicht aufs erste.
- **Altdokument:** Dokument mit alter Nr-/Zusatzspalten-Struktur öffnen; Verarbeitung bricht ab und verweist auf das ursprüngliche Plugin.
- **Gates und Suites:**
  - `./gradlew test spotbugsMain` (inkl. `ProtocolHandlerCommandsTest`, `LibreOfficeOptionsPackagingTest`),
  - `./gradlew reinstallExtension uiTests`,
  - die PTM-Online-Zusammenspiel-Suite gegen den lokalen Worker.
