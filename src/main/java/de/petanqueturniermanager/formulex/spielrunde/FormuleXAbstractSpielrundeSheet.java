/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.formulex.spielrunde;

import static com.google.common.base.Preconditions.checkNotNull;
import static de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties.CHAR_HEIGHT;
import static de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties.CHAR_WEIGHT;
import static de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties.HORI_JUSTIFY;
import static de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties.TABLE_BORDER2;
import static de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties.VERT_JUSTIFY;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.sun.star.awt.FontWeight;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;
import com.sun.star.table.TableBorder2;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.algorithmen.FormuleX;
import de.petanqueturniermanager.algorithmen.FormuleXErgebnis;
import de.petanqueturniermanager.basesheet.SheetTabFarben;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeHelper;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.formulex.konfiguration.FormuleXKonfigurationSheet;
import de.petanqueturniermanager.formulex.meldeliste.FormuleXMeldeListeSheetUpdate;
import de.petanqueturniermanager.helper.ColorHelper;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellstyle.SpielrundeHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.SpielrundeHintergrundFarbeUnGeradeStyle;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.print.PrintArea;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.EditierbaresZelleFormatHelper;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.CellData;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.model.TeamPaarung;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Abstrakte Basisklasse für alle Formule X Spielrunden-Sheets.
 * <p>
 * Verwaltet Spaltenstruktur, Sheet-Formatierung und das Einlesen gespielter Runden.
 * Subklassen implementieren die konkrete Spielrunden-Logik (Nächste / Aktualisieren).
 */
public abstract class FormuleXAbstractSpielrundeSheet extends SheetRunner implements ISheet {

    private static final Logger LOGGER = LogManager.getLogger(FormuleXAbstractSpielrundeSheet.class);

    public static final int SHEET_COLOR = SheetTabFarben.SPIELRUNDE;

    public static final int ERSTE_HEADER_ZEILE = 0;
    public static final int ZWEITE_HEADER_ZEILE = ERSTE_HEADER_ZEILE + 1;
    public static final int ERSTE_DATEN_ZEILE = ZWEITE_HEADER_ZEILE + 1;

    public static final int NR_CHARHEIGHT = 18;
    public static final int BAHN_NR_SPALTE = 0;
    public static final int TEAM_A_SPALTE = BAHN_NR_SPALTE + 1;
    public static final int TEAM_B_SPALTE = TEAM_A_SPALTE + 1;
    public static final int ERG_TEAM_A_SPALTE = TEAM_B_SPALTE + 1;
    public static final int ERG_TEAM_B_SPALTE = ERG_TEAM_A_SPALTE + 1;
    public static final int FEHLER_SPALTE = ERG_TEAM_B_SPALTE + 1;

    private static final int MIN_MELDUNGEN = 4;

    private final FormuleXKonfigurationSheet konfigurationSheet;
    private final FormuleXMeldeListeSheetUpdate meldeListe;
    private final SpielrundeHelper spielrundeHelper;
    private SpielRundeNr spielRundeNrInSheet = null;
    private boolean forceOk = false;

