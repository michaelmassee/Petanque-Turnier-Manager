/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.kaskade.spielrunde;

import static de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties.CHAR_HEIGHT;
import static de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties.CHAR_WEIGHT;
import static de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties.HORI_JUSTIFY;
import static de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties.TABLE_BORDER2;
import static de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties.VERT_JUSTIFY;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.FontWeight;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;
import com.sun.star.table.TableBorder2;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.algorithmen.KaskadenKoFeldInfo;
import de.petanqueturniermanager.algorithmen.KaskadenKoRundenPlan;
import de.petanqueturniermanager.algorithmen.KaskadenKoRundenPlaner;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.print.PrintArea;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.CellData;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.kaskade.konfiguration.KaskadeKonfigurationSheet;
import de.petanqueturniermanager.kaskade.meldeliste.KaskadeMeldeListeSheetUpdate;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Erstellt für jedes Kaskaden-Endfeld (A, B, C, D …) ein KO-Bracket-Sheet.
 * <p>
 * Der Inhalt zeigt Feld-Header (Bezeichner, Teamanzahl), Cadrage-Info wenn nötig,
 * und Positionen für die KO-Runden (Teamnamen werden manuell eingetragen).
 */
public class KaskadeKoFeldSheet extends SheetRunner implements ISheet {

    private static final Logger LOGGER = LogManager.getLogger(KaskadeKoFeldSheet.class);

    // Spalten im KO-Feld-Sheet
    public static final int POS_SPALTE     = 0;  // Position im Feld
    public static final int TEAM_SPALTE    = 1;  // Teamname (manuell)
    public static final int ERG_A_SPALTE   = 2;  // Ergebnis
    public static final int ERG_B_SPALTE   = 3;

    public static final int HEADER_ZEILE      = 0;
    public static final int INFO_ZEILE        = 1;
    public static final int CADRAGE_ZEILE     = 2;  // nur wenn Cadrage nötig
    public static final int ERSTE_DATEN_ZEILE = 3;

    private static final int CHARHEIGHT_HEADER = 16;
    private static final int CHARHEIGHT_DATEN  = 14;

    private final KaskadeKonfigurationSheet konfigurationSheet;
    private final KaskadeMeldeListeSheetUpdate meldeListe;

    /** Das zuletzt erstellte Feld-Sheet (für getXSpreadSheet). */
    private String letzterBezeichner;

    /** Wird für Tests gesetzt, damit forceCreate aktiviert wird. */
    private boolean forceOk;

