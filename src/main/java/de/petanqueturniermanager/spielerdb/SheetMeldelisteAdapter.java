package de.petanqueturniermanager.spielerdb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzManager;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

/**
 * Generischer Adapter, der direkt auf das aktive Meldeliste-Sheet schreibt.
 *
 * <p>Spalten-Layout entspricht
 * {@link de.petanqueturniermanager.basesheet.meldeliste.TeilnehmerNamenLeser}
 * (Quelle der Wahrheit über alle Turniersysteme):
 * <ul>
 *   <li>Spalte 0: Team-Nr</li>
 *   <li>Spalte 1: optional Teamname (wenn Property {@code Meldeliste Teamname = "J"})</li>
 *   <li>Ab {@code ersterSpielerOffset = teamnameAktiv ? 2 : 1}: pro Spieler
 *       <b>2 oder 3 Spalten</b> nebeneinander — Vorname, Nachname, optional
 *       Vereinsname (wenn Property {@code Meldeliste Vereinsname = "J"}).</li>
 * </ul>
 *
 * <p>Beispiel Doublette ohne Teamname/Verein:
 * <pre>
 *   A=Nr  B=Vorname1  C=Nachname1  D=Vorname2  E=Nachname2
 * </pre>
 *
 * <p>Schreibmodell für die Übernahme: das vom Dialog gesammelte Team
 * (n = {@link Formation#getAnzSpieler()}) wird als <b>eine</b> Zeile in genau
 * diese Slots geschrieben — Vorname und Nachname kommen aus
 * {@link SpielerMitVerein#vorname()} / {@link SpielerMitVerein#nachname()},
 * der Vereinsname aus {@link SpielerMitVerein#vereinName()}.
 */
final class SheetMeldelisteAdapter implements MeldelisteZiel {

    private static final Logger logger = LogManager.getLogger(SheetMeldelisteAdapter.class);

    private static final int SPALTE_NR = 0;
    private static final int HEADER_ZEILE_MAX_SCAN = 5;
    private static final int MAX_DATEN_ZEILE = 999;
    private static final int MAX_UUID_SPALTE_SCAN = 100;

    /**
     * Wert in der Aktiv-Spalte: „nimmt teil". Die Konvention ist über
     * Schweizer/JGJ/KO/Poule/FormuleX/Kaskade hinweg konstant
     * ({@code AKTIV_WERT_NIMMT_TEIL = 1}); die Aktiv-Spalte selbst sitzt zwei
     * Spalten rechts neben der letzten Spielerdaten-Spalte (dazwischen liegt
     * SP/RNG). Gesetzt nur für {@link NeueMeldungTeilnahme#AKTIV}.
     */
    private static final int AKTIV_WERT_NIMMT_TEIL = 1;
    /** In allen unterstützten Team-Meldelisten bedeutet 2 „ausgestiegen/abgemeldet“. */
    private static final int AKTIV_WERT_ABGEMELDET = 2;
    /**
     * Systeme mit Setzpositionsspalte direkt vor der Aktiv-Spalte: SP bzw. bei KO die RNG-Spalte (Setzreihenfolge
     * des Turnierbaums). Supermêlée und Trip-Tête haben keine.
     */
    private static final Set<TurnierSystem> SYSTEME_MIT_SETZPOSITION = Set.of(TurnierSystem.SCHWEIZER,
            TurnierSystem.MAASTRICHTER, TurnierSystem.JGJ, TurnierSystem.KO, TurnierSystem.KASKADE,
            TurnierSystem.POULE, TurnierSystem.FORMULEX);

    private final XSpreadsheetDocument doc;
    private final XSpreadsheet sheet;
    private final SheetHelper sheetHelper;
    private final TurnierSystem system;
    private final Formation formation;
    private final int ersteDatenZeile;
    private final boolean teamnameAktiv;
    private final boolean vereinsnameAktiv;
    private final int anzSpieler;
    private final int ersterSpielerOffset;
    private final int spaltenProSpieler;
    /** Erste Spalte rechts vom letzten Spieler-Block (exklusive). */
    private final int letzteSchreibSpalte;
    /** Einmal ermittelte UUID-Spalte, {@code -1} solange unbekannt (siehe {@link #uuidSpalte()}). */
    private int uuidSpalteCache = -1;

