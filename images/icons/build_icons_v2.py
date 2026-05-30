"""
Toolbar-Icons v2 für Petanque-Turnier-Manager.
Thematisch an den Sourcecode angepasst. 23 Icons insgesamt.
"""
import math
import os

C = {
    "ink":      "#1F2937",
    "outline":  "#374151",
    "paper":    "#FFFFFF",
    "paper2":   "#F3F4F6",
    "line":     "#D1D5DB",
    "boule":    "#B0B7C0",
    "boule_d":  "#7C8591",
    "cochon":   "#F59E0B",
    "go":       "#16A34A",
    "go_d":     "#15803D",
    "stop":     "#DC2626",
    "stop_d":   "#991B1B",
    "gold":     "#FBBF24",
    "gold_d":   "#D97706",
    "silver":   "#9CA3AF",
    "bronze":   "#B45309",
    "info":     "#2563EB",
    "info_d":   "#1D4ED8",
    "folder":   "#EAB308",
    "folder_d": "#A16207",
    "screen":   "#3B82F6",
    "screen_d": "#1E40AF",
}
S = 1.5  # Stroke
ICONS = {}

def svg(content, vb=64):
    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {vb} {vb}" width="{vb}" height="{vb}">
{content}
</svg>'''

def boule(cx, cy, r, color=C["boule"], color_d=C["boule_d"]):
    return f'''<circle cx="{cx}" cy="{cy}" r="{r}" fill="{color}" stroke="{C['ink']}" stroke-width="{S}"/>
<path d="M {cx-r*0.85} {cy} Q {cx} {cy-r*0.4} {cx+r*0.85} {cy}" fill="none" stroke="{color_d}" stroke-width="{S*0.7}"/>
<path d="M {cx-r*0.85} {cy} Q {cx} {cy+r*0.4} {cx+r*0.85} {cy}" fill="none" stroke="{color_d}" stroke-width="{S*0.7}"/>
<ellipse cx="{cx-r*0.35}" cy="{cy-r*0.35}" rx="{r*0.25}" ry="{r*0.15}" fill="{C['paper']}" opacity="0.5"/>'''

def stopwatch_base():
    """Gemeinsame Stoppuhr-Basis für alle Timer-Icons."""
    return f'''
  <!-- Bügel oben -->
  <rect x="27" y="6" width="10" height="4" rx="1" fill="{C['ink']}"/>
  <line x1="32" y1="10" x2="32" y2="14" stroke="{C['ink']}" stroke-width="{S*1.5}" stroke-linecap="round"/>
  <!-- Krone oben rechts -->
  <line x1="44" y1="13" x2="48" y2="9" stroke="{C['ink']}" stroke-width="{S*1.5}" stroke-linecap="round"/>
  <!-- Gehäuse -->
  <circle cx="32" cy="36" r="22" fill="{C['paper']}" stroke="{C['ink']}" stroke-width="{S}"/>
  <circle cx="32" cy="36" r="22" fill="none" stroke="{C['line']}" stroke-width="{S*0.5}"/>
  <!-- Skala-Marken (12, 3, 6, 9 Uhr Positionen) -->
  <line x1="32" y1="17" x2="32" y2="20" stroke="{C['ink']}" stroke-width="{S}" stroke-linecap="round"/>
  <line x1="32" y1="52" x2="32" y2="55" stroke="{C['ink']}" stroke-width="{S}" stroke-linecap="round"/>
  <line x1="13" y1="36" x2="16" y2="36" stroke="{C['ink']}" stroke-width="{S}" stroke-linecap="round"/>
  <line x1="48" y1="36" x2="51" y2="36" stroke="{C['ink']}" stroke-width="{S}" stroke-linecap="round"/>
  <!-- Zeiger -->
  <line x1="32" y1="36" x2="32" y2="22" stroke="{C['ink']}" stroke-width="{S*1.4}" stroke-linecap="round"/>
  <line x1="32" y1="36" x2="42" y2="32" stroke="{C['stop']}" stroke-width="{S*1.2}" stroke-linecap="round"/>
  <circle cx="32" cy="36" r="2" fill="{C['ink']}"/>'''


# ============================================================
# 1. START — Play + Boule-Kugeln (Turnier starten, nicht abstraktes Play)
# ============================================================
ICONS["toolbar-start"] = svg(f'''
  <!-- Hintergrund-Kreis grün -->
  <circle cx="32" cy="32" r="26" fill="{C['go']}" stroke="{C['ink']}" stroke-width="{S}"/>
  <!-- Play-Dreieck weiß -->
  <path d="M 24 18 L 46 32 L 24 46 Z" fill="{C['paper']}" stroke="{C['ink']}" stroke-width="{S}" stroke-linejoin="round"/>
  <!-- Kleine Boule rechts unten – signalisiert: Petanque-Turnier -->
  {boule(50, 50, 8)}
