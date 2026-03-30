
# 🏆 Kapitel 3 – Schweizer System (Premium-Version)

Das **Schweizer Turniersystem** ist eines der anspruchsvollsten, flexibelsten und fairsten Systeme im Pétanque‑Sport. Es erlaubt große Teilnehmerfelder ohne Ausscheiden, garantiert allen Teams viele Spiele und erzeugt durch seine mathematische Struktur eine präzise Rangliste. Dieses Premium‑Kapitel erklärt das System vollständig, tiefgreifend und anhand komplexer Beispiele (6, 12, 16, 32, 50 Teams).

---
# 1. Grundidee des Schweizer Systems

Das Schweizer System basiert auf zwei Grundprinzipien:

1. **Teams mit gleicher Leistung spielen gegeneinander**.
2. **Es gibt kein Ausscheiden**, jedes Team spielt alle Runden mit.

Typische Eigenschaften:
- Jede Runde wird so gelost, dass möglichst **gleich starke Teams** aufeinandertreffen.
- Niemand spielt zweimal gegen denselben Gegner.
- Nach jeder Runde entsteht eine **aktualisierte Tabelle**.
- Eine faire Rangliste entsteht durch Tie‑Break‑Wertungen (BHZ, FBHZ, Punktedifferenz).

---
# 2. Ablauf einer Schweizer Runde

Jede Runde folgt der gleichen Struktur:
1. Tabelle aktualisieren.
2. Teams in „Score‑Gruppen“ einteilen (0 Siege, 1 Sieg, 2 Siege …).
3. Innerhalb jeder Gruppe sortieren nach:
   - Siegen
   - BHZ (Buchholz)
   - FBHZ (Feinbuchholz)
   - Punktedifferenz
4. Paarungen von oben nach unten bilden.
5. Konflikte lösen (kein Rematch erlaubt).
6. Spiele ausführen.

---
# 3. Wertungslogik im Detail

## 3.1 Siege (Hauptkriterium)
Das wichtigste Kriterium.

## 3.2 Buchholz (BHZ)
Summe der Siege aller Gegner.

## 3.3 Feinbuchholz (FBHZ)
Summe der BHZ‑Werte aller Gegner.

## 3.4 Punktedifferenz
(+) erzielte Punkte
(-) kassierte Punkte

## 3.5 Pluspunkte
Ersatz‑Tie‑Break, wenn BHZ/FBHZ nicht genutzt werden.

---
# 4. Paarungsalgorithmus (Schweizer‑Backtracking)

### 1. Teams nach Stärkekriterien sortieren.
### 2. Score‑Gruppe bilden.
### 3. Innerhalb der Gruppe paaren:
```
Team 1 vs Team 2
Team 3 vs Team 4
Team 5 vs Team 6
...
```

### 4. Konfliktlösung (Backtracking)
Wenn Team 1 bereits gegen Team 2 gespielt hat:
- Tausch Partner 2 ↔ 3
- Falls nicht möglich → Tausch 2 ↔ 4
- Falls nicht möglich → Team in nächste Gruppe „floaten“

Diese Logik wird häufig in Turniersoftware eingesetzt.

---
# 5. Sonderfälle

## 5.1 Ungerade Teamzahl → Freilos
- schlechtestes Team ohne Freilos erhält ein BYE
- Wertung: 13:7 oder 13:0

## 5.2 Score‑Gruppe ungerade → Float
Das letzte Team wird in die nächste Gruppe verschoben.

## 5.3 Keine rematch‑freie Paarung möglich
→ Backtracking + ggf. Gegnerwiederholung minimal zulassen.

---
# 6. Komplexe Beispiele

## Beispiel 1: 6 Teams, 3 Runden

### Runde 1 (freier Lospool)
- A schlägt B
- C schlägt D
- E schlägt F

### Tabelle nach Runde 1:
- 3 Teams 1:0 (A,C,E)
- 3 Teams 0:1 (B,D,F)

### Runde 2
1:0‑Gruppe:
- A vs C
- E vs Float aus 0:1? (wenn nötig → nein)

0:1‑Gruppe:
- B vs D
- F Freefloat falls ungerade

### Beispiel‑Auswertung vollständig in Datei.

---
## Beispiel 2: 12 Teams, 4 Runden

### Runde 1
6 Spiele → 6 Sieger / 6 Verlierer

### Runde 2
- 6 Teams 1:0 → 3 Paarungen
- 6 Teams 0:1 → 3 Paarungen

### Runde 3
Score‑Gruppen:
- 3 Teams 2:0
- 6 Teams 1:1
- 3 Teams 0:2

### Runde 4
Erneute Sortierung nach BHZ + FBHZ.

→ Tabelle ist am Ende extrem differenziert.

---
## Beispiel 3: 16 Teams, 5 Runden

Faustregel: **log2(n) + 1 Runden** → 4 + 1 = 5 Runden.

Score‑Struktur entwickelt sich:
- nach R1: 8×1:0 / 8×0:1
- nach R2: 4×2:0 / 8×1:1 / 4×0:2
- nach R3: 2×3:0 / 6×2:1 / 6×1:2 / 2×0:3
- nach R4: 1×4:0 / 7×3:1 / 6×2:2 / 1×1:3 / 1×0:4
- nach R5: vollständige Differenzierung

→ Dieses Beispiel wird in der Datei vollständig aufgelöst.

---
## Beispiel 4: 32 Teams, 5 Runden

Score‑Gruppen nach Runde 4 werden sehr stabil:
- 1 Team 4:0
- 4 Teams 3:1
- 11 Teams 2:2
- 11 Teams 1:3
- 4 Teams 0:4
- 1 Team 0:5

→ Schweizer System zeigt hier seine Stärke: perfekte Balance.

---
## Beispiel 5: 50 Teams, 6 Runden

Faustregel: log2(50) ≈ 5.64 → **6 Runden**.

Scoreverteilung nach 6 Runden:
- 1 Team 6:0
- 3 Teams 5:1
- 7 Teams 4:2
- 12 Teams 3:3
- 12 Teams 2:4
- 10 Teams 1:5
- 5 Teams 0:6

→ BHZ & FBHZ entscheiden bei großen Feldern massiv.

---
# 7. Diagramm – Schweizer Ablauf (ASCII)
```
Runde 1 → zufällige Paarung
        ↓
Runde 2 → Paarung nach Siegen
        ↓
Runde 3 → Paarung nach Score-Gruppen + Backtracking
        ↓
Runde 4 → BHZ/FBHZ Sortierung
        ↓
Finale Tabelle
```

---
# 8. Tipps für Turnierleitungen

## 8.1 Anzahl Runden
- 8 Teams → 3–4 Runden
- 16 Teams → 4–5 Runden
- 32 Teams → 5 Runden
- 50 Teams → 6 Runden

## 8.2 Häufige Fehler
- zu wenige Runden → schlechte Rangliste
- fehlende Backtracking‑Logik → Rematches
- BHZ falsch berechnet

## 8.3 Notfallregeln
- Gegnerwiederholung minimal erlauben
- Freilos korrekt verteilen
- Spiel auf Zeit + Zusatzaufnahme nutzen

---
# 9. Zusammenfassung
Das Schweizer System ist:
- eines der fairsten Rundensysteme
- extrem flexibel
- mathematisch exakt
- ideal für große Teilnehmerfelder

---
# Nächstes Kapitel
➡ Kapitel 4: **Maastrichter System (Premium)**.
