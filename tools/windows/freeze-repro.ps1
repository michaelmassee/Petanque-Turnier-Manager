<#
.SYNOPSIS
  Master-Skript: orchestriert Windows-Calc-Freeze-Repro.

  Startet LO mit URP-Listener + verbose SAL_LOG, startet den Watcher
  (jcmd-Thread-Dumps + Liveness-Probe) als Background-Job, faehrt dann
  den UNO-Driver (Python) im Vordergrund, sammelt am Ende alle Logs und
  zipt das Output-Verzeichnis.

.PARAMETER OutputRoot
  Wurzelverzeichnis fuer Output (Default %USERPROFILE%\ptm-freeze).

.PARAMETER Iterations
  Anzahl der Turnier+SheetStorm-Iterationen (Default 5).

.PARAMETER WithProcMon
  ProcMon-Trace mitlaufen lassen (benoetigt Procmon.exe im PATH).

.PARAMETER SofficeExe
  Pfad zu soffice.exe (Default C:\Program Files\LibreOffice\program\soffice.exe).

.PARAMETER LoPython
  Pfad zu LO-eigenem python.exe.

.EXAMPLE
  .\freeze-repro.ps1 -Iterations 10 -WithProcMon
#>
[CmdletBinding()]
param(
    [string] $OutputRoot = 'C:\Users\michael\Desktop\Shared\tmp\ptm-freeze',
    [int]    $Iterations = 5,
    [switch] $WithProcMon,
    [string] $SofficeExe = 'C:\Program Files\LibreOffice\program\soffice.exe',
    [string] $LoPython = 'C:\Program Files\LibreOffice\program\python.exe',
    [int]    $UrpPort = 2083
)

$ErrorActionPreference = 'Stop'
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition

# Output-Verzeichnis
$ts = Get-Date -Format 'yyyyMMdd-HHmmss'
$outDir = Join-Path $OutputRoot $ts
New-Item -ItemType Directory -Path $outDir -Force | Out-Null
Write-Host "Output: $outDir"

$masterLog = Join-Path $outDir 'master.log'
function Write-Master([string] $msg) {
    $t = (Get-Date).ToString('HH:mm:ss.fff')
    $line = "$t  $msg"
    Add-Content -Path $masterLog -Value $line -Encoding UTF8
    Write-Host $line
}

Write-Master "=== freeze-repro start (iterations=$Iterations, procmon=$WithProcMon) ==="

# Voraussetzungen
foreach ($p in @($SofficeExe, $LoPython)) {
    if (-not (Test-Path $p)) { throw "Nicht gefunden: $p" }
}
if (-not (Get-Command jcmd -ErrorAction SilentlyContinue)) {
    throw 'jcmd nicht im PATH. JDK installieren / PATH ergaenzen.'
}

# Alle laufenden soffice-Prozesse killen, damit die neue Instanz nicht via
# Single-Instance-Manager an die alte delegiert (sonst wird --accept ignoriert
# und es kommt "Binary URP bridge disposed during call").
$existing = Get-Process -Name soffice, soffice.bin -ErrorAction SilentlyContinue
if ($existing) {
    Write-Master "kille $(@($existing).Count) laufende soffice(.bin)-Prozesse vor Start"
    $existing | Stop-Process -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 2
}

# Lock-Files im User-Profil aufraeumen (defensiv -- alte soffice-Reste).
# UserInstallation NICHT isolieren: das wuerde die produktiv installierte
# PTM-Extension nicht sehen.
$loUserDir = Join-Path $env:APPDATA 'LibreOffice\4\user'
Get-ChildItem -Path $loUserDir -Filter '.~lock.*' -ErrorAction SilentlyContinue | Remove-Item -Force -ErrorAction SilentlyContinue

# PTM-Plugin-Log: Plugin schreibt nach ${user.home}/.petanqueturniermanager/info.log
# (siehe src/main/resources/log4j2-test.xml).
$ptmLog = Join-Path $env:USERPROFILE '.petanqueturniermanager\info.log'
if (Test-Path $ptmLog) {
    Write-Master "PTM-Log gefunden: $ptmLog"
    Copy-Item $ptmLog (Join-Path $outDir 'ptm-debug.log.pre') -ErrorAction SilentlyContinue
}
else {
    Write-Master "PTM-Log noch nicht da, erwarte: $ptmLog"
}

# Env fuer Child-LO
$env:SAL_LOG = '+INFO.vcl+INFO.framework+INFO.svtools+INFO.sd'
$env:SAL_LOG_FILE = (Join-Path $outDir 'soffice.log')

# soffice mit URP-Listener starten
$accept = "socket,host=127.0.0.1,port=$UrpPort;urp;"
$sofficeArgs = @(
    '--calc',
    '--norestore',
    '--nologo',
    '--nodefault',
    "--accept=$accept"
)
Write-Master "starte soffice.exe $($sofficeArgs -join ' ')"
$soffice = Start-Process -FilePath $SofficeExe -ArgumentList $sofficeArgs -PassThru
Write-Master "soffice gestartet, PID=$($soffice.Id)"

