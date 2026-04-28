# Plan: Blätterliste in Sidebar als Baumstruktur

## Kontext

Die `SheetListeSidebarContent` zeigt aktuell alle PTM-Blätter als flache `XListBox` in Dokumentreihenfolge. Bei vielen Turniersystemen und Runden wird die Liste unübersichtlich, da keine Trennung nach Turniersystem stattfindet. Ziel ist eine **simulierte Baumstruktur** (weiterhin `XListBox`) mit kollabierbaren Gruppen pro Turniersystem in vordefinierter Reihenfolge.

---

## Ansatz: Simulierter Baum via XListBox

Kein neuer UNO-Control-Typ (Risiko bei Sidebar-Rendering). Stattdessen:

- **Gruppen-Header**: `"▼ Supermelee"` / `"▶ Schweizer"` (Unicode-Pfeil + `TurnierSystem.getBezeichnung()`)
- **Blatt-Einträge**: `"  Meldeliste"` (2 Leerzeichen Einrückung)
- Header-Klick → Gruppe aus-/einklappen → ListBox neu aufbauen
- Blatt-Klick → Sheet aktivieren (wie bisher)
- Kollaps-Zustand im `Set<SheetGruppe>` gespeichert (Session-Persistenz, kein Schreiben ins Dokument)

---

## Neue Dateien

### 1. `SheetGruppe.java` (Enum)
**Pfad:** `src/main/java/de/petanqueturniermanager/sidebar/sheets/SheetGruppe.java`

Enum mit einem Wert pro Turniersystem-Gruppe. Enthält:
- i18n-Key für Gruppen-Bezeichnung (nutzt `enum.turniersystem.*` die bereits existieren)
- Geordnete Liste von Key-Kategorien: `List<String>` mit Präfix-Mustern in Anzeigereihenfolge

**Gruppen und Reihenfolge:**

| Gruppe | Schlüssel-Präfixe in Reihenfolge |
|--------|----------------------------------|
| `SUPERMELEE` | MELDELISTE, TEAMS, ANMELDUNGEN_*, TEILNEHMER_*, SPIELRUNDE_PLAN_*, SPIELRUNDE_*, SPIELTAG_* (Rangliste), ENDRANGLISTE |
| `SCHWEIZER` | MELDELISTE, SPIELRUNDE_*, RANGLISTE |
| `JGJ` | MELDELISTE, SPIELPLAN, RANGLISTE, DIREKTVERGLEICH |
| `LIGA` | MELDELISTE, SPIELPLAN, RANGLISTE, DIREKTVERGLEICH |
| `KO` | MELDELISTE, TURNIERBAUM_* |
| `KASKADE` | MELDELISTE, RUNDE_*, FELD_*, GRUPPENRANGLISTE |
| `MAASTRICHTER` | MELDELISTE, VORRUNDE_*, FINALRUNDE_* |
| `POULE` | MELDELISTE, VORRUNDE, SPIELPLAN_*, VORRUNDEN_RANGLISTE, KO_* |
| `FORME` | VORRUNDEN, CADRAGE, KO_GRUPPE |
| `ALLGEMEIN` | TEILNEHMER |

Methoden:
- `static Optional<SheetGruppe> fuerSchluessel(String schluessel)` – Zuordnung Metadaten-Key → Gruppe
- `int reihenfolgeDesSchluessels(String schluessel)` – Position innerhalb der Gruppe (für Sortierung)
- `String getAnzeigeBezeichnung()` – `I18n.get(i18nKey)` (für FORME: neuer Key `enum.turniersystem.forme`)

### 2. `BlattBaumEintrag.java` (sealed interface + Records)
**Pfad:** `src/main/java/de/petanqueturniermanager/sidebar/sheets/BlattBaumEintrag.java`

```java
sealed interface BlattBaumEintrag permits GruppenKopf, BlattKnoten {}

record GruppenKopf(SheetGruppe gruppe, boolean expandiert) implements BlattBaumEintrag {
    String anzeigeText() { return (expandiert ? "▼ " : "▶ ") + gruppe.getAnzeigeBezeichnung(); }
}

record BlattKnoten(XSpreadsheet sheet, String anzeigeText, String metadatenSchluessel)
    implements BlattBaumEintrag {}
```

### 3. `SheetBaumOrganisierer.java`
**Pfad:** `src/main/java/de/petanqueturniermanager/sidebar/sheets/SheetBaumOrganisierer.java`