    public KaskadeKoFeldSheet(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet, TurnierSystem.KASKADE, "Kaskaden-KO-Feld");
        konfigurationSheet = new KaskadeKonfigurationSheet(workingSpreadsheet);
        meldeListe = new KaskadeMeldeListeSheetUpdate(workingSpreadsheet);
    }

    @Override
    public XSpreadsheet getXSpreadSheet() throws GenerateException {
        if (letzterBezeichner == null) {
            return null;
        }
        return SheetMetadataHelper.findeSheetUndHeile(
                getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
                SheetMetadataHelper.schluesselKaskadenFeld(letzterBezeichner),
                SheetNamen.kaskadenFeld(letzterBezeichner));
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
        processBoxinfo("processbox.kaskade.ko.felder.erstellen");

        meldeListe.upDateSheet();
        var meldungenNachSP = meldeListe.getMeldungenSortiertNachSetzposition();
        int gesamtTeams     = meldungenNachSP.size();

        KaskadenKoRundenPlan plan = KaskadenKoRundenPlaner.berechne(
                gesamtTeams, konfigurationSheet.getAnzahlKaskaden());

        for (var feld : plan.felder()) {
            SheetRunner.testDoCancelTask();
            feldSheetErstellen(feld);
        }
    }

    // ---------------------------------------------------------------
    // Feld-Sheet erstellen
    // ---------------------------------------------------------------

    private void feldSheetErstellen(KaskadenKoFeldInfo feld) throws GenerateException {
        if (feld.gesamtTeams() < 2) {
            LOGGER.info("Feld {} hat weniger als 2 Teams ({}), wird übersprungen.",
                    feld.bezeichner(), feld.gesamtTeams());
            return;
        }

        letzterBezeichner = feld.bezeichner();
        var sheetName  = SheetNamen.kaskadenFeld(feld.bezeichner());
        var schluessel = SheetMetadataHelper.schluesselKaskadenFeld(feld.bezeichner());
        var tabFarbe   = konfigurationSheet.getKaskadenTabFarbe();

        var erstellt = NewSheet.from(this, sheetName, schluessel)
                .pos(DefaultSheetPos.KASKADE_FELDER)
                .setForceCreate(isForceOk())
                .tabColor(tabFarbe)
                .setActiv()
                .hideGrid()
                .create()
                .isDidCreate();

        if (!erstellt) {
            return;
        }

        headerSchreiben(feld);
        positionslisteSchreiben(feld);
        datenFormatieren(feld);
        druckBereichSetzen();
    }

    private void headerSchreiben(KaskadenKoFeldInfo feld) throws GenerateException {
        var sheet       = getXSpreadSheet();
        var headerFarbe = konfigurationSheet.getMeldeListeHeaderFarbe();
        var sheetName   = SheetNamen.kaskadenFeld(feld.bezeichner());

        // Hauptheader: Feldname
        var hauptHeader = StringCellValue.from(sheet, Position.from(POS_SPALTE, HEADER_ZEILE))
                .setValue(sheetName)
                .setVertJustify(CellVertJustify2.CENTER)
                .centerHoriJustify()
                .setCharHeight(CHARHEIGHT_HEADER)
                .setCharWeight(FontWeight.BOLD)
                .setCellBackColor(headerFarbe)
                .setEndPosMergeSpaltePlus(ERG_B_SPALTE)
                .setBorder(BorderFactory.from().allThin().boldLn().forBottom().toBorder());
        getSheetHelper().setStringValueInCell(hauptHeader);

        // Info-Zeile: Teams-Anzahl
        var infoHeader = StringCellValue.from(sheet, Position.from(POS_SPALTE, INFO_ZEILE))
                .setValue(I18n.get("kaskade.feld.info.header", feld.bezeichner(), feld.gesamtTeams()))
                .setVertJustify(CellVertJustify2.CENTER)
                .centerHoriJustify()
                .setCharHeight(CHARHEIGHT_DATEN)
                .setCellBackColor(headerFarbe)
                .setEndPosMergeSpaltePlus(ERG_B_SPALTE)
                .setBorder(BorderFactory.from().allThin().toBorder());
        getSheetHelper().setStringValueInCell(infoHeader);

        // Cadrage-Info (wenn nötig)
        if (feld.isCadrageNoetig()) {
            var cadrageInfo = StringCellValue.from(sheet, Position.from(POS_SPALTE, CADRAGE_ZEILE))
                    .setValue(I18n.get("kaskade.feld.cadrage.info",
                            feld.anzCadrageSpiele(), feld.anzFreilose(), feld.zielTeams()))
                    .setVertJustify(CellVertJustify2.CENTER)
                    .centerHoriJustify()
                    .setCharHeight(CHARHEIGHT_DATEN)
                    .setCellBackColor(headerFarbe)
                    .setEndPosMergeSpaltePlus(ERG_B_SPALTE)
                    .setBorder(BorderFactory.from().allThin().toBorder());
            getSheetHelper().setStringValueInCell(cadrageInfo);
        }
    }

    private void positionslisteSchreiben(KaskadenKoFeldInfo feld) throws GenerateException {
        var datenBlock = new RangeData();

        for (int pos = 1; pos <= feld.gesamtTeams(); pos++) {
            var zeile = datenBlock.addNewRow();
            zeile.add(new CellData(pos));   // POS_SPALTE
            zeile.add(new CellData(""));    // TEAM_SPALTE (manuell eintragen)
            zeile.add(new CellData(""));    // ERG_A
            zeile.add(new CellData(""));    // ERG_B
        }

        var datenStart = Position.from(POS_SPALTE, ERSTE_DATEN_ZEILE);
        RangeHelper.from(this, datenBlock.getRangePosition(datenStart)).setDataInRange(datenBlock);
    }

    private void datenFormatieren(KaskadenKoFeldInfo feld) throws GenerateException {
        var sheet       = getXSpreadSheet();
        int letzteZeile = ERSTE_DATEN_ZEILE + feld.gesamtTeams() - 1;

        var datenRange = RangePosition.from(POS_SPALTE, ERSTE_DATEN_ZEILE, ERG_B_SPALTE, letzteZeile);
        TableBorder2 border = BorderFactory.from().allThin().toBorder();
        getSheetHelper().setPropertyInRange(sheet, datenRange, TABLE_BORDER2, border);
        getSheetHelper().setPropertyInRange(sheet, datenRange, HORI_JUSTIFY, CellHoriJustify.CENTER);
        getSheetHelper().setPropertyInRange(sheet, datenRange, VERT_JUSTIFY, CellVertJustify2.CENTER);
        getSheetHelper().setPropertyInRange(sheet, datenRange, CHAR_HEIGHT, CHARHEIGHT_DATEN);

        // Team-Spalte: linksbündig, breiter
        var teamRange = RangePosition.from(TEAM_SPALTE, ERSTE_DATEN_ZEILE, TEAM_SPALTE, letzteZeile);
        getSheetHelper().setPropertyInRange(sheet, teamRange, HORI_JUSTIFY, CellHoriJustify.LEFT);
        getSheetHelper().setPropertyInRange(sheet, teamRange, CHAR_WEIGHT, FontWeight.BOLD);

        getSheetHelper().setColumnWidth(sheet, POS_SPALTE,   700);
        getSheetHelper().setColumnWidth(sheet, TEAM_SPALTE, 5000);
        getSheetHelper().setColumnWidth(sheet, ERG_A_SPALTE, 1500);
        getSheetHelper().setColumnWidth(sheet, ERG_B_SPALTE, 1500);
    }

    public boolean isForceOk() {
        return forceOk;
    }

    public void setForceOk(boolean forceOk) {
        this.forceOk = forceOk;
    }

    private void druckBereichSetzen() throws GenerateException {
        processBoxinfo("processbox.print.bereich");
        var xSheet = getXSpreadSheet();
        if (xSheet == null) {
            return;
        }
        // Ermittle letzte Datenzeile aus POS_SPALTE
        var readRange = RangePosition.from(POS_SPALTE, ERSTE_DATEN_ZEILE, POS_SPALTE, ERSTE_DATEN_ZEILE + 999);
        var posData   = RangeHelper.from(xSheet, getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), readRange)
                .getDataFromRange();
        int letzteZeile = ERSTE_DATEN_ZEILE - 1;
        for (int i = 0; i < posData.size(); i++) {
            if (posData.get(i).get(0).getIntVal(0) > 0) {
                letzteZeile = ERSTE_DATEN_ZEILE + i;
            } else {
                break;
            }
        }
        if (letzteZeile < ERSTE_DATEN_ZEILE) {
            return;
        }
        var druckBereich = RangePosition.from(POS_SPALTE, HEADER_ZEILE, ERG_B_SPALTE, letzteZeile);
        PrintArea.from(xSheet, getWorkingSpreadsheet()).setPrintArea(druckBereich);
    }
}
