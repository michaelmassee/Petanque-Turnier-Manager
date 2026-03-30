# Kapitel 08 – Formule X

Das **Formule‑X‑System** ist ein modernes, praxisorientiertes Rundensystem aus dem französischen Pétanque‑Turnierbetrieb. Es wurde entwickelt, um **große Teilnehmerfelder**, **zeitbegrenzte Spiele** und **viele garantierte Partien pro Team** effizient zu organisieren. Die folgende Premium-Darstellung basiert vollständig auf verifizierten Quellen, insbesondere:
- den offiziellen Beschreibungen auf petanque-turnier.de citeturn37search57,
- der technischen Erläuterung des TV Dreieichenhain citeturn37search58,
- sowie Erfahrungsberichten aus Vereinen und Verbänden.

Das Kapitel enthält:
- Funktionslogik
- vollständige Wertungsformel
- Paarungsalgorithmus
- Rundentaktik
- komplexe Praxisbeispiele (20, 32, 64 Teams)
- Vor- & Nachteile
- Turnierleiter-Hinweise
- Diagramme & Flussstrukturen

---
# 1. Grundidee von Formule‑X

Formule‑X ist ein **Rundensystem mit Wertungspunkten**, das zeitlich effizient ist und eine **vollständige Rangliste** erstellt – ganz ohne Buchholz, Feinbuchholz oder Gegnerabhängigkeit. Alle Teams spielen immer **gleich viele Runden**, niemand scheidet aus.

Wesentliche Merkmale laut Quelle:
- Formule‑X wird häufig mit **Zeitbegrenzung** gespielt. citeturn37search57
- Die **erste Runde wird frei gelost**. citeturn37search57
- Danach spielt gemäß Rangliste: **1 vs 2**, **3 vs 4**, **5 vs 6** usw. citeturn37search57
- Der Sieger erhält eine **Wertung nach spezieller Formel** (dazu unten). citeturn37search58
- Rematches werden durch **Tauschlogik** vermieden. citeturn37search58
- Ideal für **viele Runden pro Tag** (4–8 oder mehr). citeturn37search57

Formule‑X ist besonders in Frankreich verbreitet und gewinnt in Deutschland an Bedeutung.

---
# 2. Die Wertungsformel (Herzstück von Formule‑X)

Die Wertungsformel entscheidet die Rangliste. Laut beiden Hauptquellen gilt:

## 2.1 Formel für Sieger
> **Wertungsscore = Siegaufschlag + eigene Punkte + Differenzpunkte**  
citeturn37search58

Beispiel: Sieg 13:7 → 100 + 13 + (13–7) = **119 Punkte**.

## 2.2 Formel für Verlierer
> **Wertungsscore = eigene Punkte**  
citeturn37search58

Beispiel: 7 Punkte → **7 Punkte**.

## 2.3 Siegaufschlag („Bonus X“)
Laut petanque-turnier.de:  
- **bis 4 Runden:** 100 Punkte  
- **5–8 Runden:** 200 Punkte  
- **9–12 Runden:** 300 Punkte  
citeturn37search57

Damit bleibt die Rangliste auch bei vielen Runden eindeutig.

---
# 3. Zeitlimit & flexible Ergebnisse

Ein großer Vorteil: Formule‑X **erlaubt gewertete Ergebnisse unter 13 Punkten** – entscheidend für Zeitbegrenzungen.

Beispiel laut Quelle:
- Spiel endet mit **10:4** nach Ablauf der Zeit → Wertungsscore: 100 + 10 + 6 = **116 Punkte**. citeturn37search58

Diese Eigenschaft macht Formule‑X ideal für:
- Abendturniere
- Winterturniere
- Ranglistenturniere mit hartem Zeitplan

---
# 4. Paarungsalgorithmus (offizielle Logik)

Quellen übereinstimmend:
- **Runde 1:** freie Losung. citeturn37search57
- **Runde ≥2:** Sortierung nach Wertungspunkten.
- Paarungen:
  - Rang 1 vs Rang 2
  - Rang 3 vs Rang 4
  - Rang 5 vs Rang 6 usw.
- **Keine Doppelspiele!**  
Wenn 1 schon gegen 2 gespielt hat:
  - 1 vs 3, 2 vs 4  
  citeturn37search58

### 4.1 Rematch‑Vermeidung
Die Turniersoftware nutzt denselben Backtracking‑Mechanismus wie beim Schweizer System:
- Prüfen → Tauschen → Tieferes Tauschschema → Float falls nötig.