    private SheetMeldelisteAdapter(XSpreadsheetDocument doc, XSpreadsheet sheet,
            SheetHelper sheetHelper, TurnierSystem system, Formation formation,
            int ersteDatenZeile, boolean teamnameAktiv, boolean vereinsnameAktiv) {
        this.doc = doc;
        this.sheet = sheet;
        this.sheetHelper = sheetHelper;
        this.system = system;
        this.formation = formation;
        this.ersteDatenZeile = ersteDatenZeile;
        this.teamnameAktiv = teamnameAktiv;
        this.vereinsnameAktiv = vereinsnameAktiv;
        this.anzSpieler = Math.max(1, formation.getAnzSpieler());
        this.ersterSpielerOffset = ersterSpielerOffset(teamnameAktiv);
        this.spaltenProSpieler = vereinsnameAktiv ? 3 : 2;
        this.letzteSchreibSpalte = ersterSpielerOffset + anzSpieler * spaltenProSpieler - 1;
    }

    /**
     * Baut einen Adapter aus expliziten Layout-Werten (Formation und Anzeige-Flags),
     * die der Aufrufer aus dem system-spezifischen KonfigurationSheet gelesen hat.
     * Vermeidet Property-Lookups direkt im Adapter — die Quelle der Wahrheit ist
     * jeweils der zum Turniersystem passende {@code *KonfigurationSheet}.
     */
    static Optional<MeldelisteZiel> fuer(WorkingSpreadsheet ws, String sheetName,
            TurnierSystem ts, Formation formation, boolean teamnameAktiv, boolean vereinsnameAktiv) {
        try {
            if (ts == null || ts == TurnierSystem.KEIN) {
                return Optional.empty();
            }
            SheetHelper sh = new SheetHelper(ws);
            XSpreadsheet sheet = sh.findByName(sheetName);
            if (sheet == null) {
                return Optional.empty();
            }
            int datenZeile = ermittleErsteDatenZeile(sh, sheet, ersterSpielerOffset(teamnameAktiv));
            return Optional.of(new SheetMeldelisteAdapter(
                    ws.getWorkingSpreadsheetDocument(), sheet, sh, ts, formation, datenZeile,
                    teamnameAktiv, vereinsnameAktiv));
        } catch (Exception e) {
            logger.warn("Adapter-Erkennung fehlgeschlagen", e);
            return Optional.empty();
        }
    }

    /** Spalte des ersten Vornamens: rechts neben Nr und ggf. Teamname. */
    private static int ersterSpielerOffset(boolean teamnameAktiv) {
        return teamnameAktiv ? 2 : 1;
    }

    /**
     * Erste Datenzeile: die erste Zeile mit Nr. Ohne vergebene Nr (leere oder noch nicht
     * aktualisierte Meldeliste) die Zeile unter der Spaltenüberschrift „Vorname“ – sonst würde
     * die Überschriftenzeile als Meldung „Vorname Nachname“ gelesen.
     */
    private static int ermittleErsteDatenZeile(SheetHelper sh, XSpreadsheet sheet, int vornameSpalte) {
        for (int zeile = 0; zeile <= HEADER_ZEILE_MAX_SCAN; zeile++) {
            String inhalt = sicherText(sh, sheet, SPALTE_NR, zeile).strip();
            if (inhalt.matches("\\d+")) {
                return zeile;
            }
        }
        String vornameUeberschrift = I18n.get("column.header.vorname");
        for (int zeile = 0; zeile <= HEADER_ZEILE_MAX_SCAN; zeile++) {
            if (vornameUeberschrift.equalsIgnoreCase(sicherText(sh, sheet, vornameSpalte, zeile).strip())) {
                return zeile + 1;
            }
        }
        // Fallback: erste Zeile direkt nach erkanntem Header — Spalte A leer.
        for (int zeile = 0; zeile <= HEADER_ZEILE_MAX_SCAN; zeile++) {
            if (sicherText(sh, sheet, SPALTE_NR, zeile).strip().isEmpty()) {
                return zeile;
            }
        }
        return 2;
    }

    private static String sicherText(SheetHelper sh, XSpreadsheet sheet, int spalte, int zeile) {
        try {
            String s = sh.getTextFromCell(sheet, Position.from(spalte, zeile));
            return s == null ? "" : s;
        } catch (Exception e) {
            return "";
        }
    }

    @Override public Formation getFormation() { return formation; }
    @Override public String getSystemBezeichnung() { return system.getBezeichnung(); }

