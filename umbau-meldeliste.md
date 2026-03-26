# TeilnehmerSheet-Erweiterung – Implementierungsplan

Ziel: TeilnehmerSheet (bereinigte Teilnehmerliste als Aushang/Webseite) für alle Turniersysteme.

Ein Metadaten-Key `SCHLUESSEL_TEILNEHMER = "__PTM_TEILNEHMER__"` reicht für alle Nicht-Supermelee-Systeme (ein Dokument = ein Turniersystem). Supermelee nutzt nummerierte Keys `__PTM_SUPERMELEE_TEILNEHMER_N__` und den aktuellen Spieltag aus dem KonfigurationSheet.

---

## Erledigte Tasks

### Task 1 – SheetMetadataHelper: Umbenennung ✅
- `SCHLUESSEL_SPIELTAG_TEILNEHMER_PREFIX` → `SCHLUESSEL_SUPERMELEE_SPIELTAG_TEILNEHMER_PREFIX` (Wert unverändert: `"__PTM_SUPERMELEE_TEILNEHMER_"`)
- Methode `schluesselSpieltagTeilnehmer(int n)` → `schluesselSupermeleeTeilnehmer(int n)`
- `SCHLUESSEL_TEILNEHMER = "__PTM_TEILNEHMER__"` bleibt und wird jetzt tatsächlich genutzt

### Task 2 – Tippfehler TielnehmerSheet → TeilnehmerSheet (Supermelee) ✅
- `supermelee/meldeliste/TielnehmerSheet.java` gelöscht, `TeilnehmerSheet.java` neu erstellt
- Footer-Strings aus Hardcode in i18n migriert: `teilnehmer.footer.anzahl`, `teilnehmer.footer.teams`, `teilnehmer.footer.bahnen`
- Aufrufer angepasst: `ProtocolHandler.java`, `SpielrundeSheet_TestDaten.java`

### Task 3 – SupermeleeAktiverSpieltagSheetResolver ✅
- Neue Klasse: `webserver/SupermeleeAktiverSpieltagSheetResolver.java`
- Liest aktiven Spieltag via `SuperMeleeKonfigurationSheet.getAktiveSpieltag()`
- Cacht letzten Spieltag als `volatile int letzterSpieltagNr`

### Task 4 – SheetResolverFactory: SPIELTAG_TEILNEHMER ✅
- Case `SPIELTAG_TEILNEHMER` nutzt jetzt `new SupermeleeAktiverSpieltagSheetResolver()` statt `MetadatenPrefixSheetResolver`

### Task 5 – i18n + SheetNamen ✅
- Alle 5 Sprachdateien erweitert um:
  - `sheet.name.schweizer.teilnehmer`, `sheet.name.jgj.teilnehmer`, `sheet.name.ko.teilnehmer`
  - `processbox.teilnehmer.meldungen.einlesen`, `processbox.teilnehmer.meldungen.einfuegen`
  - `teilnehmer.footer.anzahl`, `teilnehmer.footer.teams`, `teilnehmer.footer.bahnen`
- `SheetNamen.java`: neue Methoden `schweizerTeilnehmer()`, `jgjTeilnehmer()`, `koTeilnehmer()`

### Task 6 – Neue TeilnehmerSheet-Klassen ✅
- `schweizer/meldeliste/TeilnehmerSheet.java` – nutzt `getAktiveMeldungen()`, inline VLOOKUP, Key `SCHLUESSEL_TEILNEHMER`
- `jedergegenjeden/meldeliste/TeilnehmerSheet.java` – nutzt `getAlleMeldungen()`, `meldeliste.formulaSverweisSpielernamen()`
- `ko/meldeliste/TeilnehmerSheet.java` – nutzt `getAktiveMeldungen()`, inline VLOOKUP, Key `SCHLUESSEL_TEILNEHMER`

---

## Ausstehende Tasks

### Task 7 – Menu-Integration ⏳ (NÄCHSTER SCHRITT)

