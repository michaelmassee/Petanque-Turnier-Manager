/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.poule.vorrunde;

import com.sun.star.awt.FontWeight;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeHelper;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.ColorHelper;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellstyle.SpielrundeHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.SpielrundeHintergrundFarbeUnGeradeStyle;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.print.PrintArea;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.poule.konfiguration.PouleKonfigurationSheet;
import de.petanqueturniermanager.poule.meldeliste.PouleMeldeListeSheetUpdate;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Abstrakte Basisklasse für alle Poule-Vorrunden-Sheets.
 * Enthält die gemeinsame Spaltenstruktur, Schreibmethoden und Formatierungslogik.
 */
public abstract class AbstractPouleVorrundeSheet extends SheetRunner implements ISheet {

    public static final int ERSTE_DATEN_ZEILE = 2;

    protected static final int SPALTE_BAHN = 0;
    protected static final int SPALTE_BESCHR = 1;
    protected static final int SPALTE_POULE_NR = 2;
    protected static final int SPALTE_TEAM_A_NR = 3;
    protected static final int SPALTE_TEAM_A_NAME = 4;
    protected static final int SPALTE_TEAM_B_NR = 5;
    protected static final int SPALTE_TEAM_B_NAME = 6;
    public static final int SPALTE_ERG_A = 7;
    public static final int SPALTE_ERG_B = 8;
    protected static final int SPALTE_FEHLER = 9;
    protected static final int LETZTE_SPALTE = SPALTE_FEHLER;

    protected static final int VIERER_POULE_DATEN_ZEILEN = 5;
    protected static final int DREIER_POULE_DATEN_ZEILEN = 3;
    protected static final int SPACER_ZEILEN = 1;

    /** Gesamtzeilenanzahl pro 4er-Poule-Block (Datenzeilen + Trennzeile). */
    public static final int VIERER_POULE_ZEILEN = VIERER_POULE_DATEN_ZEILEN + SPACER_ZEILEN;
    /** Gesamtzeilenanzahl pro 3er-Poule-Block (Datenzeilen + Trennzeile). */
    public static final int DREIER_POULE_ZEILEN = DREIER_POULE_DATEN_ZEILEN + SPACER_ZEILEN;

    private static final int BAHN_SPALTE_BREITE = 900;
    private static final int BESCHR_SPALTE_BREITE = 3000;
    private static final int POULE_NR_SPALTE_BREITE = 2000;
    private static final int TEAM_NR_SPALTE_BREITE = 900;
    private static final int TEAM_NAME_SPALTE_BREITE = 4000;
    private static final int ERG_SPALTE_BREITE = 800;
    private static final int FEHLER_SPALTE_BREITE = 1500;

    protected static final String SHEET_COLOR = "4a8faa";

    private final PouleKonfigurationSheet konfigurationSheet;
    protected final PouleMeldeListeSheetUpdate meldeliste;

    @Override
    public abstract XSpreadsheet getXSpreadSheet() throws GenerateException;

    @Override
    public abstract TurnierSheet getTurnierSheet() throws GenerateException;

    protected AbstractPouleVorrundeSheet(WorkingSpreadsheet workingSpreadsheet, String logPrefix) {
        super(workingSpreadsheet, TurnierSystem.POULE, logPrefix);
        konfigurationSheet = new PouleKonfigurationSheet(workingSpreadsheet);
        meldeliste = new PouleMeldeListeSheetUpdate(workingSpreadsheet);
    }

    @Override
    protected PouleKonfigurationSheet getKonfigurationSheet() {
        return konfigurationSheet;
    }

    // ---------------------------------------------------------------
    // Header
    // ---------------------------------------------------------------