''')

# ============================================================
# 2. NEU — Dokument mit Plus, Boule-Hint
# ============================================================
ICONS["toolbar-neu-in-neuer-datei"] = svg(f'''
  <path d="M 14 10 L 38 10 L 50 22 L 50 56 L 14 56 Z" fill="{C['paper']}" stroke="{C['ink']}" stroke-width="{S}" stroke-linejoin="round"/>
  <path d="M 38 10 L 38 22 L 50 22" fill="{C['paper2']}" stroke="{C['ink']}" stroke-width="{S}" stroke-linejoin="round"/>
  <!-- Kleine Boule-Markierung innerhalb -->
  <circle cx="24" cy="38" r="4" fill="{C['boule']}" stroke="{C['ink']}" stroke-width="{S*0.7}"/>
  <circle cx="34" cy="38" r="4" fill="{C['boule']}" stroke="{C['ink']}" stroke-width="{S*0.7}"/>
  <circle cx="29" cy="46" r="2" fill="{C['cochon']}"/>
  <!-- Plus-Badge -->
  <circle cx="48" cy="48" r="11" fill="{C['go']}" stroke="{C['ink']}" stroke-width="{S}"/>
  <path d="M 48 42 L 48 54 M 42 48 L 54 48" stroke="{C['paper']}" stroke-width="{S*1.8}" stroke-linecap="round"/>
''')

# ============================================================
# 3. ÖFFNEN — Ordner (unverändert)
# ============================================================
ICONS["toolbar-oeffnen"] = svg(f'''
  <path d="M 8 18 L 26 18 L 30 22 L 56 22 L 56 52 L 8 52 Z" fill="{C['folder_d']}" stroke="{C['ink']}" stroke-width="{S}" stroke-linejoin="round"/>
  <path d="M 8 52 L 16 28 L 60 28 L 52 52 Z" fill="{C['folder']}" stroke="{C['ink']}" stroke-width="{S}" stroke-linejoin="round"/>
''')

# ============================================================
# 4. KONFIGURATION — Zahnrad (unverändert)
# ============================================================
def gear_path(cx, cy, r_outer, r_inner, teeth=8):
    pts = []
    for i in range(teeth * 2):
        angle = (i / (teeth * 2)) * 2 * math.pi
        if i % 2 == 0:
            a1 = angle - 0.18
            a2 = angle + 0.18
            pts.append((cx + r_outer * math.cos(a1), cy + r_outer * math.sin(a1)))
            pts.append((cx + r_outer * math.cos(a2), cy + r_outer * math.sin(a2)))
        else:
            pts.append((cx + r_inner * math.cos(angle), cy + r_inner * math.sin(angle)))
    return "M " + " L ".join(f"{x:.2f} {y:.2f}" for x, y in pts) + " Z"

ICONS["toolbar-konfiguration"] = svg(f'''
  <path d="{gear_path(32, 32, 26, 19, 8)}" fill="{C['info']}" stroke="{C['ink']}" stroke-width="{S}" stroke-linejoin="round"/>
  <circle cx="32" cy="32" r="9" fill="{C['paper']}" stroke="{C['ink']}" stroke-width="{S}"/>
