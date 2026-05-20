# UI-Freeze: weiße Bereiche + schwarze Toolbar-Bereiche

Stand: 2026-05-20, Version 5.30.6 (Branch `lo-crash-2`).

> **Update 5.30.6 (info-4.log, Windows-11-VM):** Die ursprünglich vermutete
> Startup-Weißwand durch `ProcessBox.initDialog()` ist **widerlegt**. Hauptursache
> ist allein der `ControllerLock` um `SheetRunner.run()`. Details siehe unten.

## Symptome (User-Report, Dirk, Windows 11 + LO 25.8.6.2)

1. **Weißes Calc-Fenster beim Start** – bis ca. 20 s, nicht zuverlässig reproduzierbar.
   Auf dem Screenshot (`Screenshot_2026-05-20_Calc_weisse_Bereiche.png`) sind weder
   Standard-Menüleiste noch Toolbar gezeichnet – nur Titelleiste + Fensterrahmen.
2. **Schwarze Bereiche statt Schaltflächen in der Plugin-Toolbar während
   Beispielturnier-Generierung**, behoben durch Minimize/Restore.
   Screenshot `Screenshot_2026-05-20_Calc_schwarze_Bereiche.png` zeigt
   Plugin-Toolbar mit dunklen/leeren Slots zwischen Icons.

In der 5.30.6-Messung wurde Symptom 1 **nicht** reproduziert (LO erreicht
`OnViewCreated` nach jvm-uptime 1805 ms, Plugin-Init ist nach 1129 ms fertig).
Wahrscheinliche Erklärungen für Dirks Originalbericht: Antivirus-Scan beim
ersten Class-Load der OXT-JARs, JAR-Verifikation, Profil-Migration. Falls
Symptom 1 bei einem User stabil wiederkehrt, ist es vermutlich aus dem
Plugin-Pfad nicht weiter zu fixen — siehe Skia-Empfehlung unten.

Symptom 2 (schwarze Toolbar-Bereiche während Lauf) ist durch die Messung
**bestätigt** und unten als alleiniger Hauptverdacht geführt.

Auf Linux nicht reproduzierbar.

## Warum Linux das nicht zeigt

- Symptom 2: X11/Wayland-Compositoren stellen den letzten gemalten Frame dar,
  während die App blockiert. Windows (GDI/D3D) füllt invalide Regionen weiß
  bzw. lässt sie schwarz, bis die App `WM_PAINT` beantwortet.
- D3D-Frame-Compositor von LO unter Windows hält offenbar die ganze
  Top-Window-Region "dirty", solange irgendein Sub-Renderer locked ist
  (siehe `ControllerLock` weiter unten). Linux/Wayland nicht.

## Messung Version 5.30.6 (info-4.log)

`[STARTUP-TIMING]` (Auszug):

| Phase | Dauer |
|---|---|
| JVM-Uptime beim Plugin-Init | 687 ms |
| `ProcessBox.init` (UNO-Dialog-Aufbau) | **72 ms** |
| `PetanqueTurnierMngrSingleton.init` GESAMT | 436 ms |
| `ProtocolHandler`-ctor GESAMT | 567 ms |
| Erster `OnViewCreated` | jvm-uptime 1805 ms |
| Erster `OnNew` | jvm-uptime 1831 ms |
| Erster User-Dispatch (`spielrunden_testdaten`) | jvm-uptime 17 646 ms |

`[WORKER-TIMING]` (Auszug):

| SheetRunner | Dauer |
|---|---|
| `SpielrundeSheet_TestDaten` | 10 134 ms |
| **`SpieltagRanglisteSheet_TestDaten`** | **56 243 ms** |
| `EndranglisteSheet` | 3 648 ms |
| `SpieltagRanglisteSheetUpdate` | 485–568 ms |
| `EndranglisteSheetUpdate` | 1 015–1 184 ms |

`TurnierEventHandler` koalesziert sauber (30 bzw. 24 Events → 1 Broadcast),
also kein Event-Storm.

## Verworfen nach 5.30.6-Messung: `ProcessBox.initDialog()`

Ursprünglicher Verdacht: Der UNO-Dialog-Aufbau in `ProcessBox.initDialog()`
(~100 UNO-Roundtrips, 2× Temp-File-IO, 1× nativer `createPeer`), aufgerufen
im `ProtocolHandler`-Konstruktor, blockiere den LO-Main-Thread vor dem ersten
Paint.

**Messung:** `ProcessBox.init` kostet **72 ms** auf der Windows-11-VM, der
gesamte `ProtocolHandler`-Konstruktor 567 ms. LO erreicht `OnViewCreated` nach
1805 ms. Damit kann der Startup-Pfad keine 20-s-Weißwand verursachen.

