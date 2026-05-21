<#
.SYNOPSIS
  Watcher fuer den Windows-Freeze-Repro.
  Pollt den laufenden soffice.bin-Prozess, zieht alle 5 s einen
  Java-Thread-Dump und macht parallel eine simple Liveness-Probe.
  Endet, wenn `stop.flag` im OutputDir auftaucht oder soffice.bin weg ist.

.PARAMETER OutputDir
  Verzeichnis, in das Thread-Dumps und Watcher-Log geschrieben werden.

.PARAMETER Interval
  Polling-Intervall in Sekunden (Default 5).

.PARAMETER LivenessPort
  Port, auf dem soffice.bin URP-Listener lauscht (fuer Liveness-Probe via
  TCP-Connect; eine echte UNO-Probe waere teurer und liefe Gefahr,
  selber zu haengen).
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

# jcmd / jstack lokalisieren (kompatibel zu PS5.1 -- kein ?.-Operator)
$jcmdCmd = Get-Command jcmd -ErrorAction SilentlyContinue
if (-not $jcmdCmd) {
    Write-WatcherLog 'FEHLER: jcmd nicht im PATH gefunden.'
    exit 1
}
$jcmd = $jcmdCmd.Source
$jstackCmd = Get-Command jstack -ErrorAction SilentlyContinue
$jstack = if ($jstackCmd) { $jstackCmd.Source } else { $null }
Write-WatcherLog "jcmd:   $jcmd"
Write-WatcherLog "jstack: $jstack"

# Thread-Dump mit Timeout. Wenn jcmd haengt (JVM wedged), nicht warten -- killen
# und mit 'jstack -F' (Force-Attach) probieren, der umgeht die Attach-API.
function Invoke-ThreadDump([int] $targetPid, [string] $outFile, [int] $timeoutSec = 10) {
    $info = New-Object System.Diagnostics.ProcessStartInfo
    $info.FileName = $jcmd
    $info.Arguments = "$targetPid Thread.print"
    $info.RedirectStandardOutput = $true
    $info.RedirectStandardError = $true
    $info.UseShellExecute = $false
    $info.CreateNoWindow = $true
    $proc = [System.Diagnostics.Process]::Start($info)
    if (-not $proc.WaitForExit($timeoutSec * 1000)) {
        try { $proc.Kill() } catch { }
        Write-WatcherLog "jcmd TIMEOUT nach ${timeoutSec}s (JVM moeglicherweise wedged) -- versuche jstack -F"
        if ($jstack) {
            $info2 = New-Object System.Diagnostics.ProcessStartInfo
            $info2.FileName = $jstack
            $info2.Arguments = "-F $targetPid"
            $info2.RedirectStandardOutput = $true
            $info2.RedirectStandardError = $true
            $info2.UseShellExecute = $false
            $info2.CreateNoWindow = $true
            $proc2 = [System.Diagnostics.Process]::Start($info2)
            if (-not $proc2.WaitForExit(30 * 1000)) {
                try { $proc2.Kill() } catch { }
                Write-WatcherLog 'jstack -F ebenfalls TIMEOUT (30s)'
                return @{ rc = -2; lines = 0 }
            }
            $out = $proc2.StandardOutput.ReadToEnd() + $proc2.StandardError.ReadToEnd()
            [System.IO.File]::WriteAllText($outFile, $out)
            return @{ rc = $proc2.ExitCode; lines = ($out -split "`n").Count }
        }
        return @{ rc = -1; lines = 0 }
    }
    $out = $proc.StandardOutput.ReadToEnd() + $proc.StandardError.ReadToEnd()
    [System.IO.File]::WriteAllText($outFile, $out)
    return @{ rc = $proc.ExitCode; lines = ($out -split "`n").Count }
}

# Auf soffice.bin warten (max 60 s). NICHT '$ofpid' verwenden -- Automatic-Variable.
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
        Write-WatcherLog 'stop.flag gesehen -- beende.'
        break
    }
    if (-not (Get-Process -Id $ofpid -ErrorAction SilentlyContinue)) {
        Write-WatcherLog "PID $ofpid weg -- soffice.bin beendet."
        break
    }

    $ts = (Get-Date).ToString('yyyyMMdd-HHmmss-fff')
    $dumpFile = Join-Path $OutputDir "threaddump-$ts.txt"

    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    $r = Invoke-ThreadDump -targetPid $ofpid -outFile $dumpFile -timeoutSec 10
    $sw.Stop()
    Write-WatcherLog "thread-dump  rc=$($r.rc)  ms=$($sw.ElapsedMilliseconds)  lines=$($r.lines)  -> $dumpFile"

    # Liveness-Probe: kann sich der TCP-Listener oeffnen lassen?
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
        $rF = Invoke-ThreadDump -targetPid $ofpid -outFile $extra -timeoutSec 20
        Write-WatcherLog "FREEZE-Flag gesehen -- Extra-Dump rc=$($rF.rc) lines=$($rF.lines) -> $extra"
        $lastFreezeDumped = $true
    }

    Start-Sleep -Seconds $Interval
}

Write-WatcherLog 'Watcher Ende.'
