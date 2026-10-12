package de.petanqueturniermanager.spielerdb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
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
import de.petanqueturniermanager.basesheet.meldeliste.NrUuidSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.NrZellFormel;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
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
 * der Vereinsname aus {@link SpielerMitVerein#vereinName()}. Bei der Spieler-DB-Übernahme
 * ({@link #schreibeBlock}) bleibt die Aktiv-Spalte (Checkin) leer – übernommene Teams gelten
 * als angemeldet.
 */
final class SheetMeldelisteAdapter implements MeldelisteZiel {

    private static final Logger logger = LogManager.getLogger(SheetMeldelisteAdapter.class);

    private static final int SPALTE_NR = 0;
    private static final int MAX_DATEN_ZEILE = 999;
    /** So viele Spalten hinter dem ersten Spieltag werden bei Supermelee nach weiteren Spieltagen durchsucht. */
    private static final int MAX_SPIELTAGE = 100;

    /**
     * Wert in der Aktiv-Spalte: „nimmt teil". Die Konvention ist über
     * Schweizer/JGJ/KO/Poule/FormuleX/Kaskade hinweg konstant
     * ({@code AKTIV_WERT_NIMMT_TEIL = 1}). Gesetzt nur für {@link NeueMeldungTeilnahme#AKTIV}.
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
    /** Letzte Spielerdaten-Spalte (inklusive). */
    private final int letzteSchreibSpalte;
    /**
     * Aktiv-Spalte (Checkin), Lage je Meldelisten-Typ aus {@link MeldelisteLayout#aktivSpaltenAbstand()}; bei
     * Supermelee die des aktiven Spieltags.
     */
    private final int aktivSpalte;

    private SheetMeldelisteAdapter(XSpreadsheetDocument doc, XSpreadsheet sheet,
            SheetHelper sheetHelper, TurnierSystem system, MeldelisteLayout layout) {
        this.doc = doc;
        this.sheet = sheet;
        this.sheetHelper = sheetHelper;
        this.system = system;
        this.formation = layout.formation();
        this.ersteDatenZeile = layout.ersteDatenZeile();
        this.teamnameAktiv = layout.teamnameAktiv();
        this.vereinsnameAktiv = layout.vereinsnameAktiv();
        this.anzSpieler = Math.max(1, formation.getAnzSpieler());
        this.ersterSpielerOffset = teamnameAktiv ? 2 : 1;
        this.spaltenProSpieler = vereinsnameAktiv ? 3 : 2;
        this.letzteSchreibSpalte = ersterSpielerOffset + anzSpieler * spaltenProSpieler - 1;
        this.aktivSpalte = letzteSchreibSpalte + layout.aktivSpaltenAbstand();
    }

    /**
     * Baut einen Adapter aus dem Layout, das der Aufrufer aus dem system-spezifischen
     * KonfigurationSheet gelesen hat. Vermeidet Property-Lookups direkt im Adapter.
     */
    static Optional<MeldelisteZiel> fuer(WorkingSpreadsheet ws, String sheetName, TurnierSystem ts,
            MeldelisteLayout layout) {
        try {
            if (ts == null || ts == TurnierSystem.KEIN) {
                return Optional.empty();
            }
            SheetHelper sh = new SheetHelper(ws);
            XSpreadsheet sheet = sh.findByName(sheetName);
            if (sheet == null) {
                return Optional.empty();
            }
            return Optional.of(new SheetMeldelisteAdapter(ws.getWorkingSpreadsheetDocument(), sheet, sh, ts, layout));
        } catch (Exception e) {
            logger.warn("Adapter-Erkennung fehlgeschlagen", e);
            return Optional.empty();
        }
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
        return schreibeBlockUndLiefereZeile(spieler, NeueMeldungTeilnahme.INAKTIV) < 0 ? 0 : spieler.size();
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

            // Aktiv-Spalte nur bei AKTIV auf „nimmt teil"
            // setzen; INAKTIV lässt sie leer (noch nicht eingecheckt).
            if (teilnahme == NeueMeldungTeilnahme.AKTIV) {
                BlattschutzManager.get().schreibeEntsperrt(sheet, () -> sheetHelper.setNumberValueInCell(NumberCellValue
                        .from(sheet, Position.from(aktivSpalte, zeile)).setValue(AKTIV_WERT_NIMMT_TEIL)));
            }
            return zeile + 1;
        } catch (Exception e) {
            throw new MeldelisteSchreibException("Schreibvorgang fehlgeschlagen", e);
        }
    }

    @Override
    public boolean istSpielerpool() {
        return system == TurnierSystem.SUPERMELEE;
    }

    /** Supermêlée: Zeilen mit einem Eintrag in der Spalte des aktiven Spieltags, in einem Lesezugriff. */
    @Override
    public Optional<Set<Integer>> zeilenDesAktivenSpieltags() {
        if (system != TurnierSystem.SUPERMELEE) {
            return Optional.empty();
        }
        RangeData daten = RangeHelper.from(sheet, doc,
                RangePosition.from(aktivSpalte, ersteDatenZeile, aktivSpalte, MAX_DATEN_ZEILE)).getDataFromRange();
        Set<Integer> zeilen = new HashSet<>();
        for (int index = 0; index < daten.size(); index++) {
            RowData zeile = daten.get(index);
            if (!zeile.isEmpty() && zeile.get(0).getIntVal(0) > 0) {
                zeilen.add(ersteDatenZeile + index + 1);
            }
        }
        return Optional.of(zeilen);
    }

    @Override
    public int getAktivWertAusZeile(int zeile1Basiert) {
        if (zeile1Basiert <= 0) {
            return -1;
        }
        return sheetHelper.getIntFromCell(sheet, Position.from(aktivSpalte, zeile1Basiert - 1));
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

    /** Leert die ganze Zeile samt Nr-Zelle (und damit UUID) – wie „Meldeliste aktualisieren“ eine Zeile ohne Namen. */
    @Override
    public void entferneMeldung(int zeile1Basiert) throws MeldelisteSchreibException {
        if (zeile1Basiert <= 0) {
            throw new MeldelisteSchreibException("Ungültige Meldelistenzeile");
        }
        try {
            int zeile = zeile1Basiert - 1;
            RangePosition bereich = RangePosition.from(SPALTE_NR, zeile, letzteMeldungsSpalte(), zeile);
            BlattschutzManager.get().schreibeEntsperrt(sheet, () -> RangeHelper.from(sheet, doc, bereich).clearRange());
        } catch (Exception e) {
            throw new MeldelisteSchreibException("Meldung konnte nicht entfernt werden", e);
        }
    }

    @Override
    public void markiereAlsAbgemeldet(int zeile1Basiert) throws MeldelisteSchreibException {
        if (zeile1Basiert <= 0) {
            throw new MeldelisteSchreibException("Ungültige Meldelistenzeile");
        }
        try {
            BlattschutzManager.get().schreibeEntsperrt(sheet, () -> sheetHelper.setNumberValueInCell(
                    NumberCellValue.from(sheet, Position.from(aktivSpalte, zeile1Basiert - 1))
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
            Position aktiv = Position.from(aktivSpalte, zeile1Basiert - 1);
            if (sheetHelper.getIntFromCell(sheet, aktiv) == AKTIV_WERT_ABGEMELDET) {
                BlattschutzManager.get().schreibeEntsperrt(sheet, () -> sheetHelper.clearValInCell(sheet, aktiv));
            }
        } catch (Exception e) {
            throw new MeldelisteSchreibException("Abmeldung konnte nicht aufgehoben werden", e);
        }
    }

    @Override
    public void ersetzeSpielerNamen(int zeile1Basiert, List<SpielerMitVerein> spieler) throws MeldelisteSchreibException {
        if (zeile1Basiert <= 0 || spieler.isEmpty() || spieler.size() > anzSpieler) {
            throw new MeldelisteSchreibException("Ungültige Namensänderung für Zeile " + zeile1Basiert);
        }
        try {
            int zeile = zeile1Basiert - 1;
            RangePosition bereich = RangePosition.from(vornameSpalte(0), zeile, letzteSchreibSpalte, zeile);
            RangeData bisher = RangeHelper.from(sheet, doc, bereich).getDataFromRange();
            RowData alt = bisher.isEmpty() ? new RowData() : bisher.get(0);
            RowData neu = new RowData();
            for (int slot = 0; slot < anzSpieler; slot++) {
                boolean belegt = slot < spieler.size();
                neu.newString(belegt ? spieler.get(slot).vorname() : "");
                neu.newString(belegt ? spieler.get(slot).nachname() : "");
                if (vereinsnameAktiv) {
                    int vereinIndex = slot * spaltenProSpieler + 2;
                    String verein = vereinIndex < alt.size() ? StringUtils.defaultString(alt.get(vereinIndex).getStringVal()) : "";
                    neu.newString(belegt ? verein : "");
                }
            }
            RangeData daten = new RangeData();
            daten.add(neu);
            RangeHelper.from(sheet, doc, bereich).setDataInRange(daten);
        } catch (Exception e) {
            throw new MeldelisteSchreibException("Namen konnten nicht geändert werden", e);
        }
    }

    @Override
    public void stelleAktivWertWiederHer(int zeile1Basiert, int aktivWert) throws MeldelisteSchreibException {
        if (zeile1Basiert <= 0) {
            throw new MeldelisteSchreibException("Ungültige Meldelistenzeile");
        }
        try {
            Position aktiv = Position.from(aktivSpalte, zeile1Basiert - 1);
            BlattschutzManager.get().schreibeEntsperrt(sheet, () -> {
                if (aktivWert > 0) {
                    sheetHelper.setNumberValueInCell(NumberCellValue.from(sheet, aktiv).setValue(aktivWert));
                } else {
                    sheetHelper.clearValInCell(sheet, aktiv);
                }
            });
        } catch (Exception e) {
            throw new MeldelisteSchreibException("Teilnahme konnte nicht wiederhergestellt werden", e);
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
        pruefeZeilen(zeilen1Basiert);
        try {
            return nrUuidSpalte().getOderErzeuge(zeilen1Basiert);
        } catch (RuntimeException e) {
            throw new MeldelisteSchreibException("Lokale PTM-Online-ID konnte nicht geschrieben werden", e);
        }
    }

    @Override
    public Map<Integer, String> leseLokaleUuids(Collection<Integer> zeilen1Basiert) {
        return nrUuidSpalte().lese(zeilen1Basiert);
    }

    @Override
    public void setzeLokaleUuid(int zeile1Basiert, String uuid) throws MeldelisteSchreibException {
        setzeLokaleUuids(Map.of(zeile1Basiert, uuid));
    }

    @Override
    public void setzeLokaleUuids(Map<Integer, String> uuidProZeile) throws MeldelisteSchreibException {
        pruefeZeilen(uuidProZeile.keySet());
        try {
            nrUuidSpalte().setze(uuidProZeile);
        } catch (RuntimeException e) {
            throw new MeldelisteSchreibException("Lokale PTM-Online-ID konnte nicht geschrieben werden", e);
        }
    }

    @Override
    public String formelTeamNrAusLokalerUuid(String uuid) {
        return NrUuidSpalte.formelNrAusUuid("$'" + SheetNamen.meldeliste() + "'.$A$1:$A$" + (MAX_DATEN_ZEILE + 1), uuid);
    }

    @Override
    public Map<String, String> leseOnlineIdsProUuid() {
        return nrUuidSpalte().onlineIdsProUuid();
    }

    @Override
    public Set<String> setzeOnlineIds(Map<String, Optional<String>> onlineIdProUuid)
            throws MeldelisteSchreibException {
        try {
            return nrUuidSpalte().setzeOnlineIds(onlineIdProUuid);
        } catch (RuntimeException e) {
            throw new MeldelisteSchreibException("PTM-Online-ID konnte nicht geschrieben werden", e);
        }
    }

    private NrUuidSpalte nrUuidSpalte() {
        return new NrUuidSpalte(sheetHelper, sheet, doc, SPALTE_NR, ersteDatenZeile,
                NrZellFormel.Art.TEAM);
    }

    private static void pruefeZeilen(Collection<Integer> zeilen1Basiert) throws MeldelisteSchreibException {
        if (zeilen1Basiert.stream().anyMatch(zeile -> zeile <= 0)) {
            throw new MeldelisteSchreibException("Ungültige Meldelistenzeile");
        }
    }

    /**
     * Letzte Spalte, die zu einer Meldung gehört: die Aktiv-Spalte, bei Supermelee die Spalte des letzten
     * Spieltags (je Spieltag eine Spalte hinter SP).
     */
    private int letzteMeldungsSpalte() {
        if (system != TurnierSystem.SUPERMELEE) {
            return aktivSpalte;
        }
        int ersteSpieltagSpalte = letzteSchreibSpalte + 2;
        RangeData daten = RangeHelper.from(sheet, doc, RangePosition.from(ersteSpieltagSpalte, headerZeile(),
                ersteSpieltagSpalte + MAX_SPIELTAGE, headerZeile())).getDataFromRange();
        RowData kopf = daten.isEmpty() ? new RowData() : daten.get(0);
        String spieltag = I18n.get("column.header.spieltag");
        int letzte = ersteSpieltagSpalte;
        for (int i = 1; i < kopf.size()
                && StringUtils.defaultString(kopf.get(i).getStringVal()).strip().startsWith(spieltag); i++) {
            letzte = ersteSpieltagSpalte + i;
        }
        return Math.max(letzte, aktivSpalte);
    }

    private int headerZeile() {
        return Math.max(0, ersteDatenZeile - 1);
    }

    /** Setzposition (SP bzw. KO-RNG) direkt vor der Aktiv-Spalte; nur für {@link #SYSTEME_MIT_SETZPOSITION}. */
    private int setzpositionSpalte() {
        return letzteSchreibSpalte + 1;
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
