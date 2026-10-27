# SheetSyncListener – Architekturregeln

> Die Infrastruktur war ursprünglich Rangliste-spezifisch (`RanglisteRefreshListener` etc.).
> Seit dem Refactor in `helper/sheetsync/` ist sie generisch und wird auch für andere
> Sheet-Synchronisationen genutzt (z. B. Supermelee-Spieltag-Teilnehmerliste). Die hier
> beschriebenen Regeln gelten für **alle** Konsumenten.

## Refresh nur bei tatsächlichen Änderungen (Signatur-basiert)

Der Listener prüft vor jedem Rebuild eine **kanonische SHA-256-Signatur** über die
semantisch relevanten Eingaben (Meldeliste, Spielergebnisse, ggf. Vorranglisten) und
vergleicht sie mit dem im Dokument persistierten Hash. Nur bei Abweichung wird
neu aufgebaut.

**Bausteine** (`helper/sheetsync/`):
- `SignaturQuelle` (Record) – beschreibt eine Eingangsquelle deklarativ: stabile ID,
  Named-Range-Schlüssel, Datenzeilen-Bereich, Whitelist-Spalten, `erwartet`-Flag.
- `EingabeSignatur` – generische Engine: bekommt einen
  `Function<XSpreadsheetDocument, List<SignaturQuelle>>`-Lieferanten und liefert ein
  `SignaturErgebnis` (`Ok`/`SheetFehlt`/`TransientFehler`/`PermanenterFehler`).
- `SignaturQuellen` (in `helper/rangliste/`) – Builder pro Turniersystem (z. B.
  `fuerSchweizer`, `fuerSupermeleeSpieltag`, `fuerSupermeleeTeilnehmer`). Keine
  system-spezifischen Signatur-Klassen.
- `SheetSyncSignaturStore` – persistiert Hash + Zeitstempel + Recovery-Flag als
  DocumentProperty (`ranking.<key>.last.rebuild.hash` etc. – der Prefix `ranking.`
  bleibt aus Kompatibilität mit bestehenden Tournament-Dokumenten erhalten).
- `SheetSyncDebouncer` – kollabiert Event-Stürme zu einem Hash-Check pro
  Doc+Schlüssel (Default 150 ms), bietet auch `scheduleRetry` für TransientFehler.

**Aktuelle Konsumenten:**
- Rangliste-Updates: Schweizer, Maastrichter, Poule, FormuleX, JGJ, Kaskade,
  Supermelee-Spieltag, Supermelee-End.
- Sheet-Sync (kein Rangliste-Kontext): Supermelee-Spieltag-Teilnehmerliste
  (`SupermeleeTeilnehmerSheetUpdate`).

**Disziplin – semantische Whitelist:**
- Im Hash landen ausschließlich **Eingabezellen**: Spieler-/Team-Nr, Namen, Setzposition,
  Aktiv-Status, Spielergebnisse. Hilfsspalten, Formeln, Formatierungen NIE in die
  Whitelist aufnehmen – sonst Phantom-Rebuilds.
- Sheet-Identifikation **ausschließlich** über Named-Range-Schlüssel
  (`SheetMetadataHelper.SCHLUESSEL_*`). Niemals Sheet-Index, -Position oder -Name.

**Verhaltens-Matrix:**

| Ergebnis | Aktion |
|---|---|
| `Ok(hash)` + identisch zum gespeicherten | Skip, nur `last.verify.ts` aktualisieren. |
| `Ok(hash)` + abweichend / kein gespeicherter | Rebuild, Hash speichern. |
| `SheetFehlt(erwartet=false)` | Skip (optionale Quelle, z. B. noch keine Spielrunde). |
| `SheetFehlt(erwartet=true)` + Recovery-Flag false | **Einmaliger** Recovery-Rebuild, Flag setzen. |
| `SheetFehlt(erwartet=true)` + Recovery-Flag true | Skip (kein Loop). |
| `TransientFehler`, Versuch ≤ 3 | Re-Schedule mit Backoff (300/600/1200 ms). |
| `TransientFehler`, Versuch > 3 | `forceNextCheck=true` setzen, kein Rebuild – nächster Trigger umgeht Skip-Pfad. |
| `PermanenterFehler` | Log-Warn, skip (kein Fail-Safe-Rebuild → Spam-Risiko). |

