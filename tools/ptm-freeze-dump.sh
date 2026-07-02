#!/usr/bin/env bash
# Thread-Dumps eines laufenden soffice.bin für die Freeze-Diagnose (z.B. langsamer Tabwechsel).
#
# Zwei Modi:
#   (default) jcmd     – nur JVM-Threads der Extension. Zeigt, OB Plugin-Java-Code läuft.
#   --native  gdb      – C++-Backtraces ALLER nativen Threads. Zeigt, WAS LO nativ tut
#                        (Repaint/Recalc/Layout), wenn kein Plugin-Code läuft.
#
# Verwendung (am besten per `! ...` direkt in der Claude-Session):
#   ./tools/ptm-freeze-dump.sh              # jcmd, 3 Dumps / 3s -> ~/ptm-freeze-<ts>.txt
#   ./tools/ptm-freeze-dump.sh 5 2          # jcmd, 5 Dumps / 2s
#   ./tools/ptm-freeze-dump.sh 0 3          # jcmd, DAUER-Modus (alle 3s bis Ctrl+C/Ende)
#   ./tools/ptm-freeze-dump.sh --native 4 2 # gdb,  4 C++-Dumps / 2s  (im Freeze auslösen)
#   ./tools/ptm-freeze-dump.sh --native 0 4 # gdb,  DAUER-Modus (pausiert LO alle 4s!)
#
# DAUER-Modus (ANZAHL=0): vorher starten und laufen lassen. Jeder Dump bekommt einen
# Zeitstempel; beim Freeze nur die Uhrzeit merken und die Datei schicken.
#
# Die soffice.bin-PID wird automatisch ermittelt (echter Prozessname). Bei mehreren
# LO-Instanzen die richtige per PTM_SOFFICE_PID=<pid> erzwingen.
#
# --native braucht ptrace-Rechte auf einen Fremd-Prozess. Standardmäßig elevatet das
# Skript NUR gdb via sudo (ptrace_scope bleibt unangetastet, Ausgabedatei bleibt in
# deinem Home). Daher --native interaktiv per `! ...` starten, damit sudo nach dem
# Passwort fragen kann (im Hintergrund-Task gibt es keine Passwort-Abfrage).
# gdb muss installiert sein. Bei ptrace_scope=0 oder als root wird kein sudo genutzt.
set -u

# ── Argumente parsen ──────────────────────────────────────────────────────────
MODE="jcmd"
if [[ "${1:-}" == "--native" ]]; then
    MODE="native"
    shift
fi
ANZAHL="${1:-3}"     # 0 = endlos (Dauer-Modus)
INTERVAL="${2:-3}"

# ── Dump-Werkzeug je Modus vorbereiten ────────────────────────────────────────
if [[ "${MODE}" == "native" ]]; then
    GDB="$(command -v gdb || true)"
    if [[ -z "${GDB}" ]]; then
        echo "FEHLER: gdb nicht gefunden (sudo apt install gdb)." >&2
        exit 1
    fi
    # gdb braucht ptrace-Rechte: bei scope!=0 und nicht-root -> gdb via sudo elevaten.
    SCOPE="$(cat /proc/sys/kernel/yama/ptrace_scope 2>/dev/null || echo '?')"
    GDB_CMD=("${GDB}")
    if [[ "${SCOPE}" != "0" && "${EUID}" -ne 0 ]]; then
        GDB_CMD=(sudo -- "${GDB}")
        echo "Hinweis: gdb wird via sudo gestartet (ptrace_scope=${SCOPE}). Passwort kann abgefragt werden."
    fi
else
    JCMD="$(command -v jcmd || true)"
    if [[ -z "${JCMD}" && -n "${JAVA_HOME:-}" ]]; then
        JCMD="${JAVA_HOME}/bin/jcmd"
    fi
    if [[ -z "${JCMD}" || ! -x "${JCMD}" ]]; then
        echo "FEHLER: jcmd nicht gefunden. JDK aktiv? ('sdk use java 25-...' oder JAVA_HOME)" >&2
        exit 1
    fi
fi

