Noch gespeichert in GlobalProperties

- Plugin-/Optionswerte: autosave, backup, newversioncheck, prozessbox.automatisch.anzeigen, prozessbox.automatisch.schliessen, performance.logging, loglevel
    - Primär über LibreOffice-Konfiguration, Fallback Legacy-Datei.
    - Code: src/main/java/de/petanqueturniermanager/comp/GlobalProperties.java:736, src/main/java/de/petanqueturniermanager/comp/LibreOfficePluginOptionenSpeicher.java:74

- Webserver-Regie:
    - Primär über LibreOffice-Konfiguration in der Node WebserverRegie.
    - Properties: Active, Port, TargetsJson, LegacyPropertiesImported
    - Fallback Legacy-Datei nur ohne LO-Kontext oder bei LO-Konfigurationsfehler.
    - Legacy-Keys werden nach erfolgreichem Import nicht mehr in die Datei geschrieben:
        - webserver_regie_aktiv
        - webserver_regie_port
        - webserver_regie_ziele
    - Aktiv und Port sind in der eigenen LO-Optionsseite "Webserver-Regie" konfiguriert; Ziele bleiben in der Webserver-Regie-Sidebar.
    - Code: src/main/java/de/petanqueturniermanager/comp/GlobalProperties.java, src/main/java/de/petanqueturniermanager/comp/LibreOfficeWebserverRegieSpeicher.java

- Turnier-Startseite:
    - startseite_port
    - startseite_aktiv
    - startseite_zoom
    - Code: src/main/java/de/petanqueturniermanager/comp/GlobalProperties.java:467

- Startup-Modus:
    - startup.turnier.modus
    - Code: src/main/java/de/petanqueturniermanager/comp/GlobalProperties.java:497

- Composite-Webserver-Views:
    - webserver_aktiv
    - webserver_composite_ports
    - pro Port: _aktiv, _name, _zoom, _mit_header_footer, _layout, _panel_count
    - pro Panel: _typ, _sheet oder _url, _zoom, _sichtbarer_tabellenanteil, _halign, _valign, _blattname
    - Code: src/main/java/de/petanqueturniermanager/comp/GlobalProperties.java:655

- Timer-Einstellungen:
    - timer_letzte_dauer
    - timer_letzter_port
    - timer_letzte_bezeichnung
    - timer_hintergrundfarbe
    - Code: src/main/java/de/petanqueturniermanager/comp/GlobalProperties.java:911

- Upload-Passwörter pro Host:
    - upload.passwort.<host>
    - Code: src/main/java/de/petanqueturniermanager/comp/GlobalProperties.java:938

- Tab-Farben:
    - tabfarbe.<name>
    - Aktuell gibt es in GlobalProperties nur Lesen, keinen Setter. Existierende Werte bleiben aber in der Datei, weil beim Speichern die ganze propMap geschrieben wird.
    - Code: src/main/java/de/petanqueturniermanager/comp/GlobalProperties.java:767

Nicht mehr regulär gespeichert

- Alte Einzel-Port-Webserver-Keys werden beim Start entfernt:
    - webserver_ports
    - webserver_port_*
    - webserver_sheetnamen_anzeigen
    - Code: src/main/java/de/petanqueturniermanager/comp/GlobalProperties.java:58

- Webserver-Regie-Legacy-Keys werden nach erfolgreichem LO-Import entfernt:
    - webserver_regie_aktiv
    - webserver_regie_port
    - webserver_regie_ziele
    - Code: src/main/java/de/petanqueturniermanager/comp/GlobalProperties.java, src/main/java/de/petanqueturniermanager/comp/LibreOfficeWebserverRegieSpeicher.java
