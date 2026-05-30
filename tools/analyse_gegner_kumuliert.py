#!/usr/bin/env python3
"""
Kumulierte Gegner-Analyse über alle Spieltage einer Supermelee-ODS.

Zeigt pro Spieltag:
  - Gegner-Sättigung (% bereits gespielter Paare) VOR dem Spieltag
  - Neue Gegner-Paare in diesem Spieltag
  - Cross-Spieltag-Wiederholungen (Paare mit Vorgeschichte aus früheren ST)
  - Intra-Spieltag-Wiederholungen (Paar trifft sich mehrfach INNERHALB dieses ST)

Gibt außerdem für alle intra-ST-Wiederholungen eine Ursachenanalyse:
  - Score den der Optimizer VOR dem Spieltag gesehen hätte (vollständige History)
  - Score nach effMaxSpieltage-Reset auf 0 (nur laufender ST geladen)
  - Visualisiert dadurch, warum history-Verlust die Gegner-Qualität verschlechtert

Verwendung:
    python3 tools/analyse_gegner_kumuliert.py <pfad/zur/datei.ods>
    python3 tools/analyse_gegner_kumuliert.py <pfad/zur/datei.ods> --spieltag 4
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
T   = '{%s}' % NS['table']
O   = '{%s}' % NS['office']
TXT = '{%s}' % NS['text']

ERSTE_DATEN_ZEILE      = 2
PAARUNG_CNTR_SPALTE    = 10
ERSTE_SPIELERNR_SPALTE = 11
ERSTE_SPALTE_ERGEBNISSE = 7


# ---------------------------------------------------------------------------
# ODS-Hilfsfunktionen
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


def erkenne_spieltage(root):
    nummern = set()
    for t in root.iter(f'{T}table'):
        m = re.match(r'^(\d+)\.(\d+)\. Spielrunde$', t.get(f'{T}name', ''))
        if m:
            nummern.add(int(m.group(1)))
    return sorted(nummern)


def parse_runde(root, spieltag_nr, runde_nr):
    """Gibt dict {spiel_nr: {'A': [nrs], 'B': [nrs]}} zurück; leer wenn Sheet nicht vorhanden."""
    sname = f'{spieltag_nr}.{runde_nr}. Spielrunde'
    table = find_sheet(root, sname)
    if table is None:
        return {}
    rows = sheet_rows(table)
    spiele = {}
    for z in range(ERSTE_DATEN_ZEILE, len(rows)):
        r = rows[z]
        if len(r) <= ERSTE_SPIELERNR_SPALTE + 5:
            continue
        marker = to_int(r[PAARUNG_CNTR_SPALTE]) if len(r) > PAARUNG_CNTR_SPALTE else None
        if marker is None or marker == -1:
            break
        t1 = [x for x in (to_int(r[ERSTE_SPIELERNR_SPALTE + i]) for i in range(3)) if x and x > 0]
        t2 = [x for x in (to_int(r[ERSTE_SPIELERNR_SPALTE + 3 + i]) for i in range(3)) if x and x > 0]
        spiel_nr = marker
        if spiel_nr not in spiele:
            spiele[spiel_nr] = {'A': [], 'B': []}
        spiele[spiel_nr]['A'].extend(t1)
        spiele[spiel_nr]['B'].extend(t2)
    return spiele


def gegner_paare_aus_spiele(spiele):
    """Gibt Counter {(min,max): anzahl} aus einem dict von Spielen zurück."""
    paare = defaultdict(int)
    for sp in spiele.values():
        for a in sp['A']:
            for b in sp['B']:
                paare[(min(a, b), max(a, b))] += 1
    return paare


def spieler_namen(root, alle_spieltage):
    """Sammelt Spielernamen aus allen Rangliste-Sheets."""
    namen = {}
    for st in alle_spieltage:
        rl_name = f'{st}. Spieltag Rangliste'
        table = find_sheet(root, rl_name)
        if table is None:
            continue
        for row in sheet_rows(table)[3:]:
            nr = to_int(row[0]) if len(row) > 0 else None
            name = row[1].strip() if len(row) > 1 and row[1] else ''
            if nr and name and nr not in namen:
                namen[nr] = name
    return namen


# ---------------------------------------------------------------------------
# Hauptanalyse
# ---------------------------------------------------------------------------

def analysiere_kumuliert(root, spieltage, ziel_spieltag=None):
    namen = spieler_namen(root, spieltage)
    def nm(n): return namen.get(n, f'#{n}')

    # kumuliert[paar] = gesamtanzahl bisher (über alle Spieltage und Runden)
    kumuliert = defaultdict(int)

    print('=' * 72)
    print('  KUMULIERTE GEGNER-ANALYSE')
    print('=' * 72)

    for st in spieltage:
        if ziel_spieltag and st > ziel_spieltag:
            break

        # Teilnehmer und Sättigung VOR diesem Spieltag ermitteln
        teilnehmer = set()
        for rnd in range(1, 20):
            sp = parse_runde(root, st, rnd)
            if not sp:
                break
            for s in sp.values():
                teilnehmer.update(s['A'] + s['B'])

        moegliche_paare = len(teilnehmer) * (len(teilnehmer) - 1) // 2
        bekannt_vor_st = sum(1 for p in kumuliert if p[0] in teilnehmer and p[1] in teilnehmer
                             and kumuliert[p] > 0)
        saettigung = bekannt_vor_st / moegliche_paare * 100 if moegliche_paare > 0 else 0

        print(f'\n{"─"*72}')
        print(f'  Spieltag {st}  ({len(teilnehmer)} Spieler, '
              f'{moegliche_paare} mögliche Paare, {saettigung:.1f}% gesättigt vor diesem ST)')
        print(f'{"─"*72}')

        # Intra-ST-Gegner akkumulieren (für Wiederholungs-Erkennung INNERHALB dieses ST)
        st_gegner = defaultdict(int)

        # Gegner aus früheren Runden dieses ST (für Ursachenanalyse)
        gegner_frueherer_rnds = defaultdict(int)

        runde_nr = 0
        while True:
            runde_nr += 1
            spiele = parse_runde(root, st, runde_nr)
            if not spiele:
                break

            rnd_paare = gegner_paare_aus_spiele(spiele)

            # Cross-ST-Wiederholungen (Paare mit Vorgeschichte aus FRÜHEREN Spieltagen)
            cross_wdh = [(p, kumuliert[p]) for p in rnd_paare
                         if kumuliert.get(p, 0) > 0 and st_gegner.get(p, 0) == 0]
            # Intra-ST-Wiederholungen (Paar trifft sich erneut INNERHALB dieses ST)
            intra_wdh = [(p, st_gegner[p]) for p in rnd_paare if st_gegner.get(p, 0) > 0]

            max_cross = max((v for _, v in cross_wdh), default=0)
            print(f'  Runde {runde_nr}: {len(rnd_paare):3d} Gegner-Paare | '
                  f'cross-ST: {len(cross_wdh):2d}', end='')
            if max_cross > 0:
                print(f' (max {max_cross}× Vorgeschichte)', end='')
            print(f' | intra-ST: {len(intra_wdh):2d}', end='')
            print()

            for p, prev in sorted(intra_wdh, key=lambda x: -x[1]):
                # Ursachenanalyse: Score mit voller History vs. nur laufendem ST
                score_voll    = (kumuliert.get(p, 0) + gegner_frueherer_rnds.get(p, 0)) * 10
                score_nur_st  = gegner_frueherer_rnds.get(p, 0) * 10
                print(f'    ⚠ INTRA  : {p[0]:4d} {nm(p[0]):26s} vs {p[1]:4d} {nm(p[1])}')
                print(f'       bereits {prev}× in diesem ST  |  '
                      f'Score vollst.: {score_voll}  |  Score nur akt.ST-Runden: {score_nur_st}')
                if score_voll > score_nur_st:
                    print(f'       → {(score_voll-score_nur_st)//10} Gegner-Kontakt(e) aus '
                          f'früheren ST unsichtbar wenn effMaxSpieltage=0 !')

            # Kumulieren für nächste Iteration
            for p, cnt in rnd_paare.items():
                kumuliert[p] += cnt
                st_gegner[p] += cnt
                gegner_frueherer_rnds[p] += cnt  # wird für nächste Runde dieses ST genutzt

            # Rücksetzen für nächste Runde: nur kumuliert und st_gegner weiterführen
            # gegner_frueherer_rnds bleibt stehen (akkumuliert innerhalb ST)

        if not any(st_gegner[p] > 1 for p in st_gegner):
            print(f'  ✓ Keine Intra-ST-Gegner-Wiederholungen')

    # Gesamtübersicht
    print(f'\n{"="*72}')
    print('  GESAMTÜBERSICHT (alle Spieltage kumuliert)')
    print(f'{"="*72}')
    gesamt_paare = sum(1 for v in kumuliert.values() if v > 0)
    mehrf = {p: v for p, v in kumuliert.items() if v > 1}
    print(f'  Einzigartige Gegner-Paare: {gesamt_paare}')
    print(f'  Davon mehrfach gespielt:   {len(mehrf)}')
    if mehrf:
        print()
        print('  Top-10 häufigste Gegner-Paare (über alle Spieltage):')
        for (a, b), cnt in sorted(mehrf.items(), key=lambda x: -x[1])[:10]:
            print(f'    {cnt}× : {a:4d} {nm(a):26s}  vs  {b:4d} {nm(b)}')


def analysiere_spiel_scores(root, spieltage, ziel_spieltag):
    """Detailanalyse: alle Spiele eines Spieltags mit Gegner-Scores (vollständig vs. nur laufender ST)."""
    namen = spieler_namen(root, spieltage)
    def nm(n): return namen.get(n, f'#{n}')

    # Baue Gegner-History bis VOR dem Ziel-Spieltag
    gegner_vor_ziel_st = defaultdict(int)
    for st in spieltage:
        if st >= ziel_spieltag:
            break
        for rnd in range(1, 20):
            sp = parse_runde(root, st, rnd)
            if not sp:
                break
            for p, cnt in gegner_paare_aus_spiele(sp).items():
                gegner_vor_ziel_st[p] += cnt

    print(f'\n{"="*72}')
    print(f'  SPIEL-SCORES Spieltag {ziel_spieltag}: vollständig vs. nur laufender ST')
    print(f'  (simuliert was der Gegner-Optimizer sieht wenn effMaxSpieltage auf 0 reduziert)')
    print(f'{"="*72}')

    gegner_frueherer_rnds_st = defaultdict(int)

    for rnd in range(1, 20):
        spiele = parse_runde(root, ziel_spieltag, rnd)
        if not spiele:
            break

        print(f'\n  --- Runde {rnd} ---')
        for spiel_nr in sorted(spiele.keys()):
            sp = spiele[spiel_nr]
            a_nrs, b_nrs = sp['A'], sp['B']
            if not a_nrs or not b_nrs:
                continue

            score_voll   = 0
            score_nur_st = 0
            details = []
            for a in a_nrs:
                for b in b_nrs:
                    p = (min(a,b), max(a,b))
                    v  = gegner_vor_ziel_st.get(p, 0)
                    s  = gegner_frueherer_rnds_st.get(p, 0)
                    score_voll   += (v + s) * 10
                    score_nur_st += s * 10
                    if v + s > 0:
                        details.append((a, b, v, s))

            wdh = sum(1 for a in a_nrs for b in b_nrs
                      if gegner_frueherer_rnds_st.get((min(a,b), max(a,b)), 0) > 0)
            marker = '  ⚠ INTRA-WDH' if wdh else ''
            print(f'  Spiel {spiel_nr:2d}: {a_nrs} vs {b_nrs}{marker}')
            print(f'    Score vollst.: {score_voll:4d}  |  Score nur akt.ST: {score_nur_st:4d}', end='')
            if score_voll > score_nur_st:
                print(f'  ← {(score_voll-score_nur_st)//10} Kontakt(e) aus früheren ST unsichtbar', end='')
            print()
            for a, b, v, s in details:
                print(f'    > {a:4d} {nm(a):24s} vs {b:4d} {nm(b):24s}: '
                      f'{v}× vor ST{ziel_spieltag} + {s}× akt.ST-Runden')

        # Runde kumulieren für nächste Runde
        for p, cnt in gegner_paare_aus_spiele(spiele).items():
            gegner_frueherer_rnds_st[p] += cnt


# ---------------------------------------------------------------------------
# main
# ---------------------------------------------------------------------------

def main():
    p = argparse.ArgumentParser(description=__doc__.splitlines()[1])
    p.add_argument('ods', help='Pfad zur Supermelee-ODS-Datei')
    p.add_argument('--spieltag', type=int, default=None,
                   help='Nur bis zu diesem Spieltag analysieren (default: alle)')
    p.add_argument('--scores', action='store_true',
                   help='Zusätzlich Spiel-Scores je Runde des Ziel-Spieltags ausgeben')
    args = p.parse_args()

    with zipfile.ZipFile(args.ods) as zf:
        root = ET.fromstring(zf.read('content.xml'))

    spieltage = erkenne_spieltage(root)
    if not spieltage:
        print('Keine Spielrunden-Sheets gefunden.', file=sys.stderr)
        return 1

    analysiere_kumuliert(root, spieltage, args.spieltag)

    if args.scores and args.spieltag:
        analysiere_spiel_scores(root, spieltage, args.spieltag)

    return 0


if __name__ == '__main__':
    sys.exit(main())
