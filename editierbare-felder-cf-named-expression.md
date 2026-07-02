# Design: „Editierbare Felder hervorheben" — Add-in-CF auf Named Expression umstellen

> **Status:** Entwurf / noch nicht umgesetzt.
> **Ziel:** Den Recalc-/Thread-Storm der bedingten Formatierung für editierbare Felder
> beseitigen, indem der Per-Zelle-Aufruf `PTM.ALG.BOOLEANPROPERTY(...)` durch eine
> **native Named Expression** ersetzt wird. Verhalten (Toggle ohne Sheet-Rebuild) bleibt
> identisch.

---

## 1. Kontext & Problem

Das Feature „editierbare Felder hervorheben" (Lachsorange-Zebra auf Eingabezellen,
togglebar) ist als **bedingte Formatierung (CF)** implementiert. Die CF-Bedingung ruft
pro Zelle die Calc-Add-in-Funktion `PTM.ALG.BOOLEANPROPERTY("editierbareFelderHervorheben")`
auf (`ConditionalFormatHelper.java:124/130/136`, angewandt in
`EditierbaresZelleFormatHelper.anwenden/anwendenFuerTeam`).

### Messung (Log `~/.petanqueturniermanager/info.log`, 39-Min-UITest-Lauf, 46 soffice-Sessions)

- **2.859.354** Aufrufe von `ptmbooleanproperty("editierbareFelderHervorheben")** — ~94 % aller Log-Zeilen.
- **100 %** landen im Fallback (`GlobalImpl.java:216`, „TurnierSystem noch nicht gesetzt" → Rückgabe 0).
- **~1 UNO-Bridge-Thread pro Aufruf** (bewiesen): Session 45 = 1.022.072 Aufrufe ↔ Thread-Peak-ID 1.032.844; Gegenprobe Session 4 = 0 Aufrufe ↔ 226 Threads (Baseline).

### Ursache

Jede CF-Auswertung einer Add-in-Funktion quert die **LibreOffice-Java-UNO-Bridge**
(`sc/source/core/tool/interpr4.cxx:4552` → `ocExternal` → `ScUnoAddInCall`). Jeder
Übergang erzeugt einen `Thread-N`. Bei jedem Recalc × jede Zelle × jeder Bereich
explodiert das. **Der Effekt tritt auch in Produktion auf** (unabhängig vom TRACE-Logging):
Thread-Churn, GC-/CPU-Last und ein realer Freeze-Kandidat auf großen Turnieren (v. a. Windows).

### Warum nicht einfach die CF nativ das Property lesen lassen?

**Geht technisch nicht.** Bewiesen am LO-Quellcode (`/home/michael/devel/projects_massee/libreoffice`):

1. Die Formel-Interpreter `sc/source/core/tool/interpr1..8.cxx` (implementieren *jede*
   eingebaute Calc-Funktion) referenzieren **kein** `DocumentProperties`/`UserDefined`.
2. Einziger Doc-Property-Zugriff in `sc/source/core/` ist `editutil.cxx:273`
   (`getDocProperties()->getTitle()`) — aber nur für **`SvxFieldData`** (Titel/Autor-*Feld*
   in Kopf-/Fußzeilen), **nicht** die Formelauswertung.
3. `INFO()` (`interpr5.cxx:3336-3365`) akzeptiert nur eine feste Whitelist
   (`SYSTEM/OSVERSION/RELEASE/NUMFILE/RECALC/DIRECTORY/MEMAVAIL/MEMUSED/ORIGIN/TOTMEM`).
4. Es existiert **kein Opcode** `ocDocProp`/`ocDocInfo`/`ocMetadata` in `include/formula/opcode.hxx`.

Die Werte liegen als **User-Defined Document Properties** in den Metadaten (`meta.xml`,
`DocumentPropertiesHelper.java:239-242`), außerhalb des Zell-/Formelmodells. Damit eine
native Formel den Wert lesen kann, **muss** er ins Blattmodell **materialisiert** werden —
genau das leistet eine Named Expression.

---

## 2. Lösungsidee: „einmal statt pro Zelle"

Eine **dokumentweite Named Expression** `__PTM_EDIT_HIGHLIGHT__` hält als Inhalt die
Konstante `1` oder `0`. Die CF-Bedingung zeigt nur noch dorthin — komplett nativ:

```
alt:  AND(ISEVEN(ROW());PTM.ALG.BOOLEANPROPERTY("editierbareFelderHervorheben"))
neu:  AND(ISEVEN(ROW());__PTM_EDIT_HIGHLIGHT__=1)

