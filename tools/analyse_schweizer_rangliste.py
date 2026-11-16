#!/usr/bin/env python3
"""
Konsistenz-Analyse für eine Schweizer/Maastrichter-Rangliste-ODS.

Liest direkt aus der ODS (content.xml + meta.xml) und prüft die
Rangliste-Werte gegen die Spielrunden-Sheets nach:

  1. Siege, Punkte+, Punkte-, Punktedifferenz — aufsummiert aus allen
     Spielrunden-Sheets ("N. Spielrunde"), inkl. Freilos-Buchung
     (konfigurierte "Freispiel Punkte +/-", Defaults 13 / 7 aus
     SchweizerPropertiesSpalte.java).
  2. Buchholz (BHZ) = Summe der Siege aller Gegner. Ungespielte
     Paarungen (Ergebnis 0:0) zählen dabei NICHT als Gegner
     (SchweizerRanglisteSheet.leseRundeEin).
  3. Feinbuchholz (FBHZ) = Summe der BHZ-Werte aller Gegner.
     Freilos-Buchholz (Property "Schweizer Freilos Buchholz", fehlend = NULL):
     NULL = Freilos liefert 0 BHZ/FBHZ; NEUTRAL = je Freilos BHZ (r-1)//2 (abgerundet) und
     FBHZ eigene Siege + (r-1)*r/2, r = Spielrunden mit mindestens einem
     eingetragenen Ergebnis; Freilose zählen nur in solchen Runden
     (FreilosBuchholz.java, 03_Schweizer.md 5.1). BHZ/FBHZ sind ganzzahlig.
  4. Rangliste-Sortierung: Siege ↓ → BHZ ↓ → FBHZ ↓ → Punktediff ↓ →
     Punkte+ ↓ (SchweizerSystem.sortiereNachAuswertungskriterien).
     Der Ranking-Modus wird aus der User-Defined-Property "Schweizer Ranking
     Modus" in meta.xml gelesen (Default MIT_BUCHHOLZ). Im Modus
     OHNE_BUCHHOLZ entfallen BHZ/FBHZ sowohl im Wertvergleich als auch im
     Sortierschlüssel (Siege ↓ → Punktediff ↓ → Punkte+ ↓), da die
     Produktionslogik dort 0 in die BHZ/FBHZ-Spalten schreibt
     (SchweizerRanglisteSheet.java).

  5. Platz-Spalte (geteilter Platz bei Gleichstand) und FL-Spalte (Freilos-Runden).
  6. Auslosung: Vollständigkeit, Freilos-Ergebnis, Gegnerwiederholungen; ab Runde 2
     exakte Nachbildung von SchweizerSystem.weitereRunde (Freilos, Sieggruppen,
     Floater, WiederholungsarmePaarung) und Vergleich Paarung für Paarung.

Funktioniert für beide Systeme, die dieselbe Rangliste-Logik verwenden:
  - Schweizer System: Sheet "Rangliste"
  - Maastrichter (Vorrunde): Sheet "Vorrunden-Rangliste"
Das passende Sheet wird automatisch erkannt.

Team-Spalten in den Spielrunden-Sheets enthalten je nach Property "Spielplan
Team Anzeige" die Team-Nummer, den Teamnamen oder die Spielernamen. Namen
werden über das "Meldeliste"-Sheet zur Team-Nr aufgelöst — analog
SchweizerListeDelegate.erstelleTeamAnzeigeIndex/TeamAnzeigeFormatierer.

Konfiguration ("Freispiel Punkte +/-") wird aus den ODS-Document-Properties
gelesen (meta.xml); fehlt sie, gelten die Defaults 13 / 7.

Verwendung:
    python3 tools/analyse_schweizer_rangliste.py <pfad/zur/datei.ods>
    python3 tools/analyse_schweizer_rangliste.py <pfad/zur/datei.ods> --bis-runde 2

Annahmen über das Sheet-Layout sind aus SchweizerRanglisteSheet.java /
SchweizerAbstractSpielrundeSheet.java übernommen und an einer Stelle im
Skript zentralisiert — bei Layout-Änderungen in der Extension nachziehen.
"""
import argparse
import re
import sys
import xml.etree.ElementTree as ET
import zipfile
from collections import defaultdict

NS = {
    'table':  'urn:oasis:names:tc:opendocument:xmlns:table:1.0',
    'office': 'urn:oasis:names:tc:opendocument:xmlns:office:1.0',
    'text':   'urn:oasis:names:tc:opendocument:xmlns:text:1.0',
}
T = '{%s}' % NS['table']
O = '{%s}' % NS['office']
TXT = '{%s}' % NS['text']

# Defaults aus SchweizerPropertiesSpalte.java (Stand 2026)
DEFAULT_FREISPIEL_PUNKTE_PLUS = 13
DEFAULT_FREISPIEL_PUNKTE_MINUS = 7
DEFAULT_RANKING_MODUS = 'MIT_BUCHHOLZ'
RANKING_MODUS_KEY = 'Schweizer Ranking Modus'
RANKING_MODUS_OHNE_BUCHHOLZ = 'OHNE_BUCHHOLZ'
FREILOS_BUCHHOLZ_KEY = 'Schweizer Freilos Buchholz'
DEFAULT_FREILOS_BUCHHOLZ = 'NULL'
FREILOS_BUCHHOLZ_NEUTRAL = 'NEUTRAL'
TOLERANZ = 1e-9

