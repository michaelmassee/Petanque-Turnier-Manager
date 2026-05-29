#!/usr/bin/env python3
"""
Konsistenz-Analyse für eine Supermelee-Spieltag-ODS.

Liest direkt aus der ODS (content.xml + meta.xml) und prüft:
  1. Mitspieler-Wiederholungen (gleiche Spieler 2× im selben Team)
  2. Gegner-Wiederholungen (Spielerpaar trifft sich mehrfach als Gegner)
  3. Crossover (Spielerpaar mehrfach im selben Spiel — egal ob Mitspieler
     oder Gegner; weicher Constraint des Supermelee-Algorithmus)
  4. Rangliste-Werte (Σ+, Σ-, Δ, Siege, Punkte) gegen Spielrunden-Daten,
     inkl. der konfigurierten Default-Punkte für nicht angetretene Spieler
  5. Rangliste-Sortierung (Siege ↓ → Punkte ↓ → Δ ↓ → Σ+ ↓)

Konfiguration wird aus den ODS-Document-Properties gelesen (meta.xml,
Property-Namen "Nicht gespielte Runde, + Punkte" / "... - Punkte"); fehlt
sie, gelten die Defaults aus SuperMeleePropertiesSpalte.java (0 / 13).

Verwendung:
    python3 tools/analyse_supermelee_spieltag.py <pfad/zur/datei.ods>

SYNC-CHECK: Spalten-Konstanten und Prüflogik müssen synchron mit
SupermeleeSpieltagAnalyseAssert.java (src/test/java/.../spieltagrangliste/) bleiben.
Sync-Verifikation: python3 tools/test_analyse_supermelee_sync.py
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
    'meta':   'urn:oasis:names:tc:opendocument:xmlns:meta:1.0',
}
T = '{%s}' % NS['table']
O = '{%s}' % NS['office']
TXT = '{%s}' % NS['text']

# Defaults aus SuperMeleePropertiesSpalte.java (Stand 5.30.0)
DEFAULT_NICHT_GESPIELT_PLUS = 0
DEFAULT_NICHT_GESPIELT_MINUS = 13

# Layout-Konstanten der Spielrunden-Sheets (SpielrundeSheetKonstanten.java)
ERSTE_DATEN_ZEILE = 2
PAARUNG_CNTR_SPALTE = 10
ERSTE_SPIELERNR_SPALTE = 11
ERSTE_SPALTE_ERGEBNISSE = 7  # Spalte H = Team1-Punkte, I = Team2-Punkte


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


def lade_ods(pfad):
    with zipfile.ZipFile(pfad) as zf:
        content = ET.fromstring(zf.read('content.xml'))
        meta = zf.read('meta.xml').decode('utf-8')
    return content, meta


def parse_spielrunden(root):
    """Findet alle Spielrunden-Sheets (Name 'X.Y. Spielrunde') und liest deren Paarungen."""
    runden = []
    for t in root.iter(f'{T}table'):
        name = t.get(f'{T}name', '')
        m = re.match(r'^(\d+)\.(\d+)\. Spielrunde$', name)
        if not m:
            continue
        runden.append((int(m.group(1)), int(m.group(2)), name))
    runden.sort()

    paarungen_je_sheet = {}
    for _, _, sname in runden:
        table = find_sheet(root, sname)
        rows = sheet_rows(table)
        paarungen = []
        for z in range(ERSTE_DATEN_ZEILE, len(rows)):
            r = rows[z]
            if len(r) <= ERSTE_SPIELERNR_SPALTE + 5:
                continue
            marker = to_int(r[PAARUNG_CNTR_SPALTE]) if len(r) > PAARUNG_CNTR_SPALTE else None
            if marker is None or marker == -1:
                break
            t1 = [to_int(r[ERSTE_SPIELERNR_SPALTE + i]) for i in range(3)]
            t1 = [x for x in t1 if x and x > 0]
            t2 = [to_int(r[ERSTE_SPIELERNR_SPALTE + 3 + i]) for i in range(3)]
            t2 = [x for x in t2 if x and x > 0]
            sa = to_int(r[ERSTE_SPALTE_ERGEBNISSE])
            sb = to_int(r[ERSTE_SPALTE_ERGEBNISSE + 1])
            paarungen.append((t1, t2, sa, sb))
        paarungen_je_sheet[sname] = paarungen
    return paarungen_je_sheet


def aggregiere_stats(paarungen_je_sheet):
    """Aggregiert je Spieler: Σ+, Σ-, Spiele, Siege; und sammelt Mitspieler-/Gegner-Paare."""
    stats = defaultdict(lambda: {'plus': 0, 'minus': 0, 'spiele': 0, 'siege': 0, 'name': ''})
    mitspieler = Counter()
    gegner = Counter()
    rundennr_je_spieler = defaultdict(set)  # für "fehlt in Runde X"-Erkennung

    for sname, paar in paarungen_je_sheet.items():
        m = re.match(r'^(\d+)\.(\d+)\. Spielrunde$', sname)
        runde_key = (int(m.group(1)), int(m.group(2)))
        for t1, t2, sa, sb in paar:
            for team in (t1, t2):
                for i in range(len(team)):
                    rundennr_je_spieler[team[i]].add(runde_key)
                    for j in range(i + 1, len(team)):
                        mitspieler[tuple(sorted([team[i], team[j]]))] += 1
            for a in t1:
                for b in t2:
                    gegner[tuple(sorted([a, b]))] += 1
            if sa is None or sb is None:
                continue
            for s in t1:
                stats[s]['plus'] += sa
                stats[s]['minus'] += sb
                stats[s]['spiele'] += 1
                if sa > sb:
                    stats[s]['siege'] += 1
            for s in t2:
                stats[s]['plus'] += sb
                stats[s]['minus'] += sa
                stats[s]['spiele'] += 1
                if sb > sa:
                    stats[s]['siege'] += 1
    return stats, mitspieler, gegner, rundennr_je_spieler


def lese_rangliste(root, spielTagNr):
    """Liest aus dem Rangliste-Sheet 'X. Spieltag Rangliste'.

    Spalten-Layout (durch Inspektion empirisch ermittelt):
      0 = Nr, 1 = Name, 14 = Σ+, 15 = Σ-, 16 = Δ, 18 = Siege, 19 = Punkte(=Siege-Niederlagen)
    """
    name = f'{spielTagNr}. Spieltag Rangliste'
    table = find_sheet(root, name)
    if table is None:
        return None, name
    rows = sheet_rows(table)
    daten = []
    for i in range(3, len(rows)):
        r = rows[i]
        nr = to_int(r[0]) if len(r) > 0 else None
        if not nr:
            continue
        daten.append({
            'rang': i - 2,
            'nr': nr,
            'name': r[1] if len(r) > 1 else '',
            'plus':  to_int(r[14]) if len(r) > 14 else None,
            'minus': to_int(r[15]) if len(r) > 15 else None,
            'delta': to_int(r[16]) if len(r) > 16 else None,
            'siege': to_int(r[18]) if len(r) > 18 else None,
            'punkte': to_int(r[19]) if len(r) > 19 else None,
        })
    return daten, name


def main():
    p = argparse.ArgumentParser(description=__doc__.splitlines()[1])
    p.add_argument('ods', help='Pfad zur Supermelee-ODS-Datei')
    p.add_argument('--spieltag', type=int, default=1, help='Spieltagnummer (default 1)')
    args = p.parse_args()

    root, meta_xml = lade_ods(args.ods)

    pkt_plus_def  = lese_konfig(meta_xml, 'Nicht gespielte Runde, + Punkte', DEFAULT_NICHT_GESPIELT_PLUS)
    pkt_minus_def = lese_konfig(meta_xml, 'Nicht gespielte Runde, - Punkte', DEFAULT_NICHT_GESPIELT_MINUS)

    paarungen = parse_spielrunden(root)
    spielrunden_dieses_spieltags = {k: v for k, v in paarungen.items()
                                    if k.startswith(f'{args.spieltag}.')}
    stats, mitspieler, gegner, rundennr_je_spieler = aggregiere_stats(spielrunden_dieses_spieltags)
    rangliste, rl_name = lese_rangliste(root, args.spieltag)

    # Namen früh hinterlegen — wird in Schritt 2 für die Gegner-Liste benötigt
    if rangliste:
        for rl in rangliste:
            stats[rl['nr']]['name'] = rl['name']

    print('=' * 72)
    print(f'  Supermelee-Spieltag-Analyse: {args.ods}')
    print('=' * 72)
    print(f'  Spieltag {args.spieltag}, {len(spielrunden_dieses_spieltags)} Spielrunden, {len(stats)} Spieler')
    print(f'  Konfig "Nicht gespielte Runde": + {pkt_plus_def} Punkte, - {pkt_minus_def} Punkte')
    if not lese_konfig(meta_xml, 'Nicht gespielte Runde, - Punkte', None):
        print('       (Defaults aus SuperMeleePropertiesSpalte.java — nicht explizit in der Datei gesetzt)')
    print()

    # --- 1) Mitspieler ---
    mw = {k: v for k, v in mitspieler.items() if v > 1}
    print('--- 1) MITSPIELER-WIEDERHOLUNGEN ---')
    if mw:
        print(f'  ❌ {len(mw)} Paare mehrfach im selben Team:')
        for (a, b), n in sorted(mw.items(), key=lambda x: -x[1])[:30]:
            print(f'     Spieler {a} + {b} → {n}×')
    else:
        print(f'  ✓ keine Wiederholungen ({len(mitspieler)} Paare bildeten je 1× ein Team)')
    print()

    # --- 2) Gegner ---
    gw = {k: v for k, v in gegner.items() if v > 1}
    print('--- 2) GEGNER-WIEDERHOLUNGEN ---')
    print(f'  Gegner-Paare insgesamt: {len(gegner)}, davon mit Wiederholung: {len(gw)}')
    vt = Counter(gw.values())
    for n in sorted(vt):
        print(f'     {vt[n]} Paare spielten {n}× gegeneinander')
    if gw:
        def nm(n): return stats.get(n, {}).get('name') or f'#{n}'
        for (a, b), n in sorted(gw.items(), key=lambda x: (-x[1], x[0])):
            print(f'     {n}× : {a:>3} {nm(a):28s}  vs  {b:>3} {nm(b)}')
    print()

    # --- 3) Crossover: Paar war sowohl Mitspieler als auch Gegner (Rollenwechsel) ---
    # Nur Paare mit mitspieler > 0 UND gegner > 0 — Fall "2× Mitspieler" oder
    # "2× Gegner" ist bereits in den vorigen Abschnitten erfasst und zählt hier nicht.
    alle_paare = set(mitspieler) | set(gegner)
    cw = {k: (mitspieler.get(k, 0), gegner.get(k, 0))
          for k in alle_paare
          if mitspieler.get(k, 0) > 0 and gegner.get(k, 0) > 0}
    print('--- 3) CROSSOVER (Paar war sowohl Mitspieler als auch Gegner) ---')
    print(f'  Spielerpaare insgesamt im selben Spiel: {len(alle_paare)}, davon Rollenwechsel: {len(cw)}')
    if cw:
        def nm(n): return stats.get(n, {}).get('name') or f'#{n}'
        for (a, b), (ms, gg) in sorted(cw.items(), key=lambda x: (-(x[1][0]+x[1][1]), x[0])):
            print(f'     {a:>3} {nm(a):28s}  +  {b:>3} {nm(b):28s}  (Team {ms}×, Gegner {gg}×)')
    print()

    # --- 4) Rangliste-Werte ---
    print(f'--- 4) RANGLISTE-WERTE  ({rl_name}) ---')
    if rangliste is None:
        print('  ⚠ Rangliste-Sheet nicht gefunden')
    else:
        # Spieltag-Rundenanzahl bestimmen (für Default-Buchung bei fehlender Runde)
        alle_runden = {k for runden_set in rundennr_je_spieler.values() for k in runden_set}
        anz_runden = len([k for k in alle_runden if k[0] == args.spieltag])

        abweich = []
        ok = 0
        for rl in rangliste:
            s = stats.get(rl['nr'], {'plus': 0, 'minus': 0, 'spiele': 0, 'siege': 0})
            # Fehlende Runden zählen
            gespielt = len([k for k in rundennr_je_spieler.get(rl['nr'], set())
                           if k[0] == args.spieltag])
            fehlt = anz_runden - gespielt

            erwartet_plus  = s['plus']  + fehlt * pkt_plus_def
            erwartet_minus = s['minus'] + fehlt * pkt_minus_def
            erwartet_spiele = s['spiele'] + fehlt
            erwartet_siege  = s['siege']  # nicht angetretene Runde zählt nicht als Sieg
            erwartet_delta  = erwartet_plus - erwartet_minus
            erwartet_punkte = erwartet_siege - (erwartet_spiele - erwartet_siege)

            diffs = []
            if rl['plus']   != erwartet_plus:   diffs.append(f'Σ+(rl={rl["plus"]},erw={erwartet_plus})')
            if rl['minus']  != erwartet_minus:  diffs.append(f'Σ-(rl={rl["minus"]},erw={erwartet_minus})')
            if rl['delta']  != erwartet_delta:  diffs.append(f'Δ(rl={rl["delta"]},erw={erwartet_delta})')
            if rl['siege']  != erwartet_siege:  diffs.append(f'Siege(rl={rl["siege"]},erw={erwartet_siege})')
            if rl['punkte'] != erwartet_punkte: diffs.append(f'Punkte(rl={rl["punkte"]},erw={erwartet_punkte})')
            if diffs:
                abweich.append((rl, fehlt, diffs))
            else:
                ok += 1

        if not abweich:
            print(f'  ✓ Alle {ok} Spieler stimmen exakt überein (inkl. Default-Buchung für nicht gespielte Runden)')
        else:
            print(f'  ❌ {len(abweich)} Abweichungen (von {len(rangliste)})')
            for rl, fehlt, diffs in abweich[:30]:
                print(f'     #{rl["nr"]:>3} {rl["name"]:30s} fehlt={fehlt}  {", ".join(diffs)}')

    # --- 5) Sortierung ---
    print()
    print('--- 5) RANGLISTE-SORTIERUNG (Siege ↓ → Punkte ↓ → Δ ↓ → Σ+ ↓) ---')
    if rangliste:
        soll = sorted(rangliste,
                      key=lambda x: (-(x['siege'] or 0), -(x['punkte'] or 0),
                                     -(x['delta'] or 0), -(x['plus'] or 0)))
        diffs = [(o, s) for o, s in zip(rangliste, soll) if o['nr'] != s['nr']]
        if not diffs:
            print(f'  ✓ Rangliste korrekt sortiert ({len(rangliste)} Spieler)')
        else:
            print(f'  ⚠ {len(diffs)} Positionen weichen ab:')
            for o, s in diffs[:20]:
                print(f'     Platz {o["rang"]:>2}: ODS=#{o["nr"]} {o["name"]:25s} | erwartet=#{s["nr"]} {s["name"]}')

    return 0


if __name__ == '__main__':
    sys.exit(main())
