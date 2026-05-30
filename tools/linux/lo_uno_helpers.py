"""
Wiederverwendbare Helfer zum Fernsteuern von LibreOffice über UNO + AT-SPI.

Funktioniert unter X11 und Wayland (uinput/Xlib erreichen LO-Modal-Dialoge
unter Wayland nicht; AT-SPI dagegen schon).

Voraussetzungen (alle ohne sudo via pip --user installierbar):
    pip install --user --break-system-packages python-xlib python-uinput evdev
    # AT-SPI ist über das System-Paket gi/python3-gi + at-spi2-core verfügbar

Aufruf-Skelett:

    from lo_uno_helpers import (
        start_soffice, connect_uno, list_components, frame_title,
        find_lo_app, find_role, select_listbox_item, click_button,
    )

    proc = start_soffice(port=2202)
    try:
        ctx = connect_uno(port=2202)
        desktop = ctx.ServiceManager.createInstanceWithContext(
            "com.sun.star.frame.Desktop", ctx)
        ...
    finally:
        try: desktop.terminate()
        except Exception: pass
        proc.wait(timeout=5)
"""

from __future__ import annotations

import os
import signal
import subprocess
import time
from typing import Iterable, Optional

import uno  # type: ignore
from com.sun.star.beans import PropertyValue  # type: ignore
from com.sun.star.connection import NoConnectException  # type: ignore

# ---------------------------------------------------------------------------
# Subprozess / UNO-Verbindung
# ---------------------------------------------------------------------------

DEFAULT_SOFFICE = "/usr/lib/libreoffice/program/soffice"


def make_pv(name: str, value) -> PropertyValue:
    p = PropertyValue()
    p.Name = name
    p.Value = value
    return p


def start_soffice(*, port: int = 2202, soffice: str = DEFAULT_SOFFICE,
                  with_calc: bool = True, extra_args: Iterable[str] = (),
                  log_path: Optional[str] = None) -> subprocess.Popen:
    """
    Startet eine soffice-Instanz mit UNO-Socket. Gibt das Popen-Objekt zurück.
    log_path: wohin soffice-stdout/stderr läuft (Default: /tmp/soffice-<port>.log).
    """
    cmd = [
        soffice,
        f"--accept=socket,host=localhost,port={port};urp;",
        "--norestore", "--nologo", "--nofirststartwizard",
    ]
    if with_calc:
        cmd.append("--calc")
    cmd.extend(extra_args)
    if log_path is None:
        log_path = f"/tmp/soffice-{port}.log"
    logf = open(log_path, "w")
    return subprocess.Popen(cmd, stdout=logf, stderr=subprocess.STDOUT)


def connect_uno(*, port: int = 2202, timeout: float = 30.0):
    """Wartet bis soffice den Socket geöffnet hat, gibt XComponentContext zurück."""
    local_ctx = uno.getComponentContext()
    resolver = local_ctx.ServiceManager.createInstanceWithContext(
        "com.sun.star.bridge.UnoUrlResolver", local_ctx)
    url = (f"uno:socket,host=localhost,port={port};urp;"
           "StarOffice.ComponentContext")
    end = time.time() + timeout
    last = None
    while time.time() < end:
        try:
            return resolver.resolve(url)
        except NoConnectException as e:
            last = e
            time.sleep(0.5)
    raise RuntimeError(f"UNO-Connect Timeout nach {timeout}s: {last}")


def terminate_soffice(desktop, proc: subprocess.Popen, *, timeout: float = 5.0) -> None:
    """Sauberes Beenden: desktop.terminate(), dann TERM, dann KILL."""
    try:
        desktop.terminate()
    except Exception:
        pass
    try:
        proc.wait(timeout=timeout)
        return
    except Exception:
        pass
    proc.send_signal(signal.SIGTERM)
    try:
        proc.wait(timeout=timeout)
        return
    except Exception:
        pass
    proc.kill()


# ---------------------------------------------------------------------------
# UNO-Dokument/Frame-Komfort
# ---------------------------------------------------------------------------

def list_components(desktop) -> list:
    out = []
    e = desktop.Components.createEnumeration()
    while e.hasMoreElements():
        out.append(e.nextElement())
    return out


def get_frame(component):
    try:
        ctrl = component.getCurrentController()
        return ctrl.getFrame() if ctrl else None
    except Exception:
        return None


def frame_title(component) -> str:
    f = get_frame(component)
    if f is None:
        return "<no-frame>"
    try:
        return f.Title
    except Exception:
        return "<no-title>"


def open_blank_calc(desktop, *, hidden: bool = False):
    return desktop.loadComponentFromURL(
        "private:factory/scalc", "_blank", 0,
        tuple([make_pv("Hidden", hidden)]))