# Layout-Konstanten (SchweizerAbstractSpielrundeSheet.java / SchweizerRanglisteSheet.java)
SR_ERSTE_DATEN_ZEILE = 2
SR_TEAM_A_SPALTE = 1
SR_TEAM_B_SPALTE = 2
SR_ERG_TEAM_A_SPALTE = 3
SR_ERG_TEAM_B_SPALTE = 4

ML_SHEET_NAME = 'Meldeliste'
ML_ERSTE_DATEN_ZEILE = 3
ML_TEAM_NR_SPALTE = 0
ML_TEAMNAME_SPALTE = 1

RL_ERSTE_DATEN_ZEILE = 2
RL_TEAM_NR_SPALTE = 0
RL_TEAM_NAME_SPALTE = 1
RL_PLATZ_SPALTE = 2
RL_SIEGE_SPALTE = 3
RL_BHZ_SPALTE = 4
RL_FBHZ_SPALTE = 5
RL_PUNKTE_PLUS_SPALTE = 6
RL_PUNKTE_MINUS_SPALTE = 7
RL_PUNKTE_DIFF_SPALTE = 8
RL_FREILOS_UEBERSCHRIFT = 'FL'

RANGLISTE_SHEET_NAMEN = ('Rangliste', 'Vorrunden-Rangliste')


# Obergrenze für wiederholte Leerzeilen: LO speichert den Rest des Blatts als eine Zeile mit
# number-rows-repeated ≈ 1 Mio. – voll ausgerollt sprengt das den Speicher.
MAX_LEERZEILEN_WIEDERHOLUNG = 100


def cells_of_row(row):
    out = []
    for c in list(row):
        if c.tag not in (f'{T}table-cell', f'{T}covered-table-cell'):
            continue
        rep = int(c.get(f'{T}number-columns-repeated', '1'))
        val = c.get(f'{O}value')
        if val is None:
            ps = c.findall(f'{TXT}p')
            txt = '\n'.join(''.join(p.itertext()) for p in ps).strip()
            val = txt if txt else None
        out.extend([val] * rep)
    while out and out[-1] is None:  # leere Restspalten (bis zu 16384) nicht mitschleppen
        out.pop()
    return out


def sheet_rows(table):
    rows = []
    for r in table.iter(f'{T}table-row'):
        rep = int(r.get(f'{T}number-rows-repeated', '1'))
        cells = cells_of_row(r)
        if not cells:
            rep = min(rep, MAX_LEERZEILEN_WIEDERHOLUNG)
        for _ in range(rep):
            rows.append(list(cells))
    return rows


def find_sheet(root, name):
    for t in root.iter(f'{T}table'):
        if t.get(f'{T}name') == name:
            return t
    return None


def to_int(v):
    if v is None:
        return None
    try:
        return int(float(v))
    except (TypeError, ValueError):
        return None


def to_float(v):
    if v is None:
        return None
    try:
        return float(v)
    except (TypeError, ValueError):
        return None


def lese_konfig(meta_xml: str, key: str, default: int) -> int:
    """Liest eine numerische User-Defined-Property aus meta.xml; fallback default."""
    pat = re.compile(r'meta:name="' + re.escape(key) + r'"[^>]*>([^<]*)<')
    m = pat.search(meta_xml)
    if not m:
        return default
    v = to_int(m.group(1))
    return v if v is not None else default


def lese_konfig_str(meta_xml: str, key: str, default: str) -> str:
    """Liest eine String-User-Defined-Property aus meta.xml; fallback default."""
    pat = re.compile(r'meta:name="' + re.escape(key) + r'"[^>]*>([^<]*)<')
    m = pat.search(meta_xml)
    if not m:
        return default
    v = m.group(1).strip()
    return v if v else default


def lade_ods(pfad):
    with zipfile.ZipFile(pfad) as zf:
        content = ET.fromstring(zf.read('content.xml'))
        meta = zf.read('meta.xml').decode('utf-8')
    return content, meta


def finde_rangliste_sheet(root):
    for name in RANGLISTE_SHEET_NAMEN:
        table = find_sheet(root, name)
        if table is not None:
            return name, table
    return None, None


def finde_spielrunden(root, bis_runde=None):
    """Findet alle Sheets 'N. Spielrunde', sortiert nach N."""
    runden = []
    for t in root.iter(f'{T}table'):
        name = t.get(f'{T}name', '')
        m = re.match(r'^(\d+)\. Spielrunde$', name)
        if not m:
            continue
        nr = int(m.group(1))
        if bis_runde is not None and nr > bis_runde:
            continue
        runden.append((nr, name))
    runden.sort()
    return runden


FORMATION_ANZ_SPIELER = {'TETE': 1, 'DOUBLETTE': 2, 'TRIPLETTE': 3, 'MELEE': 1,
                         'NUR_TEAMNAME': 0}  # Formation.java
SPIELER_TRENNER = ' / '


def spieler_anzeige(vorname, nachname, verein):
    """Wie TeamAnzeigeFormatierer.spielerAnzeige."""
    name = ' '.join(x for x in (vorname, nachname) if x)
    return f'{name} ({verein})' if name and verein else name


