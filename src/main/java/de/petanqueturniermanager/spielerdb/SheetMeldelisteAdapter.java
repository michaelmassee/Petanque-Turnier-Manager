package de.petanqueturniermanager.spielerdb;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
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
 * der Vereinsname aus {@link SpielerMitVerein#vereinName()}. Die Aktiv-Spalte
 * (Checkin) bleibt leer – übernommene Teams gelten als angemeldet.
 */
final class SheetMeldelisteAdapter implements MeldelisteZiel {

    private static final Logger logger = LogManager.getLogger(SheetMeldelisteAdapter.class);

    private static final int MAX_DATEN_ZEILE = 999;

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
    /** Aktiv-Spalte = Checkin; übernommene Teams bleiben dort leer und zählen als „angemeldet". */
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
            return spieler.size();
        } catch (Exception e) {
            throw new MeldelisteSchreibException("Schreibvorgang fehlgeschlagen", e);
        }
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
