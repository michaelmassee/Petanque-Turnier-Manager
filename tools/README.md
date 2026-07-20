# tools/

Standalone-Hilfsskripte rund um den Petanque-Turnier-Manager. Diese Skripte
gehören **nicht** zur Extension selbst — sie laufen unabhängig von LibreOffice
und werden auch nicht in die OXT gepackt. Sie sind für manuelle Analysen,
Forensik nach realen Turnieren und Cross-Check der Java-Logik gedacht.

Anforderungen: Python 3.8+, ausschliesslich Standard-Bibliothek (kein pip).

Ausnahme: `linux/` enthält Skripte zur **Fernsteuerung einer laufenden
LibreOffice-Instanz** via UNO + AT-SPI für Multi-Doc-/Modal-Repros. Diese
benötigen `python3-gi`, `at-spi2-core` (System) sowie optional `python-uinput`
für reine X11-Setups. Details siehe `tools/linux/README.md`.

## analyse_supermelee_spieltag.py

Konsistenz-Analyse einer Supermelee-Spieltag-ODS.

Liest direkt aus der ODS (`content.xml` + `meta.xml`) und prüft:

1. **Mitspieler-Wiederholungen** — war ein Spielerpaar mehrfach im selben Team?
2. **Gegner-Wiederholungen** — wie oft trafen Spielerpaare als Gegner aufeinander?
3. **Rangliste-Werte** (Σ+, Σ−, Δ, Siege, Punkte) — vergleicht die Werte im
   Rangliste-Sheet gegen die aus den Spielrunden-Sheets nachgerechneten
   Summen. Berücksichtigt dabei die Default-Punkte-Buchung für nicht
   angetretene Spieler (Property `Nicht gespielte Runde, + Punkte` /
   `... - Punkte`, Defaults 0 / 13 aus
   `SuperMeleePropertiesSpalte.java`).
4. **Rangliste-Sortierung** gegen `Siege ↓ → Punkte ↓ → Δ ↓ → Σ+ ↓`
   (Supermelee verwendet **keinen** Direktvergleich-Tiebreaker — der gilt nur
   in Poule-AB, siehe `PouleRanglisteRechner`).

Aufruf:

```bash
python3 tools/analyse_supermelee_spieltag.py /pfad/zur/datei.ods
python3 tools/analyse_supermelee_spieltag.py /pfad/zur/datei.ods --spieltag 2
```

Beispiel-Output (gekürzt):

```
--- 1) MITSPIELER-WIEDERHOLUNGEN ---
  ✓ keine Wiederholungen (212 Paare bildeten je 1× ein Team)

--- 2) GEGNER-WIEDERHOLUNGEN ---
  Gegner-Paare insgesamt: 297, davon mit Wiederholung: 23
     20 Paare spielten 2× gegeneinander
     3 Paare spielten 3× gegeneinander
     3× :  11 Schifferdecker, Horst         vs   13 Veith, Ingolf
     ...

--- 3) RANGLISTE-WERTE  (1. Spieltag Rangliste) ---
  ✓ Alle 57 Spieler stimmen exakt überein (inkl. Default-Buchung für nicht gespielte Runden)

--- 4) RANGLISTE-SORTIERUNG (Siege ↓ → Punkte ↓ → Δ ↓ → Σ+ ↓) ---
  ✓ Rangliste korrekt sortiert (57 Spieler)
```

Annahmen über das Sheet-Layout sind aus `SpielrundeSheetKonstanten.java`
übernommen und an einer Stelle im Skript zentralisiert — bei Layout-Änderungen
in der Extension nachziehen.

## analyse_schweizer_rangliste.py

Konsistenz-Analyse einer Schweizer- oder Maastrichter-Rangliste-ODS.

Liest direkt aus der ODS (`content.xml` + `meta.xml`) und prüft die
Rangliste-Werte (Siege, BHZ, FBHZ, Punkte+, Punkte-, Punktedifferenz) gegen
die aus den Spielrunden-Sheets nachgerechneten Werte:

1. **Siege / Punkte+ / Punkte-** — aus allen Spielrunden-Sheets aufsummiert,
   inkl. Freilos-Buchung mit den konfigurierten Freispiel-Punkten
   (Property `Freispiel Punkte +` / `Freispiel Punkte -`, Defaults 13 / 7
   aus `SchweizerPropertiesSpalte.java`).
2. **Buchholz (BHZ)** = Summe der Siege aller Gegner. Ungespielte
   Paarungen (Ergebnis 0:0, z.B. bereits ausgeloste aber noch nicht
   gespielte Folgerunde) zählen dabei **nicht** als Gegner.
3. **Feinbuchholz (FBHZ)** = Summe der BHZ-Werte aller Gegner.
4. **Rangliste-Sortierung** gegen `Siege ↓ → BHZ ↓ → FBHZ ↓ → Punktediff ↓
   → Punkte+ ↓` (`SchweizerSystem.sortiereNachAuswertungskriterien`).

Erkennt automatisch, ob es sich um eine Schweizer-Rangliste (Sheet
"Rangliste") oder eine Maastrichter-Vorrunden-Rangliste (Sheet
"Vorrunden-Rangliste") handelt — beide nutzen dieselbe Auswertungslogik.

Aufruf:

```bash
python3 tools/analyse_schweizer_rangliste.py /pfad/zur/datei.ods
python3 tools/analyse_schweizer_rangliste.py /pfad/zur/datei.ods --bis-runde 2
```

Beispiel-Output (gekürzt):

```
--- RANGLISTE-WERTE (Vorrunden-Rangliste, 27 Teams, 27 Teams mit Spieldaten) ---
  ✓ Alle 27 Teams stimmen exakt überein (Siege/BHZ/FBHZ/Punkte+/Punkte-/Diff)

--- RANGLISTE-SORTIERUNG (Siege ↓ → BHZ ↓ → FBHZ ↓ → Punktediff ↓ → Punkte+ ↓) ---
  ✓ Rangliste korrekt sortiert (27 Teams)
```

`--bis-runde N` schränkt die Nachrechnung auf die ersten N Spielrunden ein
— nützlich, um einen früheren Turnierstand zu prüfen. Da die Rangliste im
Sheet aber immer bis zur aktuell aktiven Runde aufgebaut ist (inkl. bereits
gezogener, aber noch nicht gespielter Freilos-Paarungen), führt ein
`--bis-runde` kleiner als die aktive Runde erwartungsgemäß zu Abweichungen.

Annahmen über das Sheet-Layout sind aus `SchweizerRanglisteSheet.java` /
`SchweizerAbstractSpielrundeSheet.java` übernommen und an einer Stelle im
Skript zentralisiert — bei Layout-Änderungen in der Extension nachziehen.

## test_analyse_supermelee_sync.py

Sync-Check zwischen `analyse_supermelee_spieltag.py` und
`SupermeleeSpieltagAnalyseAssert.java` (automatischer UITest).

Beide Dateien implementieren dieselbe Prüflogik in zwei Runtimes. Dieses Skript
liest die Java-Quellkonstanten per Regex und verifiziert, dass die Python-
Hardcodes damit übereinstimmen. Schlägt es fehl, müssen beide Dateien zusammen
angepasst werden.

```bash
python3 tools/test_analyse_supermelee_sync.py
```