    protected void headerSchreiben(XSpreadsheet xSheet) throws GenerateException {
        var headerFarbe = konfigurationSheet.getMeldeListeHeaderFarbe();

        getSheetHelper().setStringValueInCell(
                schreibeHeaderZelle(xSheet, SPALTE_BAHN, 0,
                        I18n.get("poule.vorrunde.header.bahn"), headerFarbe)
                        .setEndPosMergeZeilePlus(1));

        getSheetHelper().setStringValueInCell(
                schreibeHeaderZelle(xSheet, SPALTE_BESCHR, 0,
                        I18n.get("poule.vorrunde.header.beschreibung"), headerFarbe)
                        .setEndPosMergeZeilePlus(1));

        getSheetHelper().setStringValueInCell(
                schreibeHeaderZelle(xSheet, SPALTE_POULE_NR, 0,
                        I18n.get("poule.vorrunde.header.poule"), headerFarbe)
                        .setEndPosMergeZeilePlus(1));

        getSheetHelper().setStringValueInCell(
                schreibeHeaderZelle(xSheet, SPALTE_TEAM_A_NR, 0,
                        I18n.get("poule.vorrunde.header.team.a"), headerFarbe)
                        .setEndPosMergeSpalte(SPALTE_TEAM_A_NAME));

        getSheetHelper().setStringValueInCell(
                schreibeHeaderZelle(xSheet, SPALTE_TEAM_B_NR, 0,
                        I18n.get("poule.vorrunde.header.team.b"), headerFarbe)
                        .setEndPosMergeSpalte(SPALTE_TEAM_B_NAME));

        getSheetHelper().setStringValueInCell(
                schreibeHeaderZelle(xSheet, SPALTE_ERG_A, 0,
                        I18n.get("poule.vorrunde.header.ergebnis"), headerFarbe)
                        .setEndPosMergeSpalte(SPALTE_ERG_B));

        getSheetHelper().setStringValueInCell(
                schreibeHeaderZelle(xSheet, SPALTE_TEAM_A_NR, 1,
                        I18n.get("column.header.nr"), headerFarbe));

        getSheetHelper().setStringValueInCell(
                schreibeHeaderZelle(xSheet, SPALTE_TEAM_A_NAME, 1,
                        I18n.get("column.header.name"), headerFarbe));

        getSheetHelper().setStringValueInCell(
                schreibeHeaderZelle(xSheet, SPALTE_TEAM_B_NR, 1,
                        I18n.get("column.header.nr"), headerFarbe));

        getSheetHelper().setStringValueInCell(
                schreibeHeaderZelle(xSheet, SPALTE_TEAM_B_NAME, 1,
                        I18n.get("column.header.name"), headerFarbe));

        getSheetHelper().setStringValueInCell(
                schreibeHeaderZelle(xSheet, SPALTE_ERG_A, 1, "A", headerFarbe));

        getSheetHelper().setStringValueInCell(
                schreibeHeaderZelle(xSheet, SPALTE_ERG_B, 1, "B", headerFarbe));
    }

    private StringCellValue schreibeHeaderZelle(XSpreadsheet xSheet, int spalte, int zeile,
            String text, int farbe) {
        return StringCellValue.from(xSheet, Position.from(spalte, zeile), text)
                .setCellBackColor(farbe)
                .setBorder(BorderFactory.from().allThin().toBorder())
                .setCharWeight(FontWeight.BOLD)
                .setHoriJustify(CellHoriJustify.CENTER)
                .setVertJustify(CellVertJustify2.CENTER);
    }

    // ---------------------------------------------------------------
    // Spaltenbreiten
    // ---------------------------------------------------------------

    protected void spaltenBreitenSetzen(XSpreadsheet xSheet) throws GenerateException {
        getSheetHelper().setColumnProperties(xSheet, SPALTE_BAHN,
                ColumnProperties.from().setWidth(BAHN_SPALTE_BREITE).setHoriJustify(CellHoriJustify.CENTER));
        getSheetHelper().setColumnProperties(xSheet, SPALTE_BESCHR,
                ColumnProperties.from().setWidth(BESCHR_SPALTE_BREITE));
        getSheetHelper().setColumnProperties(xSheet, SPALTE_POULE_NR,
                ColumnProperties.from().setWidth(POULE_NR_SPALTE_BREITE).setHoriJustify(CellHoriJustify.CENTER));
        getSheetHelper().setColumnProperties(xSheet, SPALTE_TEAM_A_NR,
                ColumnProperties.from().setWidth(TEAM_NR_SPALTE_BREITE).setHoriJustify(CellHoriJustify.CENTER));
        getSheetHelper().setColumnProperties(xSheet, SPALTE_TEAM_A_NAME,
                ColumnProperties.from().setWidth(TEAM_NAME_SPALTE_BREITE));
        getSheetHelper().setColumnProperties(xSheet, SPALTE_TEAM_B_NR,
                ColumnProperties.from().setWidth(TEAM_NR_SPALTE_BREITE).setHoriJustify(CellHoriJustify.CENTER));
        getSheetHelper().setColumnProperties(xSheet, SPALTE_TEAM_B_NAME,
                ColumnProperties.from().setWidth(TEAM_NAME_SPALTE_BREITE));
        getSheetHelper().setColumnProperties(xSheet, SPALTE_ERG_A,
                ColumnProperties.from().setWidth(ERG_SPALTE_BREITE).setHoriJustify(CellHoriJustify.CENTER));
        getSheetHelper().setColumnProperties(xSheet, SPALTE_ERG_B,
                ColumnProperties.from().setWidth(ERG_SPALTE_BREITE).setHoriJustify(CellHoriJustify.CENTER));
        getSheetHelper().setColumnProperties(xSheet, SPALTE_FEHLER,
                ColumnProperties.from().setWidth(FEHLER_SPALTE_BREITE).setHoriJustify(CellHoriJustify.CENTER));
    }