# ProcMon optional
$procmon = $null
if ($WithProcMon) {
    $procmonCmd = Get-Command Procmon.exe -ErrorAction SilentlyContinue
    $procmonExe = if ($procmonCmd) { $procmonCmd.Source } else { $null }
    if (-not $procmonExe) {
        Write-Master 'WARN: -WithProcMon gesetzt, aber Procmon.exe nicht im PATH. Ueberspringe.'
    }
    else {
        $procmonPml = Join-Path $outDir 'procmon.pml'
        Write-Master "starte Procmon.exe -> $procmonPml"
        $procmon = Start-Process -FilePath $procmonExe -ArgumentList @(
            '/AcceptEula', '/Quiet', '/Minimized',
            '/BackingFile', $procmonPml
        ) -PassThru
    }
}

# Watcher als eigener Prozess (Start-Process, mit -ExecutionPolicy Bypass damit
# der Child-PS unsignierte .ps1 ausfuehren darf).
$watcherPs1 = Join-Path $scriptDir 'freeze-watch.ps1'
Write-Master "starte Watcher (Prozess) -> $watcherPs1"
$watcherProc = Start-Process -FilePath 'powershell.exe' -ArgumentList @(
    '-NoProfile', '-NonInteractive', '-ExecutionPolicy', 'Bypass',
    '-File', $watcherPs1,
    '-OutputDir', $outDir,
    '-LivenessPort', $UrpPort.ToString()
) -PassThru -WindowStyle Hidden
Write-Master "Watcher gestartet, PID=$($watcherProc.Id)"

# Driver im Vordergrund
Write-Master 'starte UNO-Driver (Python)'
$driverArgs = @(
    (Join-Path $scriptDir 'freeze-driver.py'),
    '--output-dir', $outDir,
    '--iterations', $Iterations.ToString(),
    '--ptm-log', $ptmLog
)
$driverRc = 0
try {
    & $LoPython @driverArgs
    $driverRc = $LASTEXITCODE
}
catch {
    Write-Master "Driver-Exception: $($_.Exception.Message)"
    $driverRc = 99
}
Write-Master "Driver Ende (rc=$driverRc)"

# stop.flag setzen, falls Driver vorzeitig starb
$stopFlag = Join-Path $outDir 'stop.flag'
if (-not (Test-Path $stopFlag)) {
    New-Item -ItemType File -Path $stopFlag -Force | Out-Null
}

# Watcher abwarten (Prozess endet selbstaendig, wenn stop.flag gesehen)
Write-Master 'warte auf Watcher-Ende'
if (-not $watcherProc.WaitForExit(15000)) {
    Write-Master "Watcher reagiert nicht nach 15 s -- Stop-Process PID $($watcherProc.Id)"
    Stop-Process -Id $watcherProc.Id -Force -ErrorAction SilentlyContinue
}

# ProcMon stoppen
if ($procmon) {
    Write-Master 'stoppe Procmon.exe'
    $procmonCmd2 = Get-Command Procmon.exe -ErrorAction SilentlyContinue
    if ($procmonCmd2) {
        Start-Process -FilePath $procmonCmd2.Source -ArgumentList '/Terminate' -Wait
    }
}

# PTM-Log einsammeln (post)
if (Test-Path $ptmLog) {
    Copy-Item $ptmLog (Join-Path $outDir 'ptm-debug.log') -ErrorAction SilentlyContinue
}
else {
    Write-Master "PTM-Log nach Lauf nicht gefunden: $ptmLog"
}

# soffice beenden (sanft, dann hart)
if (Get-Process -Id $soffice.Id -ErrorAction SilentlyContinue) {
    Write-Master "beende soffice (PID $($soffice.Id))"
    try {
        $soffice.CloseMainWindow() | Out-Null
        if (-not $soffice.WaitForExit(8000)) {
            Stop-Process -Id $soffice.Id -Force
        }
    }
    catch {
        Write-Master "soffice-stop-fehler: $($_.Exception.Message)"
    }
}

# Stop-Flag entfernen, damit naechster Lauf sauber ist
Remove-Item $stopFlag -Force -ErrorAction SilentlyContinue

# ZIP
$zip = Join-Path $OutputRoot "ptm-freeze-$ts.zip"
Write-Master "zippe -> $zip"
Compress-Archive -Path (Join-Path $outDir '*') -DestinationPath $zip -Force

Write-Master "=== freeze-repro ende (driver-rc=$driverRc) ==="
Write-Host ""
Write-Host "Output:  $outDir"
Write-Host "ZIP:     $zip"
if (Test-Path (Join-Path $outDir 'freeze-detected.flag')) {
    Write-Host ""
    Write-Host "*** FREEZE DETECTED ***  Thread-Dumps + Logs liegen in $outDir" -ForegroundColor Red
}