''')

# ============================================================
# 5. TEILNEHMER — Personen-Gruppe (unverändert)
# ============================================================
ICONS["toolbar-teilnehmer"] = svg(f'''
  <circle cx="16" cy="24" r="7" fill="{C['silver']}" stroke="{C['ink']}" stroke-width="{S}"/>
  <path d="M 6 50 Q 6 36 16 36 Q 26 36 26 50 Z" fill="{C['silver']}" stroke="{C['ink']}" stroke-width="{S}" stroke-linejoin="round"/>
  <circle cx="48" cy="24" r="7" fill="{C['silver']}" stroke="{C['ink']}" stroke-width="{S}"/>
  <path d="M 38 50 Q 38 36 48 36 Q 58 36 58 50 Z" fill="{C['silver']}" stroke="{C['ink']}" stroke-width="{S}" stroke-linejoin="round"/>
  <circle cx="32" cy="20" r="9" fill="{C['info']}" stroke="{C['ink']}" stroke-width="{S}"/>
  <path d="M 18 56 Q 18 36 32 36 Q 46 36 46 56 Z" fill="{C['info']}" stroke="{C['ink']}" stroke-width="{S}" stroke-linejoin="round"/>
''')

# ============================================================
# 5b. CHECKIN — Anwesenheitsliste (Klemmbrett) mit grünem Haken-Badge
# ============================================================
ICONS["toolbar-checkin"] = svg(f'''
  <!-- Klemmbrett / Anwesenheitsliste -->
  <rect x="14" y="10" width="36" height="46" rx="3" fill="{C['paper']}" stroke="{C['ink']}" stroke-width="{S}"/>
  <rect x="26" y="6" width="12" height="7" rx="2" fill="{C['silver']}" stroke="{C['ink']}" stroke-width="{S}"/>
  <!-- Zeilen mit abgehakten Teilnehmern -->
  <path d="M 19 22 l 3 3 l 5 -6" fill="none" stroke="{C['go']}" stroke-width="{S*1.4}" stroke-linecap="round" stroke-linejoin="round"/>
  <line x1="31" y1="22" x2="45" y2="22" stroke="{C['line']}" stroke-width="{S}" stroke-linecap="round"/>
  <path d="M 19 33 l 3 3 l 5 -6" fill="none" stroke="{C['go']}" stroke-width="{S*1.4}" stroke-linecap="round" stroke-linejoin="round"/>
  <line x1="31" y1="33" x2="45" y2="33" stroke="{C['line']}" stroke-width="{S}" stroke-linecap="round"/>
  <line x1="19" y1="44" x2="27" y2="44" stroke="{C['line']}" stroke-width="{S}" stroke-linecap="round"/>
  <line x1="31" y1="44" x2="45" y2="44" stroke="{C['line']}" stroke-width="{S}" stroke-linecap="round"/>
  <!-- Großes Check-Badge unten rechts -->
  <circle cx="48" cy="48" r="12" fill="{C['go']}" stroke="{C['ink']}" stroke-width="{S}"/>
  <path d="M 42 48 l 4 4 l 8 -9" fill="none" stroke="{C['paper']}" stroke-width="{S*1.8}" stroke-linecap="round" stroke-linejoin="round"/>
''')

# ============================================================
# 6. TURNIER-MODUS — KIOSK / Vollbild-Monitor (THEMATISCH KORREKT)
# ============================================================
ICONS["toolbar-turnier-modus"] = svg(f'''
  <!-- Monitor / Bildschirm -->
  <rect x="6" y="10" width="52" height="34" rx="3" fill="{C['screen']}" stroke="{C['ink']}" stroke-width="{S}"/>
  <rect x="9" y="13" width="46" height="28" rx="1" fill="{C['paper']}" stroke="{C['ink']}" stroke-width="{S*0.7}"/>
  <!-- Standfuß -->
  <rect x="26" y="44" width="12" height="6" fill="{C['screen_d']}" stroke="{C['ink']}" stroke-width="{S}"/>
  <rect x="18" y="50" width="28" height="4" rx="1" fill="{C['screen_d']}" stroke="{C['ink']}" stroke-width="{S}"/>
  <!-- Vollbild-Pfeile (4 Ecken nach außen) -->
  <path d="M 16 19 L 12 16 L 12 21 M 12 16 L 17 16" fill="none" stroke="{C['ink']}" stroke-width="{S*1.5}" stroke-linecap="round" stroke-linejoin="round"/>
  <path d="M 48 19 L 52 16 L 52 21 M 52 16 L 47 16" fill="none" stroke="{C['ink']}" stroke-width="{S*1.5}" stroke-linecap="round" stroke-linejoin="round"/>
  <path d="M 16 34 L 12 38 L 12 33 M 12 38 L 17 38" fill="none" stroke="{C['ink']}" stroke-width="{S*1.5}" stroke-linecap="round" stroke-linejoin="round"/>
  <path d="M 48 34 L 52 38 L 52 33 M 52 38 L 47 38" fill="none" stroke="{C['ink']}" stroke-width="{S*1.5}" stroke-linecap="round" stroke-linejoin="round"/>
  <!-- Inhalt: kleines Boule-Symbol (signalisiert: Turnier-Ansicht) -->
  <circle cx="32" cy="27" r="3" fill="{C['boule_d']}"/>
