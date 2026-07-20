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
  4. Rangliste-Sortierung: Siege ↓ → BHZ ↓ → FBHZ ↓ → Punktediff ↓ →
     Punkte+ ↓ (SchweizerSystem.sortiereNachAuswertungskriterien).
     Der Ranking-Modus wird aus der User-Defined-Property "Schweizer Ranking
     Modus" in meta.xml gelesen (Default MIT_BUCHHOLZ). Im Modus
     OHNE_BUCHHOLZ entfallen BHZ/FBHZ sowohl im Wertvergleich als auch im
     Sortierschlüssel (Siege ↓ → Punktediff ↓ → Punkte+ ↓), da die
     Produktionslogik dort 0 in die BHZ/FBHZ-Spalten schreibt
     (SchweizerRanglisteSheet.java).

Funktioniert für beide Systeme, die dieselbe Rangliste-Logik verwenden:
  - Schweizer System: Sheet "Rangliste"
  - Maastrichter (Vorrunde): Sheet "Vorrunden-Rangliste"
Das passende Sheet wird automatisch erkannt.

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

# Layout-Konstanten (SchweizerAbstractSpielrundeSheet.java / SchweizerRanglisteSheet.java)
SR_ERSTE_DATEN_ZEILE = 2
SR_TEAM_A_SPALTE = 1
SR_TEAM_B_SPALTE = 2
SR_ERG_TEAM_A_SPALTE = 3
SR_ERG_TEAM_B_SPALTE = 4

RL_ERSTE_DATEN_ZEILE = 2
RL_TEAM_NR_SPALTE = 0
RL_TEAM_NAME_SPALTE = 1
RL_SIEGE_SPALTE = 3
RL_BHZ_SPALTE = 4
RL_FBHZ_SPALTE = 5
RL_PUNKTE_PLUS_SPALTE = 6
RL_PUNKTE_MINUS_SPALTE = 7
RL_PUNKTE_DIFF_SPALTE = 8

RANGLISTE_SHEET_NAMEN = ('Rangliste', 'Vorrunden-Rangliste')


def cells_of_row(row):
    out = []
    for c in list(row):
        if c.tag not in (f'{T}table-cell', f'{T}covered-table-cell'):
            continue
        rep = int(c.get(f'{T}number-columns-repeated', '1'))
        val = c.get(f'{O}value')
        if val is None:
            ps = c.findall(f'{TXT}p')
            txt = ''.join(''.join(p.itertext()) for p in ps).strip()
            val = txt if txt else None
        out.extend([val] * rep)
    return out


def sheet_rows(table):
    rows = []
    for r in table.findall(f'{T}table-row'):
        rep = int(r.get(f'{T}number-rows-repeated', '1'))
        cells = cells_of_row(r)
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


def lese_spielergebnisse(root, spielrunden, freispiel_plus, freispiel_minus):
    """Aggregiert je Team: Siege, Punkte+, Punkte-, Gegner-Liste (nur gespielte Paarungen)."""
    siege = defaultdict(int)
    punkte_plus = defaultdict(int)
    punkte_minus = defaultdict(int)
    gegner = defaultdict(list)

    for _, sname in spielrunden:
        table = find_sheet(root, sname)
        rows = sheet_rows(table)
        for z in range(SR_ERSTE_DATEN_ZEILE, len(rows)):
            r = rows[z]
            if len(r) <= SR_TEAM_A_SPALTE:
                continue
            nr_a = to_int(r[SR_TEAM_A_SPALTE])
            if nr_a is None or nr_a <= 0:
                break  # Ende der Daten
            nr_b = to_int(r[SR_TEAM_B_SPALTE]) if len(r) > SR_TEAM_B_SPALTE else None

            if nr_b is None or nr_b <= 0:
                # Freilos für Team A
                siege[nr_a] += 1
                punkte_plus[nr_a] += freispiel_plus
                punkte_minus[nr_a] += freispiel_minus
                continue

            erg_a = to_int(r[SR_ERG_TEAM_A_SPALTE]) if len(r) > SR_ERG_TEAM_A_SPALTE else None
            erg_b = to_int(r[SR_ERG_TEAM_B_SPALTE]) if len(r) > SR_ERG_TEAM_B_SPALTE else None
            erg_a = erg_a or 0
            erg_b = erg_b or 0

            if erg_a > 0 or erg_b > 0:
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

    return siege, punkte_plus, punkte_minus, gegner