    // ---------------------------------------------------------------
    // Poule-Blöcke schreiben
    // ---------------------------------------------------------------

    protected int schreibeViererPoule(XSpreadsheet xSheet, PouleSeedingService.Poule poule,
            int basisZeile, int spielbahnZaehler) throws GenerateException {

        var teams = poule.teams();

        getSheetHelper().setStringValueInCell(
                StringCellValue.from(xSheet, Position.from(SPALTE_POULE_NR, basisZeile),
                        I18n.get("poule.vorrunde.poule.nr", poule.pouleNr()))
                        .setEndPosMergeZeilePlus(VIERER_POULE_DATEN_ZEILEN - 1)
                        .setHoriJustify(CellHoriJustify.CENTER)
                        .setVertJustify(CellVertJustify2.CENTER)
                        .setCharWeight(FontWeight.BOLD)
                        .setBorder(BorderFactory.from().allThin().toBorder()));

        int rSpielA = basisZeile;
        int rSpielB = basisZeile + 1;
        int rSieger = basisZeile + 2;
        int rVerlierer = basisZeile + 3;
        int rBarrage = basisZeile + 4;

        spielbahnZaehler = schreibeSpielZeileR1(xSheet, rSpielA,
                I18n.get("poule.vorrunde.spiel.a"),
                teams.get(0).getNr(), teams.get(1).getNr(), spielbahnZaehler);

        spielbahnZaehler = schreibeSpielZeileR1(xSheet, rSpielB,
                I18n.get("poule.vorrunde.spiel.b"),
                teams.get(2).getNr(), teams.get(3).getNr(), spielbahnZaehler);

        spielbahnZaehler = schreibeSpielZeileFormula(xSheet, rSieger,
                I18n.get("poule.vorrunde.siegerspiel"),
                siegerFormel(rSpielA), siegerFormel(rSpielB), spielbahnZaehler);

        spielbahnZaehler = schreibeSpielZeileFormula(xSheet, rVerlierer,
                I18n.get("poule.vorrunde.verliererspiel"),
                verliererFormel(rSpielA), verliererFormel(rSpielB), spielbahnZaehler);

        spielbahnZaehler = schreibeSpielZeileFormula(xSheet, rBarrage,
                I18n.get("poule.vorrunde.barrage"),
                verliererFormel(rSieger), siegerFormel(rVerlierer), spielbahnZaehler);

        return spielbahnZaehler;
    }

    protected int schreibeDreierPoule(XSpreadsheet xSheet, PouleSeedingService.Poule poule,
            int basisZeile, int spielbahnZaehler) throws GenerateException {

        var teams = poule.teams();

        getSheetHelper().setStringValueInCell(
                StringCellValue.from(xSheet, Position.from(SPALTE_POULE_NR, basisZeile),
                        I18n.get("poule.vorrunde.poule.nr", poule.pouleNr()))
                        .setEndPosMergeZeilePlus(DREIER_POULE_DATEN_ZEILEN - 1)
                        .setHoriJustify(CellHoriJustify.CENTER)
                        .setVertJustify(CellVertJustify2.CENTER)
                        .setCharWeight(FontWeight.BOLD)
                        .setBorder(BorderFactory.from().allThin().toBorder()));

        spielbahnZaehler = schreibeSpielZeileR1(xSheet, basisZeile,
                I18n.get("poule.vorrunde.spiel.1"),
                teams.get(0).getNr(), teams.get(1).getNr(), spielbahnZaehler);

        spielbahnZaehler = schreibeSpielZeileR1(xSheet, basisZeile + 1,
                I18n.get("poule.vorrunde.spiel.2"),
                teams.get(0).getNr(), teams.get(2).getNr(), spielbahnZaehler);

        spielbahnZaehler = schreibeSpielZeileR1(xSheet, basisZeile + 2,
                I18n.get("poule.vorrunde.spiel.3"),
                teams.get(1).getNr(), teams.get(2).getNr(), spielbahnZaehler);

        return spielbahnZaehler;
    }