**Safety-Revalidation (10 min)**: bei `onFocus` mit veraltetem `last.verify.ts` wird
`forceNextCheck` gesetzt → Hash wird frisch berechnet. Bei Identität nur Verify-Zeit
aktualisiert, kein Rebuild.

**Bewusst NICHT verwendet:** kein `XModifyListener`/DirtyMarker (redundant zu Hash +
Debouncer), keine 8 Signatur-Subklassen (Engine + Konfiguration genügt).

## Registry: `comp/SheetSyncKatalog`

Alle Sheet-Synchronisationen stehen in **einer** Liste (`SheetSyncKatalog`). Jeder Eintrag wird beim
`SheetAktivierungsDispatcher` (Tab-Wechsel) **und** beim `SheetSyncNachzieher` angemeldet. Keine eigenen
Pro-System-Listen mehr anlegen (die früheren Duplikate `WhatsAppRanglisteAktualisierer`,
`aktualisiereExportSheetWennDirty` und die Liga-Export-Eigenlogik sind entfallen).

## Nachziehen ohne Tab-Wechsel (`SheetSyncNachzieher`)

Ergebnis-Eingaben lösen nur `modified()` aus – ohne Tab-Wechsel blieben Ranglisten für Leser veraltet.
Jeder Leser abgeleiteter Blätter ruft deshalb vor dem Lesen `SheetSyncNachzieher.get().zieheNach(ws, modus)`:

| Leser | Einstieg | Modus |
|---|---|---|
| Webserver | `WebserverModifyListener.refreshTask` → `istVeraltet` → `SheetSyncNachziehRunner` (still, `join`) | `NUR_VORHANDENE` |
| PTM-Online-Live | `PtmOnlineErfassungsRunner.doRun` (Blattschutz-Scope bedarfsabhängig via `vorAenderung`) | `NUR_VORHANDENE` |
| Verzeichnis-Export | `AbstractExportInVerzeichnis.exportiere` | `FEHLENDE_ANLEGEN` |
| WhatsApp-Rangliste | `WhatsAppBildExportService.exportiere` | `FEHLENDE_ANLEGEN` |

Regeln:
- Neu aufgebaut wird nur, was laut Signatur veraltet ist; Hash-Schlüssel identisch zum Tab-Wechsel-Pfad.
- `zieheNach` **muss im laufenden SheetRunner** laufen (`SheetRunner.fuehreInLaufendemRunnerAus()` statt
  verschachteltem `start()`). `istVeraltet` liest nur und darf vorher außerhalb laufen.
- `NUR_VORHANDENE` (Hintergrund): nur vorhandene Blätter, nur Datenbereich – **kein Fokuswechsel**. Fehlende
  Blätter werden nicht angelegt (der `forceCreate`-Fallback eines `*SheetUpdate` könnte das Blatt aktivieren).
  Einträge, deren Update Blätter löscht/neu anlegt (Liga-Termine), sind per `nichtImHintergrund()` ausgenommen.
- `FEHLENDE_ANLEGEN` (Export): zusätzlich die `SheetSyncAusgabe`n des Eintrags (`mitExportAusgaben`):
  fehlende Blätter anlegen, abhängige Blätter wie JGJ-/Liga-Direktvergleich (legt per `forceCreate` an und
  aktiviert – daher **nie** im Hintergrund) neu aufbauen.

## Rückgängig-Liste

Jeder Sync-Neuaufbau – Tab-Wechsel (`SheetRunner.ohneRueckgaengig()`) wie Nachziehen – läuft über
`RueckgaengigSperre` in einem **versteckten Undo-Kontext** (`XUndoManager.enterHiddenUndoContext()`): kein eigener
Eintrag, die Schreibvorgänge hängen an der letzten Aktion. Strg+Z nimmt die Eingabe des Anwenders und den daraus
folgenden Neuaufbau gemeinsam zurück – die Rangliste bleibt konsistent (geprüft in `SheetSyncNachzieherUITest`).

