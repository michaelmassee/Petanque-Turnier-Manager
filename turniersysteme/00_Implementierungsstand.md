# Implementierungsstand: Fachliche Vorgaben vs. Code

Stand: 2026-03-30

---

## 1. Übersicht: Welche Systeme sind implementiert?

| System | Dokument | Java-Package | Menü | Status |
|---|---|---|---|---|
| SuperMelee | `07_Supermelee.md` | `supermelee/` | `Addons_A1_Supermelee.xcu` | ✅ Implementiert |
| Liga | — | `liga/` | `Addons_A3_Liga.xcu` | ✅ Implementiert |
| Jeder-gegen-Jeden | — | `jedergegenjeden/` | `Addons_A4_JederGJeden.xcu` | ✅ Implementiert |
| Schweizer System | `03_Schweizer.md` | `schweizer/` | `Addons_A5_Schweizer.xcu` | ✅ Implementiert |
| KO-System | `05_KO.md` | `ko/` + `forme/korunde/` | `Addons_A6_KO.xcu` | ✅ Implementiert |
| Maastrichter | `04_Maastrichter.md` | `maastrichter/` | `Addons_A7_Maastrichter.xcu` | ✅ Implementiert |
| Poule-AB | `02_Poule-AB.md` | — | — | ❌ Nicht implementiert |
| Kaskaden-KO | `06_Kaskaden-KO.md` | — | — | ❌ Nicht implementiert |
| Formule-X | `08_Formule_X.md` | — | — | ❌ Nicht implementiert |
| Trip-Tête | `09_TripTete.md` | — | — | ❌ Nicht implementiert |
| Kölner Sextet | `10_KoelnerSextet.md` | — | — | ❌ Nicht implementiert |
| Dänisches System | `11_DaenischesSystem.md` | — | — | ❌ Nicht implementiert |
| Monrad-System | `12_MonradSystem.md` | — | — | ❌ Nicht implementiert |
| Formule 4 | `13_Formule4.md` | — | — | ❌ Nicht implementiert |
| Tête-Series | `14_TeteSeries.md` | — | — | ❌ Nicht implementiert |
| Crazy-Mêlée | `15_CrazyMelee.md` | — | — | ❌ Nicht implementiert |
| Arena-Pétanque | `16_ArenaPetanque.md` | — | — | ❌ Nicht implementiert |

**Ergebnis: 6 von 17 Turniersystemen sind implementiert.**

---

## 2. Abweichungen in implementierten Systemen

### 2.1 Schweizer System

#### Freilos-Vergabe — ✅ Korrekt
| | Beschreibung |
|---|---|
| **Spezifikation** | Das schwächste Team ohne bisherigen Freilos erhält den Freilos |
| **Implementierung** | `SchweizerSystem.weitereRunde()`: Rückwärts-Iteration durch die nach Rangliste sortierte Teamliste mit Filter `!team.isHatteFreilos()` |
| **Bewertung** | Entspricht der Spezifikation |

#### Buchholz / Feinbuchholz-Wertung — ✅ Korrekt
| | Beschreibung |
|---|---|
| **Spezifikation** | Siege → BHZ (Summe Siege aller Gegner) → FBHZ (Summe BHZ aller Gegner) → Punktedifferenz |
| **Implementierung** | `SchweizerSystem.sortiereNachAuswertungskriterien()`: identische 4-stufige Sortierung |
| **Bewertung** | Entspricht der Spezifikation |

#### Paarungsalgorithmus — 🟡 Teilabweichung
| | Beschreibung |
|---|---|
| **Spezifikation** | Vollständiges Backtracking zur Rematch-Vermeidung innerhalb von Score-Gruppen mit Floating bei ungeraden Gruppen |
| **Implementierung** | Greedy-Paarung innerhalb Scoregruppen + 2 Tausch-Heuristiken bei Impasse (`SchweizerSystem.java`) |
| **Abweichung** | Kein vollständiges Backtracking — nur Tausch zweier bereits geparter Teams. In Grenzfällen (viele Rematches über mehrere Runden, viele Floats) kann die Lösung suboptimal sein oder scheitern. |

---

### 2.2 Maastrichter System