    protected FormuleXAbstractSpielrundeSheet(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet, TurnierSystem.FORMULEX, "Formule X Spielrunde");
        konfigurationSheet = new FormuleXKonfigurationSheet(workingSpreadsheet);
        meldeListe = new FormuleXMeldeListeSheetUpdate(workingSpreadsheet);
        spielrundeHelper = new SpielrundeHelper(this, NR_CHARHEIGHT, NR_CHARHEIGHT, true,
                konfigurationSheet.getSpielRundeHintergrundFarbeGeradeStyle(),
                konfigurationSheet.getSpielRundeHintergrundFarbeUnGeradeStyle());
    }

    @Override
    public FormuleXKonfigurationSheet getKonfigurationSheet() {
        return konfigurationSheet;
    }

    @Override
    public XSpreadsheet getXSpreadSheet() throws GenerateException {
        var rundeNr = getSpielRundeNr();
        return SheetMetadataHelper.findeSheetUndHeile(
                getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
                getSpielrundeSchluessel(rundeNr.getNr()), getLegacySheetName(rundeNr));
    }

    protected String getSpielrundeSchluessel(int rundeNr) {
        return SheetMetadataHelper.SCHLUESSEL_FORMULEX_SPIELRUNDE_PREFIX + rundeNr;
    }

    protected String getSheetName(SpielRundeNr nr) {
        return SheetNamen.formulexSpielrunde(nr.getNr());
    }

    protected final String getLegacySheetName(SpielRundeNr nr) {
        return nr.getNr() + ". Spielrunde";
    }

    @Override
    public final TurnierSheet getTurnierSheet() throws GenerateException {
        return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
    }

    public final SpielRundeNr getSpielRundeNr() throws GenerateException {
        return getKonfigurationSheet().getAktiveSpielRunde();
    }

    public SpielRundeNr getSpielRundeNrInSheet() {
        return spielRundeNrInSheet;
    }

    public void setSpielRundeNrInSheet(SpielRundeNr spielRundeNrInSheet) {
        this.spielRundeNrInSheet = spielRundeNrInSheet;
    }

    public FormuleXMeldeListeSheetUpdate getMeldeListe() {
        return meldeListe;
    }

    protected final boolean canStart(TeamMeldungen meldungen) throws GenerateException {
        if (getSpielRundeNr().getNr() < 1) {
            getSheetHelper().setActiveSheet(getMeldeListe().getXSpreadSheet());
            MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK)
                    .caption(I18n.get("msg.caption.aktuelle.spielrunde.fehler"))
                    .message(I18n.get("formulex.spielrunde.fehler.ungueltige.spielrunde", getSpielRundeNr().getNr()))
                    .show();
            return false;
        }
        if (meldungen.size() < MIN_MELDUNGEN) {
            getSheetHelper().setActiveSheet(getMeldeListe().getXSpreadSheet());
            MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK)
                    .caption(I18n.get("msg.caption.aktuelle.spielrunde.fehler"))
                    .message(I18n.get("formulex.spielrunde.fehler.zu.wenige.meldungen", meldungen.size()))
                    .show();
            return false;
        }
        return true;
    }

    /**
     * Liest alle gespielten Runden ein und akkumuliert pro Team:
     * eigenePunkte (Summe), kassiertePunkte (Summe), alle Gegner und Freilos-Flag.
     *
     * @param aktiveMeldungen aktive Teams
     * @param abSpielrunde    erste einzulesende Runde (inkl.)
     * @param bisSpielrunde   letzte einzulesende Runde (inkl.)
     * @return akkumulierte Ergebnisdaten je Team
     */
    protected List<FormuleXErgebnis> gespieltenRundenEinlesen(TeamMeldungen aktiveMeldungen, int abSpielrunde,
            int bisSpielrunde) throws GenerateException {

        Map<Integer, int[]> rawPointsMap = new HashMap<>(); // teamNr → [0]=eigenePunkte, [1]=kassiertePunkte
        Map<Integer, List<Integer>> gegnerMap = new HashMap<>();
        Set<Integer> freilosTeams = new HashSet<>();

        for (Team team : aktiveMeldungen.teams()) {
            rawPointsMap.put(team.getNr(), new int[2]);
            gegnerMap.put(team.getNr(), new ArrayList<>());
        }

        if (bisSpielrunde >= abSpielrunde && bisSpielrunde >= 1) {
            int spielrunde = Math.max(abSpielrunde, 1);
            processBoxinfo("processbox.gespielte.runden.einlesen", spielrunde, bisSpielrunde);
            var xDoc = getWorkingSpreadsheet().getWorkingSpreadsheetDocument();

            for (; spielrunde <= bisSpielrunde; spielrunde++) {
                SheetRunner.testDoCancelTask();
                XSpreadsheet sheet = SheetMetadataHelper.findeSheetUndHeile(xDoc,
                        getSpielrundeSchluessel(spielrunde), getSheetName(SpielRundeNr.from(spielrunde)));
                if (sheet == null) {
                    continue;
                }
                leseRundeEin(sheet, aktiveMeldungen, rawPointsMap, gegnerMap, freilosTeams);
            }
        }

        List<FormuleXErgebnis> ergebnisse = new ArrayList<>();
        for (Team team : aktiveMeldungen.teams()) {
            int[] pts = rawPointsMap.getOrDefault(team.getNr(), new int[2]);
            List<Integer> gegnerNrn = gegnerMap.getOrDefault(team.getNr(), new ArrayList<>());
            boolean hatteFreilos = freilosTeams.contains(team.getNr());
            ergebnisse.add(new FormuleXErgebnis(team.getNr(), pts[0], pts[1], gegnerNrn, hatteFreilos));
        }
        return ergebnisse;
    }

    private void leseRundeEin(XSpreadsheet sheet, TeamMeldungen aktiveMeldungen, Map<Integer, int[]> rawPointsMap,
            Map<Integer, List<Integer>> gegnerMap, Set<Integer> freilosTeams) throws GenerateException {
        RangePosition readRange = RangePosition.from(TEAM_A_SPALTE, ERSTE_DATEN_ZEILE, ERG_TEAM_B_SPALTE,
                ERSTE_DATEN_ZEILE + 999);
        RangeData rowsData = RangeHelper
                .from(sheet, getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), readRange).getDataFromRange();

        for (RowData row : rowsData) {
            if (row.size() < 2) {
                break;
            }
            int nrA = row.get(0).getIntVal(0); // TEAM_A (relativ: 0)
            if (nrA <= 0) {
                break;
            }
            Team teamA = aktiveMeldungen.getTeam(nrA);
            if (teamA == null) {
                continue;
            }

            int nrB = row.get(1).getIntVal(0); // TEAM_B (relativ: 1)
            if (nrB <= 0) {
                freilosTeams.add(nrA);
                continue;
            }
            Team teamB = aktiveMeldungen.getTeam(nrB);
            if (teamB == null) {
                continue;
            }

            gegnerMap.computeIfAbsent(nrA, k -> new ArrayList<>()).add(nrB);
            gegnerMap.computeIfAbsent(nrB, k -> new ArrayList<>()).add(nrA);

            int ergA = (row.size() > 2) ? row.get(2).getIntVal(0) : 0; // ERG_TEAM_A (relativ: 2)
            int ergB = (row.size() > 3) ? row.get(3).getIntVal(0) : 0; // ERG_TEAM_B (relativ: 3)

            rawPointsMap.computeIfAbsent(nrA, k -> new int[2])[0] += ergA;
            rawPointsMap.computeIfAbsent(nrA, k -> new int[2])[1] += ergB;
            rawPointsMap.computeIfAbsent(nrB, k -> new int[2])[0] += ergB;
            rawPointsMap.computeIfAbsent(nrB, k -> new int[2])[1] += ergA;
        }
    }

    protected boolean neueSpielrunde(TeamMeldungen meldungen, SpielRundeNr neueSpielrundeNr,
            List<FormuleXErgebnis> ergebnisse) throws GenerateException {
        return neueSpielrunde(meldungen, neueSpielrundeNr, ergebnisse, isForceOk());
    }

    protected boolean neueSpielrunde(TeamMeldungen meldungen, SpielRundeNr neueSpielrundeNr,
            List<FormuleXErgebnis> ergebnisse, boolean force) throws GenerateException {
        checkNotNull(meldungen);

        processBoxinfo("processbox.neue.spielrunde", neueSpielrundeNr.getNr());
        processBoxinfo("processbox.anzahl.meldungen", meldungen.size());

        if (!NewSheet.from(this, getSheetName(getSpielRundeNr()), getSpielrundeSchluessel(getSpielRundeNr().getNr()))
                .pos(DefaultSheetPos.SCHWEIZER_WORK).setForceCreate(force).setActiv().hideGrid().create()
                .isDidCreate()) {
            ProcessBox.from().info(I18n.get("formulex.spielrunde.abbruch"));
            return false;
        }

        getKonfigurationSheet().setAktiveSpielRunde(getSpielRundeNr());

        FormuleX formuleX = new FormuleX();
        List<TeamPaarung> paarungen;

        if (neueSpielrundeNr.getNr() == 1) {
            paarungen = formuleX.ersteRunde(meldungen.teams());
        } else {
            List<FormuleXErgebnis> sortiert = formuleX.sortiereNachWertung(ergebnisse,
                    neueSpielrundeNr.getNr() - 1);
            paarungen = formuleX.weitereRunde(sortiert);
        }

        teamPaarungenEinfuegen(paarungen);
        datenErsteSpalte();
        datenformatieren();
        fehlerSpalteFormatieren();
        header();
        trennlinienSetzen();
        druckBereichSetzen();

        return true;
    }

    private void teamPaarungenEinfuegen(List<TeamPaarung> paarungen) throws GenerateException {
        if (paarungen == null) {
            return;
        }
        RangeData rangeData = new RangeData();

        for (TeamPaarung teamPaarung : paarungen) {
            SheetRunner.testDoCancelTask();
            RowData row = rangeData.addNewRow();
            row.add(new CellData(teamPaarung.getA().getNr()));
            if (teamPaarung.hasB()) {
                row.add(new CellData(teamPaarung.getB().getNr()));
            } else {
                row.add(new CellData("")); // Freilos: kein Gegner
            }
        }

        Position startPos = Position.from(TEAM_A_SPALTE, ERSTE_DATEN_ZEILE);
        RangeHelper.from(this, rangeData.getRangePosition(startPos)).setDataInRange(rangeData);
    }

    private void datenErsteSpalte() throws GenerateException {
        Integer headerColor = getKonfigurationSheet().getSpielRundeHeaderFarbe();
        Integer letzteZeile = letztePositionRechtsUnten().getZeile();
        SpielrundeSpielbahn spielrundeSpielbahn = getKonfigurationSheet().getSpielrundeSpielbahn();
        spielrundeHelper.datenErsteSpalte(spielrundeSpielbahn, ERSTE_DATEN_ZEILE, letzteZeile, BAHN_NR_SPALTE,
                ERSTE_HEADER_ZEILE, ZWEITE_HEADER_ZEILE, headerColor);
    }

    private void datenformatieren() throws GenerateException {
        processBoxinfo("processbox.formatiere.daten");

        XSpreadsheet sheet = getXSpreadSheet();
        Position datenStart = Position.from(TEAM_A_SPALTE, ERSTE_DATEN_ZEILE);
        Position datenEnd = letztePositionRechtsUnten();

        RangePosition datenRangeInclErg = RangePosition.from(datenStart, datenEnd);
        TableBorder2 border = BorderFactory.from().allThin().toBorder();
        getSheetHelper().setPropertyInRange(sheet, datenRangeInclErg, TABLE_BORDER2, border);

        SpielrundeHintergrundFarbeGeradeStyle geradeColor = getKonfigurationSheet()
                .getSpielRundeHintergrundFarbeGeradeStyle();
        SpielrundeHintergrundFarbeUnGeradeStyle unGeradeColor = getKonfigurationSheet()
                .getSpielRundeHintergrundFarbeUnGeradeStyle();

        RangePosition datenRangeSpielpaarungen = RangePosition.from(datenRangeInclErg).endeSpalte(TEAM_B_SPALTE);
        spielrundeHelper.formatiereGeradeUngradeSpielpaarungen(this, datenRangeSpielpaarungen, geradeColor,
                unGeradeColor);

        getSheetHelper().setPropertyInRange(sheet, datenRangeInclErg, HORI_JUSTIFY, CellHoriJustify.CENTER);
        getSheetHelper().setPropertyInRange(sheet, datenRangeInclErg, VERT_JUSTIFY, CellVertJustify2.CENTER);

        // Team-Spalten: große Schrift
        RangePosition teamSpalten = RangePosition.from(datenRangeInclErg).endeSpalte(TEAM_B_SPALTE);
        getSheetHelper().setPropertyInRange(sheet, teamSpalten, CHAR_HEIGHT, 32);
        getSheetHelper().setPropertyInRange(sheet, teamSpalten, CHAR_WEIGHT, FontWeight.BOLD);

        // Ergebnis-Spalten: große Schrift + Validierung
        datenEnd = letztePositionRechtsUnten();
        RangePosition ergebnisRange = RangePosition.from(
                Position.from(ERG_TEAM_A_SPALTE, ERSTE_DATEN_ZEILE), Position.from(datenEnd));

        getSheetHelper().setPropertyInRange(sheet, ergebnisRange, CHAR_HEIGHT, 32);
        getSheetHelper().setPropertyInRange(sheet, ergebnisRange, CHAR_WEIGHT, FontWeight.BOLD);
        spielrundeHelper.formatiereErgebnissRange(this, ergebnisRange, ERG_TEAM_A_SPALTE);

        if (datenEnd != null) {
            RangePosition ergebnisEditierbarRange = RangePosition.from(
                    ERG_TEAM_A_SPALTE, ERSTE_DATEN_ZEILE, ERG_TEAM_B_SPALTE, datenEnd.getZeile());
            EditierbaresZelleFormatHelper.anwenden(this, ergebnisEditierbarRange);
        }
    }

    private void header() throws GenerateException {
        processBoxinfo("processbox.formatiere.header");
        Integer headerColor = getKonfigurationSheet().getSpielRundeHeaderFarbe();

        Position headerStart = Position.from(TEAM_A_SPALTE, ERSTE_HEADER_ZEILE);

        StringCellValue headerValue = StringCellValue.from(getXSpreadSheet(), headerStart)
                .setVertJustify(CellVertJustify2.CENTER).setHoriJustify(CellHoriJustify.CENTER)
                .setBorder(BorderFactory.from().allThin().toBorder()).setCellBackColor(headerColor)
                .setCharHeight(NR_CHARHEIGHT).setShrinkToFit(true).setEndPosMergeSpaltePlus(3)
                .setValue(I18n.get("formulex.spielrunde.header.spielrunde", getSpielRundeNr().getNr()));
        getSheetHelper().setStringValueInCell(headerValue);

        StringCellValue headerValueZeile2 = StringCellValue
                .from(getXSpreadSheet(), headerStart.zeile(ZWEITE_HEADER_ZEILE))
                .setVertJustify(CellVertJustify2.CENTER)
                .setHoriJustify(CellHoriJustify.CENTER)
                .setBorder(BorderFactory.from().allThin().boldLn().forBottom().toBorder())
                .setCellBackColor(headerColor)
                .setCharHeight(NR_CHARHEIGHT).setShrinkToFit(true);

        headerValueZeile2.setValue("A");
        getSheetHelper().setStringValueInCell(headerValueZeile2);

        headerValueZeile2.setValue("B").spaltePlus(1);
        getSheetHelper().setStringValueInCell(headerValueZeile2);

        headerValueZeile2.setValue(I18n.get("schweizer.spielrunde.spalte.ergebnis")).spaltePlus(1)
                .setEndPosMergeSpaltePlus(1);
        getSheetHelper().setStringValueInCell(headerValueZeile2);
    }

    private void fehlerSpalteFormatieren() throws GenerateException {
        XSpreadsheet sheet = getXSpreadSheet();
        Position letztePos = letztePositionRechtsUnten();
        if (letztePos == null) {
            return;
        }
        int letzteZeile = letztePos.getZeile();

        getSheetHelper().setColumnWidth(sheet, Position.from(FEHLER_SPALTE, ERSTE_HEADER_ZEILE), 1800);

        for (int zeile = ERSTE_DATEN_ZEILE; zeile <= letzteZeile; zeile++) {
            String ergA = Position.from(ERG_TEAM_A_SPALTE, zeile).getAddress();
            String ergB = Position.from(ERG_TEAM_B_SPALTE, zeile).getAddress();

            // @formatter:off
            String formel = "IF(OR(" +
                    "AND(ISBLANK(" + ergA + ");ISBLANK(" + ergB + "));" +
                    "AND(" + ergA + "<14;" + ergB + "<14;" + ergA + ">-1;" + ergB + ">-1;" + ergA + "<>" + ergB + ")" +
                    ");\"\";\"" + I18n.get("formulex.spielrunde.fehler.formel") + "\")";
            // @formatter:on

            StringCellValue cv = StringCellValue
                    .from(sheet, Position.from(FEHLER_SPALTE, zeile), formel)
                    .setCharColor(ColorHelper.CHAR_COLOR_RED)
                    .setCharWeight(FontWeight.BOLD)
                    .setCharHeight(14)
                    .setHoriJustify(CellHoriJustify.CENTER);
            getSheetHelper().setFormulaInCell(cv);
        }
    }

    private void trennlinienSetzen() throws GenerateException {
        XSpreadsheet sheet = getXSpreadSheet();
        Position letztePos = letztePositionRechtsUnten();
        if (letztePos == null) {
            return;
        }
        int letzteZeile = letztePos.getZeile();

        RangePosition bahnNrRange = RangePosition.from(BAHN_NR_SPALTE, ERSTE_HEADER_ZEILE,
                BAHN_NR_SPALTE, letzteZeile);
        getSheetHelper().setPropertyInRange(sheet, bahnNrRange, TABLE_BORDER2,
                BorderFactory.from().allThin().doubleLn().forRight().toBorder());

        RangePosition teamBRange = RangePosition.from(TEAM_B_SPALTE, ERSTE_HEADER_ZEILE,
                TEAM_B_SPALTE, letzteZeile);
        getSheetHelper().setPropertyInRange(sheet, teamBRange, TABLE_BORDER2,
                BorderFactory.from().allThin().boldLn().forRight().toBorder());

        RangePosition headerUntenRange = RangePosition.from(BAHN_NR_SPALTE, ZWEITE_HEADER_ZEILE,
                ERG_TEAM_B_SPALTE, ZWEITE_HEADER_ZEILE);
        getSheetHelper().setPropertyInRange(sheet, headerUntenRange, TABLE_BORDER2,
                BorderFactory.from().boldLn().forBottom().toBorder());
    }

    private void druckBereichSetzen() throws GenerateException {
        processBoxinfo("processbox.print.bereich");
        Position letztePos = letztePositionRechtsUnten();
        if (letztePos == null) {
            return;
        }
        RangePosition druckBereich = RangePosition.from(BAHN_NR_SPALTE, ERSTE_HEADER_ZEILE,
                Position.from(ERG_TEAM_B_SPALTE, letztePos.getZeile()));
        PrintArea.from(getXSpreadSheet(), getWorkingSpreadsheet()).setPrintArea(druckBereich);
    }

    public Position letztePositionRechtsUnten() throws GenerateException {
        Position spielerNrPos = Position.from(TEAM_A_SPALTE, ERSTE_DATEN_ZEILE);
        RangePosition erstSpielrNrRange = RangePosition.from(TEAM_A_SPALTE, ERSTE_DATEN_ZEILE, TEAM_A_SPALTE,
                ERSTE_DATEN_ZEILE + 999);
        RangeData nrDaten = RangeHelper.from(this, erstSpielrNrRange).getDataFromRange();

        int index = IntStream.range(0, nrDaten.size()).filter(nrDatenIdx -> {
            String val = nrDaten.get(nrDatenIdx).get(0).getStringVal();
            return val == null || val.isEmpty();
        }).findFirst().orElse(-1);

        if (index == 0) {
            return null;
        }
        if (index > 0) {
            spielerNrPos.zeilePlus(index - 1);
        }
        return spielerNrPos.spalte(ERG_TEAM_B_SPALTE);
    }

    @VisibleForTesting
    public boolean isForceOk() {
        return forceOk;
    }

    @VisibleForTesting
    public void setForceOk(boolean forceOk) {
        this.forceOk = forceOk;
    }
}
