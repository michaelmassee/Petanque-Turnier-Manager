#!/usr/bin/env python3
"""
Sync-Check: Python-Analyse-Skript ↔ Java-UITest

Liest die relevanten Java-Quellcode-Konstanten per Regex und assertiert, dass die
hardcodierten Spaltenindizes in analyse_supermelee_spieltag.py damit übereinstimmen.

Bei einer Java-Konstanten-Änderung schlägt dieser Test sofort fehl und erzwingt
die gleichzeitige Anpassung beider Dateien.

Aufruf:  python3 tools/test_analyse_supermelee_sync.py
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).parent.parent

# Pfade zu den relevanten Quellcode-Dateien
SPIELRUNDE_KONST = (ROOT / 'src/main/java/de/petanqueturniermanager/supermelee/spielrunde'
                    '/SpielrundeSheetKonstanten.java')
RANGLISTE_DELEGATE = (ROOT / 'src/main/java/de/petanqueturniermanager/supermelee/spieltagrangliste'
                      '/SpieltagRanglisteDelegate.java')
SUMMEN_SPALTEN = (ROOT / 'src/main/java/de/petanqueturniermanager/supermelee'
                  '/SuperMeleeSummenSpalten.java')
RANGLISTE_SHEET = (ROOT / 'src/main/java/de/petanqueturniermanager/supermelee/spieltagrangliste'
                   '/SpieltagRanglisteSheet.java')
ANALYSE_SKRIPT = ROOT / 'tools/analyse_supermelee_spieltag.py'


def java_int_const(src: str, name: str) -> int:
    """Extrahiert einen Integer-Konstantenwert aus Java-Quellcode.

    Unterstützt einfache Literale (`FOO = 3`) und einstellige additive Ausdrücke
    (`FOO = BAR + 2`). Für zusammengesetzte Ausdrücke wird der Wert rekursiv aufgelöst.
    Schlägt fehl, wenn der Name nicht gefunden wird.
    """
    pattern = re.compile(
        r'\b' + re.escape(name) + r'\s*=\s*([^;]+);'
    )
    m = pattern.search(src)
    if not m:
        raise AssertionError(f"Konstante '{name}' nicht in Java-Quelle gefunden")
    expr = m.group(1).strip()
    # Einfaches Literal
    try:
        return int(expr)
    except ValueError:
        pass
    # Einfacher Additionsausdruck: TOKEN [+-] NUMBER
    add_m = re.match(r'^(\w+)\s*([+-])\s*(\d+)$', expr)
    if add_m:
        base = java_int_const(src, add_m.group(1))
        op = add_m.group(2)
        offset = int(add_m.group(3))
        return base + offset if op == '+' else base - offset
    # Einfache Alias-Zuweisung: anderer Konstantenname
    if re.match(r'^\w+$', expr):
        return java_int_const(src, expr)
    raise AssertionError(f"Konnte Ausdruck '{expr}' für '{name}' nicht auswerten")


def py_int_const(src: str, name: str) -> int:
    """Extrahiert einen Integer-Konstantenwert aus Python-Quellcode."""
    m = re.search(r'^' + re.escape(name) + r'\s*=\s*(\d+)', src, re.MULTILINE)
    if not m:
        raise AssertionError(f"Konstante '{name}' nicht in Python-Quelle gefunden")
    return int(m.group(1))


def py_rangliste_col(src: str, feldname: str) -> int:
    """Extrahiert den Spaltenindex aus dem lese_rangliste-Dict-Literal (to_int-Form)."""
    # Sucht explizit nach: 'feldname':  to_int(r[ZAHL])
    m = re.search(
        r"'" + re.escape(feldname) + r"':\s*to_int\(r\[(\d+)\]\)",
        src
    )
    if not m:
        raise AssertionError(f"Rangliste-Spalte '{feldname}' (to_int-Form) nicht in Python-Quelle gefunden")
    return int(m.group(1))


def py_rangliste_daten_zeile(src: str) -> int:
    """Extrahiert die erste Datenzeile aus lese_rangliste: 'for i in range(N, ...'"""
    m = re.search(r'for i in range\((\d+),\s*len\(rows\)\)', src)
    if not m:
        raise AssertionError("lese_rangliste ERSTE_DATEN_ZEILE nicht gefunden")
    return int(m.group(1))


def check(label: str, got: int, expected: int) -> bool:
    if got == expected:
        print(f'  ✓  {label}: {got}')
        return True
    print(f'  ❌ {label}: Python={got}, Java-erwartet={expected}')
    return False


def main():
    print('=' * 70)
    print('  Sync-Check: analyse_supermelee_spieltag.py ↔ Java-Konstanten')
    print('=' * 70)

    spielrunde_src = SPIELRUNDE_KONST.read_text()
    delegate_src = RANGLISTE_DELEGATE.read_text()
    summen_src = SUMMEN_SPALTEN.read_text()
    sheet_src = RANGLISTE_SHEET.read_text()
    analyse_src = ANALYSE_SKRIPT.read_text()

    errors = 0

    # --- Spielrunden-Sheet-Konstanten ---
    print('\n--- Spielrunden-Sheet-Layout ---')
    j_erste_daten = java_int_const(spielrunde_src, 'ERSTE_DATEN_ZEILE')
    j_spielernr = java_int_const(spielrunde_src, 'ERSTE_SPIELERNR_SPALTE')
    j_paarung_cntr = java_int_const(spielrunde_src, 'PAARUNG_CNTR_SPALTE')  # = j_spielernr - 1
    j_ergebnisse = java_int_const(spielrunde_src, 'ERSTE_SPALTE_ERGEBNISSE')
    j_letzte = java_int_const(spielrunde_src, 'LETZTE_SPALTE')

    p_erste_daten = py_int_const(analyse_src, 'ERSTE_DATEN_ZEILE')
    p_spielernr = py_int_const(analyse_src, 'ERSTE_SPIELERNR_SPALTE')
    p_paarung_cntr = py_int_const(analyse_src, 'PAARUNG_CNTR_SPALTE')
    p_ergebnisse = py_int_const(analyse_src, 'ERSTE_SPALTE_ERGEBNISSE')

    if not check('ERSTE_DATEN_ZEILE (Spielrunde)', p_erste_daten, j_erste_daten):
        errors += 1
    if not check('ERSTE_SPIELERNR_SPALTE', p_spielernr, j_spielernr):
        errors += 1
    if not check('PAARUNG_CNTR_SPALTE', p_paarung_cntr, j_paarung_cntr):
        errors += 1
    if not check('ERSTE_SPALTE_ERGEBNISSE', p_ergebnisse, j_ergebnisse):
        errors += 1

    # --- Rangliste-Sheet-Konstanten (für ANZ_RUNDEN = 4) ---
    print('\n--- Rangliste-Sheet-Layout (ANZ_RUNDEN=4) ---')
    j_erste_spielrunde = java_int_const(delegate_src, 'ERSTE_SPIELRUNDE_SPALTE')
    j_rl_daten_zeile = java_int_const(delegate_src, 'ERSTE_DATEN_ZEILE')

    j_spiele_plus_offs = java_int_const(summen_src, 'SPIELE_PLUS_OFFS')
    j_spiele_div_offs = java_int_const(summen_src, 'SPIELE_DIV_OFFS')
    j_punkte_plus_offs = java_int_const(summen_src, 'PUNKTE_PLUS_OFFS')
    j_punkte_minus_offs = java_int_const(summen_src, 'PUNKTE_MINUS_OFFS')
    j_punkte_div_offs = java_int_const(summen_src, 'PUNKTE_DIV_OFFS')
    j_erste_sort_offset = java_int_const(sheet_src, 'ERSTE_SORTSPALTE_OFFSET')

    anz_runden = 4
    erste_summe = j_erste_spielrunde + anz_runden * 2
    letzte_spalte = erste_summe + j_punkte_div_offs
    manuell_sort = letzte_spalte + j_erste_sort_offset

    p_rl_daten_zeile = py_rangliste_daten_zeile(analyse_src)
    if not check('ERSTE_DATEN_ZEILE (Rangliste)', p_rl_daten_zeile, j_rl_daten_zeile):
        errors += 1

    # Σ+, Σ−, Δ aus originalen Summen-Spalten
    if not check('Rangliste Σ+ Spalte', py_rangliste_col(analyse_src, 'plus'),
                 erste_summe + j_punkte_plus_offs):
        errors += 1
    if not check('Rangliste Σ− Spalte', py_rangliste_col(analyse_src, 'minus'),
                 erste_summe + j_punkte_minus_offs):
        errors += 1
    if not check('Rangliste Δ Spalte', py_rangliste_col(analyse_src, 'delta'),
                 erste_summe + j_punkte_div_offs):
        errors += 1

    # Siege und Punkte aus den ManuellSort-Spalten (Kopie der Sortierschlüssel)
    # ManuellSortSpalte = letzteSummeSpalte + ERSTE_SORTSPALTE_OFFSET
    # Die Reihenfolge der Sortierschlüssel: Siege (SPIELE_PLUS_OFFS=0), Spielediff (SPIELE_DIV_OFFS=2)
    if not check('Rangliste Siege Spalte (ManuellSort+0)', py_rangliste_col(analyse_src, 'siege'),
                 manuell_sort + j_spiele_plus_offs):
        errors += 1
    if not check('Rangliste Punkte Spalte (ManuellSort+1)', py_rangliste_col(analyse_src, 'punkte'),
                 manuell_sort + j_spiele_div_offs - 1):
        errors += 1

    print()
    if errors:
        print(f'❌ {errors} Abweichungen gefunden — bitte analyse_supermelee_spieltag.py anpassen!')
        return 1
    print(f'✓ Alle Konstanten synchron ({anz_runden} Spielrunden konfiguriert)')
    return 0


if __name__ == '__main__':
    sys.exit(main())
