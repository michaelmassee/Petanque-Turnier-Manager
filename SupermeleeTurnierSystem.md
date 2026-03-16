# Turniersystem: Supermêlée im Pétanque

## Grundkonzept
Das **Supermêlée** (aus dem Französischen für "großes Durcheinander" oder "Mischmasch") ist eine beliebte Turnierform im Pétanque (Boule). Der entscheidende Unterschied zu klassischen Turnieren ist, dass sich die Spieler nicht als festes Team anmelden, sondern **einzeln**.

Die Teams werden für *jede einzelne Spielrunde* komplett neu zusammengelost.

---

## Ablauf eines Turniers

1. **Anmeldung:** Jeder Spieler meldet sich individuell bei der Turnierleitung an.
2. **Flexibler Ein- und Ausstieg:** Da jede Runde komplett unabhängig von der vorherigen gezogen wird, bietet das System eine enorme zeitliche Flexibilität am Turniertag:
    * **Späterer Einstieg:** Spieler, die sich verspäten, können problemlos ab der 2. oder 3. Runde in das Turnier einsteigen. Sie werden dann einfach für die kommende Auslosung in den Spielerpool aufgenommen.
    * **Vorzeitiger Ausstieg:** Spieler können das Turnier nach jeder beendeten Runde vorzeitig verlassen. Sie melden sich bei der Turnierleitung ab und werden für die nächste Auslosung aus dem Pool entfernt.
    * **Auswirkung auf die Wertung (Fehlrunden):** Verpasst ein Spieler eine oder mehrere Runden an diesem Spieltag (durch späten Einstieg oder frühen Ausstieg), werden diese nicht gespielten Runden in der Wertung automatisch als **Niederlage mit 0:13 Punkten** gewertet.
3. **Auslosung & Teamgrößen:** Vor Beginn jeder Runde zieht die Turnierleitung (oder eine Software) zufällig die Teams aus dem *aktuell anwesenden* Spielerpool zusammen. Dabei gelten folgende Besonderheiten:
    * **Keine Freilose:** Es wird grundsätzlich ohne Freilose gespielt. Jeder anwesende Spieler nimmt aktiv an jeder Runde teil.
    * **Auffüllen der Teams:** Damit die Spielerzahl in jeder Runde exakt aufgeht, wird je nach Hauptmodus flexibel aufgefüllt (Doublette wird mit Triplette aufgefüllt und umgekehrt). Die genaue Berechnung dafür ist im Abschnitt "Berechnung der Team-Aufteilung" dokumentiert.
    * *Hinweis zu gemischten Spielen:* Es kann zu Partien kommen, in denen ein Doublette gegen ein Triplette (3 gegen 2) spielt. In diesem Fall haben die Spieler des Doublettes in der Regel jeweils 3 Kugeln, um den Nachteil von insgesamt 6 gegen 6 Kugeln auszugleichen.
4. **Die Runde (Punkte vs. Zeitlimit):** Die neu gebildeten Teams spielen ihre Partie. Dabei gibt es zwei Varianten, wobei in beiden Fällen **zwingend ein Sieger** ermittelt werden muss (Unentschieden gibt es nicht):
    * **Spiel bis 13 Punkte:** Das Spiel endet regulär, sobald ein Team 13 Punkte erreicht hat.
    * **Spiel mit Zeitlimit:** Wird auf Zeit gespielt, muss nicht zwingend bis 13 gespielt werden. Nach Ablauf der vorgegebenen Zeit (zzgl. der eventuell gerade laufenden Aufnahme) gewinnt das Team, das in Führung liegt (z. B. mit 9:6).
    * *Gleichstand bei Zeitlimit:* Steht es nach Ablauf der Zeit unentschieden (z. B. 8:8), muss zwingend eine weitere, entscheidende Aufnahme (Zusatzaufnahme) gespielt werden, um einen Sieger zu ermitteln.
5. **Wiederholung:** Nach der Runde werden die Ergebnisse notiert. Für die nächste Runde werden **alle** Teams wieder komplett neu zusammengewürfelt. Niemand spielt in der Regel zweimal mit demselben Partner.

---

## Berechnung der Team-Aufteilung (Algorithmus)

Damit alle Spieler ohne Freilos spielen können, muss die aktuell anwesende Gesamtanzahl der Spieler ($N$) in Partien zu 4 Spielern (Doublette vs. Doublette), 5 Spielern (Triplette vs. Doublette) oder 6 Spielern (Triplette vs. Triplette) aufgeteilt werden.

