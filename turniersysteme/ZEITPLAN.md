# Zeitplan-Feature: Rundenzeitplanung mit Durchgang-Aufteilung

Optionales Feature zur zeitlichen Planung von Turnierrunden. Wenn eine Runde mehr Paarungen hat
als Bahnen zur Verfügung stehen, teilt PTM die Runde automatisch in mehrere zeitlich versetzte
**Durchgänge** (auch „Heats" genannt) auf: Durchgang 1 spielt zuerst auf allen Bahnen, danach
Durchgang 2 auf denselben (wiederverwendeten) Bahnen usw. Für jeden Durchgang wird Start- und
Endzeit berechnet, Runden hängen sich per Formel an das Ende der Vorrunde, und Bahnen werden pro
Durchgang neu nummeriert.

Anwendbare Systeme: **Schweizer System**, **Maastrichter** (übernimmt die Schweizer-Logik 1:1),
**Formule X**. Siehe [Warum nur diese drei Systeme](#9-warum-nur-diese-drei-systeme).

---

## 1. Überblick

Ohne das Feature schreibt PTM alle Paarungen einer Runde untereinander, ohne Zeitbezug — die
Turnierleitung organisiert die Bahnenbelegung manuell. Mit aktiviertem Zeitplan-Feature entsteht
zusätzlich eine dritte Spalte mit Uhrzeiten, die angibt, wann welcher Durchgang beginnt und endet,
plus optische Trennlinien zwischen den Durchgängen und (optional) eine pro Durchgang neu
beginnende Bahn-Nummerierung.

Das Feature ist rein additiv: Ist es deaktiviert (Default), verhalten sich alle drei Systeme exakt
wie vorher — keine Zeit-Spalte, keine Aufteilung, keine Änderung an Paarungslogik oder Wertung.

---

## 2. Konfiguration — die sechs Optionen im Detail

Alle Optionen werden über den **Zeitplan-Dialog** (`ZeitplanDialog`, Filterung nach
`ZeitplanConfigProperty`) im jeweiligen Konfigurationssheet des Turniersystems gesetzt. Die
Property-Key-Konstanten sind in `IZeitplanPropertiesSpalte` als Single Source of Truth
zentralisiert; jedes System (`SchweizerPropertiesSpalte`, `FormuleXPropertiesSpalte`) registriert
dieselben sechs Properties mit identischen Defaults.

### 2.1 Zeitplan aktiv

| | |
|---|---|
| **Property-Key** | `Zeitplan aktiv` (`KONFIG_PROP_ZEITPLAN_AKTIV`) |
| **Typ / Default** | Boolean / `false` |
| **Bedeutung** | Hauptschalter des Features. `false` → keine Zeit-Spalte, keine Aufteilung, unverändertes Verhalten. |
| **Auswirkung bei Änderung** | Wird beim nächsten Rundenaufbau (`Neu auslosen` / `Nächste Runde`) wirksam — bereits erzeugte Runden werden nicht rückwirkend verändert. |

### 2.2 Anzahl Bahnen

| | |
|---|---|
| **Property-Key** | `Zeitplan Anzahl Bahnen` (`KONFIG_PROP_ZEITPLAN_ANZAHL_BAHNEN`) |
| **Typ / Default** | Integer / `0` |
| **Bedeutung** | Wie viele Paarungen gleichzeitig (auf physisch verschiedenen Bahnen) gespielt werden können. Bestimmt die Blockgröße der Durchgang-Aufteilung. |
| **Zusammenspiel** | `isDurchgangAufteilungWirksam()` = `isZeitplanAktiv() && getZeitplanAnzahlBahnen() > 0`. Bei `0` bleibt die Zeit-Spalte zwar sichtbar (Rundenstartzeit wird weiter berechnet), aber es findet **keine** Durchgang-Aufteilung statt — die ganze Runde gilt als ein einziger Durchgang. |
| **Beispiel** | 8 Paarungen, `Anzahl Bahnen = 3` → 3 Durchgänge (siehe [Abschnitt 3](#3-was-ist-ein-durchgang--der-aufteilungsalgorithmus)). |

### 2.3 Durchgang Zeitlimit (Minuten)

| | |
|---|---|
| **Property-Key** | `Durchgang Zeitlimit (Minuten)` (`KONFIG_PROP_ZEITPLAN_ZEITLIMIT_MINUTEN`) |
| **Typ / Default** | Integer / `15` |
| **Bedeutung** | Angenommene Spieldauer eines Durchgangs (nicht einer einzelnen Partie — alle Bahnen eines Durchgangs spielen parallel, daher ist die Durchgang-Dauer die Dauer der langsamsten Partie, geschätzt durch dieses Zeitlimit). Bestimmt den Abstand zwischen Start- und Endzeit jedes Durchgang-Blocks. |
| **Beispiel** | Durchgang startet 09:00 → endet 09:15. |

### 2.4 Durchgang Pause (Minuten)

| | |
|---|---|
| **Property-Key** | `Durchgang Pause (Minuten)` (`KONFIG_PROP_ZEITPLAN_DURCHGANG_PAUSE_MINUTEN`) |
| **Typ / Default** | Integer / `5` |
| **Bedeutung** | Pufferzeit zwischen zwei Durchgängen **innerhalb derselben Runde** (Bahnwechsel, Ergebniserfassung, kurze Pause). |
| **Beispiel** | Durchgang 1 endet 09:15 → Durchgang 2 startet 09:20 (09:15 + 5 Min). |

### 2.5 Runden Pause (Minuten)

| | |
|---|---|
| **Property-Key** | `Runden Pause (Minuten)` (`KONFIG_PROP_ZEITPLAN_RUNDEN_PAUSE_MINUTEN`) |
| **Typ / Default** | Integer / `10` |
| **Bedeutung** | Pufferzeit **zwischen zwei Runden** (üblicherweise länger als die Durchgang-Pause, da nach der letzten Partie einer Runde neue Paarungen berechnet/erfasst werden müssen). |
| **Beispiel** | Runde 1 endet 09:55 → Runde 2 startet 10:05 (09:55 + 10 Min). |

### 2.6 Turnier Startzeit

| | |
|---|---|
| **Property-Key** | `Turnier Startzeit` (`KONFIG_PROP_ZEITPLAN_TURNIER_STARTZEIT`) |
| **Typ / Default** | String im Format `HH:MM` / `"09:00"` |
| **Bedeutung** | Default-Startzeit für Runde 1. Wird beim Rundenaufbau **einmalig als literaler Wert** in die Sheet-Zelle übernommen (kein Formelbezug auf die Konfiguration) — das Sheet-Feld selbst ist danach die führende Quelle. Ändert man die zentrale Turnier-Startzeit später, verschiebt sich eine bereits erzeugte Runde 1 **nicht** rückwirkend; die Rundenstartzeit-Zelle in Runde 1 kann zudem manuell überschrieben werden (sie ist als einzige Zeit-Zelle des Features frei editierbar). |
| **Validierung** | `StringTools.isValidUhrzeitHhMm()` prüft strikt auf 24h-Format `HH:MM`. Ungültige Zwischenstände (z. B. während der Eingabe) werden im Dialogfeld **live rot markiert**, aber nicht persistiert — verhindert `#VALUE!`-Fehler in den nachgelagerten Zeitformeln. Bewusst kein modaler Focus-Listener (Freeze-/Crash-Risiko bei UNO-Event-Callbacks). |

---

## 3. Was ist ein Durchgang? — Der Aufteilungsalgorithmus

Ein **Durchgang** ist eine Gruppe von Paarungen, die zeitgleich auf den verfügbaren Bahnen
gespielt wird. Reichen die Bahnen für alle Paarungen der Runde nicht aus, entstehen mehrere
Durchgänge, die nacheinander stattfinden.

Die Aufteilung übernimmt `DurchgangAufteilungRechner.berechne(anzahlPaarungen, anzahlBahnen)`:
eine einfache Chunk-Bildung in Blöcke der Größe `anzahlBahnen`. Der letzte Block enthält den Rest,
auch wenn er kleiner ist — anders als beim verwandten `GruppenAufteilungRechner` wird ein
1-Paarung-Rest hier **nicht** in den vorherigen Block gefaltet, sondern bildet einen eigenen,
kleineren Durchgang.

**Durchgehendes Beispiel dieser Seite:** 8 Paarungen, 3 Bahnen.

```
DurchgangAufteilungRechner.berechne(8, 3) → [3, 3, 2]
```

| Durchgang | Paarungen | Bahnen belegt |
|---|---|---|
| 1 | 1–3 | 1, 2, 3 |
| 2 | 4–6 | 1, 2, 3 (wiederverwendet) |
| 3 | 7–8 | 1, 2 |

Sind alle Paarungen in einem Durchgang unterzubringen (`bloecke.size() <= 1`, z. B. 3 Paarungen
bei 3 Bahnen), findet **keine** Aufteilung statt — weder Zeit-Verkettung zwischen Durchgängen noch
Trennlinien noch Bahn-Neu-Nummerierung.

---

## 4. Zeitplan-Berechnung im Detail

Alle Zeit-Zellen stehen in der Zeit-Spalte (`getZeitSpalte()`), rechts neben Bahn-Nr/Team/Ergebnis.
Fortsetzung des 8/3-Beispiels mit Turnier-Startzeit `09:00`, Zeitlimit `15 Min`, Durchgang-Pause
`5 Min`.

### 4.1 Rundenstartzeit-Feld

Ganz oben in der Zeit-Spalte steht ein einzelnes, editierbares Feld mit der Rundenstartzeit
(`rundenStartzeitFeld()`):

- **Runde 1**: literaler Textwert aus der Turnier-Startzeit-Konfiguration (`09:00`) — kein
  Formelbezug, siehe [2.6](#26-turnier-startzeit).
- **Runde N > 1**: Formel `TEXT(<Rundenstartzeit-Formel>;"HH:MM")`, die sich auf das Ende der
  Vorrunde bezieht (siehe [4.3](#43-rundenverkettung)).

### 4.2 Durchgang-Verkettung

`durchgangInfoSpaltenSchreiben()` schreibt für jeden Durchgang-Block Start- und Endzeit:

- Der **erste** Durchgang startet bei der Rundenstartzeit.
- Jeder **weitere** Durchgang startet an der tatsächlichen Endzeit-Zelle des vorherigen Durchgangs
  plus Durchgang-Pause — nicht an einem rechnerisch rekonstruierten `Rundenstart + (n-1) ×
  (Zeitlimit + Pause)`. Das vermeidet Drift, falls Zeitlimit oder Pause während der laufenden
  Runde nachträglich geändert werden: jede Zelle hängt nur an ihrem direkten Vorgänger.
- Bei einem **einzeiligen** Block (letzter Durchgang mit nur einer Paarung) wird nur die Endzeit
  in die einzige Zeile geschrieben; der Endausdruck wird dabei direkt aus dem Start-Ausdruck
  abgeleitet statt per Zellreferenz auf die eigene Zeile — siehe
  [Zirkelbezug-Vermeidung](#71-zirkelbezug-vermeidung-bei-einzeiligen-blöcken).

Ergebnis für das Beispiel:

| Durchgang | Start | Ende | Formel-Herleitung |
|---|---|---|---|
| 1 | **09:00** | **09:15** | Start = Rundenstartzeit; Ende = Start + 15 Min |
| 2 | **09:20** | **09:35** | Start = Ende Durchgang 1 (09:15) + 5 Min Pause; Ende = Start + 15 Min |
| 3 | **09:40** | **09:55** | Start = Ende Durchgang 2 (09:35) + 5 Min Pause; Ende = Start + 15 Min (einzeiliger Block: nur Ende sichtbar) |

Die Runde dauert damit insgesamt von 09:00 bis 09:55 (55 Minuten für 8 Paarungen auf 3 Bahnen).

### 4.3 Rundenverkettung

Die Startzeit von Runde 2 hängt sich an das **tatsächliche** Ende von Runde 1 (letzte befüllte
Zeit-Zelle) plus Runden-Pause — nicht an einen fixen Offset ab der Turnier-Startzeit:

```
Runde 2 Startzeit = TIMEVALUE(Runde1_letzte_Zeit-Zelle) + Runden-Pause
```

Beispiel: Runde 1 endet um 09:55 (siehe oben), Runden-Pause = 10 Min →
**Runde 2 startet um 10:05.** Hat Runde 2 wieder 8 Paarungen bei 3 Bahnen, wiederholt sich exakt
dasselbe Muster ab 10:05: Durchgang 1 = 10:05–10:20, Durchgang 2 = 10:25–10:40, Durchgang 3 =
10:45–11:00.

War die Vorrunde **nicht** aufgeteilt (keine Durchgang-Zeiten vorhanden, z. B. weil `Anzahl
Bahnen` genügte), fällt die Formel ersatzweise auf `Rundenstartzeit der Vorrunde + Zeitlimit +
Runden-Pause` zurück (Dauer der einzigen, ungeteilten Runde).

Der Sheet-Name der Vorrunde wird dabei über `SheetMetadataHelper.findeSheetUndHeile` aufgelöst
(Metadaten-first), nicht über den lokalisierten Default-Namen — die Formel bleibt so auch nach
einer manuellen Umbenennung des Vorrunden-Sheets gültig statt `#REF!` zu liefern.

---

## 5. Bahn-Nummerierung pro Durchgang

Standardmäßig nummeriert PTM die Bahn-Nr-Spalte fortlaufend über die ganze Runde (1..8 im
Beispiel). Bei aktiver Durchgang-Aufteilung ergibt das aber keinen Sinn, da physische Bahnen
zwischen den Durchgängen wiederverwendet werden — `bahnNummerierungProDurchgang()` beginnt die
Nummerierung stattdessen pro Block neu bei 1:

| Paarung | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 |
|---|---|---|---|---|---|---|---|---|
| **Standard** (Bahn-Nr) | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 |
| **Pro Durchgang** (Bahn-Nr) | 1 | 2 | 3 | 1 | 2 | 3 | 1 | 2 |

Das gilt nur in den nummerierten `SpielrundeSpielbahn`-Modi **X** und **N**; in den Modi **L**
(händisch) und **R** (zufällig) bleibt die Bahn-Nr-Spalte unverändert — die Durchgang-Aufteilung
ändert nichts an deren Semantik.

### Duplikat-Prüfung pro Block

Die normale Bahn-Nr-Duplikatprüfung markiert doppelte Werte über die **gesamte Spalte** per
bedingter Formatierung (`COUNTIF` über den ganzen Bereich) rot. Bei pro-Durchgang-Nummerierung
(1,2,3,1,2,3,1,2) würde diese globale Prüfung **jede** Zeile fälschlich als Duplikat markieren.
`bahnNrDuplikatPruefungProDurchgang()` ersetzt sie deshalb durch eine Variante mit einer eigenen
`COUNTIF`-Regel je Durchgang-Block — Duplikate werden nur noch innerhalb desselben Durchgangs
erkannt (z. B. zwei Paarungen im selben Durchgang, die versehentlich beide „Bahn 2" tragen).

Vor dem Anwenden der Block-Regeln wird die bedingte Formatierung über die **gesamte** Spalte in
einem Rutsch gelöscht (nicht nur die jeweilige Block-Range) — sonst bleiben bei wiederholtem
„Neu auslosen" mit wechselnder Bahnen-Anzahl (unterschiedliche Block-Grenzen je Lauf) Fragmente
älterer Regeln auf Zeilen liegen, die von der neuen Blockstruktur nicht mehr exakt überschrieben
werden. LO häuft dann mehrere, unabhängig auswertende Regeln pro Zelle an — beobachteter Bug:
Zellen mit legitim wiederkehrenden Werten wurden fälschlich rot markiert.

---

## 6. Optische Trennlinien

`durchgangTrennlinienSetzen()` zieht eine doppelte horizontale Linie am oberen Rand der jeweils
ersten Zeile eines **neuen** Durchgangs — im Beispiel also über Zeile 4 (Start Durchgang 2) und
Zeile 7 (Start Durchgang 3). Vor dem ersten Durchgang gibt es keine Trennlinie, da er direkt unter
dem bereits farblich abgesetzten Header beginnt.

Die Durchgänge werden bewusst **rein optisch** unterschieden (Trennlinie), nicht durch ein
Text-Label wie „Durchgang 2" — das hält die Tabelle kompakt.

Die Methode muss **nach** der normalen Daten-Formatierung des Sheets aufgerufen werden, da deren
dünner Rundum-Rahmen (`allThin()`) die Trennlinie sonst überschreiben würde.

---

## 7. Technische Design-Entscheidungen

### 7.1 Zirkelbezug-Vermeidung bei einzeiligen Blöcken

Frühere Version (Bug, gefixt in Commit `a125a8b7`): Bei einem einzeiligen letzten Durchgang
(nur Endzeit sichtbar) verwies die Endzeit-Formel per Zellreferenz auf ihre **eigene** Zelle
(Startposition = Endposition) → Zirkelbezug. Fix: Der Endausdruck wird direkt textuell aus dem
bereits vorliegenden `startAusdruck` abgeleitet (`startAusdruck + "+" + Zeitlimit-als-Tagesbruchteil`)
statt eine Zellreferenz auf die eigene Zeile zu bauen.

### 7.2 Zeit-Zellen sind TEXT, kein Zeitwert

Alle Zeit-Zellen werden als `TEXT(<numerischer Ausdruck>;"HH:MM")`-Formel geschrieben, nicht als
numerischer Zeitwert mit `NumberFormat`-Property. Grund: LibreOffice Calc kann Formelzellen bei
Neuberechnung (z. B. nach Änderung eines Zeitplan-Konfigwerts) intern ein eigenes Anzeigeformat
zuweisen und dabei das per API gesetzte `HH:MM` lautlos auf `HH:MM:SS` erweitern — im echten
Dokument beobachtet, nicht deterministisch reproduzierbar. Text-Zellen sind von dieser
Auto-Formatierung nicht betroffen. Konsequenz: Jede Formel, die eine Vorgänger-Zeit-Zelle
referenziert, muss sie zuerst per `TIMEVALUE(...)` in einen numerischen Wert zurückwandeln.

### 7.3 Bedingte Formatierung zellenweise löschen

`ConditionalFormatHelper.clearOnly()` löschte ursprünglich per Range-Clear — LibreOffice konnte
dabei heterogene Regel-Bereiche nicht zuverlässig bereinigen. Fix: zellenweise Iteration statt
Range-Löschung, siehe [Duplikat-Prüfung pro Block](#duplikat-prüfung-pro-block).

### 7.4 Maastrichter-Blattschutz

Maastrichter erbt die komplette Zeitplan-Logik von den Schweizer-Spielrunde-Klassen (siehe
[Abschnitt 8](#8-architektur--klassenübersicht)), hatte aber ursprünglich die
Rundenstartzeit-Zelle nicht als editierbar freigegeben — `MaastrichterBlattschutzKonfiguration`
gab sie im Blattschutz nicht frei, obwohl das Feature vollständig „geerbt" war. Gefixt in Commit
`a125a8b7` durch explizite Freigabe der Zeit-Spalte analog zu Schweizer/Formule X.

---

## 8. Architektur / Klassenübersicht

### Zentrale Interfaces (systemübergreifend, in `basesheet/`)

| Klasse | Paket | Zweck |
|---|---|---|
| `IZeitplanSpielrundeSheet` | `basesheet/spielrunde/` | Default-Methoden-Sammlung: `rundenStartzeitFeld()`, `durchgangInfoSpaltenSchreiben()`, `zeitZelleSchreiben()`, `durchgangTrennlinienSetzen()`, `bahnNummerierungProDurchgangFallsAktiv()`, `bahnNrDuplikatPruefungProDurchgang()` — die eigentliche Zeitplan-Logik, einmalig implementiert. |
| `IZeitplanPropertiesSpalte` | `basesheet/konfiguration/` | Vertragsschnittstelle: die 6 `KONFIG_PROP_ZEITPLAN_*`-Konstanten als Single Source of Truth, Getter/Setter für alle sechs Optionen, plus `isDurchgangAufteilungWirksam()`. |
| `DurchgangAufteilungRechner` | `algorithmen/common/` | Reine Berechnungsklasse: Paarungsanzahl + Bahnenanzahl → Liste der Durchgang-Blockgrößen. |
| `ZeitplanConfigProperty` | `konfigdialog/` | Marker-Klasse zur Filterung der Zeitplan-Properties im Dialog. |
| `ZeitplanDialog` | `konfigdialog/properties/` | `BasePropertiesDialog`, zeigt nur Properties vom Typ `ZeitplanConfigProperty`. |
| `StringTools.isValidUhrzeitHhMm()` | `helper/` | Strikte 24h-`HH:MM`-Validierung für die Turnier-Startzeit. |

### Turniersystem-Implementierungen

| System | Spielrunde-Sheet | Properties-Spalte | Besonderheit |
|---|---|---|---|
| **Schweizer** | `SchweizerAbstractSpielrundeSheet` | `SchweizerPropertiesSpalte` | Ur-Implementierer des Features |
| **Formule X** | `FormuleXAbstractSpielrundeSheet` | `FormuleXPropertiesSpalte` | Portierung (Commit `2880b72d`); identischer Spaltenaufbau (Bahn-Nr, Team A/B, Ergebnis A/B, Zeit-Spalte) → wortwörtlich dieselbe Logik über die Default-Methoden, keine eigene Implementierung nötig. `FormuleXBlattschutzKonfiguration` gibt die neue Zeit-Spalte zusätzlich als editierbar frei. |
| **Maastrichter** | nutzt `SchweizerSpielrundeSheetNaechste`/`-Update` direkt | erbt über `MaastrichterKonfigurationSheet extends SchweizerKonfigurationSheet` | Kein eigener Code für die Zeitplan-Logik — vollständig geerbt. `MaastrichterBlattschutzKonfiguration` musste die Zeit-Zelle separat freigeben (siehe [7.4](#74-maastrichter-blattschutz)). |

### Zero-Duplication-Refactor (Commit `c32ba42f`)

Schweizer und Formule X hatten zunächst **identische** Zeitplan-Logik dupliziert in beiden
Spielrunde-Klassen (~190 Zeilen pro Klasse). Der Refactor zieht diese Logik einmalig als
Default-Methoden in `IZeitplanSpielrundeSheet` und zentralisiert die Property-Konstanten in
`IZeitplanPropertiesSpalte`. Implementierende Klassen müssen seither nur noch die kleinen,
Sheet-spezifischen Zugriffsmethoden bereitstellen (Spalten-/Zeilenkonstanten, Sheet-Namensauflösung,
Konfigurationssheet-Zugriff) — die eigentliche Berechnung läuft für alle Systeme identisch.

---

## 9. Warum nur diese drei Systeme?

Das Feature ist speziell für **Rundenliga-Systeme** mit potenziell vielen Teams und begrenzt
verfügbaren Bahnen gedacht (viele Runden mit vielen Paarungen pro Runde): Schweizer, Maastrichter
und Formule X passen alle in dieses Muster.

Nicht implementiert für: Poule-AB, KO-Systeme (einfaches KO, Kaskaden-KO), Supermêlée, Trip-Tête,
Monrad-System, Dänisches System, Kölner Sextet. Bei diesen Systemen ist entweder die Anzahl der
gleichzeitigen Spiele pro Runde von vornherein klein (KO-Systeme: pro Runde nur noch die Hälfte
der Teams), oder die Struktur passt nicht zum Konzept „eine Runde in mehrere Durchgänge
aufteilen" (z. B. Poule-Gruppenspiele, die bereits eigenständig geplant werden).

---

## Siehe auch

- [`03_Schweizer.md`](03_Schweizer.md) — Schweizer System (Ur-Implementierer des Features)
- [`04_Maastrichter.md`](04_Maastrichter.md) — Maastrichter (erbt die Zeitplan-Logik vollständig)
- [`08_Formule_X.md`](08_Formule_X.md) — Formule X (Portierung des Features)
- [`BLATTSCHUTZ.md`](BLATTSCHUTZ.md) — Blattschutz-Architektur (relevant für die Freigabe der
  editierbaren Zeit-Zellen im Turnier-Modus)
