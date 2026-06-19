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
│   │                   bis zu 10 Shuffles × Pass 1/2 × n-abhängiges Knotenbudget
│   │                   sammelt den besten Kandidaten, statt beim ersten Treffer zu stoppen
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

## 2. Ausnahme-Team-Fairness — und die Doublette-Modus-Lücke

Wenn die Spieleranzahl nicht durch die Default-Teamgröße teilbar ist, entsteht pro
Runde mindestens ein **Ausnahme-Team** abweichender Größe. Über mehrere Runden soll
niemand wiederholt im Ausnahme-Team landen. Dafür gibt es einen Fairness-Mechanismus —
**aber nur im Triplette-Modus.** Im Doublette-Modus existiert er strukturell nicht.

### Triplette-Modus (Standard = 3er, Ausnahme = Doublette) → Fairness AKTIV

`neueSpielrundeTripletteMode(...)` ruft `generiereRundeMitDummies(..., dummyTeamIstAusnahme = true)`.

1. **Tracking** (`Spieler.anzMalKleinesTeam`): jeder reale Spieler in einem Team
   kleiner als `teamSize` (= Doublette) bekommt `incAnzMalKleinesTeam()`
   — beim Generieren (`generiereRundeMitDummies`) UND beim Wieder-Einlesen der Sheets
   (`SpielrundeDelegate.gespieltenRundenEinlesen`, `defaultTeamGroesse = 3`).
   Der Zähler überlebt damit Session-Neustarts.
2. **Constraint** (`generiereRundeMitFairnessConstraint`): Spieler mit hohem Zähler
   werden per Hard-Constraint gegen die Dummy-Plätze gesperrt. Schwelle startet bei
   `minKlein + 1` (nur die am wenigsten belasteten Spieler dürfen in die Doublette)
   und wird schrittweise bis `maxKlein` gelockert, wenn das Backtracking keine Lösung
   findet. Danach `generiereRundeMitFesteTeamGroese` **ganz ohne** Fairness-Constraint.

→ Garantie ist **weich**: „möglichst nicht wiederholt in der Doublette", aber bei
Constraint-Erschöpfung bewusst aufgegeben, statt die Auslosung scheitern zu lassen.

### Doublette-Modus (Standard = 2er, Ausnahme = Triplette) → Fairness FEHLT

`neueSpielrundeDoubletteMode(...)` ruft `generiereRundeMitDummies(..., dummyTeamIstAusnahme = false)`.
Begründung im Code: die Dummy-Teams werden nach Cleanup zu **Doublettes** (= Default-Größe),
die „Ausnahme" sind die dummylosen **Triplettes** (das *größere* Team).

Konsequenz — die im `false`-Flag steckende, nicht ausgesprochene Lücke:

- **Kein Constraint**: `generiereRundeMitFairnessConstraint` fällt sofort auf den
  unbeschränkten Pfad zurück (`dummyTeamIstAusnahme == false`).
- **Kein Tracking**: `incAnzMalKleinesTeam()` feuert nur bei `team.size() < teamSize`.
  Die Triplette-Ausnahme hat `size == 3`, was nie kleiner als die Default-Größe 2 ist —
  also wird der Counter im Doublette-Modus **nie erhöht**, weder beim Generieren
  (`teamSize == 3`, aber `dummyTeamIstAusnahme == false`) noch beim Einlesen
  (`defaultTeamGroesse == 2`, Bedingung `size < 2` greift nie).

→ Im Doublette-Modus kann ein Spieler **wiederholt im Triplette landen**; es gibt
weder Vermeidung noch Fallback dagegen, weil das Problem nicht erfasst wird.

### Kern der Asymmetrie

| | Triplette-Modus | Doublette-Modus |
|---|---|---|
| Ausnahme-Team | Doublette (**kleiner**) | Triplette (**größer**) |
| `dummyTeamIstAusnahme` | `true` | `false` |
| Counter `anzMalKleinesTeam` | gepflegt (Gen + Einlesen) | **nie inkrementiert** |
| Fairness-Constraint | aktiv, gestuft lockerbar | deaktiviert |
| Fallback bei Erschöpfung | Schwelle → unbeschränkt → Zufall | n/a (kein Constraint) |

