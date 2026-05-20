# UI-Freeze: weißes Startfenster + schwarze Toolbar-Bereiche

Stand: 2026-05-20, Version 5.30.4 (Branch `lo-crash-2`).

## Symptome (User-Report, Dirk, Windows 11 + LO 25.8.6.2)

1. **Weißes Calc-Fenster beim Start** – bis ca. 20 s, nicht zuverlässig reproduzierbar.
   Auf dem Screenshot (`Screenshot_2026-05-20_Calc_weisse_Bereiche.png`) sind weder
   Standard-Menüleiste noch Toolbar gezeichnet – nur Titelleiste + Fensterrahmen.
2. **Schwarze Bereiche statt Schaltflächen in der Plugin-Toolbar während
   Beispielturnier-Generierung**, behoben durch Minimize/Restore.
   Screenshot `Screenshot_2026-05-20_Calc_schwarze_Bereiche.png` zeigt
   Plugin-Toolbar mit dunklen/leeren Slots zwischen Icons.

Auf Linux nicht reproduzierbar.

## Warum Linux das nicht zeigt

- Symptom 1: X11/Wayland-Compositoren stellen den letzten gemalten Frame dar,
  während die App blockiert. Windows (GDI/D3D) füllt invalide Regionen weiß
  bzw. lässt sie schwarz, bis die App `WM_PAINT` beantwortet.
- Antivirus auf Windows scannt JAR-Entries beim ersten Class-Load und Temp-File-
  Schreibungen – beides explodiert exakt auf den heißen Pfaden, die hier
  gefunden wurden.
- D3D-Frame-Compositor von LO unter Windows hält offenbar die ganze
  Top-Window-Region "dirty", solange irgendein Sub-Renderer locked ist
  (siehe ControllerLock weiter unten). Linux/Wayland nicht.

## Hauptverdacht 1: `ProcessBox.initDialog()` (Commit `1e7c7ce0`, ab 5.30.2)

Vor 5.30.2 war die ProcessBox ein Swing-`JFrame`, der im Java-AWT-EDT initialisiert
wurde – also **nicht** auf dem LO-Main-Thread. Mit dem Umbau auf einen UNO-Dialog
mit Throbber läuft der komplette Dialog-Aufbau jetzt synchron im
`ProtocolHandler`-Konstruktor (über `PetanqueTurnierMngrSingleton.init` →
`ProcessBox.init`).

Was `ProcessBox.initDialog()` (`src/main/java/de/petanqueturniermanager/helper/msgbox/ProcessBox.java:223-382`)
auf dem LO-Main-Thread macht:

| Schritt | Operation | Anmerkung |
|--------|-----------|-----------|
| 1 | `createInstanceWithContext("com.sun.star.awt.AsyncCallback", …)` | UNO-Service-Lookup |
| 2 | `createInstanceWithContext("com.sun.star.awt.UnoControlDialogModel", …)` | Service-Lookup |
| 3 | 8× `setPropertyValue` auf `dlgProps` | Position, Size, Moveable, … |
| 4 | 11× `addControl(...)` | jeder Call: `msf.createInstance` + 5 Base-`setPropertyValue` + 1–3 Spezial-Properties + `container.insertByName` → **≈80 UNO-Calls** |
| 5 | 2× `extractImageToTemp(...)` (Z. 404-419) | `getResourceAsStream` + `Files.createTempFile` + `Files.copy` + `deleteOnExit()` – **Disk-IO**, AV-anfällig |
| 6 | `createInstanceWithContext("com.sun.star.awt.UnoControlDialog", …)` | Service-Lookup |
| 7 | `createInstanceWithContext("com.sun.star.awt.Toolkit", …)` | Service-Lookup |
| 8 | **`dialogControl.createPeer(xToolkit, null)`** (Z. 356) | **teuerster Einzel-Call**: legt nativen Window-Peer an (Windows: HWND-Allokation, GDI-Resources, ggf. D3D-/Skia-Init für verstecktes Fenster). Einmaliger Kalt-Pfad pro Calc-Session. |
| 9 | `addTopWindowListener` + 2× `addActionListener` | UNO-Listener-Registrierung |

