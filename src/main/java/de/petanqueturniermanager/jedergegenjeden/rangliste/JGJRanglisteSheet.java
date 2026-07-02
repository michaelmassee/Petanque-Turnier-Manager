package de.petanqueturniermanager.jedergegenjeden.rangliste;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ColorHelper;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.print.PrintArea;
import de.petanqueturniermanager.helper.sheetsync.EingabeSignatur;
import de.petanqueturniermanager.helper.sheetsync.SheetSyncSignaturStore;
import de.petanqueturniermanager.helper.rangliste.SignaturQuellen;
import de.petanqueturniermanager.helper.rangliste.IRangliste;
import de.petanqueturniermanager.helper.rangliste.RangListeSpalte;
import de.petanqueturniermanager.helper.rangliste.RangListeSorter;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.RanglisteGeradeUngeradeFormatHelper;
import de.petanqueturniermanager.helper.sheet.SheetFreeze;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.helper.sheet.search.RangeSearchHelper;
import de.petanqueturniermanager.jedergegenjeden.JGJGruppenAufteiler;
import de.petanqueturniermanager.jedergegenjeden.konfiguration.JGJKonfigurationSheet;
import de.petanqueturniermanager.jedergegenjeden.meldeliste.JGJMeldeListeSheet_Update;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

/**
 * Rangliste für das JGJ-Turniersystem (Jeder gegen Jeden).
 * <p>
 * Liest den Spielplan per Block-Read ein, berechnet alle Statistiken in Java
 * und schreibt die Ergebnisse als Werte per RangeData – keine Sheet-Formeln.
 * <p>
 * Sortierkriterien:
 * <ol>
 *   <li>Spiele+ (absteigend)</li>
 *   <li>Spielpunkte-Differenz (absteigend)</li>
 *   <li>Spielpunkte+ (absteigend)</li>
 * </ol>
 */
public class JGJRanglisteSheet extends SheetRunner implements ISheet, IRangliste {

    public static final int HEADER_ZEILE = 0;
    public static final int ZWEITE_HEADER_ZEILE = 1;
    public static final int ERSTE_DATEN_ZEILE = 2;

    public static final int TEAM_NR_SPALTE = 0;
    public static final int TEAM_NAME_SPALTE = 1;
    public static final int PLATZ_SPALTE = 2;
    public static final int SPIELE_PLUS_SPALTE = 3;
    public static final int SPIELE_MINUS_SPALTE = 4;
    public static final int SPIELE_DIFF_SPALTE = 5;
    public static final int SPIELPUNKTE_PLUS_SPALTE = 6;
    public static final int SPIELPUNKTE_MINUS_SPALTE = 7;
    public static final int SPIELPUNKTE_DIFF_SPALTE = 8;
    public static final int VALIDATE_SPALTE = 9;

    private static final int COL_WIDTH_NR = 800;
    private static final int COL_WIDTH_NAME = 7000;
    private static final int COL_WIDTH_DATA = 1400;

    private static final String METADATA_SCHLUESSEL = SheetMetadataHelper.SCHLUESSEL_JGJ_RANGLISTE;

    private final JGJKonfigurationSheet konfigurationSheet;
    private final RangListeSorter rangListeSorter;
    private final JGJRanglisteRechner rangListeRechner;