def team_anzeige_spielernamen(zeile, erste_spalte, anz_spieler, mit_verein):
    """Wie TeamAnzeigeFormatierer.formatiere(false, ...): 'Vorname Nachname / Vorname Nachname'."""
    def wert(i):
        return (zeile[i] or '').strip() if i < len(zeile) else ''
    breite = 3 if mit_verein else 2
    namen = []
    for spieler in range(anz_spieler):
        sp = erste_spalte + spieler * breite
        n = spieler_anzeige(wert(sp), wert(sp + 1), wert(sp + 2) if mit_verein else '')
        if n:
            namen.append(n)
    return SPIELER_TRENNER.join(namen)


def lese_meldeliste_namen(root, meta_xml):
    """Liest Anzeige-Kennung -> Team-Nr aus dem Meldeliste-Sheet.

    Analog SchweizerListeDelegate.erstelleTeamAnzeigeIndex: indiziert den Teamnamen
    (falls "Meldeliste Teamname" aktiv) UND die zusammengesetzten Spielernamen, damit
    sowohl "Spielplan Team Anzeige" = NAME als auch = SPIELERNAMEN auflösbar ist.
    """
    table = find_sheet(root, ML_SHEET_NAME)
    if table is None:
        return {}
    teamname_aktiv = lese_konfig_str(meta_xml, 'Meldeliste Teamname', 'N') == 'J'
    mit_verein = lese_konfig_str(meta_xml, 'Meldeliste Vereinsname', 'N') == 'J'
    formation = lese_konfig_str(meta_xml, 'Meldeliste Formation', 'DOUBLETTE')
    anz_spieler = FORMATION_ANZ_SPIELER.get(formation, 2)
    erste_spieler_spalte = ML_TEAMNAME_SPALTE + (1 if teamname_aktiv else 0)

    rows = sheet_rows(table)
    name_zu_nr = {}
    for z in range(ML_ERSTE_DATEN_ZEILE, len(rows)):
        r = rows[z]
        nr = to_int(r[ML_TEAM_NR_SPALTE]) if len(r) > ML_TEAM_NR_SPALTE else None
        if nr is None or nr <= 0:
            continue
        if teamname_aktiv and len(r) > ML_TEAMNAME_SPALTE and r[ML_TEAMNAME_SPALTE]:
            name_zu_nr[r[ML_TEAMNAME_SPALTE].strip()] = nr
        spieler = team_anzeige_spielernamen(r, erste_spieler_spalte, anz_spieler, mit_verein)
        if spieler:
            name_zu_nr[spieler] = nr
    return name_zu_nr


def resolve_team_nr(zelle, name_zu_nr):
    """Löst eine Team-Spalten-Zelle (Nummer, Teamname oder Spielernamen) zur Team-Nr auf.

    Gibt None nur für eine leere Zelle zurück (echtes Zeilenende/Freilos). Ein nicht
    auflösbarer Name wird als Warnung ausgegeben statt still als Freilos missinterpretiert.
    """
    nr = to_int(zelle)
    if nr is not None:
        return nr
    if not zelle:
        return None
    nr = name_zu_nr.get(zelle.strip())
    if nr is None:
        print(f'  WARNUNG: Team "{zelle}" nicht in Meldeliste auflösbar', file=sys.stderr)
    return nr


NICHT_GEWERTET_PRAEFIX = '__PTM_NICHT_GEWERTET_'


def nicht_gewertete_sheets(root):
    """Sheet-Namen mit blattgebundenem Named Range __PTM_NICHT_GEWERTET_* (RundenWertungHelper)."""
    namen = set()
    for t in root.iter(f'{T}table'):
        for nr in t.iter(f'{T}named-range'):
            if nr.get(f'{T}name', '').startswith(NICHT_GEWERTET_PRAEFIX):
                namen.add(t.get(f'{T}name'))
    return namen


def lese_runden(root, spielrunden, name_zu_nr):
    """Liest je gewertete Spielrunde die Paarungen (nrA, nrB, ergA, ergB, Zeile) in Sheet-Reihenfolge
    und die Freilos-Teams. Nicht gewertete Runden werden übersprungen (wie SchweizerRundenLeser)."""
    ausgeschlossen = nicht_gewertete_sheets(root)
    runden = []
    for nr, sname in spielrunden:
        if sname in ausgeschlossen:
            print(f'  Hinweis: {sname} ist "nicht gewertet" und wird übersprungen')
            continue
        rows = sheet_rows(find_sheet(root, sname))
        paare, freilose, freilos_ergebnisse = [], [], []
        for z in range(SR_ERSTE_DATEN_ZEILE, len(rows)):
            r = rows[z]
            if len(r) <= SR_TEAM_A_SPALTE:
                break
            nr_a = resolve_team_nr(r[SR_TEAM_A_SPALTE], name_zu_nr)
            if nr_a is None or nr_a <= 0:
                break  # Ende der Daten
            nr_b = resolve_team_nr(r[SR_TEAM_B_SPALTE], name_zu_nr) if len(r) > SR_TEAM_B_SPALTE else None
            erg_a = (to_int(r[SR_ERG_TEAM_A_SPALTE]) if len(r) > SR_ERG_TEAM_A_SPALTE else None) or 0
            erg_b = (to_int(r[SR_ERG_TEAM_B_SPALTE]) if len(r) > SR_ERG_TEAM_B_SPALTE else None) or 0
            if nr_b is None or nr_b <= 0:
                freilose.append(nr_a)
                freilos_ergebnisse.append((nr_a, erg_a, erg_b))
            else:
                paare.append((nr_a, nr_b, erg_a, erg_b, z + 1))
        runden.append({'nr': nr, 'name': sname, 'paare': paare, 'freilose': freilose,
                       'freilos_ergebnisse': freilos_ergebnisse})
    return runden


