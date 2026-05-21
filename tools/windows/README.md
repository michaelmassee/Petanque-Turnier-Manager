# Windows-Freeze-Repro

Reproduktions-Werkzeug für den Calc-Freeze nach Turnier-Erstellung
(siehe `~/tmp/winboat/info-5.log`, Session S2). Läuft komplett unter
Windows, sammelt alle Logs, die zum Diagnostizieren eines VCL-/Thread-Deadlocks
nötig sind.

## Was wird gemacht

Schleife über mehrere Iterationen:

1. Calc-Mappe öffnen.
2. `ptm:SpieltagRanglisteSheet_TestDaten` dispatchen → Supermelee-Beispielturnier
   wird erstellt.
3. Sheet-Switch-Storm: ca. 20 Sheets aus dem fertigen Turnier in
   semi-zufälliger Reihenfolge aktivieren (Spieltage, Anmeldungen,
   Spielrunden) — das ist der Pfad, der in S2 unmittelbar vor dem Freeze lief.
4. Nach jedem Switch kurze Liveness-Probe (UNO-Property-Read mit 10-s-Timeout).

Parallel:

- Watcher zieht alle 5 s einen Java-Thread-Dump per `jcmd Thread.print`.
- LO läuft mit `SAL_LOG=+INFO.vcl+INFO.framework+INFO.svtools+INFO.sd`.
- PTM-Plugin-Log auf DEBUG (Standard).
- Optional ProcMon-Trace im Hintergrund.

Wenn der Freeze auftritt: Liveness-Probe schlägt aus → finaler Thread-Dump
wird erzwungen, alle Logs landen in einem Ordner und werden gezippt.

## Voraussetzungen

| Voraussetzung | Pfad/Wert (Standard) |
|---|---|
| LibreOffice 26.x mit OXT installiert | `C:\Program Files\LibreOffice\program\soffice.exe` |
| LO-eigenes Python (für UNO-Driver) | `C:\Program Files\LibreOffice\program\python.exe` |
| JDK mit `jcmd` im PATH | Adoptium 25 (laut Log bereits da) |
| PowerShell 5+ | Bordmittel |
| Optional: Sysinternals `Procmon.exe` | per `winget install Microsoft.Sysinternals.ProcessMonitor` |

## Aufruf

In PowerShell (im Repo-Wurzelverzeichnis):

```powershell
# Schneller Smoke-Lauf, 1 Iteration, ohne ProcMon
.\tools\windows\freeze-repro.ps1 -Iterations 1

# Voller Repro-Lauf, 10 Iterationen, mit ProcMon
.\tools\windows\freeze-repro.ps1 -Iterations 10 -WithProcMon

# Eigener Output-Pfad
.\tools\windows\freeze-repro.ps1 -OutputRoot D:\ptm-freeze -Iterations 5
```

Output-Verzeichnis (Standard `%USERPROFILE%\ptm-freeze\<timestamp>\`):

```
soffice.log               # SAL_LOG
ptm-debug.log             # %USERPROFILE%\.petanqueturniermanager\info.log nach Lauf
ptm-debug.log.pre         # dito vor Lauf (Baseline für Diff)
threaddump-*.txt          # jcmd-Snapshots alle 5 s
threaddump-FREEZE-*.txt   # Extra-Dump direkt nach Freeze-Verdacht
driver.log                # Schritt-für-Schritt des UNO-Drivers
watcher.log               # jcmd- und Liveness-Probe-Output
master.log                # Master-Skript-Trace
procmon.pml               # nur wenn -WithProcMon
freeze-detected.flag      # vorhanden, wenn Freeze festgestellt
```

Ergebnis-ZIP unter `<OutputRoot>\ptm-freeze-<timestamp>.zip`.

## Architektur

```
freeze-repro.ps1    — Master (Prozesse starten, Env setzen, Output sammeln, ZIP)
  ├─ soffice.exe    — LO Calc mit URP-Listener auf Port 2083
  ├─ freeze-watch.ps1 (Background-Job) — jcmd + Liveness-Probe
  └─ freeze-driver.py — UNO-Driver in LO-Python (Dispatch + Sheet-Storm)
```

### Warum Python und nicht PowerShell für den UNO-Driver?

LO bringt unter `program\python.exe` einen kompletten Python-Interpreter mit
`uno`-Modul mit. Python-UNO ist die offizielle, dokumentierte Bridge unter
Windows. Eine reine PowerShell-UNO-Anbindung wäre nur über die
.NET-COM-Bridge möglich, die fragil und schlecht dokumentiert ist. Der Master
bleibt PowerShell (orchestriert, sammelt Logs, ZIP).

## Was tun, wenn der Freeze reproduziert wird

1. `threaddump-<letzter Timestamp vor Freeze>.txt` öffnen.
2. Suchen nach Threads im Status `BLOCKED` / `WAITING (on object monitor)`.
3. PTM-Plugin-Threads (`SheetRunner`, `Debouncer`, `Refresh`) auf VCL-/UNO-Calls
   prüfen — Deadlock-Kandidat ist meist ein Plugin-Thread, der in
   `XComponent`/`XSpreadsheet` hängt.
4. `soffice.log` zeigt die letzte VCL-Aktion vor dem Hang.
5. `ptm-debug.log` zeigt die letzte PTM-Aktion (sollte mit einer
   Listener-Methode enden, wenn die Hypothese stimmt).

## Vorlage

Der `jcmd`-Polling-Loop des Watchers ist 1:1 von `tools/lo-threaddump.sh`
übernommen — gleiche Logik, nur PowerShell-Syntax.
