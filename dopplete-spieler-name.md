# Doppelte Spielernamen — Filter „bereits in Meldeliste" / Spieler-DB-Übernahme

## Problem (User-Report)

Im Spieler-DB-Suchdialog (`SpielerSucheDialog`) gibt es eine Filter-Checkbox
„Bereits gemeldete ausblenden". Wird ein Spieler in die Meldeliste übernommen,
verschwindet aktuell auch ein **anderer** Spieler mit demselben Namen aber
anderem Verein aus der Trefferliste — obwohl es eine andere Person ist.

## Ursache (Code-Stand vor Fix)

`SpielerSucheDialog.java`:

- Feld `bereitsGemeldeteNormiert: Set<String>` enthält nur normierte
  „Vorname Nachname"-Schlüssel (Z. 96).
- Befüllt aus `ziel.getVorhandeneSpielernamen()` (Z. 453), das in
  `SheetMeldelisteAdapter.getVorhandeneSpielernamen()` (Z. 164–183) **nur**
  Vor- und Nachname aus dem Meldeliste-Sheet liest. Verein wird ignoriert,
  obwohl in `MeldelisteSpielerDaten` (vorhanden via `leseAlleSpielerRoh()`)
  der Vereinsname pro Slot bereits gelesen wird, **wenn** die Meldeliste eine
  Vereinsspalte führt.
- Filter (Z. 339–340):
  `!bereitsGemeldeteNormiert.contains(norm(s.spielernameVollstaendig()))`
  → reine Namensgleichheit verbirgt jeden Namensvetter.

`doppeltErlaubt()` (Z. 684–702) ruft `ziel.findeZeileMitName(name)` —
ebenfalls rein namensbasiert. Folge: für Namensvettern wird die
„Trotzdem hinzufügen"-Warn-MessageBox angezeigt. Nicht blockierend, aber
inkonsistent mit der Filter-Erwartung.

## Architekturkontext (für Lösungs-Design)

- **DB-Speicherung:** SQLite-Datei, Pfad lokal pro Rechner
  (`SpielerDbConnection.dbDatei()` → `jdbc:sqlite:...`). `SPIELER.NR` (PK) ist
  **nicht portabel**.
- **Stabile DB-Identifikatoren:**
  - `LIZENZNR` ist UNIQUE in der DB, jedoch **nullable**
    (vgl. `SpielerRepository.findByLizenz`, `LizenzDuplikatException`).
  - `SPIELER.NR` ist nur lokal stabil.
- **Meldeliste-Layout pro Slot:** Vorname-Spalte + Nachname-Spalte + optional
  Vereinsname-Spalte (`vereinsnameAktiv` in `SheetMeldelisteAdapter`). Es gibt
  *keine* Slot-Spalte für eine Spieler-ID.
- **Vorhandenes Pattern für versteckte Metadaten:**
  `helper/sheet/SheetMetadataHelper.java` schreibt Werte in *Named Ranges*
  (`addNewByName`, `removeByName`, `aufloeseNamedRangeAddresse`).
  Damit lässt sich pro Sheet ein verstecktes Mapping
  „Sheet-Zeile,Slot → DB-Identifier" speichern, ohne Spalten-Layout zu ändern.
- **Tests:** Weder `getVorhandeneSpielernamen` noch
  `bereitsGemeldeteNormiert` werden in `src/test/...` referenziert; reine
  Refactoring-Freiheit für Dialog + Adapter.

## Portabilitäts-Problem (Dok auf anderem Rechner / andere DB)

Wenn das Calc-Dokument auf einem anderen Rechner geöffnet wird, zeigt
`SpielerDbConnection` dort auf eine andere SQLite-Datei. Ein dort gespeicherter
DB-PK trifft typischerweise einen falschen oder gar keinen Datensatz.

**Mögliche Strategien:**

1. **Lizenz-Sticker + Name+Verein-Fallback** (geringster Aufwand,
   gut genug). Lizenznummer in Named-Range-Mapping. Auf fremdem Rechner via
   `SpielerRepository.findByLizenz()` Best-Effort-Match; bei Fehl-Match oder
   fehlender Lizenz: Vergleich Name+Verein. Spieler ohne Lizenz und ohne
   eindeutigen Verein bleiben mehrdeutig — akzeptabel (Filter ist nur
   Convenience; im Zweifel werden beide gezeigt).
