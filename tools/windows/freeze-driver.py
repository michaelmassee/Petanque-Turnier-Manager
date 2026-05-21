"""
UNO-Driver für den Windows-Freeze-Repro.

Ausführen mit LO's eigenem Python:
    C:\\Program Files\\LibreOffice\\program\\python.exe freeze-driver.py
       --iterations 5 --output-dir <dir>

Erwartet, dass soffice mit URP-Listener auf 127.0.0.1:2083 läuft
(wird vom Master-Skript gestartet).

Loggt Schritt-für-Schritt nach <output-dir>/driver.log. Setzt
<output-dir>/freeze-detected.flag, wenn eine UNO-Operation länger
als FREEZE_THRESHOLD_S blockiert. Setzt <output-dir>/stop.flag, wenn
fertig (auch im Fehlerfall) — damit der Watcher sauber endet.
"""

import argparse
import os
import random
import sys
import threading
import time
from contextlib import contextmanager

import uno
from com.sun.star.beans import PropertyValue
from com.sun.star.connection import NoConnectException

CONNECT_URL = (
    "uno:socket,host=127.0.0.1,port=2083;urp;"
    "StarOffice.ComponentContext"
)
FREEZE_THRESHOLD_S = 10.0
SHEET_SWITCHES_PER_ITER = 20


class DriverLog:
    def __init__(self, path):
        self._path = path
        self._lock = threading.Lock()

    def write(self, msg):
        ts = time.strftime("%H:%M:%S", time.localtime()) + f".{int(time.time()*1000)%1000:03d}"
        line = f"{ts}  {msg}\n"
        with self._lock:
            with open(self._path, "a", encoding="utf-8") as f:
                f.write(line)
        print(line, end="", flush=True)


def make_props(pairs):
    out = []
    for k, v in pairs:
        p = PropertyValue()
        p.Name = k
        p.Value = v
        out.append(p)
    return tuple(out)


def connect(log):
    local_ctx = uno.getComponentContext()
    resolver = local_ctx.ServiceManager.createInstanceWithContext(
        "com.sun.star.bridge.UnoUrlResolver", local_ctx
    )
    last_exc = None
    for attempt in range(30):
        try:
            ctx = resolver.resolve(CONNECT_URL)
            log.write(f"connect ok (attempt {attempt+1})")
            return ctx
        except NoConnectException as e:
            last_exc = e
            time.sleep(1.0)
    raise RuntimeError(f"konnte UNO-Bridge nicht erreichen: {last_exc}")


@contextmanager
def timed_call(log, label, freeze_flag_path):
    """Wickelt einen UNO-Call so, dass langes Hängen einen Freeze-Marker setzt."""
    start = time.time()
    done_event = threading.Event()

    def watchdog():
        while not done_event.wait(1.0):
            elapsed = time.time() - start
            if elapsed > FREEZE_THRESHOLD_S:
                log.write(f"FREEZE-VERDACHT bei '{label}': {elapsed:.1f}s blockiert")
                try:
                    open(freeze_flag_path, "a", encoding="utf-8").close()
                except OSError:
                    pass
                return

    t = threading.Thread(target=watchdog, daemon=True)
    t.start()
    try:
        yield
    finally:
        done_event.set()
        log.write(f"{label}  dauer={time.time()-start:.2f}s")


def open_calc(ctx, log, freeze_flag):
    smgr = ctx.ServiceManager
    desktop = smgr.createInstanceWithContext(
        "com.sun.star.frame.Desktop", ctx
    )
    with timed_call(log, "openCalc", freeze_flag):
        doc = desktop.loadComponentFromURL(
            "private:factory/scalc", "_blank", 0,
            make_props([("Hidden", False)]),
        )
    return desktop, doc


def dispatch(ctx, frame, url, log, freeze_flag):
    smgr = ctx.ServiceManager
    helper = smgr.createInstanceWithContext(
        "com.sun.star.frame.DispatchHelper", ctx
    )
    with timed_call(log, f"dispatch {url}", freeze_flag):
        helper.executeDispatch(frame, url, "", 0, ())


