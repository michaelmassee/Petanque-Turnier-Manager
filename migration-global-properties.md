# Migration GlobalProperties: Legacy-Datei → LibreOffice-Konfiguration

## Ziel

`GlobalProperties` speichert historisch alle Plugin-Einstellungen in einer einzigen Legacy-Properties-Datei
`PetanqueTurnierManager.properties` im User-Home (`user.home`). Ziel der laufenden Migration ist es, diese Datei
schrittweise durch die native LibreOffice-Konfiguration (XCU-Nodes unter Extras > Optionen > PétTurnMngr) zu
ersetzen.

## Stand: hybrid

`GlobalProperties` speichert aktuell **hybrid** — je Bereich unterschiedlich weit migriert:

- **Vollständig auf LO-Konfiguration umgestellt** (kein Legacy-Fallback mehr): Webserver-Regie.
- **Primär LO-Konfiguration, mit Legacy-Datei-Fallback** (falls kein LO-Kontext verfügbar, z.B. in reinen
  Unit-Tests ohne laufendes LibreOffice): Plugin-/Optionswerte, FTP-Server.
- **Noch ausschließlich Legacy-Datei**, keine LO-Konfiguration vorgesehen: Turnier-Startseite, Startup-Modus,
  Composite-Webserver-Views, Tab-Farben.

Die Legacy-Datei bleibt also so lange relevant, wie nicht jeder Bereich einzeln migriert wurde. Einmal migrierte
Alt-Keys werden beim Start einmalig aus der Datei entfernt (siehe „Nicht mehr regulär gespeichert" unten).

## Noch gespeichert in GlobalProperties

- Plugin-/Optionswerte: autosave, backup, newversioncheck, prozessbox.automatisch.anzeigen, prozessbox.automatisch.schliessen, performance.logging, loglevel
    - Primär über LibreOffice-Konfiguration, Fallback Legacy-Datei.
    - Code: src/main/java/de/petanqueturniermanager/comp/GlobalProperties.java:1081 (speichern), src/main/java/de/petanqueturniermanager/comp/LibreOfficePluginOptionenSpeicher.java:84

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
    - Code: src/main/java/de/petanqueturniermanager/comp/GlobalProperties.java:777 (getStartseitePort), :793 (speichernStartseite)

- Startup-Modus:
    - startup.turnier.modus
    - Code: src/main/java/de/petanqueturniermanager/comp/GlobalProperties.java:819 (isStartupTurnierModus), :823 (setStartupTurnierModus)

- Composite-Webserver-Views:
    - webserver_aktiv
    - webserver_composite_ports
    - pro Port: _aktiv, _name, _zoom, _mit_header_footer, _layout, _panel_count, _rand_dicke, _rand_art, _rand_farbe, _rand_transparenz, _rand_animation
    - pro Panel: _typ, _sheet oder _url, _zoom, _sichtbarer_tabellenanteil, _halign, _valign, _blattname
    - Rand-Properties (Gesamtrahmen: Dicke/Art/Farbe/Transparenz/Animation) werden nur bei Abweichung vom Default (`RandKonfiguration.KEINER`) geschrieben (migrationssicher für Alt-Configs).
    - Verwaltung jetzt direkt auf der eigenen LibreOffice-Optionsseite Extras > Optionen > PétTurnMngr > Composite Views (kein separater `CompositeViewListeDialog` mehr); Detail-Konfiguration je Zeile über `CompositeViewDetailDialog`, "Übernehmen" persistiert sofort und benachrichtigt den laufenden Webserver live.
    - Code: src/main/java/de/petanqueturniermanager/comp/GlobalProperties.java:839 (getCompositeViewEintraege), :983 (speichernCompositeViews); src/main/java/de/petanqueturniermanager/webserver/RandKonfiguration.java; src/main/java/de/petanqueturniermanager/comp/CompositeViewsOptionsEventHandler.java

- FTP-Server (zentrale Liste, primär über LibreOffice-Konfiguration):
    - Node FtpServer, Property ServersJson (JSON-Liste von GlobalProperties.FtpServerEintrag,
      inkl. Name, Protokoll, Host, Port, Benutzer, Passwort, Ziel-Verzeichnis).
    - Kein Legacy-Import: die bisherigen Upload-Einstellungen lagen pro Dokument
      (Document Properties je Turniersystem), nicht global — daher kein Migrationspfad,
      Liste startet leer.
    - Verwaltung auf eigener LibreOffice-Optionsseite Extras > Optionen > PétTurnMngr > FTP-Server;
      Detail-Konfiguration je Server über FtpServerDetailDialog.
    - Beim Upload wählt der Nutzer über FtpServerAuswahlDialog einen Server aus der Liste;
      die Auswahl wird pro Dokument als Document Property "FTP Letzter Server" (Server-Id)
      gemerkt und beim nächsten Upload vorselektiert.
    - Das frühere host-basierte Upload-Passwort (GlobalProperties.getUploadPasswort/
      setUploadPasswort) ist entfallen — das Passwort ist jetzt Teil des Server-Objekts.
    - Code: src/main/java/de/petanqueturniermanager/comp/GlobalProperties.java (getFtpServerEintraege,
      speichernFtpServer); src/main/java/de/petanqueturniermanager/comp/LibreOfficeFtpServerSpeicher.java;
      src/main/java/de/petanqueturniermanager/comp/FtpServerOptionsEventHandler.java;
      src/main/java/de/petanqueturniermanager/helper/upload/{FtpServerDetailDialog,FtpServerAuswahlDialog,AbstractFtpUpload}.java

- Tab-Farben:
    - tabfarbe.<name>
    - Aktuell gibt es in GlobalProperties nur Lesen, keinen Setter. Existierende Werte bleiben aber in der Datei, weil beim Speichern die ganze propMap geschrieben wird.
    - Code: src/main/java/de/petanqueturniermanager/comp/GlobalProperties.java:1112 (getTabFarbe)

## Nicht mehr regulär gespeichert

- Alte Einzel-Port-Webserver-Keys werden beim Start entfernt:
    - webserver_ports
    - webserver_port_*
    - webserver_sheetnamen_anzeigen
    - Code: src/main/java/de/petanqueturniermanager/comp/GlobalProperties.java:333 (bereinigeLegacyEinzelPortProperties)

- Webserver-Regie-Legacy-Keys werden nach erfolgreichem LO-Import entfernt:
    - webserver_regie_aktiv
    - webserver_regie_port
    - webserver_regie_ziele
    - Code: src/main/java/de/petanqueturniermanager/comp/GlobalProperties.java:588 (bereinigeLegacyWebserverRegieProperties), src/main/java/de/petanqueturniermanager/comp/LibreOfficeWebserverRegieSpeicher.java

- Timer-Einstellungen-Legacy-Keys werden beim Start entfernt (keine Wertübernahme):
    - timer_letzte_dauer
    - timer_letzter_port
    - timer_letzte_bezeichnung
    - timer_hintergrundfarbe
    - Neue Werte liegen pro Dokument als UserDefined Document Properties (DocumentPropertiesHelper): Timer Letzte Dauer, Timer Letzter Port, Timer Letzte Bezeichnung, Timer Hintergrundfarbe.
    - Code: src/main/java/de/petanqueturniermanager/comp/GlobalProperties.java:360 (bereinigeLegacyTimerProperties), src/main/java/de/petanqueturniermanager/timer/TimerDialog.java
