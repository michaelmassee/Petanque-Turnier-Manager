
# 🏆 Kapitel 4 – Maastrichter System (Premium-Version)

Das **Maastrichter Turniersystem** ist eine Kombination aus dem **Schweizer System** (als Vorrunde) und anschließender **Aufteilung in leistungsbasierte KO-Turniere** (A, B, C, D). Es eignet sich hervorragend für mittelgroße bis sehr große Turniere und wird in vielen Vereinen und Turnierserien eingesetzt. Die folgende Premium-Version erklärt das System maximal detailliert, inklusive mathematischer Struktur, komplexer Szenarien und praxisrelevanter Sonderfälle.

---
# 1. Grundidee

Das Maastrichter System verbindet:
- die **Fairness** einer Schweizer Vorrunde,
- die **Strukturklarheit** eines KO-Systems,
- die **Breitensportfreundlichkeit** eines ABCD-Systems.

Ablauf:
1. Vorrunde: 3 Schweizer Runden (manchmal 4)
2. Aufteilung in Felder:
   - 3 Siege → A-Turnier
   - 2 Siege → B-Turnier
   - 1 Sieg → C-Turnier
   - 0 Siege → D-Turnier
3. K.O.-Phase in jedem Feld
4. Cadrage, wenn nötig

Das System ist mathematisch stabil, flexibel und erzeugt sehr klare Leistungsgruppen.

---
# 2. Schweizer Vorrunde (3 Runden)

## 2.1 Struktur
Die Vorrunde besteht aus **drei** Spielen pro Team. Diese Anzahl ist ideal, weil sich die Siegverteilung stabil ausbildet:

Nach 3 Runden gibt es folgende Score-Pyramide:
- 1 Team (selten 2): **3 Siege**
- ca. 3–6 Teams: **2 Siege**
- ca. 3–6 Teams: **1 Sieg**
- 1 Team (selten 2): **0 Siege**

Diese Struktur ist ideal für eine Klassifikation in A, B, C, D.

## 2.2 Paarungslogik
Gleiche Regeln wie im Schweizer System (siehe Kapitel 3):
- Paarung nach Score-Gruppen
- kein Rematch
- Float bei ungerader Gruppenanzahl
- Freilos bei ungerader Gesamtzahl

---
# 3. Aufteilung in A-, B-, C- und D-Turniere

## 3.1 Standard: Viertel-Aufteilung

Nach Runde 3 werden alle Teams in eine Rangliste gebracht:
1. zuerst nach Siegen (3:0, 2:1, 1:2, 0:3)
2. bei Gleichstand nach Feinwertung (z. B. Buchholz, Punktdifferenz)

Diese Rangliste wird von oben nach unten in vier gleich große Gruppen aufgeteilt:
- oberes Viertel → A-Turnier
- zweites Viertel → B-Turnier
- drittes Viertel → C-Turnier
- unteres Viertel → D-Turnier

> **Hinweis:** Es kann vorkommen, dass ein 2:1-Team im A-Turnier spielt, während ein anderes
> 2:1-Team nur im B-Turnier landet – entscheidend ist die Platzierung, nicht allein die Bilanz.

## 3.2 Alternative bei sehr großer Teilnehmerzahl: Aufteilung strikt nach Siegen

Nur wenn die Teilnehmerzahl so groß ist, dass alle vier Felder gut besetzt sind, kann alternativ
eine **strikte Aufteilung nach Sieganzahl** verwendet werden:

- **A-Turnier:** alle Teams mit 3:0
- **B-Turnier:** alle Teams mit 2:1
- **C-Turnier:** alle Teams mit 1:2
- **D-Turnier:** alle Teams mit 0:3

Wenn einzelne Felder ungerade sind, wird per **Cadrage** aufgefüllt (siehe Kapitel 5).

---
# 4. KO-Phase in den Feldern

Jedes Feld spielt ein klassisches KO-System:
- Viertelfinale
- Halbfinale
- Finale

Wenn das Feld **keine Zweierpotenz** hat, wird eine **Cadrage** gespielt.

Beispiele:
- 6 Teams → Cadrage auf 4
- 10 Teams → Cadrage auf 8
- 14 Teams → Cadrage auf 8

---
# 5. Cadrage – Berechnung

Formel:
```
Ziel = größte Zweierpotenz ≤ Anzahl
Differenz = Anzahl – Ziel
Cadrage-Teams = 2 × Differenz
```

