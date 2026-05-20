#!/usr/bin/env bash
# Pollt einen laufenden soffice.bin-Prozess und zieht alle 5s einen Java-Thread-Dump.
# Verwendung:
#   ./tools/lo-threaddump.sh               # Ausgaben in /tmp/triptete-hang-*.dump
#   ./tools/lo-threaddump.sh /tmp/myhang   # eigener Prefix
#
# Beenden mit Ctrl+C.
set -u

OUT_PREFIX="${1:-/tmp/triptete-hang}"
INTERVAL=5

# jcmd aus dem aktiven JDK (SDKMAN bevorzugt, sonst PATH)
JCMD="$(command -v jcmd || true)"
if [[ -z "${JCMD}" ]]; then
    echo "FEHLER: jcmd nicht im PATH. Java JDK installiert?" >&2
    exit 1
fi

echo "Warte auf soffice.bin …"
PID=""
while [[ -z "${PID}" ]]; do
    PID="$(pgrep -f 'soffice.bin' | head -n1 || true)"
    sleep 1
done
echo "soffice.bin PID=${PID}"
echo "Schreibe Thread-Dumps nach ${OUT_PREFIX}-<timestamp>.dump (alle ${INTERVAL}s, Ctrl+C zum Stop)."

trap 'echo; echo "Beendet."; exit 0' INT TERM

while kill -0 "${PID}" 2>/dev/null; do
    TS="$(date +%Y%m%d-%H%M%S)"
    OUT="${OUT_PREFIX}-${TS}.dump"
    if "${JCMD}" "${PID}" Thread.print > "${OUT}" 2>&1; then
        echo "${OUT}  ($(wc -l < "${OUT}") Zeilen)"
    else
        echo "${OUT}  -- jcmd-Fehler (siehe Datei)"
    fi
    sleep "${INTERVAL}"
done

echo "Prozess ${PID} beendet."
