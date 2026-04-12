# 🏆 Kapitel 6 – Kaskaden‑KO‑System

Das **Kaskaden‑KO‑System** ist eine erweiterte und hochflexible Turnierform, die in vielen Boule‑Vereinen und größeren Turnierserien genutzt wird. Es basiert auf der Logik bekannter ABCD‑KO‑Systeme, erweitert diese aber beliebig um weitere Ebenen (E, F, G, H …). Ziel ist es, ein Teilnehmerfeld *schrittweise zu filtern*, ohne dass jemand früh ausscheidet, und dennoch am Ende klare Sieger pro Leistungsebene zu erhalten.

Dieses Kapitel ist vollständig ausgearbeitet und umfasst:
- Struktur & Mechanik
- mathematische Ableitung der Ebenen
- Kaskadierungslogik
- Cadrage‑Regeln je Ebene
- Diagramme
- komplexe Beispiele (16, 24, 28, 30, 34, 50 Teams)
- organisatorische Profi‑Tipps

---
# 1. Grundidee des Kaskaden‑KO‑Systems

Im Gegensatz zum einfachen KO‑System scheidet im Kaskaden‑KO niemand nach der ersten Niederlage aus. Stattdessen „fallen“ Teams in **tiefer liegende Consolante‑Turniere**.

**Kaskadenprinzip:**
1. Erste Runde trennt Feld grob → A & C
2. Zweite Runde trennt erneut → A/B & C/D
3. Optional: weitere Kaskaden → E/F/G/H …
4. Wenn gewünschte Mindestanzahl Spiele erreicht:
   - Stop der Kaskade
   - jedes Feld spielt K.O. bis zum Sieger

---
# 2. Struktur der ersten beiden Runden

## Runde 1 – A/C‑Trennung
- Gewinner → **A‑Ebene**
- Verlierer → **C‑Ebene**

## Runde 2 – A/B und C/D
In beiden Ebenen wird gespielt:
- A‑Gewinner bleiben in A, A‑Verlierer → B
- C‑Gewinner bleiben in C, C‑Verlierer → D

Ergebnis nach 2 Runden: **4 Turnierfelder**:
- A
- B
- C
- D

Dies ist identisch zur bekannten ABCD‑Struktur.

---
# 3. Erweiterte Kaskaden (E/F/G/H)

Ab Runde 3 kann man das System beliebig vertiefen:
- Verlierer aus A → **E**
- Verlierer aus B → **F**
- Verlierer aus C → **G**
- Verlierer aus D → **H**

Dies kann mit Runde 4 fortgesetzt werden:
- E‑Verlierer → I
- F‑Verlierer → J …

Es entsteht ein **Kaskadenbaum**.

---
# 4. Start des KO‑Modus

Sobald eine Turnierleitung bestimmt, dass genug „Garantie‑Spiele“ stattgefunden haben (z. B. 2 oder 3), wird das Kaskadieren gestoppt.

Dann gilt:
- A-Feld spielt KO bis zum Sieger
- B-Feld spielt KO bis zum Sieger
- C-Feld … usw.

Ergebnis: mehrere Sieger auf verschiedenen Leistungsniveaus.

---
# 5. Cadrage‑Regeln je Ebene

Da viele Ebenen ungerade oder zu groß für eine Zweierpotenz sind, wird **je Ebene einzeln** eine Cadrage durchgeführt.

Formel:
```
Ziel = größte Zweierpotenz unterhalb der Teams
Diff = Teams – Ziel
Cadrage = 2 × Diff
```

Beispiele:
- 10 Teams → 8 → Diff = 2 → Cadrage = 4 Teams
- 18 Teams → 16 → Diff = 2 → Cadrage = 4 Teams
- 22 Teams → 16 → Diff = 6 → Cadrage = 12 Teams

---
# 6. Diagramm – Kaskadenstruktur (ASCII)
```
ROUND 1:         ROUND 2:                ROUND 3:                 KO-PHASE
────────         ───────                ────────                ─────────
   ALL    → WIN →   A   → WIN →   A     → WIN  →  A-KO → Sieger A
            LOSS →  C   → LOSS →  D     → LOSS →  D-KO → Sieger D

                       A-LOSS → B       → WIN → B-KO → Sieger B
                       C-LOSS → D       → LOSS → E   → Sieger E
```