def berechne_buchholz(teams, siege, gegner):
    return {nr: sum(siege.get(g, 0) for g in gegner.get(nr, [])) for nr in teams}


def berechne_feinbuchholz(teams, bhz, gegner):
    return {nr: sum(bhz.get(g, 0) for g in gegner.get(nr, [])) for nr in teams}


def lese_rangliste(rl_table):
    rows = sheet_rows(rl_table)
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
            'bhz':   to_int(r[RL_BHZ_SPALTE]) if len(r) > RL_BHZ_SPALTE else None,
            'fbhz':  to_int(r[RL_FBHZ_SPALTE]) if len(r) > RL_FBHZ_SPALTE else None,
            'pp':    to_int(r[RL_PUNKTE_PLUS_SPALTE]) if len(r) > RL_PUNKTE_PLUS_SPALTE else None,
            'pm':    to_int(r[RL_PUNKTE_MINUS_SPALTE]) if len(r) > RL_PUNKTE_MINUS_SPALTE else None,
            'diff':  to_int(r[RL_PUNKTE_DIFF_SPALTE]) if len(r) > RL_PUNKTE_DIFF_SPALTE else None,
        })
    return daten


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
    print()

    siege, punkte_plus, punkte_minus, gegner = lese_spielergebnisse(
        root, spielrunden, freispiel_plus, freispiel_minus)
    # Teamuniversum: nicht nur siege.keys() (Team mit 0 Siegen hätte sonst keinen
    # Eintrag) — punkte_plus wird für jedes Team gebucht, das gespielt oder ein
    # Freilos hatte (Sieger UND Verlierer).
    alle_teams = sorted(set(punkte_plus.keys()) | set(punkte_minus.keys()) | set(gegner.keys()))
    bhz = berechne_buchholz(alle_teams, siege, gegner)
    fbhz = berechne_feinbuchholz(alle_teams, bhz, gegner)

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
        if not ohne_buchholz and ist['bhz'] != soll_bhz:
            diffs.append(f"BHZ(sheet={ist['bhz']},berechnet={soll_bhz})")
        if not ohne_buchholz and ist['fbhz'] != soll_fbhz:
            diffs.append(f"FBHZ(sheet={ist['fbhz']},berechnet={soll_fbhz})")
        if ist['pp'] != soll_pp:
            diffs.append(f"Punkte+(sheet={ist['pp']},berechnet={soll_pp})")
        if ist['pm'] != soll_pm:
            diffs.append(f"Punkte-(sheet={ist['pm']},berechnet={soll_pm})")
        if ist['diff'] != soll_diff:
            diffs.append(f"Diff(sheet={ist['diff']},berechnet={soll_diff})")

        if diffs:
            abweichungen += 1
            print(f"  ❌ Team {nr:>3} ({name}): " + ", ".join(diffs))

    if abweichungen == 0:
        print(f'  ✓ Alle {len(rangliste)} Teams stimmen exakt überein '
              f'(Siege/BHZ/FBHZ/Punkte+/Punkte-/Diff)')
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

    fehler_sort = 0
    for i in range(1, len(rangliste)):
        prev, cur = rangliste[i - 1], rangliste[i]
        if key(prev) > key(cur):
            fehler_sort += 1
            print(f"  ⚠ Sortierfehler bei Platz {cur['rang']}: "
                  f"#{prev['nr']} {prev['name']} {key(prev)} vor "
                  f"#{cur['nr']} {cur['name']} {key(cur)}")
    if fehler_sort == 0:
        print(f'  ✓ Rangliste korrekt sortiert ({len(rangliste)} Teams)')

    return 1 if (abweichungen or fehler_sort) else 0


if __name__ == '__main__':
    sys.exit(main())