#### Addons_A5_Schweizer.xcu
Einfügen **zwischen A5A4 (Update Meldeliste) und dem jetzigen Separator A5A5**, d.h. alle Nummern ab A5A5 um 1 hochzählen:
- A5A5 (neu): Teilnehmer → `ptm:schweizer_teilnehmer`
- A5A6 (war A5A5): separator
- A5A7 (war A5A6): Neue Spielrunde
- A5A8 (war A5A7): Spielrunde neu auslosen
- A5A9 (war A5A8): separator
- A5A10 (war A5A9): Rangliste
- A5A11 (war A5A10): Rangliste sortieren

#### Addons_A4_JederGJeden.xcu
Einfügen **nach A4A3 (Meldeliste aktualisieren)**, alle ab A4A4 um 1 hochzählen:
- A4A4 (neu): Teilnehmer → `ptm:jgj_teilnehmer`
- A4A5 (war A4A4): separator
- A4A6 (war A4A5): Spielplan
- A4A7 (war A4A6): Rangliste
- A4A8 (war A4A7): Rangliste sortieren
- A4A9 (war A4A8): Direktvergleich

#### Addons_A6_KO.xcu
Einfügen **nach A6A3 (Update Meldeliste)**, alle ab A6A4 um 1 hochzählen:
- A6A4 (neu): Teilnehmer → `ptm:ko_teilnehmer`
- A6A5 (war A6A4): separator
- A6A6 (war A6A5): Turnierbaum

#### ProtocolHandler.java – Konstanten (nach CMD_KO_TURNIERBAUM)
```java
public static final String CMD_SCHWEIZER_TEILNEHMER = "schweizer_teilnehmer";
public static final String CMD_JGJ_TEILNEHMER       = "jgj_teilnehmer";
public static final String CMD_KO_TEILNEHMER        = "ko_teilnehmer";
```

#### ProtocolHandler.java – Switch-Cases (execute-Methode)
```java
case CMD_KO_TEILNEHMER:
    new de.petanqueturniermanager.ko.meldeliste.TeilnehmerSheet(ws).testTurnierVorhanden().start();
    break;
case CMD_JGJ_TEILNEHMER:
    new de.petanqueturniermanager.jedergegenjeden.meldeliste.TeilnehmerSheet(ws).testTurnierVorhanden().start();
    break;
case CMD_SCHWEIZER_TEILNEHMER:
    new de.petanqueturniermanager.schweizer.meldeliste.TeilnehmerSheet(ws).testTurnierVorhanden().start();
    break;
```
Hinweis: Fully Qualified Names nötig wegen Namenskonflikt mit `supermelee.meldeliste.TeilnehmerSheet` (bereits importiert).

#### ProtocolHandler.java – isEnabled()-Switch
```java
// JGJ-Block erweitern:
case CMD_JGJ_UPDATE_MELDELISTE, CMD_JGJ_SPIELPLAN,
     CMD_JGJ_RANGLISTE, CMD_JGJ_RANGLISTE_SORTIEREN,
     CMD_JGJ_DIREKTVERGLEICH, CMD_JGJ_TEILNEHMER   -> ts == TurnierSystem.JGJ;
// Schweizer-Block erweitern:
case CMD_SCHWEIZER_NEUE_MELDELISTE,
     CMD_SCHWEIZER_UPDATE_MELDELISTE,
     CMD_SCHWEIZER_NAECHSTE_SPIELRUNDE,
     CMD_SCHWEIZER_RANGLISTE,
     CMD_SCHWEIZER_RANGLISTE_SORTIEREN,
     CMD_SCHWEIZER_TEILNEHMER                      -> ts == TurnierSystem.SCHWEIZER;
// KO-Block erweitern:
case CMD_KO_UPDATE_MELDELISTE,
     CMD_KO_TURNIERBAUM, CMD_KO_TEILNEHMER         -> ts == TurnierSystem.KO;
```

### Task 8 – Build prüfen ⏳
```bash
./gradlew test
./gradlew buildPlugin
```
