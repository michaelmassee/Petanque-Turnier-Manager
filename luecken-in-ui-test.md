# UI-Test-Abdeckung – Analyse & Empfehlungen

## Context

Der Auftrag „Prüfe Testabdeckung mit UI-Test" soll Klarheit darüber schaffen, welche Bereiche der LibreOffice-Calc-Extension durch UI-Tests (Klassen mit Suffix `*UITest`, basierend auf `BaseCalcUITest`) abgesichert sind und wo signifikante Lücken bestehen. Ergebnis ist eine Bestandsaufnahme als Grundlage für eine spätere gezielte Test-Erweiterung.

## Bestand

**33 UI-Test-Klassen** unter `src/test/`, ca. 935 Zeilen Testcode. UI-Tests laufen gegen eine headless LibreOffice-Instanz (`OfficeStarter`) mit der vorher installierten Extension (`./gradlew reinstallExtension`).

## Abdeckung pro Turniersystem / Feature

| System / Feature | Status | Tests | Anmerkung |
|---|---|---|---|
| Supermelee | ✅ stark | 8 | Meldeliste, Spielrunden, Rangliste, Spielplan, Spieltag-Rangliste |
| Schweizer | ✅ stark | 7 | Vollständiger Workflow (Meldeliste→3 Runden→Rangliste), Menü-Status, 19-Team-Edge-Case |
| Liga | ✅ gut | 3 | Meldeliste, Rangliste, Spielplan – Smoke-Niveau |
| JederGegenJeden (JGJ) | ⚠️ partiell | 4 | Rangliste, Spielplan, Setzpos, Direktvergleich – **keine Meldeliste-Tests** |
| KO / Maastrichter / Forme | ❌ minimal | 1 | Nur `KoGruppeABSheet` – keine Knockout-Workflows |
| Formule_X | ⚠️ minimal | 1 | Nur Rangliste |
| Sidebar | ✅ gut | 2 | Panel-Factory, Version-Label, Sheet-Toggle |
| Blattschutz | ✅ gut | 2 | StyleRestrictions, CellStyleHelper-Verhalten |
| Add-in `PTM.ALG.*` | ⚠️ minimal | 1 | `GlobalImplUITest` testet nur `INTPROPERTY` |
| Konfigdialog | ❌ keine | 0 | `MainKonfigDialogTest` ist `@Disabled` |
| Beispielturnier | ✅ grün | 1 | 17/17 Tests grün (NPE & Endlosrekursion behoben) |

## Test-Tiefe

- **Überwiegend Smoke**: Sheet erzeugen + Strukturvalidierung (Spalten, Zellinhalte zählen).
- **Echter End-to-End-Workflow nur bei Schweizer System** (`SchweizerSystemTestDaten*` erzeugt Meldeliste → 3 Runden mit Ergebnissen → Rangliste).
- Blattschutz wird isoliert getestet, nicht im Kontext kompletter Tournament-Abläufe.

## Priorisierte Lücken

1. **KO/Forme/Maastrichter** – größte Lücke; produktiv genutzt, aber 0 echte UI-Tests für Knockout-Logik/Ranglisten.
2. **Konfigdialog** – `@Disabled` reaktivieren oder durch headless-fähigen Ansatz ersetzen.
3. **Add-in-Funktionen** – nur `INTPROPERTY` getestet; `DIREKTVERGLEICH`, `MEDIAN`, `PUNKTE` u.a. ungetestet (siehe `addins/GlobalImpl`).
4. **JGJ Meldeliste** + **Formule_X Meldeliste/Spielplan** – fehlende Sheets im Workflow.
5. **Workflow-Tiefe Supermelee/Liga** – Smoke-Niveau auf End-to-End-Tests heben (analog Schweizer).

## Empfehlung (keine Implementierung in dieser Runde)

Die Analyse selbst ist das Ergebnis; konkrete neue Tests nur nach Auswahl durch den User. Vorschlag: in folgender Reihenfolge angehen, jeweils als eigener PR/Branch:

1. KO/Forme E2E-UITest (höchster Nutzen, größte Lücke).
2. Add-in-Suite-UITest erweitern (kleiner Aufwand, hoher Wert).
3. JGJ/Formule_X Meldeliste-Smoke-Tests.
4. Konfigdialog reaktivieren / ersetzen.

## Verifikation

- `find src/test -name "*UITest.java" | wc -l` → aktuell 33.
- `./gradlew test` läuft die Unit-Tests; UI-Tests benötigen vorher `./gradlew reinstallExtension`.
- Keine Code-Änderungen geplant in dieser Phase – nur Bericht.