def lese_spielergebnisse(runden, freispiel_plus, freispiel_minus):
    """Aggregiert je Team: Siege, Punkte+, Punkte-, Gegner-Liste (nur gespielte Paarungen),
    Freilose (nur in Runden mit Ergebnis), Freilos-Runden (alle) sowie die Anzahl der Runden
    mit Ergebnis (r). Entspricht SchweizerRundenLeser.leseErgebnisse."""
    siege = defaultdict(int)
    punkte_plus = defaultdict(int)
    punkte_minus = defaultdict(int)
    gegner = defaultdict(list)
    freilose = defaultdict(int)
    freilos_runden = defaultdict(list)
    anz_runden_mit_ergebnis = 0

    for runde in runden:
        for nr_a in runde['freilose']:
            siege[nr_a] += 1
            punkte_plus[nr_a] += freispiel_plus
            punkte_minus[nr_a] += freispiel_minus
            freilos_runden[nr_a].append(runde['nr'])
        hat_ergebnis = False
        for nr_a, nr_b, erg_a, erg_b, _ in runde['paare']:
            punkte_plus[nr_a] += 0  # Team auch ohne Ergebnis im Teamuniversum
            punkte_plus[nr_b] += 0
            if erg_a > 0 or erg_b > 0:
                hat_ergebnis = True
                gegner[nr_a].append(nr_b)
                gegner[nr_b].append(nr_a)
                punkte_plus[nr_a] += erg_a
                punkte_minus[nr_a] += erg_b
                punkte_plus[nr_b] += erg_b
                punkte_minus[nr_b] += erg_a
                if erg_a > erg_b:
                    siege[nr_a] += 1
                elif erg_b > erg_a:
                    siege[nr_b] += 1
        if hat_ergebnis:
            anz_runden_mit_ergebnis += 1
            for nr in runde['freilose']:
                freilose[nr] += 1

    return siege, punkte_plus, punkte_minus, gegner, freilose, freilos_runden, anz_runden_mit_ergebnis


def freilos_bhz_beitrag(modus, r):
    return (r - 1) // 2 if modus == FREILOS_BUCHHOLZ_NEUTRAL and r > 0 else 0


def freilos_fbhz_beitrag(modus, r, eigene_siege):
    return eigene_siege + (r - 1) * r // 2 if modus == FREILOS_BUCHHOLZ_NEUTRAL and r > 0 else 0


def berechne_buchholz(teams, siege, gegner, freilose, modus, r):
    return {nr: sum(siege.get(g, 0) for g in gegner.get(nr, []))
            + freilose.get(nr, 0) * freilos_bhz_beitrag(modus, r) for nr in teams}


def berechne_feinbuchholz(teams, bhz, gegner, siege, freilose, modus, r):
    return {nr: sum(bhz.get(g, 0) for g in gegner.get(nr, []))
            + freilose.get(nr, 0) * freilos_fbhz_beitrag(modus, r, siege.get(nr, 0)) for nr in teams}


def gleich(a, b):
    return a is not None and b is not None and abs(a - b) < TOLERANZ


def finde_freilos_spalte(rows):
    """FL-Spalte über die Überschrift in Zeile 0, Fallback: direkt hinter der Punktedifferenz."""
    kopf = rows[0] if rows else []
    for i, wert in enumerate(kopf):
        if wert and wert.strip() == RL_FREILOS_UEBERSCHRIFT:
            return i
    return RL_PUNKTE_DIFF_SPALTE + 1


def lese_rangliste(rl_table):
    rows = sheet_rows(rl_table)
    fl_spalte = finde_freilos_spalte(rows)
    daten = []
    for i in range(RL_ERSTE_DATEN_ZEILE, len(rows)):
        r = rows[i]
        if len(r) <= RL_TEAM_NR_SPALTE:
            break
        nr = to_int(r[RL_TEAM_NR_SPALTE])
        if nr is None or nr <= 0:
            break
        daten.append({
            'rang': i - RL_ERSTE_DATEN_ZEILE + 1,
            'nr': nr,
            'name': r[RL_TEAM_NAME_SPALTE] if len(r) > RL_TEAM_NAME_SPALTE else '',
            'siege': to_int(r[RL_SIEGE_SPALTE]) if len(r) > RL_SIEGE_SPALTE else None,
            'bhz':   to_float(r[RL_BHZ_SPALTE]) if len(r) > RL_BHZ_SPALTE else None,
            'fbhz':  to_float(r[RL_FBHZ_SPALTE]) if len(r) > RL_FBHZ_SPALTE else None,
            'pp':    to_int(r[RL_PUNKTE_PLUS_SPALTE]) if len(r) > RL_PUNKTE_PLUS_SPALTE else None,
            'pm':    to_int(r[RL_PUNKTE_MINUS_SPALTE]) if len(r) > RL_PUNKTE_MINUS_SPALTE else None,
            'diff':  to_int(r[RL_PUNKTE_DIFF_SPALTE]) if len(r) > RL_PUNKTE_DIFF_SPALTE else None,
            'platz': to_int(r[RL_PLATZ_SPALTE]) if len(r) > RL_PLATZ_SPALTE else None,
            'fl':    (r[fl_spalte] or '') if len(r) > fl_spalte else '',
        })
    return daten


