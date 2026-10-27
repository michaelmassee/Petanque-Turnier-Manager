# Calc-Absturz beim ersten PTM-Online-Abgleich am 27.10.2026

## Untersuchte Stände

- PTM: `2a3e2202`, Branch `ptm-online-2`, anschließend die hier beschriebene lokale Korrektur.
- PTM Online: `d6b8eaa`, lokaler Worker mit eigener frischer D1.
- LibreOffice: Ubuntu-PPA-Paket `libreoffice-core 4:26.2.6.3-0ubuntu0.24.04.1~lo1`.
- Java: Temurin 25.0.2; Calc wurde für die Tests headless gestartet und das Testdokument mit `Hidden=true` geladen.

## Befund und Reproduktion

Die ursprüngliche kombinierte Suite erfasste 32 Tests: 27 bestanden, fünf schlugen fehl. Beim ersten manuellen Anmeldungsabgleich stürzte LibreOffice nativ ab. Die übrigen Tests konnten nach dem automatischen Neustart weiterlaufen.

Der originale Apport-Dump stammt vom 27.10.2026, 20:09 Uhr. Er wurde mit GDB untersucht. Die relevante native Kette lautet:

```text
ScTabViewObj::setActiveSheet
  ScTabView::SetTabNo
    SfxViewShell::setLibreOfficeKitViewCallback
      ScTabViewShell::afterCallbackRegistered
        ScTabViewShell::UpdateInputHandler
          Eingabezeile: Zugriff auf ein fehlendes Textfenster
```

An der ursprünglichen Fehleradresse `libsclo.so + 0x9dc12c` dereferenziert die Anweisung `mov (%rbx),%rcx` den Wert `rbx=0`. RTTI identifiziert das betroffene Objekt als `ScInputWindow`. Sowohl dessen Positionsfenster als auch sein Textfenster sind bereits geleert; der zuständige `ScInputHandler` hält weiterhin einen Verweis auf dieses Eingabefenster. Die Eigentümeradresse des Eingabefensters entspricht dem gerade aktualisierten `ScTabViewShell`.

