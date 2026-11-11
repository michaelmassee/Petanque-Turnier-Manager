
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
- Eine faire Rangliste entsteht durch Tie‑Break‑Wertungen (BHZ, FBHZ, Punktedifferenz, Punkte+).

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
   - Punkte+ (Pluspunkte)
4. Paarungen von oben nach unten bilden.
5. Konflikte lösen (kein Rematch erlaubt).
6. Spiele ausführen.

---
# 3. Wertungslogik im Detail

## 3.1 Siege (Hauptkriterium)
Das wichtigste Kriterium.

## 3.2 Buchholz (BHZ)
Summe der Siege aller Gegner. Ein Freilos liefert je nach Einstellung „Freilos-Buchholz“ keinen Beitrag (NULL) oder
den Beitrag eines fiktiven Durchschnittsgegners (NEUTRAL), siehe 5.1. Ganzzahlig, Anzeige ohne Nachkommastelle.

## 3.3 Feinbuchholz (FBHZ)
Summe der BHZ‑Werte aller Gegner (bei NEUTRAL bereits um deren Freilose korrigiert) plus ggf. der eigene
Freilos-Beitrag, siehe 5.1.

## 3.4 Punktedifferenz
(+) erzielte Punkte
(-) kassierte Punkte

## 3.5 Pluspunkte
Letztes Wertungskriterium nach Punktedifferenz (Modus MIT_BUCHHOLZ) bzw. direkt nach
Punktedifferenz, wenn BHZ/FBHZ nicht genutzt werden (Modus OHNE_BUCHHOLZ).

## 3.6 Gleicher Platz
Sind zwei Teams in allen Wertungskriterien gleich, **teilen sie sich den Platz** (z. B. 1, 1, 3). Einen
Direktvergleich oder Losentscheid gibt es nicht. Die **Teamnummer ist kein Wertungskriterium**: Sie legt nur fest, in
welcher Reihenfolge Teams mit geteiltem Platz in der Liste stehen (aufsteigend), damit Rangliste und Auslosung der
nächsten Runde eindeutig sind. Gleiche Regel in PTM-Online (`sortSwiss`);
gemeinsame Testfälle: `src/test/resources/de/petanqueturniermanager/schweizer/schweizer-wertung-faelle.json`.

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
- hatten schon alle Teams ein Freilos, erhält es das schlechteste Team
- Runde 1: ein zufälliges ungesetztes Team; nur wenn alle gesetzt sind, eines aus der größten Setzgruppe
- Wertung: 13:7 oder 13:0

### Freilos und Buchholz (Einstellung „Freilos-Buchholz“, Issue #38)
Das Freilos zählt als Sieg mit den konfigurierten Freispiel-Punkten. Für die anderen Teams zählt der Freilos-Sieg in
deren BHZ normal mit. Welchen BHZ/FBHZ-Beitrag das Freilos dem Freilos-Team selbst bringt, ist einstellbar
(Konfiguration, Schweizer und Maastrichter; PTM-Online: Turnieroptionen):

| Modus | Beitrag je Freilos | Verwendung |
|---|---|---|
| **NULL** | 0 BHZ, 0 FBHZ | BPV-Regel; **Standard** (auch für neu angelegte Turniere) |
| **NEUTRAL** | BHZ `⌊(r − 1) / 2⌋`, FBHZ `eigene Siege + (r − 1) · r / 2` | Option; bevorzugt Freilos-Teams (siehe unten) |

`r` = Anzahl gewerteter Runden mit mindestens einem eingetragenen Ergebnis. Ein Freilos zählt nur, wenn seine Runde
in `r` zählt – eine ausgeloste, aber ungespielte Runde verändert BHZ/FBHZ nicht.

**Herleitung NEUTRAL (PTM-Regel):** Das Freilos wird als fiktiver Durchschnittsgegner gewertet. Er verliert in der
Freilos-Runde gegen das eigene Team und gewinnt in den übrigen `r − 1` Runden rechnerisch die Hälfte, abgerundet →
BHZ-Beitrag `⌊(r − 1) / 2⌋`. Abgerundet wird, weil es im Pétanque kein Unentschieden und damit keine halben
Buchholz-Punkte gibt (anders als im Schach). Seine eigene BHZ besteht aus den Siegen des eigenen Teams plus `r − 1` Durchschnittsgegnern mit je
`r / 2` Siegen → FBHZ-Beitrag `eigene Siege + (r − 1) · r / 2`. Dieser ist immer ganzzahlig, weil `(r − 1) · r` als
Produkt zweier aufeinanderfolgender Zahlen gerade ist.