def wait_for_fertig(log, ptm_log_path, timeout_s=180.0):
    """Wartet bis '**FERTIG**' frisch im PTM-Plugin-Log auftaucht."""
    if not ptm_log_path or not os.path.exists(ptm_log_path):
        log.write(f"ptm-log nicht gefunden ({ptm_log_path}) — fallback: sleep 30s")
        time.sleep(30)
        return
    start_size = os.path.getsize(ptm_log_path)
    deadline = time.time() + timeout_s
    while time.time() < deadline:
        time.sleep(2.0)
        try:
            with open(ptm_log_path, "rb") as f:
                f.seek(start_size)
                chunk = f.read().decode("utf-8", errors="replace")
            if "**FERTIG**" in chunk:
                log.write("ptm-log: **FERTIG** gesehen")
                return
        except OSError as e:
            log.write(f"ptm-log lese-fehler: {e}")
    log.write(f"WARN: **FERTIG** nicht binnen {timeout_s}s gesehen")


def list_sheets(doc, log, freeze_flag):
    with timed_call(log, "list-sheets", freeze_flag):
        sheets = doc.getSheets()
        names = [sheets.getByIndex(i).getName() for i in range(sheets.getCount())]
    return names


def activate_sheet(doc, name, log, freeze_flag):
    sheets = doc.getSheets()
    sheet = sheets.getByName(name)
    controller = doc.getCurrentController()
    with timed_call(log, f"activate '{name}'", freeze_flag):
        controller.setActiveSheet(sheet)
    with timed_call(log, f"liveness-read '{name}'", freeze_flag):
        _ = controller.getFrame().getTitle()


def sheet_storm(doc, log, freeze_flag, rng):
    names = list_sheets(doc, log, freeze_flag)
    candidates = [n for n in names if any(tag in n for tag in ("SPIELTAG", "SPIELRUNDE", "ANMELDUNGEN", "TEILNEHMER", "MELDELISTE"))]
    if not candidates:
        log.write(f"sheet-storm: keine Kandidaten in {names!r}")
        return
    log.write(f"sheet-storm: {len(candidates)} Kandidaten")
    for i in range(SHEET_SWITCHES_PER_ITER):
        name = rng.choice(candidates)
        activate_sheet(doc, name, log, freeze_flag)
        time.sleep(rng.uniform(0.2, 0.8))


def run(args):
    log = DriverLog(os.path.join(args.output_dir, "driver.log"))
    freeze_flag = os.path.join(args.output_dir, "freeze-detected.flag")
    stop_flag = os.path.join(args.output_dir, "stop.flag")
    rng = random.Random(args.seed)

    try:
        ctx = connect(log)
        desktop, doc = open_calc(ctx, log, freeze_flag)
        frame = doc.getCurrentController().getFrame()

        for it in range(1, args.iterations + 1):
            log.write(f"=== Iteration {it}/{args.iterations} ===")
            dispatch(ctx, frame, "ptm:SpieltagRanglisteSheet_TestDaten", log, freeze_flag)
            wait_for_fertig(log, args.ptm_log)
            sheet_storm(doc, log, freeze_flag, rng)
            if os.path.exists(freeze_flag):
                log.write("Freeze-Flag gesetzt — Abbruch der Iterationen.")
                break

        try:
            with timed_call(log, "doc.close", freeze_flag):
                doc.close(False)
        except Exception as e:
            log.write(f"doc.close fehlgeschlagen: {e}")
    except Exception as e:
        log.write(f"FATAL: {type(e).__name__}: {e}")
        import traceback
        log.write(traceback.format_exc())
        raise
    finally:
        open(stop_flag, "a", encoding="utf-8").close()
        log.write("stop.flag gesetzt — Driver Ende.")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output-dir", required=True)
    parser.add_argument("--iterations", type=int, default=5)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--ptm-log", default="",
                        help="Pfad zum PTM-Plugin-Log (für **FERTIG**-Polling)")
    args = parser.parse_args()
    if not os.path.isdir(args.output_dir):
        print(f"output-dir existiert nicht: {args.output_dir}", file=sys.stderr)
        sys.exit(2)
    run(args)


if __name__ == "__main__":
    main()
