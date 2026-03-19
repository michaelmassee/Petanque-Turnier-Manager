# K.-o.-Runde im Boule -- Erweiterte Beschreibung

## 1. Das Grundprinzip: „Alles oder Nichts"

Im K.-o.-System (Knock-out) gilt kompromisslos:\
**Ein Sieg bedeutet Weiterkommen, eine Niederlage das sofortige
Ausscheiden.**

-   Gespielt wird in der Regel **bis 13 Punkte**.
-   Jede Partie ist ein „Finale" -- es gibt **keine zweite Chance**.
-   Zeitlimits sind möglich (z. B. 60 Minuten + 2 Aufnahmen).

------------------------------------------------------------------------

## 2. Die Paarungen: Das Überkreuz-System (Setzliste)

Nach einer Vorrunde werden die Teams anhand ihrer Leistung gesetzt.

### Prinzip:

-   **1 vs. Letzter**, **2 vs. Vorletzter**, usw.
-   Beispiel bei 16 Teams:\
    1--16, 2--15, 3--14 ... (Summe immer 17)

### Ziel:

-   Schutz der stärksten Teams
-   Top-Teams treffen erst spät aufeinander

------------------------------------------------------------------------

## 3. Der Turnierbaum und die „Cadrage"

Ein K.-o.-Turnier benötigt Teilnehmerzahlen wie: **8, 16, 32, 64 ...**

### Lösung bei Abweichung: Cadrage

-   Reduktion auf nächste Zweierpotenz
-   Beispiel:
    -   40 Teams → Ziel: 32
    -   16 Teams spielen Cadrage
    -   8 scheiden aus, 24 haben Freilos

------------------------------------------------------------------------

## 4. Spielorganisation

Typischer Ablauf: - 1/16-Finale\
- Achtelfinale\
- Viertelfinale\
- Halbfinale\
- Finale

Merkmale: - Parallele Spiele auf mehreren Bahnen - Ergebnisse werden
direkt gemeldet - Neue Paarungen werden sofort veröffentlicht

------------------------------------------------------------------------

## 5. Taktische Besonderheiten

### Risikomanagement

-   Mehr Risiko bei Rückstand
-   Kontrolliertes Spiel bei Führung

### Psychologischer Druck

-   Jeder Fehler kann entscheidend sein
-   Hohe Konzentration erforderlich

### Gegneranpassung

-   Gegen schwächere Teams: sicher spielen
-   Gegen starke Teams: Chancen erzwingen

------------------------------------------------------------------------

## 6. Varianten

### B-, C- und D-Turniere

-   Verlierer spielen in Nebenrunden weiter
-   Mehr Spiele für alle Teilnehmer

### Double-K.-o.

-   Ausscheiden erst nach zwei Niederlagen
-   Aufwendiger, aber fairer

### Zeitbegrenzung

-   Häufig bei großen Turnieren
-   Beispiel: 60 Minuten + 2 Aufnahmen

------------------------------------------------------------------------

## 7. Vor- und Nachteile

Vorteile                 Nachteile
  ------------------------ -----------------------------
Klarer Ablauf            Sofortiges Ausscheiden
Hohe Spannung            Zufall spielt größere Rolle
Belohnung der Vorrunde   Wenige Spiele pro Team
Zuschauerfreundlich      Frustrationspotenzial

------------------------------------------------------------------------

## 8. Dynamische Zuteilung: K.-o.-Bäume (A, B, C...) und Cadrage-Regel

Nach der Vorrunde werden die Teams nach Rang auf A-, B-, C-Turniere
(und ggf. weitere) aufgeteilt. Jede Gruppe bekommt ihren eigenen
K.-o.-Baum (Turnierbaum-Sheet).

### Konfigurationsparameter

| Parameter          | Default | Beschreibung |
|--------------------|---------|--------------|
| `maxGruppenGroesse` | 16     | Maximale Teamanzahl pro Gruppe (Zweierpotenz). |
| `minRestGroesse`   | 16      | Mindestzahl für ein eigenes Folgeturnier (Zweierpotenz: 4, 8, 16, 32 …). |

### Aufteilungslogik

Sei `anzTeams` die Gesamtzahl der gemeldeten Teams:

```
volleGruppen = anzTeams / maxGruppenGroesse   (ganzzahlig)
rest         = anzTeams % maxGruppenGroesse
```

**Fall A – rest == 0:** Perfekte Aufteilung, keine Cadrage nötig.

**Szenario 1 – rest ≥ minRestGroesse:**
Der Rest bildet ein eigenständiges Folgeturnier (z. B. Gruppe C).
Cadrage wird dort ausgetragen.

*Beispiel:* 42 Teams, max 16, minRest 16 → Gruppen: 16 | 16 | 10
(Gruppe C mit 10 Teams: Cadrage auf 8, 4 spielen Cadrage, 6 Freilos)

**Szenario 2 – 0 < rest < minRestGroesse:**
Der Rest ist zu klein für ein eigenes Turnier und wird in die letzte
volle Gruppe gefaltet. Cadrage wird in dieser vergrößerten Gruppe
ausgetragen.

*Beispiel:* 34 Teams, max 16, minRest 16 → Gruppen: 16 | 18
(Gruppe B mit 18 Teams: Cadrage auf 16, 4 spielen Cadrage, 14 Freilos)

**Sonderfall – keine vollen Gruppen (rest < minRestGroesse):**
Alle Teams kommen in eine einzige Gruppe (kein Buchstabe-Suffix).

### Cadrage innerhalb einer Gruppe

Die Cadrage-Berechnung (→ `CadrageRechner`) arbeitet unabhängig pro
Gruppe auf Basis der tatsächlichen Gruppengrö­ße. Sie ist von der
Aufteilungslogik vollständig entkoppelt.

------------------------------------------------------------------------

## 9. Fazit

Die K.-o.-Runde ist das Herzstück vieler Boule-Turniere:

-   Hohe Spannung und klare Entscheidungen
-   Belohnt Leistung und mentale Stärke
-   Optimal in Kombination mit Vorrunden und Trostrunden