    private int schreibeSpielZeileR1(XSpreadsheet xSheet, int zeile, String beschreibung,
            int teamANr, int teamBNr, int spielbahnZaehler) throws GenerateException {

        getSheetHelper().setNumberValueInCell(
                NumberCellValue.from(xSheet, Position.from(SPALTE_BAHN, zeile)).setValue(spielbahnZaehler));

        getSheetHelper().setStringValueInCell(
                StringCellValue.from(xSheet, Position.from(SPALTE_BESCHR, zeile), beschreibung));

        getSheetHelper().setNumberValueInCell(
                NumberCellValue.from(xSheet, Position.from(SPALTE_TEAM_A_NR, zeile)).setValue(teamANr));

        getSheetHelper().setFormulaInCell(StringCellValue.from(xSheet,
                Position.from(SPALTE_TEAM_A_NAME, zeile),
                vlookupDirekt(Position.from(SPALTE_TEAM_A_NR, zeile).getAddress())));

        getSheetHelper().setNumberValueInCell(
                NumberCellValue.from(xSheet, Position.from(SPALTE_TEAM_B_NR, zeile)).setValue(teamBNr));

        getSheetHelper().setFormulaInCell(StringCellValue.from(xSheet,
                Position.from(SPALTE_TEAM_B_NAME, zeile),
                vlookupDirekt(Position.from(SPALTE_TEAM_B_NR, zeile).getAddress())));

        schreibeFehlerFormel(xSheet, zeile, false);

        return spielbahnZaehler + 1;
    }

    private int schreibeSpielZeileFormula(XSpreadsheet xSheet, int zeile, String beschreibung,
            String teamAFormel, String teamBFormel, int spielbahnZaehler) throws GenerateException {

        getSheetHelper().setNumberValueInCell(
                NumberCellValue.from(xSheet, Position.from(SPALTE_BAHN, zeile)).setValue(spielbahnZaehler));

        getSheetHelper().setStringValueInCell(
                StringCellValue.from(xSheet, Position.from(SPALTE_BESCHR, zeile), beschreibung));

        getSheetHelper().setFormulaInCell(
                StringCellValue.from(xSheet, Position.from(SPALTE_TEAM_A_NR, zeile), teamAFormel));

        getSheetHelper().setFormulaInCell(StringCellValue.from(xSheet,
                Position.from(SPALTE_TEAM_A_NAME, zeile),
                vlookupMitGuard(Position.from(SPALTE_TEAM_A_NR, zeile).getAddress())));

        getSheetHelper().setFormulaInCell(
                StringCellValue.from(xSheet, Position.from(SPALTE_TEAM_B_NR, zeile), teamBFormel));

        getSheetHelper().setFormulaInCell(StringCellValue.from(xSheet,
                Position.from(SPALTE_TEAM_B_NAME, zeile),
                vlookupMitGuard(Position.from(SPALTE_TEAM_B_NR, zeile).getAddress())));

        schreibeFehlerFormel(xSheet, zeile, true);

        return spielbahnZaehler + 1;
    }

    // ---------------------------------------------------------------
    // Formel-Hilfsmethoden
    // ---------------------------------------------------------------

    /**
     * Formel: Teamnummer des Siegers in {@code refZeile} (höhere Punktzahl).
     */
    protected String siegerFormel(int refZeile) {
        var ergA = Position.from(SPALTE_ERG_A, refZeile).getAddress();
        var ergB = Position.from(SPALTE_ERG_B, refZeile).getAddress();
        var teamA = Position.from(SPALTE_TEAM_A_NR, refZeile).getAddress();
        var teamB = Position.from(SPALTE_TEAM_B_NR, refZeile).getAddress();
        return "IF(AND(ISNUMBER(" + ergA + ");ISNUMBER(" + ergB + "));"
                + "IF(" + ergA + ">" + ergB + ";" + teamA + ";" + teamB + ");\"\")";
    }

    /**
     * Formel: Teamnummer des Verlierers in {@code refZeile} (niedrigere Punktzahl).
     */
    protected String verliererFormel(int refZeile) {
        var ergA = Position.from(SPALTE_ERG_A, refZeile).getAddress();
        var ergB = Position.from(SPALTE_ERG_B, refZeile).getAddress();
        var teamA = Position.from(SPALTE_TEAM_A_NR, refZeile).getAddress();
        var teamB = Position.from(SPALTE_TEAM_B_NR, refZeile).getAddress();
        return "IF(AND(ISNUMBER(" + ergA + ");ISNUMBER(" + ergB + "));"
                + "IF(" + ergA + "<" + ergB + ";" + teamA + ";" + teamB + ");\"\")";
    }

