# Migration GlobalProperties: Legacy-Datei → LibreOffice-Konfiguration

## Ziel

`GlobalProperties` speicherte historisch alle Plugin-Einstellungen in einer einzigen Legacy-Properties-Datei
`PetanqueTurnierManager.properties` im User-Home (`user.home`). Ziel der laufenden Migration ist es, diese Datei
schrittweise durch die native LibreOffice-Konfiguration (XCU-Nodes unter Extras > Optionen > PétTurnMngr) zu
ersetzen.

## Stand: fast vollständig migriert

Bis auf die Tab-Farben ist jeder Bereich inzwischen auf das Standard-LO-Muster umgestellt:

- **Vollständig auf LO-Konfiguration umgestellt** (Legacy-Keys aktiv aus dem Datei-Schreibpfad ausgefiltert,
  sobald die Migration erfolgreich war): Webserver-Regie, Plugin-/Optionswerte, FTP-Server,
  Turnier-Startseite, Startup-Modus, Composite-Webserver-Views.
- **Noch ausschließlich Legacy-Datei**, keine LO-Konfiguration vorgesehen: Tab-Farben (reiner Lesepfad,
  kein Setter vorhanden).

Die Legacy-Datei bleibt also nur noch für Tab-Farben sowie als Fallback (kein LO-Kontext, z.B. in
Unit-Tests, oder LO-Zugriffsfehler) relevant. Einmal migrierte Alt-Keys werden beim Start einmalig aus der
Datei entfernt (siehe „Nicht mehr regulär gespeichert" unten).

### Wichtig: propMap ist ein Vollständig-Mirror, Filterung läuft pro Bereich

`speichernDatei()` (GlobalProperties.java:435) schreibt bei **jedem** Speichervorgang die **komplette**
`propMap` in die Datei — unabhängig davon, welcher Bereich gerade geändert wurde. Für jeden migrierten
Bereich gibt es daher ein eigenes `<bereich>InLibreOffice`-Flag plus eine `ist<Bereich>LegacyKey(...)`-Prüfung,
die dessen Flat-Keys aus dem Schreibpfad herausfiltert, sobald der Bereich erfolgreich nach LO migriert ist.
Die Filterung läuft gebündelt über `istBereitsInLibreOfficeGespeichert(...)` (GlobalProperties.java:483):

- Webserver-Regie: `istWebserverRegieLegacyKey`
- Plugin-Optionen: `istPluginOptionenLegacyKey`
- FTP-Server: `istFtpServerLegacyKey`
- Turnier-Startseite: `istStartseiteLegacyKey`
- Startup-Modus: `istStartupModusLegacyKey`
- Composite-Views: `istCompositeViewsLegacyKey`

Composite-Views werden dabei — wie FTP-Server und Webserver-Regie zuvor — als **ein JSON-Blob**
(`CompositeViewsOptionen.eintraegeJson`, Liste von `GlobalProperties.CompositeViewEintragRoh` inkl. Panels)
in der LO-Konfiguration abgelegt, nicht als verschachtelte XCU-Sets. Intern bleiben die Flat-Keys in der
`propMap` (für Legacy-Fallback und den bestehenden `getCompositeViewEintraege()`-Lesepfad) unverändert
erhalten – `compositeViewsFlatInMap(...)` befüllt sie sowohl beim Speichern als auch beim Laden aus LO.

## Noch (physisch) in PetanqueTurnierManager.properties gespeichert

- Tab-Farben:
    - tabfarbe.<name>
    - Keine LO-Konfiguration vorgesehen, ausschließlich Legacy-Datei. Aktuell gibt es in GlobalProperties nur Lesen, keinen Setter. Existierende Werte bleiben aber in der Datei, weil beim Speichern die ganze propMap geschrieben wird.
    - Code: src/main/java/de/petanqueturniermanager/comp/GlobalProperties.java:1458 (getTabFarbe)

## Nicht mehr in der Datei (nur noch LO-Konfiguration)

- Plugin-/Optionswerte:
    - autosave, backup, newversioncheck, prozessbox.automatisch.anzeigen, prozessbox.automatisch.schliessen, performance.logging, loglevel
    - Ausschließlich über LibreOffice-Konfiguration im Node `/org.openoffice.Office.Custom.PetanqueTurnierManager/Settings`.
    - Legacy-Datei ist Quelle nur bis zur erfolgreichen Migration (`LegacyPropertiesImported=false`). Nach erfolgreichem Import ist die LO-Konfiguration alleinige, persistente Quelle.
    - Legacy-Keys werden nach erfolgreichem Import einmalig aus der Datei entfernt (`bereinigeLegacyPluginOptionenProperties`) und danach bei jedem Speichern erneut herausgefiltert (`istPluginOptionenLegacyKey`).
    - Schlägt der LO-Zugriff nach erfolgter Migration ausnahmsweise fehl, greift der Legacy-Datei-Fallback für diese Sitzung wieder.
    - Verwaltung über die LO-Optionsseite Extras > Optionen > PétTurnMngr.
    - Code: src/main/java/de/petanqueturniermanager/comp/GlobalProperties.java:1425 (speichern), :750 (istPluginOptionenLegacyKey), :729 (bereinigeLegacyPluginOptionenProperties); src/main/java/de/petanqueturniermanager/comp/LibreOfficePluginOptionenSpeicher.java:84

- Webserver-Regie:
    - Ausschließlich über LibreOffice-Konfiguration in der Node WebserverRegie.
    - Properties: Active, Port, TargetsJson, LegacyPropertiesImported
    - Legacy-Datei ist Quelle nur bis zur erfolgreichen Migration: ohne LO-Kontext, oder bei LO-Fehler, bevor der Import erledigt ist (`LegacyPropertiesImported=false`). Nach erfolgreichem Import ist die LO-Konfiguration alleinige, persistente Quelle.
    - Legacy-Keys werden nach erfolgreichem Import einmalig aus der Datei entfernt und danach bei jedem Speichern erneut herausgefiltert:
        - webserver_regie_aktiv
        - webserver_regie_port
        - webserver_regie_ziele
    - Schlägt der LO-Read nach erfolgter Migration ausnahmsweise fehl, liefern die Getter für diese Sitzung Defaults (aktiv=true, Port 9090); die Werte bleiben in der LO-Konfiguration erhalten und werden bei der nächsten erfolgreichen Sitzung wieder gelesen. Es gibt bewusst keinen Legacy-Datei-Fallback mehr für die Regie.
    - Aktiv und Port sind in der eigenen LO-Optionsseite "Webserver-Regie" konfiguriert; Ziele bleiben in der Webserver-Regie-Sidebar.
    - Code: src/main/java/de/petanqueturniermanager/comp/GlobalProperties.java:696 (bereinigeLegacyWebserverRegieProperties), :713 (istWebserverRegieLegacyKey), src/main/java/de/petanqueturniermanager/comp/LibreOfficeWebserverRegieSpeicher.java

- FTP-Server (zentrale Liste):
    - Node FtpServer, Property ServersJson (JSON-Liste von GlobalProperties.FtpServerEintrag,
      inkl. Name, Protokoll, Host, Port, Benutzer, Passwort, Ziel-Verzeichnis).
    - Kein Legacy-Import: die bisherigen Upload-Einstellungen lagen pro Dokument
      (Document Properties je Turniersystem), nicht global — daher kein Migrationspfad,
      Liste startet leer. Der Legacy-Key `ftp_server_liste` wird dennoch aktiv aus dem
      Datei-Schreibpfad gefiltert (`istFtpServerLegacyKey`), sobald das Lesen aus LO gelingt.
    - Verwaltung auf eigener LibreOffice-Optionsseite Extras > Optionen > PétTurnMngr > FTP-Server;
      Detail-Konfiguration je Server über FtpServerDetailDialog.
    - Beim Upload wählt der Nutzer über FtpServerAuswahlDialog einen Server aus der Liste;
      die Auswahl wird pro Dokument als Document Property "FTP Letzter Server" (Server-Id)
      gemerkt und beim nächsten Upload vorselektiert.
    - Das frühere host-basierte Upload-Passwort (GlobalProperties.getUploadPasswort/
      setUploadPasswort) ist entfallen — das Passwort ist jetzt Teil des Server-Objekts.
    - Code: src/main/java/de/petanqueturniermanager/comp/GlobalProperties.java:1049 (getFtpServerEintraege),
      :1064 (speichernFtpServer), :770 (istFtpServerLegacyKey); src/main/java/de/petanqueturniermanager/comp/LibreOfficeFtpServerSpeicher.java;
      src/main/java/de/petanqueturniermanager/comp/FtpServerOptionsEventHandler.java;
      src/main/java/de/petanqueturniermanager/helper/upload/{FtpServerDetailDialog,FtpServerAuswahlDialog,AbstractFtpUpload}.java

- Turnier-Startseite:
    - Node Startseite, Properties Port, Active, Zoom, LegacyPropertiesImported.
    - Legacy-Keys (startseite_port, startseite_aktiv, startseite_zoom) werden nach erfolgreichem Import
      einmalig entfernt (`bereinigeLegacyStartseiteProperties`) und danach bei jedem Speichern erneut
      herausgefiltert (`istStartseiteLegacyKey`).
    - Code: src/main/java/de/petanqueturniermanager/comp/GlobalProperties.java (getStartseitePort, isStartseiteAktiv,
      getStartseiteZoom, speichernStartseite); src/main/java/de/petanqueturniermanager/comp/LibreOfficeStartseiteSpeicher.java

- Startup-Modus:
    - Node Startup, Property TurnierModus, LegacyPropertiesImported.
    - Legacy-Key (startup.turnier.modus) wird nach erfolgreichem Import einmalig entfernt
      (`bereinigeLegacyStartupModusProperties`) und danach bei jedem Speichern erneut herausgefiltert
      (`istStartupModusLegacyKey`).
    - Code: src/main/java/de/petanqueturniermanager/comp/GlobalProperties.java (isStartupTurnierModus,
      setStartupTurnierModus); src/main/java/de/petanqueturniermanager/comp/LibreOfficeStartupModusSpeicher.java

- Composite-Webserver-Views:
    - Node CompositeViews, Properties Active, EntriesJson (JSON-Liste von GlobalProperties.CompositeViewEintragRoh
      inkl. Panels und Rand-Konfiguration), LegacyPropertiesImported.
    - Legacy-Flat-Keys (webserver_aktiv, webserver_composite_ports, webserver_composite_*) werden nach
      erfolgreichem Import einmalig entfernt (`bereinigeLegacyCompositeViewsProperties`) und danach bei
      jedem Speichern erneut herausgefiltert (`istCompositeViewsLegacyKey`). Intern bleiben die Flat-Keys
      in der propMap als Cache erhalten (siehe oben, `compositeViewsFlatInMap`) — nur der Schreibpfad in
      die Legacy-Datei ist gefiltert.
    - Verwaltung weiterhin auf der eigenen LibreOffice-Optionsseite Extras > Optionen > PétTurnMngr > Composite Views;
      Detail-Konfiguration je Zeile über `CompositeViewDetailDialog`, "Übernehmen" persistiert sofort und
      benachrichtigt den laufenden Webserver live.
    - Code: src/main/java/de/petanqueturniermanager/comp/GlobalProperties.java (getCompositeViewEintraege,
      speichernCompositeViews, speichernWebserverAktiv, compositeViewsFlatInMap); src/main/java/de/petanqueturniermanager/comp/LibreOfficeCompositeViewsSpeicher.java;
      src/main/java/de/petanqueturniermanager/webserver/RandKonfiguration.java; src/main/java/de/petanqueturniermanager/comp/CompositeViewsOptionsEventHandler.java

## Nicht mehr regulär gespeichert

- Alte Einzel-Port-Webserver-Keys werden beim Start entfernt:
    - webserver_ports
    - webserver_port_*
    - webserver_sheetnamen_anzeigen
    - Code: src/main/java/de/petanqueturniermanager/comp/GlobalProperties.java:344 (bereinigeLegacyEinzelPortProperties)

- Webserver-Regie-Legacy-Keys werden nach erfolgreichem LO-Import entfernt:
    - webserver_regie_aktiv
    - webserver_regie_port
    - webserver_regie_ziele
    - Code: src/main/java/de/petanqueturniermanager/comp/GlobalProperties.java:696 (bereinigeLegacyWebserverRegieProperties), src/main/java/de/petanqueturniermanager/comp/LibreOfficeWebserverRegieSpeicher.java

- Plugin-Optionen-Legacy-Keys werden nach erfolgreichem LO-Import entfernt:
    - autosave, backup, newversioncheck, prozessbox.automatisch.anzeigen, prozessbox.automatisch.schliessen, performance.logging, loglevel
    - Code: src/main/java/de/petanqueturniermanager/comp/GlobalProperties.java:729 (bereinigeLegacyPluginOptionenProperties), src/main/java/de/petanqueturniermanager/comp/LibreOfficePluginOptionenSpeicher.java

- Turnier-Startseite-Legacy-Keys werden nach erfolgreichem LO-Import entfernt:
    - startseite_port
    - startseite_aktiv
    - startseite_zoom
    - Code: src/main/java/de/petanqueturniermanager/comp/GlobalProperties.java (bereinigeLegacyStartseiteProperties), src/main/java/de/petanqueturniermanager/comp/LibreOfficeStartseiteSpeicher.java

- Startup-Modus-Legacy-Key wird nach erfolgreichem LO-Import entfernt:
    - startup.turnier.modus
    - Code: src/main/java/de/petanqueturniermanager/comp/GlobalProperties.java (bereinigeLegacyStartupModusProperties), src/main/java/de/petanqueturniermanager/comp/LibreOfficeStartupModusSpeicher.java

- Composite-Views-Legacy-Flat-Keys werden nach erfolgreichem LO-Import entfernt:
    - webserver_aktiv, webserver_composite_ports, alle webserver_composite_*-Keys (Ports/Panels/Rand)
    - Code: src/main/java/de/petanqueturniermanager/comp/GlobalProperties.java (bereinigeLegacyCompositeViewsProperties), src/main/java/de/petanqueturniermanager/comp/LibreOfficeCompositeViewsSpeicher.java

- Timer-Einstellungen-Legacy-Keys werden beim Start entfernt (keine Wertübernahme):
    - timer_letzte_dauer
    - timer_letzter_port
    - timer_letzte_bezeichnung
    - timer_hintergrundfarbe
    - Neue Werte liegen pro Dokument als UserDefined Document Properties (DocumentPropertiesHelper): Timer Letzte Dauer, Timer Letzter Port, Timer Letzte Bezeichnung, Timer Hintergrundfarbe.
    - Code: src/main/java/de/petanqueturniermanager/comp/GlobalProperties.java:371 (bereinigeLegacyTimerProperties), src/main/java/de/petanqueturniermanager/timer/TimerDialog.java