alt:  AND(ISODD(ROW());PTM.ALG.BOOLEANPROPERTY("editierbareFelderHervorheben"))
neu:  AND(ISODD(ROW());__PTM_EDIT_HIGHLIGHT__=1)

alt:  PTM.ALG.BOOLEANPROPERTY("editierbareFelderHervorheben")     (anwendenFuerTeam)
neu:  __PTM_EDIT_HIGHLIGHT__=1
```

- **Property** (Metadaten) bleibt die kanonische Konfig-Quelle für **Java-Code**
  (`BasePropertiesSpalte.readBooleanProperty(...)`, Konfig-Dialog).
- **Named Expression** (Blattmodell) ist die native Quelle für die **CF-Formel**.
- Toggle schreibt **beide** (O(1)); die CF prüft nativ und quert nie die Bridge.

### Warum das Verhalten identisch bleibt

| | Heute | Nach Fix |
|---|---|---|
| Toggle an/aus | Property setzen → `calculateAll()` | Property setzen **+ Named Expression `setContent("1"/"0")`** → `calculateAll()` |
| CF-Neuauswertung | Add-in-Aufruf pro Zelle | nativer Vergleich pro Zelle |
| Sichtbares Ergebnis | Zebra an/aus, sofort | **identisch**, sofort |
| Add-in-Aufrufe / Threads | ~2,86 Mio | ~0 |

---

## 3. Konkrete Änderungspunkte

### 3.1 Neue Helper-Methoden für die Named Expression (Wiederverwendung vorhandener Infrastruktur)

`SheetMetadataHelper` nutzt bereits `XNamedRanges` (`namedRangesAusDoc(xDoc)` Zeile 818,
`addNewByName(...)` Zeile 270, `setContent(...)` Zeile 266). Analog dazu neue statische Methoden
(z. B. in `SheetMetadataHelper` oder einem kleinen neuen `EditHighlightNamedExpression`-Helper):

```
static final String EDIT_HIGHLIGHT_NAME = "__PTM_EDIT_HIGHLIGHT__";

