"""Rendert v2-Icons in alle Größen, baut Übersicht & Vergleich Alt/Neu."""
import os
import cairosvg
from PIL import Image, ImageDraw, ImageFont

SVG_DIR = "/home/claude/icons_v2/svg"
PNG_DIR = "/home/claude/icons_v2/png"
SIZES = [16, 24, 32, 48, 64, 128]

# Reihenfolge: Haupt-Toolbar, Spieltag-Toolbar, Timer-Toolbar
HAUPT = [
    "toolbar-start", "toolbar-neu-in-neuer-datei", "toolbar-oeffnen",
    "toolbar-konfiguration", "toolbar-teilnehmer", "toolbar-turnier-modus",
    "toolbar-spielerdb-meldungen", "toolbar-neu-auslosen", "toolbar-weiter",
    "toolbar-vorrunden-rangliste", "toolbar-abschluss", "toolbar-abbruch",
    "toolbar-drucken", "toolbar-druckvorschau",
    "toolbar-webserver-starten", "toolbar-webserver-stoppen",
]
SPIELTAG = ["toolbar-naechster-spieltag", "toolbar-gesamtrangliste"]
TIMER = [
    "toolbar-timer-start", "toolbar-timer-pause", "toolbar-timer-stop",
    "toolbar-timer-plus1", "toolbar-timer-minus1", "toolbar-timer-snooze",
]

ALL = HAUPT + SPIELTAG + TIMER

LABELS = {
    "toolbar-start":              ("Turnier starten",     "ptm:toolbar_start"),
    "toolbar-neu-in-neuer-datei": ("Neues Turnier",       "ptm:toolbar_neu_in_neuer_datei"),
    "toolbar-oeffnen":            ("Öffnen",              "ptm:toolbar_oeffnen"),
    "toolbar-konfiguration":      ("Konfiguration",       "ptm:konfiguration_turnier"),
    "toolbar-teilnehmer":         ("Teilnehmer",          "ptm:toolbar_teilnehmer"),
    "toolbar-turnier-modus":      ("Turnieransicht",      "ptm:turnier_modus"),
    "toolbar-spielerdb-meldungen": ("Spieler-DB Übernahme", "ptm:spielerdb_in_meldeliste"),
    "toolbar-neu-auslosen":       ("Neu auslosen",        "ptm:toolbar_neu_auslosen"),
    "toolbar-weiter":             ("Nächste Runde",       "ptm:toolbar_weiter"),
    "toolbar-naechster-spieltag": ("Nächster Spieltag",   "ptm:toolbar_naechster_spieltag"),
    "toolbar-vorrunden-rangliste": ("Vorrunden-Rangliste", "ptm:toolbar_vorrunden_rangliste"),
    "toolbar-gesamtrangliste":    ("Gesamtrangliste",     "ptm:toolbar_gesamtrangliste"),
    "toolbar-abschluss":          ("Abschlussphase",      "ptm:toolbar_abschluss"),
    "toolbar-abbruch":            ("Abbruch",             "ptm:abbruch"),
    "toolbar-drucken":            ("Drucken",             "ptm:toolbar_drucken"),
    "toolbar-druckvorschau":      ("Druckvorschau",       "ptm:toolbar_druckvorschau"),
    "toolbar-webserver-starten":  ("Server starten",      "ptm:webserver_starten"),
    "toolbar-webserver-stoppen":  ("Server stoppen",      "ptm:webserver_stoppen"),
    "toolbar-timer-start":        ("Timer starten",       "ptm:timer_starten_dialog"),
    "toolbar-timer-pause":        ("Timer Pause",         "ptm:timer_pause_fortsetzen"),
    "toolbar-timer-stop":         ("Timer stoppen",       "ptm:timer_stoppen"),
    "toolbar-timer-plus1":        ("+1 Minute",           "ptm:timer_plus_minute"),
    "toolbar-timer-minus1":       ("-1 Minute",           "ptm:timer_minus_minute"),
    "toolbar-timer-snooze":       ("Timer Snooze",        "ptm:timer_snooze"),
}

# 1. PNGs in allen Größen rendern
for size in SIZES:
    out = f"{PNG_DIR}/{size}"
    os.makedirs(out, exist_ok=True)
    for name in ALL:
        cairosvg.svg2png(
            url=f"{SVG_DIR}/{name}.svg",
            write_to=f"{out}/{name}.png",
            output_width=size,
            output_height=size,
        )
print(f"PNGs in {len(SIZES)} Größen × {len(ALL)} Icons = {len(SIZES)*len(ALL)} Dateien.")


def load_font(size, bold=False):
    for c in [
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf" if bold else "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
        "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf" if bold else "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
    ]:
        if os.path.exists(c):
            return ImageFont.truetype(c, size)
    return ImageFont.load_default()

font_title = load_font(22, bold=True)
font_subtitle = load_font(13)
font_section = load_font(14, bold=True)
font_label = load_font(11)
font_url = load_font(9)


# 2. Übersicht – nach Toolbar-Gruppe gegliedert
CELL_W, CELL_H = 175, 115
PAD = 12
COLS = 4

