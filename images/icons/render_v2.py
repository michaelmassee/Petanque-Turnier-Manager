"""Rendert die Toolbar-Icons aus svg/ in alle Größen, synchronisiert die flachen
images/toolbar-*.png (die der Gradle-Build einpackt) und baut eine Übersicht.

Pfade sind relativ zum Skript-Verzeichnis (<repo>/images/icons), damit das Skript
ortsunabhängig läuft. Fehlt für einen gelisteten Namen die SVG-Datei, wird er mit
Warnung übersprungen statt abzubrechen.

Aufruf:  python3 build_icons_v2.py && python3 render_v2.py
Abhängigkeiten: cairosvg, Pillow
"""
import os
import cairosvg
from PIL import Image, ImageDraw, ImageFont

# --- Pfade (skript-relativ) -------------------------------------------------
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
SVG_DIR = os.path.join(SCRIPT_DIR, "svg")
PNG_DIR = os.path.join(SCRIPT_DIR, "png")
# Flache Icons, die Addons_Z*_*.xcu via %origin%/images/toolbar-*.png referenzieren
# und die der Build nach registry/.../images/ kopiert. Liegen eine Ebene über icons/.
FLAT_DIR = os.path.dirname(SCRIPT_DIR)
FLAT_SIZE = 128  # Größe der flachen Repo-Icons (einheitlich mit Bestand)

SIZES = [16, 24, 32, 48, 64, 128]

# Reihenfolge: Haupt-Toolbar, Spieltag-Toolbar, Timer-Toolbar
HAUPT = [
    "toolbar-start", "toolbar-neu-in-neuer-datei", "toolbar-oeffnen",
    "toolbar-konfiguration", "toolbar-teilnehmer", "toolbar-checkin", "toolbar-turnier-modus",
    "toolbar-sidebar",
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
    "toolbar-checkin":            ("Checkin",             "ptm:toolbar_checkin"),
    "toolbar-turnier-modus":      ("Turnieransicht",      "ptm:turnier_modus"),
    "toolbar-sidebar":            ("Seitenleiste",        ".uno:SidebarDeck.PetanqueTurnierManagerDeck"),
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


def svg_pfad(name):
    return os.path.join(SVG_DIR, f"{name}.svg")


# Nur Icons mit vorhandener SVG-Quelle verarbeiten (z.B. fehlt toolbar-timer-snooze).
RENDERED = []
for name in ALL:
    if os.path.exists(svg_pfad(name)):
        RENDERED.append(name)
    else:
        print(f"  übersprungen (keine SVG): {name}")

# 1. PNGs in allen Größen rendern
for size in SIZES:
    out = os.path.join(PNG_DIR, str(size))
    os.makedirs(out, exist_ok=True)
    for name in RENDERED:
        cairosvg.svg2png(
            url=svg_pfad(name),
            write_to=os.path.join(out, f"{name}.png"),
            output_width=size,
            output_height=size,
        )
print(f"PNGs in {len(SIZES)} Größen × {len(RENDERED)} Icons = {len(SIZES) * len(RENDERED)} Dateien.")

# 2. Flache Repo-Icons (images/toolbar-*.png) synchronisieren – diese packt der Build.
for name in RENDERED:
    cairosvg.svg2png(
        url=svg_pfad(name),
        write_to=os.path.join(FLAT_DIR, f"{name}.png"),
        output_width=FLAT_SIZE,
        output_height=FLAT_SIZE,
    )
print(f"Flache Icons ({FLAT_SIZE}×{FLAT_SIZE}) nach {FLAT_DIR}/ synchronisiert: {len(RENDERED)} Dateien.")


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


# 3. Übersicht – nach Toolbar-Gruppe gegliedert (nur tatsächlich gerenderte Icons)
CELL_W, CELL_H = 175, 115
PAD = 12
COLS = 4


def sichtbar(names):
    return [n for n in names if n in RENDERED]


def build_section(names, title, x0, y0):
    names = sichtbar(names)
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
            ico = Image.open(os.path.join(PNG_DIR, str(s), f"{name}.png")).convert("RGBA")
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


def section_h(n):
    return 24 + ((n + COLS - 1) // COLS) * (CELL_H + PAD)


TITLE_H = 60
W = COLS * CELL_W + (COLS + 1) * PAD
H = (TITLE_H + section_h(len(sichtbar(HAUPT))) + section_h(len(sichtbar(SPIELTAG)))
     + section_h(len(sichtbar(TIMER))) + 30)

img = Image.new("RGB", (W, H), (250, 250, 252))
draw = ImageDraw.Draw(img)

draw.text((PAD, 14), "Petanque-Turnier-Manager — Toolbar Icons",
           fill=(31, 41, 55), font=font_title)
draw.text((PAD, 40), f"{len(RENDERED)} Icons in 3 Toolbar-Gruppen",
           fill=(107, 114, 128), font=font_subtitle)

y = TITLE_H
y = build_section(HAUPT, "Haupt-Toolbar  (Addons_Z2_Toolbar.xcu)", PAD, y) + 6
y = build_section(SPIELTAG, "Spieltag-Toolbar  (Addons_Z3_SpieltagToolbar.xcu)", PAD, y) + 6
y = build_section(TIMER, "Timer-Toolbar  (Addons_Z4_TimerToolbar.xcu)", PAD, y) + 6

uebersicht = os.path.join(SCRIPT_DIR, "preview-overview-v2.png")
img.save(uebersicht, optimize=True)
print(f"Übersicht: {uebersicht} ({W}x{H})")
