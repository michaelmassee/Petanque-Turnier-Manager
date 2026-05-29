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
│  Ladereihenfolge: LETZT → ÄLTESTE (neueste Spieltage zuerst eingelesen)
│  Fallback-Abbau:  älteste werden ZUERST entfernt — neueste bleiben am längsten
│
│  Konkrete Retry-Stufen bei Spieltag 5 (effMaxSpieltage startet bei 4):
│   Stufe 1: effMaxSpieltage=4 → lädt ST4,ST3,ST2,ST1  + aktueller ST
│   Stufe 2: effMaxSpieltage=3 → lädt ST4,ST3,ST2       + aktueller ST  ← ST1 entfernt
│   Stufe 3: effMaxSpieltage=2 → lädt ST4,ST3           + aktueller ST  ← ST1,ST2 entfernt
│   Stufe 4: effMaxSpieltage=1 → lädt ST4               + aktueller ST  ← nur neuester
│   Stufe 5: effMaxSpieltage=0 → NUR aktueller ST       → BREAK wenn nächster Fehler
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

---

## 5. Nebenwirkung: Gegner-Qualität leidet bei effMaxSpieltage-Reset

Wenn der `effMaxSpieltage`-Reset auf 0 ausgelöst wird (Knotenlimit), lädt
`gespieltenRundenEinlesenMitLimit` nur den laufenden Spieltag. Dabei wird
`addGegner` **nur für die bereits gespielten Runden dieses Spieltags**
wiederhergestellt — die Gegner-Geschichte früherer Spieltage ist für den
Optimizer in `optimiereGegnerPaarung` unsichtbar.

**Effekt**: Paare, die in ST1–3 bereits mehrfach Gegner waren, erscheinen
dem Optimizer als "frisch". Der Score berechnet sich nur aus den laufenden
Spieltag-Runden → der Optimizer vermeidet sie weniger stark.

**Messbare Folge** (analysiert an `super-test-alk.ods`):

| ST | Gegner-Sättigung vorher | Intra-ST-Wiederholungen | Ursache |
|---|---|---|---|
| 1 | 0 % | 1 | Optimizer-Grenze (alle Alternativen gleich schlecht) |
| 2 | 6 % | 1 | dto. |
| 3 | 9 % | 2 | dto. |
| 4 | 13 % | **5** | effMaxSpieltage-Reset → cross-ST-Gegnergeschichte verloren |
| 5 | 13 % | 1 | Reset seltener nötig (Multi-Shuffle-Fix greift) |

Meiners/Radde (ST4): vor ST4 bereits **3× Gegner** in ST1–3, aber beim
Reset unsichtbar → Optimizer sah nur 10 bzw. 20 Pkt statt 40/50 Pkt →
wurde trotz hoher Vorbelastung dreimal in ST4 zusammengelost.

**Fix**: Multi-Shuffle (Commit b4db75fb) reduziert effMaxSpieltage-Resets
drastisch → cross-ST-Gegnergeschichte bleibt erhalten → Optimizer arbeitet
mit vollständiger Information.

### Analyse-Werkzeuge

```bash
# Überblick aller Spieltage (Tabelle: Mitspieler-Wdh, Gegner-Wdh, Crossover)
python3 tools/analyse_alle_spieltage.py turnier.ods

# Kumulierte Gegner-Sättigung + Ursachenanalyse je Runde
python3 tools/analyse_gegner_kumuliert.py turnier.ods

# Detaillierte Scores je Spiel für einen Spieltag
python3 tools/analyse_gegner_kumuliert.py turnier.ods --spieltag 4 --scores

# Einzelner Spieltag (vollständige 5-Punkte-Analyse)
python3 tools/analyse_supermelee_spieltag.py turnier.ods --spieltag 4
```
