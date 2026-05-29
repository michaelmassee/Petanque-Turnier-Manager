#!/usr/bin/env python3
"""
Überblick-Analyse aller Spieltage einer Supermelee-ODS.

Erkennt alle vorhandenen Spieltage automatisch und gibt je Spieltag
eine Zusammenfassungszeile aus:

  ST | Spieler | Runden | Mitspieler-Wdh | Gegner-Wdh (max) | Crossover | Rangliste OK

Verwendung:
    python3 tools/analyse_alle_spieltage.py <pfad/zur/datei.ods>
"""
import argparse
import re
import sys
import xml.etree.ElementTree as ET
import zipfile
from collections import Counter, defaultdict

NS = {
    'table':  'urn:oasis:names:tc:opendocument:xmlns:table:1.0',
    'office': 'urn:oasis:names:tc:opendocument:xmlns:office:1.0',
    'text':   'urn:oasis:names:tc:opendocument:xmlns:text:1.0',
}
T   = '{%s}' % NS['table']
O   = '{%s}' % NS['office']
TXT = '{%s}' % NS['text']

DEFAULT_NICHT_GESPIELT_PLUS  = 0
DEFAULT_NICHT_GESPIELT_MINUS = 13

ERSTE_DATEN_ZEILE      = 2
PAARUNG_CNTR_SPALTE    = 10
ERSTE_SPIELERNR_SPALTE = 11
ERSTE_SPALTE_ERGEBNISSE = 7


# ---------------------------------------------------------------------------
# ODS-Hilfsfunktionen (identisch zu analyse_supermelee_spieltag.py)
# ---------------------------------------------------------------------------

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
        rows.extend([list(cells_of_row(r))] * rep)
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


def lese_konfig(meta_xml, key, default):
    pat = re.compile(r'meta:name="' + re.escape(key) + r'"[^>]*>([^<]*)<')
    m = pat.search(meta_xml)
    if not m:
        return default
    v = to_int(m.group(1))
    return v if v is not None else default


# ---------------------------------------------------------------------------
# Kern-Analyse
# ---------------------------------------------------------------------------

def parse_spielrunden_fuer_spieltag(root, spieltag_nr):
    """Liest alle Paarungen eines Spieltags; gibt Liste (team1, team2, sa, sb) zurück."""
    paarungen = []
    for runde in range(1, 20):
        sname = f'{spieltag_nr}.{runde}. Spielrunde'
        table = find_sheet(root, sname)
        if table is None:
            break
        rows = sheet_rows(table)
        for z in range(ERSTE_DATEN_ZEILE, len(rows)):
            r = rows[z]
            if len(r) <= ERSTE_SPIELERNR_SPALTE + 5:
                continue
            marker = to_int(r[PAARUNG_CNTR_SPALTE]) if len(r) > PAARUNG_CNTR_SPALTE else None
            if marker is None or marker == -1:
                break
            t1 = [x for x in (to_int(r[ERSTE_SPIELERNR_SPALTE + i]) for i in range(3)) if x and x > 0]
            t2 = [x for x in (to_int(r[ERSTE_SPIELERNR_SPALTE + 3 + i]) for i in range(3)) if x and x > 0]
            sa = to_int(r[ERSTE_SPALTE_ERGEBNISSE])
            sb = to_int(r[ERSTE_SPALTE_ERGEBNISSE + 1])
            if t1 or t2:
                paarungen.append((t1, t2, sa, sb, runde))
    return paarungen


def analysiere_spieltag(root, meta_xml, spieltag_nr, pkt_plus_def, pkt_minus_def):
    """Gibt ein Dict mit Kennzahlen für einen Spieltag zurück."""
    paarungen = parse_spielrunden_fuer_spieltag(root, spieltag_nr)
    if not paarungen:
        return None

    spieler_set = set()
    mitspieler = Counter()
    gegner     = Counter()
    stats      = defaultdict(lambda: {'plus': 0, 'minus': 0, 'spiele': 0, 'siege': 0, 'name': ''})
    runden_je_spieler = defaultdict(set)
    runden_gesamt = set()

    for t1, t2, sa, sb, runde in paarungen:
        runden_gesamt.add(runde)
        for team in (t1, t2):
            spieler_set.update(team)
            for s in team:
                runden_je_spieler[s].add(runde)
            for i in range(len(team)):
                for j in range(i + 1, len(team)):
                    mitspieler[tuple(sorted([team[i], team[j]]))] += 1
        for a in t1:
            for b in t2:
                gegner[tuple(sorted([a, b]))] += 1
        if sa is not None and sb is not None:
            for s in t1:
                stats[s]['plus'] += sa; stats[s]['minus'] += sb
                stats[s]['spiele'] += 1
                if sa > sb: stats[s]['siege'] += 1
            for s in t2:
                stats[s]['plus'] += sb; stats[s]['minus'] += sa
                stats[s]['spiele'] += 1
                if sb > sa: stats[s]['siege'] += 1

    mw = sum(1 for v in mitspieler.values() if v > 1)
    gw = {k: v for k, v in gegner.items() if v > 1}
    crossover = Counter()
    for k, v in mitspieler.items(): crossover[k] += v
    for k, v in gegner.items():     crossover[k] += v
    cw = sum(1 for v in crossover.values() if v > 1)
    max_gegner_wdh = max(gw.values(), default=0)

    # Namen aus Rangliste befüllen
    rl_name = f'{spieltag_nr}. Spieltag Rangliste'
    rl_table = find_sheet(root, rl_name)
    if rl_table is not None:
        for row in sheet_rows(rl_table)[3:]:
            nr = to_int(row[0]) if len(row) > 0 else None
            name = row[1].strip() if len(row) > 1 and row[1] else ''
            if nr and name:
                stats[nr]['name'] = name

    # Rangliste-Check
    rl_ok = None
    if rl_table is not None:
        rows = sheet_rows(rl_table)
        anz_runden = len(runden_gesamt)
        abweich = 0
        for i in range(3, len(rows)):
            r = rows[i]
            nr = to_int(r[0]) if len(r) > 0 else None
            if not nr:
                continue
            s = stats.get(nr, {'plus': 0, 'minus': 0, 'spiele': 0, 'siege': 0})
            gespielt = len(runden_je_spieler.get(nr, set()))
            fehlt = anz_runden - gespielt
            ep = s['plus'] + fehlt * pkt_plus_def
            em = s['minus'] + fehlt * pkt_minus_def
            ed = ep - em
            es = s['siege']
            epk = es - ((s['spiele'] + fehlt) - es)
            rl_plus   = to_int(r[14]) if len(r) > 14 else None
            rl_minus  = to_int(r[15]) if len(r) > 15 else None
            rl_delta  = to_int(r[16]) if len(r) > 16 else None
            rl_siege  = to_int(r[18]) if len(r) > 18 else None
            rl_punkte = to_int(r[19]) if len(r) > 19 else None
            if any(x != y for x, y in
                   [(rl_plus, ep), (rl_minus, em), (rl_delta, ed),
                    (rl_siege, es), (rl_punkte, epk)]):
                abweich += 1
        rl_ok = abweich == 0

    return {
        'spieltag':     spieltag_nr,
        'spieler':      len(spieler_set),
        'runden':       len(runden_gesamt),
        'mitspieler_wdh': mw,
        'gegner_wdh':   len(gw),
        'max_gegner_wdh': max_gegner_wdh,
        'crossover':    cw,
        'rl_ok':        rl_ok,
        'gegner_detail': gw,
        'stats':        stats,
    }


