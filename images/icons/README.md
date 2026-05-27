# Toolbar Icons v2 — Petanque-Turnier-Manager

Überarbeitetes, einheitliches Icon-Set basierend auf einer Analyse des Source-Codes im GitHub-Repo `michaelmassee/Petanque-Turnier-Manager`.

**Drop-in:** Die Dateinamen sind identisch zu den Pfaden in den `Addons_Z*_*Toolbar.xcu`-Dateien. Einfach die `images/`-Inhalte ersetzen — keine XCU-Anpassungen nötig.

## Was sich gegenüber dem ersten Set geändert hat

Fünf Icons wurden thematisch korrigiert, nachdem klar wurde, was die zugehörige Aktion im Code tatsächlich tut:

| Datei | Code-URL | Alte Metapher | Korrigiert |
|-------|----------|---------------|------------|
| `toolbar-turnier-modus` | `ptm:turnier_modus` | Turnierbaum / Bracket | **Monitor mit Vollbild-Pfeilen** — laut `TurnierModus.java` ist das ein **Kiosk-Toggle**: blendet Menubar/Standardbar/Statusbar aus, nicht die Modusauswahl. |
| `toolbar-abschluss` | `ptm:toolbar_abschluss` | Grünes Häkchen | **Karierte Zielflagge** — der UI-Text ist "Abschlussphase / Final Phase" und ruft je nach Turniersystem die Finalrunden auf (z.B. KO-Turnierbaum, A/B-Poule). Eine Zielflagge trifft "Phase", nicht "abgeschlossen". |
| `toolbar-spielerdb-meldungen` | `ptm:spielerdb_in_meldeliste` | DB mit Play-Button | **DB → Pfeil → Liste** — der Code ruft `uebernehmenInMeldeliste()`. Das Icon zeigt jetzt den Übertrag, nicht den DB-Start. |
| `toolbar-weiter` | `ptm:toolbar_weiter` | Doppelpfeil (Fast-Forward) | **Liste mit Einzelpfeil** — Aktion heißt "Next Round" / "Nächste Runde". Doppelpfeil suggeriert "viele Runden überspringen". |
| `toolbar-start` | `ptm:toolbar_start` | Generischer Play-Kreis | **Play + Boule-Kugel** — Aktion ist "Turnier starten", nicht abstrakter App-Start. |

## Was komplett neu ist (fehlte im alten Set)

Die Timer-Toolbar (`Addons_Z4_TimerToolbar.xcu`) war im alten Icon-Set überhaupt nicht abgedeckt. Fünf Icons mit gemeinsamer Stoppuhr-Basis und unterschiedlichen Action-Badges rechts unten:

| Datei | Code-URL | Action-Badge |
|-------|----------|--------------|
| `toolbar-timer-start` | `ptm:timer_starten_dialog` | Grünes Play |
| `toolbar-timer-pause` | `ptm:timer_pause_fortsetzen` | Orange Pause-Striche (Toggle) |
| `toolbar-timer-stop` | `ptm:timer_stoppen` | Rotes Stop-Quadrat |
| `toolbar-timer-plus1` | `ptm:timer_plus_minute` | Grünes Plus |
| `toolbar-timer-minus1` | `ptm:timer_minus_minute` | Oranges Minus |

Die einheitliche Stoppuhr macht die Gruppe in der Toolbar sofort als zusammengehörig erkennbar.

## Vollständiges Mapping (XCU-Eintrag → Bilddatei)

### Haupt-Toolbar (`Addons_Z2_Toolbar.xcu`)

| Schaltfläche | Code-URL | Icon-Datei |
|--------------|----------|------------|
| m1 Turnier starten | `ptm:toolbar_start` | `toolbar-start.png` |
| m3 Weiter | `ptm:toolbar_weiter` | `toolbar-weiter.png` |
| m4 Neu Auslosen | `ptm:toolbar_neu_auslosen` | `toolbar-neu-auslosen.png` |
| m5 Abschlussphase | `ptm:toolbar_abschluss` | `toolbar-abschluss.png` |
| m6 Vorrunden Rangliste | `ptm:toolbar_vorrunden_rangliste` | `toolbar-vorrunden-rangliste.png` |
| m7 Teilnehmer | `ptm:toolbar_teilnehmer` | `toolbar-teilnehmer.png` |
| m8 Turnier Konfiguration | `ptm:konfiguration_turnier` | `toolbar-konfiguration.png` |
| m10 Turnieransicht (Kiosk) | `ptm:turnier_modus` | `toolbar-turnier-modus.png` |
| m12 Webserver starten | `ptm:webserver_starten` | `toolbar-webserver-starten.png` |
| m13 Webserver stoppen | `ptm:webserver_stoppen` | `toolbar-webserver-stoppen.png` |
| m15 Neues Turnier in neuer Datei | `ptm:toolbar_neu_in_neuer_datei` | `toolbar-neu-in-neuer-datei.png` |
| m16 Öffnen | `ptm:toolbar_oeffnen` | `toolbar-oeffnen.png` |
| m17 Speichern | `.uno:Save` | *LibreOffice-Standard, nicht eigenes Icon* |
| m18 Drucken | `ptm:toolbar_drucken` | `toolbar-drucken.png` |
| m19 Druckvorschau | `ptm:toolbar_druckvorschau` | `toolbar-druckvorschau.png` |
| m20 Als PDF exportieren | `.uno:ExportToPDF` | *LibreOffice-Standard* |
| m21 Druckbereich | `.uno:DefinePrintArea` | *LibreOffice-Standard* |
| m23 Spieler-DB → Meldungen | `ptm:spielerdb_in_meldeliste` | `toolbar-spielerdb-meldungen.png` |
| m25 Verarbeitung abbrechen | `ptm:abbruch` | `toolbar-abbruch.png` |

