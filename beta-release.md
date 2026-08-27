# Beta-Versionen optional in den Update-Check einbeziehen

## Context

`ReleaseUpdateService` prüft beim LO-Start (und manuell) via GitHub-API, ob eine neue Version verfügbar ist. Aktuell wird ausschließlich der Endpoint `/repos/{repo}/releases/latest` abgefragt — dieser liefert laut GitHub-API-Doku **nie** ein Pre-Release, sondern immer das neueste "volle" (nicht-Beta/nicht-Draft) Release. Der bestehende Code prüft zusätzlich defensiv `release.prerelease()` und würde ein Pre-Release ignorieren (`ReleaseUpdateService.java:309-312`) — dieser Zweig greift aber in der Praxis nie, weil der verwendete Endpoint so ein Release gar nicht erst zurückgibt.

Der Nutzer möchte eine zusätzliche Option, mit der auch Beta-Versionen (GitHub Pre-Releases) als Update erkannt und angeboten werden — Standardverhalten (nur Stable) bleibt unverändert, die neue Option ist ein Opt-in.

## Ansatz

Ein neuer GitHub-Abruf-Pfad über die Releases-Liste (`/repos/{repo}/releases`, liefert auch Pre-Releases, sortiert neueste zuerst) wird zusätzlich zum bestehenden `/releases/latest`-Pfad eingeführt. Eine neue Checkbox in der PTM-Optionsseite steuert, welcher Pfad verwendet wird. Die Persistierung folgt exakt dem bestehenden Muster von `autoUpdateDialogBeimStart` (Property → `PluginOptionen`-Record → `GlobalProperties.speichern(...)` → Options-Dialog).

## Änderungen

### 1. `GithubReleaseClient.java`
- Bestehende private Mapping-Logik `ReleaseDto → ReleaseInfo` (aktuell in `parseAntwort`, Zeilen 102-125) in eine wiederverwendbare private Methode extrahieren (`mappe(ReleaseDto dto)`).
- `ReleaseDto` um `boolean draft;` erweitern (Feldname aus GitHub-API: `draft`).
- Neue Methode `Optional<ReleaseInfo> ladeNeuestesReleaseInkludiertPrerelease()`:
  - ruft `/repos/{repository}/releases` auf (Liste, GitHub sortiert absteigend nach Erstellungsdatum),
  - parsed als `ReleaseDto[]` bzw. `List<ReleaseDto>`,
  - überspringt Einträge mit `draft == true`,
  - nimmt den ersten verbleibenden Eintrag, mappt ihn wie gehabt.
  - Gleiche Fehlerbehandlung wie `ladeLetztesRelease()` (HTTP-Status ≠ 200 → `Optional.empty()`, IOException/Interrupted analog).

### 2. `ReleaseUpdateService.java`
- In `fuehreRefreshAus()` (Zeile 270 ff.): vor dem Client-Aufruf `GlobalProperties.get().isAutoUpdateBetaAktiv()` lesen und je nach Wert `client.ladeLetztesRelease()` oder `client.ladeNeuestesReleaseInkludiertPrerelease()` aufrufen. (Analoges Muster für Zugriff auf `GlobalProperties` bereits vorhanden in `AutoUpdateStartupChecker.java:65,72,78` — funktioniert auch ohne gesetzten LO-Kontext, da `GlobalProperties.get()` mit Defaults initialisiert.)
- `aktualisiereStatusAusRelease(ReleaseInfo release)` (Zeile 306-318): den Prerelease-Skip nur noch anwenden, wenn Beta **nicht** aktiv ist:
  ```java
  if (!betaAktiv && release.prerelease()) {
      setzeStatus(UpdateStatus.KEIN_UPDATE);
      return;
  }
  ```
  (`betaAktiv` als Parameter durchreichen oder erneut aus `GlobalProperties` lesen — Konsistenz mit obigem Aufrufpunkt beachten.)

### 3. `GlobalProperties.java`
- Neue Konstante `AUTO_UPDATE_BETA_PROP = "auto.update.beta.aktiv"` neben `AUTO_UPDATE_DIALOG_PROP` (Zeile 60).
- Neuer Getter `isAutoUpdateBetaAktiv()` analog `isAutoUpdateDialogBeimStartAktiv()` (Zeile 1270), Default `false`.
- `PluginOptionen`-Record (`PluginOptionen.java:11-24`): neues Feld `boolean autoUpdateBetaAktiv` **am Ende** anhängen (gleiches Anhänge-Muster wie zuletzt bei `autoUpdateDialogBeimStart`).
- `pluginOptionenInMap()` (Zeile ~822) und `pluginOptionenAusMap()` (Zeile ~805) um das neue Feld ergänzen.
- `speichern(...)`-Signatur (Zeile 1857) um `boolean autoUpdateBetaAktiv` als letzten Parameter erweitern; alle Aufrufer anpassen.