def build_section(names, title, x0, y0):
    rows = (len(names) + COLS - 1) // COLS
    draw.text((x0, y0), title, fill=(31, 41, 55), font=font_section)
    y = y0 + 24
    for i, name in enumerate(names):
        col = i % COLS
        row = i // COLS
        x = x0 + col * (CELL_W + PAD)
        yc = y + row * (CELL_H + PAD)
        draw.rounded_rectangle([x, yc, x + CELL_W, yc + CELL_H], radius=8,
                                fill=(255, 255, 255), outline=(229, 231, 235), width=1)
        # Icons 24, 32, 48
        sizes = [24, 32, 48]
        total_w = sum(sizes) + 12 * 2
        sx = x + (CELL_W - total_w) // 2
        baseline = yc + 14 + max(sizes) // 2
        for s in sizes:
            ico = Image.open(f"{PNG_DIR}/{s}/{name}.png").convert("RGBA")
            img.paste(ico, (sx, baseline - s // 2), ico)
            sx += s + 12
        # Label
        label, url = LABELS[name]
        bbox = draw.textbbox((0, 0), label, font=font_label)
        lw = bbox[2] - bbox[0]
        draw.text((x + (CELL_W - lw) // 2, yc + CELL_H - 30),
                   label, fill=(55, 65, 81), font=font_label)
        # URL klein darunter
        bbox = draw.textbbox((0, 0), url, font=font_url)
        uw = bbox[2] - bbox[0]
        draw.text((x + (CELL_W - uw) // 2, yc + CELL_H - 15),
                   url, fill=(156, 163, 175), font=font_url)
    return y + rows * (CELL_H + PAD)


# Größe berechnen
def section_h(n):
    return 24 + ((n + COLS - 1) // COLS) * (CELL_H + PAD)

TITLE_H = 60
W = COLS * CELL_W + (COLS + 1) * PAD
H = TITLE_H + section_h(len(HAUPT)) + section_h(len(SPIELTAG)) + section_h(len(TIMER)) + 30

img = Image.new("RGB", (W, H), (250, 250, 252))
draw = ImageDraw.Draw(img)

draw.text((PAD, 14), "Petanque-Turnier-Manager — Toolbar Icons v2",
           fill=(31, 41, 55), font=font_title)
draw.text((PAD, 40), "Thematisch an Sourcecode angepasst  ·  23 Icons in 3 Toolbar-Gruppen",
           fill=(107, 114, 128), font=font_subtitle)

y = TITLE_H
y = build_section(HAUPT, "Haupt-Toolbar  (Addons_Z2_Toolbar.xcu)", PAD, y) + 6
y = build_section(SPIELTAG, "Spieltag-Toolbar  (Addons_Z3_SpieltagToolbar.xcu)", PAD, y) + 6
y = build_section(TIMER, "Timer-Toolbar  (Addons_Z4_TimerToolbar.xcu)", PAD, y) + 6

img.save("/home/claude/icons_v2/preview-overview-v2.png", optimize=True)
print(f"Übersicht: preview-overview-v2.png ({W}x{H})")


# 3. Vorher/Nachher-Vergleich für die 5 thematisch geänderten Icons
CHANGED = [
    ("toolbar-start",              "Start: + Boule-Hint statt nur Play"),
    ("toolbar-turnier-modus",      "Kiosk-Modus (Monitor + Vollbild-Pfeile) statt Bracket"),
    ("toolbar-spielerdb-meldungen", "DB→Liste-Übernahme statt nur DB-Start"),
    ("toolbar-weiter",             "Liste + Pfeil (Runde) statt Doppelpfeil (Fast-Forward)"),
    ("toolbar-abschluss",          "Zielflagge (Final Phase) statt Häkchen"),
]

ROW_H = 110
CV_W = 900
CV_H = 70 + len(CHANGED) * ROW_H + 30

cmp = Image.new("RGB", (CV_W, CV_H), (250, 250, 252))
cd = ImageDraw.Draw(cmp)
cd.text((PAD, 14), "Thematische Korrekturen — Vorher / Nachher",
         fill=(31, 41, 55), font=font_title)
cd.text((PAD, 42), "Was sich gegenüber dem ersten Set geändert hat, basierend auf dem Code im GitHub-Repo.",
         fill=(107, 114, 128), font=font_subtitle)

import shutil
# Wir haben die alten Icons aus dem ersten Repo-Klon!
OLD_REPO_IMAGES = "/tmp/ptm-repo/images"

y = 75
for name, desc in CHANGED:
    # Rahmen
    cd.rounded_rectangle([PAD, y, CV_W - PAD, y + ROW_H - 8], radius=8,
                          fill=(255, 255, 255), outline=(229, 231, 235), width=1)
    # Alt (aus Repo)
    old_path = f"{OLD_REPO_IMAGES}/{name}.png"
    if os.path.exists(old_path):
        old = Image.open(old_path).convert("RGBA")
        old.thumbnail((64, 64))
        cmp.paste(old, (PAD + 20, y + (ROW_H - 8 - old.height) // 2), old)
    cd.text((PAD + 100, y + 38), "Alt", fill=(156, 163, 175), font=font_url)

    # Pfeil
    cd.line([(PAD + 140, y + ROW_H // 2 - 4),
             (PAD + 175, y + ROW_H // 2 - 4)], fill=(107, 114, 128), width=2)
    cd.polygon([(PAD + 175, y + ROW_H // 2 - 10),
                (PAD + 185, y + ROW_H // 2 - 4),
                (PAD + 175, y + ROW_H // 2 + 2)], fill=(107, 114, 128))

    # Neu
    new = Image.open(f"{PNG_DIR}/64/{name}.png").convert("RGBA")
    cmp.paste(new, (PAD + 210, y + (ROW_H - 8 - 64) // 2), new)
    cd.text((PAD + 280, y + 38), "Neu", fill=(22, 163, 74), font=font_url)

    # Beschreibung
    cd.text((PAD + 350, y + 30), name + ".png",
             fill=(31, 41, 55), font=font_section)
    cd.text((PAD + 350, y + 55), desc, fill=(75, 85, 99), font=font_subtitle)

    y += ROW_H

cmp.save("/home/claude/icons_v2/preview-vorher-nachher.png", optimize=True)
print(f"Vergleich: preview-vorher-nachher.png ({CV_W}x{CV_H})")
