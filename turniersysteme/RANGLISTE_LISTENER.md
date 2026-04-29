# RanglisteRefreshListener – Architekturregeln

## Problem: Race Condition bei forceCreate()

`NewSheet.forceCreate().create()` **löscht** das bestehende Sheet und legt es neu an. Das triggert LibreOffice-interne Events (u.a. `selectionChanged`). Wenn der `RanglisteRefreshListener` dabei das Rangliste-Sheet als aktiv erkennt **und** `SheetRunner.isRunning() == false` ist (was bei direkten `doRun()`-Aufrufen immer der Fall ist, da diese den `SheetRunnerKoordinator` umgehen), startet der Listener sofort einen zweiten parallelen Thread → beide Threads schreiben gleichzeitig auf dasselbe Sheet → Datenverlust/Korruption.

## Regel: Listener müssen `*SheetUpdate`-Klassen verwenden

**NIEMALS** eine `*RanglisteSheet`-Klasse (Vollaufbau) direkt im `RanglisteRefreshListener` registrieren.
**IMMER** eine `*RanglisteSheetUpdate`-Klasse (nur Datenbereich) verwenden.

| Listener-Registrierung in `PetanqueTurnierMngrSingleton` | Klasse |
|---|---|
| Schweizer Rangliste | `SchweizerRanglisteSheetUpdate` |
| Maastrichter Vorrunden-Rangliste | `MaastrichterVorrundenRanglisteSheetUpdate` |
| Formule X Rangliste | `FormuleXRanglisteSheetUpdate` |
| Poule Vorrunden-Rangliste | `PouleVorrundenRanglisteSheetUpdate` |

## Muster für neue Turniersysteme

Jedes neue Turniersystem, das einen `RanglisteRefreshListener` bekommt, benötigt eine `*SheetUpdate`-Klasse:

1. `FooRanglisteSheet` – vollständiger Aufbau (Menüaktion, Erstaufbau): verwendet `NewSheet.forceCreate()`
2. `FooRanglisteSheetUpdate extends FooRanglisteSheet` – Update-Pfad (Listener):
   - Überschreibt `doRun()`: **kein** `forceCreate`, kein Sheet-Event, keine Race Condition
   - Prüft ob Sheet existiert; falls nicht → Fallback auf `FooRanglisteSheet.doRun()` (Erstaufbau)
   - Schreibt nur den Datenbereich neu via `berechnungUndSchreiben()` (shared protected method)
   - Löscht überzählige Zeilen wenn Teamanzahl gesunken ist (via `RanglisteUpdateHelper.loescheDatenzeilen()`)
   - Registrierung in `PetanqueTurnierMngrSingleton` via `RanglisteRefreshListener.fuerSchluessel(..., (ws, ignored) -> new FooRanglisteSheetUpdate(ws))`

## `setActiveSheet()` – nur im SheetRunner-Kontext

`getSheetHelper().setActiveSheet(sheet)` **nur aufrufen wenn `SheetRunner.isRunning() == true`** (d.h. der Aufruf kommt vom Menü über `SheetRunner.run()`). Bei direktem `doRun()`-Aufruf (Listener, Test) darf `setActiveSheet` **nicht** aufgerufen werden – das würde erneut `selectionChanged` feuern.

## Dirty-Flag-Optimierung: Update nur wenn nötig

`RanglisteRefreshListener` hält einen `RanglisteDirtyFlagTracker`, der pro Dokument
verfolgt ob seit dem letzten Rangliste-Update eine Änderung stattfand. Updates werden
übersprungen wenn kein Dirty-Flag gesetzt ist.

**Mechanismus:**
1. **Initialisierung**: Dirty-Flag startet `true` → erste Tab-Switch löst immer Update aus
2. **Änderungserkennung**: `XModifyListener` am Dokument setzt Flag auf `true` bei jeder Änderung
3. **Check-and-Consume**: `isDirtyUndConsume()` prüft und setzt das Flag atomar zurück auf `false`

**Reihenfolge in `selectionChanged()` und `onFocus()`:**
1. `consumeSelectionChangeSuppression()` – immer zuerst (auch bei clean-Dokument)
2. `isDirtyUndConsume()` – danach Dirty-Flag prüfen

**Edge Cases:**
- Flag nicht registriert (`null`) → Failsafe dirty=true (kein Update wird unterdrückt)
- `XModifyBroadcaster` nicht verfügbar → Graceful Degradation: Dokument bleibt initial dirty=true, Updates erfolgen bedingungslos
- Neue Spielrunde / neue Teilnehmer → Menü-Aktion modifiziert Dokument → dirty=true → nächster Tab-Switch triggert Update
- Mehrere Dokumente → `WeakHashMap` mit `AtomicBoolean` pro Dokument (automatische GC bei Schließen)