# ---------------------------------------------------------------------------
# Auslosung nachbilden (SchweizerSystem.weitereRunde + WiederholungsarmePaarung)
# ---------------------------------------------------------------------------

def augmentiere(wurzel, erlaubt, partner, aktiv):
    """Port von WiederholungsarmePaarung.augmentiere (Edmonds-Blüten, augmentierender Pfad)."""
    n = len(partner)
    besucht = [False] * n
    vorgaenger = [-1] * n
    basis = list(range(n))
    warteschlange = [wurzel]
    besucht[wurzel] = True
    kopf = 0

    def gemeinsame_basis(a, b):
        gesehen = [False] * n
        while True:
            a = basis[a]
            gesehen[a] = True
            if partner[a] < 0:
                break
            a = vorgaenger[partner[a]]
        while True:
            b = basis[b]
            if gesehen[b]:
                return b
            b = vorgaenger[partner[b]]

    def markiere_pfad(v, bluete_basis, kind, bluete):
        while basis[v] != bluete_basis:
            bluete[basis[v]] = True
            bluete[basis[partner[v]]] = True
            vorgaenger[v] = kind
            kind = partner[v]
            v = vorgaenger[partner[v]]

    while kopf < len(warteschlange):
        v = warteschlange[kopf]
        kopf += 1
        for zu in range(n):
            if not aktiv[zu] or not erlaubt[v][zu] or basis[v] == basis[zu] or partner[v] == zu:
                continue
            if zu == wurzel or (partner[zu] >= 0 and vorgaenger[partner[zu]] >= 0):
                bb = gemeinsame_basis(v, zu)
                bluete = [False] * n
                markiere_pfad(v, bb, zu, bluete)
                markiere_pfad(zu, bb, v, bluete)
                for i in range(n):
                    if aktiv[i] and bluete[basis[i]]:
                        basis[i] = bb
                        if not besucht[i]:
                            besucht[i] = True
                            warteschlange.append(i)
            elif vorgaenger[zu] < 0:
                vorgaenger[zu] = v
                if partner[zu] < 0:
                    u = zu
                    while u >= 0:
                        pv = vorgaenger[u]
                        naechster = partner[pv]
                        partner[u] = pv
                        partner[pv] = u
                        u = naechster
                    return True
                besucht[partner[zu]] = True
                warteschlange.append(partner[zu])
    return False


def wiederholungsarm_paaren(anzahl, gespielt):
    """Port von WiederholungsarmePaarung.paare(..., NAECHSTER_IN_RANGLISTE).
    Liefert [(indexA, indexB, wiederholung)] nach besser platziertem Team aufsteigend."""
    erlaubt = [[a != b and not gespielt(a, b) and not gespielt(b, a) for b in range(anzahl)]
               for a in range(anzahl)]
    aktiv = [True] * anzahl
    partner = [-1] * anzahl
    for a in range(anzahl):
        for b in range(a + 1, anzahl):
            if partner[a] >= 0:
                break
            if partner[b] < 0 and erlaubt[a][b]:
                partner[a], partner[b] = b, a
    for i in range(anzahl):
        if partner[i] < 0:
            augmentiere(i, erlaubt, partner, aktiv)
    groesse = sum(1 for p in partner if p >= 0) // 2

    paare = []
    for team in range(anzahl):
        if not aktiv[team]:
            continue
        aktiv[team] = False
        for gegner in range(team + 1, anzahl):
            if not aktiv[gegner]:
                continue
            neu = list(partner)
            neue_groesse = groesse
            ziel = groesse - (1 if erlaubt[team][gegner] else 0)
            aktiv[gegner] = False
            for entfernt in (team, gegner):
                if neu[entfernt] >= 0:
                    neu[neu[entfernt]] = -1
                    neu[entfernt] = -1
                    neue_groesse -= 1
            frei = 0
            while frei < anzahl and neue_groesse < ziel:
                if aktiv[frei] and neu[frei] < 0 and augmentiere(frei, erlaubt, neu, aktiv):
                    neue_groesse += 1
                frei += 1
            if neue_groesse == ziel:
                partner, groesse = neu, neue_groesse
                paare.append((team, gegner, not erlaubt[team][gegner]))
                break
            aktiv[gegner] = True
    return paare