Beispiel:
- 14 Teams → Ziel 8 → Diff = 6 → Cadrage = 12 Teams → 6 Spiele
- 10 Teams → Ziel 8 → Diff = 2 → Cadrage = 4 Teams → 2 Spiele

---
# 6. Komplexe Szenarien

## Szenario 1: 13 Teams

### Schweizer Vorrunde → Score-Verteilung:
- **3:0 → 1 Team** → A
- **2:1 → 3 Teams** → A (durch Aufstockung)
- **1:2 → 6 Teams** → B
- **0:3 → 3 Teams** → C

### Felder nach Vorrunde:
- A‑Turnier: 4 Teams
- B‑Turnier: 6 Teams
- C‑Turnier: 3 Teams

### Cadragen:
- B: 6 Teams → Cadrage auf 4 → 4 Teams spielen (2 Spiele)
- C: 3 Teams → Sonderfall → Round Robin oder Cadrage auf 2

**Empfohlene Lösung:** 1 Cadrage (2 Teams) → 1 Freilos → Finale.

---
## Szenario 2: 14 Teams

### Score nach 3 Runden:
- 3:0 → 2 Teams
- 2:1 → 4–5 Teams
- 1:2 → 4–5 Teams
- 0:3 → 2 Teams

### Felder:
- A: 2 + 2 = **4 Teams**
- B: 4–5 Teams → Ziel 8 → Cadrage
- C: 4–5 Teams → Ziel 8 → Cadrage
- D: 2 Teams → direktes Finale oder Einbau in Cadrage

### B-Feld Beispiel:
- 5 Teams → Ziel = 4 → Diff = 1 → 2 Teams spielen Cadrage

---
## Szenario 3: 15 Teams

### Schweizer:
- 3:0 → 1 Team
- 2:1 → 4–5 Teams
- 1:2 → 4–5 Teams
- 0:3 → 3 Teams

### Felder:
- A: 1 + 3 = 4
- B: 5
- C: 5
- D: 3

### Cadrage-Ablauf:
- B: 5 → Cadrage auf 4 (2 Teams)
- C: 5 → Cadrage auf 4 (2 Teams)
- D: 3 → Round Robin oder 1× KO + Finale

---
## Szenario 4: 25 Teams

### Schweizer:
Score-Pyramide:
- 3:0 → 1 Team
- 2:1 → ca. 6 Teams
- 1:2 → ca. 11–12 Teams
- 0:3 → 5–6 Teams

### Felder:
- A: 4 Teams
- B: 8 Teams
- C: 8 Teams
- D: 4–5 Teams

### Cadrage:
D-Feld: 5 → Cadrage 2 Teams → ergibt 4

---
## Szenario 5: 40 Teams

### Schweizer (3 Runden):
Score-Struktur:
- 3:0 → 1–2 Teams
- 2:1 → ca. 10–12 Teams
- 1:2 → ca. 18–20 Teams
- 0:3 → ca. 8–10 Teams

### Felder:
A = 2 + 6 = 8
B = 10–12 → Cadrage auf 8
C = 18–20 → Cadrage auf 16
D = 8–10 → Cadrage auf 8

### Beispielrechnung:
C: 19 Teams → Ziel = 16 → Diff = 3 → Cadrage = 6 Teams → 3 Spiele

---
# 7. Diagramm – Gesamtfluss
```
 SCHWEIZER R1
      ↓
 SCHWEIZER R2
      ↓
 SCHWEIZER R3
      ↓
  SCORE-GRUPPEN
      ↓
  A / B / C / D
   (mit Cadrage)
      ↓
 KO-PHASE JE FELD
      ↓
  4 TURNIERSIEGER
```

---
# 8. Vorteile
- ideale Balance aus Fairness & KO-Dramatik
- große Turniere gut abbildbar
- viele Spiele pro Team
- klare Leistungsgruppen

# 9. Nachteile
- Turnierdauer höher als Poule-System
- aufwendige Software nötig (Schweizer)

---
# 10. Zusammenfassung
Das Maastrichter System bietet:
- Schweizer Fairness
- klare ABCD-Struktur
- dynamische KO-Phase
- ideale Turnierlogik für 12–60 Teams

---
# Nächstes Kapitel
➡ Kapitel 5: **KO-System (Premium)**.
