/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.formulex.rangliste;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.algorithmen.FormuleX;
import de.petanqueturniermanager.algorithmen.FormuleXErgebnis;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.formulex.konfiguration.FormuleXKonfigurationSheet;
import de.petanqueturniermanager.formulex.meldeliste.FormuleXMeldeListeSheetUpdate;
import de.petanqueturniermanager.formulex.spielrunde.FormuleXAbstractSpielrundeSheet;
import de.petanqueturniermanager.helper.ColorHelper;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.print.PrintArea;
import de.petanqueturniermanager.helper.rangliste.IRangliste;
import de.petanqueturniermanager.helper.rangliste.RangListeSorter;
import de.petanqueturniermanager.helper.rangliste.RangListeSpalte;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.RanglisteGeradeUngeradeFormatHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzManager;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzRegistry;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.helper.sheet.search.RangeSearchHelper;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;
import de.petanqueturniermanager.toolbar.TurnierModus;

/**
 * Erstellt die Rangliste für das Formule X Turniersystem.
 * <p>
 * Liest alle vorhandenen Spielrunden-Sheets ein und berechnet Wertung,
 * Siege, Punkte und Differenz. Sortierreihenfolge: Wertung → Differenz → Punkte+.
 */
public class FormuleXRanglisteSheet extends SheetRunner implements IRangliste, ISheet {

    private static final Logger LOGGER = LogManager.getLogger(FormuleXRanglisteSheet.class);

    public static final int HEADER_ZEILE      = 0;
    public static final int ZWEITE_HEADER_ZEILE = 1;
    public static final int ERSTE_DATEN_ZEILE = 2;

    public static final int TEAM_NR_SPALTE     = 0; // A
    public static final int TEAM_NAME_SPALTE   = 1; // B
    public static final int PLATZ_SPALTE       = 2; // C
    public static final int WERTUNG_SPALTE     = 3; // D
    public static final int SIEGE_SPALTE       = 4; // E
    public static final int PUNKTE_PLUS_SPALTE = 5; // F
    public static final int PUNKTE_DIFF_SPALTE = 6; // G
    public static final int VALIDATE_SPALTE    = PUNKTE_DIFF_SPALTE + 1; // H (versteckt)

    private static final int COL_WIDTH_NR   = 800;
    private static final int COL_WIDTH_NAME = 7000;
    private static final int COL_WIDTH_DATA = 1600;

    private final FormuleXKonfigurationSheet konfigurationSheet;
    private final RangListeSorter rangListeSorter;

