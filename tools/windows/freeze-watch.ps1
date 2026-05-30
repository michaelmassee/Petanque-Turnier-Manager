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

# jcmd / jhsdb / procdump lokalisieren. 'jstack -F' ist in modernen JDKs
# entfernt; Ersatz ist 'jhsdb jstack --pid'. jhsdb braucht u.U. Admin und
# scheitert sonst mit 'WaitForEvent failed (0x80070057)'. Als letzten Anker
# nutzen wir Sysinternals procdump (Minidump, kein JVM-Mitwirken noetig).
$jcmdCmd = Get-Command jcmd -ErrorAction SilentlyContinue
if (-not $jcmdCmd) {
    Write-WatcherLog 'FEHLER: jcmd nicht im PATH gefunden.'
    exit 1
}
$jcmd = $jcmdCmd.Source
$jhsdbCmd = Get-Command jhsdb -ErrorAction SilentlyContinue
$jhsdb = if ($jhsdbCmd) { $jhsdbCmd.Source } else { $null }
$procdumpCmd = Get-Command procdump.exe -ErrorAction SilentlyContinue
$procdump = if ($procdumpCmd) { $procdumpCmd.Source } else { $null }
Write-WatcherLog "jcmd:     $jcmd"
Write-WatcherLog "jhsdb:    $jhsdb"
Write-WatcherLog "procdump: $procdump"

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
                Write-WatcherLog 'jhsdb jstack TIMEOUT (30s) -- versuche procdump'
            }
            else {
                $out = $proc2.StandardOutput.ReadToEnd() + $proc2.StandardError.ReadToEnd()
                if ($proc2.ExitCode -eq 0 -and ($out -notmatch 'WaitForEvent failed')) {
                    [System.IO.File]::WriteAllText($outFile, $out)
                    return @{ rc = $proc2.ExitCode; lines = ($out -split "`n").Count }
                }
                Write-WatcherLog "jhsdb jstack rc=$($proc2.ExitCode), output suggests failure -- versuche procdump"
            }
        }
        # procdump-Fallback: Minidump, das man offline mit WinDbg / Analyser
        # untersuchen kann. Braucht kein Mitwirken der JVM.
        if ($procdump) {
            $dumpDmp = $outFile -replace '\.txt$', '.dmp'
            $info3 = New-Object System.Diagnostics.ProcessStartInfo
            $info3.FileName = $procdump
            $info3.Arguments = "-accepteula -ma $targetPid `"$dumpDmp`""
            $info3.RedirectStandardOutput = $true
            $info3.RedirectStandardError = $true
            $info3.UseShellExecute = $false
            $info3.CreateNoWindow = $true
            $proc3 = [System.Diagnostics.Process]::Start($info3)
            if ($proc3.WaitForExit(60 * 1000)) {
                $pdOut = $proc3.StandardOutput.ReadToEnd() + $proc3.StandardError.ReadToEnd()
                [System.IO.File]::WriteAllText($outFile, "procdump rc=$($proc3.ExitCode)`n$pdOut")
                return @{ rc = $proc3.ExitCode; lines = ($pdOut -split "`n").Count }
            }
            try { $proc3.Kill() } catch { }
            Write-WatcherLog 'procdump TIMEOUT (60s)'
        }
        return @{ rc = -1; lines = 0 }
    }
    $out = $proc.StandardOutput.ReadToEnd() + $proc.StandardError.ReadToEnd()
    [System.IO.File]::WriteAllText($outFile, $out)
    return @{ rc = $proc.ExitCode; lines = ($out -split "`n").Count }
}

# Dumpt ALLE aktuell laufenden soffice.bin-Prozesse. Frueher haben wir versucht
# den "Java-hostenden" Prozess via jcmd VM.version zu identifizieren -- das
# scheitert aber genau dann, wenn die JVM wedged ist (die Diagnose-Situation,
# die uns interessiert). Stattdessen dumpen wir ALLE, akzeptieren ein paar
# leere/Fehler-Dumps fuer Nicht-JVM-Prozesse, und kriegen verlaesslich den
# Dump des wedged Prozesses sobald jhsdb-Fallback greift.
function Dump-AllSoffice([string] $tag = '') {
    $cands = Get-Process -Name soffice.bin -ErrorAction SilentlyContinue
    if (-not $cands) {
        Write-WatcherLog "dump-all: keine soffice.bin gefunden"
        return
    }
    $tsBase = (Get-Date).ToString('yyyyMMdd-HHmmss-fff')
    foreach ($p in $cands) {
        $suffix = if ($tag) { "$tag-pid$($p.Id)" } else { "pid$($p.Id)" }
        $file = Join-Path $OutputDir "threaddump-$tsBase-$suffix.txt"
        $sw = [System.Diagnostics.Stopwatch]::StartNew()
        $r = Invoke-ThreadDump -targetPid $p.Id -outFile $file -timeoutSec 10
        $sw.Stop()
        Write-WatcherLog "dump pid=$($p.Id) rc=$($r.rc) ms=$($sw.ElapsedMilliseconds) lines=$($r.lines) -> $file"
    }
}

# Warte bis mindestens eine soffice.bin auftaucht (max 60s).
$haveSoffice = $false
for ($i = 0; $i -lt 60; $i++) {
    if (Test-Path $stopFlag) {
        Write-WatcherLog 'stop.flag gesehen vor Hauptschleife -- ende.'
        exit 0
    }
    if (Get-Process -Name soffice.bin -ErrorAction SilentlyContinue) {
        $haveSoffice = $true
        break
    }
    Start-Sleep -Seconds 1
}
if (-not $haveSoffice) {
    Write-WatcherLog 'FEHLER: keine soffice.bin nach 60s.'
    exit 1
}
Write-WatcherLog 'soffice.bin gesehen -- starte Polling-Schleife.'

$lastFreezeDumped = $false

while ($true) {
    if (Test-Path $stopFlag) {
        Write-WatcherLog 'stop.flag gesehen -- beende.'
        break
    }
    if (-not (Get-Process -Name soffice.bin -ErrorAction SilentlyContinue)) {
        Write-WatcherLog 'keine soffice.bin mehr da -- beende.'
        break
    }

    Dump-AllSoffice

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

    # Freeze-Flag: sofortigen Extra-Dump-Schub aller soffice.bin
    if ((Test-Path $freezeFlag) -and (-not $lastFreezeDumped)) {
        Write-WatcherLog 'FREEZE-Flag gesehen -- Extra-Dump-Schub:'
        Dump-AllSoffice -tag 'FREEZE'
        $lastFreezeDumped = $true
    }

    Start-Sleep -Seconds $Interval
}

Write-WatcherLog 'Watcher Ende.'
