# Poule-A/B-System

Das **Poule-A/B-System** ist das klassische Format für Pétanque- und Boule-Meisterschaften. Es kombiniert eine strukturierte Gruppenphase (Poules) mit anschließenden K.O.-Runden in zwei getrennten Turnieren: dem A-Turnier (Hauptturnier) und dem B-Turnier (Consolante).

**Weiterführende Spezifikation:** → [02_Poule-AB.md](https://github.com/michaelmassee/Petanque-Turnier-Manager/blob/master/turniersysteme/02_Poule-AB.md)

---

## Voraussetzungen

| Merkmal | Wert |
|---|---|
| Mindestteamzahl | 6 Teams (zwei 3er-Poules) |
| Empfohlene Teamzahl | 8, 12, 16, 20, 24, 28, 32 Teams |
| Formation | Tête (1er), Doublette (2er), Triplette (3er) |
| Bahnen pro 4er-Poule | 2 Bahnen gleichzeitig |
| Gesamtdauer | 4–6 Stunden (Vorrunde + K.O.-Phase) |

---

## Spielsystem kurz erklärt

### Vorrunde: Poule-Phase (Double-Elimination-Light)

Jede Poule besteht aus **4 Teams** (Ausnahme: 3er-Poule bei nicht durch 4 teilbarer Teamzahl).

```
Runde 1:  Spiel A (T1 vs T2)    |   Spiel B (T3 vs T4)
Runde 2:  Siegerspiel (A-Sieger vs B-Sieger)
          Verliererspiel (A-Verlierer vs B-Verlierer)
Runde 3:  Barrage (Verlierer Siegerspiel vs Gewinner Verliererspiel)
```

**Endstand einer 4er-Poule:**

| Platz | Bilanz | K.O.-Turnier |
|-------|--------|--------------|
| 1     | 2:0    | A-Turnier    |
| 2     | 2:1    | A-Turnier    |
| 3     | 1:2    | B-Turnier    |
| 4     | 0:2    | B-Turnier    |

### K.O.-Phase

A- und B-Turnier laufen **unabhängig** voneinander als einfaches K.O.-System.

Setzlogik „über Kreuz": Teams aus derselben Poule treffen frühestens im Finale aufeinander.

Wenn die Teilnehmerzahl keine Zweierpotenz ist, wird automatisch eine **Cadrage** (Zwischenrunde) gespielt:
```
Ziel     = größte Zweierpotenz ≤ Teilnehmeranzahl
Differenz = Teilnehmeranzahl − Ziel
Cadrage-Teilnehmer = 2 × Differenz
```

---

## Workflow: Schritt-für-Schritt-Bedienung

### Schritt 1 – Start (neues Turnier)

Menü: **PétTurnMngr → Poule A/B → Start (neues Turnier)**

- Öffnet den Turnier-Parameter-Dialog
- Formation wählen: Tête / Doublette / Triplette
- Anzeigeoption für Teamnamen / Vereinsnamen festlegen
- Bestätigen → Konfigurationsblatt und leere Meldeliste werden angelegt

### Schritt 2 – Meldeliste befüllen

Menü: **PétTurnMngr → Poule A/B → Neue Meldeliste** (bei Erstanlage)

oder

Menü: **PétTurnMngr → Poule A/B → Meldeliste Aktualisieren** (bei nachträglichen Änderungen)

- Teams (Spielernamen, ggf. Vereinsname) in das Meldeliste-Sheet eintragen
- Spalte „Aktiv" auf `J` setzen für alle teilnehmenden Teams

### Schritt 3 – Teilnehmer prüfen

Menü: **PétTurnMngr → Poule A/B → Teilnehmer**

- Erzeugt das Teilnehmer-Sheet mit allen aktiven Teams
- Dient als Kontrollübersicht vor dem Turnierbeginn

### Schritt 4 – Vorrunde (Poule-Phase) erstellen

Menü: **PétTurnMngr → Poule A/B → Vorrunde erstellen**

- Berechnet die Gruppenaufteilung (4er- und ggf. 3er-Poules)
- Verteilt Teams per Snake-Seeding auf die Gruppen
- Legt das Vorrunden-Sheet an mit Spielfeldern für alle 3 Runden je Poule

### Schritt 5 – Spielpläne erstellen

Menü: **PétTurnMngr → Poule A/B → Poule Spielpläne erstellen**

- Erzeugt pro Poule ein eigenes Spielplan-Sheet
- Diese können ausgedruckt und an den Bahnen ausgehängt werden

### Schritt 6 – Ergebnisse eintragen

- Ergebnisse direkt in das Vorrunden-Sheet eintragen
- Die Formeln berechnen automatisch: Siegerspiel, Verliererspiel, Barrage-Teilnehmer und Platzierungen

### Schritt 7 – Rangliste Vorrunde

Menü: **PétTurnMngr → Poule A/B → Rangliste Vorrunde**

- Erstellt/aktualisiert die Vorrunden-Rangliste
- Zeigt für jede Poule: Platz, Bilanz (Siege/Niederlagen), Punktedifferenz
- Kennzeichnet, welche Teams ins A-Turnier bzw. B-Turnier kommen

### Schritt 8 – K.O.-Runden erstellen

Menü: **PétTurnMngr → Poule A/B → KO-Runden erstellen**

- Liest die Poule-Platzierungen aus der Rangliste
- Setzt die Teams nach der „über Kreuz"-Logik in den K.O.-Turnierbaum
- Berechnet ggf. eine Cadrage, falls die Teilnehmerzahl keine Zweierpotenz ist
- Erzeugt separate Sheets für A-Turnier und B-Turnier

---

## Tipps für die Turniersleitung

- **Bahnenplanung:** 16 Teams → mind. 8 Bahnen; 32 Teams → 16 Bahnen
- **Zeitplanung:** Vorrunde (4er-Poule) ca. 2–3 Std. | K.O.-Phase ca. 3–4 Std.
- **Ergebniserfassung:** Sofort nach Spielende eintragen, damit Folgepaarungen rechtzeitig aushängen
- **Cadrage:** Bei ungeraden Teamzahlen frühzeitig kommunizieren, welche Teams ein Freilos erhalten

---

## Detaillierte Spezifikation

Vollständige Regelwerk-Dokumentation, Sonderfälle (3er-Poule, komplexe Beispiele mit 14, 28, 50 Teams) und das ASCII-Gesamtdiagramm sind in der Spezifikationsdatei beschrieben:

→ [turniersysteme/02_Poule-AB.md](https://github.com/michaelmassee/Petanque-Turnier-Manager/blob/master/turniersysteme/02_Poule-AB.md)
