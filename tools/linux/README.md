# tools/linux – LibreOffice-Fernsteuerung für End-to-End-Tests

Python-Skripte zum **automatisierten Steuern von LibreOffice Calc** unter
Linux (X11 *und* Wayland). Wird genutzt für Bug-Repros, die echte
Mehr-Fenster-Szenarien oder Modal-Dialoge der PTM-Extension benötigen –
also Dinge, die `BaseCalcUITest` nicht headless abbilden kann.

Nicht Teil des OXT-Builds.

## Voraussetzungen

```bash
# Wayland-tauglich: AT-SPI über GObject Introspection (System-Pakete)
sudo apt install python3-gi gir1.2-atspi-2.0 at-spi2-core libreoffice

# Hilfsmodule (pip --user, kein sudo nötig)
pip install --user --break-system-packages python-xlib python-uinput evdev
```

`python-uno` ist Teil der LibreOffice-Distribution (`/usr/lib/python3/dist-packages/uno.py`)
und sollte ohne weitere Installation per `import uno` funktionieren.

## Warum AT-SPI?

Unter **Wayland** erreichen `xdotool` / Xlib-`fake_input` / `python-uinput`
**modale LibreOffice-Dialoge nicht** – der Compositor leitet
Tastatur-Events nur an das tatsächlich fokussierte Wayland-Surface weiter,
und die UNO-Bridge hält den GUI-Thread nicht in der erwarteten Weise. AT-SPI
dagegen ist ein **In-Process-Accessibility-Kanal** und triggert Buttons
unabhängig vom Fokus.

`lo_uno_helpers.uinput_keyboard()` ist als optionaler Helfer vorhanden,
funktioniert aber nur zuverlässig auf reinen X11-Sessions.

## Dateien

| Datei | Zweck |
|---|---|
| `lo_uno_helpers.py` | Bibliothek: `start_soffice`, `connect_uno`, `find_lo_app`, `select_listbox_item`, `click_button`, `cleanup_environment` etc. |
| `fokus_atspi_neues_turnier.py` | End-to-End-Repro für den Toolbar-Fokus-Bug („Neues Turnier in neuer Datei", drei Docs, Dispatch aus Mitte, OK via AT-SPI, Fokus-Check). |
| `fokus_dispatch_runner.py` | Repro für den SheetRunner-Fokus-Bug („normale" Toolbar-Aktion mit ProcessBox-Lauf, zwei Docs, modal-frei via `ptm:neue_meldeliste`). Erwartet nach Fix: letzte Polling-Zeile zeigt aufrufendes Doc. URL überstellbar via `PTM_URL=…`. |
| `dispatch_routing_test.py` | Diagnose: pro Frame ein non-modaler Dispatch, zeigt im `[FOKUS-TRACE]`-Log welcher `ProtocolHandler` welche Aktion bekommt. |

## Typischer Lauf

```bash
# 0) Extension installieren (falls noch nicht)
cd /pfad/zur/Petanque-Turnier-Manager
./gradlew reinstallExtension

# 1) Fokus-Bug reproduzieren / Fix verifizieren
DISPLAY=:0 python3 tools/linux/fokus_atspi_neues_turnier.py
```

Erwartete letzte Zeile bei wirksamem Fix:

```
>>> ✅ FOKUS auf NEUEM Doc – Fix wirkt
```

Logs:

| Datei | Inhalt |
|---|---|
| `/tmp/soffice-<port>.log` | soffice stdout/stderr |
| `~/.petanqueturniermanager/info.log` | Plugin-Trace, Zeilen mit `[FOKUS-TRACE]` |

## Hilfsbibliothek-Beispiel

```python
from lo_uno_helpers import (
    cleanup_environment, start_soffice, connect_uno, terminate_soffice,
    ensure_n_components, list_components, frame_title, make_url,
    find_lo_app, select_listbox_item, click_button,
)

cleanup_environment()
proc = start_soffice(port=2210)
try:
    ctx = connect_uno(port=2210)
    smgr = ctx.ServiceManager
    desktop = smgr.createInstanceWithContext(
        "com.sun.star.frame.Desktop", ctx)
    ensure_n_components(desktop, 3)
    # ... eigener Test ...
finally:
    terminate_soffice(desktop, proc)
```

## Hängende Instanzen aufräumen

```bash
pkill -9 -f soffice
rm -f ~/.config/libreoffice/4/.lock
```

Oder bequemer aus Python: `cleanup_environment()` (macht beides).
