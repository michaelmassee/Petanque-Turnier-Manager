#!/usr/bin/env bash
# Synct den Projektordner einseitig nach pCloud (lokal → pCloud).
# Ausgeschlossen werden Build-Artefakte, IDE-Dateien und temporäre Dateien
# (analog .gitignore).

set -euo pipefail

SRC="/home/massee/devel/projekts/privat/Petanque-Turnier-Manager/"
DST="/home/massee/pCloudDrive/firma/Petanque-Turnier-Manager/"

# Dry-run per Flag -n / --dry-run möglich
DRY_RUN=""
if [[ "${1:-}" == "-n" || "${1:-}" == "--dry-run" ]]; then
    DRY_RUN="--dry-run"
    echo "==> DRY-RUN – keine Dateien werden tatsächlich kopiert"
fi

rsync -av --delete \
    $DRY_RUN \
    --exclude='.git/' \
    --exclude='.claude/' \
    --exclude='.gradle/' \
    --exclude='build/' \
    --exclude='libs/' \
    --exclude='libs-test/' \
    --exclude='.idea/' \
    --exclude='*.iml' \
    --exclude='*.iws' \
    --exclude='.vscode/' \
    --exclude='.project' \
    --exclude='.classpath' \
    --exclude='.settings/' \
    --exclude='*.launch' \
    --exclude='.externalToolBuilders/' \
    --exclude='bin/' \
    --exclude='.checkstyle' \
    --exclude='.unoproject' \
    --exclude='types.rdb' \
    --exclude='package.properties' \
    --exclude='xcu-backup/' \
    --exclude='*.class' \
    --exclude='*.log' \
    "$SRC" "$DST"

echo ""
echo "==> Sync abgeschlossen: $SRC → $DST"