**Summe: ≈100 UNO-Roundtrips + 2 Disk-Schreiboperationen + 1 native Peer-Allokation**,
alles seriell auf dem Main-Thread, ausgelöst aus dem `ProtocolHandler`-Konstruktor,
also bevor LO sein eigenes UI fertig zeichnen kann.

## Hauptverdacht 2: `ControllerLock` um `SheetRunner.run()` (Commit `2a6b004a`, ab 5.30.2)

`src/main/java/de/petanqueturniermanager/helper/sheet/ControllerLock.java` ist ein
schlanker `AutoCloseable`-Wrapper um `XModel.lockControllers()` /
`unlockControllers()`. In `SheetRunner.run()` umklammert er den **kompletten**
Lauf inklusive `finally`-Block.

**Motivation laut Commit:** Windows-11-Renderer-Crash (D3D, `scfiltlo` +
`D3DScreenUpdateManager`) bei hunderten unbatchter UNO-Property-Writes pro Lauf.
`lockControllers()` unterdrückt den Repaint zwischen den Writes → kein
Render-Flush mehr pro Property → kein Crash.

**Nebenwirkung:** Auf Windows mit D3D-Frame-Compositor bleibt die gesamte
Top-Window-Region "dirty", solange irgendein Sub-Renderer (Sheet-View) locked
ist. Damit werden auch Toolbar-/Sidebar-Repaints für die Lauf-Dauer angehalten.
`notifyAllListeners()` + `SpieltagToolbarSteuerung.aktualisiereInAllenFrames()`,
die während des Laufs durch unsere eigenen Event-Listener mehrfach gefeuert
werden, **können den Repaint nicht durchsetzen** – Ergebnis sind die schwarzen
Toolbar-Slots auf Dirks Screenshot.

Auf Linux: Compositor zeigt einfach den letzten Frame, daher unsichtbar.

**Zusatz-Effekt aus demselben Commit:** `CellStyleHelper.apply()` läuft jetzt
zweigleisig (`tryEnsureUnprotectedInScope()` im Scope), `BlattschutzManager`
ebenfalls mit mehr Lazy-Logik. Diese Pfade laufen jetzt unter dem Lock und
können die D3D-Render-Queue im "locked"-Zustand zusätzlich belasten.

## Fix-Ansätze

### Für `ProcessBox.init` (Symptom 1)

**Empfohlen: Lazy Dialog-Aufbau.**

- Konstruktor speichert nur `xContext`, kein Service-Lookup, kein
  `createPeer`, keine Temp-Files.
- `initDialog()` läuft erst beim ersten Aufruf, der den Dialog wirklich
  *sichtbar* braucht (`visible()` / `toFront()` / `applyVordergrundEinstellung()`).
- `info(...)`-/`fehler(...)`-/`clear()`-Aufrufe puffern in einem In-Memory-Log
  (`StringBuilder` oder `Deque<String>` mit `MAX_LOG_CHARS`-Cap). Beim ersten
  echten Dialog-Aufbau wird der Puffer einmal an das dann existierende
  Edit-Control übergeben.
- Headless-Modus bleibt wie er ist.

Damit kostet der `ProtocolHandler`-Konstruktor nur noch:
Klassen-Load + Singleton-Anlage + Listener-Registrierungen → erwartete
Reduktion: 20 s → < 1 s.

**Risiko:** Niedrig. Das `ProcessBox`-API ist bereits defensiv (`if (disposed ||
logEditProps == null) return this;`), die Lazy-Variante baut auf demselben
Pattern auf.

### Für `ControllerLock` (Symptom 2)

Mehrere Optionen, in Reihenfolge des Erwartungswerts:

1. **Periodische Unlock-Yields.** Worker löst den Lock alle ~250 ms kurz,
   damit Frame-Repaints durchkommen, und setzt ihn sofort wieder. Erfordert
   Integration in die heißen Schreib-Helper (`RangeHelper.setDataInRange`,
   `CellStyleHelper.apply` im Scope). Aufwand mittel, Effekt vermutlich groß.

2. **Granularer Lock.** Lock nicht um `run()` insgesamt, sondern nur um die
   wirklich heißen Schreib-Schleifen (innerhalb `RangeHelper.setDataInRange`,
   maximal um `doRun()`, aber nicht um die `finally`-Cleanup-Phase – damit
   ProcessBox-Ready-Update + Auto-Save Repaints kriegen). Aufwand größer,
   weil pro Generator-Klasse das Scope-Pattern eingepflegt werden muss.