| Gewertete Runden r | Eigene Siege | BHZ je Freilos | FBHZ je Freilos |
|---|---|---|---|
| 1 | 1 | 0 | 1 |
| 2 | 2 | 0 | 3 |
| 3 | 2 | 1 | 5 |
| 4 | 3 | 1 | 9 |
| 5 | 3 | 2 | 13 |

Nach Runde 1 hat das Freilos-Team damit BHZ 0 / FBHZ 1 – genau wie jeder andere Sieger. Mit NULL hat es FBHZ 0,
steht am Ende seiner Sieggruppe und liegt nach 5 Runden typischerweise 2–3 BHZ-Punkte hinter einem gleich starken
Team.

**Warum NULL der Standard ist (Simulation, Issue #38):** Das Freilos ist ein geschenkter Sieg – gegen einen
typischen Gegner im Schnitt rund 0,6 Siege wert. Dadurch landet das Freilos-Team in einer höheren Sieggruppe, als es
erspielt hat. NULL setzt es dort ans Ende und gleicht den geschenkten Sieg so weitgehend aus; NEUTRAL nimmt diesen
Ausgleich weg. Simulation mit der Online-Auslosung (Teams mit bekannter Spielstärke, 5 Runden; Vorteil = Plätze
besser als ein gleich starkes Team ohne Freilos, Freilos ab Runde 2 im Vergleich zum Team direkt darüber):

| Feld | NULL (Freilos R1 / ab R2) | NEUTRAL (Freilos R1 / ab R2) | BHZ des schlechtesten Teams (NRW) |
|---|---|---|---|
| 15 Teams | −0,8 / −0,5 | +0,2 / +0,1 | +2,1 / +1,4 |
| 25 Teams | ±0 / +0,4 | +1,8 / +1,3 | +4,4 / +3,6 |
| 57 Teams | +2,2 / +2,3 | +6,6 / +4,7 | +11,5 / +10,4 |

NULL kommt einer fairen Wertung am nächsten; NEUTRAL bleibt als Option für Ausrichter, die das Freilos-Team nicht
ans Ende seiner Sieggruppe setzen wollen.

Berechnung in zwei Schritten, ohne Rekursion: (1) BHZ = Siege der echten Gegner + Freilos-Beiträge; (2) FBHZ =
**korrigierte** BHZ der echten Gegner + eigene FBHZ-Freilos-Beiträge. So profitieren auch die echten Gegner des
Freilos-Teams über ihre FBHZ von dessen korrigierter BHZ. Alle Werte sind ganzzahlig und werden ohne
Nachkommastelle angezeigt. Im Ranglisten-Modus OHNE_BUCHHOLZ hat die Einstellung keine Wirkung.

Quellen und Abgrenzung:
- NULL entspricht Bayerischer Pétanque Verband, [„Schweizer System“](https://www.petanque-bayern.de/files/0010_Allgemeine%20Informationen/Verband/Dokumente/Schweizer%20System.pdf):
  „Das Team erhält einen Siegpunkt, aber keine Buchholz- oder Feinbuchholzpunkte für das Freilos.“
- NEUTRAL ist eine eigene PTM-Regel. Geprüft und nicht übernommen (Issue #35/#38): FAQ des Pétanque-Verbands NRW
  (BHZ des schlechtesten Teams) und FIDE C.07 Art. 16.4 (virtueller Gegner, schachspezifisch).

### Freilos-Spalte in der Rangliste (Issue #18)
Die Rangliste zeigt rechts eine Spalte „FL“ (Freilos, Breite wie BHZ/FBHZ) mit den Runden, in denen ein Team ein Freilos hatte („2“ bzw.
„1, 3, 6“, ohne Freilos leer). Gezählt wird jede ausgeloste, gewertete Runde – auch wenn dort noch kein Ergebnis
eingetragen ist, denn das Freilos zählt bereits als Sieg. Nicht gewertete Runden fehlen. Die Spalte ist nur
sichtbar, wenn die Einstellung „Rangliste Freilos Spalte“ aktiv ist (Default: an) **und** mindestens ein Freilos
vergeben wurde. Sie ist kein Sortierkriterium und wird bei jedem Ranglisten-Update neu geschrieben.

## 5.2 Score‑Gruppe ungerade → Float (Herunterlosen)
Ist eine Sieggruppe ungerade, wird ein Team in die nächsttiefere Gruppe heruntergelost und dort zuerst gepaart.
Welches Team, entscheidet diese Reihenfolge:
1. nicht in der letzten Runde heruntergelost
2. nicht in der vorletzten Runde heruntergelost
3. bisher möglichst selten heruntergelost
4. rangniedrigstes Team der Gruppe

Ein Team, das in dieser Runde schon aus einer höheren Gruppe heruntergelost wurde, wird nicht gleich weiter
heruntergelost (außer es ist allein in seiner Gruppe).

Ein Team gilt in Runde r als heruntergelost, wenn sein Gegner vor Runde r weniger Siege hatte. Die Historie wird
aus den Spielrunden abgeleitet.

**Freilos zählt als Herunterlosung** (Issue #38, feste Regel ohne Einstellung): Ein Freilos ist wie ein Downfloat
eine Runde ohne gleich starken Gegner. Diese Sonderrolle soll reihum gehen, deshalb wird das Freilos-Team in der
Folgerunde möglichst nicht auch noch heruntergelost (vgl. FIDE Dutch: wer eine Runde nicht spielt, gilt als
heruntergelost). Die Freilos-Runde wird wie eine Herunterlosung in der Historie vermerkt (pro Runde höchstens einmal).

Die Regel gilt unabhängig von der Freilos-Buchholz-Einstellung (5.1): Diese korrigiert die Wertung, die
Herunterlos-Regel nur die Floater-Wahl. Besonders wichtig ist sie bei NULL, weil das Freilos-Team dort mangels
Gegner (0 BHZ/FBHZ) z. B. bei 9 Teams vor Runde 2 als letzter der 5 Sieger erster Kandidat fürs Herunterlosen wäre.

Das ist ein Optimierungskriterium, kein Verbot (vgl. FIDE Dutch C.04.3, Kriterien C14/C16): Die Auslosung blockiert
nie. Die Vermeidung von Gegnerwiederholungen (Kapitel 4) hat Vorrang – erzwingt sie eine andere Paarung, kann auch
ein geschütztes Team die Gruppe verlassen.

## 5.3 Keine rematch‑freie Paarung möglich
→ Backtracking + ggf. Gegnerwiederholung minimal zulassen.

## 5.4 Setzen in Runde 1
Teams mit gleicher Setzposition (> 0) werden in Runde 1 nicht gegeneinander gelost, solange das vermeidbar ist.
Nur wenn eine Setzgruppe mehr als die Hälfte der Teams stellt, spielen so wenige ihrer Teams wie möglich
gegeneinander.

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
# 8.4a Spielrunde nicht werten (PTM)

Über **„Spielrunde werten / nicht werten"** im Turniermenü lässt sich die Schweizer-Spielrunde des gerade
aktiven Blatts aus der Wertung nehmen, z. B. eine versehentlich ausgeloste oder vor Ort abgesagte
Runde. Ein zweiter Aufruf nimmt die Runde wieder in die Wertung auf.

- Das Blatt und die Nummerierung aller Runden bleiben unverändert, der Tab wird grau, und in der Sidebar-Blätterliste erscheint der Blattname durchgestrichen.
- Im Blatt selbst ist die Runde deutlich gekennzeichnet: Die ganze Tabelle (Kopf und alle Paarungen) erscheint per **bedingter Formatierung** hellgrau mit grauer, durchgestrichener Schrift, und der Titel erhält den Zusatz „– NICHT GEWERTET“. Der Titelzusatz ist direkt geschrieben und damit auch im HTML-/Webserver-/PDF-Export sichtbar; die bedingte Formatierung gilt nur in Calc. Beim Wiederaufnehmen wird beides entfernt, die übrigen Regeln (Fehler-, Editierfarbe) bleiben unverändert.
- Die Runde zählt **nicht in der Rangliste** (Siege, Punkte, BHZ/FBHZ).
- Die Runde zählt **nicht in der Paarungshistorie**: Weitere Auslosungen dürfen ihre Paarungen wiederholen.
- Bereits ausgeloste spätere Runden bleiben unverändert.
- Wird die Runde neu ausgelost, ist sie wieder gewertet.

Technisch: blattgebundener Named Range `__PTM_NICHT_GEWERTET_<Rundenschlüssel>` (`RundenWertungHelper`); Kennzeichnung im Blatt über `NichtGewertetFormatHelper` (Regel wird allen vorhandenen Regeln vorangestellt, da bei LO das erste passende Format gewinnt).

---
# 8.4 Optionales Zeitplan-Feature: Rundenzeitplanung mit Durchgang-Aufteilung

Reichen die verfügbaren Bahnen nicht für alle Paarungen einer Runde, kann PTM die Runde
automatisch in mehrere zeitlich versetzte Durchgänge aufteilen und dafür Start-/Endzeiten je
Durchgang sowie eine pro Durchgang neu beginnende Bahn-Nummerierung berechnen. Ausführlich
dokumentiert in [`ZEITPLAN.md`](ZEITPLAN.md).

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