Verantwortlich für:
1. **`baumAufbauen(List<XSpreadsheet>, XSpreadsheetDocument, Set<SheetGruppe> kollabiert)`**  
   → gibt `List<BlattBaumEintrag>` zurück (Header + sichtbare Knoten)
2. Interne Logik: Key-Präfix-Matching über `SheetGruppe.fuerSchluessel()`, Sortierung nach `reihenfolgeDesSchluessels()`
3. Blätter ohne erkannte Gruppe → Gruppe `ALLGEMEIN` oder am Ende ohne Header

---

## Geänderte Dateien

### `SheetListeSidebarContent.java`
`src/main/java/de/petanqueturniermanager/sidebar/sheets/SheetListeSidebarContent.java`

**Feldänderungen:**
- Ersetze `List<XSpreadsheet> aktuelleSheets` durch `List<BlattBaumEintrag> baumEintraege`
- Neu: `Set<SheetGruppe> kollabierteGruppen = new HashSet<>()`
- Neu: `SheetBaumOrganisierer organisierer = new SheetBaumOrganisierer()`

**Methodenänderungen:**

`sheetListeAufbauenMitEintraegen()`:
- Ruft `organisierer.baumAufbauen(sheets, xDoc, kollabierteGruppen)` auf
- Höhe: `Math.max(MIN_HOEHE, Math.min(baumEintraege.size() * ZEILEN_HOEHE, MAX_HOEHE))`
- Befüllt ListBox mit `eintrag.anzeigeText()` für jeden Eintrag

`listBoxBefuellen()`:
- Iteriert über `baumEintraege`, schreibt `anzeigeText()` in die ListBox

`itemStateChanged()`:
- Switch auf `baumEintraege.get(idx)`:
  - `GruppenKopf k` → Toggle: `kollabierteGruppen.add/remove(k.gruppe())`, dann `auswahlMerken()` + `allesFelderEntfernenUndNeuFenster()` + `felderHinzufuegen()`
  - `BlattKnoten b` → `TurnierSheet.from(b.sheet(), ...).setActiv()`

`auswahlMerken()` / `auswahlWiederherstellen()`:
- Iteriert über `baumEintraege` statt `aktuelleSheets`, nutzt nur `BlattKnoten`-Einträge

`sheetOeffnen()`:
- Liest `BlattKnoten` aus `baumEintraege` nach Index

`onDisposing()`:
- Setzt `baumEintraege.clear()`, kein Änderungsbedarf sonst

---

## i18n-Änderungen

**Neue Keys in allen 5 Sprach-Dateien** (`messages.properties`, `_en`, `_fr`, `_nl`, `_es`):

```properties
# Forme-System (hat keinen TurnierSystem-Enum-Eintrag)
enum.turniersystem.forme=Formule X
```

Alle anderen Gruppen-Bezeichnungen nutzen bereits vorhandene `enum.turniersystem.*`-Keys.

---

## Kritische Dateien

| Datei | Aktion |
|-------|--------|
| `sidebar/sheets/SheetListeSidebarContent.java` | Hauptrefactor |
| `sidebar/sheets/SheetGruppe.java` | Neu erstellen |
| `sidebar/sheets/BlattBaumEintrag.java` | Neu erstellen |
| `sidebar/sheets/SheetBaumOrganisierer.java` | Neu erstellen |
| `helper/sheet/SheetMetadataHelper.java` | Nur Referenz (keine Änderungen) |
| `supermelee/meldeliste/TurnierSystem.java` | Nur Referenz (keine Änderungen) |
| `i18n/messages*.properties` | 1 neuer Key pro Datei |

---

## Verifikation

1. `./gradlew test` – bestehende Tests müssen weiterhin grünen
2. `./gradlew reinstallExtension` + LibreOffice starten
3. PTM-Dokument mit Schweizer-System öffnen → Sidebar zeigt Gruppe "Schweizer" mit Meldeliste, Spielrunden, Rangliste
4. Header-Klick → Gruppe kollabiert (Pfeil wechselt ▼ → ▶, Einträge verschwinden)
5. Nochmal klicken → Gruppe expandiert
6. Blatt klicken → Sheet wird aktiviert
7. Turnier-Event auslösen (neue Spielrunde anlegen) → Sidebar aktualisiert sich, neuer Eintrag erscheint in richtiger Gruppe
8. Gruppen ohne Blätter werden nicht angezeigt