Die Logik richtet sich nach dem Restwert der Division (Modulo) der Gesamtspielerzahl.

### Modus 1: Hauptmodus Doublette (Auffüllen mit Triplette)
Ziel ist es, so viele 4er-Partien wie möglich zu bilden. Man teilt die Spielerzahl $N$ durch 4. Der Rest bestimmt die abweichenden Partien:

* **Rest 0:** Perfekt teilbar. **Alle** spielen Doublette (4 Spieler pro Partie).
* **Rest 1:** **1 Partie** wird gemischt gespielt (Triplette vs. Doublette = 5 Spieler). Der Rest spielt Doublette.
* **Rest 2:** **1 Partie** wird als Triplette gespielt (Triplette vs. Triplette = 6 Spieler). Der Rest spielt Doublette.
* **Rest 3:** **1 Partie** wird als Triplette gespielt (6 Spieler) **UND** **1 Partie** wird gemischt gespielt (5 Spieler). Der Rest spielt Doublette. *(Zusammen 11 Spieler, was die überschüssigen 3 Spieler absorbiert: $2 \times 4 + 3 = 11$)*.

### Modus 2: Hauptmodus Triplette (Auffüllen mit Doublette)
Ziel ist es, so viele 6er-Partien wie möglich zu bilden. Man teilt die Spielerzahl $N$ durch 6. Der Rest bestimmt die abweichenden Partien:

* **Rest 0:** Perfekt teilbar. **Alle** spielen Triplette (6 Spieler pro Partie).
* **Rest 5:** **1 Partie** wird gemischt gespielt (Triplette vs. Doublette = 5 Spieler). Der Rest spielt Triplette.
* **Rest 4:** **1 Partie** wird als Doublette gespielt (Doublette vs. Doublette = 4 Spieler). Der Rest spielt Triplette.
* **Rest 3:** **1 Partie** wird als Doublette gespielt (4 Spieler) **UND** **1 Partie** wird gemischt gespielt (5 Spieler). Der Rest spielt Triplette. *(Zusammen 9 Spieler, was $6 + 3$ entspricht)*.
* **Rest 2:** **2 Partien** werden als Doublette gespielt (4 + 4 = 8 Spieler). Der Rest spielt Triplette. *(8 Spieler entspricht $6 + 2$)*.
* **Rest 1:** **2 Partien** werden als Doublette gespielt (4 + 4 = 8 Spieler) **UND** **1 Partie** wird gemischt gespielt (5 Spieler). Der Rest spielt Triplette. *(Zusammen 13 Spieler, was $2 \times 6 + 1$ entspricht)*.

---

## Wertung und Platzierung

Da es keine festen Teams gibt, wird eine **Einzelwertung** geführt. Jeder Spieler sammelt individuell Punkte aus seinen Partien. Die Rangliste wird exakt nach der folgenden Reihenfolge ermittelt:

* **Sonderregel Fehlrunden:** Jede am Spieltag nicht absolvierte Runde (z. B. durch späteren Einstieg oder vorzeitigen Ausstieg) wird für den betroffenen Spieler automatisch als **Niederlage mit 0:13 Punkten** (-13 Punktdifferenz) in die Wertung aufgenommen.

1. **Spiele + (Gewonnene Spiele):** Die absolute Anzahl der Siege ist das primäre Sortierkriterium. Wer die meisten Partien gewonnen hat, steht oben.
2. **Spiele Δ (Spieldifferenz):** Herrscht bei den Siegen Gleichstand, entscheidet die Differenz aus gewonnenen und verlorenen Partien.
3. **Punkte Δ (Punktdifferenz):** Bei weiterem Gleichstand greift die Differenz aus selbst erzielten Punkten und gegnerischen Punkten über alle gewerteten Runden. *(Beispiele: Ein regulärer 13:5 Sieg bringt eine Punktdifferenz von +8; ein knapper 9:8 Sieg nach Zeitablauf bringt +1; eine Fehlrunde bringt -13).*
4. **Punkte + (Erzielte Punkte):** Ist auch die Punktdifferenz identisch, entscheiden am Ende die insgesamt selbst erzielten positiven Punkte in allen Partien.

---

## Vorteile des Supermêlée