def erkenne_spieltage(root):
    """Gibt sortierte Liste aller Spieltagnummern zurück, für die Spielrunden existieren."""
    nummern = set()
    for t in root.iter(f'{T}table'):
        m = re.match(r'^(\d+)\.(\d+)\. Spielrunde$', t.get(f'{T}name', ''))
        if m:
            nummern.add(int(m.group(1)))
    return sorted(nummern)


# ---------------------------------------------------------------------------
# main
# ---------------------------------------------------------------------------

def main():
    p = argparse.ArgumentParser(description=__doc__.splitlines()[1])
    p.add_argument('ods', help='Pfad zur Supermelee-ODS-Datei')
    args = p.parse_args()

    with zipfile.ZipFile(args.ods) as zf:
        root = ET.fromstring(zf.read('content.xml'))
        meta_xml = zf.read('meta.xml').decode('utf-8')

    pkt_plus_def  = lese_konfig(meta_xml, 'Nicht gespielte Runde, + Punkte', DEFAULT_NICHT_GESPIELT_PLUS)
    pkt_minus_def = lese_konfig(meta_xml, 'Nicht gespielte Runde, - Punkte', DEFAULT_NICHT_GESPIELT_MINUS)

    spieltage = erkenne_spieltage(root)
    if not spieltage:
        print('Keine Spielrunden-Sheets gefunden.', file=sys.stderr)
        return 1

    ergebnisse = []
    for st in spieltage:
        r = analysiere_spieltag(root, meta_xml, st, pkt_plus_def, pkt_minus_def)
        if r:
            ergebnisse.append(r)

    # Tabellenausgabe
    print(f'Supermelee-Überblick: {args.ods}')
    print()
    print(f'{"ST":>3} | {"Spieler":>7} | {"Runden":>6} | '
          f'{"Mitspieler-Wdh":>14} | {"Gegner-Wdh":>14} | {"Crossover":>9} | Rangliste')
    print('-' * 80)

    fehler_gesamt = False
    for r in ergebnisse:
        ms_ok  = '✓ 0' if r['mitspieler_wdh'] == 0 else f'❌ {r["mitspieler_wdh"]}'
        gw_str = (f'{r["gegner_wdh"]} (max {r["max_gegner_wdh"]}×)'
                  if r['gegner_wdh'] > 0 else '✓ 0')
        cw_str = str(r['crossover']) if r['crossover'] > 0 else '✓ 0'
        rl_str = ('✓' if r['rl_ok'] else ('❌' if r['rl_ok'] is False else '?'))
        print(f'{r["spieltag"]:>3} | {r["spieler"]:>7} | {r["runden"]:>6} | '
              f'{ms_ok:>14} | {gw_str:>14} | {cw_str:>9} | {rl_str}')
        if r['mitspieler_wdh'] > 0:
            fehler_gesamt = True

    print()

    # Detaillierte Gegner-Wiederholungen je Spieltag
    hat_gw = any(r['gegner_wdh'] > 0 for r in ergebnisse)
    if hat_gw:
        print('Gegner-Wiederholungen im Detail:')
        for r in ergebnisse:
            if not r['gegner_detail']:
                continue
            def nm(n): return r['stats'].get(n, {}).get('name', '') or f'#{n}'
            for (a, b), cnt in sorted(r['gegner_detail'].items(), key=lambda x: (-x[1], x[0])):
                print(f'  ST{r["spieltag"]}  {cnt}× : {a:>4} {nm(a):28s}  vs  {b:>4} {nm(b)}')
        print()

    if fehler_gesamt:
        print('❌ Mitspieler-Wiederholungen gefunden — Hard-Constraint verletzt!')
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main())