### 4. `PluginOptionsEventHandler.java`
- Neue Konstante `CTL_AUTO_UPDATE_BETA = "AutoUpdateBeta"`.
- In `ladeInOberflaeche()` (Zeile 85 ff.): `setCheckbox(container, CTL_AUTO_UPDATE_BETA, properties.isAutoUpdateBetaAktiv());`
- In `setzeLabels()` (Zeile 117 ff.): `setLabel(container, CTL_AUTO_UPDATE_BETA, I18n.get("konfig.plugin.auto.update.beta"));`
- In `speichereAusOberflaeche()` (Zeile 98 ff.): neuen Wert in `properties.speichern(...)` als letztes Argument übergeben.
- `ReleaseUpdateService.get().loeseListenerAus();` (Zeile 111) durch `ReleaseUpdateService.get().triggerRefresh();` ersetzen — nur `loeseListenerAus()` würde die neue Beta-Einstellung nicht sofort wirksam machen, da kein neuer Fetch angestoßen wird. `triggerRefresh()` ist nicht-blockierend (läuft im bestehenden Hintergrund-Executor) und damit sicher aus dem UI-Callback aufrufbar.

### 5. `registry/data/org/openoffice/Office/dialogs/PluginOptions.xdl`
- Neue Checkbox `dlg:id="AutoUpdateBeta"` direkt unter `AutoUpdateDialogStartup` einfügen (z.B. `dlg:top="43"`, leicht eingerückt `dlg:left="16"` um die Zugehörigkeit zur Update-Option zu zeigen, oder gleiche Einrückung wie die anderen — visuell konsistent mit bestehenden Checkboxen halten).
- Alle nachfolgenden Controls (`ProcessBoxAutomaticallyShow` ff.) um den zusätzlichen Platzbedarf nach unten verschieben (+14).
- `dlg:height` des `dlg:window` entsprechend erhöhen.

### 6. i18n
Neuer Key `konfig.plugin.auto.update.beta` in allen fünf Sprachdateien unter `src/main/resources/de/petanqueturniermanager/i18n/`:
- `messages.properties` (DE): z.B. "Auch Beta-Versionen (Vorabversionen) als Update anbieten"
- `messages_en.properties`
- `messages_fr.properties`
- `messages_nl.properties`
- `messages_es.properties`

### 7. Tests
- `GithubReleaseClientTest.java`: neuer Test für `ladeNeuestesReleaseInkludiertPrerelease()` — Liste mit gemischten Stable/Pre-Release/Draft-Einträgen, erwartet wird der erste Nicht-Draft-Eintrag (auch wenn Pre-Release).
- `ReleaseUpdateServiceTest.java`: neuer Test, der `GlobalProperties.resetForTest()` + Beta-Flag setzt und prüft, dass bei aktivem Beta-Flag ein Pre-Release als `UPDATE_VERFUEGBAR` erkannt wird (Test-Double-Client muss dafür `ladeNeuestesReleaseInkludiertPrerelease()` statt `ladeLetztesRelease()` bedienen — bestehende Test-Doubles `FesterClient`/`ScriptedClient` entsprechend erweitern).
- `GlobalPropertiesTest.java`: alle 4 bestehenden `speichern(...)`-Aufrufe (Zeilen 87, 117, 121, 537) um den neuen letzten Parameter ergänzen; neuer Roundtrip-Test für `isAutoUpdateBetaAktiv()`/Default `false`.
- `AutoUpdateStartupCheckerTest.java`: beim Umbau auffallendes Fehlen von `GlobalProperties.resetForTest()` im `@BeforeEach` (Zeile 26-29) beheben, um Test-Isolation sicherzustellen (Seiteneffekt, unabhängig von diesem Feature, aber durch die neue Property zusätzlich relevant).

## Verifikation
- `./gradlew test --tests "de.petanqueturniermanager.comp.newrelease.*"` und `--tests "de.petanqueturniermanager.comp.GlobalPropertiesTest"`.
- `./gradlew spotbugsMain` (Zero-Warnings-Vorgabe).
- `./gradlew reinstallExtension` und manueller Check: Extras → Optionen → PétTurnMngr → neue Checkbox sichtbar, Zustand persistiert nach Neustart von LO.
- Manueller Funktionscheck (falls ein GitHub Pre-Release im Test-Repo verfügbar gemacht werden kann): mit aktivierter Checkbox wird ein Pre-Release als Update erkannt; mit deaktivierter Checkbox nicht.
