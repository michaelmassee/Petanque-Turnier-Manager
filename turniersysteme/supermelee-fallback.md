# SuperMelee: Vollständige Fallback-Kette & Constraint-Modell

## 1. Vollständige Fallback-Kette

```
SpielrundeDelegate.neueSpielrunde()
│
│  effMaxSpieltage = min(Konfiguration, aktuelleSpielTagNr - 1)
│  Beispiel Spieltag 5, Konfig=10 → effMaxSpieltage = 4
│
├─ WHILE meleeSpielRunde == null:
│   │
│   ├─ Versuch: neueSpielrundeTripletteMode(rndNr, meldungen, ...)
│   │    │
│   │    └─ generiereRundeMitDummies()
│   │         └─ generiereRundeMitFairnessConstraint()
│   │              │  (für jede Fairness-Schwelle minKlein+1 .. maxKlein)
│   │              └─ generiereRundeMitFesteTeamGroese()
│   │                   10 Shuffles × (Pass 1: 200K Knoten) × (Pass 2: 200K Knoten)
│   │
│   ├─ [Erfolg] → Runde ins Sheet schreiben, fertig
│   │
│   └─ [AlgorithmenException]:
│        • effMaxSpieltage <= 0? → BREAK  (letzter Versuch war mit 0)
│        • sonst: effMaxSpieltage--
│          resetAllHistorie() + gespieltenRundenEinlesenMitLimit(effMaxSpieltage)
│          → weiter in der WHILE-Schleife
│
│  Konkrete Retry-Stufen bei Spieltag 5 (effMaxSpieltage startet bei 4):
│   Stufe 1: effMaxSpieltage=4 → Spieltage 4,3,2,1 + aktueller ST geladen
│   Stufe 2: effMaxSpieltage=3 → Spieltage 4,3,2   + aktueller ST geladen
│   Stufe 3: effMaxSpieltage=2 → Spieltage 4,3     + aktueller ST geladen
│   Stufe 4: effMaxSpieltage=1 → Spieltag 4        + aktueller ST geladen
│   Stufe 5: effMaxSpieltage=0 → NUR aktueller ST  → BREAK wenn nächster Fehler
│
├─ [meleeSpielRunde == null nach WHILE]:
│   └─ erzeugeZufallsRunde()  ← NOTNAGEL: rein zufällig, KEINE Constraints
│        └─ Erfolg → Runde ins Sheet + WARN-MessageBox "Zufalls-Spielplan"
│        └─ AlgorithmenException (nur bei ungültiger Spieleranzahl)
│             → ERROR-Dialog, Sheet wird gelöscht, RuntimeException
│
└─ Erfolg mit Lockerung → konfigurationSheet.setMaxAnzGespielteSpieltage(effMaxSpieltage)
   (Konfiguration wird dauerhaft auf den erfolgreichen Wert reduziert)
```

**Wichtig**: Der aktuelle Spieltag wird IMMER geladen — unabhängig von `effMaxSpieltage`.
Bei `effMaxSpieltage=0` enthält die Constraint-Matrix nur die Runden des laufenden Spieltags.

---

## 2. Constraint-Modell: warImTeamMit vs. warImSpielMit

### warImTeamMit (Hard Constraint → `matrix[][]`)

**Bedeutung**: "Diese zwei Spieler dürfen NICHT ins gleiche Team."

**Wann `true`**:
```java
// Spieler.warImTeamMit(other):
return gleicheSetzPos(other)              // beide SetzPos > 0 und identisch
    || warImTeamMit.contains(other.getNr()); // schon einmal zusammen im Team
```

**Wann akkumuliert**: Bei `Team.addSpielerWennNichtVorhanden(spieler)`:
- Jeder neue Spieler im Team wird mit ALLEN bisherigen Mitspielern gegenseitig eingetragen
- Passiert beim Generieren (via `buildSpielRunde`) UND beim Einlesen der Sheets (via `gespieltenRundenEinlesen`)

**Kapazität** (Triplette-Modus, 12 Spieler):
- Pro Runde: +2 Einträge pro Spieler (2 Mitspieler im Dreier-Team)
- Erschöpft bei: 11 mögliche Partner / 2 pro Runde → ab Runde 6 keine neuen Kombinationen
- → AlgorithmenException "alle Kombinationen ausgeschöpft" (schnell erkannt, wenige Knoten)