* **Hoher sozialer Faktor:** Man lernt sehr viele verschiedene Spieler kennen, da man ständig neue Mitspieler und Gegner hat.
* **Anfängerfreundlich:** Da starke und schwache Spieler zufällig gemischt werden, haben auch Anfänger die Chance, durch gute Partner Turniersiege einzufahren.
* **Maximale Flexibilität:** Man kann spontan alleine erscheinen, später einsteigen oder früher gehen – das Turnier läuft reibungslos weiter.
* **Planbare Dauer:** Durch die Möglichkeit, mit Zeitlimit zu spielen, ist das Ende des Turniers organisatorisch sehr gut vorhersehbar.
* **Maximale Spielzeit:** Da es keine Freilose gibt, steht niemand unbeteiligt am Rand.

## Nachteile

* **Glücksfaktor:** Die Platzierung hängt stark vom Losglück ab (Wer bekommt die stärksten Mitspieler?).
* **Kein Einspielen möglich:** Da man sich nicht an seinen Partner gewöhnen kann, sind komplexe taktische Absprachen oft schwieriger als in eingespielten Teams.

---

## Öffentliche Calc-Formeln (PTM.SUPERMELEE.*)

Der Turnier-Manager stellt alle Berechnungslogiken des `SuperMeleeTeamRechner` direkt als Calc-Formeln bereit. Sie können in jeder Zelle eines LibreOffice Calc-Dokuments verwendet werden und erleichtern z. B. eigene Dashboards oder Makros.

Alle Formeln erwarten genau einen Parameter:

| Parameter | Typ | Beschreibung |
|-----------|-----|--------------|
| `anzSpieler` | Ganzzahl | Anzahl der aktuell aktiven Spieler |

Für ungültige Eingaben (`anzSpieler < 1` oder `anzSpieler = 7`) geben die Formeln `0` zurück.

---

### Triplette-Modus (`TRIPL_*`)

Berechnung auf Basis des **Hauptmodus Triplette** (Auffüllen mit Doublette).

| Formel | Rückgabe | Beschreibung |
|--------|----------|--------------|
| `=PTM.SUPERMELEE.TRIPL_ANZ_TRIPLETTE(anzSpieler)` | Ganzzahl | Anzahl der Triplette-Teams |
| `=PTM.SUPERMELEE.TRIPL_ANZ_DOUBLETTE(anzSpieler)` | Ganzzahl | Anzahl der Doublette-Teams |
| `=PTM.SUPERMELEE.TRIPL_ANZ_PAARUNGEN(anzSpieler)` | Ganzzahl | Gesamtanzahl der Paarungen (Spiele) |
| `=PTM.SUPERMELEE.TRIPL_ANZ_BAHNEN(anzSpieler)` | Ganzzahl | Anzahl der benötigten Bahnen (= Paarungen / 2) |
| `=PTM.SUPERMELEE.TRIPL_NUR_DOUBLETTE(anzSpieler)` | 0 oder 1 | `1` wenn ausschließlich Doublette-Teams möglich sind |

**Beispiel für 15 Spieler im Triplette-Modus:**
```
=PTM.SUPERMELEE.TRIPL_ANZ_TRIPLETTE(15)  →  3
=PTM.SUPERMELEE.TRIPL_ANZ_DOUBLETTE(15)  →  3
=PTM.SUPERMELEE.TRIPL_ANZ_PAARUNGEN(15)  →  6  (= 3+3 Paarungen, also 3 Spiele)
=PTM.SUPERMELEE.TRIPL_ANZ_BAHNEN(15)     →  3
=PTM.SUPERMELEE.TRIPL_NUR_DOUBLETTE(15)  →  0
```

---

### Doublette-Modus (`DOUBL_*`)

Berechnung auf Basis des **Hauptmodus Doublette** (Auffüllen mit Triplette).

| Formel | Rückgabe | Beschreibung |
|--------|----------|--------------|
| `=PTM.SUPERMELEE.DOUBL_ANZ_DOUBLETTE(anzSpieler)` | Ganzzahl | Anzahl der Doublette-Teams |
| `=PTM.SUPERMELEE.DOUBL_ANZ_TRIPLETTE(anzSpieler)` | Ganzzahl | Anzahl der Triplette-Teams |
| `=PTM.SUPERMELEE.DOUBL_ANZ_PAARUNGEN(anzSpieler)` | Ganzzahl | Gesamtanzahl der Paarungen (Spiele) |
| `=PTM.SUPERMELEE.DOUBL_ANZ_BAHNEN(anzSpieler)` | Ganzzahl | Anzahl der benötigten Bahnen (= Paarungen / 2) |
| `=PTM.SUPERMELEE.DOUBL_NUR_TRIPLETTE(anzSpieler)` | 0 oder 1 | `1` wenn ausschließlich Triplette-Teams möglich sind |

