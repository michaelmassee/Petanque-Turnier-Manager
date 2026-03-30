
# 🏆 Kapitel 2 – Das Poule‑A/B‑System (Premium-Version)

Dieses Kapitel beschreibt das vollständige **Poule‑A/B‑Turniersystem** – das Standardformat für viele Pétanque‑Meisterschaften, regionale Wettbewerbe und große Vereinsturniere. Die Darstellung ist maximal detailliert, professionell gegliedert und enthält komplexe Beispiele (16, 14, 28, 32, 50 Teams) sowie Diagrammstrukturen.

---
# 1. Grundidee des Poule‑A/B‑Systems

Das Poule‑A/B‑System kombiniert:
- eine **strukturierte Vorrunde** in Poules (meist 4 Teams),
- ein **vereinfachtes Double‑Elimination‑System**,
- und eine anschließende **A‑ und B‑K.O.-Phase**.

Zentrale Ziele:
- faire Chancen für alle Teams,
- mindestens 2–3 Spiele pro Team in der Vorrunde,
- klare Abtrennung starker und schwächerer Teams für K.O.-Phase,
- Wiederbegegnungen werden vermieden.

Typisch für Meisterschaften in Deutschland.

---
# 2. Struktur der Poules

Standard-Poule: **4 Teams**. 
Sonderfall-Poule: **3 Teams**, wenn die Gesamtzahl ungerade ist.

Diagramm (4er‑Poule):
```
Team 1 ─┐        ┌─ Gewinner A
         ├─ Spiel A ┐
Team 2 ─┘           │
                     ├─ Siegerspiel → Platz 1 (A-Turnier)
Team 3 ─┐           │
         ├─ Spiel B ┘
Team 4 ─┘

Verliererspiel → Platz 4 (B-Turnier)
Barrage → Platz 2 (A) / Platz 3 (B)
```

---
# 3. Ablauf der Vorrunde (Double‑Elimination‑Light)

## Runde 1
- Spiel A: Team 1 vs 2
- Spiel B: Team 3 vs 4

## Runde 2
**Siegerspiel** (A‑Sieger vs B‑Sieger):
- Gewinner: Platz 1 → A‑Turnier
- Verlierer: geht in Barrage

**Verliererspiel** (A‑Verlierer vs B‑Verlierer):
- Gewinner: spielt Barrage
- Verlierer: Platz 4 → B‑Turnier

## Runde 3 – Barrage
- Verlierer des Siegerspiels vs Gewinner des Verliererspiels
- Gewinner → Platz 2 (A‑Turnier)
- Verlierer → Platz 3 (B‑Turnier)

---
# 4. Ergebnisse einer Poule

| Platz | Bilanz | K.O.-Turnier |
|-------|--------|--------------|
| 1     | 2:0    | A-Turnier     |
| 2     | 2:1    | A-Turnier     |
| 3     | 1:2    | B-Turnier     |
| 4     | 0:2    | B-Turnier     |

---
# 5. Sonderfall: 3er‑Poule

Spiele:
1. Team 1 vs Team 2
2. Team 2 vs Team 3
3. Team 1 vs Team 3

Tie‑Break-Regeln:
1. Siege
2. Punktdifferenz
3. direkter Vergleich

Qualifikation:
- Platz 1 → A-Turnier
- Platz 2 → A-Turnier
- Platz 3 → B-Turnier

---
# 6. K.O.-Phase des A‑ und B‑Turniers

Beide Turniere laufen unabhängig:
- Viertelfinale
- Halbfinale
- Finale

Setzlogik „über Kreuz“:
```
Poule 1 (1.) vs Poule 2 (2.)
Poule 3 (1.) vs Poule 4 (2.)
Poule 2 (1.) vs Poule 1 (2.)
Poule 4 (1.) vs Poule 3 (2.)
```

---
# 7. Cadrage (Zwischenrunde)

Wenn eine Turnierstufe keine Zweierpotenz bildet, wird eine Cadrage gespielt.

Beispiele:
- 10 Teams → Cadrage auf 8
- 6 Teams → Cadrage auf 4
- 12 Teams → Cadrage auf 8 (4 Spiele)

Formel:
```
Ziel = größte Zweierpotenz ≤ Anzahl
Differenz = Anzahl – Ziel
Cadrage-Teilnehmer = 2 × Differenz
```

---
# 8. Komplexe Beispiele

## Beispiel 1: 16 Teams (Standardfall)
- 4 Poules à 4 Teams
- 8 Teams ins A‑Turnier
- 8 Teams ins B‑Turnier
- Keine Cadrage notwendig

## Beispiel 2: 14 Teams (mit zwei 3er‑Poules)
Einteilung:
- Poule A: 4 Teams
- Poule B: 4 Teams
- Poule C: 3 Teams
- Poule D: 3 Teams

A‑Turnier:
- 2×2 (A/B)
- 2×2 (C/D)
= 8 Teams

B‑Turnier:
- 2×2 (A/B)
- 1× (C 3. Platz)
- 1× (D 3. Platz)
= 6 Teams → Cadrage → 4 Teams

## Beispiel 3: 28 Teams (7 Poules)
7 Poules erzeugen:
- 7×2 = 14 A‑Teams
- 7×2 = 14 B‑Teams

A‑Turnier: 14 Teams → Cadrage auf 8 → 12 spielen Cadrage
B‑Turnier: 14 Teams → identischer Ablauf

## Beispiel 4: 50 Teams
Poule‑Einteilung:
- 12 Poules à 4 Teams = 48
- 1 Poule à 2 Teams (Sonderfall) → automatisch 1 Spiel

A‑Turnier:
- 13 Poule‑Sieger
- 12 Barrage‑Gewinner
= 25 Teams
→ Cadrage auf 16 (18 Teams spielen Cadrage)

B‑Turnier:
- 25 Teams
→ identischer Cadrage‑Prozess

---
# 9. Diagramm – Gesamtübersicht (ASCII)
```
 ROUND 1        ROUND 2         ROUND 3         KO-PHASE
 ┌─────┐        ┌─────┐         ┌─────┐        ┌────────┐
 │ A1  │─┐    ┌→│ A1W │─┐     ┌→│ P1  │───┐   │ A-VF   │
 │ A2  │─┘    │ │ A1L │─┘     │ │ P2  │   │   │ B-VF   │
 └─────┘      │ └─────┘       │ └─────┘   │   └────────┘
              │               │           │
              └───────────────┴───────────┘
```

---
# 10. Empfehlungen für Turnierleitungen

## Bahnenplanung
- 16 Teams → mindestens 8 Bahnen
- 32 Teams → 16 Bahnen
- 64 Teams → 24–32 Bahnen

## Zeitplanung (Erfahrung)
- Vorrunde 4er‑Poule → 2–3 Stunden
- Vorrunde 3er‑Poule → 1–1.5 Stunden
- KO‑Phase → 3–4 Stunden

## Häufige Probleme
1. zu wenig Bahnen → Verzögerungen
2. fehlende Struktur bei Ausrufen der Spiele
3. unklare Cadrage-Zuteilung

Lösung: Klare Poule‑Boards + Live‑Ergebnisführung.

---
# 11. Zusammenfassung
Das Poule‑A/B‑System ist:
- fair
- sehr gut etablierter Standard
- strukturiert & planbar
- optimal für große Turniere

Das nächste Kapitel ist **Kapitel 3 – Schweizer System (Premium-Version)**.