    /** Spaltenindex der Vornamen-Zelle für Spieler-Slot {@code i} (0-basiert). */
    private int vornameSpalte(int slotIndex) {
        return ersterSpielerOffset + slotIndex * spaltenProSpieler;
    }

    /** Spaltenindex der Nachnamen-Zelle für Spieler-Slot {@code i} (0-basiert). */
    private int nachnameSpalte(int slotIndex) {
        return vornameSpalte(slotIndex) + 1;
    }

    @Override
    public List<String> getVorhandeneSpielernamen() {
        List<String> namen = new ArrayList<>();
        for (int zeile = ersteDatenZeile; zeile <= MAX_DATEN_ZEILE; zeile++) {
            String erstesVor = sicherText(sheetHelper, sheet, vornameSpalte(0), zeile).strip();
            String erstesNach = sicherText(sheetHelper, sheet, nachnameSpalte(0), zeile).strip();
            if (erstesVor.isEmpty() && erstesNach.isEmpty()) {
                break; // erste Spieler-Position leer → Zeile als unbelegt werten
            }
            for (int s = 0; s < anzSpieler; s++) {
                String vor  = sicherText(sheetHelper, sheet, vornameSpalte(s), zeile).strip();
                String nach = sicherText(sheetHelper, sheet, nachnameSpalte(s), zeile).strip();
                String voll = (vor + " " + nach).strip();
                if (!voll.isEmpty()) {
                    namen.add(voll);
                }
            }
        }
        return namen;
    }

    @Override
    public MeldelisteStatus getMeldelisteStatus() {
        int checkin = 0;
        int gesamt = 0;
        int aktivSpalte = aktivSpalte();
        for (int zeile = ersteDatenZeile; zeile <= MAX_DATEN_ZEILE; zeile++) {
            if (!istTeamZeileBelegt(zeile)) {
                break;
            }
            gesamt++;
            if (!sicherText(sheetHelper, sheet, aktivSpalte, zeile).strip().isEmpty()) {
                checkin++;
            }
        }
        return new MeldelisteStatus(gesamt - checkin, checkin, gesamt);
    }

