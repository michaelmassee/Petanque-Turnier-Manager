#!/usr/bin/env python3
"""
Bug-Repro: PTM-Dispatch aus doc2 (mit doc1 als Beifang offen).
Default-Befehl 'ptm:neue_meldeliste' — kein Modal, ProcessBox + SheetRunner aktiv.

Ohne den Fix in SheetRunner.fokussiereArbeitsDokument() springt der LO-aktive
Frame innerhalb von ~100 ms nach `ProcessBox run` von doc2 auf doc1 und bleibt
dort. Mit dem Fix wechselt er nach SheetRunner-Ende zurück auf doc2.

Sample-Pfad: pollt `desktop.getCurrentFrame()` über mehrere Sekunden und gibt
den Title aus. Letzte Zeile sollte den aufrufenden Frame zeigen.
"""
from __future__ import annotations
import os, sys, time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from lo_uno_helpers import (
    start_soffice, connect_uno, terminate_soffice, cleanup_environment,
    list_components, get_frame, frame_title, ensure_n_components, make_url,
)

SOCKET_PORT = int(os.environ.get("LO_PORT", "2208"))
URL_DEFAULT = os.environ.get("PTM_URL", "ptm:neue_meldeliste")
LOG_FILE = os.path.expanduser("~/.petanqueturniermanager/info.log")


def log(m: str) -> None:
    print(m, flush=True)


def main() -> int:
    cleanup_environment()
    try:
        with open(LOG_FILE, "w") as f:
            f.write(f"=== fokus_dispatch_runner {time.strftime('%H:%M:%S')} ===\n")
    except FileNotFoundError:
        pass

    proc = start_soffice(port=SOCKET_PORT)
    desktop = None
    try:
        ctx = connect_uno(port=SOCKET_PORT)
        smgr = ctx.ServiceManager
        desktop = smgr.createInstanceWithContext("com.sun.star.frame.Desktop", ctx)
        ensure_n_components(desktop, 2)
        comps = list_components(desktop)
        for i, c in enumerate(comps):
            log(f"   [{i}] frameTitle='{frame_title(c)}'")
        if len(comps) < 2:
            log("!! <2 Docs"); return 1

        doc2_c = comps[0]
        doc2_f = get_frame(doc2_c); doc2_title = doc2_f.Title
        doc1_title = frame_title(comps[-1])
        log(f">>> doc2 (aufrufendes) = '{doc2_title}'")
        log(f">>> doc1 (älteres)     = '{doc1_title}'")
        log(f">>> URL                = '{URL_DEFAULT}'")

        url = make_url(smgr, ctx, URL_DEFAULT)
        disp = doc2_f.queryDispatch(url, "_self", 0)
        if disp is None:
            log("!! kein dispatcher"); return 1

        cur_pre = desktop.getCurrentFrame()
        log(f">>> AKTIV vor Dispatch = '{cur_pre.Title if cur_pre else None}'")

        log(">>> dispatch()")
        disp.dispatch(url, tuple())
        log(">>> dispatch() return")

        prev = 0
        for t in (3, 10, 30, 60, 90, 120):
            time.sleep(t - prev); prev = t
            cur = desktop.getCurrentFrame()
            cur_title = cur.Title if cur else "<none>"
            log(f">>> t+{t:>3}s AKTIV='{cur_title}'")

        cur_final = desktop.getCurrentFrame()
        final_title = cur_final.Title if cur_final else "<none>"
        if final_title == doc2_title:
            log(">>> ✅ FOKUS auf aufrufendem Doc – Fix wirkt")
            return 0
        if final_title == doc1_title:
            log(">>> ❌ FOKUS auf ältestem Doc – BUG (Fix wirkt nicht)")
            return 11
        log(f">>> ❓ FOKUS auf: '{final_title}'")
        return 12

    finally:
        log(">>> Beende soffice")
        if desktop is not None:
            terminate_soffice(desktop, proc, timeout=10)
        log("\n=== [FOKUS-TRACE] / SheetRunner LOG-AUSZUG ===")
        try:
            with open(LOG_FILE) as f:
                for line in f:
                    if any(s in line for s in
                           ("FOKUS-TRACE", "OnFocus", "OnUnfocus", "ProcessBox", "SheetRunner")):
                        log(line.rstrip())
        except FileNotFoundError:
            log("(kein Log)")


if __name__ == "__main__":
    sys.exit(main())
