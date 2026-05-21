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
import uno  # noqa: F401  -- importiert pyuno, muss vor com.sun.star-Imports kommen
from com.sun.star.beans import PropertyValue
from com.sun.star.connection import NoConnectException

CONNECT_URL = (
    "uno:socket,host=127.0.0.1,port=2083;urp;"
    "StarOffice.ComponentContext"
)
# Per-Operation-Freeze-Thresholds in Sekunden. Turnier-Erstellung darf
# legitime Minute(n) dauern; Sheet-Switches / Property-Reads müssen schnell
# sein — wenn die hängen, ist der echte Freeze da.
FREEZE_THRESHOLD_DEFAULT_S = 10.0
FREEZE_THRESHOLD_DISPATCH_S = 180.0
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


class timed_call:
    """Context manager (Klasse, NICHT @contextmanager-Generator). Pyuno hat
    einen Bug, dass contextlib.contextmanager in __exit__ versucht
    exc.__traceback__ auf der UNO-Exception zu setzen, was den
    UNO-Struct-Setter sprengt ('Couldn't convert traceback object to UNO
    type'). Class-based __exit__ vermeidet das.
    """

    def __init__(self, log, label, freeze_flag_path,
                 threshold_s=FREEZE_THRESHOLD_DEFAULT_S):
        self.log = log
        self.label = label
        self.freeze_flag = freeze_flag_path
        self.threshold = threshold_s

    def __enter__(self):
        self.start = time.time()
        self.done_event = threading.Event()
        t = threading.Thread(target=self._watchdog, daemon=True)
        t.start()
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.done_event.set()
        elapsed = time.time() - self.start
        if exc_type is None:
            self.log.write(f"{self.label}  dauer={elapsed:.2f}s")
        else:
            self.log.write(
                f"{self.label}  dauer={elapsed:.2f}s  EXC "
                f"{exc_type.__name__}: {exc_val}"
            )
        return False  # Exception weiterreichen

    def _watchdog(self):
        while not self.done_event.wait(1.0):
            elapsed = time.time() - self.start
            if elapsed > self.threshold:
                self.log.write(
                    f"FREEZE-VERDACHT bei '{self.label}': {elapsed:.1f}s "
                    f"blockiert (threshold {self.threshold:.0f}s)"
                )
                try:
                    open(self.freeze_flag, "a", encoding="utf-8").close()
                except OSError:
                    pass
                return


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


def dispatch(ctx, frame, url, log, freeze_flag, threshold_s=FREEZE_THRESHOLD_DEFAULT_S):
    smgr = ctx.ServiceManager
    helper = smgr.createInstanceWithContext(
        "com.sun.star.frame.DispatchHelper", ctx
    )
    with timed_call(log, f"dispatch {url}", freeze_flag, threshold_s):
        helper.executeDispatch(frame, url, "", 0, ())


def wait_for_idle(ptm_log_path, log, max_wait_s=300.0, quiet_s=5.0,
                  fallback_sleep_s=90.0):
    """Wartet, bis das PTM-Log fuer mindestens quiet_s Sekunden nicht mehr
    waechst (Quiescence) -- robusterer Indikator als '**FERTIG**' (das kommt
    pro Spieltag, nicht nur am Ende).

    Fallback, falls Log nicht erreichbar: fester Sleep.
    """
    if not ptm_log_path or not os.path.exists(ptm_log_path):
        log.write(f"wait_for_idle: PTM-Log fehlt ({ptm_log_path}) "
                  f"-- fallback sleep {fallback_sleep_s}s")
        time.sleep(fallback_sleep_s)
        return
    deadline = time.time() + max_wait_s
    last_size = -1
    last_change = time.time()
    while time.time() < deadline:
        try:
            sz = os.path.getsize(ptm_log_path)
        except OSError as e:
            log.write(f"wait_for_idle: getsize-Fehler {e} -- fallback sleep")
            time.sleep(fallback_sleep_s)
            return
        now = time.time()
        if sz != last_size:
            last_size = sz
            last_change = now
        elif now - last_change >= quiet_s:
            log.write(f"wait_for_idle: PTM-Log quiet seit {quiet_s}s (idle)")
            return
        time.sleep(0.5)
    log.write(f"WARN: PTM-Log nicht quiet binnen {max_wait_s}s")


def list_sheets(doc, log, freeze_flag):
    with timed_call(log, "list-sheets", freeze_flag):
        sheets = doc.getSheets()
        names = [sheets.getByIndex(i).getName() for i in range(sheets.getCount())]
    return names


def activate_sheet(doc, name, log, freeze_flag):
    sheets = doc.getSheets()
    if not sheets.hasByName(name):
        log.write(f"activate '{name}'  uebersprungen (Sheet weg)")
        return
    sheet = sheets.getByName(name)
    controller = doc.getCurrentController()
    with timed_call(log, f"activate '{name}'", freeze_flag):
        controller.setActiveSheet(sheet)
    with timed_call(log, f"liveness-read '{name}'", freeze_flag):
        _ = controller.getFrame().getTitle()


def sheet_storm(doc, log, freeze_flag, rng):
    names = list_sheets(doc, log, freeze_flag)
    tags = ("spieltag", "spielrunde", "anmeldungen", "teilnehmer", "meldeliste")
    candidates = [n for n in names if any(t in n.lower() for t in tags)]
    if not candidates:
        log.write(f"sheet-storm: keine Kandidaten in {names!r}")
        return
    log.write(f"sheet-storm: {len(candidates)} Kandidaten von {len(names)}")
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
            dispatch(
                ctx, frame, "ptm:SpieltagRanglisteSheet_TestDaten",
                log, freeze_flag, threshold_s=FREEZE_THRESHOLD_DISPATCH_S,
            )
            wait_for_idle(args.ptm_log, log)
            sheet_storm(doc, log, freeze_flag, rng)
            if os.path.exists(freeze_flag):
                log.write("Freeze-Flag gesetzt -- Abbruch der Iterationen.")
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
                        help="Pfad zum PTM-Plugin-Log (Quiescence-Detektion fuer wait_for_idle)")
    args = parser.parse_args()
    if not os.path.isdir(args.output_dir):
        print(f"output-dir existiert nicht: {args.output_dir}", file=sys.stderr)
        sys.exit(2)
    run(args)


if __name__ == "__main__":
    main()
