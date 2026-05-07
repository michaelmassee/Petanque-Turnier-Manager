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
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

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

    private static final String PROP_TEAMNAME    = "Meldeliste Teamname";
    private static final String PROP_VEREINSNAME = "Meldeliste Vereinsname";
    private static final String FLAG_AN          = "J";

    /**
     * Wert in der Aktiv-Spalte: „nimmt teil". Die Konvention ist über
     * Schweizer/JGJ/KO/Poule/FormuleX/Kaskade hinweg konstant
     * ({@code AKTIV_WERT_NIMMT_TEIL = 1}); die Aktiv-Spalte selbst sitzt zwei
     * Spalten rechts neben der letzten Spielerdaten-Spalte (dazwischen liegt
     * SP/RNG). Übernommene Teams würden ohne dieses Flag als „inaktiv"
     * gelten und der Update-Workflow käme mit „Es sind keine Teams aktiv".
     */
    private static final int AKTIV_WERT_NIMMT_TEIL = 1;

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
        this.ersterSpielerOffset = teamnameAktiv ? 2 : 1;
        this.spaltenProSpieler = vereinsnameAktiv ? 3 : 2;
        this.letzteSchreibSpalte = ersterSpielerOffset + anzSpieler * spaltenProSpieler - 1;
    }

    /**
     * Baut einen Adapter, falls das Dokument ein bekanntes Turniersystem nutzt
     * und das angegebene Sheet existiert. Liest die Anzeige-Flags
     * {@code Meldeliste Teamname} / {@code Meldeliste Vereinsname} aus den
     * Dokument-Properties; defensiv {@code false} bei nicht gesetzt.
     */
    static Optional<MeldelisteZiel> fuer(WorkingSpreadsheet ws, String sheetName, Formation formation) {
        try {
            DocumentPropertiesHelper props = new DocumentPropertiesHelper(ws);
            TurnierSystem ts = props.getTurnierSystemAusDocument();
            if (ts == null || ts == TurnierSystem.KEIN) {
                return Optional.empty();
            }
            SheetHelper sh = new SheetHelper(ws);
            XSpreadsheet sheet = sh.findByName(sheetName);
            if (sheet == null) {
                return Optional.empty();
            }
            int datenZeile = ermittleErsteDatenZeile(sh, sheet);
            boolean teamname    = FLAG_AN.equalsIgnoreCase(props.getStringProperty(PROP_TEAMNAME, ""));
            boolean vereinsname = FLAG_AN.equalsIgnoreCase(props.getStringProperty(PROP_VEREINSNAME, ""));
            return Optional.of(new SheetMeldelisteAdapter(
                    ws.getWorkingSpreadsheetDocument(), sheet, sh, ts, formation, datenZeile,
                    teamname, vereinsname));
        } catch (Exception e) {
            logger.warn("Adapter-Erkennung fehlgeschlagen", e);
            return Optional.empty();
        }
    }

    private static int ermittleErsteDatenZeile(SheetHelper sh, XSpreadsheet sheet) {
        for (int zeile = 0; zeile <= HEADER_ZEILE_MAX_SCAN; zeile++) {
            String inhalt = sicherText(sh, sheet, SPALTE_NR, zeile).strip();
            if (inhalt.matches("\\d+")) {
                return zeile;
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
            schreibeNeueTeamNr(zeile);

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

            // Aktiv-Spalte (= letzteDatenSpalte + 2) auf „nimmt teil" setzen,
            // sonst kommt „Meldeliste Aktualisieren" mit der Frage „Es sind
            // keine Teams aktiv. Sollen alle aktiviert werden?".
            int aktivSpalte = letzteSchreibSpalte + 2;
            sheetHelper.setNumberValueInCell(NumberCellValue
                    .from(sheet, Position.from(aktivSpalte, zeile)).setValue(AKTIV_WERT_NIMMT_TEIL));
            return spieler.size();
        } catch (Exception e) {
            throw new MeldelisteSchreibException("Schreibvorgang fehlgeschlagen", e);
        }
    }

    private void schreibeNeueTeamNr(int zeile) {
        int neueNr = letzteVergebeneNr() + 1;
        sheetHelper.setStringValueInCell(StringCellValue
                .from(sheet, Position.from(SPALTE_NR, zeile), String.valueOf(neueNr)));
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

    private int letzteVergebeneNr() {
        int max = 0;
        for (int zeile = ersteDatenZeile; zeile <= MAX_DATEN_ZEILE; zeile++) {
            String nrText = sicherText(sheetHelper, sheet, SPALTE_NR, zeile).strip();
            if (nrText.isEmpty()) {
                break;
            }
            try {
                int nr = Integer.parseInt(nrText);
                if (nr > max) {
                    max = nr;
                }
            } catch (NumberFormatException e) {
                // ignorieren — Header-Reste o.ä.
            }
        }
        return max;
    }
}
