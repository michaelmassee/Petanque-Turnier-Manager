# Das Schweizer Turniersystem im Pétanque

## Überblick

Im Pétanque wird bei Turnieren mit vielen Mannschaften (oft im Doublette oder Triplette) meist das Schweizer System gespielt, wenn nicht genug Zeit für ein "Jeder-gegen-Jeden" bleibt und man ein direktes Ausscheiden (K.o.-System) vermeiden möchte. Der große Vorteil: Alle Teams spielen in jeder Runde mit.

Ab der zweiten Runde spielt man idealerweise immer gegen Gegner, die in etwa gleich erfolgreich sind (z. B. spielen Sieger gegen Sieger). Um am Ende bei Punktgleichheit eine absolut faire Platzierung zu ermitteln, nutzt man die Buchholz- und Feinbuchholz-Wertung. Diese Werte gleichen aus, wenn jemand das Pech hatte, gegen sehr starke Gegner gelost zu werden.

**Mindestanzahl:** Das Schweizer System erfordert mindestens **6 Teams**.

---

## Die Auswertungskriterien (Rangfolge)

| Rang | Kriterium | Beschreibung |
|------|-----------|--------------|
| 1 | **Anzahl der Siege** | Hauptkriterium |
| 2 | **Buchholz (BHZ)** | Summe der Siege aller Gegner |
| 3 | **Feinbuchholz (FBHZ)** | Summe der BHZ-Werte aller Gegner |
| 4 | **Punktedifferenz** | Erzielte minus kassierte Punkte (über alle Spiele) |
| 5 | **Direktvergleich** | Ergebnis aus dem direkten Aufeinandertreffen |
| 6 | **Los** | Letztes Mittel bei vollständigem Gleichstand |

> **Hinweis:** Manchmal tauscht die Turnierleitung die Kriterien 2 und 4, sodass die Punktedifferenz vor der Gegnerstärke zählt. Bei professionellen Turnieren wird der Buchholz-Wertung aber meist der Vorzug gegeben, da es schwerer ist, gegen einen starken Gegner knapp zu gewinnen, als einen schwachen Gegner hoch (z. B. 13:0) zu schlagen.

### Variante: Ohne Buchholz (`OHNE_BUCHHOLZ`)

Bei kleineren oder informellen Pétanque-Turnieren wird die Buchholz-Wertung häufig weggelassen. In diesem Modus gilt:

| Rang | Kriterium | Beschreibung |
|------|-----------|--------------|
| 1 | **Anzahl der Siege** | Hauptkriterium |
| 2 | **Punktedifferenz** | Erzielte minus kassierte Punkte |
| 3 | **Punkte+** | Summe aller erzielten Punkte |

In der Konfiguration (Sidebar oder Startdialog) kann zwischen den Modi **Mit Buchholz (Standard)** und **Ohne Buchholz** gewählt werden. Bei „Ohne Buchholz" werden die BHZ- und FBHZ-Spalten in der Rangliste ausgeblendet.

---

## Die Paarungsregeln

### Runde 1 – Auslosung mit Setzpositionen

Die erste Runde wird zufällig ausgelost. Dabei können vermeintlich stärkere Teams eine **Setzposition** erhalten. Teams mit der gleichen Setzposition werden nicht gegeneinander ausgelost – so vermeidet man, dass die Favoriten bereits in Runde 1 aufeinandertreffen.

### Ab Runde 2 – Paarung nach Spielstärke

Ab der zweiten Runde werden die Teams nach ihrem bisherigen Abschneiden gepaart: Sieger spielen gegen Sieger, Verlierer gegen Verlierer. Der Algorithmus stellt dabei sicher, dass zwei Teams nie zweimal gegeneinander spielen. Ist kein passender Gegner verfügbar, wird getauscht.

#### Gruppenbildung: Nur Siege zählen

Die Paarungsgruppen (Score Groups) werden **ausschließlich anhand der Anzahl der Siege** gebildet. Alle Teams mit gleich vielen Siegen bilden eine Gruppe.