''')

# ============================================================
# 7. SPIELERDB → MELDELISTE (Übernahme, nicht nur DB-Start)
# ============================================================
ICONS["toolbar-spielerdb-meldungen"] = svg(f'''
  <!-- Datenbank-Zylinder links -->
  <path d="M 4 18 L 4 42 Q 4 47 16 47 Q 28 47 28 42 L 28 18" fill="{C['info']}" stroke="{C['ink']}" stroke-width="{S}" stroke-linejoin="round"/>
  <ellipse cx="16" cy="18" rx="12" ry="4" fill="{C['info_d']}" stroke="{C['ink']}" stroke-width="{S}"/>
  <path d="M 4 26 Q 16 30 28 26" fill="none" stroke="{C['ink']}" stroke-width="{S*0.6}" opacity="0.6"/>
  <path d="M 4 34 Q 16 38 28 34" fill="none" stroke="{C['ink']}" stroke-width="{S*0.6}" opacity="0.6"/>
  <!-- Pfeil von DB nach Liste -->
  <path d="M 30 32 L 38 32" stroke="{C['go']}" stroke-width="{S*2.2}" stroke-linecap="round"/>
  <path d="M 35 28 L 39 32 L 35 36" fill="none" stroke="{C['go']}" stroke-width="{S*2.2}" stroke-linecap="round" stroke-linejoin="round"/>
  <!-- Liste rechts -->
  <rect x="40" y="14" width="20" height="36" rx="2" fill="{C['paper']}" stroke="{C['ink']}" stroke-width="{S}"/>
  <circle cx="44" cy="22" r="1.6" fill="{C['ink']}"/>
  <line x1="48" y1="22" x2="57" y2="22" stroke="{C['line']}" stroke-width="{S}" stroke-linecap="round"/>
  <circle cx="44" cy="30" r="1.6" fill="{C['ink']}"/>
  <line x1="48" y1="30" x2="57" y2="30" stroke="{C['line']}" stroke-width="{S}" stroke-linecap="round"/>
  <circle cx="44" cy="38" r="1.6" fill="{C['ink']}"/>
  <line x1="48" y1="38" x2="57" y2="38" stroke="{C['line']}" stroke-width="{S}" stroke-linecap="round"/>
  <circle cx="44" cy="46" r="1.6" fill="{C['go']}"/>
  <line x1="48" y1="46" x2="55" y2="46" stroke="{C['go']}" stroke-width="{S}" stroke-linecap="round"/>
''')

# ============================================================
# 8. NEU AUSLOSEN — Shuffle (unverändert)
# ============================================================
ICONS["toolbar-neu-auslosen"] = svg(f'''
  <path d="M 10 18 L 22 18 Q 30 18 32 28 L 36 40 Q 38 46 46 46 L 54 46"
        fill="none" stroke="{C['go']}" stroke-width="{S*2.2}" stroke-linecap="round" stroke-linejoin="round"/>
  <path d="M 50 41 L 56 46 L 50 51" fill="none" stroke="{C['go']}" stroke-width="{S*2.2}" stroke-linecap="round" stroke-linejoin="round"/>
  <path d="M 10 46 L 22 46 Q 30 46 32 36 L 36 24 Q 38 18 46 18 L 54 18"
        fill="none" stroke="{C['info']}" stroke-width="{S*2.2}" stroke-linecap="round" stroke-linejoin="round"/>
  <path d="M 50 13 L 56 18 L 50 23" fill="none" stroke="{C['info']}" stroke-width="{S*2.2}" stroke-linecap="round" stroke-linejoin="round"/>
