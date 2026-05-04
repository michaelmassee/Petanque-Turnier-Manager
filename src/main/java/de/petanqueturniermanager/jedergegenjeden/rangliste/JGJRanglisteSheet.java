package de.petanqueturniermanager.jedergegenjeden.rangliste;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
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
import de.petanqueturniermanager.helper.rangliste.IRangliste;
import de.petanqueturniermanager.helper.rangliste.RangListeSpalte;
import de.petanqueturniermanager.helper.rangliste.RangListeSorter;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.RanglisteGeradeUngeradeFormatHelper;
import de.petanqueturniermanager.helper.sheet.SheetFreeze;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.helper.sheet.search.RangeSearchHelper;
import de.petanqueturniermanager.jedergegenjeden.konfiguration.JGJKonfigurationSheet;
import de.petanqueturniermanager.jedergegenjeden.meldeliste.JGJMeldeListeSheet_Update;
import de.petanqueturniermanager.jedergegenjeden.spielplan.JGJSpielPlanSheet;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

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
    private static final int MAX_SPIELPLAN_ZEILEN = 500;

    private static final String METADATA_SCHLUESSEL = SheetMetadataHelper.SCHLUESSEL_JGJ_RANGLISTE;

    private record TeamStats(int teamNr, int spielePlus, int spieleMinus,
            int spielPunktePlus, int spielPunkteMinus) {

        int spielDiff() {
            return spielePlus - spieleMinus;
        }

        int spielPunkteDiff() {
            return spielPunktePlus - spielPunkteMinus;
        }
    }

    private final JGJKonfigurationSheet konfigurationSheet;
    private final RangListeSorter rangListeSorter;

    public JGJRanglisteSheet(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet, TurnierSystem.JGJ, "JGJ-RanglisteSheet");
        konfigurationSheet = new JGJKonfigurationSheet(workingSpreadsheet);
        rangListeSorter = new RangListeSorter(this);
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
    }

    private void berechnungUndSchreibenEinzel(XSpreadsheet sheet, JGJMeldeListeSheet_Update meldeListe,
            TeamMeldungen aktiveMeldungen) throws GenerateException {
        List<TeamStats> sortiert = berechneUndSortiere(aktiveMeldungen);
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
        setzeDruckbereich(sheet, letzteZeile);
        SheetFreeze.from(getTurnierSheet()).anzZeilen(ERSTE_DATEN_ZEILE).anzSpalten(PLATZ_SPALTE + 1).doFreeze();
        getxCalculatable().calculateAll();
    }

    private void berechnungUndSchreibenGruppen(XSpreadsheet sheet, JGJMeldeListeSheet_Update meldeListe,
            TeamMeldungen aktiveMeldungen, int gruppengroesse) throws GenerateException {
        List<TeamMeldungen> gruppen = teileInGruppen(aktiveMeldungen, gruppengroesse);
        Map<Integer, String> teamNamen = leseTeamNamen(meldeListe);
        int aktuelleZeile = ERSTE_DATEN_ZEILE;

        for (int g = 0; g < gruppen.size(); g++) {
            TeamMeldungen gruppe = gruppen.get(g);
            String buchstabe = gruppenBuchstabe(g);

            schreibeGruppenHeaderRangliste(sheet, aktuelleZeile, buchstabe);
            aktuelleZeile++;

            List<TeamStats> sortiert = berechneUndSortiere(gruppe);
            insertDatenAlsWerte(sheet, sortiert, teamNamen, aktuelleZeile, true);
            if (!sortiert.isEmpty()) {
                formatiereZahlenSpalten(sheet, aktuelleZeile, sortiert.size());
                formatiereZebraStreifen(sheet, aktuelleZeile, sortiert.size());
            }
            aktuelleZeile += sortiert.size();
        }

        int letzteZeile = aktuelleZeile - 1;
        addFooter(sheet, letzteZeile + 2);
        setzeDruckbereich(sheet, letzteZeile);
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

    private List<TeamStats> berechneUndSortiere(TeamMeldungen aktiveMeldungen) throws GenerateException {
        Map<Integer, int[]> statsRaw = leseSpielplanStats(aktiveMeldungen);
        List<TeamStats> daten = new ArrayList<>();
        for (Team team : aktiveMeldungen.teams()) {
            int[] s = statsRaw.getOrDefault(team.getNr(), new int[4]);
            daten.add(new TeamStats(team.getNr(), s[0], s[1], s[2], s[3]));
        }
        daten.sort(Comparator.comparingInt(TeamStats::spielePlus).reversed()
                .thenComparing(Comparator.comparingInt(TeamStats::spielPunkteDiff).reversed())
                .thenComparing(Comparator.comparingInt(TeamStats::spielPunktePlus).reversed()));
        return daten;
    }

    private Map<Integer, int[]> leseSpielplanStats(TeamMeldungen aktiveMeldungen) throws GenerateException {
        Map<Integer, int[]> statsMap = new HashMap<>();
        for (Team team : aktiveMeldungen.teams()) {
            statsMap.put(team.getNr(), new int[4]);
        }

        var xDoc = getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
        XSpreadsheet spielplanSheet = SheetMetadataHelper.findeSheetUndHeile(
                xDoc, SheetMetadataHelper.SCHLUESSEL_JGJ_SPIELPLAN, JGJSpielPlanSheet.sheetName());
        if (spielplanSheet == null) {
            return statsMap;
        }

        int startZeile = JGJSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE;
        RangeData teamNrData = RangeHelper.from(spielplanSheet, xDoc,
                RangePosition.from(JGJSpielPlanSheet.TEAM_A_NR_SPALTE, startZeile,
                        JGJSpielPlanSheet.TEAM_B_NR_SPALTE, startZeile + MAX_SPIELPLAN_ZEILEN))
                .getDataFromRange();

        RangeData spielpunkteData = RangeHelper.from(spielplanSheet, xDoc,
                RangePosition.from(JGJSpielPlanSheet.SPIELPNKT_A_SPALTE, startZeile,
                        JGJSpielPlanSheet.SPIELPNKT_B_SPALTE, startZeile + MAX_SPIELPLAN_ZEILEN))
                .getDataFromRange();

        for (int i = 0; i < teamNrData.size(); i++) {
            RowData teamNrZeile = teamNrData.get(i);
            if (teamNrZeile.size() < 2) {
                break;
            }
            int nrA = teamNrZeile.get(0).getIntVal(0);
            if (nrA <= 0) {
                continue; // Gruppen-Header-Zeile oder Ende
            }
            int nrB = teamNrZeile.get(1).getIntVal(0);

            if (nrB <= 0) {
                var freispielStats = statsMap.computeIfAbsent(nrA, k -> new int[4]);
                freispielStats[0]++;
                RowData freispielPunkte = spielpunkteData.get(i);
                freispielStats[2] += freispielPunkte.size() > 0 ? freispielPunkte.get(0).getIntVal(0) : 0;
                freispielStats[3] += freispielPunkte.size() > 1 ? freispielPunkte.get(1).getIntVal(0) : 0;
                continue;
            }

            RowData punkteZeile = spielpunkteData.get(i);
            int pktA = punkteZeile.size() > 0 ? punkteZeile.get(0).getIntVal(0) : 0;
            int pktB = punkteZeile.size() > 1 ? punkteZeile.get(1).getIntVal(0) : 0;

            if (pktA <= 0 && pktB <= 0) {
                continue;
            }

            statsMap.computeIfAbsent(nrA, k -> new int[4])[2] += pktA;
            statsMap.computeIfAbsent(nrA, k -> new int[4])[3] += pktB;
            statsMap.computeIfAbsent(nrB, k -> new int[4])[2] += pktB;
            statsMap.computeIfAbsent(nrB, k -> new int[4])[3] += pktA;

            if (pktA > pktB) {
                statsMap.computeIfAbsent(nrA, k -> new int[4])[0]++;
                statsMap.computeIfAbsent(nrB, k -> new int[4])[1]++;
            } else if (pktB > pktA) {
                statsMap.computeIfAbsent(nrB, k -> new int[4])[0]++;
                statsMap.computeIfAbsent(nrA, k -> new int[4])[1]++;
            }
        }
        return statsMap;
    }

    private Map<Integer, String> leseTeamNamen(JGJMeldeListeSheet_Update meldeListe) throws GenerateException {
        Map<Integer, String> result = new HashMap<>();
        XSpreadsheet mlSheet = meldeListe.getXSpreadSheet();
        if (mlSheet == null) {
            return result;
        }

        var xDoc = getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
        int ersteZeile = meldeListe.getErsteDatenZiele();
        boolean zeigeTeamname = konfigurationSheet.isMeldeListeTeamnameAnzeigen();
        boolean zeigeVerein = konfigurationSheet.isMeldeListeVereinsnameAnzeigen();
        Formation formation = konfigurationSheet.getMeldeListeFormation();
        int anzSpieler = formation.getAnzSpieler();
        int ersterSpielerOffset = zeigeTeamname ? 2 : 1;
        int spaltenProSpieler = zeigeVerein ? 3 : 2;
        int maxSpalte = ersterSpielerOffset + anzSpieler * spaltenProSpieler - 1;

        RangeData data = RangeHelper.from(mlSheet, xDoc,
                RangePosition.from(0, ersteZeile, maxSpalte, ersteZeile + 999)).getDataFromRange();

        for (RowData row : data) {
            if (row.isEmpty()) {
                break;
            }
            int nr = row.get(0).getIntVal(0);
            if (nr <= 0) {
                break;
            }
            String name = zeigeTeamname
                    ? (row.size() > 1 ? row.get(1).getStringVal() : "")
                    : bauspielerNamenZusammen(row, anzSpieler, ersterSpielerOffset, spaltenProSpieler);
            result.put(nr, name != null ? name : "");
        }
        return result;
    }

    private String bauspielerNamenZusammen(RowData row, int anzSpieler, int ersterSpielerOffset,
            int spaltenProSpieler) {
        var sb = new StringBuilder();
        for (int s = 0; s < anzSpieler; s++) {
            int vorSpalte = ersterSpielerOffset + s * spaltenProSpieler;
            int nachSpalte = vorSpalte + 1;
            String vorname = vorSpalte < row.size() ? row.get(vorSpalte).getStringVal() : "";
            String nachname = nachSpalte < row.size() ? row.get(nachSpalte).getStringVal() : "";
            String spielerName = baueSpielerName(vorname, nachname);
            if (!spielerName.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append(" / ");
                }
                sb.append(spielerName);
            }
        }
        return sb.toString();
    }

    private static String baueSpielerName(String vorname, String nachname) {
        String vn = vorname != null ? vorname.trim() : "";
        String nn = nachname != null ? nachname.trim() : "";
        if (vn.isEmpty() && nn.isEmpty()) {
            return "";
        }
        if (vn.isEmpty()) {
            return nn;
        }
        if (nn.isEmpty()) {
            return vn;
        }
        return vn + " " + nn;
    }

    protected void insertHeader(XSpreadsheet sheet) throws GenerateException {
        Integer headerFarbe = konfigurationSheet.getRanglisteHeaderFarbe();

        int[][] spaltenBreiten = {
                { TEAM_NR_SPALTE, COL_WIDTH_NR },
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
            getSheetHelper().setStringValueInCell(StringCellValue
                    .from(sheet, Position.from(col, HEADER_ZEILE), einzelTexte[i])
                    .setCellBackColor(headerFarbe)
                    .setBorder(border)
                    .setHoriJustify(CellHoriJustify.CENTER)
                    .setEndPosMergeZeilePlus(1)
                    .setShrinkToFit(true));
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

    private void insertDatenAlsWerte(XSpreadsheet sheet, List<TeamStats> sortiert,
            Map<Integer, String> teamNamen, int startZeile, boolean mitPlatz) throws GenerateException {
        if (sortiert.isEmpty()) {
            return;
        }

        int letzteZeile = startZeile + sortiert.size() - 1;

        // Block 1: Nr, Name (+ optional Platz)
        RangeData block1 = new RangeData();
        for (int i = 0; i < sortiert.size(); i++) {
            TeamStats stats = sortiert.get(i);
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
        for (TeamStats stats : sortiert) {
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

        // Nr-Spalte: grau + doppelte rechte Linie
        getSheetHelper().setPropertiesInRange(sheet,
                RangePosition.from(TEAM_NR_SPALTE, startZeile, TEAM_NR_SPALTE, letzteZeile),
                CellProperties.from()
                        .setCharColor(ColorHelper.CHAR_COLOR_GRAY_SPIELER_NR)
                        .setBorder(BorderFactory.from().allThin().doubleLn().forRight().toBorder()));

        // Name-Spalte: linksbündig
        getSheetHelper().setPropertiesInRange(sheet,
                RangePosition.from(TEAM_NAME_SPALTE, startZeile, TEAM_NAME_SPALTE, letzteZeile),
                CellProperties.from().setAllThinBorder().setHoriJustify(CellHoriJustify.LEFT));
    }

    private void formatiereZahlenSpalten(XSpreadsheet sheet, int startZeile, int anzTeams)
            throws GenerateException {
        int letzteZeile = startZeile + anzTeams - 1;
        getSheetHelper().setPropertiesInRange(sheet,
                RangePosition.from(PLATZ_SPALTE, startZeile, SPIELPUNKTE_DIFF_SPALTE, letzteZeile),
                CellProperties.from()
                        .setAllThinBorder()
                        .setHoriJustify(CellHoriJustify.CENTER)
                        .setBorder(BorderFactory.from().allThin().boldLn().forTop().toBorder()));
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
                .setCharHeight(8));
    }

    private void setzeDruckbereich(XSpreadsheet sheet, int letzteZeile) throws GenerateException {
        processBoxinfo("processbox.print.bereich");
        PrintArea.from(sheet, getWorkingSpreadsheet()).setPrintArea(
                RangePosition.from(TEAM_NR_SPALTE, HEADER_ZEILE, SPIELPUNKTE_DIFF_SPALTE, letzteZeile));
    }

    private List<TeamMeldungen> teileInGruppen(TeamMeldungen meldungen, int gruppengroesse) {
        List<Team> teams = meldungen.teams();
        List<TeamMeldungen> gruppen = new ArrayList<>();
        for (int i = 0; i < teams.size(); i += gruppengroesse) {
            TeamMeldungen gruppe = new TeamMeldungen();
            for (int j = i; j < Math.min(i + gruppengroesse, teams.size()); j++) {
                gruppe.addTeamWennNichtVorhanden(teams.get(j));
            }
            gruppen.add(gruppe);
        }
        return gruppen;
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
