#!/usr/bin/env python3
"""
End-to-End-Repro für den Toolbar-Fokus-Bug
('Neues Turnier in neuer Datei' → Fokus springt auf doc1).

Ablauf:
  1. soffice mit UNO-Socket starten, 3 leere Calc-Docs öffnen.
  2. Dispatch `ptm:toolbar_neu_in_neuer_datei` aus dem MITTLEREN Doc.
  3. Erscheinenden Modal-Dialog via AT-SPI bedienen:
       - erste ListBox: select_child(0)
       - OK-Button: do_action(0)
  4. Nach Settle prüfen, auf welchem Frame der Fokus jetzt steht.

Voraussetzungen:
    pip install --user --break-system-packages python-xlib python-uinput evdev
    (AT-SPI kommt mit at-spi2-core + python3-gi vom System.)

Aufruf:
    DISPLAY=:0 python3 fokus_atspi_neues_turnier.py

Logs:
    /tmp/soffice-2206.log     (soffice-Konsole)
    ~/.petanqueturniermanager/info.log    (Plugin-Trace, [FOKUS-TRACE])
"""
from __future__ import annotations

import os
import sys
import time
import threading

# Helper-Modul liegt im gleichen Verzeichnis
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from lo_uno_helpers import (  # noqa: E402
    start_soffice, connect_uno, terminate_soffice, cleanup_environment,
    list_components, get_frame, frame_title, ensure_n_components, make_url,
    find_lo_app, select_listbox_item, click_button,
)

SOCKET_PORT = 2206
LOG_FILE = os.path.expanduser("~/.petanqueturniermanager/info.log")


def log(m: str) -> None:
    print(m, flush=True)


def main() -> int:
    cleanup_environment()

    # Plugin-Log auf jungfräulich setzen
    try:
        with open(LOG_FILE, "w") as f:
            f.write(f"=== fokus_atspi_neues_turnier {time.strftime('%H:%M:%S')} ===\n")
    except FileNotFoundError:
        pass

    proc = start_soffice(port=SOCKET_PORT)
    desktop = None
    try:
        ctx = connect_uno(port=SOCKET_PORT)
        log(">>> UNO verbunden")
        smgr = ctx.ServiceManager
        desktop = smgr.createInstanceWithContext(
            "com.sun.star.frame.Desktop", ctx)

        ensure_n_components(desktop, 3)
        comps = list_components(desktop)
        for i, c in enumerate(comps):
            log(f"   [{i}] frameTitle='{frame_title(c)}'")
        if len(comps) < 3:
            log("!! Erwartet >=3 Komponenten – Abbruch")
            return 1

        ziel_c = comps[1]
        ziel_f = get_frame(ziel_c)
        ziel_title = ziel_f.Title
        log(f">>> Dispatch-Quelle = frame[1] '{ziel_title}'")

        url = make_url(smgr, ctx, "ptm:toolbar_neu_in_neuer_datei")
        disp = ziel_f.queryDispatch(url, "_self", 0)
        if disp is None:
            log("!! kein Dispatcher gefunden")
            return 1

        # dispatch() blockiert, solange Modal offen → in Thread auslagern
        done = threading.Event()

        def do_dispatch() -> None:
            t0 = time.time()
            try:
                disp.dispatch(url, tuple())
            except Exception as e:
                log(f"!! dispatch FEHLER: {e}")
            log(f">>> dispatch() return nach {time.time() - t0:.2f}s")
            done.set()

        threading.Thread(target=do_dispatch, daemon=True).start()

        # Modal-Dialog kommt nach ~2s
        time.sleep(2.0)
        lo_app = None
        for _ in range(10):
            lo_app = find_lo_app()
            if lo_app:
                break
            time.sleep(0.5)
        if lo_app is None:
            log("!! LO-App via AT-SPI nicht gefunden")
            return 1

        log(">>> select_listbox_item(0)")
        ok_sel = select_listbox_item(lo_app, index=0)
        log(f"   → {ok_sel}")
        log(">>> click_button('OK')")
        ok_btn = click_button(lo_app, labels_lower=("ok",))
        log(f"   → {ok_btn}")

        if not done.wait(timeout=20):
            log("!! Dispatch TIMEOUT – Modal hängt")
            return 2

        # Settle: neues Doc + Meldeliste-Setup
        time.sleep(7.0)

        comps_after = list_components(desktop)
        for i, c in enumerate(comps_after):
            log(f"   [{i}] frameTitle='{frame_title(c)}'")

        cur_frame = desktop.getCurrentFrame()
        cur_title = cur_frame.Title if cur_frame else "<none>"
        log(f">>> getCurrentFrame.Title='{cur_title}'")

        if len(comps_after) <= len(comps):
            log(">>> !! kein neues Doc – Modal nicht bestätigt")
            return 3

        neuer = frame_title(comps_after[0])
        alter = frame_title(comps_after[-1])
        if cur_title == neuer:
            log(">>> ✅ FOKUS auf NEUEM Doc – Fix wirkt")
            return 0
        if cur_title == ziel_title:
            log(">>> ⚠ FOKUS auf aufrufendem Doc (Mitte) – fokussiereDokument greift nicht")
            return 10
        if cur_title == alter:
            log(">>> ❌ FOKUS auf ältestem Doc – BUG NICHT BEHOBEN")
            return 11
        log(f">>> ❓ FOKUS auf unbekanntem Frame: '{cur_title}'")
        return 12

    finally:
        log(">>> Beende soffice")
        if desktop is not None:
            terminate_soffice(desktop, proc)
        else:
            try:
                proc.terminate()
                proc.wait(timeout=5)
            except Exception:
                proc.kill()

        log("\n=== LOG-AUSZUG [FOKUS-TRACE] ===")
        try:
            with open(LOG_FILE) as f:
                for line in f:
                    if "FOKUS-TRACE" in line or "Neues Turnier" in line:
                        log(line.rstrip())
        except FileNotFoundError:
            log("(kein Plugin-Log)")


if __name__ == "__main__":
    sys.exit(main())