''')

# ============================================================
# 9. WEITER — Pfeil + Listenzeilen (Nächste Runde, nicht fast-forward)
# ============================================================
ICONS["toolbar-weiter"] = svg(f'''
  <!-- Liste hinter dem Pfeil (deutet "Runde" an) -->
  <rect x="8" y="14" width="28" height="36" rx="2" fill="{C['paper']}" stroke="{C['ink']}" stroke-width="{S}"/>
  <line x1="13" y1="22" x2="31" y2="22" stroke="{C['line']}" stroke-width="{S*1.2}" stroke-linecap="round"/>
  <line x1="13" y1="29" x2="31" y2="29" stroke="{C['line']}" stroke-width="{S*1.2}" stroke-linecap="round"/>
  <line x1="13" y1="36" x2="31" y2="36" stroke="{C['line']}" stroke-width="{S*1.2}" stroke-linecap="round"/>
  <line x1="13" y1="43" x2="25" y2="43" stroke="{C['line']}" stroke-width="{S*1.2}" stroke-linecap="round"/>
  <!-- Großer Pfeil rechts -->
  <path d="M 36 32 L 56 32 M 48 24 L 58 32 L 48 40"
        fill="none" stroke="{C['go']}" stroke-width="{S*3}" stroke-linecap="round" stroke-linejoin="round"/>
''')

# ============================================================
# 10. NÄCHSTER SPIELTAG — Kalender mit Pfeil (unverändert)
# ============================================================
ICONS["toolbar-naechster-spieltag"] = svg(f'''
  <rect x="8" y="14" width="40" height="42" rx="3" fill="{C['paper']}" stroke="{C['ink']}" stroke-width="{S}"/>
  <rect x="8" y="14" width="40" height="10" rx="3" fill="{C['stop']}" stroke="{C['ink']}" stroke-width="{S}"/>
  <rect x="15" y="8" width="3" height="10" rx="1.5" fill="{C['ink']}"/>
  <rect x="38" y="8" width="3" height="10" rx="1.5" fill="{C['ink']}"/>
  <line x1="14" y1="32" x2="42" y2="32" stroke="{C['line']}" stroke-width="{S*0.6}"/>
  <line x1="14" y1="42" x2="42" y2="42" stroke="{C['line']}" stroke-width="{S*0.6}"/>
  <line x1="21" y1="26" x2="21" y2="52" stroke="{C['line']}" stroke-width="{S*0.6}"/>
  <line x1="35" y1="26" x2="35" y2="52" stroke="{C['line']}" stroke-width="{S*0.6}"/>
  <rect x="22" y="33" width="12" height="8" fill="{C['go']}" opacity="0.3"/>
  <circle cx="50" cy="48" r="11" fill="{C['go']}" stroke="{C['ink']}" stroke-width="{S}"/>
  <path d="M 45 43 L 53 48 L 45 53 Z" fill="{C['paper']}" stroke-linejoin="round"/>
''')

# ============================================================
# 11. VORRUNDEN-RANGLISTE — Liste mit farbigen Rängen
# ============================================================
ICONS["toolbar-vorrunden-rangliste"] = svg(f'''
  <rect x="10" y="8" width="44" height="48" rx="3" fill="{C['paper']}" stroke="{C['ink']}" stroke-width="{S}"/>
  <circle cx="18" cy="18" r="3" fill="{C['gold']}" stroke="{C['ink']}" stroke-width="{S*0.7}"/>
  <line x1="25" y1="18" x2="48" y2="18" stroke="{C['line']}" stroke-width="{S*1.4}" stroke-linecap="round"/>
  <circle cx="18" cy="28" r="3" fill="{C['silver']}" stroke="{C['ink']}" stroke-width="{S*0.7}"/>
  <line x1="25" y1="28" x2="46" y2="28" stroke="{C['line']}" stroke-width="{S*1.4}" stroke-linecap="round"/>
  <circle cx="18" cy="38" r="3" fill="{C['bronze']}" stroke="{C['ink']}" stroke-width="{S*0.7}"/>
  <line x1="25" y1="38" x2="44" y2="38" stroke="{C['line']}" stroke-width="{S*1.4}" stroke-linecap="round"/>
  <circle cx="18" cy="48" r="3" fill="{C['paper2']}" stroke="{C['ink']}" stroke-width="{S*0.7}"/>
  <line x1="25" y1="48" x2="42" y2="48" stroke="{C['line']}" stroke-width="{S*1.4}" stroke-linecap="round"/>
