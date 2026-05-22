#!/usr/bin/env python3
"""
Diagnose: Welche ProtocolHandler-Instanz verarbeitet einen Dispatch von
welchem Frame? Verifiziert das initialize()/dispatch()-Routing OHNE Modal.

Öffnet 3 leere Calc-Docs, dispatched einen NON-MODAL-Befehl
(`ptm:timer_pause_fortsetzen`) auf jedem Frame, und gibt den
[FOKUS-TRACE]-Auszug aus dem Plugin-Log aus.

Erwartung (Fix wirkt):
    initialize:    je ein Handler pro Frame, frameTitle stimmt
    dispatch:      handlerHash + frameTitle stimmen pro Frame

Aufruf:
    python3 dispatch_routing_test.py
"""
from __future__ import annotations

import os
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from lo_uno_helpers import (  # noqa: E402
    start_soffice, connect_uno, terminate_soffice, cleanup_environment,
    list_components, get_frame, frame_title, ensure_n_components, make_url,
)

SOCKET_PORT = 2204
LOG_FILE = os.path.expanduser("~/.petanqueturniermanager/info.log")


def log(m: str) -> None:
    print(m, flush=True)


def main() -> int:
    cleanup_environment()
    try:
        with open(LOG_FILE, "w") as f:
            f.write(f"=== dispatch_routing_test {time.strftime('%H:%M:%S')} ===\n")
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

        url = make_url(smgr, ctx, "ptm:timer_pause_fortsetzen")
        for i, c in enumerate(comps):
            f = get_frame(c)
            log(f">>> dispatch von frame[{i}] '{f.Title}'")
            disp = f.queryDispatch(url, "_self", 0)
            disp.dispatch(url, tuple())
            time.sleep(0.5)

        time.sleep(1.0)
        return 0
    finally:
        log(">>> Beende")
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
                    if "FOKUS-TRACE" in line:
                        log(line.rstrip())
        except FileNotFoundError:
            log("(kein Plugin-Log)")


if __name__ == "__main__":
    sys.exit(main())