> **Wichtig:** Buchholz (BHZ) und Feinbuchholz (FBHZ) sind **reine Ranglisten-Kriterien (Tie-Breaks)** und haben **keinen direkten Einfluss auf die Gruppenbildung**. Sie bestimmen nicht, in welche Gruppe ein Team fällt.

#### Indirekter Einfluss von Buchholz auf die Paarung

BHZ und FBHZ wirken **indirekt** auf die Paarung: Vor der Gruppenbildung werden die Teams innerhalb ihrer Siege-Gruppe nach den Auswertungskriterien sortiert (Siege → BHZ → FBHZ → Punktedifferenz). Diese Sortierung legt fest, welches Team in der Gruppe oben steht – und damit gegen wen gepaart wird. BHZ bestimmt also den **Paarungspartner innerhalb der Gruppe**, nicht die Gruppenzugehörigkeit selbst.

#### Carry-Over bei ungerader Gruppengröße

Hat eine Gruppe eine ungerade Anzahl von Teams, wird das schwächste Team dieser Gruppe (das letzte in der sortierten Reihenfolge) in die nächste Gruppe mit weniger Siegen "herabgesetzt" (Float/Carry-Over). So kann es gegen ein Team mit weniger Siegen spielen – dies ist besser als ein Freilos zu vergeben, solange noch Teams ohne Gegner existieren.

### Freilos (bei ungerader Teamzahl)

Gibt es eine ungerade Anzahl von Teams, erhält in jeder Runde genau ein Team ein **Freilos** (ein Sieg ohne Spiel). Das Freilos wird dem in der aktuellen Rangliste am schlechtesten platzierten Team zugeteilt, das noch kein Freilos erhalten hat. Teams mit Setzposition sollen möglichst kein Freilos in der ersten Runde erhalten.

---

## Buchholz (BHZ)

Die Buchholz-Wertung misst die Spielstärke der Teams, gegen die man im Turnierverlauf angetreten ist.

**Berechnung:** Am Ende des Turniers werden die Siege aller Gegner zusammengezählt. Das Ergebnis sind die Buchholz-Punkte.

**Bedeutung:** Ein hoher Buchholz-Wert bedeutet, dass man ein schweres Turnier hatte. Ein Team mit 3 Siegen und hohen Buchholz-Punkten wird höher platziert als ein Team mit 3 Siegen, das gegen schwächere Gegner antreten durfte.

---

## Feinbuchholz (FBHZ)

Wenn Teams nicht nur gleich viele Siege, sondern auch gleich viele Buchholz-Punkte haben, bewertet das Feinbuchholz die "Stärke der Gegner der Gegner".

**Berechnung:** Die Buchholz-Punkte aller Gegner werden addiert.

**Bedeutung:** Das Feinbuchholz gleicht noch feinere Nuancen in der Auslosung aus und verhindert, dass das Turnier durch Direktvergleich oder Los entschieden werden muss.

---

## Punktedifferenz

Beim Pétanque gibt es als viertes Kriterium die Punktedifferenz (erzielte Punkte minus kassierte Punkte, aufsummiert über alle Spiele). Ein 13:8-Sieg ergibt eine Differenz von +5.

---

## Direktvergleich

Sind nach Siegen, BHZ, FBHZ und Punktedifferenz noch immer Teams punktgleich, entscheidet der direkte Vergleich. Dabei werden alle gemeinsamen Spiele der betroffenen Teams herangezogen:
1. Wer hat im direkten Vergleich mehr Spiele gewonnen?
2. Bei Gleichstand: Wer hat die bessere Punktedifferenz im direkten Vergleich?

---

## Rechenbeispiel (6 Teams, 3 Runden)

*Warum 6 Teams?* Wenn 4 Teams 3 Runden spielen, hat am Ende jeder gegen jeden gespielt. Dann hätten alle exakt dieselben Gegner gehabt und die Buchholz-Punkte wären für alle gleich. Bei 6 Teams im Schweizer System sieht man die Unterschiede besser.

### Der Turnierverlauf

**Runde 1 (Zufällige Auslosung):**
- **Team A** gewinnt gegen Team B
- **Team C** gewinnt gegen Team D
- **Team E** gewinnt gegen Team F