''')

# ============================================================
# 12. GESAMTRANGLISTE — Pokal + Podest (unverändert)
# ============================================================
ICONS["toolbar-gesamtrangliste"] = svg(f'''
  <path d="M 24 8 L 40 8 L 39 22 Q 39 28 32 28 Q 25 28 25 22 Z" fill="{C['gold']}" stroke="{C['ink']}" stroke-width="{S}" stroke-linejoin="round"/>
  <path d="M 24 12 Q 18 12 18 18 Q 18 22 24 22" fill="none" stroke="{C['ink']}" stroke-width="{S}"/>
  <path d="M 40 12 Q 46 12 46 18 Q 46 22 40 22" fill="none" stroke="{C['ink']}" stroke-width="{S}"/>
  <rect x="29" y="28" width="6" height="4" fill="{C['gold_d']}" stroke="{C['ink']}" stroke-width="{S}"/>
  <rect x="25" y="32" width="14" height="3" rx="1" fill="{C['gold_d']}" stroke="{C['ink']}" stroke-width="{S}"/>
  <rect x="22" y="40" width="20" height="16" fill="{C['gold']}" stroke="{C['ink']}" stroke-width="{S}"/>
  <rect x="6"  y="46" width="16" height="10" fill="{C['silver']}" stroke="{C['ink']}" stroke-width="{S}"/>
  <rect x="42" y="48" width="16" height="8"  fill="{C['bronze']}" stroke="{C['ink']}" stroke-width="{S}"/>
  <text x="32" y="51" font-family="sans-serif" font-size="9" font-weight="bold" text-anchor="middle" fill="{C['ink']}">1</text>
  <text x="14" y="54" font-family="sans-serif" font-size="7" font-weight="bold" text-anchor="middle" fill="{C['ink']}">2</text>
  <text x="50" y="55" font-family="sans-serif" font-size="7" font-weight="bold" text-anchor="middle" fill="{C['ink']}">3</text>
''')

# ============================================================
# 13. ABSCHLUSS — Zielflagge (Final Phase, nicht "fertig")
# ============================================================
ICONS["toolbar-abschluss"] = svg(f'''
  <!-- Fahnenstange -->
  <line x1="14" y1="8" x2="14" y2="58" stroke="{C['ink']}" stroke-width="{S*2}" stroke-linecap="round"/>
  <!-- Karierte Zielflagge (4x3 Felder, schwarz/weiß alternierend) -->
  <rect x="14" y="10" width="40" height="24" fill="{C['paper']}" stroke="{C['ink']}" stroke-width="{S}"/>
  <!-- Schachbrett-Muster -->
  <rect x="14" y="10" width="10" height="8" fill="{C['ink']}"/>
  <rect x="34" y="10" width="10" height="8" fill="{C['ink']}"/>
  <rect x="24" y="18" width="10" height="8" fill="{C['ink']}"/>
  <rect x="44" y="18" width="10" height="8" fill="{C['ink']}"/>
  <rect x="14" y="26" width="10" height="8" fill="{C['ink']}"/>
  <rect x="34" y="26" width="10" height="8" fill="{C['ink']}"/>
  <!-- Sockel -->
  <ellipse cx="14" cy="58" rx="6" ry="2" fill="{C['ink']}"/>
''')

# ============================================================
# 14. ABBRUCH — X im roten Kreis (unverändert)
# ============================================================
ICONS["toolbar-abbruch"] = svg(f'''
  <circle cx="32" cy="32" r="26" fill="{C['stop']}" stroke="{C['ink']}" stroke-width="{S}"/>
  <path d="M 21 21 L 43 43 M 43 21 L 21 43" fill="none" stroke="{C['paper']}" stroke-width="{S*3.2}" stroke-linecap="round"/>
''')

# ============================================================
# 15. DRUCKEN — Drucker (unverändert)
# ============================================================
ICONS["toolbar-drucken"] = svg(f'''
  <rect x="16" y="8" width="32" height="20" fill="{C['paper']}" stroke="{C['ink']}" stroke-width="{S}"/>
  <line x1="20" y1="15" x2="40" y2="15" stroke="{C['line']}" stroke-width="{S}"/>
  <line x1="20" y1="20" x2="36" y2="20" stroke="{C['line']}" stroke-width="{S}"/>
  <rect x="10" y="26" width="44" height="22" rx="3" fill="{C['silver']}" stroke="{C['ink']}" stroke-width="{S}"/>
  <circle cx="46" cy="34" r="2" fill="{C['go']}"/>
  <rect x="16" y="40" width="32" height="16" fill="{C['paper']}" stroke="{C['ink']}" stroke-width="{S}"/>
  <line x1="20" y1="47" x2="40" y2="47" stroke="{C['line']}" stroke-width="{S}"/>
  <line x1="20" y1="52" x2="36" y2="52" stroke="{C['line']}" stroke-width="{S}"/>
