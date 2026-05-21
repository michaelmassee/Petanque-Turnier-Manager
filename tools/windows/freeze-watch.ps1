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

# jcmd / jhsdb lokalisieren (kompatibel zu PS5.1 -- kein ?.-Operator).
# 'jstack -F' ist in modernen JDKs entfernt; Ersatz ist 'jhsdb jstack --pid'.
$jcmdCmd = Get-Command jcmd -ErrorAction SilentlyContinue
if (-not $jcmdCmd) {
    Write-WatcherLog 'FEHLER: jcmd nicht im PATH gefunden.'
    exit 1
}
$jcmd = $jcmdCmd.Source
$jhsdbCmd = Get-Command jhsdb -ErrorAction SilentlyContinue
$jhsdb = if ($jhsdbCmd) { $jhsdbCmd.Source } else { $null }
Write-WatcherLog "jcmd:  $jcmd"
Write-WatcherLog "jhsdb: $jhsdb"

# Thread-Dump mit Timeout. Wenn jcmd haengt (JVM wedged), nicht warten -- killen
# und mit 'jhsdb jstack --pid' (HotSpot-Serviceability-Agent) probieren.
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
        Write-WatcherLog "jcmd TIMEOUT nach ${timeoutSec}s (JVM moeglicherweise wedged) -- versuche jhsdb jstack"
        if ($jhsdb) {
            $info2 = New-Object System.Diagnostics.ProcessStartInfo
            $info2.FileName = $jhsdb
            $info2.Arguments = "jstack --pid $targetPid"
            $info2.RedirectStandardOutput = $true
            $info2.RedirectStandardError = $true
            $info2.UseShellExecute = $false
            $info2.CreateNoWindow = $true
            $proc2 = [System.Diagnostics.Process]::Start($info2)
            if (-not $proc2.WaitForExit(30 * 1000)) {
                try { $proc2.Kill() } catch { }
                Write-WatcherLog 'jhsdb jstack ebenfalls TIMEOUT (30s)'
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

# Findet unter mehreren soffice.bin-Prozessen den, dessen JVM jcmd anspricht
# (=jvm.dll ist in dem Prozess geladen). LO startet ggf. mehrere soffice.bin;
# nur der mit dem Java-UNO-Component-Loader hat die JVM. Probiert
# jcmd <pid> VM.version mit kurzem Timeout pro Kandidat.
function Find-JavaSoffice([int] $maxWaitSec = 60) {
    $deadline = (Get-Date).AddSeconds($maxWaitSec)
    while ((Get-Date) -lt $deadline) {
        $cands = Get-Process -Name soffice.bin -ErrorAction SilentlyContinue
        foreach ($p in $cands) {
            $info = New-Object System.Diagnostics.ProcessStartInfo
            $info.FileName = $jcmd
            $info.Arguments = "$($p.Id) VM.version"
            $info.RedirectStandardOutput = $true
            $info.RedirectStandardError = $true
            $info.UseShellExecute = $false
            $info.CreateNoWindow = $true
            $j = [System.Diagnostics.Process]::Start($info)
            if ($j.WaitForExit(3000)) {
                $out = $j.StandardOutput.ReadToEnd() + $j.StandardError.ReadToEnd()
                if ($j.ExitCode -eq 0 -and $out -match 'JVM') {
                    Write-WatcherLog "Java-soffice.bin gefunden: PID=$($p.Id)"
                    return [int] $p.Id
                }
            }
            else {
                try { $j.Kill() } catch { }
            }
        }
        Start-Sleep -Seconds 1
    }
    return 0
}

# Auf den Java-hostenden soffice.bin warten (LO startet u.U. mehrere; nur der
# mit geladenem jvm.dll laesst sich mit jcmd ansprechen). Variable nicht $pid
# nennen -- ist Automatic-Variable.
$ofpid = Find-JavaSoffice -maxWaitSec 90
if (-not $ofpid) {
    Write-WatcherLog 'FEHLER: kein Java-soffice.bin innerhalb 90s gefunden.'
    exit 1
}

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