def stand_vor_runde(runden_davor, teams, freispiel_plus, freispiel_minus, freilos_modus, ohne_buchholz):
    """Stand für die Auslosung wie SchweizerAbstractSpielrundeSheet.gespieltenRundenEinlesen:
    Gegner zählen auch ohne Ergebnis, Herunterlos-Chronik inkl. Freilos (HerunterlosChronik)."""
    siege = {t: 0 for t in teams}
    diff = {t: 0 for t in teams}
    plus = {t: 0 for t in teams}
    gegner = {t: [] for t in teams}
    chronik = {t: [] for t in teams}
    hatte_freilos = set()
    freilose = defaultdict(int)
    r_mit_ergebnis = 0
    for runde in runden_davor:
        vor = dict(siege)
        hat_ergebnis = False
        for t in runde['freilose']:
            hatte_freilos.add(t)
            siege[t] += 1
            diff[t] += freispiel_plus - freispiel_minus
            plus[t] += freispiel_plus
            if runde['nr'] not in chronik[t]:
                chronik[t].append(runde['nr'])
        for a, b, ea, eb, _ in runde['paare']:
            gegner[a].append(b)
            gegner[b].append(a)
            for oben, unten in ((a, b), (b, a)):
                if vor[oben] > vor[unten] and runde['nr'] not in chronik[oben]:
                    chronik[oben].append(runde['nr'])
            hat_ergebnis |= ea > 0 or eb > 0
            diff[a] += ea - eb
            diff[b] += eb - ea
            plus[a] += ea
            plus[b] += eb
            if ea > eb:
                siege[a] += 1
            elif eb > ea:
                siege[b] += 1
        if hat_ergebnis:
            r_mit_ergebnis += 1
            for t in runde['freilose']:
                freilose[t] += 1
    if ohne_buchholz:
        bhz = fbhz = {t: 0 for t in teams}
    else:
        bhz = berechne_buchholz(teams, siege, gegner, freilose, freilos_modus, r_mit_ergebnis)
        fbhz = berechne_feinbuchholz(teams, bhz, gegner, siege, freilose, freilos_modus, r_mit_ergebnis)
    reihenfolge = sorted(teams, key=lambda t: (-siege[t], -bhz[t], -fbhz[t], -diff[t], -plus[t], t))
    return reihenfolge, siege, gegner, chronik, hatte_freilos


def waehle_floater(kandidaten, chronik, neue_runde):
    """Port von SchweizerSystem.waehleFloater."""
    def vorrang(pos):
        runden = chronik[kandidaten[pos]]
        return (neue_runde - 1 in runden, neue_runde - 2 in runden, len(runden), -pos)
    return kandidaten[min(range(len(kandidaten)), key=vorrang)]


def erwartete_auslosung(reihenfolge, siege, gegner, chronik, hatte_freilos, neue_runde):
    """Port von SchweizerSystem.weitereRunde: Freilos, Sieggruppen mit Floater, Paarung."""
    freilos = None
    rest = list(reihenfolge)
    if len(rest) % 2 == 1:
        freilos = next((t for t in reversed(rest) if t not in hatte_freilos), rest[-1])
        rest.remove(freilos)
    gruppen = []
    for t in rest:
        if not gruppen or siege[t] != siege[gruppen[-1][0]]:
            gruppen.append([])
        gruppen[-1].append(t)
    eingehender_floater = False
    for i in range(len(gruppen) - 1):
        gruppe = gruppen[i]
        floatet = len(gruppe) % 2 == 1
        if floatet:
            kandidaten = gruppe[1:] if eingehender_floater and len(gruppe) > 1 else gruppe
            floater = waehle_floater(kandidaten, chronik, neue_runde)
            gruppe.remove(floater)
            gruppen[i + 1].insert(0, floater)
        eingehender_floater = floatet
    reihe = [t for g in gruppen for t in g]
    paare = wiederholungsarm_paaren(len(reihe), lambda a, b: reihe[b] in gegner[reihe[a]])
    return freilos, [(reihe[a], reihe[b], w) for a, b, w in paare], [g for g in gruppen if g]


def pruefe_auslosung(runden, teams, freispiel_plus, freispiel_minus, freilos_modus, ohne_buchholz):
    """Prüft jede Runde: Vollständigkeit, Unentschieden, Freilos-Ergebnis, Gegnerwiederholungen und
    (ab Runde 2) Paarungen/Freilos gegen die nachgebildete Auslosung. Liefert die Fehlerzahl."""
    fehler = 0

    def melde(text):
        nonlocal fehler
        fehler += 1
        print(f'  ❌ {text}')

    bisher = set()
    for idx, runde in enumerate(runden):
        nr = runde['nr']
        im_blatt = [t for a, b, *_ in runde['paare'] for t in (a, b)] + runde['freilose']
        doppelt = sorted({t for t in im_blatt if im_blatt.count(t) > 1})
        fehlend = sorted(set(teams) - set(im_blatt))
        if doppelt or fehlend:
            melde(f'Runde {nr}: doppelt {doppelt}, fehlend {fehlend}')
        for t, ea, eb in runde['freilos_ergebnisse']:
            if (ea, eb) != (freispiel_plus, freispiel_minus):
                melde(f'Runde {nr}: Freilos Team {t} mit {ea}:{eb} statt {freispiel_plus}:{freispiel_minus}')
        for a, b, ea, eb, zeile in runde['paare']:
            if ea == eb and ea > 0:
                melde(f'Runde {nr} Zeile {zeile}: Unentschieden {a}–{b} {ea}:{eb}')
            if frozenset((a, b)) in bisher:
                print(f'  ⚠ Runde {nr}: Gegnerwiederholung {a}–{b}')
        bisher |= {frozenset((a, b)) for a, b, *_ in runde['paare']}

        if idx == 0:
            print(f'  Runde {nr}: zufällige Erstrunde – nur Vollständigkeit geprüft, Freilos {runde["freilose"]}')
            continue
        reihenfolge, siege, gegner, chronik, hatte_freilos = stand_vor_runde(
            runden[:idx], teams, freispiel_plus, freispiel_minus, freilos_modus, ohne_buchholz)
        freilos, paare, gruppen = erwartete_auslosung(reihenfolge, siege, gegner, chronik, hatte_freilos, nr)
        ist = [(a, b) for a, b, *_ in runde['paare']]
        soll = [(a, b) for a, b, _ in paare]
        ist_freilos = runde['freilose'][0] if runde['freilose'] else None
        wiederholungen = sum(1 for *_, w in paare if w)
        if ist == soll and ist_freilos == freilos:
            print(f'  ✓ Runde {nr}: Auslosung identisch (Freilos {freilos}, '
                  f'{wiederholungen} unvermeidbare Gegnerwiederholung(en))')
        else:
            if ist_freilos != freilos:
                melde(f'Runde {nr}: Freilos Team {ist_freilos}, erwartet {freilos}')
            if sorted(map(frozenset, ist)) != sorted(map(frozenset, soll)):
                melde(f'Runde {nr}: Paarungen weichen ab\n      Sieggruppen: '
                      + ' | '.join(','.join(f'{t}({siege[t]})' for t in g) for g in gruppen)
                      + f'\n      erwartet: {soll}\n      im Blatt: {ist}')
            elif ist != soll:
                melde(f'Runde {nr}: Paarungen gleich, aber Reihenfolge/Seiten abweichend\n'
                      f'      erwartet: {soll}\n      im Blatt: {ist}')
    return fehler