    public JGJRanglisteSheet(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet, TurnierSystem.JGJ, "JGJ-RanglisteSheet");
        konfigurationSheet = new JGJKonfigurationSheet(workingSpreadsheet);
        rangListeSorter = new RangListeSorter(this);
        rangListeRechner = new JGJRanglisteRechner(workingSpreadsheet);
    }

    @Override
    protected JGJKonfigurationSheet getKonfigurationSheet() {
        return konfigurationSheet;
    }

    @Override
    public XSpreadsheet getXSpreadSheet() throws GenerateException {
        return SheetMetadataHelper.findeSheetUndHeile(
                getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
                METADATA_SCHLUESSEL, SheetNamen.LEGACY_RANGLISTE);
    }

    @Override
    protected void doRun() throws GenerateException {
        upDateSheet();
    }

    public void upDateSheet() throws GenerateException {
        var meldeListe = new JGJMeldeListeSheet_Update(getWorkingSpreadsheet());
        meldeListe.upDateSheet();

        TeamMeldungen aktiveMeldungen = meldeListe.getAktiveMeldungen();
        if (aktiveMeldungen == null || aktiveMeldungen.size() == 0) {
            processBoxinfo("processbox.abbruch");
            MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK)
                    .caption(I18n.get("msg.caption.jgj.spielplan"))
                    .message(I18n.get("msg.text.ungueltige.anzahl.meldungen")).show();
            return;
        }

        getxCalculatable().enableAutomaticCalculation(false);

        if (!NewSheet.from(this, SheetNamen.rangliste(), METADATA_SCHLUESSEL)
                .pos(DefaultSheetPos.JGJ_ENDRANGLISTE).setForceCreate(true).setActiv()
                .hideGrid().tabColor(getKonfigurationSheet().getRanglisteTabFarbe())
                .create().isDidCreate()) {
            ProcessBox.from().info("Abbruch vom Benutzer");
            return;
        }

        XSpreadsheet sheet = getXSpreadSheet();
        if (sheet == null) {
            return;
        }

        insertHeader(sheet);
        berechnungUndSchreiben(sheet, meldeListe, aktiveMeldungen);

        if (SheetRunner.isRunning()) {
            getSheetHelper().setActiveSheet(sheet);
            SheetRunner.unterdrückeNaechstesSelectionChange();
        }
        SheetSyncSignaturStore.commitVollaufbau(
                getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
                METADATA_SCHLUESSEL,
                new EingabeSignatur(SignaturQuellen::fuerJGJ));
    }

    /**
     * Berechnet alle Ranglisten-Daten aus dem Spielplan und schreibt sie ins Sheet.
     * Wird vom vollständigen Neuaufbau ({@link #upDateSheet()}) und vom
     * inkrementellen Update ({@link JGJRanglisteSheetUpdate}) verwendet.
     */
    protected void berechnungUndSchreiben(XSpreadsheet sheet, JGJMeldeListeSheet_Update meldeListe,
            TeamMeldungen aktiveMeldungen) throws GenerateException {
        processBoxinfo("processbox.rangliste.einfuegen");

        int gruppengroesse = konfigurationSheet.getGruppengroesse();
        if (gruppengroesse > 0 && aktiveMeldungen.size() > gruppengroesse) {
            berechnungUndSchreibenGruppen(sheet, meldeListe, aktiveMeldungen, gruppengroesse);
        } else {
            berechnungUndSchreibenEinzel(sheet, meldeListe, aktiveMeldungen);
        }
        getSheetHelper().setOptimaleBreitePlusMarge(sheet, TEAM_NR_SPALTE, SheetHelper.OPTIMALE_BREITE_MARGE);
    }

    private void berechnungUndSchreibenEinzel(XSpreadsheet sheet, JGJMeldeListeSheet_Update meldeListe,
            TeamMeldungen aktiveMeldungen) throws GenerateException {
        List<JGJRanglisteRechner.TeamStats> sortiert = rangListeRechner.berechneUndSortiere(aktiveMeldungen);
        Map<Integer, String> teamNamen = leseTeamNamen(meldeListe);

        insertDatenAlsWerte(sheet, sortiert, teamNamen, ERSTE_DATEN_ZEILE, false);

        if (!sortiert.isEmpty()) {
            new RangListeSpalte(PLATZ_SPALTE, this).upDateRanglisteSpalte();
            rangListeSorter.insertSortValidateSpalte(true);
            formatiereZahlenSpalten(sheet, ERSTE_DATEN_ZEILE, sortiert.size());
            formatiereZebraStreifen(sheet, ERSTE_DATEN_ZEILE, sortiert.size());
        }

        int letzteZeile = ERSTE_DATEN_ZEILE + sortiert.size() - 1;
        addFooter(sheet, letzteZeile + 2);
        setzeDruckbereich(sheet, letzteZeile + 2);
        SheetFreeze.from(getTurnierSheet()).anzZeilen(ERSTE_DATEN_ZEILE).anzSpalten(PLATZ_SPALTE + 1).doFreeze();
        getxCalculatable().calculateAll();
    }

    private void berechnungUndSchreibenGruppen(XSpreadsheet sheet, JGJMeldeListeSheet_Update meldeListe,
            TeamMeldungen aktiveMeldungen, int gruppengroesse) throws GenerateException {
        List<TeamMeldungen> gruppen = JGJGruppenAufteiler.teileInGruppen(aktiveMeldungen, gruppengroesse);
        Map<Integer, String> teamNamen = leseTeamNamen(meldeListe);
        int aktuelleZeile = ERSTE_DATEN_ZEILE;

        for (int g = 0; g < gruppen.size(); g++) {
            TeamMeldungen gruppe = gruppen.get(g);
            String buchstabe = gruppenBuchstabe(g);

            schreibeGruppenHeaderRangliste(sheet, aktuelleZeile, buchstabe);
            aktuelleZeile++;

            List<JGJRanglisteRechner.TeamStats> sortiert = rangListeRechner.berechneUndSortiere(gruppe);
            insertDatenAlsWerte(sheet, sortiert, teamNamen, aktuelleZeile, true);
            if (!sortiert.isEmpty()) {
                formatiereZahlenSpalten(sheet, aktuelleZeile, sortiert.size());
                formatiereZebraStreifen(sheet, aktuelleZeile, sortiert.size());
            }
            aktuelleZeile += sortiert.size();
        }

        int letzteZeile = aktuelleZeile - 1;
        addFooter(sheet, letzteZeile + 2);
        setzeDruckbereich(sheet, letzteZeile + 2);
        SheetFreeze.from(getTurnierSheet()).anzZeilen(ERSTE_DATEN_ZEILE).anzSpalten(0).doFreeze();
        getxCalculatable().calculateAll();
    }

    private void schreibeGruppenHeaderRangliste(XSpreadsheet sheet, int zeile, String buchstabe)
            throws GenerateException {
        String gruppenName = I18n.get("jgj.gruppe.name") + " " + buchstabe;
        getSheetHelper().setStringValueInCell(StringCellValue
                .from(sheet, Position.from(TEAM_NR_SPALTE, zeile), gruppenName)
                .setCellBackColor(konfigurationSheet.getRanglisteHeaderFarbe())
                .setBorder(BorderFactory.from().allThin().boldLn().forBottom().toBorder())
                .setHoriJustify(CellHoriJustify.CENTER)
                .setEndPosMergeSpalte(SPIELPUNKTE_DIFF_SPALTE));
    }

    private Map<Integer, String> leseTeamNamen(JGJMeldeListeSheet_Update meldeListe) throws GenerateException {
        return meldeListe.leseTeamNamen();
    }

    protected void insertHeader(XSpreadsheet sheet) throws GenerateException {
        Integer headerFarbe = konfigurationSheet.getRanglisteHeaderFarbe();

        int[][] spaltenBreiten = {
                { TEAM_NAME_SPALTE, COL_WIDTH_NAME },
                { PLATZ_SPALTE, COL_WIDTH_NR },
                { SPIELE_PLUS_SPALTE, COL_WIDTH_DATA },
                { SPIELE_MINUS_SPALTE, COL_WIDTH_DATA },
                { SPIELE_DIFF_SPALTE, COL_WIDTH_DATA },
                { SPIELPUNKTE_PLUS_SPALTE, COL_WIDTH_DATA },
                { SPIELPUNKTE_MINUS_SPALTE, COL_WIDTH_DATA },
                { SPIELPUNKTE_DIFF_SPALTE, COL_WIDTH_DATA },
        };
        for (int[] sw : spaltenBreiten) {
            getSheetHelper().setColumnProperties(sheet, sw[0],
                    ColumnProperties.from().setWidth(sw[1])
                            .setHoriJustify(CellHoriJustify.CENTER)
                            .setVertJustify(CellVertJustify2.CENTER));
        }

        int[][] einzelSpalten = {
                { TEAM_NR_SPALTE, 0 },
                { TEAM_NAME_SPALTE, 0 },
                { PLATZ_SPALTE, 1 },
        };
        String[] einzelTexte = {
                I18n.get("column.header.nr"),
                I18n.get("column.header.name"),
                I18n.get("column.header.platz"),
        };
        for (int i = 0; i < einzelSpalten.length; i++) {
            int col = einzelSpalten[i][0];
            var border = einzelSpalten[i][1] == 1
                    ? BorderFactory.from().allThin().boldLn().forBottom().forRight().toBorder()
                    : BorderFactory.from().allThin().boldLn().forBottom().toBorder();
            var cv = StringCellValue
                    .from(sheet, Position.from(col, HEADER_ZEILE), einzelTexte[i])
                    .setCellBackColor(headerFarbe)
                    .setBorder(border)
                    .setHoriJustify(CellHoriJustify.CENTER)
                    .setVertJustify(CellVertJustify2.CENTER)
                    .setEndPosMergeZeilePlus(1)
                    .setShrinkToFit(true);
            if (col == PLATZ_SPALTE) {
                cv.setRotate90().setCharWeight(com.sun.star.awt.FontWeight.BOLD);
            }
            getSheetHelper().setStringValueInCell(cv);
        }

        getSheetHelper().setStringValueInCell(StringCellValue
                .from(sheet, Position.from(SPIELE_PLUS_SPALTE, HEADER_ZEILE),
                        I18n.get("column.header.spiele"))
                .setCellBackColor(headerFarbe)
                .setBorder(BorderFactory.from().allThin().toBorder())
                .setHoriJustify(CellHoriJustify.CENTER)
                .setEndPosMergeSpalte(SPIELE_DIFF_SPALTE)
                .setShrinkToFit(true));

        getSheetHelper().setStringValueInCell(StringCellValue
                .from(sheet, Position.from(SPIELPUNKTE_PLUS_SPALTE, HEADER_ZEILE),
                        I18n.get("column.header.punkte"))
                .setCellBackColor(headerFarbe)
                .setBorder(BorderFactory.from().allThin().toBorder())
                .setHoriJustify(CellHoriJustify.CENTER)
                .setEndPosMergeSpalte(SPIELPUNKTE_DIFF_SPALTE)
                .setShrinkToFit(true));

        int[] subCols = {
                SPIELE_PLUS_SPALTE, SPIELE_MINUS_SPALTE, SPIELE_DIFF_SPALTE,
                SPIELPUNKTE_PLUS_SPALTE, SPIELPUNKTE_MINUS_SPALTE, SPIELPUNKTE_DIFF_SPALTE,
        };
        String[] subTexte = {
                I18n.get("schweizer.rangliste.spalte.punkte.plus"),
                I18n.get("schweizer.rangliste.spalte.punkte.minus"),
                I18n.get("schweizer.rangliste.spalte.punkte.differenz"),
                I18n.get("schweizer.rangliste.spalte.punkte.plus"),
                I18n.get("schweizer.rangliste.spalte.punkte.minus"),
                I18n.get("schweizer.rangliste.spalte.punkte.differenz"),
        };
        for (int i = 0; i < subCols.length; i++) {
            getSheetHelper().setStringValueInCell(StringCellValue
                    .from(sheet, Position.from(subCols[i], ZWEITE_HEADER_ZEILE), subTexte[i])
                    .setCellBackColor(headerFarbe)
                    .setBorder(BorderFactory.from().allThin().boldLn().forBottom().toBorder())
                    .setHoriJustify(CellHoriJustify.CENTER)
                    .setShrinkToFit(true));
        }
    }

    private void insertDatenAlsWerte(XSpreadsheet sheet, List<JGJRanglisteRechner.TeamStats> sortiert,
            Map<Integer, String> teamNamen, int startZeile, boolean mitPlatz) throws GenerateException {
        if (sortiert.isEmpty()) {
            return;
        }

        int letzteZeile = startZeile + sortiert.size() - 1;

        // Block 1: Nr, Name (+ optional Platz)
        RangeData block1 = new RangeData();
        for (int i = 0; i < sortiert.size(); i++) {
            JGJRanglisteRechner.TeamStats stats = sortiert.get(i);
            RowData row = block1.addNewRow();
            row.newInt(stats.teamNr());
            row.newString(teamNamen.getOrDefault(stats.teamNr(), ""));
            if (mitPlatz) {
                row.newInt(i + 1);
            }
        }
        int block1EndSpalte = mitPlatz ? PLATZ_SPALTE : TEAM_NAME_SPALTE;
        RangeHelper.from(this, RangePosition.from(TEAM_NR_SPALTE, startZeile, block1EndSpalte, letzteZeile))
                .setDataInRange(block1);

        // Block 2: Spiele+, Spiele-, SpieleΔ, SpPunkte+, SpPunkte-, SpPunkteΔ
        RangeData block2 = new RangeData();
        for (JGJRanglisteRechner.TeamStats stats : sortiert) {
            RowData row = block2.addNewRow();
            row.newInt(stats.spielePlus());
            row.newInt(stats.spieleMinus());
            row.newInt(stats.spielDiff());
            row.newInt(stats.spielPunktePlus());
            row.newInt(stats.spielPunkteMinus());
            row.newInt(stats.spielPunkteDiff());
        }
        RangeHelper.from(this, block2.getRangePosition(Position.from(SPIELE_PLUS_SPALTE, startZeile)))
                .setDataInRange(block2);

        // Nr-Spalte: grau + doppelte rechte Linie + zentriert
        getSheetHelper().setPropertiesInRange(sheet,
                RangePosition.from(TEAM_NR_SPALTE, startZeile, TEAM_NR_SPALTE, letzteZeile),
                CellProperties.from()
                        .margin(MeldeListeKonstanten.CELL_MARGIN)
                        .setCharColor(ColorHelper.CHAR_COLOR_GRAY_SPIELER_NR)
                        .setBorder(BorderFactory.from().allThin().doubleLn().forRight().toBorder())
                        .centerJustify());

        // Name-Spalte: linksbündig
        getSheetHelper().setPropertiesInRange(sheet,
                RangePosition.from(TEAM_NAME_SPALTE, startZeile, TEAM_NAME_SPALTE, letzteZeile),
                CellProperties.from().margin(MeldeListeKonstanten.CELL_MARGIN).setAllThinBorder().setHoriJustify(CellHoriJustify.LEFT)
                        .setShrinkToFit(true));
    }

    private void formatiereZahlenSpalten(XSpreadsheet sheet, int startZeile, int anzTeams)
            throws GenerateException {
        int letzteZeile = startZeile + anzTeams - 1;
        getSheetHelper().setPropertiesInRange(sheet,
                RangePosition.from(PLATZ_SPALTE, startZeile, SPIELPUNKTE_DIFF_SPALTE, letzteZeile),
                CellProperties.from()
                        .margin(MeldeListeKonstanten.CELL_MARGIN)
                        .setAllThinBorder()
                        .setHoriJustify(CellHoriJustify.CENTER)
                        .setBorder(BorderFactory.from().allThin().boldLn().forTop().toBorder()));

        // Platz-Spalte: fett + dicke rechte Linie (überschreibt bulk-Formatierung)
        getSheetHelper().setPropertiesInRange(sheet,
                RangePosition.from(PLATZ_SPALTE, startZeile, PLATZ_SPALTE, letzteZeile),
                CellProperties.from()
                        .margin(MeldeListeKonstanten.CELL_MARGIN)
                        .setCharWeight(com.sun.star.awt.FontWeight.BOLD)
                        .setBorder(BorderFactory.from().allThin().boldLn().forTop().forRight().toBorder()));
    }

    private void formatiereZebraStreifen(XSpreadsheet sheet, int startZeile, int anzTeams)
            throws GenerateException {
        int letzteZeile = startZeile + anzTeams - 1;
        RangePosition datenRange = RangePosition.from(
                TEAM_NR_SPALTE, startZeile, SPIELPUNKTE_DIFF_SPALTE, letzteZeile);
        RanglisteGeradeUngeradeFormatHelper.from(this, datenRange)
                .geradeFarbe(konfigurationSheet.getRanglisteHintergrundFarbeGerade())
                .ungeradeFarbe(konfigurationSheet.getRanglisteHintergrundFarbeUnGerade())
                .validateSpalte(validateSpalte())
                .apply();
    }

    private void addFooter(XSpreadsheet sheet, int fusszeile) throws GenerateException {
        processBoxinfo("processbox.fusszeile.einfuegen");
        getSheetHelper().setStringValueInCell(StringCellValue
                .from(sheet, Position.from(TEAM_NR_SPALTE, fusszeile), I18n.get("jgj.rangliste.fusszeile"))
                .setHoriJustify(CellHoriJustify.LEFT)
                .setCharHeight(8)
                .setEndPosMergeSpalte(SPIELPUNKTE_DIFF_SPALTE));
    }

    private void setzeDruckbereich(XSpreadsheet sheet, int letzteZeile) throws GenerateException {
        processBoxinfo("processbox.print.bereich");
        PrintArea.from(sheet, getWorkingSpreadsheet()).setPrintArea(
                RangePosition.from(TEAM_NR_SPALTE, HEADER_ZEILE, SPIELPUNKTE_DIFF_SPALTE, letzteZeile));
    }

    private static String gruppenBuchstabe(int index) {
        return String.valueOf((char) ('A' + index));
    }

    // ─── IRangliste ──────────────────────────────────────────────────────────

    @Override
    public int getErsteDatenZiele() throws GenerateException {
        return ERSTE_DATEN_ZEILE;
    }

    @Override
    public int getErsteSpalte() throws GenerateException {
        return TEAM_NR_SPALTE;
    }

    @Override
    public int getLetzteSpalte() throws GenerateException {
        return SPIELPUNKTE_DIFF_SPALTE;
    }

    @Override
    public int getErsteSummeSpalte() throws GenerateException {
        return SPIELE_PLUS_SPALTE;
    }

    @Override
    public int getManuellSortSpalte() throws GenerateException {
        return -1;
    }

    @Override
    public int validateSpalte() throws GenerateException {
        return VALIDATE_SPALTE;
    }

    @Override
    public void calculateAll() {
        // nicht benötigt – wird via calculateAll() am Ende von berechnungUndSchreiben() erledigt
    }

    @Override
    public List<Position> getRanglisteSpalten() throws GenerateException {
        return List.of(
                Position.from(SPIELE_PLUS_SPALTE, ERSTE_DATEN_ZEILE),
                Position.from(SPIELPUNKTE_DIFF_SPALTE, ERSTE_DATEN_ZEILE),
                Position.from(SPIELPUNKTE_PLUS_SPALTE, ERSTE_DATEN_ZEILE));
    }

    @Override
    public int sucheLetzteZeileMitSpielerNummer() throws GenerateException {
        var searchProp = new HashMap<String, Object>();
        searchProp.put(RangeSearchHelper.SEARCH_BACKWARDS, true);
        Position result = RangeSearchHelper.from(this,
                RangePosition.from(TEAM_NR_SPALTE, ERSTE_DATEN_ZEILE, TEAM_NR_SPALTE, ERSTE_DATEN_ZEILE + 999))
                .searchNachRegExprInSpalte("^\\d", searchProp);
        return result != null ? result.getZeile() : ERSTE_DATEN_ZEILE;
    }

    @Override
    public int getLetzteMitDatenZeileInSpielerNrSpalte() throws GenerateException {
        return sucheLetzteZeileMitSpielerNummer();
    }

    @Override
    public TurnierSheet getTurnierSheet() throws GenerateException {
        return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
    }
}