### Spieltag-Toolbar (`Addons_Z3_SpieltagToolbar.xcu`)

| Schaltfläche | Code-URL | Icon-Datei |
|--------------|----------|------------|
| m1 Nächster Spieltag | `ptm:toolbar_naechster_spieltag` | `toolbar-naechster-spieltag.png` |
| m2 Gesamtrangliste | `ptm:toolbar_gesamtrangliste` | `toolbar-gesamtrangliste.png` |

### Timer-Toolbar (`Addons_Z4_TimerToolbar.xcu`)

Siehe oben — fünf neue Icons.

## Drop-In Installation ins Repo

```bash
cd /pfad/zum/Petanque-Turnier-Manager
cp icons_v2/png/64/*.png images/
# oder für mehrere Größen (LO wählt automatisch die passende):
cp icons_v2/png/32/*.png images/
```

Die XCU-Dateien referenzieren nur `%origin%/images/toolbar-XYZ.png` ohne Größenangabe — LibreOffice nimmt die Datei wie sie ist und skaliert sie. **Empfehlung:** PNG-Größe `64×64` ist ein guter Default; bei HiDPI-Displays ist die Skalierung weniger sichtbar.

Keine XCU-Anpassungen nötig — die Dateinamen sind identisch.

## Design-System

Einheitlich über alle 23 Icons:

- **Format:** SVG mit `viewBox="0 0 64 64"`, Stroke 1.5 px, Radius 3 px
- **Farben:**
  - Aktion / Start / OK: `#16A34A` (Grün)
  - Stopp / Abbruch / Fehler: `#DC2626` (Rot)
  - Information / Konfiguration: `#2563EB` (Blau)
  - Auszeichnung / Akzent: `#FBBF24` (Gold) und `#F59E0B` (Cochonnet-Orange)
  - Neutral / Werkzeug: `#B0B7C0` (Boule-Silber)
  - Outline: `#1F2937` (Dunkelgrau)
- **Petanque-spezifische Elemente:** Boule-Kugel mit Rillen als wiederverwendbares Atom (in `toolbar-start` und `toolbar-neu-in-neuer-datei`).
- **Geteilte Basen:** Alle Timer-Icons nutzen dieselbe Stoppuhr; nur das Badge unten rechts wechselt.

## Verzeichnisstruktur im ZIP

```
icons_v2/
├── svg/                 ← Quelldateien
├── png/{16,24,32,48,64,128}/
├── build_icons_v2.py    ← SVG-Generator (Farben zentral im C-dict)
├── render_v2.py         ← PNG-Renderer + Preview-Generator
├── preview-overview-v2.png
├── preview-vorher-nachher.png
└── README.md            ← diese Datei
```

Änderungen am Design zentral in `build_icons_v2.py` im `C`-Dictionary; danach `python3 build_icons_v2.py && python3 render_v2.py`.

Abhängigkeiten: `cairosvg`, `Pillow`.

## Sidebar-Deck-Icon (separat)

Das Sidebar-Deck-Tab-Icon ist **nicht** Teil dieses SVG-Sets. Es wird aus dem Logo-Master
`images/petanqueturniermanager-logo.png` passgenau auf 24×24px gerendert (Design-Größe der
LO-Sidebar-TabBar), damit LibreOffice das Icon nicht zur Laufzeit skalieren muss:

```bash
python3 images/render_logo.py   # → images/petanqueturniermanager-logo-sidebar-24px.png
```

Abhängigkeit: `Pillow`. Eingebunden in `registry/org/openoffice/Office/UI/Sidebar.xcu` (`IconURL`).

## Sidebar-Info-Panel-Icons (separat)

Das Info-Panel (`InfoSidebarContent`) zeigt Status-Icons (Fortschritt, Timer, Webserver) in
20×20px-Controls. Damit LibreOffice die früher geladenen 128px-/648×730px-Master nicht zur
Laufzeit herunterskalieren muss, werden passgenaue 20×20-PNGs nach `images/sidebar/` gerendert:

```bash
python3 images/render_sidebar_icons.py   # → images/sidebar/*-20px.png
```

Abhängigkeit: `Pillow`. Quellen: die 128px-Toolbar-Master (Timer/Webserver) bzw. der
`sidebar-fortschritt.png`-Master. Referenziert in `InfoSidebarContent` über
`getImageUrlDir() + "sidebar/..."`.