def main():
    p = argparse.ArgumentParser(
        description='Konsistenz-Analyse einer Schweizer/Maastrichter-Rangliste-ODS '
                     '(Siege/BHZ/FBHZ/Punkte).')
    p.add_argument('ods', help='Pfad zur ODS-Datei')
    p.add_argument('--bis-runde', type=int, default=None,
                    help='nur Spielrunden bis einschließlich dieser Nummer berücksichtigen (default: alle)')
    args = p.parse_args()

    root, meta_xml = lade_ods(args.ods)

    rl_name, rl_table = finde_rangliste_sheet(root)
    if rl_table is None:
        print(f'Kein Rangliste-Sheet gefunden (gesucht: {RANGLISTE_SHEET_NAMEN})')
        return 1

    spielrunden = finde_spielrunden(root, args.bis_runde)
    if not spielrunden:
        print('Keine Spielrunden-Sheets ("N. Spielrunde") gefunden')
        return 1

    freispiel_plus = lese_konfig(meta_xml, 'Freispiel Punkte +', DEFAULT_FREISPIEL_PUNKTE_PLUS)
    freispiel_minus = lese_konfig(meta_xml, 'Freispiel Punkte -', DEFAULT_FREISPIEL_PUNKTE_MINUS)
    ranking_modus = lese_konfig_str(meta_xml, RANKING_MODUS_KEY, DEFAULT_RANKING_MODUS)
    ohne_buchholz = ranking_modus == RANKING_MODUS_OHNE_BUCHHOLZ
    freilos_modus = lese_konfig_str(meta_xml, FREILOS_BUCHHOLZ_KEY, DEFAULT_FREILOS_BUCHHOLZ)

    print('=' * 72)
    print(f'  Schweizer/Maastrichter-Rangliste-Analyse: {args.ods}')
    print('=' * 72)
    print(f'  Rangliste-Sheet: {rl_name}')
    print(f'  Spielrunden: {[n for _, n in spielrunden]}')
    print(f'  Konfig "Freispiel Punkte": + {freispiel_plus}, - {freispiel_minus}')
    if lese_konfig(meta_xml, 'Freispiel Punkte +', None) is None:
        print('       (Default aus SchweizerPropertiesSpalte.java — nicht explizit in der Datei gesetzt)')
    print(f'  Ranking-Modus: {ranking_modus}' +
          (' (BHZ/FBHZ werden nicht geprüft)' if ohne_buchholz else ''))
    print(f'  Freilos-Buchholz: {freilos_modus}')
    print()

    name_zu_nr = lese_meldeliste_namen(root, meta_xml)
    runden = lese_runden(root, spielrunden, name_zu_nr)
    siege, punkte_plus, punkte_minus, gegner, freilose, freilos_runden, anz_runden = lese_spielergebnisse(
        runden, freispiel_plus, freispiel_minus)
    # Teamuniversum: nicht nur siege.keys() (Team mit 0 Siegen hätte sonst keinen
    # Eintrag) — punkte_plus wird für jedes Team gebucht, das gespielt oder ein
    # Freilos hatte (Sieger UND Verlierer).
    alle_teams = sorted(set(punkte_plus.keys()) | set(punkte_minus.keys()) | set(gegner.keys()))
    bhz = berechne_buchholz(alle_teams, siege, gegner, freilose, freilos_modus, anz_runden)
    fbhz = berechne_feinbuchholz(alle_teams, bhz, gegner, siege, freilose, freilos_modus, anz_runden)

    rangliste = lese_rangliste(rl_table)
    rl_by_nr = {rl['nr']: rl for rl in rangliste}

    print(f'--- RANGLISTE-WERTE ({rl_name}, {len(rangliste)} Teams, '
          f'{len(alle_teams)} Teams mit Spieldaten) ---')

    abweichungen = 0
    for nr in sorted(set(alle_teams) | set(rl_by_nr.keys())):
        ist = rl_by_nr.get(nr)
        name = (ist or {}).get('name') or f'#{nr}'
        if ist is None:
            print(f'  Team {nr} ({name}): fehlt in Rangliste-Sheet, hat aber Spieldaten '
                  f'(Siege={siege.get(nr, 0)})')
            abweichungen += 1
            continue

        soll_siege = siege.get(nr, 0)
        soll_bhz = bhz.get(nr, 0)
        soll_fbhz = fbhz.get(nr, 0)
        soll_pp = punkte_plus.get(nr, 0)
        soll_pm = punkte_minus.get(nr, 0)
        soll_diff = soll_pp - soll_pm

        diffs = []
        if ist['siege'] != soll_siege:
            diffs.append(f"Siege(sheet={ist['siege']},berechnet={soll_siege})")
        if not ohne_buchholz and not gleich(ist['bhz'], soll_bhz):
            diffs.append(f"BHZ(sheet={ist['bhz']},berechnet={soll_bhz})")
        if not ohne_buchholz and not gleich(ist['fbhz'], soll_fbhz):
            diffs.append(f"FBHZ(sheet={ist['fbhz']},berechnet={soll_fbhz})")
        if ist['pp'] != soll_pp:
            diffs.append(f"Punkte+(sheet={ist['pp']},berechnet={soll_pp})")
        if ist['pm'] != soll_pm:
            diffs.append(f"Punkte-(sheet={ist['pm']},berechnet={soll_pm})")
        if ist['diff'] != soll_diff:
            diffs.append(f"Diff(sheet={ist['diff']},berechnet={soll_diff})")
        soll_fl = ', '.join(map(str, freilos_runden.get(nr, [])))
        if ist['fl'] != soll_fl:
            diffs.append(f"FL(sheet='{ist['fl']}',berechnet='{soll_fl}')")

        if diffs:
            abweichungen += 1
            print(f"  ❌ Team {nr:>3} ({name}): " + ", ".join(diffs))

    if abweichungen == 0:
        print(f'  ✓ Alle {len(rangliste)} Teams stimmen exakt überein '
              f'(Siege/BHZ/FBHZ/Punkte+/Punkte-/Diff/FL)')
    else:
        print(f'\n  => {abweichungen} Team(s) mit Abweichung(en)')

    # --- Sortierprüfung ---
    # MIT_BUCHHOLZ:  Siege ↓ → BHZ ↓ → FBHZ ↓ → Punktediff ↓ → Punkte+ ↓
    # OHNE_BUCHHOLZ: Siege ↓ → Punktediff ↓ → Punkte+ ↓ (SchweizerSystem.java)
    print()
    if ohne_buchholz:
        print('--- RANGLISTE-SORTIERUNG (Siege ↓ → Punktediff ↓ → Punkte+ ↓) ---')

        def key(rl):
            return (-(rl['siege'] or 0), -(rl['diff'] or 0), -(rl['pp'] or 0))
    else:
        print('--- RANGLISTE-SORTIERUNG (Siege ↓ → BHZ ↓ → FBHZ ↓ → Punktediff ↓ → Punkte+ ↓) ---')

        def key(rl):
            return (-(rl['siege'] or 0), -(rl['bhz'] or 0), -(rl['fbhz'] or 0),
                    -(rl['diff'] or 0), -(rl['pp'] or 0))

    # Teamnummer ist kein Wertungskriterium, legt aber die Reihenfolge bei geteiltem Platz fest
    # (SchweizerSystem.sortiereNachAuswertungskriterien, 03_Schweizer.md 3.6)
    fehler_sort = 0
    for i in range(1, len(rangliste)):
        prev, cur = rangliste[i - 1], rangliste[i]
        if key(prev) + (prev['nr'],) > key(cur) + (cur['nr'],):
            fehler_sort += 1
            print(f"  ⚠ Sortierfehler bei Zeile {cur['rang']}: "
                  f"#{prev['nr']} {prev['name']} {key(prev)} vor "
                  f"#{cur['nr']} {cur['name']} {key(cur)}")
    if fehler_sort == 0:
        print(f'  ✓ Rangliste korrekt sortiert ({len(rangliste)} Teams, Gleichstand nach Teamnummer)')

    # Platz: bei Gleichstand in allen Kriterien geteilter Platz (1, 1, 3)
    fehler_platz = 0
    for i, rl in enumerate(rangliste):
        soll = rangliste[i - 1]['soll_platz'] if i > 0 and key(rangliste[i - 1]) == key(rl) else i + 1
        rl['soll_platz'] = soll
        if rl['platz'] != soll:
            fehler_platz += 1
            print(f"  ❌ Platz Team #{rl['nr']} {rl['name']}: sheet={rl['platz']}, erwartet={soll}")
    if fehler_platz == 0:
        geteilt = len(rangliste) - len({rl['platz'] for rl in rangliste})
        print(f'  ✓ Platz-Spalte korrekt ({geteilt} geteilte Plätze)')

    print()
    print('--- AUSLOSUNG (Nachbildung SchweizerSystem.weitereRunde) ---')
    fehler_auslosung = pruefe_auslosung(runden, alle_teams, freispiel_plus, freispiel_minus,
                                        freilos_modus, ohne_buchholz)
    if fehler_auslosung == 0:
        print(f'  ✓ Alle {len(runden)} Runden konsistent')

    return 1 if (abweichungen or fehler_sort or fehler_platz or fehler_auslosung) else 0


if __name__ == '__main__':
    sys.exit(main())