Das `dummyTeamIstAusnahme`-Flag und die `size() < teamSize`-Bedingung kodieren beide die
Annahme „Ausnahme = **kleineres** Team". Im Doublette-Modus ist die Ausnahme aber das
*größere* Team → die gesamte Fairness-Logik greift dort nicht. Bewusste (bzw. übersehene)
Asymmetrie, kein Bug im engeren Sinn — eine korrekte Behandlung bräuchte einen eigenen
„war-im-größeren-Ausnahme-Team"-Zähler mit Bedingung `size() > teamSize`.

---

## 3. Constraint-Modell: warImTeamMit vs. warImSpielMit

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
          → Lösung ohne Mitspieler-Crossover
          → bevorzugt gegenüber Pass 2, wenn mindestens ein Pass-1-Kandidat existiert
          → wird vorab übersprungen, wenn die Soft-Dichte eines Spielers
            strukturell zu hoch ist (zu wenig kompatible Team-Partner)

  Pass 2: backtrack mit nur matrix (hardMatrix)
          → Soft-Constraints nur noch als Value-Ordering-Tie-Breaker
          → Fallback innerhalb des Algorithmus, falls Pass 1 keine Lösung liefert

  Jeder Pass bekommt EIGENEN Knotenzähler.
  Das Knotenbudget skaliert mit Spielerzahl:
    n ≤ 24  →  50K Knoten/Pass
    n ≤ 36  → 200K Knoten/Pass
    n ≤ 48  → 400K Knoten/Pass
    n > 48  → 600K Knoten/Pass

  Fairness-Schwellenversuche nutzen ein reduziertes Budget:
    max(20K, maxKnotenProPass(n) / 8)

  Pro Pass werden bis zu 128 vollständige Layouts bewertet.
  Bewertet wird nicht nur die Teambildung, sondern auch die voraussichtliche
  spätere Gegner-Paarung:
    - Gegner-Wiederholung dominiert mit Gewicht 10_000
    - Crossover bleibt Tie-Breaker mit Gewicht 1
    - Gegner-Wiederholung und Crossover werden exklusiv gezählt

  Kleine Gruppen werden bei der simulierten Gegner-Paarung exakt bewertet.
  Große Gruppen (> 12 Teams je Doublette-/Triplette-Gruppe) nutzen eine
  deterministische Greedy-Bewertung, damit die Scoring-Phase nicht faktoriell
  explodiert.

  Bis zu 10 Shuffles pro Aufruf.
  Der beste Kandidat wird über die Shuffle-Versuche hinweg gesammelt:
    - Pass 1 hat Priorität vor Pass 2.
    - Innerhalb derselben Pass-Stufe gewinnt der niedrigste Gegner-Score.
    - Bei perfektem Score 0 wird früh zurückgegeben, solange dadurch kein
      vorhandener Pass-1-Kandidat durch Pass 2 verdrängt wird.