Konsequenz: Lazy-Umbau bleibt als saubere Hygiene-Option (Native-Peer-Allokation
und Temp-File-IO erst on demand), ist aber **nicht** mehr Symptom-Fix. Wenn er
gemacht wird, dann unabhängig — z. B. zusammen mit anderen Init-Hardenings.

Referenz-Stelle: `src/main/java/de/petanqueturniermanager/helper/msgbox/ProcessBox.java:223-382`.

## Hauptverdacht (alleinig): `ControllerLock` um `SheetRunner.run()` (Commit `2a6b004a`)

`src/main/java/de/petanqueturniermanager/helper/sheet/ControllerLock.java` ist
ein schlanker `AutoCloseable`-Wrapper um `XModel.lockControllers()` /
`unlockControllers()`. In `SheetRunner.run()` umklammert er den **kompletten**
Lauf inklusive `finally`-Block.

**Motivation laut Commit:** Windows-11-Renderer-Crash (D3D, `scfiltlo` +
`D3DScreenUpdateManager`) bei hunderten unbatchter UNO-Property-Writes pro
Lauf. `lockControllers()` unterdrückt den Repaint zwischen den Writes → kein
Render-Flush mehr pro Property → kein Crash.

**Nebenwirkung — durch info-4.log belegt:** Während eines 56-Sekunden-Laufs
(`SpieltagRanglisteSheet_TestDaten`) bleibt die D3D-Top-Window-Region "dirty".
Toolbar-Repaints (z. B. nach `ToolbarAnzeigenListener` oder
`SpieltagToolbarSteuerung.aktualisiereInAllenFrames()`) können nicht
durchsetzen — Resultat: schwarze Toolbar-Slots auf Dirks Screenshot. Bei sehr
langen Läufen (1 min) kann der User das Gesamtfenster zudem als "weiß" oder
"eingefroren" wahrnehmen, selbst wenn der Startup-Pfad sauber ist.

Auf Linux: Compositor zeigt einfach den letzten Frame, daher unsichtbar.

**Zusatz-Effekt aus demselben Commit:** `CellStyleHelper.apply()` läuft jetzt
zweigleisig (`tryEnsureUnprotectedInScope()` im Scope), `BlattschutzManager`
ebenfalls mit mehr Lazy-Logik. Diese Pfade laufen jetzt unter dem Lock und
verlängern die "locked"-Phase weiter.

## Fix-Ansätze (neu priorisiert)

### Priorität 1: Yield-Pattern für `ControllerLock`

Worker löst den Lock alle 250–500 ms kurz, damit Frame-Repaints durchkommen,
und setzt ihn sofort wieder. Integrationspunkte:

- `RangeHelper.setDataInRange` (Hauptlast bei den großen `SheetRunner`-Läufen)
- `CellStyleHelper.apply` im Scope
- ggf. zentrale Yield-API in `ControllerLock` (z. B.
  `ControllerLock.yieldIfDue()` mit Wandtaktzeit-Check)

Erwarteter Effekt: Toolbar-Repaints kommen alle ~250 ms durch → keine
schwarzen Slots mehr; der Crash-Schutz (kein einzelner Property-Flush
synchron rendert) bleibt für die jeweiligen Sub-Intervalle erhalten.

**Risiko:** Wenn der Crash-Mechanismus tatsächlich nur ab einer
*Mindest-Sweep-Länge* greift, kann ein Yield mitten im Sweep den Crash
zurückbringen. Vor Live-Roll mit `SpieltagRanglisteSheet_TestDaten` und
echtem 8000+-Zeilen-Sheet validieren.

### Priorität 2: Granularer Lock

Lock nicht um `run()` insgesamt, sondern nur um die wirklich heißen
Schreib-Schleifen (innerhalb `RangeHelper.setDataInRange`, maximal um
`doRun()`, aber nicht um die `finally`-Cleanup-Phase). Aufwand größer, weil
pro Generator-Klasse das Scope-Pattern eingepflegt werden muss, dafür
Crash-Schutz an den richtigen Stellen punktgenau.

### Priorität 3: Lazy `ProcessBox` (Hygiene, nicht Symptom-Fix)

Konstruktor speichert nur `xContext`. `initDialog()` läuft erst beim ersten
sichtbarkeits-relevanten Aufruf. `info(...)`-/`fehler(...)`-Aufrufe puffern
in einer `Deque<String>` mit `MAX_LOG_CHARS`-Cap.