def ensure_n_components(desktop, n: int, *, settle: float = 0.5) -> list:
    """Öffnet so viele leere Calc-Docs, bis insgesamt mindestens n offen sind."""
    for _ in range(30):
        if list_components(desktop):
            break
        time.sleep(0.3)
    while len(list_components(desktop)) < n:
        open_blank_calc(desktop)
        time.sleep(settle)
    return list_components(desktop)


def make_url(smgr, ctx, complete: str):
    url = uno.createUnoStruct("com.sun.star.util.URL")
    url.Complete = complete
    tr = smgr.createInstanceWithContext("com.sun.star.util.URLTransformer", ctx)
    _, url = tr.parseStrict(url)
    return url


# ---------------------------------------------------------------------------
# AT-SPI (Wayland-tauglich)
# ---------------------------------------------------------------------------

def atspi_init():
    import gi
    gi.require_version("Atspi", "2.0")
    from gi.repository import Atspi
    Atspi.init()
    return Atspi


def find_lo_app(*, name_substr: tuple = ("soffice", "libreoffice")):
    Atspi = atspi_init()
    desktop = Atspi.get_desktop(0)
    for i in range(desktop.get_child_count()):
        try:
            app = desktop.get_child_at_index(i)
            nm = (app.get_name() or "").lower()
        except Exception:
            continue
        if any(s in nm for s in name_substr):
            return app
    return None


def walk_atspi(acc, *, depth: int = 0, maxdepth: int = 10):
    """Liefert (depth, accessible) für jeden Knoten im Teilbaum (Tiefenrekursion)."""
    yield depth, acc
    if depth >= maxdepth:
        return
    try:
        n = acc.get_child_count()
    except Exception:
        return
    for i in range(n):
        try:
            ch = acc.get_child_at_index(i)
        except Exception:
            continue
        if ch is None:
            continue
        yield from walk_atspi(ch, depth=depth + 1, maxdepth=maxdepth)


def find_role(root, role_names: tuple, *, name_lower_in: Optional[tuple] = None,
              maxdepth: int = 10):
    """
    Erster Knoten unterhalb root mit get_role_name() in role_names; optional
    Filter auf den (kleinen) Namen.
    """
    for _, acc in walk_atspi(root, maxdepth=maxdepth):
        try:
            if acc.get_role_name() not in role_names:
                continue
        except Exception:
            continue
        if name_lower_in is None:
            return acc
        try:
            nm = (acc.get_name() or "").strip().lower()
        except Exception:
            nm = ""
        if nm in name_lower_in:
            return acc
    return None


def select_listbox_item(root, *, index: int = 0) -> bool:
    """Findet erste ListBox/ComboBox unter root und selektiert Element index."""
    lb = find_role(root, ("list box", "combo box", "list"))
    if lb is None:
        return False
    try:
        sel = lb.get_selection_iface()
        if sel is None:
            return False
        return bool(sel.select_child(index))
    except Exception:
        return False


def click_button(root, *, labels_lower: tuple = ("ok",)) -> bool:
    """Findet ersten Push-Button mit passendem Label und löst die Default-Action aus."""
    btn = find_role(root, ("push button",), name_lower_in=labels_lower)
    if btn is None:
        return False
    try:
        ai = btn.get_action_iface()
        if ai and ai.get_n_actions() > 0:
            ai.do_action(0)
            return True
    except Exception:
        pass
    return False


# ---------------------------------------------------------------------------
# Optional: Tastatur/Maus via uinput (X11 OK; Wayland-Modale empfangen oft NICHT)
# ---------------------------------------------------------------------------

def uinput_keyboard():
    """
    Erstellt ein virtuelles uinput-Tastatur/Maus-Device. /dev/uinput muss
    schreibbar sein (z.B. via ACL für die Gruppe 'input').
    Aufrufer ist für device.destroy() verantwortlich.
    """
    import uinput  # type: ignore
    dev = uinput.Device(
        [uinput.KEY_ENTER, uinput.KEY_ESC, uinput.KEY_TAB,
         uinput.BTN_LEFT, uinput.REL_X, uinput.REL_Y],
        name="lo_uno_helpers_virtual")
    time.sleep(0.3)
    return dev


def uinput_press(dev, keysym_attr: str) -> None:
    """dev.emit_click(uinput.<keysym_attr>)."""
    import uinput  # type: ignore
    dev.emit_click(getattr(uinput, keysym_attr))


# ---------------------------------------------------------------------------
# Hygiene: stale lock + Reste killen
# ---------------------------------------------------------------------------

def cleanup_environment(*, lock_path: str = None) -> None:
    """
    Entfernt stale LibreOffice-Lock und killt verwaiste soffice-Prozesse.
    Aufruf VOR start_soffice() empfohlen, wenn ein vorheriger Lauf hängengeblieben ist.
    """
    if lock_path is None:
        lock_path = os.path.expanduser("~/.config/libreoffice/4/.lock")
    try:
        os.remove(lock_path)
    except FileNotFoundError:
        pass
    subprocess.run(["pkill", "-9", "-f", "soffice.bin"], check=False)
    time.sleep(1.0)