''')

# ============================================================
# 16. DRUCKVORSCHAU — Drucker mit Lupe (unverändert)
# ============================================================
ICONS["toolbar-druckvorschau"] = svg(f'''
  <rect x="12" y="6" width="28" height="18" fill="{C['paper']}" stroke="{C['ink']}" stroke-width="{S}"/>
  <line x1="16" y1="12" x2="34" y2="12" stroke="{C['line']}" stroke-width="{S}"/>
  <line x1="16" y1="17" x2="30" y2="17" stroke="{C['line']}" stroke-width="{S}"/>
  <rect x="6" y="22" width="40" height="20" rx="3" fill="{C['silver']}" stroke="{C['ink']}" stroke-width="{S}"/>
  <circle cx="38" cy="29" r="1.8" fill="{C['go']}"/>
  <rect x="12" y="35" width="28" height="14" fill="{C['paper']}" stroke="{C['ink']}" stroke-width="{S}"/>
  <line x1="16" y1="41" x2="34" y2="41" stroke="{C['line']}" stroke-width="{S}"/>
  <circle cx="44" cy="44" r="11" fill="{C['paper']}" stroke="{C['ink']}" stroke-width="{S*1.5}" opacity="0.95"/>
  <circle cx="44" cy="44" r="6" fill="none" stroke="{C['info']}" stroke-width="{S*1.2}"/>
  <line x1="51" y1="51" x2="59" y2="59" stroke="{C['ink']}" stroke-width="{S*2.5}" stroke-linecap="round"/>
''')

# ============================================================
# 17. WEBSERVER STARTEN — Server-Rack mit Play (unverändert)
# ============================================================
ICONS["toolbar-webserver-starten"] = svg(f'''
  <rect x="10" y="10" width="36" height="46" rx="3" fill="{C['paper2']}" stroke="{C['ink']}" stroke-width="{S}"/>
  <rect x="14" y="14" width="28" height="10" rx="1.5" fill="{C['paper']}" stroke="{C['ink']}" stroke-width="{S}"/>
  <circle cx="38" cy="19" r="1.5" fill="{C['go']}"/>
  <rect x="14" y="27" width="28" height="10" rx="1.5" fill="{C['paper']}" stroke="{C['ink']}" stroke-width="{S}"/>
  <circle cx="38" cy="32" r="1.5" fill="{C['go']}"/>
  <rect x="14" y="40" width="28" height="10" rx="1.5" fill="{C['paper']}" stroke="{C['ink']}" stroke-width="{S}"/>
  <circle cx="38" cy="45" r="1.5" fill="{C['go']}"/>
  <circle cx="50" cy="48" r="11" fill="{C['go']}" stroke="{C['ink']}" stroke-width="{S}"/>
  <path d="M 47 43 L 55 48 L 47 53 Z" fill="{C['paper']}" stroke-linejoin="round"/>
''')

# ============================================================
# 18. WEBSERVER STOPPEN — Server-Rack mit Stop (unverändert)
# ============================================================
ICONS["toolbar-webserver-stoppen"] = svg(f'''
  <rect x="10" y="10" width="36" height="46" rx="3" fill="{C['paper2']}" stroke="{C['ink']}" stroke-width="{S}"/>
  <rect x="14" y="14" width="28" height="10" rx="1.5" fill="{C['paper']}" stroke="{C['ink']}" stroke-width="{S}"/>
  <circle cx="38" cy="19" r="1.5" fill="{C['stop']}"/>
  <rect x="14" y="27" width="28" height="10" rx="1.5" fill="{C['paper']}" stroke="{C['ink']}" stroke-width="{S}"/>
  <circle cx="38" cy="32" r="1.5" fill="{C['stop']}"/>
  <rect x="14" y="40" width="28" height="10" rx="1.5" fill="{C['paper']}" stroke="{C['ink']}" stroke-width="{S}"/>
  <circle cx="38" cy="45" r="1.5" fill="{C['stop']}"/>
  <circle cx="50" cy="48" r="11" fill="{C['stop']}" stroke="{C['ink']}" stroke-width="{S}"/>
  <rect x="45" y="43" width="10" height="10" rx="1.5" fill="{C['paper']}"/>
