# Release-Runbook

Checkliste für einen GitHub-Release des Petanque-Turnier-Managers. Der Ablauf ist
**tag-getrieben und teilautomatisiert** und verteilt sich auf zwei Repos:

- **Hauptprojekt** (`michaelmassee/Petanque-Turnier-Manager`) – baut die OXT, hält das
  kuratierte Release.
- **Installer-Repo** (`Petanque-Turnier-Manager-Installer`) – baut die nativen Installer
  (Linux AppImage, Windows exe/msi, macOS dmg) und hängt sie ans Hauptprojekt-Release.

> **Kritische Reihenfolge:** Das **Hauptprojekt-Release mit angehängter OXT muss zuerst
> existieren.** Der Installer-Workflow lädt die OXT aus diesem Release herunter
> (`releases/download/v<VERSION>/PetanqueTurnierManager-<VERSION>.oxt`) und lädt die fertigen
> Installer per `gh release upload --clobber` dorthin zurück. Fehlt das Release, scheitern
> sowohl Download als auch Rück-Upload.

> **Achtung:** Das Hauptprojekt-CI (`.github/workflows/ci.yml`) läuft nur auf Branch-Push,
> **nicht** auf Tags. Es baut **keine** OXT und erstellt **kein** Release. OXT-Build (Schritt 3)
> und Release-Erstellung (Schritt 6) sind daher **manuelle Pflicht**.

Konventionen:
- Tag-Format: `v<MAJOR>.<MINOR>.<PATCH>` (z. B. `v5.34.40`).
- In `description.xml` steht die Version **ohne** `v`.
- Commit-Message für den Versions-Bump: `Release: Version X.Y.Z`.

---

## 1. Vorbereitung (Hauptprojekt, lokal)

```bash
cd /home/michael/devel/projects_massee/Petanque-Turnier-Manager

# Letzten Tag und offene Commits prüfen
git describe --tags --abbrev=0
git log "$(git describe --tags --abbrev=0)"..HEAD --oneline
```

- Neue Version (SemVer) festlegen.
- `description.xml` (Zeile 3) auf die neue Version setzen: `<version value="X.Y.Z"/>`.
- Optional (Boy-Scout): i18n-Vollständigkeit prüfen – alle fünf Dateien synchron
  (`messages.properties`, `messages_en.properties`, `messages_fr.properties`,
  `messages_nl.properties`, `messages_es.properties` unter
  `src/main/resources/de/petanqueturniermanager/i18n/`).

## 2. Qualitäts-Gate (müssen grün sein)

```bash
./gradlew test            # Unit-Tests
./gradlew spotbugsMain    # 0 Findings erwartet (siehe CLAUDE.md)

# Optional, benötigen vorab reinstallExtension:
./gradlew reinstallExtension && ./gradlew uiTests

# Optional Vollumfang:
./gradlew runAllTests
```

## 3. OXT bauen

```bash
./gradlew buildPlugin
# Ergebnis: build/distributions/PetanqueTurnierManager-X.Y.Z.oxt
```

> Muss auf **Linux** mit installiertem LibreOffice-SDK laufen
> (`compileIdl` benötigt `unoidl-write`).

## 4. Commit & Tag (Hauptprojekt)

```bash
git add description.xml
git commit -m "Release: Version X.Y.Z"
git tag -a vX.Y.Z -m "Release vX.Y.Z"
git push github_michael_massee_privat master vX.Y.Z
```

> Remote-Name im Hauptprojekt ist `github_michael_massee_privat` (nicht `origin`) – mit
> `git remote -v` prüfen. Branch ist `master` und per Branch-Protection PR-pflichtig; der
> direkte Push gelingt nur als Owner (Bypass).

## 5. Release-Notes vorbereiten (zweisprachig: DE-Block + EN-Block)

Notes in eine Datei `notes.md` schreiben – zuerst ein deutscher, dann ein englischer Block:

```markdown
## 🇩🇪 Deutsch
- <Änderung 1>
- <Änderung 2>

## 🇬🇧 English
- <change 1>
- <change 2>
```

Als Ausgangsmaterial helfen `git log <letzterTag>..HEAD --oneline` und – falls gewünscht –
einmalig `gh release create … --generate-notes` zum Sichten; der finale Body wird aber
manuell DE/EN strukturiert.

## 6. GitHub-Release im Hauptprojekt anlegen – MIT OXT (kritisch, muss zuerst!)

```bash
gh release create vX.Y.Z \
  --repo michaelmassee/Petanque-Turnier-Manager \
  --title "Release vX.Y.Z" \
  --notes-file notes.md \
  build/distributions/PetanqueTurnierManager-X.Y.Z.oxt
```

> Dieses Release MUSS existieren, bevor das Installer-Repo getaggt wird.

## 7. Installer-Repo nachziehen & taggen (löst Installer-Build aus)

```bash
cd ../Petanque-Turnier-Manager-Installer
# gradle.properties: ptmVersion=X.Y.Z setzen (muss zur Hauptprojekt-Version passen)
git add gradle.properties
git commit -m "Release: Version X.Y.Z"
git tag -a vX.Y.Z -m "Release vX.Y.Z"
git push origin main vX.Y.Z   # Tag-Push triggert build-installers.yml
```

> Aktiver Branch im Installer-Repo ist `main` (Remote `origin`). Ein veralteter `master`
> existiert daneben – **nicht** dorthin pushen.

Der Workflow erstellt automatisch das Installer-Release und lädt AppImage/exe/msi/dmg
zusätzlich ins Hauptprojekt-Release.

> Hinweis: Das **Installer**-Release übernimmt die kuratierten **DE/EN-Notes aus dem
> Hauptprojekt-Release** (Single Source of Truth) und hängt die trilingualen
> Systemvoraussetzungen an. Der `release`-Job in `build-installers.yml` liest dazu
> `gh release view <TAG> --repo michaelmassee/Petanque-Turnier-Manager --json body`.
> Deshalb ist die Reihenfolge bindend: Hauptprojekt-Release (Schritt 6) **vor** dem
> Installer-Tag. Fehlt das Hauptprojekt-Release, fällt das Installer-Release auf reine
> Systemvoraussetzungen zurück (Workflow-Warnung).

## 8. Verifikation nach dem Release

```bash
# Installer-Workflow beobachten (im Installer-Repo)
gh run watch

# Hauptprojekt-Release prüfen: OXT + 3 Installer-Assets vorhanden?
gh release view vX.Y.Z --repo michaelmassee/Petanque-Turnier-Manager
```

- Optional macOS-Smoketest manuell anstoßen: `gh workflow run "Smoketest macOS"`.

---

## Fallstricke

- **Reihenfolge:** Hauptprojekt-Release vor Installer-Tag, sonst 404 beim OXT-Download.
- **Versions-Konsistenz:** `description.xml` (ohne `v`), Tag (`vX.Y.Z`) und OXT-Dateiname müssen
  zueinander passen.
- **Kein Auto-Build auf Tags:** `ci.yml` läuft nicht auf Tags – Schritte 3 + 6 sind manuell Pflicht.
- **Installer-Version:** `gradle.properties` (`ptmVersion`) muss synchron sein, sonst Fehler
  „Zuerst das Hauptprojekt … releasen".
- **Build-Plattform:** OXT-Build muss auf Linux mit LO-SDK laufen (`unoidl-write`).