    private boolean istTeamZeileBelegt(int zeile) {
        for (int s = 0; s < anzSpieler; s++) {
            String vor = sicherText(sheetHelper, sheet, vornameSpalte(s), zeile).strip();
            String nach = sicherText(sheetHelper, sheet, nachnameSpalte(s), zeile).strip();
            if (!vor.isEmpty() || !nach.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** Spaltenindex der optionalen Vereinsname-Zelle für Spieler-Slot {@code i} (0-basiert). */
    private int vereinSpalte(int slotIndex) {
        return nachnameSpalte(slotIndex) + 1;
    }

    @Override
    public List<MeldelisteSpielerDaten> leseAlleSpielerRoh() {
        List<MeldelisteSpielerDaten> result = new ArrayList<>();
        for (int zeile = ersteDatenZeile; zeile <= MAX_DATEN_ZEILE; zeile++) {
            String erstesVor = sicherText(sheetHelper, sheet, vornameSpalte(0), zeile).strip();
            String erstesNach = sicherText(sheetHelper, sheet, nachnameSpalte(0), zeile).strip();
            if (erstesVor.isEmpty() && erstesNach.isEmpty()) {
                break;
            }
            for (int s = 0; s < anzSpieler; s++) {
                String vor  = sicherText(sheetHelper, sheet, vornameSpalte(s), zeile).strip();
                String nach = sicherText(sheetHelper, sheet, nachnameSpalte(s), zeile).strip();
                if (vor.isEmpty() && nach.isEmpty()) {
                    continue;
                }
                String verein = vereinsnameAktiv
                        ? sicherText(sheetHelper, sheet, vereinSpalte(s), zeile).strip()
                        : null;
                result.add(new MeldelisteSpielerDaten(vor, nach, verein, zeile + 1));
            }
        }
        return result;
    }

    @Override
    public int findeZeileMitName(String spielerName) {
        String norm = spielerName.strip().toLowerCase(Locale.ROOT);
        for (int zeile = ersteDatenZeile; zeile <= MAX_DATEN_ZEILE; zeile++) {
            String erstesVor = sicherText(sheetHelper, sheet, vornameSpalte(0), zeile).strip();
            String erstesNach = sicherText(sheetHelper, sheet, nachnameSpalte(0), zeile).strip();
            if (erstesVor.isEmpty() && erstesNach.isEmpty()) {
                return -1;
            }
            for (int s = 0; s < anzSpieler; s++) {
                String vor  = sicherText(sheetHelper, sheet, vornameSpalte(s), zeile).strip();
                String nach = sicherText(sheetHelper, sheet, nachnameSpalte(s), zeile).strip();
                String voll = (vor + " " + nach).strip();
                if (voll.toLowerCase(Locale.ROOT).equals(norm)) {
                    return zeile + 1; // 1-basiert (Sheet-Zeile inkl. Header)
                }
            }
        }
        return -1;
    }

    /**
     * Schreibt ein Team in <b>eine</b> Meldeliste-Zeile: pro Spieler getrennte
     * Vorname-/Nachname-Zellen (und ggf. Verein), passend zum Layout
     * {@code TeilnehmerNamenLeser}. Nicht gefüllte Slots (z.B. unvollständiges
     * Triplette-Sammelpanel) bleiben leer.
     *
     * @return Anzahl tatsächlich geschriebener Spieler.
     */
    @Override
    public int schreibeBlock(List<SpielerMitVerein> spieler) throws MeldelisteSchreibException {
        return schreibeBlockUndLiefereZeile(spieler, NeueMeldungTeilnahme.AKTIV) < 0 ? 0 : spieler.size();
    }

    @Override
    public int schreibeBlockUndLiefereZeile(List<SpielerMitVerein> spieler, NeueMeldungTeilnahme teilnahme)
            throws MeldelisteSchreibException {
        if (spieler.isEmpty()) {
            return 0;
        }
        if (spieler.size() > anzSpieler) {
            throw new MeldelisteSchreibException(
                    "Mehr Spieler als Slots: " + spieler.size() + " > " + anzSpieler);
        }
        int zeile = naechsteFreieZeile();
        if (zeile < 0) {
            throw new MeldelisteSchreibException("Keine freie Zeile in der Meldeliste");
        }

        try {
            // Team-Nr (Spalte 0) wird bewusst nicht hier gesetzt — der
            // anschließende „Meldeliste Aktualisieren"-Lauf vergibt fortlaufend
            // konsistente Nummern für alle aktiven Teams.
            RowData row = new RowData();
            // Teamname-Slot bleibt leer — User füllt das ggf. manuell.
            if (teamnameAktiv) {
                row.newEmpty();
            }
            for (SpielerMitVerein s : spieler) {
                row.newString(s.vorname());
                row.newString(s.nachname());
                if (vereinsnameAktiv) {
                    row.newString(s.vereinName() == null ? "" : s.vereinName());
                }
            }
            // Padding für nicht gefüllte Slots (Schutz, eigentlich blockiert der
            // Dialog die Übernahme bevor das Team unvollständig ist):
            int luecke = (anzSpieler - spieler.size()) * spaltenProSpieler;
            for (int i = 0; i < luecke; i++) {
                row.newEmpty();
            }

            RangeData rangeData = new RangeData();
            rangeData.add(row);

            // Range startet immer in Spalte 1 (rechts neben Spalte 0 = Nr).
            RangePosition pos = RangePosition.from(1, zeile, letzteSchreibSpalte, zeile);
            RangeHelper.from(sheet, doc, pos).setDataInRange(rangeData);

            // Aktiv-Spalte (= letzteDatenSpalte + 2) nur bei AKTIV auf „nimmt teil"
            // setzen; INAKTIV lässt sie leer (noch nicht eingecheckt).
            if (teilnahme == NeueMeldungTeilnahme.AKTIV) {
                BlattschutzManager.get().schreibeEntsperrt(sheet, () -> sheetHelper.setNumberValueInCell(NumberCellValue
                        .from(sheet, Position.from(aktivSpalte(), zeile)).setValue(AKTIV_WERT_NIMMT_TEIL)));
            }
            return zeile + 1;
        } catch (Exception e) {
            throw new MeldelisteSchreibException("Schreibvorgang fehlgeschlagen", e);
        }
    }

    @Override
    public int getAktivWertAusZeile(int zeile1Basiert) {
        if (zeile1Basiert <= 0) {
            return -1;
        }
        return sheetHelper.getIntFromCell(sheet, Position.from(aktivSpalte(), zeile1Basiert - 1));
    }

    @Override
    public int getTeamNrAusZeile(int zeile1Basiert) {
        if (zeile1Basiert <= 0) {
            return -1;
        }
        return sheetHelper.getIntFromCell(sheet, Position.from(SPALTE_NR, zeile1Basiert - 1));
    }

    @Override
    public OptionalInt getSetzpositionAusZeile(int zeile1Basiert) {
        if (zeile1Basiert <= 0 || !SYSTEME_MIT_SETZPOSITION.contains(system)) {
            return OptionalInt.empty();
        }
        int setzposition = sheetHelper.getIntFromCell(sheet, Position.from(setzpositionSpalte(), zeile1Basiert - 1));
        return setzposition > 0 ? OptionalInt.of(setzposition) : OptionalInt.empty();
    }

    @Override
    public void uebernehmeOnlineSetzposition(int zeile1Basiert, int setzposition) throws MeldelisteSchreibException {
        if (setzposition <= 0 || !SYSTEME_MIT_SETZPOSITION.contains(system)
                || getSetzpositionAusZeile(zeile1Basiert).isPresent()) {
            return;
        }
        try {
            BlattschutzManager.get().schreibeEntsperrt(sheet, () -> sheetHelper.setNumberValueInCell(
                    NumberCellValue.from(sheet, Position.from(setzpositionSpalte(), zeile1Basiert - 1))
                            .setValue(setzposition)));
        } catch (Exception e) {
            throw new MeldelisteSchreibException("Setzposition konnte nicht geschrieben werden", e);
        }
    }

    @Override
    public void markiereAlsAbgemeldet(int zeile1Basiert) throws MeldelisteSchreibException {
        if (zeile1Basiert <= 0) {
            throw new MeldelisteSchreibException("Ungültige Meldelistenzeile");
        }
        try {
            BlattschutzManager.get().schreibeEntsperrt(sheet, () -> sheetHelper.setNumberValueInCell(
                    NumberCellValue.from(sheet, Position.from(aktivSpalte(), zeile1Basiert - 1))
                            .setValue(AKTIV_WERT_ABGEMELDET)));
        } catch (Exception e) {
            throw new MeldelisteSchreibException("Abmeldung konnte nicht markiert werden", e);
        }
    }

    @Override
    public void hebeAbmeldungAuf(int zeile1Basiert) throws MeldelisteSchreibException {
        if (zeile1Basiert <= 0) {
            throw new MeldelisteSchreibException("Ungültige Meldelistenzeile");
        }
        try {
            Position aktiv = Position.from(aktivSpalte(), zeile1Basiert - 1);
            if (sheetHelper.getIntFromCell(sheet, aktiv) == AKTIV_WERT_ABGEMELDET) {
                BlattschutzManager.get().schreibeEntsperrt(sheet, () -> sheetHelper.clearValInCell(sheet, aktiv));
            }
        } catch (Exception e) {
            throw new MeldelisteSchreibException("Abmeldung konnte nicht aufgehoben werden", e);
        }
    }

    @Override
    public String getOderErzeugeLokaleUuid(int zeile1Basiert) throws MeldelisteSchreibException {
        String uuid = getOderErzeugeLokaleUuids(List.of(zeile1Basiert)).get(zeile1Basiert);
        if (uuid == null) {
            throw new MeldelisteSchreibException("Lokale PTM-Online-ID konnte nicht geschrieben werden");
        }
        return uuid;
    }

    @Override
    public Map<Integer, String> getOderErzeugeLokaleUuids(Collection<Integer> zeilen1Basiert)
            throws MeldelisteSchreibException {
        if (zeilen1Basiert.stream().anyMatch(zeile -> zeile <= 0)) {
            throw new MeldelisteSchreibException("Ungültige Meldelistenzeile");
        }
        try {
            return uuidSpalteBlock().getOderErzeuge(zeilen1Basiert);
        } catch (Exception e) {
            throw new MeldelisteSchreibException("Lokale PTM-Online-ID konnte nicht geschrieben werden", e);
        }
    }

    @Override
    public Map<Integer, String> leseLokaleUuids(Collection<Integer> zeilen1Basiert) {
        int spalte = findeSpalte(leseKopfzeile(), I18n.get("ptmonline.meldeliste.header.lokaleuuid"));
        if (spalte < 0) {
            return Map.of();
        }
        return new LokaleUuidSpalte(sheet, doc, spalte).lese(zeilen1Basiert);
    }

    @Override
    public void entferneLokaleUuids() {
        int spalte = findeSpalte(leseKopfzeile(), I18n.get("ptmonline.meldeliste.header.lokaleuuid"));
        uuidSpalteCache = -1;
        if (spalte >= 0) {
            RangeHelper.from(sheet, doc, RangePosition.from(spalte, headerZeile(), spalte, MAX_DATEN_ZEILE)).clearRange();
        }
    }

    @Override
    public void setzeLokaleUuid(int zeile1Basiert, String uuid) throws MeldelisteSchreibException {
        setzeLokaleUuids(Map.of(zeile1Basiert, uuid));
    }

    @Override
    public void setzeLokaleUuids(Map<Integer, String> uuidProZeile) throws MeldelisteSchreibException {
        try {
            uuidSpalteBlock().setze(uuidProZeile);
        } catch (Exception e) {
            throw new MeldelisteSchreibException("Lokale PTM-Online-ID konnte nicht wiederhergestellt werden", e);
        }
    }

    @Override
    public String formelTeamNrAusLokalerUuid(String uuid) throws MeldelisteSchreibException {
        try {
            int spalte = uuidSpalte();
            String uuidStart = Position.from(spalte, 0).getAddressWith$();
            String uuidEnde = Position.from(spalte, MAX_DATEN_ZEILE).getAddressWith$();
            String nummern = "$'" + SheetNamen.meldeliste() + "'.$A$1:$A$" + (MAX_DATEN_ZEILE + 1);
            String uuids = "$'" + SheetNamen.meldeliste() + "'." + uuidStart + ":" + uuidEnde;
            return "IFNA(INDEX(" + nummern + ";MATCH(\"" + uuid + "\";" + uuids + ";0));\"\")";
        } catch (Exception e) {
            throw new MeldelisteSchreibException("Teamnummer-Formel konnte nicht erzeugt werden", e);
        }
    }

    private LokaleUuidSpalte uuidSpalteBlock() throws Exception {
        return new LokaleUuidSpalte(sheet, doc, uuidSpalte());
    }

    /**
     * UUID-Spalte dieser Meldeliste. Ermittelt wird sie einmal je Adapter; danach genügt ein Blick auf die
     * Überschrift, ob sie noch gilt (bei Supermelee verschiebt ein neuer Spieltag die Spalte nach rechts).
     */
    private int uuidSpalte() throws Exception {
        String header = I18n.get("ptmonline.meldeliste.header.lokaleuuid");
        int headerZeile = headerZeile();
        if (uuidSpalteCache >= 0 && header.equals(sicherText(sheetHelper, sheet, uuidSpalteCache, headerZeile).strip())
                && (system != TurnierSystem.SUPERMELEE || uuidSpalteCache == supermeleeUuidZielSpalte(leseKopfzeile()))) {
            return uuidSpalteCache;
        }
        uuidSpalteCache = BlattschutzManager.get().mitEntsperrt(sheet, () -> ermittleUuidSpalte(header));
        return uuidSpalteCache;
    }

    private int headerZeile() {
        return Math.max(0, ersteDatenZeile - 1);
    }

    /**
     * Sichtbare letzte Spalte. Sie wird nicht aus Teamnummern oder Namen hergeleitet. Schreibt Überschrift und
     * Ausblendung – nur innerhalb von {@link BlattschutzManager#mitEntsperrt} aufrufen.
     */
    private int ermittleUuidSpalte(String header) throws GenerateException {
        int headerZeile = headerZeile();
        String[] kopfzeile = leseKopfzeile();
        if (system == TurnierSystem.SUPERMELEE) {
            int zielSpalte = supermeleeUuidZielSpalte(kopfzeile);
            int bisherigeSpalte = findeSpalte(kopfzeile, header);
            if (bisherigeSpalte >= 0 && bisherigeSpalte != zielSpalte) {
                verschiebeUuids(bisherigeSpalte, zielSpalte);
                sheetHelper.setStringValueInCell(StringCellValue.from(sheet, Position.from(bisherigeSpalte, headerZeile), ""));
            }
            if (zielSpalte >= kopfzeile.length || !header.equals(kopfzeile[zielSpalte])) {
                sheetHelper.setStringValueInCell(StringCellValue.from(sheet, Position.from(zielSpalte, headerZeile), header));
            }
            sheetHelper.setColumnProperties(sheet, zielSpalte, ColumnProperties.from().isVisible(false));
            return zielSpalte;
        }
        int vorhandeneSpalte = findeSpalte(kopfzeile, header);
        if (vorhandeneSpalte >= 0) {
            sheetHelper.setColumnProperties(sheet, vorhandeneSpalte, ColumnProperties.from().isVisible(false));
            return vorhandeneSpalte;
        }
        int letzteBenutzteSpalte = aktivSpalte();
        for (int spalte = 0; spalte < kopfzeile.length; spalte++) {
            if (!kopfzeile[spalte].isEmpty()) {
                letzteBenutzteSpalte = Math.max(letzteBenutzteSpalte, spalte);
            }
        }
        int uuidSpalte = letzteBenutzteSpalte + 1;
        sheetHelper.setStringValueInCell(StringCellValue.from(sheet, Position.from(uuidSpalte, headerZeile), header));
        sheetHelper.setColumnProperties(sheet, uuidSpalte, ColumnProperties.from().isVisible(false));
        return uuidSpalte;
    }

    /** Überschriftenzeile bis {@link #MAX_UUID_SPALTE_SCAN} in einem Lesezugriff, getrimmt. */
    private String[] leseKopfzeile() {
        RangeData daten = RangeHelper.from(sheet, doc,
                RangePosition.from(0, headerZeile(), MAX_UUID_SPALTE_SCAN, headerZeile())).getDataFromRange();
        String[] kopfzeile = new String[MAX_UUID_SPALTE_SCAN + 1];
        RowData zeile = daten.isEmpty() ? new RowData() : daten.get(0);
        for (int spalte = 0; spalte < kopfzeile.length; spalte++) {
            kopfzeile[spalte] = spalte < zeile.size()
                    ? StringUtils.strip(StringUtils.defaultString(zeile.get(spalte).getStringVal()))
                    : "";
        }
        return kopfzeile;
    }

    private static int findeSpalte(String[] kopfzeile, String header) {
        for (int spalte = 0; spalte < kopfzeile.length; spalte++) {
            if (header.equals(kopfzeile[spalte])) {
                return spalte;
            }
        }
        return -1;
    }

    /** Kopiert die UUIDs aller Datenzeilen als Block in die neue UUID-Spalte. */
    private void verschiebeUuids(int vonSpalte, int nachSpalte) throws GenerateException {
        RangeData uuids = RangeHelper.from(sheet, doc,
                RangePosition.from(vonSpalte, ersteDatenZeile, vonSpalte, MAX_DATEN_ZEILE)).getDataFromRange();
        RangeHelper.from(sheet, doc, RangePosition.from(nachSpalte, ersteDatenZeile, nachSpalte, MAX_DATEN_ZEILE))
                .setDataInRange(uuids);
    }

    /** UUID direkt nach dem letzten Spieltag; die folgende Spalte bleibt als Abstand zum Infoblock frei. */
    private int supermeleeUuidZielSpalte(String[] kopfzeile) {
        int letzte = letzteSchreibSpalte + 2; // Spielername(n), SP, erster Spieltag
        String spieltag = I18n.get("column.header.spieltag");
        for (int spalte = letzte; spalte < kopfzeile.length; spalte++) {
            if (kopfzeile[spalte].startsWith(spieltag)) {
                letzte = spalte;
            } else {
                break;
            }
        }
        return letzte + 1;
    }

    /** Setzposition (SP bzw. KO-RNG) direkt vor der Aktiv-Spalte; nur für {@link #SYSTEME_MIT_SETZPOSITION}. */
    private int setzpositionSpalte() {
        return letzteSchreibSpalte + 1;
    }

    /**
     * Aktiv-Spalte: bei den meisten Team-Meldelisten zwei Spalten rechts der letzten Spielerdaten-Spalte
     * (dazwischen SP/RNG), bei Trip-Tête direkt daneben – dort gibt es keine Setzpositionsspalte.
     */
    private int aktivSpalte() {
        return letzteSchreibSpalte + (system == TurnierSystem.TRIPTETE ? 1 : 2);
    }

    /** Erste Zeile, in der kein Spieler eingetragen ist (Vorname + Nachname Slot 0 leer). */
    private int naechsteFreieZeile() {
        for (int zeile = ersteDatenZeile; zeile <= MAX_DATEN_ZEILE; zeile++) {
            String vor  = sicherText(sheetHelper, sheet, vornameSpalte(0), zeile).strip();
            String nach = sicherText(sheetHelper, sheet, nachnameSpalte(0), zeile).strip();
            if (vor.isEmpty() && nach.isEmpty()) {
                return zeile;
            }
        }
        return -1;
    }
}