''')

# ============================================================
# 19. TIMER START — Stoppuhr mit grünem Play (NEU)
# ============================================================
ICONS["toolbar-timer-start"] = svg(f'''
  {stopwatch_base()}
  <!-- Play-Badge rechts unten -->
  <circle cx="52" cy="52" r="10" fill="{C['go']}" stroke="{C['ink']}" stroke-width="{S}"/>
  <path d="M 49 48 L 56 52 L 49 56 Z" fill="{C['paper']}" stroke-linejoin="round"/>
''')

# ============================================================
# 20. TIMER PAUSE — Stoppuhr mit Pause (NEU)
# ============================================================
ICONS["toolbar-timer-pause"] = svg(f'''
  {stopwatch_base()}
  <!-- Pause-Badge rechts unten -->
  <circle cx="52" cy="52" r="10" fill="{C['cochon']}" stroke="{C['ink']}" stroke-width="{S}"/>
  <rect x="48" y="47" width="3" height="10" rx="0.5" fill="{C['paper']}"/>
  <rect x="53" y="47" width="3" height="10" rx="0.5" fill="{C['paper']}"/>
''')

# ============================================================
# 21. TIMER STOP — Stoppuhr mit rotem Stop (NEU)
# ============================================================
ICONS["toolbar-timer-stop"] = svg(f'''
  {stopwatch_base()}
  <!-- Stop-Badge rechts unten -->
  <circle cx="52" cy="52" r="10" fill="{C['stop']}" stroke="{C['ink']}" stroke-width="{S}"/>
  <rect x="48" y="48" width="8" height="8" rx="1" fill="{C['paper']}"/>
''')

# ============================================================
# 22. TIMER +1 — Stoppuhr mit Plus-Badge (NEU)
# ============================================================
ICONS["toolbar-timer-plus1"] = svg(f'''
  {stopwatch_base()}
  <!-- +1-Badge rechts unten -->
  <circle cx="52" cy="52" r="10" fill="{C['go']}" stroke="{C['ink']}" stroke-width="{S}"/>
  <path d="M 52 47 L 52 57 M 47 52 L 57 52" stroke="{C['paper']}" stroke-width="{S*1.8}" stroke-linecap="round"/>
''')

# ============================================================
# 23. TIMER -1 — Stoppuhr mit Minus-Badge (NEU)
# ============================================================
ICONS["toolbar-timer-minus1"] = svg(f'''
  {stopwatch_base()}
  <!-- -1-Badge rechts unten -->
  <circle cx="52" cy="52" r="10" fill="{C['cochon']}" stroke="{C['ink']}" stroke-width="{S}"/>
  <path d="M 47 52 L 57 52" stroke="{C['paper']}" stroke-width="{S*1.8}" stroke-linecap="round"/>
''')

# ============================================================
# 24. TIMER SNOOZE — Stoppuhr mit blauem Z-Badge (Gong stummschalten)
# ============================================================
ICONS["toolbar-timer-snooze"] = svg(f'''
  {stopwatch_base()}
  <!-- Snooze-Badge rechts unten: weisses "Z" auf blauem Kreis -->
  <circle cx="52" cy="52" r="10" fill="{C['info']}" stroke="{C['ink']}" stroke-width="{S}"/>
  <path d="M 47.5 47.5 L 56.5 47.5 L 47.5 56.5 L 56.5 56.5"
        fill="none" stroke="{C['paper']}" stroke-width="{S*1.8}"
        stroke-linecap="round" stroke-linejoin="round"/>
''')


# ============================================================
# SCHREIBEN
# ============================================================
OUT = "/home/claude/icons_v2/svg"
os.makedirs(OUT, exist_ok=True)
for name, content in ICONS.items():
    with open(f"{OUT}/{name}.svg", "w") as f:
        f.write(content)
    print(f"✓ {name}.svg")

print(f"\n{len(ICONS)} Icons erstellt.")