2. **UUID pro Spieler in DB-Schema** (robusteste Lösung, hoher Aufwand).
   Erfordert Schema-Migration, UUID-Mit-Export im CSV, plus alle
   Übernahme-/Adapter-/Webview-Codepfade.
3. **Reine DB-PK-Sticker** — nicht portabel, daher als alleinige Lösung
   ungeeignet.

## Sofort-Fix (Minimal-Scope, ohne Sticker)

Wenn der Sticker-Ansatz aufgeschoben wird, ist der pragmatische Sofort-Fix:

- `SpielerSucheDialog.bereitsGemeldeteNormiert` wird zu
  `Map<normName, Set<normVereinOrEmpty>>`, gespeist aus
  `MeldelisteZiel.leseAlleSpielerRoh()` (existiert bereits).
- Spezialwert „leerer Verein" bedeutet: Meldeliste führt keine Vereinsspalte
  *oder* das Verein-Feld ist leer → in diesem Slot kann der Filter nicht
  unterscheiden und muss konservativ ausblenden.
- Filter-Logik:
  - kein Eintrag unter `normName` → Kandidat **bleibt sichtbar**;
  - Eintrag mit „leerem Verein" vorhanden → **ausblenden** (nicht
    unterscheidbar);
  - Kandidat selbst ohne Verein → **ausblenden** (nicht unterscheidbar);
  - sonst nur ausblenden, wenn Vereins-Schlüssel exakt matcht.
- `getVorhandeneSpielernamen()` in `MeldelisteZiel` + `SheetMeldelisteAdapter`
  entfällt (Boy-Scout: nur noch `leseAlleSpielerRoh()` wird gebraucht).

**Bewusst nicht im Sofort-Fix:**

- `doppeltErlaubt()` / `findeZeileMitName()` bleiben rein namensbasiert. Bei
  Namensvettern erscheint weiterhin die „Trotzdem hinzufügen"-Frage. Saubere
  Lösung erfordert die Sticker-Architektur.
- Meldelisten ohne Vereinsspalte (z.B. Supermelee) profitieren weiterhin
  nicht — dort bleibt der Fall „zwei verschiedene Personen, gleicher Name,
  kein Verein gespeichert" weiterhin als ein einziger Eintrag sichtbar.

## Offene Designentscheidungen (für Sticker-Variante)

1. **ID-Strategie:** Lizenz + Name+Verein-Fallback / UUID-Schema / DB-PK?
2. **Speicherort des Stickers:** Named Range mit JSON-Mapping
   (nutzt `SheetMetadataHelper`-Pattern) vs. zusätzliche versteckte Spalte
   pro Slot (ändert Meldeliste-Layout)?
3. **Verhalten bei fremder DB:** Best-Effort-Match vs. Filter automatisch
   deaktivieren, wenn der Sticker auf eine andere DB-Identität (SHA der
   DB-Datei oder DB-interne INSTANCE_ID) zeigt als die aktuell verbundene?

## Betroffene Dateien

- `src/main/java/de/petanqueturniermanager/spielerdb/ui/SpielerSucheDialog.java`
  (Filter-Logik, Cache-Befüllung)
- `src/main/java/de/petanqueturniermanager/spielerdb/MeldelisteZiel.java`
  (ggf. `getVorhandeneSpielernamen()` entfernen)
- `src/main/java/de/petanqueturniermanager/spielerdb/SheetMeldelisteAdapter.java`
  (Implementierung anpassen / Methoden entfernen)
- *Falls Sticker-Variante:*
  `src/main/java/de/petanqueturniermanager/helper/sheet/SheetMetadataHelper.java`
  (Vorbild für Named-Range-Schreibvorgänge), neue Klasse für das Mapping.

## Bisher gemachte Mini-Änderungen

In `SpielerSucheDialog.java` wurden lediglich zwei Imports vorbereitet
(`java.util.HashMap`, `java.util.Map`, `MeldelisteSpielerDaten`) — kein
Logik-Code geändert. Diese Imports können stehen bleiben oder beim
Sticker-Entwurf neu sortiert werden.
