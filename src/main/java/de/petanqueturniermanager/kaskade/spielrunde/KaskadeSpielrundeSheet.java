/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.kaskade.spielrunde;

import static de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties.CHAR_HEIGHT;
import static de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties.CHAR_WEIGHT;
import static de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties.HORI_JUSTIFY;
import static de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties.TABLE_BORDER2;
import static de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties.VERT_JUSTIFY;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.FontWeight;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;
import com.sun.star.table.TableBorder2;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.algorithmen.KaskadenKoGruppenRunde;
import de.petanqueturniermanager.algorithmen.KaskadenKoRunde;
import de.petanqueturniermanager.algorithmen.KaskadenKoRundenPlan;
import de.petanqueturniermanager.algorithmen.KaskadenKoRundenPlaner;
import de.petanqueturniermanager.algorithmen.KaskadenKoSpielPaar;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ColorHelper;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.print.PrintArea;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.CellData;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.kaskade.konfiguration.KaskadeKonfigurationSheet;
import de.petanqueturniermanager.kaskade.meldeliste.KaskadeMeldeListeSheetUpdate;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Erstellt das Rundenplan-Sheet für die nächste Kaskadenrunde.
 * <p>
 * Zustandssteuerung via {@link KaskadeKonfigurationSheet}:
 * <ul>
 *   <li>Wenn alle Kaskadenrunden abgeschlossen sind → delegiert an {@link KaskadeKoFeldSheet}</li>
 *   <li>Wenn KO-Felder bereits erstellt → Hinweis ausgeben</li>
 *   <li>Sonst → nächste Kaskadenrunde erstellen</li>
 * </ul>
 */
public class KaskadeSpielrundeSheet extends SheetRunner implements ISheet {

    private static final Logger LOGGER = LogManager.getLogger(KaskadeSpielrundeSheet.class);

    public static final int GRUPPE_SPALTE     = 0;
    public static final int SPIEL_NR_SPALTE   = 1;
    public static final int TEAM_A_SPALTE     = 2;
    public static final int TEAM_B_SPALTE     = 3;
    public static final int ERG_TEAM_A_SPALTE = 4;
    public static final int ERG_TEAM_B_SPALTE = 5;
    public static final int FEHLER_SPALTE     = 6;

    public static final int HEADER_ZEILE      = 0;
    public static final int ERSTE_DATEN_ZEILE = HEADER_ZEILE + 1;

    private static final int CHARHEIGHT_NR       = 18;
    private static final int CHARHEIGHT_TEAM     = 28;
    private static final int CHARHEIGHT_ERGEBNIS = 28;

    private final KaskadeKonfigurationSheet konfigurationSheet;
    private final KaskadeMeldeListeSheetUpdate meldeListe;

    /** Rundennummer des im aktuellen Lauf erstellten Sheets (0 = noch nicht erstellt). */
    private int aktuelleRundeNr;

    /** Wird für Tests gesetzt, damit forceCreate aktiviert wird. */
    private boolean forceOk;