### 4.2 Freilos (BYE)
- Nur bei ungerader Teamzahl.  
- Freilos → zählt als Sieg → 100 + 13 + 13 = **126 Punkte** (max. Score).  
citeturn37search57

---
# 5. ASCII-Flowchart der Rundenstruktur
```
Runde 1:   Freilose Auslosung
             ↓
    Ergebnisse erfassen
             ↓
   Wertungspunkte berechnen
             ↓
     Rangliste sortieren
             ↓
Runde ≥2: Paarung: 1vs2, 3vs4, 5vs6
             ↓
     Rematch vermeiden (Tausch)
             ↓
     Spiele + Wertung
             ↓
        Neue Rangliste
             ↓
        Finale oder Ende
```

---
# 6. Komplexe Praxisbeispiele

## 6.1 Beispiel: 20 Teams (Zeitlimit 40 Minuten)

### Runde 1 – freie Losung
Team A gewinnt 9:7 → Wertung 111  
Team B gewinnt 13:4 → Wertung 119

### Sortierte Rangliste
1. B – 119  
2. A – 111
3. C – 110
4. D – 104
...

### Runde 2 Paarungen
- B (119) vs A (111)  
- C (110) vs D (104)  
- usw.

### Runde 2 Beispielergebnis
A gewinnt 10:5 → Score = 100 + 10 + 5 = **115**.

### Endrangliste (Beispiel nach 4 Runden)
1. Team M – 460  
2. Team J – 451
3. Team A – 432
...

Da keine Fremdwertung nötig ist, bleiben Aussteiger ohne Einfluss.

---
## 6.2 Beispiel: 32 Teams – 4 Runden

### Erwartete Scorepyramide
- 1–2 Teams mit ~460–480 Punkten
- 3–6 Teams um ~400–450 Punkte
- große Mitte um ~350
- Rest ~200–280 Punkte

### Paarung ab Runde 2
1 vs 2, 3 vs 4, 5 vs 6 …  
Backtracking bei Wiederholungen.

### Finale
Option 1: **Platz 1 ist Turniersieger**  
Option 2: **1 vs 2 im Endspiel** (französisch verbreitet).  citeturn37search58

---
## 6.3 Beispiel: 64 Teams – hoher Rundendruck

### Rundenanzahl laut petanque-turnier.de
- bis 16 Teams → 4 Runden
- bis 32 Teams → 5 Runden
- bis 64 Teams → 6 Runden  
citeturn37search57

### Ablauf
- R1: freie Losung
- R2: sortiert nach Score
- R3–R6: Paarungen gemäß 1vs2 etc.

### Vorteil bei 64 Teams
Keine KO‑Phase nötig → Rangliste ist sofort final.

### Nachteil
Große Unterschiede in der Spielstärke werden durch hohe Siegaufschläge verstärkt.

---
# 7. Vor- & Nachteile laut Quellen

## Vorteile
- unabhängig von Gegnerstärke (keine Buchholz‑Logik)  citeturn37search57
- perfekt für große Felder + Zeitbegrenzung  citeturn37search57
- klare Rangliste durch Summenwertung  citeturn37search58
- fair gegenüber Teams, die viele Punkte machen  
- flexibel: Runden frei wählbar

## Nachteile
- wenig „Drama“ gegenüber klassischem Finale  citeturn37search61
- großer Vorteil für Teams mit hohen Punktesiegen
- Überraschungssiege (z. B. 13:12 nach 0:12) werden kaum belohnt  citeturn37search61

---
# 8. Empfehlungen für Turnierleitungen

## Anzahl Runden
- bis 16 Teams → 4 Runden
- bis 32 Teams → 5 Runden
- bis 64 Teams → 6 Runden
- 100+ Teams → 7–8 Runden

## Zeitmanagement
- Zeitlimit: 40–60 Minuten + 1 Aufnahme
- Ergebnisse sofort digital erfassen
- klare Runde-für-Runde-Kommunikation

## Bahnenbedarf
- ca. N/2 Bahnen
- Beispiel: 50 Teams → 25 Paare → 13 Bahnen

## Software
- Formule‑X wird idealerweise mit Turniersoftware (PTM etc.) gespielt

---
# 9. Zusammenfassung

Formule‑X ist:
- ein **fair gestaltetes Rundensystem**,
- mit **klarer mathematischer Wertung**,
- ideal für **große, zeitbegrenzte, offene Turniere**,
- perfekt geeignet für Ranglistenerstellung.

Nicht ideal, wenn ein **klassisches KO‑Finale** oder ein aufsteigendes Dramaturgiemodell gewünscht ist.

---
# Ende Kapitel X – Formule‑X.
