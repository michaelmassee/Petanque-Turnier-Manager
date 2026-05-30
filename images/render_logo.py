#!/usr/bin/env python3
"""Rendert das Sidebar-Deck-Icon passgenau aus dem Logo-Master.

Das Sidebar-Deck (`PetanqueTurnierManagerDeck` in Sidebar.xcu) referenziert genau eine
`IconURL`. LibreOffice/GTK zeigt diese PNG in der Sidebar-TabBar. Die Design-Groesse der
TabBar ist 24x24px (vgl. LOs eigene `sfx2/res/symphony/sidebar-*-large.png`). Damit LO das
Icon NICHT zur Laufzeit skalieren muss, wird hier eine passgenaue 24x24-PNG erzeugt
(seitenverhaeltnistreu eingepasst, auf transparentem Canvas zentriert).

Quelle ist der hochaufloesende PNG-Master (eine SVG-Quelle des Logos existiert nicht).

Abhaengigkeit: Pillow. Aufruf:
    python3 images/render_logo.py
"""
from pathlib import Path

from PIL import Image

HIER = Path(__file__).resolve().parent
MASTER = HIER / "petanqueturniermanager-logo.png"
ZIEL = HIER / "petanqueturniermanager-logo-sidebar-24px.png"
KANTE = 24


def rendere_sidebar_icon() -> None:
    master = Image.open(MASTER).convert("RGBA")

    # Seitenverhaeltnistreu in KANTE x KANTE einpassen (LANCZOS = hochwertiges Resampling).
    skaliert = master.copy()
    skaliert.thumbnail((KANTE, KANTE), Image.Resampling.LANCZOS)

    # Auf transparentem, quadratischem Canvas zentrieren -> keine Verzerrung, quadratisches Icon.
    canvas = Image.new("RGBA", (KANTE, KANTE), (0, 0, 0, 0))
    versatz = ((KANTE - skaliert.width) // 2, (KANTE - skaliert.height) // 2)
    canvas.paste(skaliert, versatz, skaliert)

    canvas.save(ZIEL, "PNG")
    print(f"Geschrieben: {ZIEL.name} ({canvas.width}x{canvas.height}, {canvas.mode})")


if __name__ == "__main__":
    rendere_sidebar_icon()
