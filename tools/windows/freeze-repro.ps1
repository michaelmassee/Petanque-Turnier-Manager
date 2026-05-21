<#
.SYNOPSIS
  Master-Skript: orchestriert Windows-Calc-Freeze-Repro.

  Startet LO mit URP-Listener + verbose SAL_LOG, startet den Watcher
  (jcmd-Thread-Dumps + Liveness-Probe) als Background-Job, fährt dann
  den UNO-Driver (Python) im Vordergrund, sammelt am Ende alle Logs und
  zipt das Output-Verzeichnis.

.PARAMETER OutputRoot
  Wurzelverzeichnis für Output (Default %USERPROFILE%\ptm-freeze).

.PARAMETER Iterations
  Anzahl der Turnier+SheetStorm-Iterationen (Default 5).

.PARAMETER WithProcMon
  ProcMon-Trace mitlaufen lassen (benötigt Procmon.exe im PATH).

.PARAMETER SofficeExe
  Pfad zu soffice.exe (Default C:\Program Files\LibreOffice\program\soffice.exe).

.PARAMETER LoPython
  Pfad zu LO-eigenem python.exe.

.EXAMPLE
  .\freeze-repro.ps1 -Iterations 10 -WithProcMon
#>
[CmdletBinding()]
param(
    [string] $OutputRoot = (Join-Path $env:USERPROFILE 'ptm-freeze'),
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

function Write-Master([string] $msg) {
    $t = (Get-Date).ToString('HH:mm:ss.fff')
    "$t  $msg" | Tee-Object -FilePath (Join-Path $outDir 'master.log') -Append | Write-Host
}

Write-Master "=== freeze-repro start (iterations=$Iterations, procmon=$WithProcMon) ==="

# Voraussetzungen
foreach ($p in @($SofficeExe, $LoPython)) {
    if (-not (Test-Path $p)) { throw "Nicht gefunden: $p" }
}
if (-not (Get-Command jcmd -ErrorAction SilentlyContinue)) {
    throw 'jcmd nicht im PATH. JDK installieren / PATH ergänzen.'
}

# LO-Profil-Locks säubern, damit soffice frisch startet
$loUserDir = Join-Path $env:APPDATA 'LibreOffice\4\user'
$lockFile = Join-Path $loUserDir '.~lock.*'
Get-ChildItem -Path $loUserDir -Filter '.~lock.*' -ErrorAction SilentlyContinue | Remove-Item -Force -ErrorAction SilentlyContinue
$singleLock = Join-Path $loUserDir 'singletons'
if (Test-Path $singleLock) {
    Get-ChildItem -Path $singleLock -Force -ErrorAction SilentlyContinue | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue
}

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

# Env für Child-LO
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
        Write-Master 'WARN: -WithProcMon gesetzt, aber Procmon.exe nicht im PATH. Überspringe.'
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

# Watcher als Background-Job
Write-Master 'starte Watcher (Background-Job)'
$watcherJob = Start-Job -ScriptBlock {
    param($scriptDir, $outDir, $urpPort)
    & (Join-Path $scriptDir 'freeze-watch.ps1') -OutputDir $outDir -LivenessPort $urpPort
} -ArgumentList $scriptDir, $outDir, $UrpPort

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

# Watcher abwarten
Write-Master 'warte auf Watcher-Ende'
Wait-Job $watcherJob -Timeout 15 | Out-Null
Receive-Job $watcherJob -Keep | Out-File (Join-Path $outDir 'watcher-job.out') -Encoding UTF8
Remove-Job $watcherJob -Force

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

# Stop-Flag entfernen, damit nächster Lauf sauber ist
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