**Beispiel für 12 Spieler im Doublette-Modus:**
```
=PTM.SUPERMELEE.DOUBL_ANZ_DOUBLETTE(12)  →  6
=PTM.SUPERMELEE.DOUBL_ANZ_TRIPLETTE(12)  →  0
=PTM.SUPERMELEE.DOUBL_ANZ_PAARUNGEN(12)  →  6
=PTM.SUPERMELEE.DOUBL_ANZ_BAHNEN(12)     →  3
=PTM.SUPERMELEE.DOUBL_NUR_TRIPLETTE(12)  →  1
```

---

### Modusunabhängige Formeln

Diese Formeln liefern unabhängig vom gewählten Modus immer dasselbe Ergebnis, da sie nur von der Spieleranzahl abhängen.

| Formel | Rückgabe | Beschreibung |
|--------|----------|--------------|
| `=PTM.SUPERMELEE.VALIDE_ANZ_SPIELER(anzSpieler)` | 0 oder 1 | `1` wenn die Spieleranzahl gültig ist (nicht 7), sonst `0` |
| `=PTM.SUPERMELEE.ANZ_TRIPLETTE_WENN_NUR_TRIPLETTE(anzSpieler)` | Ganzzahl | Anzahl der Triplette-Teams wenn `anzSpieler % 6 == 0`, sonst `0` |
| `=PTM.SUPERMELEE.ANZ_DOUBLETTE_WENN_NUR_DOUBLETTE(anzSpieler)` | Ganzzahl | Anzahl der Doublette-Teams wenn `anzSpieler % 4 == 0`, sonst `0` |

**Beispiele:**
```
=PTM.SUPERMELEE.VALIDE_ANZ_SPIELER(7)                        →  0  (7 Spieler sind ungültig)
=PTM.SUPERMELEE.VALIDE_ANZ_SPIELER(12)                       →  1
=PTM.SUPERMELEE.ANZ_TRIPLETTE_WENN_NUR_TRIPLETTE(18)         →  6  (18 / 3 = 6)
=PTM.SUPERMELEE.ANZ_TRIPLETTE_WENN_NUR_TRIPLETTE(10)         →  0  (gemischte Aufteilung nötig)
=PTM.SUPERMELEE.ANZ_DOUBLETTE_WENN_NUR_DOUBLETTE(8)          →  4  (8 / 2 = 4)
=PTM.SUPERMELEE.ANZ_DOUBLETTE_WENN_NUR_DOUBLETTE(9)          →  0  (gemischte Aufteilung nötig)
```

---

### Übersicht aller PTM.SUPERMELEE.*-Formeln

| Formel | Modus |
|--------|-------|
| `PTM.SUPERMELEE.TRIPL_ANZ_TRIPLETTE` | Triplette |
| `PTM.SUPERMELEE.TRIPL_ANZ_DOUBLETTE` | Triplette |
| `PTM.SUPERMELEE.TRIPL_ANZ_PAARUNGEN` | Triplette |
| `PTM.SUPERMELEE.TRIPL_ANZ_BAHNEN` | Triplette |
| `PTM.SUPERMELEE.TRIPL_NUR_DOUBLETTE` | Triplette |
| `PTM.SUPERMELEE.DOUBL_ANZ_DOUBLETTE` | Doublette |
| `PTM.SUPERMELEE.DOUBL_ANZ_TRIPLETTE` | Doublette |
| `PTM.SUPERMELEE.DOUBL_ANZ_PAARUNGEN` | Doublette |
| `PTM.SUPERMELEE.DOUBL_ANZ_BAHNEN` | Doublette |
| `PTM.SUPERMELEE.DOUBL_NUR_TRIPLETTE` | Doublette |
| `PTM.SUPERMELEE.VALIDE_ANZ_SPIELER` | modusunabhängig |
| `PTM.SUPERMELEE.ANZ_TRIPLETTE_WENN_NUR_TRIPLETTE` | modusunabhängig |
| `PTM.SUPERMELEE.ANZ_DOUBLETTE_WENN_NUR_DOUBLETTE` | modusunabhängig |