**Nicht `XUndoManager.lock()` verwenden:** Calc-Operationen sichern ihren eigenen Undo-Schalter und stellen ihn
danach wieder her (`ScDocument::EnableUndo(bOld)`), damit ist die Sperre mitten im Lauf aufgehoben (im Test landeten
trotz `lock()` 60 Einträge in der Liste). Die Dokument-Property `IsUndoEnabled=false` scheidet ebenfalls aus: sie setzt
`SetMaxUndoActionCount(0)` und löscht damit die gesamte Rückgängig-Liste des Anwenders. `lock()` bleibt nur
Notlösung bei leerer Liste (dann gibt es keine Aktion zum Anhängen).

## Problem: Race Condition bei forceCreate()

`NewSheet.forceCreate().create()` **löscht** das bestehende Sheet und legt es neu an. Das triggert LibreOffice-interne Events (u.a. `selectionChanged`). Wenn der `SheetSyncListener` dabei das Ziel-Sheet als aktiv erkennt **und** `SheetRunner.isRunning() == false` ist (was bei direkten `doRun()`-Aufrufen immer der Fall ist, da diese den `SheetRunnerKoordinator` umgehen), startet der Listener sofort einen zweiten parallelen Thread → beide Threads schreiben gleichzeitig auf dasselbe Sheet → Datenverlust/Korruption.

## Regel: Listener müssen `*SheetUpdate`-Klassen verwenden

**NIEMALS** eine Vollaufbau-Klasse (z. B. `*RanglisteSheet`, `SupermeleeTeilnehmerSheet`) direkt im `SheetSyncListener` registrieren.
**IMMER** eine `*SheetUpdate`-Klasse (nur Datenbereich) verwenden.

| Eintrag in `SheetSyncKatalog` | Klasse |
|---|---|
| Schweizer Rangliste | `SchweizerRanglisteSheetUpdate` |
| Maastrichter Vorrunden-Rangliste | `MaastrichterVorrundenRanglisteSheetUpdate` |
| Formule X Rangliste | `FormuleXRanglisteSheetUpdate` |
| Poule Vorrunden-Rangliste | `PouleVorrundenRanglisteSheetUpdate` |
| Supermelee-Spieltag-Teilnehmer | `SupermeleeTeilnehmerSheetUpdate` |

## Muster für neue Sheet-Sync-Konsumenten

Jeder neue Konsument (Rangliste oder anderer Sheet-Typ), der einen `SheetSyncListener` bekommt, benötigt eine `*SheetUpdate`-Klasse:

1. `FooSheet` – vollständiger Aufbau (Menüaktion, Erstaufbau): verwendet `NewSheet.forceCreate()`
2. `FooSheetUpdate extends FooSheet` – Update-Pfad (Listener):
   - Überschreibt `doRun()`: **kein** `forceCreate`, kein Sheet-Event, keine Race Condition
   - Prüft ob Sheet existiert; falls nicht → Fallback auf Vollaufbau ODER silent return (Teilnehmer-Pattern: Erstaufbau erfolgt nur über Menü, nicht über Listener)
   - Schreibt nur den Datenbereich neu (Rangliste: `berechnungUndSchreiben()`; Teilnehmer: `loescheBisherigenInhalt() + befuelleTeilnehmerDaten(false)`)
   - Löscht überzählige Zeilen wenn Datenmenge gesunken ist (Rangliste: `RanglisteUpdateHelper.loescheDatenzeilen()`; sonst eigener Clear-Range)
   - Eintrag in `SheetSyncKatalog` via `SheetSyncListener.fuerSchluessel(..., (ws, ignored) -> new FooSheetUpdate(ws))` (bzw. `fuerSpieltagSheet` für spieltag-abhängige Sheets); muss das Blatt vor einem Export ggf. angelegt werden, zusätzlich `mitExportAusgaben(...)`

## `setActiveSheet()` in `*SheetUpdate` – verboten

`getSheetHelper().setActiveSheet(sheet)` darf am Ende von `*SheetUpdate.doRun()` **nicht** aufgerufen werden – auch nicht hinter einer `SheetRunner.isRunning()`-Abfrage.