Das passt zum versionsgenau abgeglichenen [Calc-Quelltext von LibreOffice 26.2.6.3](https://github.com/LibreOffice/core/blob/libreoffice-26.2.6.3/sc/source/ui/app/inputwin.cxx#L289-L321): `ScInputWindow::dispose()` setzt Verweise der sichtbaren View-Shells zurück und leert anschließend seine Kindfenster. Die Enumeration verwendet `GetFirst(true)` / `GetNext(..., true)` und berücksichtigt damit nur sichtbare Frames. Ein verstecktes Dokument kann einen Verweis auf das bereits abgebaute Eingabefenster behalten. Der nächste Blattwechsel aktualisiert dieses Fenster und trifft den Nullzeiger. LibreOffice behandelt den ursprünglichen Speicherzugriffsfehler schließlich mit `SIGABRT` / „Fatal exception: Signal 6“.

Ein eigenständiger UI-Test reproduzierte denselben nativen Absturz mit identischen Bibliotheks-Offsets ohne Online-Worker:

1. Ein verstecktes Calc-Dokument mit Schweizer Meldeliste anlegen.
2. Die normalen UI-Elemente wiederherstellen und die LO-Ereignisschleife abarbeiten.
3. Den Turniermodus aktivieren und erneut die Ereignisschleife abarbeiten.
4. Das PTM-Online-Sync-Blatt anlegen.

Der mit TRACE gesicherte Java-Pfad des Minimalfalls lautet:

```text
PtmOnlineRegistrationMapping.sicherstellen
  PtmOnlineSyncSheet.anlegen
    NewSheet.create
      TurnierSheet.toggleSheetGrid
        TurnierSheet.setActiv
          XSpreadsheetView.setActiveSheet
```

Auch das Fixieren der Kopfzeilen verwendet `TurnierSheet.setActiv()`. Es führt bei einem bereits vorhandenen Sync-Blatt ebenfalls zu einem Blattwechsel. Der Server ist für die Reproduktion des nativen Fehlers nicht erforderlich.

Der sichtbare Kontrollfall bestand bereits vor der Korrektur. Ein isolierter Online-Test mit umfassendem TRACE sowie drei begrenzte Wiederholungen waren ebenfalls erfolgreich. Sie belegten damit allein keine Behebung; der gezielt vorbereitete versteckte Turniermodus-Fall reproduzierte den Fehler anschließend wiederholt.

## Einordnung für den Produktionsbetrieb

Nachgewiesen ist ein Ablauf mit einem ausdrücklich versteckt geladenen Dokument (`Hidden=true`). Für ein sichtbares Turnierfenster wurde dieser Absturz nicht reproduziert. Ein minimiertes oder lediglich unfokussiertes Fenster ist durch diese Untersuchung nicht als Auslöser belegt.

Die normalen neuen Turnierdokumente entstehen in `TurnierSystemNeueDateiAuswahlDialog` über `createSichtbaresCalc()`. Versteckte Calc-Dokumente werden im Produktcode beim Spielerdatenbank-Import und -Export verwendet. Diese Aufrufpfade enthalten keinen regulären Online-Turnierabgleich. Beim Laden eines bestehenden Dokuments kann `ProtocolHandler.onLoad()` den gespeicherten Turniermodus wiederherstellen; deshalb ist ein versteckt geladenes Turnierdokument mit anschließenden PTM-Blattoperationen grundsätzlich die relevante Kombination, etwa bei externer UNO-Automatisierung.

Ein normaler Produktions-Bedienablauf, der die gesamte nachgewiesene Kombination herstellt, ist bislang nicht belegt. Die Reproduktion ist als Test-/Automatisierungsfall einzuordnen. Die lokale Schutzbedingung sichert diese versteckten Dokumentfenster unabhängig vom Aufrufer ab.

## Lokale Korrektur

`TurnierModus.setzeRechnerleiste()` prüft jetzt die Sichtbarkeit des zugehörigen Fensters über `XWindow2`. Die Eingabezeile wird nur bei sichtbaren Fenstern per `.uno:InputLineVisible` umgeschaltet. Der dokumentbezogene Turniermodus und der Blattschutz funktionieren auch bei unsichtbaren Dokumenten weiter.

`TurnierModusBlattwechselUITest` prüft denselben Ablauf mit sichtbarem und verstecktem Dokument, einschließlich des regulären Blattschutz-Scopes beim Sync-Blatt-Aufbau. Der Test stellt die Turniermodus-Oberfläche anschließend wieder her.

## Weitere Fehler der ursprünglichen kombinierten Suite

Drei fehlgeschlagene Ergebnismeldungsprüfungen erhielten HTTP 403; die Live-Ranglistenprüfung erhielt `live=null`. Die Testturniere setzten das inzwischen erforderliche `liveViewEnabled` nicht. Der aktuelle Worker sperrt bei fehlender Freigabe die Live-Daten und Spieler-Ergebnismeldungen.

Eine separate lokale Anpassung des Testhelfers `PtmOnlineWebApi` schaltet die Live-Ansicht für diese Live-Synchronisationsturniere ausdrücklich ein. Ein explizit übergebener Wert bleibt erhalten. Diese Anpassung der Testvorbereitung gehört nicht zum hier beschriebenen Absturzfix; der Worker wird dafür nicht verändert.

## Validierung

- Vor der Korrektur: nativer Absturz im gezielten versteckten Minimalfall; sichtbarer Kontrollfall erfolgreich.
- Nach der Korrektur: 11 gezielte UI-Tests bestanden, keine Fehler und keine übersprungenen Tests. Enthalten sind der sichtbare und versteckte Regressionstest sowie die vorhandenen Turniermodus-Prüfungen für mehrere Dokumente, automatische Aktivierung und Formatierungsschutz.
- Erneute Abschlussprüfung der 11 gezielten UI-Tests mit vollständiger Test-Aufräumung: bestanden, keine Fehler und keine übersprungenen Tests, 43 Sekunden.
- Vollständige kombinierte `ptmOnlineTests`-Suite mit der separaten lokalen Live-Freigabe-Anpassung im Testhelfer: alle 32 Tests bestanden, keine Fehler, keine übersprungenen Tests und kein nativer Absturz, 5 Minuten 27 Sekunden. Dieses Gesamtergebnis setzt beide Änderungen voraus.
- Reguläre Tests: 1.739 bestanden, keine Fehler oder übersprungenen Tests. SpotBugs: null `BugInstance`. Die separaten PTM-Online-Testquellen kompilieren.

Die zuvor bestandene vollständige `uiTests`-Suite und die kombinierte `ptmOnlineTests`-Suite sind getrennte Prüfungen. Die Erkenntnisse und die Korrektur beziehen sich auf den hier reproduzierten Lebenszyklusfehler; daraus folgt keine allgemeine Absturzfreiheit sämtlicher Calc-Pfade.

Die ursprünglichen Logs, XML-Ergebnisse, GDB-Ausgaben und der entpackte native Dump liegen lokal unter `/tmp/ptm-online-crash-20261027/`. Das Verzeichnis ist nur für den Benutzer zugänglich. Der Speicher-Dump gehört nicht ins Repository.
