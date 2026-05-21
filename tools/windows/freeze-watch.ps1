<#
.SYNOPSIS
  Watcher für den Windows-Freeze-Repro.
  Pollt den laufenden soffice.bin-Prozess, zieht alle 5 s einen
  Java-Thread-Dump und macht parallel eine simple Liveness-Probe.
  Endet, wenn `stop.flag` im OutputDir auftaucht oder soffice.bin weg ist.

.PARAMETER OutputDir
  Verzeichnis, in das Thread-Dumps und Watcher-Log geschrieben werden.

.PARAMETER Interval
  Polling-Intervall in Sekunden (Default 5).

.PARAMETER LivenessPort
  Port, auf dem soffice.bin URP-Listener lauscht (für Liveness-Probe via
  TCP-Connect; eine echte UNO-Probe wäre teurer und liefe Gefahr,
  selber zu hängen).
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)] [string] $OutputDir,
    [int] $Interval = 5,
    [int] $LivenessPort = 2083
)

$ErrorActionPreference = 'Stop'
$watcherLog = Join-Path $OutputDir 'watcher.log'
$stopFlag = Join-Path $OutputDir 'stop.flag'
$freezeFlag = Join-Path $OutputDir 'freeze-detected.flag'

function Write-WatcherLog([string] $line) {
    $ts = (Get-Date).ToString('HH:mm:ss.fff')
    "$ts  $line" | Out-File -FilePath $watcherLog -Append -Encoding UTF8
}

# jcmd lokalisieren (kompatibel zu PS5.1 — kein ?.-Operator)
$jcmdCmd = Get-Command jcmd -ErrorAction SilentlyContinue
if (-not $jcmdCmd) {
    Write-WatcherLog 'FEHLER: jcmd nicht im PATH gefunden.'
    exit 1
}
$jcmd = $jcmdCmd.Source
Write-WatcherLog "jcmd: $jcmd"

# Auf soffice.bin warten (max 60 s). NICHT '$ofpid' verwenden — Automatic-Variable.
$ofpid = $null
for ($i = 0; $i -lt 60; $i++) {
    $proc = Get-Process -Name soffice.bin -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($proc) { $ofpid = $proc.Id; break }
    Start-Sleep -Seconds 1
}
if (-not $ofpid) {
    Write-WatcherLog 'FEHLER: soffice.bin nach 60 s nicht gefunden.'
    exit 1
}
Write-WatcherLog "soffice.bin PID=$ofpid"

$lastFreezeDumped = $false

while ($true) {
    if (Test-Path $stopFlag) {
        Write-WatcherLog 'stop.flag gesehen — beende.'
        break
    }
    if (-not (Get-Process -Id $ofpid -ErrorAction SilentlyContinue)) {
        Write-WatcherLog "PID $ofpid weg — soffice.bin beendet."
        break
    }

    $ts = (Get-Date).ToString('yyyyMMdd-HHmmss-fff')
    $dumpFile = Join-Path $OutputDir "threaddump-$ts.txt"

    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    & $jcmd $ofpid Thread.print > $dumpFile 2>&1
    $rc = $LASTEXITCODE
    $sw.Stop()
    $lines = (Get-Content $dumpFile -ErrorAction SilentlyContinue).Count
    Write-WatcherLog "thread-dump  rc=$rc  ms=$($sw.ElapsedMilliseconds)  lines=$lines  -> $dumpFile"

    # Liveness-Probe: kann sich der TCP-Listener öffnen lassen?
    try {
        $client = New-Object System.Net.Sockets.TcpClient
        $iar = $client.BeginConnect('127.0.0.1', $LivenessPort, $null, $null)
        $ok = $iar.AsyncWaitHandle.WaitOne(2000)
        if ($ok -and $client.Connected) {
            Write-WatcherLog 'liveness ok (tcp connect)'
            $client.Close()
        }
        else {
            Write-WatcherLog 'liveness FAIL (tcp connect timeout 2s)'
        }
    }
    catch {
        Write-WatcherLog "liveness exception: $($_.Exception.Message)"
    }

    # Freeze-Flag: sofortigen Extra-Dump erzwingen, einmalig pro Flag
    if ((Test-Path $freezeFlag) -and (-not $lastFreezeDumped)) {
        $tsX = (Get-Date).ToString('yyyyMMdd-HHmmss-fff')
        $extra = Join-Path $OutputDir "threaddump-FREEZE-$tsX.txt"
        & $jcmd $ofpid Thread.print > $extra 2>&1
        Write-WatcherLog "FREEZE-Flag gesehen — Extra-Dump -> $extra"
        $lastFreezeDumped = $true
    }

    Start-Sleep -Seconds $Interval
}

Write-WatcherLog 'Watcher Ende.'