# ── soffice.bin finden (Dauer-Modus wartet auf Start) ─────────────────────────
# Robust: primär der echte Prozessname (comm == soffice.bin). Ein reines
# `pgrep -f 'soffice.bin'` würde auch Fremdprozesse treffen, die den String nur in
# ihrer Kommandozeile führen (Editor, grep, dieses Skript, eine Shell-Zeile) – und
# so die falsche PID nehmen. Fallback: Pfad-Match auf die Binary ohne die eigene PID.
# Manuelle Vorgabe via Umgebungsvariable PTM_SOFFICE_PID=<pid> möglich.
sofficePids() {
    if [[ -n "${PTM_SOFFICE_PID:-}" ]]; then
        echo "${PTM_SOFFICE_PID}"
        return
    fi
    local pids
    pids="$(pgrep -x 'soffice.bin' 2>/dev/null || true)"
    if [[ -z "${pids}" ]]; then
        # Pfad-Match (führender Slash) statt beliebigem Substring; eigene PID ausschließen.
        pids="$(pgrep -f '[/]soffice\.bin' 2>/dev/null | grep -vx "$$" || true)"
    fi
    echo "${pids}"
}

waehlePid() {
    local pids anzahl
    pids="$(sofficePids)"
    anzahl="$(printf '%s\n' ${pids} | grep -c . || true)"
    if [[ "${anzahl}" -gt 1 ]]; then
        echo "Hinweis: mehrere soffice.bin-Prozesse ($(printf '%s ' ${pids}))– nehme den ersten. Override: PTM_SOFFICE_PID=<pid>" >&2
    fi
    printf '%s\n' ${pids} | head -n1
}

PID="$(waehlePid)"
if [[ -z "${PID}" ]]; then
    if [[ "${ANZAHL}" -eq 0 ]]; then
        echo "Warte auf soffice.bin … (Ctrl+C zum Abbrechen)"
        while [[ -z "${PID}" ]]; do
            sleep 1
            PID="$(waehlePid)"
        done
    else
        echo "FEHLER: kein laufender soffice.bin-Prozess gefunden." >&2
        exit 1
    fi
fi

OUT=~/"ptm-freeze-${MODE}-$(date +%Y%m%d-%H%M%S).txt"

einDump() {
    local nr="$1"
    {
        echo "===== DUMP ${nr}  $(date +%T)  pid=${PID}  mode=${MODE} ====="
        if [[ "${MODE}" == "native" ]]; then
            "${GDB_CMD[@]}" -p "${PID}" -batch \
                -ex "set pagination off" \
                -ex "thread apply all bt"
        else
            "${JCMD}" "${PID}" Thread.print
        fi
        echo
    } >> "${OUT}" 2>&1
    echo "  Dump ${nr} geschrieben ($(date +%T))"
}

# ── Treffer-Muster für den Schnellblick ───────────────────────────────────────
if [[ "${MODE}" == "native" ]]; then
    HIGHLIGHT='ScGridWindow|ScTabView|Paint|ScDocument|ScColumn|ScTable|ScInterpreter|Interpret|Recalc|Broadcast|SfxBroadcaster|JNI'
else
    HIGHLIGHT='"main"|SolarMutex|ScInterpreter|ScDocument|Recalc|de\.petanqueturniermanager'
fi

# ── Dauer-Modus ───────────────────────────────────────────────────────────────
if [[ "${ANZAHL}" -eq 0 ]]; then
    echo "soffice.bin PID=${PID} – ${MODE}-DAUER-Modus alle ${INTERVAL}s -> ${OUT}  (Ctrl+C zum Stop)"
    trap 'echo; echo "Beendet: ${OUT}"; exit 0' INT TERM
    i=0
    while kill -0 "${PID}" 2>/dev/null; do
        i=$((i + 1))
        einDump "${i}"
        sleep "${INTERVAL}"
    done
    echo "Prozess ${PID} beendet. Datei: ${OUT}"
    exit 0
fi

# ── Burst-Modus ───────────────────────────────────────────────────────────────
echo "soffice.bin PID=${PID} – ${MODE}: ${ANZAHL} Dumps im ${INTERVAL}s-Abstand -> ${OUT}"
for ((i = 1; i <= ANZAHL; i++)); do
    einDump "${i}"
    [[ "${i}" -lt "${ANZAHL}" ]] && sleep "${INTERVAL}"
done

echo
echo "Fertig: ${OUT}"
echo "Schnellblick auf die interessanten Frames:"
grep -nE "${HIGHLIGHT}" "${OUT}" | head -25 || true
