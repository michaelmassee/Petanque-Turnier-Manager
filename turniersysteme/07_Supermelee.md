
# 🏆 Kapitel 7 – Supermêlée (Premium-Version)

Das **Supermêlée‑System** ist eines der dynamischsten und sozialsten Turnierformate im Boule-/Pétanque‑Sport. Es eignet sich perfekt für Vereinsabende, offene Turniere, Ranglistensysteme und Großveranstaltungen mit wechselnden Teams. Dieses Kapitel erklärt das System maximal detailliert – inklusive vollständiger Algorithmik, Teamformel‑Logik, Wertungen, komplexer Beispiele und ASCII‑Diagrammen.

---
# 1. Grundidee des Supermêlée

Supermêlée bedeutet: **jede Runde neue Teams – aber Einzelwertung**.

Wesentliche Merkmale:
- Spieler melden sich **einzeln**, nicht als Teams.
- Teams werden **in jeder Runde neu gelost**.
- Es gibt **keine festen Partner**, kein festes Doublette/Triplette.
- Gespielt wird **ohne Freilose**, da flexible Teamaufteilung jede Spielerzahl abdeckt.
- Jede Person sammelt **eigene Siege und Punkte**.

Dieses System ist kein KO‑System – jede Runde ist eigenständig.

---
# 2. Ablauf eines Supermêlée-Turniers

## 2.1 Anmeldung
Alle Spieler melden sich einzeln an und bilden den **Spielerpool**.

## 2.2 Runde generieren
Vor jeder Runde:
1. aktueller Spielerpool bestimmen
2. Teams per Los/Software bilden
3. Paarungen erzeugen
4. ggf. Triplette/Doublette mischen, damit alle spielen können

## 2.3 Spielmodus
Es gibt zwei Varianten:
- **Regulär bis 13** (Standard)
- **Zeitlimit (z. B. 60 Min. + 1 Aufnahme)**

**Unentschieden sind nicht möglich** → bei Zeitlimit entscheidet Zusatzaufnahme.

## 2.4 Wertung erfassen
Nach jeder Runde:
- Sieg/Niederlage
- Punkte + / -
- Differenz

## 2.5 Nächste Runde
Alle Teams werden **wieder neu gelost** → keine festen Partner.

---
# 3. Teamgrößen & Aufteilung – Mathematische Logik

Da ohne Freilose gespielt wird, muss die Spielerzahl **N** auf Teams à 4, 5 oder 6 Spieler aufgeteilt werden:
- 4 Spieler = Doublette vs Doublette
- 5 Spieler = Triplette vs Doublette
- 6 Spieler = Triplette vs Triplette

Die Verteilung folgt festen Modulo-Regeln:

---
## 3.1 Hauptmodus: **Doublette** (Auffüllung mit Triplette)

Berechne: `N % 4`

| Rest | Teamaufteilung |
|------|----------------|
| 0    | Alles Doublettes (perfekt) |
| 1    | 1× 5er‑Partie (D vs T) |
| 2    | 1× 6er‑Partie (T vs T) |
| 3    | 1× 6er + 1× 5er‑Partie |

---
## 3.2 Hauptmodus: **Triplette** (Auffüllung mit Doublette)

Berechne: `N % 6`

| Rest | Teamaufteilung |
|------|----------------|
| 0    | Alles Triplettes |
| 5    | 1× 5er‑Partie |
| 4    | 1× Doublette‑Partie |
| 3    | 1× Doublette + 1× 5er |
| 2    | 2× Doublette |
| 1    | 2× Doublette + 1× 5er |

---
# 4. Algorithmus für die Teamlosung

Supermêlée verwendet eine **partnerfreie Loslogik**:

1. Spieler zufällig mischen.
2. Gruppen zu 4/5/6 bilden (je nach Modulo‑Regel).
3. Partner dürfen **nicht zweimal** in gleichen Teams sein.
4. Gegnerwiederholungen möglich, aber zu minimieren.

ASCII‑Diagramm:
```
Spielerpool → Shuffle → Blockbildung → Teamvalidierung → Paarungen
```

---
# 5. Wertung & Rangliste

Jeder Spieler führt eine **Einzelwertung**.

Rangfolgekriterien:
1. **Siege** (höchste Priorität)
2. **Spieldifferenz** (gewonnene – verlorene Spiele)
3. **Punktdifferenz** (Punkte+ – Punkte–)
4. **Punkte+** (erzielte Punkte)

## 5.1 Fehlrunden
Wenn ein Spieler früher geht oder später kommt:
- Runde gilt als „Fehlrunde“
- Wertung: **0:13 Niederlage** (–13)

---
# 6. Komplexe Beispiele

## Beispiel 1: 11 Spieler
Doublette-Modus (`11 % 4 = 3`):
- 1× Triplette vs Triplette = 6 Spieler
- 1× Doublette vs Triplette = 5 Spieler

### Ergebnis:
- Alle 11 spielen
- Kein Freilos

---
## Beispiel 2: 15 Spieler
Triplette-Modus (`15 % 6 = 3`):
- 1× Doublette (4)
- 1× gemischt (5)
- verbleibend: 6 → Triplette vs Triplette

### Gesamt:
- 3 Partien → 6 + 5 + 4 = 15 Spieler

---
## Beispiel 3: 22 Spieler
Doublette-Modus (`22 % 4 = 2`):
- 1× Triplette vs Triplette (6)
- 4er‑Teams für übrige 16 → 4 Partien

---
## Beispiel 4: 37 Spieler
Triplette-Modus (`37 % 6 = 1`):
- 2× Doublette (4+4)
- 1× gemischt (5)
- Rest 24 → 4× Triplette‑Partien (6 pro Partie)

Komplette Aufteilung:
- 24 Spieler = 4×6 → Triplette vs Triplette
- 5 Spieler → gemischt
- 8 Spieler → Doublette vs Doublette (2 Partien)

### Gesamt: 4 + 2 + 1 = **7 Partien**, alle 37 Spieler aktiv.

---
# 7. ASCII‑Diagramm der Teamaufteilung
```
N Spieler
 ↓
Modulo‑Berechnung
 ↓
Teamgrößen definieren (4/5/6)
 ↓
Shuffle
 ↓
Teamblöcke bilden
 ↓
Paarungen erzeugen
```

---
# 8. Profi-Tipps für Turnierleitungen

## 8.1 Bahnenbedarf
Partien pro Runde = `N / 2` (gerundet). Beispiel:
- 30 Spieler → 15 Spielerpaare → ca. 7–8 Bahnen

## 8.2 Zeitmanagement
- 60 Minuten + 1 Aufnahme funktioniert am besten
- Ergebnisse direkt digital erfassen

## 8.3 Fairness erhöhen
- Software mit Partnervermeidung verwenden
- Punktdifferenz bei Überbelegung nicht überbewerten

## 8.4 Häufige Fehler vermeiden
- zufällige Teambildung ohne Partner‑Check → Wiederholungen
- falsche Behandlung von Fehlrunden

---
# 9. Zusammenfassung
Das Supermêlée ist:
- extrem flexibel
- sozial und abwechslungsreich
- fair in der Einzelwertung
- ideal für Mannschaftsmischung und Vereinsabende

---
# Nächstes Kapitel
➡ **Kapitel 8: Vergleich aller Systeme (Premium)**.