**Grund:** Der Listener ruft `runnerFactory.apply(...).run()` synchron im UI-Thread aus dem `selectionChanged`-Handler heraus auf. Während dieser Verarbeitung ist `SheetRunner.isRunning() == true`, also wäre die Abfrage wirkungslos. Ein zusätzliches `setActiveSheet(sheet)` aus dem `selectionChanged`-Handler heraus kollidiert mit LO-internem Tab-Klick-/Navigator-Handling: LO revertiert daraufhin den Tab-Wechsel; der User braucht 2–3 Klicks bis das Sheet aktiv bleibt. (Über die eigene Sidebar-Liste tritt das Problem nicht auf, weil dort `view.setActiveSheet(...)` programmatisch *vor* dem Event ausgeführt wird.)

- Listener-Pfad: User ist beim Tab-/Navigator-Klick bereits auf der Rangliste – `setActiveSheet` ist überflüssig.
- Programmatischer Aufruf (z.B. aus `MaastrichterFinalrundeSheet`, `PouleKoSheet`): Der aufrufende Parent-Runner setzt sein eigenes aktives Sheet.

`setActiveSheet()` bleibt zulässig im **Vollaufbau** (`*RanglisteSheet.doRunIntern()` mit `forceCreate()`), weil das Sheet dort gelöscht und neu angelegt wird.

## Rangliste beim Erstellen der ersten Runde anlegen

Der Listener legt keine Rangliste an, das passiert beim Erstellen der ersten Runde bzw. des Spielplans. Danach bleibt das neue Spielblatt aktiv (`setActiveSheet` nach dem Vollaufbau, der die Rangliste aktiv setzt).

| System | Stelle | Verhalten |
|---|---|---|
| Schweizer, Maastrichter-Vorrunde, Formule X | `*SpielrundeSheetNaechste.doRun()` nach Runde 1 | `getRanglistenAktualisierer()` (`*RanglisteSheetUpdate`: Vollaufbau, wenn sie fehlt). Ab Runde 2 läuft derselbe Aktualisierer wie bisher vor der neuen Runde. |
| Supermelee | `SpielrundeSheet_Naechste.doRun()` | Tagesrangliste wird nach jeder neuen Runde per `generate()` vollständig aufgebaut (jede Runde braucht eine eigene Spalte). Die Endrangliste wird weiterhin nicht automatisch angelegt. |
| JGJ, Liga, Trip-Tête | `*SpielPlanSheet.doRun()` | Rangliste wird nur angelegt, wenn sie fehlt. Eine vorhandene Rangliste bleibt unverändert. |
| Poule | `PouleVorrundeSheet.doRun()` | Vorrunden-Rangliste wird nur angelegt, wenn sie fehlt. |

Kaskade (Gruppenrangliste erst nach allen Kaskadenrunden) und KO (keine Rangliste) sind bewusst ausgenommen. Die TestDaten-Klassen überschreiben `doRun()` bzw. erben nicht von `*Naechste` und legen ihre Ranglisten weiterhin selbst an.

## Geänderte Teamzahl im Update

Die `*RanglisteSheetUpdate`-Klassen schreiben nur den Datenbereich neu. Vorher müssen alte Zeilen weg:
- `RanglisteUpdateHelper.loescheDatenzeilen` (Schweizer, Maastrichter, Formule X, JGJ, Liga, Supermelee) leert immer bis einschließlich der alten Fußzeile (`FUSSZEILEN_ABSTAND` unter der alten letzten Datenzeile). Kommen Teams hinzu, beginnt das Leeren ab der alten letzten Datenzeile, weil die alte, verbundene Fußzeile sonst mitten in den neuen Daten läge.
- `RanglisteUpdateHelper.loescheAlteZeilenBeiGeaenderterAnzahl` (Trip-Tête, Poule) leert nur, wenn sich die Teamzahl geändert hat.

`clearRange()` löscht auch die Attribute und hebt damit die Zellverbindung der alten Fußzeile auf (LO: `ScAttrArray::DeleteArea` → `RemoveAreaMerge`). Absicherung: `RanglisteTeamAnzahlAenderungUITest`.