#### Finalgruppen-Einteilung — 🔴 Kritische Abweichung
| | Beschreibung |
|---|---|
| **Spezifikation** | Teams werden nach **exakter Sieganzahl** in Finalgruppen eingeteilt: 3 Siege → A-Turnier, 2 Siege → B-Turnier, 1 Sieg → C-Turnier, 0 Siege → D-Turnier. Die Score-Pyramide bildet sich nach 3 Schweizer Runden selbst (typisch bei 24 Teams: A=4, B=8, C=8, D=4). |
| **Implementierung** | `MaastrichterFinalrundeSheet.java`: Teams werden nach Schweizer-Kriterien (Siege → BHZ → FBHZ → Differenz) gesamtsortiert und dann gleichmäßig via `GruppenAufteilungRechner` aufgeteilt (max. Gruppengröße als Parameter). |
| **Abweichung** | Die Einteilung erfolgt quantitativ statt qualitativ. Beispiel bei 24 Teams mit max. Gruppengröße=8: → 3 Gruppen à 8. Laut Spezifikation: 4 Gruppen à 4 / 8 / 8 / 4 (nach Siegen). Teams mit unterschiedlicher Sieganzahl können in derselben Finalgruppe landen. |

#### Cadrage je Finalfeld — ⚪ Nicht vollständig geprüft
| | Beschreibung |
|---|---|
| **Spezifikation** | Jedes Finalfeld erhält einen eigenen Cadrage, wenn die Teilnehmerzahl keine Zweierpotenz ist |
| **Implementierung** | Cadrage-Logik ist in `forme/korunde/CadrageSheet.java` und `KoGruppeABSheet.java` vorhanden — ob sie korrekt je Gruppe angewendet wird, muss gesondert verifiziert werden |

---

### 2.3 KO-System

#### Setzlogik — ✅ Korrekt
| | Beschreibung |
|---|---|
| **Spezifikation** | Verschachteltes Bracket-Schema: 1 vs. 16, 8 vs. 9, 5 vs. 12, 4 vs. 13, 3 vs. 14, 6 vs. 11, 7 vs. 10, 2 vs. 15 |
| **Implementierung** | `KoTurnierbaumSheet.berechneSetzliste(n)`: rekursiver Algorithmus — für n=8 ergibt er [1,8,4,5,2,7,3,6] → Matches [1v8, 4v5, 2v7, 3v6]. Seed 1 und Seed 2 treffen sich garantiert erst im Finale. |
| **Hinweis** | `KoRundeTeamPaarungen.java` (lineares „Erster gegen Letzten") ist eine separate Hilfsklasse für Zwischenrunden in anderen Systemen (`forme/`, Maastrichter-Finale), **nicht** für den KO-Turnierbaum. |

---

### 2.4 SuperMelee

#### Partner-Vermeidungsalgorithmus — ✅ Korrekt (übertrifft Spezifikation)
| | Beschreibung |
|---|---|
| **Spezifikation** | Niemand zweimal mit gleichen Partnern |
| **Implementierung** | `SuperMeleePaarungenV2.java`: Vollständiges Backtracking mit MCV-Heuristik (Most Constrained Variable), Forward-Checking, Symmetriebrechung, Adjazenz-Matrix für O(1)-Constraint-Lookup, Knotenlimit 10 Mio. |
| **Bewertung** | Garantiert optimale Lösung; algorithmisch stärker als die Spezifikation verlangt |

#### Gegner-Vermeidung — 🟡 Teilabweichung
| | Beschreibung |
|---|---|
| **Spezifikation** | Niemand zweimal gegen dieselben Gegner |
| **Implementierung** | `optimiereGegnerPaarung()`: Minimiert Gegner-Wiederholungen nach der Teambildung als nachgelagerte Optimierung (kein hartes Constraint im Backtracking) |
| **Abweichung** | Keine Garantie — Best-Effort-Minimierung. Bei großen Feldern oder vielen Runden sind Gegner-Wiederholungen möglich. |

---

## 3. Bewertungsübersicht

| System | Aspekt | Bewertung |
|---|---|---|
| Schweizer | Freilos-Vergabe | ✅ Korrekt |
| Schweizer | BHZ / FBHZ-Wertung | ✅ Korrekt |
| Schweizer | Paarungsalgorithmus | 🟡 Tausch-Heuristik statt vollständigem Backtracking |
| Maastrichter | Finalgruppen-Einteilung | 🔴 Gleichmäßig statt nach Sieganzahl |
| Maastrichter | Cadrage je Finalfeld | ⚪ Nicht vollständig geprüft |
| KO | Setzlogik / Bracket | ✅ Korrekt (rekursives verschachteltes Bracket) |
| SuperMelee | Partner-Vermeidung | ✅ Korrekt + besser als Spec |
| SuperMelee | Gegner-Vermeidung | 🟡 Best-Effort, keine Garantie |

**Legende:** ✅ Korrekt · 🟡 Teilabweichung · 🔴 Kritische Abweichung · ⚪ Nicht geprüft
