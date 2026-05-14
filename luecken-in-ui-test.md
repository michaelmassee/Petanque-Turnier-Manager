# UI-Test-Abdeckung – Analyse & Empfehlungen

## Context

Der Auftrag „Prüfe Testabdeckung mit UI-Test" soll Klarheit darüber schaffen, welche Bereiche der LibreOffice-Calc-Extension durch UI-Tests (Klassen mit Suffix `*UITest`, basierend auf `BaseCalcUITest`) abgesichert sind und wo signifikante Lücken bestehen. Ergebnis ist eine Bestandsaufnahme als Grundlage für eine spätere gezielte Test-Erweiterung.

## Bestand

**41 UI-Test-Klassen** unter `src/test/`. UI-Tests laufen gegen eine headless LibreOffice-Instanz (`OfficeStarter`) mit der vorher installierten Extension (`./gradlew reinstallExtension`).

## Abdeckung pro Turniersystem / Feature

| System / Feature | Status | Tests | Anmerkung |
|---|---|---|---|
| Supermelee | ✅ stark | 8 | Vollständiger E2E: Meldeliste + Spieltag-Ranglisten 1–5 (alle 5 validiert) |
| Schweizer | ✅ stark | 7 | Vollständiger Workflow (Meldeliste→3 Runden→Rangliste), Menü-Status, 19-Team-Edge-Case |
| Liga | ✅ stark | 4 | Zwei Konstellationen: 6 Teams Standard + 7 Teams Freispiel (ungerade Teamzahl) |
| JederGegenJeden (JGJ) | ✅ stark | 4 | Zwei Konstellationen: 10 Teams Tête + 17 Teams Doublette (3 Gruppen); Direktvergleich-Sheet validiert |
| KO / Maastrichter | ✅ gut | 4 | KO: 8/16/10-Cadrage mit Folgerunden-Scores (vollständig „gespielter" Bracket); Maastrichter: 12-Teams + 57-Teams-4-Gruppen + Forme |
| Formule_X | ✅ gut | 2 | Vollständiger E2E: Meldeliste + 5 Spielrunden + Rangliste + Teilnehmerliste |
| Sidebar | ✅ gut | 2 | Panel-Factory, Version-Label, Sheet-Toggle |
| Blattschutz | ✅ gut | 2 | StyleRestrictions, CellStyleHelper-Verhalten |
| Add-in `PTM.ALG.*` | ⚠️ minimal | 1 | `GlobalImplUITest` testet nur `INTPROPERTY` |
| Konfigdialog | ❌ keine | 0 | `MainKonfigDialogTest` ist `@Disabled` |
| Beispielturnier | ✅ grün | 1 | 17/17 Tests grün |

## Test-Tiefe

- **Echter End-to-End-Workflow** für alle Turniersysteme vorhanden: jeweils `*TurnierTestDatenUITest` erzeugt Meldeliste → Spielplan/Runden mit Zufallsergebnissen → Rangliste und validiert jedes Sheet gegen JSON-Referenzen.
- **Reproduzierbarkeit** über `RandomSource.setSeed(42L)` in allen E2E-Tests.
- **Mehrere Konstellationen pro System** (z. B. ungerade vs. gerade Teamzahl, Gruppen-Setup, Cadrage-Pfad, Forme-Phase) decken Edge Cases ab.
- Blattschutz wird isoliert getestet, nicht im Kontext kompletter Tournament-Abläufe.

## Verbleibende Lücken (priorisiert)

1. **Konfigdialog** – `MainKonfigDialogTest` reaktivieren oder durch headless-fähigen Ansatz ersetzen. Höchste Lücke, da kein UI-Test existiert.
2. **Add-in-Funktionen** – nur `INTPROPERTY` getestet; `DIREKTVERGLEICH`, `MEDIAN`, `PUNKTE` u. a. (`addins/GlobalImpl`) ungetestet.
3. **Supermelee Endrangliste** – `SupermeleeTurnierTestDaten` erzeugt nur Spieltag-Ranglisten, keine kumulative Endrangliste. Ggf. `EndranglisteSheetUpdate` zusätzlich aufrufen und validieren.
4. **Schweizer Rangliste-Tiebreaker** – Edge Cases (mehrere Teams mit identischen Punkten) bisher nicht explizit getestet.

## Historische Vertiefungen (umgesetzt)

Stand frühere Analyse (vor Mai 2026): Vier identifizierte Lücken in der Test-Tiefe sind inzwischen geschlossen.

- **Supermelee**: bisher nur Spieltag-Ranglisten 1 und 5 validiert → jetzt alle fünf (`b19b3614`).
- **JGJ**: nur 10 Teams Tête → zweite Konstellation 17 Teams Doublette + Direktvergleich-Sheet ergänzt (`304e06b0`).
- **Liga**: nur 6-Teams-Standard → zusätzlich 7-Teams-Freispiel-Variante; Rangliste-Range-Bug nebenbei gefixt (`602ab742`).
- **KO**: Bracket nur bis Runde 1 + Cadrage gefüllt → Folgerunden-Scores (Halbfinale, Finale) ergänzt; alle JSON-Referenzen zeigen jetzt vollständig „gespielten" Bracket (`0a81bde5`).

## Verifikation

- `find src/test -name "*UITest.java" | wc -l` → aktuell 41.
- `./gradlew test` läuft die Unit-Tests; `./gradlew uiTests` führt die UI-Tests durch (benötigt vorher `./gradlew reinstallExtension`).
- Reproduzierbar mit `RandomSource.setSeed(42L)`; bei Algorithmen-Änderungen Referenz-JSONs neu erfassen (siehe CLAUDE.md, Abschnitt „JSON-Referenzen erfassen / aktualisieren").
