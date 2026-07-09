## 🇩🇪 Deutsch
- Neu: Export als ein einziges Dokument (PDF/DOCX/ODT/Markdown) statt mehrerer Einzeldateien
- Neu: Automatischer Update-Dialog beim Programmstart, informiert über verfügbare neue Versionen
- Neu: Menüeintrag „Fehler melden" öffnet direkt die GitHub-Issues-Seite im Browser
- Plugin-Konfiguration (Tab-Farben, FTP-Server, Composite Views, Webserver-Regie, Timer) läuft jetzt vollständig über die nativen LibreOffice-Optionsseiten (Extras > Optionen > PétTurnMngr) statt eigener Dialoge
- FTP-Server-Verwaltung zentralisiert: eine gemeinsame Server-Liste, Verbindungstest-Button, Passwort-Klartext-Anzeige, erneute Passwortabfrage bei Loginfehler
- Diverse Deadlock-/Freeze-Fixes: Export-Format-Dialog, HTML-Export-Callback, Update-Dialog ohne offenes Dokument
- Diverse Stabilitäts-Fixes rund um Composite Views (Vorschau, OK/Abbrechen-Verhalten, geteilte Werte beim Moduswechsel)
- Interne Qualitätssicherung verschärft: SpotBugs-Gate deckt jetzt auch Findings mit niedriger Konfidenz ab

## 🇬🇧 English
- New: Export as a single document (PDF/DOCX/ODT/Markdown) instead of multiple separate files
- New: Automatic update dialog on startup, informs about available new versions
- New: "Report a bug" menu entry opens the GitHub issues page directly in the browser
- Plugin configuration (tab colors, FTP server, composite views, web server control, timer) now fully runs through native LibreOffice options pages (Tools > Options > PétTurnMngr) instead of custom dialogs
- Centralized FTP server management: one shared server list, connection test button, plaintext password display, re-prompt for password on login failure
- Several deadlock/freeze fixes: export format dialog, HTML export callback, update dialog without an open document
- Several stability fixes around composite views (preview, OK/Cancel behavior, shared values on mode switch)
- Internal QA hardened: SpotBugs gate now also covers low-confidence findings