```

---

## 4. Constraint-Sättigung pro Spieltag-Konstellation

| Szenario | matrix-Dichte | softMatrix-Dichte | Verhalten |
|---|---|---|---|
| 12 Spieler, Runde 1 | 0% | 0% | Beide Pässe trivial lösbar |
| 12 Spieler, nach 3 Runden | ~55% | ~100% | Pass 1 scheitert in 4-5 Knoten, Pass 2 findet Lösung |
| 12 Spieler, nach 6 Runden | ~100% | ~100% | Alles TRUE → erschöpft, Fallback greift |
| 30 Spieler, nach 20 Runden | ~138% (all-true) | >> 100% | All-true → Forward-Check sofort, effMaxSpieltage-Lockerung |

**Warum "wenige Runden" früher problematisch waren** (Szenario "Zeile 2"):
- `softMatrix` bereits voll → `unionMatrix ≈ all-true`
- Aber: "all-true" → Forward-Check scheitert in **wenigen Knoten** (gut!)
- Das Problem war "moderat dichte" softMatrix: Pass 1 suchte tief, erschöpfte das Budget
- Pass 2 hatte durch den geteilten Zähler kein Budget mehr
- Fix: eigener Zähler pro Pass → Pass 2 hat immer volles Budget
- Weitere Optimierung: gültige Layouts werden bewertet und über mehrere
  Shuffles verglichen, statt den ersten gültigen Treffer direkt zu übernehmen.

---

## 5. Relevante Klassen & Methoden

| Klasse | Methode | Zweck |
|---|---|---|
| `SpielrundeDelegate` | `neueSpielrunde()` | Fallback-Schleife mit effMaxSpieltage |
| `SpielrundeDelegate` | `gespieltenRundenEinlesenMitLimit()` | Geschichte nach effMaxSpieltage laden |
| `SuperMeleePaarungenV2` | `generiereRundeMitFesteTeamGroese()` | Backtracking mit Multi-Shuffle |
| `SuperMeleePaarungenV2` | `generiereRunde()` | Pass-1/Pass-2-Koordination, bester Kandidat über Shuffles |
| `SuperMeleePaarungenV2` | `berechneLayoutGegnerScore()` | bewertet vollständige Team-Layouts nach erwarteter Gegnerqualität |
| `SuperMeleePaarungenV2` | `erzeugeZufallsRunde()` | Letzter Ausweg: keinerlei Constraints |
| `SuperMeleePaarungenV2` | `protokolliereWarImSpielMit()` | softMatrix-Einträge nach jeder Runde |
| `SuperMeleePaarungenV2` | `generiereRundeMitDummies(...)` | Dummy-Auffüllung; setzt `dummyTeamIstAusnahme` + Counter (nur Triplette) |
| `SuperMeleePaarungenV2` | `generiereRundeMitFairnessConstraint(...)` | Ausnahme-Team-Fairness mit gestufter Schwelle (nur Triplette aktiv) |
| `SpielrundeDelegate` | `gespieltenRundenEinlesen()` | pflegt `anzMalKleinesTeam` (`size < defaultTeamGroesse`) |
| `Team` | `addSpielerWennNichtVorhanden()` | warImTeamMit-Einträge beim Generieren |
| `Spieler` | `warImTeamMit(other)` | Hard-Constraint-Abfrage (+ SetzPos) |
| `Spieler` | `warImSpielMit(other)` | Soft-Constraint-Abfrage |
| `Spieler` | `incAnzMalKleinesTeam()` / `getAnzMalKleinesTeam()` | Ausnahme-Team-Zähler (greift nur bei kleineren Teams) |

---

## 6. Nebenwirkung: Gegner-Qualität leidet bei effMaxSpieltage-Reset

Wenn der `effMaxSpieltage`-Reset auf 0 ausgelöst wird (Knotenlimit), lädt
`gespieltenRundenEinlesenMitLimit` nur den laufenden Spieltag. Dabei wird
`addGegner` **nur für die bereits gespielten Runden dieses Spieltags**
wiederhergestellt — die Gegner-Geschichte früherer Spieltage ist für den
Optimizer in `optimiereGegnerPaarung` unsichtbar.

**Effekt**: Paare, die in ST1–3 bereits mehrfach Gegner waren, erscheinen
dem Optimizer als "frisch". Der Score berechnet sich nur aus den laufenden
Spieltag-Runden → der Optimizer vermeidet sie weniger stark.

**Historisch messbare Folge** (analysiert an `super-test-alk.ods`, vor den
späteren Gegner-Optimierungen):

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

**Aktueller Stand**: Multi-Shuffle, Best-of-Layouts und Best-of-Shuffles
reduzieren effMaxSpieltage-Resets und Gegner-Wiederholungen deutlich.
Dadurch bleibt die cross-ST-Gegnergeschichte häufiger erhalten und der
Optimizer arbeitet öfter mit vollständiger Information.

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