    public FormuleXRanglisteSheet(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet, TurnierSystem.FORMULEX);
        konfigurationSheet = new FormuleXKonfigurationSheet(workingSpreadsheet);
        rangListeSorter = new RangListeSorter(this);
    }

    @Override
    public FormuleXKonfigurationSheet getKonfigurationSheet() {
        return konfigurationSheet;
    }

    @Override
    public XSpreadsheet getXSpreadSheet() throws GenerateException {
        return SheetMetadataHelper.findeSheetUndHeile(
                getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
                SheetMetadataHelper.SCHLUESSEL_FORMULEX_RANGLISTE,
                SheetNamen.formulexRangliste());
    }

    @Override
    public TurnierSheet getTurnierSheet() throws GenerateException {
        return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
    }

    @Override
    public void doRun() throws GenerateException {
        if (TurnierModus.get().istAktiv()) {
            BlattschutzRegistry.fuer(getTurnierSystem())
                    .ifPresent(k -> BlattschutzManager.get().entsperren(k, getWorkingSpreadsheet()));
        }
        doRunIntern();
        if (TurnierModus.get().istAktiv()) {
            BlattschutzRegistry.fuer(getTurnierSystem())
                    .ifPresent(k -> BlattschutzManager.get().schuetzen(k, getWorkingSpreadsheet()));
        }
    }

    private void doRunIntern() throws GenerateException {
        LOGGER.debug("doRunIntern START – Thread='{}'", Thread.currentThread().getName());
        processBoxinfo("processbox.rangliste.einfuegen");

        NewSheet.from(this, SheetNamen.formulexRangliste(), SheetMetadataHelper.SCHLUESSEL_FORMULEX_RANGLISTE)
                .pos(DefaultSheetPos.SCHWEIZER_ENDRANGLISTE)
                .forceCreate()
                .tabColor(konfigurationSheet.getRanglisteTabFarbe())
                .create();

        XSpreadsheet sheet = getXSpreadSheet();
        if (sheet == null) {
            return;
        }

        FormuleXMeldeListeSheetUpdate meldeliste = new FormuleXMeldeListeSheetUpdate(getWorkingSpreadsheet());
        TeamMeldungen aktiveMeldungen = meldeliste.getAktiveMeldungen();
        if (aktiveMeldungen == null || aktiveMeldungen.size() == 0) {
            processBoxinfo("processbox.abbruch");
            return;
        }

        insertHeader(sheet);
        berechnungUndSchreiben(sheet, meldeliste, aktiveMeldungen);

        if (SheetRunner.isRunning()) {
            getSheetHelper().setActiveSheet(sheet);
            SheetRunner.unterdrückeNaechstesSelectionChange();
        }
        LOGGER.debug("doRunIntern ENDE – Thread='{}'", Thread.currentThread().getName());
    }

    /**
     * Berechnet alle Ranglisten-Daten aus den Spielrunden und schreibt sie ins Sheet.
     * Wird sowohl vom vollständigen Neuaufbau als auch vom inkrementellen Update verwendet.
     */
    protected void berechnungUndSchreiben(XSpreadsheet sheet, FormuleXMeldeListeSheetUpdate meldeliste,
            TeamMeldungen aktiveMeldungen) throws GenerateException {
        int bisSpielrunde = konfigurationSheet.getAktiveSpielRunde().getNr();
        LOGGER.debug("berechnungUndSchreiben – {} Spielrunden, Thread='{}'",
                bisSpielrunde, Thread.currentThread().getName());

        var akkumulierung = leseAlleRunden(aktiveMeldungen, bisSpielrunde);
        List<FormuleXErgebnis> sortiert = new FormuleX().sortiereNachWertung(akkumulierung.ergebnisse(),
                bisSpielrunde);

        Map<Integer, String> teamNrZuName = leseTeamnamen(meldeliste);
        insertDatenAlsWerte(sheet, sortiert, bisSpielrunde, teamNrZuName, akkumulierung.siegeMap());

        if (!sortiert.isEmpty()) {
            new RangListeSpalte(PLATZ_SPALTE, this).upDateRanglisteSpalte();
            getRangListeSorter().insertSortValidateSpalte(true);

            int letzteZeilePlatz = ERSTE_DATEN_ZEILE + sortiert.size() - 1;
            getSheetHelper().setPropertiesInRange(sheet,
                    RangePosition.from(PLATZ_SPALTE, ERSTE_DATEN_ZEILE, PLATZ_SPALTE, letzteZeilePlatz),
                    CellProperties.from()
                            .setBorder(BorderFactory.from().allThin().boldLn().forTop().forRight().toBorder())
                            .setCharWeight(com.sun.star.awt.FontWeight.BOLD));
        }

        if (!sortiert.isEmpty()) {
            int letzteZeile = ERSTE_DATEN_ZEILE + sortiert.size() - 1;
            RangePosition datenRange = RangePosition.from(TEAM_NR_SPALTE, ERSTE_DATEN_ZEILE,
                    PUNKTE_DIFF_SPALTE, letzteZeile);
            RanglisteGeradeUngeradeFormatHelper.from(this, datenRange)
                    .geradeFarbe(konfigurationSheet.getRanglisteHintergrundFarbeGerade())
                    .ungeradeFarbe(konfigurationSheet.getRanglisteHintergrundFarbeUnGerade())
                    .validateSpalte(validateSpalte())
                    .apply();
        }

        int letzteZeile = sortiert.isEmpty() ? ZWEITE_HEADER_ZEILE
                : ERSTE_DATEN_ZEILE + sortiert.size() - 1;
        setzeDruckbereich(sheet, letzteZeile);
        getxCalculatable().calculateAll();
    }

    private record RanglisteAccumulation(List<FormuleXErgebnis> ergebnisse, Map<Integer, Integer> siegeMap) {}

    private RanglisteAccumulation leseAlleRunden(TeamMeldungen aktiveMeldungen, int bisSpielrunde)
            throws GenerateException {
        Map<Integer, int[]> statsMap = new HashMap<>(); // teamNr → [0]=eigenePunkte, [1]=kassiert, [2]=siege
        Map<Integer, Boolean> freilosMap = new HashMap<>();

        for (Team team : aktiveMeldungen.teams()) {
            statsMap.put(team.getNr(), new int[3]);
            freilosMap.put(team.getNr(), false);
        }

        var xDoc = getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
        for (int runde = 1; runde <= bisSpielrunde; runde++) {
            SheetRunner.testDoCancelTask();
            XSpreadsheet rundeSheet = SheetMetadataHelper.findeSheetUndHeile(xDoc,
                    SheetMetadataHelper.SCHLUESSEL_FORMULEX_SPIELRUNDE_PREFIX + runde,
                    SpielRundeNr.from(runde).getNr() + ". Spielrunde");
            if (rundeSheet == null) {
                LOGGER.debug("leseAlleRunden: Runde {} – Sheet nicht gefunden, übersprungen", runde);
                continue;
            }
            leseRundeEin(rundeSheet, aktiveMeldungen, statsMap, freilosMap);
        }

        List<FormuleXErgebnis> ergebnisse = new ArrayList<>();
        Map<Integer, Integer> siegeMap = new HashMap<>();
        for (Team team : aktiveMeldungen.teams()) {
            int[] stats = statsMap.getOrDefault(team.getNr(), new int[3]);
            boolean hatteFreilos = freilosMap.getOrDefault(team.getNr(), false);
            ergebnisse.add(new FormuleXErgebnis(team.getNr(), stats[0], stats[1], List.of(), hatteFreilos));
            siegeMap.put(team.getNr(), stats[2]);
        }
        return new RanglisteAccumulation(ergebnisse, siegeMap);
    }

    private void leseRundeEin(XSpreadsheet rundeSheet, TeamMeldungen aktiveMeldungen,
            Map<Integer, int[]> statsMap, Map<Integer, Boolean> freilosMap) throws GenerateException {
        var readRange = RangePosition.from(
                FormuleXAbstractSpielrundeSheet.TEAM_A_SPALTE,
                FormuleXAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE,
                FormuleXAbstractSpielrundeSheet.ERG_TEAM_B_SPALTE,
                FormuleXAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + 999);
        var rowsData = RangeHelper
                .from(rundeSheet, getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), readRange)
                .getDataFromRange();

        for (var row : rowsData) {
            if (row.size() < 2) {
                break;
            }
            int nrA = row.get(0).getIntVal(0);
            if (nrA <= 0) {
                break;
            }
            if (aktiveMeldungen.getTeam(nrA) == null) {
                continue;
            }

            int nrB = row.get(1).getIntVal(0);
            if (nrB <= 0) {
                freilosMap.put(nrA, true);
                statsMap.computeIfAbsent(nrA, k -> new int[3])[2]++; // Freilos zählt als Sieg (Doku 4.2)
                continue;
            }
            if (aktiveMeldungen.getTeam(nrB) == null) {
                continue;
            }

            int ergA = (row.size() > 2) ? row.get(2).getIntVal(0) : 0;
            int ergB = (row.size() > 3) ? row.get(3).getIntVal(0) : 0;

            if (ergA > 0 || ergB > 0) {
                statsMap.computeIfAbsent(nrA, k -> new int[3])[0] += ergA;
                statsMap.computeIfAbsent(nrA, k -> new int[3])[1] += ergB;
                statsMap.computeIfAbsent(nrB, k -> new int[3])[0] += ergB;
                statsMap.computeIfAbsent(nrB, k -> new int[3])[1] += ergA;
                if (ergA > ergB) {
                    statsMap.computeIfAbsent(nrA, k -> new int[3])[2]++;
                } else if (ergB > ergA) {
                    statsMap.computeIfAbsent(nrB, k -> new int[3])[2]++;
                }
            }
        }
    }

    private Map<Integer, String> leseTeamnamen(FormuleXMeldeListeSheetUpdate meldeliste) throws GenerateException {
        Map<Integer, String> result = new HashMap<>();
        XSpreadsheet mlSheet = meldeliste.getXSpreadSheet();
        if (mlSheet == null) {
            return result;
        }

        XSpreadsheetDocument doc = getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
        int nrSpalte    = meldeliste.getNrSpalte();
        int ersteZeile  = meldeliste.getErsteDatenZeile();

        if (konfigurationSheet.isMeldeListeTeamnameAnzeigen()) {
            int nameSpalte = meldeliste.getTeamnameSpalte();
            int maxSpalte  = Math.max(nrSpalte, nameSpalte);
            var data = RangeHelper
                    .from(mlSheet, doc, RangePosition.from(0, ersteZeile, maxSpalte, ersteZeile + 999))
                    .getDataFromRange();
            for (RowData row : data) {
                if (row.size() <= nrSpalte) {
                    break;
                }
                int nr = row.get(nrSpalte).getIntVal(0);
                if (nr <= 0) {
                    break;
                }
                String name = nameSpalte < row.size() ? row.get(nameSpalte).getStringVal() : null;
                result.put(nr, name != null ? name.trim() : "");
            }
        } else {
            int anzSpieler = konfigurationSheet.getMeldeListeFormation().getAnzSpieler();
            int[] vorSpalten  = new int[anzSpieler];
            int[] nachSpalten = new int[anzSpieler];
            int maxSpalte = nrSpalte;
            for (int s = 0; s < anzSpieler; s++) {
                vorSpalten[s]  = meldeliste.getVornameSpalte(s);
                nachSpalten[s] = meldeliste.getNachnameSpalte(s);
                maxSpalte = Math.max(maxSpalte, Math.max(vorSpalten[s], nachSpalten[s]));
            }
            var data = RangeHelper
                    .from(mlSheet, doc, RangePosition.from(0, ersteZeile, maxSpalte, ersteZeile + 999))
                    .getDataFromRange();
            for (RowData row : data) {
                if (row.size() <= nrSpalte) {
                    break;
                }
                int nr = row.get(nrSpalte).getIntVal(0);
                if (nr <= 0) {
                    break;
                }
                var sb = new StringBuilder();
                for (int s = 0; s < anzSpieler; s++) {
                    String vn = vorSpalten[s]  < row.size() ? row.get(vorSpalten[s]).getStringVal()  : null;
                    String nn = nachSpalten[s] < row.size() ? row.get(nachSpalten[s]).getStringVal() : null;
                    String spielerName = erstelleSpielerName(vn, nn);
                    if (!spielerName.isEmpty()) {
                        if (sb.length() > 0) {
                            sb.append(" / ");
                        }
                        sb.append(spielerName);
                    }
                }
                result.put(nr, sb.toString());
            }
        }
        return result;
    }

    private static String erstelleSpielerName(String vorname, String nachname) {
        String vn = vorname  != null ? vorname.trim()  : "";
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

    private void insertHeader(XSpreadsheet sheet) throws GenerateException {
        Integer headerColor = konfigurationSheet.getMeldeListeHeaderFarbe();

        int[][] spaltenBreiten = {
                { TEAM_NR_SPALTE,     COL_WIDTH_NR   },
                { TEAM_NAME_SPALTE,   COL_WIDTH_NAME  },
                { PLATZ_SPALTE,       COL_WIDTH_NR   },
                { WERTUNG_SPALTE,     COL_WIDTH_DATA  },
                { SIEGE_SPALTE,       COL_WIDTH_DATA  },
                { PUNKTE_PLUS_SPALTE, COL_WIDTH_DATA  },
                { PUNKTE_DIFF_SPALTE, COL_WIDTH_DATA  },
        };
        for (int[] sw : spaltenBreiten) {
            getSheetHelper().setColumnProperties(sheet, sw[0],
                    ColumnProperties.from().setWidth(sw[1])
                            .setHoriJustify(CellHoriJustify.CENTER).setVertJustify(CellVertJustify2.CENTER));
        }

        String[] texte = {
                I18n.get("column.header.nr"),
                I18n.get("formulex.rangliste.spalte.team"),
                I18n.get("column.header.platz"),
                I18n.get("formulex.rangliste.spalte.wertung"),
                I18n.get("column.header.siege"),
                I18n.get("formulex.rangliste.spalte.punkte.plus"),
                I18n.get("formulex.rangliste.spalte.punkte.differenz"),
        };
        int[] spalten = {
                TEAM_NR_SPALTE, TEAM_NAME_SPALTE, PLATZ_SPALTE,
                WERTUNG_SPALTE, SIEGE_SPALTE, PUNKTE_PLUS_SPALTE, PUNKTE_DIFF_SPALTE,
        };

        for (int i = 0; i < spalten.length; i++) {
            int col = spalten[i];
            var border = (col == TEAM_NR_SPALTE)
                    ? BorderFactory.from().allThin().doubleLn().forBottom().forRight().toBorder()
                    : (col == PLATZ_SPALTE)
                    ? BorderFactory.from().allThin().boldLn().forBottom().forRight().toBorder()
                    : BorderFactory.from().allThin().boldLn().forBottom().toBorder();
            var cv = StringCellValue
                    .from(sheet, Position.from(col, HEADER_ZEILE), texte[i])
                    .setCellBackColor(headerColor)
                    .setBorder(border)
                    .setHoriJustify(CellHoriJustify.CENTER)
                    .setEndPosMergeZeilePlus(1)
                    .setShrinkToFit(true);
            if (col == PLATZ_SPALTE) {
                cv.setRotate90().setCharWeight(com.sun.star.awt.FontWeight.BOLD)
                        .setVertJustify(CellVertJustify2.CENTER);
            }
            getSheetHelper().setStringValueInCell(cv);
        }
    }

    private void insertDatenAlsWerte(XSpreadsheet sheet, List<FormuleXErgebnis> sortiert,
            int bisSpielrunde, Map<Integer, String> teamNrZuName,
            Map<Integer, Integer> siegeMap) throws GenerateException {
        if (sortiert.isEmpty()) {
            return;
        }

        FormuleX formuleX = new FormuleX();
        int letzteZeile = ERSTE_DATEN_ZEILE + sortiert.size() - 1;

        RangeData block1 = new RangeData();
        for (FormuleXErgebnis erg : sortiert) {
            RowData row = block1.addNewRow();
            row.newInt(erg.teamNr());
            row.newString(teamNrZuName.getOrDefault(erg.teamNr(), ""));
        }
        RangeHelper.from(this,
                block1.getRangePosition(Position.from(TEAM_NR_SPALTE, ERSTE_DATEN_ZEILE)))
                .setDataInRange(block1);

        RangeData block2 = new RangeData();
        for (FormuleXErgebnis erg : sortiert) {
            RowData row = block2.addNewRow();
            row.newInt(formuleX.berechneWertung(erg, bisSpielrunde));
            row.newInt(siegeMap.getOrDefault(erg.teamNr(), 0));
            row.newInt(erg.eigenePunkte());
            row.newInt(erg.punktedifferenz());
        }
        RangeHelper.from(this,
                block2.getRangePosition(Position.from(WERTUNG_SPALTE, ERSTE_DATEN_ZEILE)))
                .setDataInRange(block2);

        getSheetHelper().setPropertiesInRange(sheet,
                RangePosition.from(TEAM_NR_SPALTE, ERSTE_DATEN_ZEILE, TEAM_NR_SPALTE, letzteZeile),
                CellProperties.from()
                        .setCharColor(ColorHelper.CHAR_COLOR_GRAY_SPIELER_NR)
                        .setBorder(BorderFactory.from().allThin().doubleLn().forRight().toBorder()));

        getSheetHelper().setPropertiesInRange(sheet,
                RangePosition.from(TEAM_NAME_SPALTE, ERSTE_DATEN_ZEILE, TEAM_NAME_SPALTE, letzteZeile),
                CellProperties.from().setAllThinBorder().setHoriJustify(CellHoriJustify.LEFT));

        getSheetHelper().setPropertiesInRange(sheet,
                RangePosition.from(WERTUNG_SPALTE, ERSTE_DATEN_ZEILE, PUNKTE_DIFF_SPALTE, letzteZeile),
                CellProperties.from().setAllThinBorder().setHoriJustify(CellHoriJustify.CENTER));
    }

    private void setzeDruckbereich(XSpreadsheet sheet, int letzteZeile) throws GenerateException {
        PrintArea.from(sheet, getWorkingSpreadsheet())
                .setPrintArea(RangePosition.from(
                        Position.from(TEAM_NR_SPALTE, HEADER_ZEILE),
                        Position.from(PUNKTE_DIFF_SPALTE, letzteZeile)));
    }

    // ── IRangliste ──────────────────────────────────────────────────────────────

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
        return PUNKTE_DIFF_SPALTE;
    }

    @Override
    public int getErsteSummeSpalte() throws GenerateException {
        return WERTUNG_SPALTE;
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
        // nicht benötigt
    }

    @Override
    public List<Position> getRanglisteSpalten() throws GenerateException {
        return List.of(
                Position.from(WERTUNG_SPALTE, ERSTE_DATEN_ZEILE),
                Position.from(PUNKTE_DIFF_SPALTE, ERSTE_DATEN_ZEILE));
    }

    @Override
    public int sucheLetzteZeileMitSpielerNummer() throws GenerateException {
        var searchProp = new HashMap<String, Object>();
        searchProp.put(RangeSearchHelper.SEARCH_BACKWARDS, true);
        Position result = RangeSearchHelper
                .from(this, RangePosition.from(TEAM_NR_SPALTE, ERSTE_DATEN_ZEILE, TEAM_NR_SPALTE,
                        ERSTE_DATEN_ZEILE + 999))
                .searchNachRegExprInSpalte("^\\d", searchProp);
        return result != null ? result.getZeile() : ERSTE_DATEN_ZEILE;
    }

    @Override
    public int getLetzteMitDatenZeileInSpielerNrSpalte() throws GenerateException {
        return sucheLetzteZeileMitSpielerNummer();
    }

    protected RangListeSorter getRangListeSorter() {
        return rangListeSorter;
    }
}
