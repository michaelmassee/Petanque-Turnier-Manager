# Migration GlobalProperties: Legacy-Datei → LibreOffice-Konfiguration

## Ziel

`GlobalProperties` speichert historisch alle Plugin-Einstellungen in einer einzigen Legacy-Properties-Datei
`PetanqueTurnierManager.properties` im User-Home (`user.home`). Ziel der laufenden Migration ist es, diese Datei
schrittweise durch die native LibreOffice-Konfiguration (XCU-Nodes unter Extras > Optionen > PétTurnMngr) zu
ersetzen.

## Stand: hybrid

`GlobalProperties` speichert aktuell **hybrid** — je Bereich unterschiedlich weit migriert:

- **Vollständig auf LO-Konfiguration umgestellt** (kein Legacy-Fallback mehr): Webserver-Regie.
- **Primär LO-Konfiguration beim Lesen, mit Legacy-Datei-Fallback** (falls kein LO-Kontext verfügbar, z.B. in
  reinen Unit-Tests ohne laufendes LibreOffice): Plugin-/Optionswerte, FTP-Server. Geschrieben wird aber trotzdem
  immer auch in die Legacy-Datei (siehe propMap-Mirror unten) — "primär LO" gilt hier nur für das Lesen.
- **Noch ausschließlich Legacy-Datei**, keine LO-Konfiguration vorgesehen: Turnier-Startseite, Startup-Modus,
  Composite-Webserver-Views, Tab-Farben.

