#!/usr/bin/env python3
"""Rendert die Sidebar-Info-Panel-Icons passgenau auf ihre Anzeigegroesse.

Das Info-Panel (`InfoSidebarContent`) zeigt seine Status-Icons in 20x20px-Controls.
Bislang wurden dafuer die 128x128-Toolbar-Flat-PNGs bzw. das 648x730-Fortschritt-PNG
geladen, die LibreOffice zur Laufzeit auf 20x20 herunterskalieren musste. Hier werden
passgenaue 20x20-PNGs erzeugt (seitenverhaeltnistreu eingepasst, auf transparentem Canvas
zentriert), damit LO nicht zur Laufzeit skalieren muss.

Quellen sind die hochaufloesenden PNG-Master (eine SVG-Quelle ist fuer Fortschritt nicht
vorhanden; fuer Timer/Webserver wird der 128px-Flat-Master verwendet, der via render_v2.py
aus den SVGs entsteht).

Abhaengigkeit: Pillow. Aufruf:
    python3 images/render_sidebar_icons.py
"""
from pathlib import Path

from PIL import Image

HIER = Path(__file__).resolve().parent
ZIEL_DIR = HIER / "sidebar"
KANTE = 20

# Zielname -> Quelldatei (relativ zu images/)
ICONS = {
    "timer-start-20px.png": "toolbar-timer-start.png",
    "timer-pause-20px.png": "toolbar-timer-pause.png",
    "timer-stop-20px.png": "toolbar-timer-stop.png",
    "webserver-starten-20px.png": "toolbar-webserver-starten.png",
    "webserver-stoppen-20px.png": "toolbar-webserver-stoppen.png",
    "fortschritt-20px.png": "sidebar-fortschritt.png",
}


def rendere(zielname: str, quellname: str) -> None:
    quelle = Image.open(HIER / quellname).convert("RGBA")

    skaliert = quelle.copy()
    skaliert.thumbnail((KANTE, KANTE), Image.Resampling.LANCZOS)

    canvas = Image.new("RGBA", (KANTE, KANTE), (0, 0, 0, 0))
    versatz = ((KANTE - skaliert.width) // 2, (KANTE - skaliert.height) // 2)
    canvas.paste(skaliert, versatz, skaliert)

    ziel = ZIEL_DIR / zielname
    canvas.save(ziel, "PNG")
    print(f"Geschrieben: sidebar/{zielname} ({canvas.width}x{canvas.height}, {canvas.mode})")


if __name__ == "__main__":
    ZIEL_DIR.mkdir(exist_ok=True)
    for zielname, quellname in ICONS.items():
        rendere(zielname, quellname)