    /**
     * Direkte VLOOKUP-Formel (R1: Team-Nr ist garantiert eine Zahl).
     */
    private String vlookupDirekt(String nrAdresse) {
        return "VLOOKUP(" + nrAdresse + ";$'" + SheetNamen.meldeliste() + "'.$A$4:$B$999;2;0)";
    }

    /**
     * VLOOKUP mit ISNUMBER-Guard (R2/R3: Team-Nr ist eine Formel, die "" liefern kann).
     */
    private String vlookupMitGuard(String nrAdresse) {
        return "IF(ISNUMBER(" + nrAdresse + ");VLOOKUP(" + nrAdresse
                + ";$'" + SheetNamen.meldeliste() + "'.$A$4:$B$999;2;0);\"\")";
    }

    /**
     * Schreibt die Validierungs-Fehlerformel in die FEHLER-Spalte.
     *
     * @param mitTeamGuard true für R2/R3-Zeilen, bei denen die Team-Nr eine Formel ist
     */
    private void schreibeFehlerFormel(XSpreadsheet xSheet, int zeile, boolean mitTeamGuard)
            throws GenerateException {
        var ergA = Position.from(SPALTE_ERG_A, zeile).getAddress();
        var ergB = Position.from(SPALTE_ERG_B, zeile).getAddress();
        var leer = "AND(ISBLANK(" + ergA + ");ISBLANK(" + ergB + "))";
        var gueltig = "AND(" + ergA + "<14;" + ergB + "<14;" + ergA + ">-1;" + ergB + ">-1;" + ergA + "<>" + ergB + ")";
        var fehlerText = "\"" + I18n.get("poule.vorrunde.fehler.formel") + "\"";

        String formel;
        if (mitTeamGuard) {
            var teamA = Position.from(SPALTE_TEAM_A_NR, zeile).getAddress();
            var teamB = Position.from(SPALTE_TEAM_B_NR, zeile).getAddress();
            formel = "IF(OR(NOT(ISNUMBER(" + teamA + "));NOT(ISNUMBER(" + teamB + ")));\"\";IF("
                    + leer + ";\"\";IF(" + gueltig + ";\"\";" + fehlerText + ")))";
        } else {
            formel = "IF(" + leer + ";\"\";IF(" + gueltig + ";\"\";" + fehlerText + "))";
        }

        getSheetHelper().setFormulaInCell(
                StringCellValue.from(xSheet, Position.from(SPALTE_FEHLER, zeile), formel)
                        .setCharColor(ColorHelper.CHAR_COLOR_RED)
                        .setCharWeight(FontWeight.BOLD)
                        .setHoriJustify(CellHoriJustify.CENTER));
    }

    // ---------------------------------------------------------------
    // Formatierung
    // ---------------------------------------------------------------

    protected void formatierungDurchfuehren(XSpreadsheet xSheet, int letzteDatenZeile) throws GenerateException {
        var datenRange = RangePosition.from(SPALTE_BAHN, ERSTE_DATEN_ZEILE, LETZTE_SPALTE, letzteDatenZeile);

        getSheetHelper().setPropertiesInRange(xSheet, datenRange,
                CellProperties.from().setBorder(BorderFactory.from().allThin().toBorder()));

        int geradeColor = konfigurationSheet.getMeldeListeHintergrundFarbeGerade();
        int unGeradeColor = konfigurationSheet.getMeldeListeHintergrundFarbeUnGerade();
        SheetHelper.faerbeZeilenAbwechselnd(this, datenRange, geradeColor, unGeradeColor);

        var ergebnissRange = RangePosition.from(SPALTE_ERG_A, ERSTE_DATEN_ZEILE, SPALTE_ERG_B, letzteDatenZeile);
        var spielrundeHelper = new SpielrundeHelper(this,
                new SpielrundeHintergrundFarbeGeradeStyle(geradeColor),
                new SpielrundeHintergrundFarbeUnGeradeStyle(unGeradeColor));
        spielrundeHelper.formatiereErgebnissRange(this, ergebnissRange, SPALTE_ERG_A);
    }

    protected void printBereichSetzen(XSpreadsheet xSheet, int letzteDatenZeile) throws GenerateException {
        PrintArea.from(xSheet, getWorkingSpreadsheet())
                .setPrintArea(RangePosition.from(
                        Position.from(SPALTE_BAHN, 0),
                        Position.from(LETZTE_SPALTE, letzteDatenZeile)));
    }
}