---
# 7. Komplexe Beispiele

## Beispiel 1: 16 Teams (klassisch)

### Runde 1
- 8 Sieger → A
- 8 Verlierer → C

### Runde 2
- A: 4/4 → 4 bleiben in A / 4 → B
- C: 4/4 → 4 bleiben in C / 4 → D

Felder:
- A: 4
- B: 4
- C: 4
- D: 4

Direktes KO möglich → 4 separate Sieger.

---
## Beispiel 2: 24 Teams (Cadrage nötig)

### Runde 1
- 12/12 → A/C

### Runde 2
A: 6 bleiben / 6 → B
C: 6 bleiben / 6 → D

Felder:
- A: 6 → Cadrage auf 4
- B: 6 → Cadrage auf 4
- C: 6 → Cadrage auf 4
- D: 6 → Cadrage auf 4

→ Pro Feld: 6 Teams → 2 Spiele Cadrage → ergibt 4 für KO.

---
## Beispiel 3: 28 Teams (7er‑Blöcke)

### Runde 1
- 14/14 → A/C

### Runde 2
- A: 7/7 → 7 bleiben A / 7 → B
- C: 7/7 → 7 bleiben C / 7 → D

Felder:
- A = 7 → Cadrage 6 Teams → Ziel 4
- B = 7 → Cadrage 6 Teams → Ziel 4
- C = 7 → Cadrage 6 Teams → Ziel 4
- D = 7 → Cadrage 6 Teams → Ziel 4

→ Pro Feld Finale aus 4 Teams.

---
## Beispiel 4: 30 Teams

### Runde 1
- 15/15 → A/C

### Runde 2
- A: 7/8 → 7 bleiben A / 8 → B
- C: 7/8 → 7 bleiben C / 8 → D

### Felder
A = 7
B = 8
C = 7
D = 8

### Cadragen
- A: 7 → Ziel 4 → Cadrage: 6 Teams
- B: 8 → keine Cadrage
- C: 7 → Cadrage: 6 Teams
- D: 8 → keine Cadrage

---
## Beispiel 5: 34 Teams

### Runde 1
- 17/17 → A/C

### Runde 2
- A: 8/9 → 8 bleiben A / 9 → B
- C: 8/9 → 8 bleiben C / 9 → D

### Felder:
- A = 8 → KO sofort
- B = 9 → Ziel = 8 → Cadrage = 2 Teams
- C = 8 → KO sofort
- D = 9 → Cadrage = 2 Teams

→ In B und D wird je 1 Spiel Cadrage gespielt.

---
## Beispiel 6: 50 Teams (maximale Komplexität)

### Runde 1
25/25 → A/C

### Runde 2
A: 12 bleiben / 13 → B
C: 12 bleiben / 13 → D

### Felder
A = 12 → Ziel 8 → Diff = 4 → Cadrage 8 Teams
B = 13 → Ziel 8 → Diff = 5 → Cadrage 10 Teams
C = 12 → Ziel 8 → Diff = 4
D = 13 → Ziel 8 → Diff = 5

### Beispiel: B‑Feld (13 Teams)
- Ziel = 8
- Diff = 5
- Cadrage = 10 Teams → 5 Spiele
- 5 Sieger + 3 Freilose = 8 → KO

---
# 8. Empfehlungen für Turnierleitungen

## 8.1 Optimale Anzahl Kaskaden
- 2 Kaskaden (A/B/C/D) sind Standard
- 3 Kaskaden (A–H) bei großen Turnieren möglich

## 8.2 Bahnenbedarf
- Runde 1: komplette Feldgröße
- Runde 2: halbe Feldgröße
- KO-Phase: planbar (8, 4, 2 Bahnen)

## 8.3 Turnierdauer
- 16 Teams → 4–5 Stunden
- 32 Teams → 6–8 Stunden
- 50 Teams → 8–10 Stunden

## 8.4 Fehler vermeiden
- Überfüllte Cadrage vermeiden
- klare Baumstrukturen ausgeben
- Verlierer sofort ZUTEILEN, nicht sammeln

---
# 9. Zusammenfassung
Das Kaskaden‑KO‑System ist:
- maximal flexibel
- sehr spielerfreundlich
- planbar und fair
- ideal für mittlere und große Turniere