Die Legacy-Datei bleibt also so lange relevant, wie nicht jeder Bereich einzeln migriert wurde. Einmal migrierte
Alt-Keys werden beim Start einmalig aus der Datei entfernt (siehe „Nicht mehr regulär gespeichert" unten).

### Wichtig: propMap ist ein Vollständig-Mirror

`speichernDatei()` (GlobalProperties.java:434) schreibt bei **jedem** Speichervorgang die **komplette**
`propMap` in die Datei — unabhängig davon, welcher Bereich gerade geändert wurde. Auch Werte, deren
"primäre" Quelle die LO-Konfiguration ist (Plugin-Optionen, FTP-Server-JSON), werden beim Laden in dieselbe
`propMap` gespiegelt und landen dadurch bei der nächsten beliebigen Speicherung (z.B. beim Speichern der
Composite-Views) erneut physisch in `PetanqueTurnierManager.properties`. Die einzige Ausnahme ist die
Webserver-Regie: ihre 3 Keys werden gezielt herausgefiltert (`istWebserverRegieLegacyKey`,
GlobalProperties.java:605), sobald die Migration nach LO erfolgreich war.

Die folgende Liste zeigt daher, was **tatsächlich noch in der Datei `PetanqueTurnierManager.properties`
steht** (nicht nur, was konzeptionell "primär" dort verwaltet wird).

## Noch (physisch) in PetanqueTurnierManager.properties gespeichert

- Plugin-/Optionswerte: autosave, backup, newversioncheck, prozessbox.automatisch.anzeigen, prozessbox.automatisch.schliessen, performance.logging, loglevel
    - Lesen: primär LibreOffice-Konfiguration, Fallback Legacy-Datei. Schreiben: landet trotzdem immer in der Datei (propMap-Mirror), auch wenn LO die eigentliche Quelle ist.
    - Code: src/main/java/de/petanqueturniermanager/comp/GlobalProperties.java:1081 (speichern), src/main/java/de/petanqueturniermanager/comp/LibreOfficePluginOptionenSpeicher.java:84

- Turnier-Startseite:
    - startseite_port
    - startseite_aktiv
    - startseite_zoom
    - Keine LO-Konfiguration vorgesehen, ausschließlich Legacy-Datei.
    - Code: src/main/java/de/petanqueturniermanager/comp/GlobalProperties.java:777 (getStartseitePort), :793 (speichernStartseite)

- Startup-Modus:
    - startup.turnier.modus
    - Keine LO-Konfiguration vorgesehen, ausschließlich Legacy-Datei.
    - Code: src/main/java/de/petanqueturniermanager/comp/GlobalProperties.java:819 (isStartupTurnierModus), :823 (setStartupTurnierModus)

- Composite-Webserver-Views:
    - webserver_aktiv
    - webserver_composite_ports
    - pro Port: _aktiv, _name, _zoom, _mit_header_footer, _layout, _panel_count, _rand_dicke, _rand_art, _rand_farbe, _rand_transparenz, _rand_animation
    - pro Panel: _typ, _sheet oder _url, _zoom, _sichtbarer_tabellenanteil, _halign, _valign, _blattname
    - Rand-Properties (Gesamtrahmen: Dicke/Art/Farbe/Transparenz/Animation) werden nur bei Abweichung vom Default (`RandKonfiguration.KEINER`) geschrieben (migrationssicher für Alt-Configs).
    - Keine LO-Konfiguration vorgesehen, ausschließlich Legacy-Datei. Die Optionsseite Extras > Optionen > PétTurnMngr > Composite Views ist nur UI, die Werte landen weiterhin in der Legacy-Datei.
    - Code: src/main/java/de/petanqueturniermanager/comp/GlobalProperties.java:839 (getCompositeViewEintraege), :983 (speichernCompositeViews); src/main/java/de/petanqueturniermanager/webserver/RandKonfiguration.java; src/main/java/de/petanqueturniermanager/comp/CompositeViewsOptionsEventHandler.java

- FTP-Server (zentrale Liste):
    - Legacy-Key: ftp_server_liste (JSON-Liste von GlobalProperties.FtpServerEintrag,
      inkl. Name, Protokoll, Host, Port, Benutzer, Passwort, Ziel-Verzeichnis).
    - Lesen: primär LibreOffice-Konfiguration (Node FtpServer, Property ServersJson). Schreiben: landet
      trotzdem als Mirror in der Legacy-Datei (propMap-Vollschreiber), sobald irgendein anderer Bereich
      gespeichert wird.
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
    - Code: src/main/java/de/petanqueturniermanager/comp/GlobalProperties.java:737 (getFtpServerEintraege),
      :752 (speichernFtpServer); src/main/java/de/petanqueturniermanager/comp/LibreOfficeFtpServerSpeicher.java;
      src/main/java/de/petanqueturniermanager/comp/FtpServerOptionsEventHandler.java;
      src/main/java/de/petanqueturniermanager/helper/upload/{FtpServerDetailDialog,FtpServerAuswahlDialog,AbstractFtpUpload}.java

- Tab-Farben:
    - tabfarbe.<name>
    - Keine LO-Konfiguration vorgesehen, ausschließlich Legacy-Datei. Aktuell gibt es in GlobalProperties nur Lesen, keinen Setter. Existierende Werte bleiben aber in der Datei, weil beim Speichern die ganze propMap geschrieben wird.
    - Code: src/main/java/de/petanqueturniermanager/comp/GlobalProperties.java:1112 (getTabFarbe)

## Nicht mehr in der Datei (nur noch LO-Konfiguration)

- Webserver-Regie:
    - Ausschließlich über LibreOffice-Konfiguration in der Node WebserverRegie.
    - Properties: Active, Port, TargetsJson, LegacyPropertiesImported
    - Legacy-Datei ist Quelle nur bis zur erfolgreichen Migration: ohne LO-Kontext, oder bei LO-Fehler, bevor der Import erledigt ist (`LegacyPropertiesImported=false`). Nach erfolgreichem Import ist die LO-Konfiguration alleinige, persistente Quelle — und die einzige Kategorie, die aktiv aus dem propMap-Vollschreiber ausgefiltert wird (`istWebserverRegieLegacyKey`).
    - Legacy-Keys werden nach erfolgreichem Import einmalig aus der Datei entfernt und danach bei jedem Speichern erneut herausgefiltert:
        - webserver_regie_aktiv
        - webserver_regie_port
        - webserver_regie_ziele
    - Schlägt der LO-Read nach erfolgter Migration ausnahmsweise fehl, liefern die Getter für diese Sitzung Defaults (aktiv=true, Port 9090); die Werte bleiben in der LO-Konfiguration erhalten und werden bei der nächsten erfolgreichen Sitzung wieder gelesen. Es gibt bewusst keinen Legacy-Datei-Fallback mehr für die Regie.
    - Aktiv und Port sind in der eigenen LO-Optionsseite "Webserver-Regie" konfiguriert; Ziele bleiben in der Webserver-Regie-Sidebar.
    - Code: src/main/java/de/petanqueturniermanager/comp/GlobalProperties.java:588 (bereinigeLegacyWebserverRegieProperties), :605 (istWebserverRegieLegacyKey), src/main/java/de/petanqueturniermanager/comp/LibreOfficeWebserverRegieSpeicher.java

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