    public KaskadeSpielrundeSheet(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet, TurnierSystem.KASKADE, "Kaskaden-Spielrunde");
        konfigurationSheet = new KaskadeKonfigurationSheet(workingSpreadsheet);
        meldeListe = new KaskadeMeldeListeSheetUpdate(workingSpreadsheet);
    }

    @Override
    public XSpreadsheet getXSpreadSheet() throws GenerateException {
        int rundeNr = (aktuelleRundeNr > 0) ? aktuelleRundeNr : konfigurationSheet.getAktiveKaskadenRunde();
        if (rundeNr <= 0) {
            return null;
        }
        return SheetMetadataHelper.findeSheetUndHeile(
                getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
                SheetMetadataHelper.schluesselKaskadenRunde(rundeNr),
                SheetNamen.kaskadenRunde(rundeNr));
    }

    @Override
    public TurnierSheet getTurnierSheet() throws GenerateException {
        return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
    }

    @Override
    public KaskadeKonfigurationSheet getKonfigurationSheet() {
        return konfigurationSheet;
    }

    @Override
    protected void doRun() throws GenerateException {
        getxCalculatable().enableAutomaticCalculation(false);

        var anzahlKaskaden = konfigurationSheet.getAnzahlKaskaden();
        var aktiveRunde    = konfigurationSheet.getAktiveKaskadenRunde();

        if (konfigurationSheet.isKoFelderErstellt()) {
            processBoxinfo("kaskade.spielrunde.ko.felder.bereits.erstellt");
            return;
        }

        if (aktiveRunde >= anzahlKaskaden) {
            processBoxinfo("kaskade.spielrunde.alle.kaskaden.abgeschlossen", anzahlKaskaden);
            processBoxinfo("processbox.kaskade.ko.felder.erstellen");
            new KaskadeKoFeldSheet(getWorkingSpreadsheet()).doRun();
            konfigurationSheet.setKoFelderErstellt(true);
            return;
        }

        if (aktiveRunde > 0 && !alleErgebnisseEingetragen(aktiveRunde)) {
            var aktuelleSheet = getXSpreadSheet();
            if (aktuelleSheet != null) {
                getSheetHelper().setActiveSheet(aktuelleSheet);
            }
            MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK)
                    .caption(I18n.get("msg.caption.naechste.runde.nicht.moeglich"))
                    .message(I18n.get("kaskade.spielrunde.fehler.ergebnisse.fehlen", aktiveRunde))
                    .show();
            return;
        }

        var naechsteRundeNr = aktiveRunde + 1;
        processBoxinfo("processbox.kaskade.naechste.runde", naechsteRundeNr);

        meldeListe.upDateSheet();
        var meldungenNachSP = meldeListe.getMeldungenSortiertNachSetzposition();

        if (meldungenNachSP.size() < 4) {
            MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK)
                    .caption(I18n.get("msg.caption.aktuelle.spielrunde.fehler"))
                    .message(I18n.get("kaskade.spielrunde.fehler.zu.wenige.teams"))
                    .show();
            return;
        }

        var plan  = KaskadenKoRundenPlaner.berechne(meldungenNachSP.size(), anzahlKaskaden);
        var runde = plan.kaskadeRunden().get(naechsteRundeNr - 1);

        aktuelleRundeNr = naechsteRundeNr;
        var sheetName  = SheetNamen.kaskadenRunde(naechsteRundeNr);
        var schluessel = SheetMetadataHelper.schluesselKaskadenRunde(naechsteRundeNr);

        var erstellt = NewSheet.from(this, sheetName, schluessel)
                .pos(DefaultSheetPos.KASKADE_WORK)
                .setForceCreate(isForceOk())
                .tabColor(konfigurationSheet.getSpielrundeTabFarbe())
                .setActiv()
                .hideGrid()
                .create()
                .isDidCreate();

        if (!erstellt) {
            processBoxinfo("schweizer.spielrunde.abbruch");
            return;
        }

        spielrundeEintragen(runde, naechsteRundeNr == 1, meldungenNachSP);
        headerFormatieren(naechsteRundeNr);
        datenFormatieren(runde);
        druckBereichSetzen();

        konfigurationSheet.setAktiveKaskadenRunde(naechsteRundeNr);
    }

    // ---------------------------------------------------------------
    // Daten eintragen
    // ---------------------------------------------------------------

    private void spielrundeEintragen(KaskadenKoRunde runde, boolean ersteRunde, TeamMeldungen meldungenNachSP)
            throws GenerateException {
        processBoxinfo("processbox.neue.spielrunde", runde.rundenNr());

        var datenBlock    = new RangeData();
        var gruppeBlocks  = new ArrayList<GruppeBlock>();
        int laufendeNr         = 1;
        int aktuelleDatenZeile = ERSTE_DATEN_ZEILE;

        for (var gruppenRunde : runde.gruppenRunden()) {
            SheetRunner.testDoCancelTask();
            int gruppeStart = aktuelleDatenZeile;

            for (var spiel : gruppenRunde.spielPaare()) {
                var zeile = datenBlock.addNewRow();
                zeile.add(new CellData(laufendeNr));
                zeile.add(teamCellData(gruppenRunde.pfad(), spiel.positionA(), ersteRunde, meldungenNachSP));
                zeile.add(teamCellData(gruppenRunde.pfad(), spiel.positionB(), ersteRunde, meldungenNachSP));
                zeile.add(new CellData(""));
                zeile.add(new CellData(""));
                laufendeNr++;
                aktuelleDatenZeile++;
            }

            if (gruppenRunde.anzFreilose() > 0) {
                var freilosZeile = datenBlock.addNewRow();
                freilosZeile.add(new CellData(laufendeNr));
                freilosZeile.add(teamCellData(gruppenRunde.pfad(), gruppenRunde.anzTeams(), ersteRunde, meldungenNachSP));
                freilosZeile.add(new CellData(""));  // kein Gegner = Freilos
                freilosZeile.add(new CellData(""));
                freilosZeile.add(new CellData(""));
                laufendeNr++;
                aktuelleDatenZeile++;
            }

            gruppeBlocks.add(new GruppeBlock(gruppenRunde.pfad(), gruppeStart, aktuelleDatenZeile - 1));
        }

        var datenStart = Position.from(SPIEL_NR_SPALTE, ERSTE_DATEN_ZEILE);
        RangeHelper.from(this, datenBlock.getRangePosition(datenStart)).setDataInRange(datenBlock);

        gruppenLabelsSchreiben(gruppeBlocks);
    }

    private CellData teamCellData(String pfad, int position, boolean ersteRunde, TeamMeldungen meldungenNachSP) {
        if (ersteRunde) {
            int idx = position - 1;
            if (idx >= 0 && idx < meldungenNachSP.size()) {
                return new CellData(meldungenNachSP.teams().get(idx).getNr());
            }
            return new CellData(position);
        }
        var label = pfad.isEmpty() ? String.valueOf(position) : pfad + "-" + position;
        return new CellData(label);
    }

    /**
     * Schreibt die Gruppen-Label-Zellen in der GRUPPE_SPALTE.
     * Pro Gruppe wird eine vertikal gemergte Zelle geschrieben.
     */
    private void gruppenLabelsSchreiben(List<GruppeBlock> gruppeBlocks) throws GenerateException {
        var sheet = getXSpreadSheet();
        for (var block : gruppeBlocks) {
            var label     = block.pfad().isEmpty() ? I18n.get("kaskade.gruppe.alle") : block.pfad();
            var cellValue = StringCellValue.from(sheet, Position.from(GRUPPE_SPALTE, block.startZeile()))
                    .setValue(label)
                    .setVertJustify(CellVertJustify2.CENTER)
                    .centerHoriJustify()
                    .setCharHeight(CHARHEIGHT_NR)
                    .setBorder(BorderFactory.from().allThin().toBorder());
            if (block.endZeile() > block.startZeile()) {
                cellValue.setEndPosMergeZeile(block.endZeile());
            }
            getSheetHelper().setStringValueInCell(cellValue);
        }
    }

    // ---------------------------------------------------------------
    // Formatierung
    // ---------------------------------------------------------------

    private void headerFormatieren(int rundeNr) throws GenerateException {
        processBoxinfo("processbox.formatiere.header");
        var sheet       = getXSpreadSheet();
        var headerFarbe = konfigurationSheet.getMeldeListeHeaderFarbe();

        var headerValue = StringCellValue.from(sheet, Position.from(GRUPPE_SPALTE, HEADER_ZEILE))
                .setValue(SheetNamen.kaskadenRunde(rundeNr))
                .setVertJustify(CellVertJustify2.CENTER)
                .centerHoriJustify()
                .setCharHeight(14)
                .setCellBackColor(headerFarbe)
                .setEndPosMergeSpaltePlus(ERG_TEAM_B_SPALTE)
                .setBorder(BorderFactory.from().allThin().boldLn().forBottom().toBorder());
        getSheetHelper().setStringValueInCell(headerValue);

        getSheetHelper().setColumnWidth(sheet, GRUPPE_SPALTE,     1400);
        getSheetHelper().setColumnWidth(sheet, SPIEL_NR_SPALTE,    700);
        getSheetHelper().setColumnWidth(sheet, TEAM_A_SPALTE,     2500);
        getSheetHelper().setColumnWidth(sheet, TEAM_B_SPALTE,     2500);
        getSheetHelper().setColumnWidth(sheet, ERG_TEAM_A_SPALTE, 1500);
        getSheetHelper().setColumnWidth(sheet, ERG_TEAM_B_SPALTE, 1500);
        getSheetHelper().setColumnWidth(sheet, FEHLER_SPALTE,     1800);
    }

    private void datenFormatieren(KaskadenKoRunde runde) throws GenerateException {
        processBoxinfo("processbox.formatiere.daten");
        var sheet       = getXSpreadSheet();
        int letzteZeile = letzteZeileMitDaten();
        if (letzteZeile < ERSTE_DATEN_ZEILE) {
            return;
        }

        var datenRange = RangePosition.from(GRUPPE_SPALTE, ERSTE_DATEN_ZEILE, ERG_TEAM_B_SPALTE, letzteZeile);
        TableBorder2 border = BorderFactory.from().allThin().toBorder();
        getSheetHelper().setPropertyInRange(sheet, datenRange, TABLE_BORDER2, border);
        getSheetHelper().setPropertyInRange(sheet, datenRange, HORI_JUSTIFY, CellHoriJustify.CENTER);
        getSheetHelper().setPropertyInRange(sheet, datenRange, VERT_JUSTIFY, CellVertJustify2.CENTER);

        var nrRange = RangePosition.from(SPIEL_NR_SPALTE, ERSTE_DATEN_ZEILE, SPIEL_NR_SPALTE, letzteZeile);
        getSheetHelper().setPropertyInRange(sheet, nrRange, CHAR_HEIGHT, CHARHEIGHT_NR);

        var teamRange = RangePosition.from(TEAM_A_SPALTE, ERSTE_DATEN_ZEILE, TEAM_B_SPALTE, letzteZeile);
        getSheetHelper().setPropertyInRange(sheet, teamRange, CHAR_HEIGHT, CHARHEIGHT_TEAM);
        getSheetHelper().setPropertyInRange(sheet, teamRange, CHAR_WEIGHT, FontWeight.BOLD);

        var ergRange = RangePosition.from(ERG_TEAM_A_SPALTE, ERSTE_DATEN_ZEILE, ERG_TEAM_B_SPALTE, letzteZeile);
        getSheetHelper().setPropertyInRange(sheet, ergRange, CHAR_HEIGHT, CHARHEIGHT_ERGEBNIS);
        getSheetHelper().setPropertyInRange(sheet, ergRange, CHAR_WEIGHT, FontWeight.BOLD);

        var zebraRange = RangePosition.from(SPIEL_NR_SPALTE, ERSTE_DATEN_ZEILE, ERG_TEAM_B_SPALTE, letzteZeile);
        SheetHelper.faerbeZeilenAbwechselnd(this, zebraRange,
                BasePropertiesSpalte.DEFAULT_GERADE_BACK_COLOR,
                BasePropertiesSpalte.DEFAULT_UNGERADE_BACK_COLOR);

        fehlerSpalteFormatieren(letzteZeile);
        gruppentrennlinienSetzen(runde);
    }

    private void fehlerSpalteFormatieren(int letzteZeile) throws GenerateException {
        var sheet = getXSpreadSheet();
        getSheetHelper().setColumnWidth(sheet, FEHLER_SPALTE, 1800);

        var ergA  = Position.from(ERG_TEAM_A_SPALTE, ERSTE_DATEN_ZEILE).getAddress();
        var ergB  = Position.from(ERG_TEAM_B_SPALTE, ERSTE_DATEN_ZEILE).getAddress();
        var teamB = Position.from(TEAM_B_SPALTE, ERSTE_DATEN_ZEILE).getAddress();

        // @formatter:off
        var formel =
                "IF(AND(NOT(ISBLANK(" + teamB + "));OR(" +
                "AND(" + ergA + "=0;" + ergB + "=0);" +
                "OR(" + ergA + ">13;" + ergB + ">13);" +
                "AND(NOT(ISBLANK(" + ergA + "));NOT(ISBLANK(" + ergB + "));" + ergA + "=" + ergB + ")" +
                "));" +
                "\"" + I18n.get("schweizer.spielrunde.fehler.formel") + "\";\"\"" +
                ")";
        // @formatter:on

        var fehlerValue = StringCellValue.from(sheet, Position.from(FEHLER_SPALTE, ERSTE_DATEN_ZEILE), formel)
                .setCharColor(ColorHelper.CHAR_COLOR_RED)
                .setFillAutoDown(letzteZeile);
        getSheetHelper().setFormulaInCell(fehlerValue);
    }

    /**
     * Setzt nach jeder Gruppe (außer der letzten) eine dicke Trennlinie.
     */
    private void gruppentrennlinienSetzen(KaskadenKoRunde runde) throws GenerateException {
        var sheet        = getXSpreadSheet();
        int aktuellZeile = ERSTE_DATEN_ZEILE;
        var gruppen      = runde.gruppenRunden();

        for (int i = 0; i < gruppen.size() - 1; i++) {
            var gruppe    = gruppen.get(i);
            int anzZeilen = gruppe.spielPaare().size() + gruppe.anzFreilose();
            int trennZeile = aktuellZeile + anzZeilen - 1;

            var trennRange = RangePosition.from(GRUPPE_SPALTE, trennZeile, ERG_TEAM_B_SPALTE, trennZeile);
            getSheetHelper().setPropertyInRange(sheet, trennRange, TABLE_BORDER2,
                    BorderFactory.from().allThin().boldLn().forBottom().toBorder());

            aktuellZeile += anzZeilen;
        }
    }

    private void druckBereichSetzen() throws GenerateException {
        processBoxinfo("processbox.print.bereich");
        var xSheet = getXSpreadSheet();
        if (xSheet == null) {
            return;
        }
        int letzteZeile = letzteZeileMitDaten();
        if (letzteZeile < ERSTE_DATEN_ZEILE) {
            return;
        }
        var druckBereich = RangePosition.from(GRUPPE_SPALTE, HEADER_ZEILE, ERG_TEAM_B_SPALTE, letzteZeile);
        PrintArea.from(xSheet, getWorkingSpreadsheet()).setPrintArea(druckBereich);
    }

    // ---------------------------------------------------------------
    // Hilfsmethoden
    // ---------------------------------------------------------------

    private int letzteZeileMitDaten() throws GenerateException {
        var readRange = RangePosition.from(SPIEL_NR_SPALTE, ERSTE_DATEN_ZEILE, SPIEL_NR_SPALTE,
                ERSTE_DATEN_ZEILE + 999);
        var nrDaten = RangeHelper.from(this, readRange).getDataFromRange();
        for (int i = 0; i < nrDaten.size(); i++) {
            if (nrDaten.get(i).get(0).getIntVal(0) <= 0) {
                return ERSTE_DATEN_ZEILE + i - 1;
            }
        }
        return ERSTE_DATEN_ZEILE + nrDaten.size() - 1;
    }

    /**
     * Prüft ob in der Kaskadenrunde {@code rundeNr} alle Spiele (außer Freilose) ein Ergebnis haben.
     */
    private boolean alleErgebnisseEingetragen(int rundeNr) throws GenerateException {
        var xDoc  = getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
        var sheet = SheetMetadataHelper.findeSheetUndHeile(xDoc,
                SheetMetadataHelper.schluesselKaskadenRunde(rundeNr), SheetNamen.kaskadenRunde(rundeNr));
        if (sheet == null) {
            return true;
        }

        // Lese SPIEL_NR..ERG_TEAM_B (relative Offsets: 0=NR, 1=TeamA, 2=TeamB, 3=ErgA, 4=ErgB)
        var readRange = RangePosition.from(SPIEL_NR_SPALTE, ERSTE_DATEN_ZEILE, ERG_TEAM_B_SPALTE,
                ERSTE_DATEN_ZEILE + 999);
        var rowsData = RangeHelper.from(sheet, xDoc, readRange).getDataFromRange();

        for (var row : rowsData) {
            if (row.size() < 2 || row.get(0).getIntVal(0) <= 0) {
                break; // Ende der Daten
            }
            // Freilos-Zeile: Team B leer
            var teamBStr = row.get(2).getStringVal();
            if (teamBStr == null || teamBStr.isEmpty()) {
                continue;
            }
            int ergA = row.size() > 3 ? row.get(3).getIntVal(0) : 0;
            int ergB = row.size() > 4 ? row.get(4).getIntVal(0) : 0;
            if (ergA == 0 && ergB == 0) {
                return false;
            }
        }
        return true;
    }

    public boolean isForceOk() {
        return forceOk;
    }

    public void setForceOk(boolean forceOk) {
        this.forceOk = forceOk;
    }

    // ---------------------------------------------------------------
    // Interner Hilfsdatensatz
    // ---------------------------------------------------------------

    private record GruppeBlock(String pfad, int startZeile, int endZeile) {}
}