**Nicht** mehr als Fix für die berichtete Weißwand verkauft — die Messung
zeigt 72 ms, nicht 20 s. Trotzdem sinnvoll, weil der native `createPeer`
und die Temp-File-IO im Startup-Pfad eine zukünftige Regressions-Falle sind
(AV-/JAR-Cache-Effekte beim ersten Class-Load).

### Notnägel

- `enableAutomaticCalculation(false)` + manueller `calculate()` am Ende
  greift den D3D-Crash-Mechanismus nicht (geht über Property-Writes, nicht
  Recalcs) — kein Ersatz für den Lock.
- **Skia-Backend statt D3D** dem User empfehlen, falls Symptom 1 (weiße
  Bereiche beim Start) bei ihm stabil wiederkehrt: `Extras → Optionen →
  LibreOffice → Ansicht → Skia für Rendering verwenden`. Reiner Workaround.

## Diagnose-Logging (in 5.30.4, Commit `4b4b01f3`)

Eingebaut sind Präfixe `[STARTUP-TIMING]` und `[WORKER-TIMING]` (info-Level)
in:

- `PetanqueTurnierMngrSingleton.init` – pro Abschnitt + Gesamt
- `ProtocolHandler`-Konstruktor – Singleton-init, Toolbar-Show, SpieltagToolbar,
  REGISTERED-Block + Gesamt
- `ToolbarAnzeigenListener.zeigeToolbarInFrame` – Dauer + Ergebnis pro Frame
- `ProtocolHandler.notifyAllListeners` – erster Broadcast (Dauer, Listener,
  Thread)
- `SheetRunner.run` – START/ENDE mit Klassenname und Dauer
- `TurnierEventHandler.trigger` – Dauer, Listener-Anzahl, Caller-Thread
- `RanglisteRefreshListener.pruefeUndStarte` – Key, System, Hash-Dauer,
  Gesamt-Dauer, Ergebnis-Typ, Thread

**Auswertungsfokus nach 5.30.6:** `[STARTUP-TIMING]` ist als Diagnose-Treiber
abgeräumt — Plugin-Init bewegt sich im < 1 s-Bereich. Künftige Runs primär
auf `[WORKER-TIMING]` schauen, insbesondere `SheetRunner.run`-Dauern > 5 s.

## Was sich zwischen 5.30.0 und 5.30.2 änderte – Kurzliste

Aus `git log --oneline 757c5475..25bf00e9`:

| Commit | Inhalt | Relevanz |
|--------|--------|----------|
| `1e7c7ce0` | ProcessBox: Swing-JFrame durch UNO-Dialog mit LO-Throbber ersetzt | nach 5.30.6-Messung verworfen (72 ms) |
| `b370e87d` | ProcessBox: `java.awt.Toolkit.beep()` entfernt | klein |
| `e15bb756` | ProcessBox: Auto-Scroll zur letzten Log-Zeile beim Anhängen | klein |
| `e06b0c9d` | ProcessBox: Klick auf X im Fensterrahmen blendet Dialog aus | klein |
| `5a47d7fc` | ProcessBox: Fehlerstatus-Reset, Log-Begrenzung, Prefix-JavaDoc | klein |
| `792503ae` | ProcessBox: UI-Updates auf LO-Main-Thread marshallen (Fix LO-Crash) | reduziert Marshalling-Risiko – nicht ursächlich |
| `2a6b004a` | SheetRunner: ControllerLock um `run()` gegen LO-Render-Crash | **alleiniger Hauptverdacht** |
| `0ada9816` | Blattschutz: `endCommandScope` schützt immer am Scope-Ende | unter Lock relevant |

## Beteiligte Dateien (Kurzliste)

- `src/main/java/de/petanqueturniermanager/SheetRunner.java`
  – `run()` mit `try (ControllerLock _ = ControllerLock.lock(doc))` um den
  gesamten Lauf
- `src/main/java/de/petanqueturniermanager/helper/sheet/ControllerLock.java`
  – schlanker Wrapper, Kandidat für Yield-/Re-Entry-API
- `src/main/java/de/petanqueturniermanager/helper/sheet/blattschutz/BlattschutzManager.java`
  – läuft im Lock-Scope, sollte mit-yielden
- `src/main/java/de/petanqueturniermanager/helper/cellstyle/CellStyleHelper.java`
  – Apply-Pfad läuft jetzt unter Lock
- `src/main/java/de/petanqueturniermanager/helper/sheet/RangeHelper.java`
  – Haupt-Schreib-Schleife, Hauptort für ein Yield-Pattern
- `src/main/java/de/petanqueturniermanager/helper/msgbox/ProcessBox.java`
  – `initDialog()` (Z. 223-382): nach Messung **kein** Symptom-Fix-Kandidat
  mehr, nur noch Hygiene-Optimierung