---

### warImSpielMit (Soft Constraint → `softMatrix[][]`)

**Bedeutung**: "Diese zwei Spieler waren schon in derselben Partie — als Mitspieler ODER als Gegner."

**Wann `true`**:
```java
// Spieler.warImSpielMit(other):
return warImSpielMit.contains(other.getNr());
```

**Wann akkumuliert**: In `protokolliereWarImSpielMit()` nach jeder Runde:
- Pro Partie (Spiel) = 2 Teams gegeneinander, z.B. 3+3 = 6 Spieler → C(6,2) = 15 neue Paare
- Alle Spieler aus TeamA + TeamB werden gegenseitig eingetragen

**Kapazität** (Triplette, 12 Spieler):
- Pro Runde pro Spieler: +5 Einträge (2 Mitspieler + 3 Gegner)
- Bereits nach Runde 3: ~15 Einträge bei 11 möglichen → de facto gesättigt

---

### Verwendung im Backtracking

```
generiereRundeMitFesteTeamGroese():

  matrix[][]     = aufgebaut aus warImTeamMit()   → Hard: darf NICHT zusammen sein
  softMatrix[][] = aufgebaut aus warImSpielMit()  → Soft: besser nicht zusammen sein

  Pass 1: backtrack mit unionMatrix = matrix OR softMatrix
          → Lösung ohne jegliche Crossover-Wiederholung
          → Scheitert wenn keine solche Lösung existiert (dichte softMatrix)

  Pass 2: backtrack mit nur matrix (hardMatrix)
          → Soft-Constraints nur noch als Value-Ordering-Tie-Breaker
          → Findet Lösung sobald harte Constraints eine erlauben

  Jeder Pass bekommt EIGENEN Knotenzähler (200K Limit).
  Bis zu 10 Shuffles pro Aufruf.
```

---

## 3. Constraint-Sättigung pro Spieltag-Konstellation

| Szenario | matrix-Dichte | softMatrix-Dichte | Verhalten |
|---|---|---|---|
| 12 Spieler, Runde 1 | 0% | 0% | Beide Pässe trivial lösbar |
| 12 Spieler, nach 3 Runden | ~55% | ~100% | Pass 1 scheitert in 4-5 Knoten, Pass 2 findet Lösung |
| 12 Spieler, nach 6 Runden | ~100% | ~100% | Alles TRUE → erschöpft, Fallback greift |
| 30 Spieler, nach 20 Runden | ~138% (all-true) | >> 100% | All-true → Forward-Check sofort, effMaxSpieltage-Lockerung |

**Warum "wenige Runden" das Problem auslöste** (Szenario "Zeile 2"):
- `softMatrix` bereits voll → `unionMatrix ≈ all-true`
- Aber: "all-true" → Forward-Check scheitert in **wenigen Knoten** (gut!)
- Das Problem war "moderat dichte" softMatrix: Pass 1 suchte tief, erschöpfte das Budget
- Pass 2 hatte durch den geteilten Zähler kein Budget mehr
- Fix: eigener Zähler pro Pass → Pass 2 hat immer volles Budget

---

## 4. Relevante Klassen & Methoden

| Klasse | Methode | Zweck |
|---|---|---|
| `SpielrundeDelegate` | `neueSpielrunde()` | Fallback-Schleife mit effMaxSpieltage |
| `SpielrundeDelegate` | `gespieltenRundenEinlesenMitLimit()` | Geschichte nach effMaxSpieltage laden |
| `SuperMeleePaarungenV2` | `generiereRundeMitFesteTeamGroese()` | Backtracking mit Multi-Shuffle |
| `SuperMeleePaarungenV2` | `erzeugeZufallsRunde()` | Letzter Ausweg: keinerlei Constraints |
| `SuperMeleePaarungenV2` | `protokolliereWarImSpielMit()` | softMatrix-Einträge nach jeder Runde |
| `Team` | `addSpielerWennNichtVorhanden()` | warImTeamMit-Einträge beim Generieren |
| `Spieler` | `warImTeamMit(other)` | Hard-Constraint-Abfrage (+ SetzPos) |
| `Spieler` | `warImSpielMit(other)` | Soft-Constraint-Abfrage |