// idempotent: anlegen falls fehlt, sonst Inhalt aktualisieren
static void setzeEditHighlight(XSpreadsheetDocument xDoc, boolean an) {
    XNamedRanges nr = namedRangesAusDoc(xDoc);
    if (nr == null) return;
    String inhalt = an ? "1" : "0";                 // Konstanten-Ausdruck
    if (nr.hasByName(EDIT_HIGHLIGHT_NAME)) {
        XNamedRange r = Lo.qi(XNamedRange.class, nr.getByName(EDIT_HIGHLIGHT_NAME));
        if (r != null) { r.setContent(inhalt); return; }
    }
    CellAddress base = new CellAddress();           // Basis für rel. Refs — bei Konstante irrelevant
    base.Sheet = 0; base.Column = 0; base.Row = 0;
    nr.addNewByName(EDIT_HIGHLIGHT_NAME, inhalt, base, 0);
}
```

> **Grammatik-Hinweis:** `getContent()`/`addNewByName` arbeiten mit der Formel-Grammatik
> (`SheetMetadataHelper.java:168` erwähnt `GRAM_API`). Eine reine Ganzzahl-Konstante `"1"`/`"0"`
> ist grammatikneutral. Vor der Umsetzung mit einem Smoke-Test verifizieren, dass
> `addNewByName(..., "1", ...)` eine auswertbare Named Expression ergibt (Alternative siehe §6).

### 3.2 `ConditionalFormatHelper` — CF-Formeln nativ machen

`ConditionalFormatHelper.java:123-139`: die drei Methoden von `GlobalImpl.FORMAT_PTM_BOOLEAN_PROPERTY(propKey)`
auf den Named-Expression-Bezug umstellen. Da diese Methoden nur für das Edit-Highlight
genutzt werden (verifiziert: einzige Verwender sind die 3 `EditierbaresZelleFormatHelper`-Aufrufe),
können sie den festen Namen verwenden oder einen Bezug-String als Parameter erhalten:

```
formulaIsEvenRowAndBoolProp: "AND(" + FORMULA_ISEVEN_ROW + ";" + EDIT_HIGHLIGHT_NAME + "=1)"
formulaIsOddRowAndBoolProp:  "AND(" + FORMULA_ISODD_ROW  + ";" + EDIT_HIGHLIGHT_NAME + "=1)"
formulaBoolProp:             EDIT_HIGHLIGHT_NAME + "=1"
```

Optional (Boy-Scout): Methoden von `…BoolProp(String propKey)` in sprechendere Namen
umbenennen, da der `propKey` nun nicht mehr in die Formel fließt.

### 3.3 `EditierbaresZelleFormatHelper` — Named Expression sicherstellen

In `anwenden(...)` und `anwendenFuerTeam(...)` (aktuell `initBooleanPropertyIfAbsent(...)`)
zusätzlich die Named Expression aus dem aktuellen Property-Wert setzen:

```
DocumentPropertiesHelper dph = new DocumentPropertiesHelper(ws);
dph.initBooleanPropertyIfAbsent(PROPERTY_KEY, true);
SheetMetadataHelper.setzeEditHighlight(ws, dph.getBooleanProperty(PROPERTY_KEY, true));
```

Damit ist bei jedem Sheet-Aufbau garantiert, dass Named Expression und Property übereinstimmen
(deckt auch Drift / Altdokumente ab, siehe §5).

### 3.4 Toggle-Pfad — Named Expression mitschreiben

`BasePropertiesSpalte.java:106-114`, die `mitNachSpeichernAktion` der Property
`KONFIG_PROP_EDITIERBARE_FELDER_HERVORHEBEN`: **vor** `calculateAll()` die Named Expression
aktualisieren:

```
.mitNachSpeichernAktion(ws -> {
    boolean an = Boolean.TRUE.equals(readBooleanProperty(KONFIG_PROP_EDITIERBARE_FELDER_HERVORHEBEN));
    SheetMetadataHelper.setzeEditHighlight(ws.getWorkingSpreadsheetDocument(), an);
    var calc = Lo.qi(XCalculatable.class, ws.getWorkingSpreadsheetDocument());
    if (calc != null) calc.calculateAll();
});
```

### 3.5 Add-in-Funktion `PTM.ALG.BOOLEANPROPERTY`

Nach dem Umbau **ohne Formel-Verwender**. Nicht entfernen (IDL/Registrierung, API-Kompatibilität),
sondern:
- `GlobalImpl.java:204-218` (`ptmbooleanproperty`) behalten, `logger.trace`-Fallback (`:216`)
  **entschärfen** (entfernen oder auf einmaliges/rate-limitiertes Logging), damit selbst bei
  Alt-CF in fremden Dokumenten keine Log-Flut mehr entsteht.
- Methode als `@Deprecated` markieren mit Verweis auf dieses Dokument.

---

## 4. Warum das den Thread-Storm beseitigt

Die `Thread-N` entstehen **ausschließlich** beim Sprung Calc → Java-Add-in
(`ScUnoAddInCall`). Native Formelbestandteile (`ISEVEN`, `ROW`, Named-Expression-Bezug,
Vergleich) bleiben komplett im C++-Formel-Interpreter — **kein Bridge-Übergang, kein Thread**.
Da `BOOLEANPROPERTY` zu 100 % nur für dieses Feature genutzt wird, verschwindet der
Storm praktisch vollständig.

---

## 5. Migration / Altdokumente

Bestehende gespeicherte Dokumente enthalten CF-Formeln mit dem alten Add-in-Aufruf. Optionen:

1. **Kein aktiver Migrationslauf nötig** — sobald ein betroffenes Sheet neu formatiert/aufgebaut
   wird (jeder `EditierbaresZelleFormatHelper.anwenden`-Pfad, der bei Turnier-Operationen
   ohnehin läuft), wird die CF mit `applyAndDoReset()` neu gesetzt → alte Formel ersetzt.
   Bis dahin funktioniert die Alt-CF weiter (nur mit dem alten, teuren Pfad).
2. **Optionaler expliziter Reparaturlauf** (falls sofortige Bereinigung gewünscht): beim Laden
   analog `UpdatePropertieFunctionsSheetRecalcOnLoad` die Named Expression sicherstellen; die
   CF-Erneuerung übernimmt der reguläre Aufbau.

Die Named Expression selbst persistiert im ODF (Teil des Dokuments) und wird beim Toggle/Build
konsistent gehalten; beim Laden ist keine Aktion nötig, wenn beim letzten Speichern synchron.

---

## 6. Risiken & offene Punkte

- **Konstante vs. Zell-Referenz als Inhalt:** Primär Konstanten-Ausdruck `"1"/"0"`. Falls
  `addNewByName` mit Konstante grammatikbedingt zickt, **Fallback-Design**: Named Expression
  zeigt auf eine reale versteckte Config-Zelle (Muster exakt wie `schreibeScoreZellenMetadaten`,
  Inhalt `$'…'.$A$1`), in die `1`/`0` geschrieben wird. CF-Formel bleibt identisch
  (`__PTM_EDIT_HIGHLIGHT__=1`). **Vor Umsetzung per Smoke-Test klären.**
- **Named-Expression-Scope:** Dokumentweit (über Doc-`XNamedRanges`). Ein Name genügt für
  alle Sheets/Systeme (Property ist dokumentglobal).
- **CF referenziert definierten Namen:** Von LO unterstützt; im Smoke-Test verifizieren, dass
  der Name in der CF-Bedingung korrekt aufgelöst wird (inkl. nach Speichern/Laden).
- **HTML-Export:** Laut `[[feedback_zebra_direkt_schreiben]]` wird CF-Zebra beim HTML-Export
  ohnehin ignoriert — durch den Umbau unverändert (Edit-Highlight ist eine reine Eingabehilfe,
  im Export nicht benötigt).

---

## 7. Teststrategie

1. **Neuer/erweiterter UITest** (z. B. an `SpielplanFormatierer`-/Meldeliste-UITest):
   - Sheet mit Edit-Highlight aufbauen → prüfen, dass Named Expression `__PTM_EDIT_HIGHLIGHT__`
     existiert und Inhalt zum Property passt.
   - CF-Bedingungsformel der editierbaren Range enthält `__PTM_EDIT_HIGHLIGHT__`, **nicht**
     `PTM.ALG.BOOLEANPROPERTY`.
   - Toggle (Property umschalten + `mitNachSpeichernAktion`) → Named-Expression-Inhalt wechselt
     `1`↔`0`, Zebra erscheint/verschwindet nach `calculateAll()`.
2. **Regressions-Beleg (Kern des Fixes):** Nach einem Aufbau-/Recalc-Zyklus im `info.log`
   `grep -c 'BooleanProperty fallback used'` → drastisch reduziert / 0, und Thread-Peak-ID
   pro Session im niedrigen Hunderterbereich statt Millionen.
3. **Round-Trip:** Dokument speichern, neu laden, `calculateAll()` → Highlight korrekt,
   keine Add-in-Aufrufe.
4. `./gradlew test spotbugsMain` grün; `./gradlew uiTests` grün.

---

## 8. Rollback

Der Umbau ist lokal in `ConditionalFormatHelper` (3 Formeln), `EditierbaresZelleFormatHelper`
(2 Methoden), `BasePropertiesSpalte` (1 Aktion) und einem Named-Expression-Helper gekapselt.
Rückkehr zum Add-in-Aufruf = die 3 CF-Formeln wieder auf `GlobalImpl.FORMAT_PTM_BOOLEAN_PROPERTY`
zeigen lassen. Die Named Expression stört auch bei Rollback nicht (bleibt ungenutzt).
