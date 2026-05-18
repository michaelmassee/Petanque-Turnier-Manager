# tools/

Standalone-Hilfsskripte rund um den Petanque-Turnier-Manager. Diese Skripte
gehören **nicht** zur Extension selbst — sie laufen unabhängig von LibreOffice
und werden auch nicht in die OXT gepackt. Sie sind für manuelle Analysen,
Forensik nach realen Turnieren und Cross-Check der Java-Logik gedacht.

Anforderungen: Python 3.8+, ausschliesslich Standard-Bibliothek (kein pip).

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
