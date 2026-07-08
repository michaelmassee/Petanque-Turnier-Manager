Noch gespeichert in GlobalProperties

- Plugin-/Optionswerte: autosave, backup, newversioncheck, prozessbox.automatisch.anzeigen, prozessbox.automatisch.schliessen, performance.logging, loglevel
    - Primär über LibreOffice-Konfiguration, Fallback Legacy-Datei.
    - Code: src/main/java/de/petanqueturniermanager/comp/GlobalProperties.java:874 (speichern), src/main/java/de/petanqueturniermanager/comp/LibreOfficePluginOptionenSpeicher.java:74

- Webserver-Regie:
    - Primär über LibreOffice-Konfiguration in der Node WebserverRegie.
    - Properties: Active, Port, TargetsJson, LegacyPropertiesImported
    - Legacy-Datei ist Quelle nur bis zur erfolgreichen Migration: ohne LO-Kontext, oder bei LO-Fehler, bevor der Import erledigt ist (`LegacyPropertiesImported=false`). Nach erfolgreichem Import ist die LO-Konfiguration alleinige, persistente Quelle.
    - Legacy-Keys werden nach erfolgreichem Import einmalig aus der Datei entfernt und nicht mehr geschrieben:
        - webserver_regie_aktiv
        - webserver_regie_port
        - webserver_regie_ziele
    - Schlägt der LO-Read nach erfolgter Migration ausnahmsweise fehl, liefern die Getter für diese Sitzung Defaults (aktiv=true, Port 9090); die Werte bleiben in der LO-Konfiguration erhalten und werden bei der nächsten erfolgreichen Sitzung wieder gelesen. Es gibt bewusst keinen Legacy-Datei-Fallback mehr für die Regie.
    - Aktiv und Port sind in der eigenen LO-Optionsseite "Webserver-Regie" konfiguriert; Ziele bleiben in der Webserver-Regie-Sidebar.
    - Code: src/main/java/de/petanqueturniermanager/comp/GlobalProperties.java, src/main/java/de/petanqueturniermanager/comp/LibreOfficeWebserverRegieSpeicher.java

- Turnier-Startseite:
    - startseite_port
    - startseite_aktiv
    - startseite_zoom
    - Code: src/main/java/de/petanqueturniermanager/comp/GlobalProperties.java:570 (getStartseitePort), :586 (speichernStartseite)

- Startup-Modus:
    - startup.turnier.modus
    - Code: src/main/java/de/petanqueturniermanager/comp/GlobalProperties.java:613

- Composite-Webserver-Views:
    - webserver_aktiv
    - webserver_composite_ports
    - pro Port: _aktiv, _name, _zoom, _mit_header_footer, _layout, _panel_count, _rand_dicke, _rand_art, _rand_farbe, _rand_transparenz, _rand_animation
    - pro Panel: _typ, _sheet oder _url, _zoom, _sichtbarer_tabellenanteil, _halign, _valign, _blattname
    - Rand-Properties (Gesamtrahmen: Dicke/Art/Farbe/Transparenz/Animation) werden nur bei Abweichung vom Default (`RandKonfiguration.KEINER`) geschrieben (migrationssicher für Alt-Configs).
    - Verwaltung jetzt direkt auf der eigenen LibreOffice-Optionsseite Extras > Optionen > PétTurnMngr > Composite Views (kein separater `CompositeViewListeDialog` mehr); Detail-Konfiguration je Zeile über `CompositeViewDetailDialog`, "Übernehmen" persistiert sofort und benachrichtigt den laufenden Webserver live.
    - Code: src/main/java/de/petanqueturniermanager/comp/GlobalProperties.java:632 (getCompositeViewEintraege), :776 (speichernCompositeViews); src/main/java/de/petanqueturniermanager/webserver/RandKonfiguration.java; src/main/java/de/petanqueturniermanager/comp/CompositeViewsOptionsEventHandler.java

- Upload-Passwörter pro Host:
    - upload.passwort.<host>
    - Code: src/main/java/de/petanqueturniermanager/comp/GlobalProperties.java:1052

- Tab-Farben:
    - tabfarbe.<name>
    - Aktuell gibt es in GlobalProperties nur Lesen, keinen Setter. Existierende Werte bleiben aber in der Datei, weil beim Speichern die ganze propMap geschrieben wird.
    - Code: src/main/java/de/petanqueturniermanager/comp/GlobalProperties.java:905

Nicht mehr regulär gespeichert

- Alte Einzel-Port-Webserver-Keys werden beim Start entfernt:
    - webserver_ports
    - webserver_port_*
    - webserver_sheetnamen_anzeigen
    - Code: src/main/java/de/petanqueturniermanager/comp/GlobalProperties.java:60

- Webserver-Regie-Legacy-Keys werden nach erfolgreichem LO-Import entfernt:
    - webserver_regie_aktiv
    - webserver_regie_port
    - webserver_regie_ziele
    - Code: src/main/java/de/petanqueturniermanager/comp/GlobalProperties.java, src/main/java/de/petanqueturniermanager/comp/LibreOfficeWebserverRegieSpeicher.java

- Timer-Einstellungen-Legacy-Keys werden beim Start entfernt (keine Wertübernahme):
    - timer_letzte_dauer
    - timer_letzter_port
    - timer_letzte_bezeichnung
    - timer_hintergrundfarbe
    - Neue Werte liegen pro Dokument als UserDefined Document Properties (DocumentPropertiesHelper): Timer Letzte Dauer, Timer Letzter Port, Timer Letzte Bezeichnung, Timer Hintergrundfarbe.
    - Code: src/main/java/de/petanqueturniermanager/comp/GlobalProperties.java, src/main/java/de/petanqueturniermanager/timer/TimerDialog.java