3. **Lock-Replacement durch `enableAutomaticCalculation(false)` + manuellen
   `calculate()` am Ende.** Greift den ursprünglichen D3D-Crash-Mechanismus
   nicht (Crash ging über Property-Writes, nicht Recalcs), Crash-Schutz fiele
   weg.

4. **Skia-Backend statt D3D empfehlen** – Workaround, kein Fix; nur als
   Fallback für Anwender mit hartnäckigem Problem.

**Empfohlene Reihenfolge:** Erst Lazy-ProcessBox (großer Hebel, kleines Risiko),
dann `[WORKER-TIMING]`-Logs aus Dirks Lauf auswerten. Falls die schwarzen
Bereiche bestehen bleiben, Variante 1 (Yield-Pattern) als zweiten Schritt.

## Vor Fix: Diagnose-Logging (in 5.30.4, Commit `4b4b01f3`)

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

**Diagnose-Run für Dirk:**

1. Version 5.30.4 installieren.
2. Calc starten (Weißwand abwarten).
3. Ein Beispielturnier generieren (Schweizer oder Supermêlée).
4. Plugin-Menü → Info → Logfile anzeigen.
5. Zeilen mit `[STARTUP-TIMING]` und `[WORKER-TIMING]` zurückschicken.

## Was sich zwischen 5.30.0 und 5.30.2 änderte – Kurzliste

Aus `git log --oneline 757c5475..25bf00e9`:

| Commit | Inhalt | Relevanz |
|--------|--------|----------|
| `1e7c7ce0` | ProcessBox: Swing-JFrame durch UNO-Dialog mit LO-Throbber ersetzt | **Hauptverdacht Symptom 1** |
| `b370e87d` | ProcessBox: `java.awt.Toolkit.beep()` entfernt | Verwandt (AWT-Init im LO-Prozess) |
| `e15bb756` | ProcessBox: Auto-Scroll zur letzten Log-Zeile beim Anhängen | klein |
| `e06b0c9d` | ProcessBox: Klick auf X im Fensterrahmen blendet Dialog aus | klein |
| `5a47d7fc` | ProcessBox: Fehlerstatus-Reset, Log-Begrenzung, Prefix-JavaDoc | klein |
| `792503ae` | ProcessBox: UI-Updates auf LO-Main-Thread marshallen (Fix LO-Crash) | reduziert Marshalling-Risiko – nicht ursächlich |
| `2a6b004a` | SheetRunner: ControllerLock um `run()` gegen LO-Render-Crash | **Hauptverdacht Symptom 2** |
| `0ada9816` | Blattschutz: `endCommandScope` schützt immer am Scope-Ende | unter Lock relevant |

Ältere Versionen (vor 5.30.1) zeigten weder die Weißwand noch die schwarzen
Toolbar-Bereiche, wenn die Hypothesen stimmen.

## Beteiligte Dateien (Kurzliste)

- `src/main/java/de/petanqueturniermanager/helper/msgbox/ProcessBox.java`
  – `initDialog()` (Z. 223-382) ist der Lazy-Kandidat
- `src/main/java/de/petanqueturniermanager/SheetRunner.java`
  – `run()` mit `try (ControllerLock _ = ControllerLock.lock(doc))` um den
  gesamten Lauf
- `src/main/java/de/petanqueturniermanager/helper/sheet/ControllerLock.java`
  – schlanker Wrapper, würde um Yield-/Re-Entry-API erweitert
- `src/main/java/de/petanqueturniermanager/helper/sheet/blattschutz/BlattschutzManager.java`
  – läuft im Lock-Scope, könnte mit-yielden
- `src/main/java/de/petanqueturniermanager/helper/cellstyle/CellStyleHelper.java`
  – Apply-Pfad läuft jetzt unter Lock
- `src/main/java/de/petanqueturniermanager/comp/PetanqueTurnierMngrSingleton.java`
  – Ruf-Kette zum `ProcessBox.init` (Z. 107)
- `src/main/java/de/petanqueturniermanager/comp/ProtocolHandler.java`
  – Konstruktor blockiert auf der Init-Kette