**Runde 2 (Sieger gegen Sieger, Verlierer gegen Verlierer):**
- **Team A** gewinnt gegen Team C
- **Team E** gewinnt gegen Team B
- **Team D** gewinnt gegen Team F

**Runde 3 (Erneut nach Spielstärke gepaart):**
- **Team A** gewinnt gegen Team E *(Beide hatten vorher 2 Siege)*
- **Team C** gewinnt gegen Team F
- **Team B** gewinnt gegen Team D

---

### Abschlusstabelle (nur Siege)

| Platz | Team | Siege |
|-------|------|-------|
| 1. | Team A | 3 |
| 2./3. | Team C | 2 |
| 2./3. | Team E | 2 |
| 4./5. | Team B | 1 |
| 4./5. | Team D | 1 |
| 6. | Team F | 0 |

Team C und Team E haben beide 2 Siege → Buchholz entscheidet.

---

### Schritt 1: Buchholz (BHZ) berechnen

Für die Buchholz-Punkte werden die Siege aller Gegner am Turnierends addiert.

**Buchholz für Team C:**
Team C spielte gegen D (1 Sieg), A (3 Siege) und F (0 Siege).
*Rechnung:* 1 + 3 + 0 = **4 BHZ**

**Buchholz für Team E:**
Team E spielte gegen F (0 Siege), B (1 Sieg) und A (3 Siege).
*Rechnung:* 0 + 1 + 3 = **4 BHZ**

Beide Teams haben exakt gleich viele Buchholz-Punkte → Feinbuchholz entscheidet.

---

### Schritt 2: Feinbuchholz (FBHZ) berechnen

Zunächst die BHZ-Werte aller Teams bestimmen:

| Team | Gegner | BHZ-Rechnung | BHZ |
|------|--------|--------------|-----|
| Team A | B, C, E | 1 + 2 + 2 | 5 |
| Team B | A, E, D | 3 + 2 + 1 | 6 |
| Team C | D, A, F | 1 + 3 + 0 | 4 |
| Team D | C, F, B | 2 + 0 + 1 | 3 |
| Team E | F, B, A | 0 + 1 + 3 | 4 |
| Team F | E, D, C | 2 + 1 + 2 | 5 |

**Feinbuchholz für Team C:**
Team C spielte gegen D (3 BHZ), A (5 BHZ) und F (5 BHZ).
*Rechnung:* 3 + 5 + 5 = **13 FBHZ**

**Feinbuchholz für Team E:**
Team E spielte gegen F (5 BHZ), B (6 BHZ) und A (5 BHZ).
*Rechnung:* 5 + 6 + 5 = **16 FBHZ**

---

### Das finale Endergebnis

Team E hatte die stärkeren Gegner der Gegner (FBHZ = 16 > 13) und zieht an Team C vorbei.

| Platz | Team | Siege | BHZ | FBHZ |
|-------|------|-------|-----|------|
| **1.** | **Team A** | 3 | 5 | 14 |
| **2.** | **Team E** | 2 | 4 | **16** |
| **3.** | **Team C** | 2 | 4 | **13** |



Die Punktedifferenz funktioniert im Grunde exakt wie das Torverhältnis beim Fußball. Es ist die Gesamtsumme deiner selbst erzielten Punkte abzüglich der kassierten Gegenpunkte über alle Spiele eines Turniers hinweg.Ein kurzes RechenbeispielNehmen wir an, ein Team absolviert drei Runden:SpielErgebnisErzielte Punkte (+)Kassierte Punkte (-)Differenz pro SpielSpiel 1Sieg (13:8)138+5Spiel 2Niederlage (10:13)1013-3Spiel 3Sieg (13:5)135+8Gesamt3626+10Die finale Punktedifferenz für dieses Team beträgt am Ende also +10. Du kannst dafür entweder alle Pluspunkte und alle Minuspunkte zusammenzählen und voneinander abziehen (36 - 26 = 10) oder einfach die Differenzen der Einzelspiele addieren (5 - 3 + 8 = 10). Das Ergebnis ist dasselbe.