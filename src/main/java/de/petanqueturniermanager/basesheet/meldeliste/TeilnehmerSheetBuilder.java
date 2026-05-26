package de.petanqueturniermanager.basesheet.meldeliste;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import com.sun.star.sheet.XPrintAreas;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellRangeAddress;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.print.PrintArea;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetFreeze;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;

/**
 * Zentraler Builder für alle Teilnehmer-Sheets der Turniersysteme.
 * Erzeugt einen einheitlichen mehrspaltigen Block mit Header, Daten, Zebra-Farben,
 * Freeze des Header und Druckbereich inkl. Wiederholungs-Header-Zeile.
 *
 * Layout pro Block (dynamisch je nach Teamname-Property):
 * <ul>
 * <li>{@code teamnameAktiv = false}: Nr · Spieler · (Leerspalte)</li>
 * <li>{@code teamnameAktiv = true}:  Nr · Teamname · Spieler · (Leerspalte)</li>
 * </ul>
 */
public final class TeilnehmerSheetBuilder {

    /**
     * Ein Eintrag der Teilnehmerliste. {@code sortNachname} (Nachname von Spieler 1) dient
     * ausschließlich der Sortierung und wird nicht angezeigt.
     */
    public record TeilnehmerEintrag(int nr, String teamname, String spielerNamen, String sortNachname) {
    }

    public static final int ERSTE_DATEN_ZEILE = 1;
    public static final int HEADER_ZEILE = 0;

    private static final int NR_SPALTE_WIDTH = MeldungenSpalte.DEFAULT_SPALTE_NUMBER_WIDTH;
    private static final int TEAMNAME_SPALTE_WIDTH = 4000;
    private static final int SPIELER_SPALTE_DEFAULT_WIDTH = 6000;

    private final ISheet sheet;
    private List<TeilnehmerEintrag> daten = List.of();
    private boolean teamnameAktiv = false;
    private int maxProBlock = 40;
    private int spielerSpalteWidth = SPIELER_SPALTE_DEFAULT_WIDTH;
    private int headerFarbe = 0xC0C0C0;
    private int geradeFarbe = 0xFFFFFF;
    private int ungeradeFarbe = 0xE0E0E0;
    private String spielerHeaderKey = "column.header.spieler";

    private int letzteDatenZeile = ERSTE_DATEN_ZEILE - 1;
    private int letzteDatenSpalte = -1;
    private boolean aufgebaut = false;

    private TeilnehmerSheetBuilder(ISheet sheet) {
        this.sheet = checkNotNull(sheet);
    }

    public static TeilnehmerSheetBuilder from(ISheet sheet) {
        return new TeilnehmerSheetBuilder(sheet);
    }

    public TeilnehmerSheetBuilder daten(List<TeilnehmerEintrag> data) {
        this.daten = checkNotNull(data);
        return this;
    }

    public TeilnehmerSheetBuilder teamnameAktiv(boolean aktiv) {
        this.teamnameAktiv = aktiv;
        return this;
    }

    public TeilnehmerSheetBuilder maxProBlock(int n) {
        this.maxProBlock = Math.max(1, n);
        return this;
    }

    public TeilnehmerSheetBuilder spielerSpalteWidth(int width) {
        this.spielerSpalteWidth = width;
        return this;
    }

    public TeilnehmerSheetBuilder headerFarbe(int farbe) {
        this.headerFarbe = farbe;
        return this;
    }

    public TeilnehmerSheetBuilder zebraFarben(int gerade, int ungerade) {
        this.geradeFarbe = gerade;
        this.ungeradeFarbe = ungerade;
        return this;
    }

    /** i18n-Key für die Spaltenüberschrift der Spielernamen-Spalte (Default {@code column.header.spieler}). */
    public TeilnehmerSheetBuilder spielerHeaderKey(String i18nKey) {
        this.spielerHeaderKey = checkNotNull(i18nKey);
        return this;
    }

    /** Erstellt Header, Daten, Border, Zebra-Farben und liefert die Zeilennummer der letzten Datenzeile zurück. */
    public TeilnehmerSheetBuilder aufbauen() throws GenerateException {
        int spaltenProBlock = teamnameAktiv ? 4 : 3; // Nr · [Teamname ·] Spieler · Leer
        int datenSpaltenProBlock = teamnameAktiv ? 3 : 2;
        int anzahlEintraege = daten.size();
        int zeilenProBlock = Math.min(maxProBlock, Math.max(anzahlEintraege, 1));
        int anzBloecke = Math.max(1, (anzahlEintraege + maxProBlock - 1) / maxProBlock);
        // letzte Daten-Spalte: keine Leer-Trennspalte hinter dem letzten Block
        int totalSpalten = (anzBloecke - 1) * spaltenProBlock + datenSpaltenProBlock - 1;

        spaltenbreitenSetzen(anzBloecke, spaltenProBlock, datenSpaltenProBlock);
        headerSchreiben(anzBloecke, spaltenProBlock);

        if (anzahlEintraege == 0) {
            this.letzteDatenZeile = HEADER_ZEILE;
            this.letzteDatenSpalte = totalSpalten;
            this.aufgebaut = true;
            return this;
        }

        datenSchreiben(anzBloecke, spaltenProBlock, zeilenProBlock, totalSpalten);
        datenBorderSetzen(anzBloecke, spaltenProBlock, zeilenProBlock);
        zebraFarbenSetzen(anzBloecke, spaltenProBlock, zeilenProBlock);

        this.letzteDatenZeile = ERSTE_DATEN_ZEILE + zeilenProBlock - 1;
        this.letzteDatenSpalte = totalSpalten;
        this.aufgebaut = true;
        return this;
    }

    /** Header beim Scrollen fixieren und Druckbereich + Wiederholungs-Header-Zeile setzen. */
    public TeilnehmerSheetBuilder freezeUndPrintbereich(int letzteZeileImBereich) throws GenerateException {
        if (!aufgebaut) {
            throw new IllegalStateException("aufbauen() muss vor freezeUndPrintbereich() aufgerufen werden");
        }
        SheetFreeze.from(sheet.getXSpreadSheet(), sheet.getWorkingSpreadsheet())
                .anzZeilen(ERSTE_DATEN_ZEILE).doFreeze();

        Position linksOben = Position.from(0, HEADER_ZEILE);
        Position rechtsUnten = Position.from(letzteDatenSpalte, letzteZeileImBereich);
        RangePosition druckBereich = RangePosition.from(linksOben, rechtsUnten);
        PrintArea.from(sheet.getXSpreadSheet(), sheet.getWorkingSpreadsheet()).setPrintArea(druckBereich);

        XPrintAreas printAreas = Lo.qi(XPrintAreas.class, sheet.getXSpreadSheet());
        if (printAreas != null) {
            CellRangeAddress titelZeilen = new CellRangeAddress();
            titelZeilen.Sheet = 0;
            titelZeilen.StartColumn = 0;
            titelZeilen.EndColumn = letzteDatenSpalte;
            titelZeilen.StartRow = HEADER_ZEILE;
            titelZeilen.EndRow = HEADER_ZEILE;
            printAreas.setTitleRows(titelZeilen);
            printAreas.setPrintTitleRows(true);
        }
        return this;
    }

    public int getLetzteDatenZeile() {
        return letzteDatenZeile;
    }

    public int getLetzteDatenSpalte() {
        return letzteDatenSpalte;
    }

    /** Anzahl tatsächlich befüllter Zeilen im längsten Block (für Footer-Positionierung). */
    public int getZeilenImBlock() {
        return letzteDatenZeile - ERSTE_DATEN_ZEILE + 1;
    }

    private void spaltenbreitenSetzen(int anzBloecke, int spaltenProBlock, int datenSpaltenProBlock)
            throws GenerateException {
        SheetHelper sheetHelper = sheet.getSheetHelper();
        var xSheet = sheet.getXSpreadSheet();
        ColumnProperties propNr = ColumnProperties.from().setHoriJustify(CellHoriJustify.CENTER).setWidth(NR_SPALTE_WIDTH);
        ColumnProperties propTeamname = ColumnProperties.from().setHoriJustify(CellHoriJustify.LEFT)
                .setWidth(TEAMNAME_SPALTE_WIDTH);
        ColumnProperties propSpieler = ColumnProperties.from().setHoriJustify(CellHoriJustify.LEFT)
                .setWidth(spielerSpalteWidth);
        ColumnProperties propLeer = ColumnProperties.from().setWidth(NR_SPALTE_WIDTH);

        for (int b = 0; b < anzBloecke; b++) {
            int base = b * spaltenProBlock;
            sheetHelper.setColumnProperties(xSheet, base, propNr);
            if (teamnameAktiv) {
                sheetHelper.setColumnProperties(xSheet, base + 1, propTeamname);
                sheetHelper.setColumnProperties(xSheet, base + 2, propSpieler);
            } else {
                sheetHelper.setColumnProperties(xSheet, base + 1, propSpieler);
            }
            if (b < anzBloecke - 1) {
                sheetHelper.setColumnProperties(xSheet, base + datenSpaltenProBlock, propLeer);
            }
        }
    }

    private void headerSchreiben(int anzBloecke, int spaltenProBlock) throws GenerateException {
        var headerBorder = BorderFactory.from().allThin().boldLn().forBottom().toBorder();
        for (int b = 0; b < anzBloecke; b++) {
            int base = b * spaltenProBlock;
            schreibeHeaderZelle(base, "column.header.nr", headerBorder);
            if (teamnameAktiv) {
                schreibeHeaderZelle(base + 1, "column.header.teamname", headerBorder);
                schreibeHeaderZelle(base + 2, spielerHeaderKey, headerBorder);
            } else {
                schreibeHeaderZelle(base + 1, spielerHeaderKey, headerBorder);
            }
        }
    }

    private void schreibeHeaderZelle(int spalte, String i18nKey,
            com.sun.star.table.TableBorder2 headerBorder) throws GenerateException {
        sheet.getSheetHelper().setStringValueInCell(StringCellValue
                .from(sheet.getXSpreadSheet(), Position.from(spalte, HEADER_ZEILE), I18n.get(i18nKey))
                .setBorder(headerBorder)
                .setCellBackColor(headerFarbe)
                .setHoriJustify(CellHoriJustify.CENTER)
                .setShrinkToFit(true));
    }

    private void datenSchreiben(int anzBloecke, int spaltenProBlock, int zeilenProBlock, int totalSpalten)
            throws GenerateException {
        RangeData rangeData = new RangeData();
        for (int r = 0; r < zeilenProBlock; r++) {
            RowData row = rangeData.addNewRow();
            for (int b = 0; b < anzBloecke; b++) {
                int idx = b * maxProBlock + r;
                fuelleBlockZelle(row, idx);
                if (b < anzBloecke - 1) {
                    row.newEmpty();
                }
            }
        }
        RangePosition datenRange = RangePosition.from(0, ERSTE_DATEN_ZEILE,
                totalSpalten, ERSTE_DATEN_ZEILE + zeilenProBlock - 1);
        RangeHelper.from(sheet, datenRange).setDataInRange(rangeData);
    }

    private void fuelleBlockZelle(RowData row, int idx) {
        if (idx < daten.size()) {
            TeilnehmerEintrag t = daten.get(idx);
            row.newInt(t.nr());
            if (teamnameAktiv) {
                row.newString(t.teamname() != null ? t.teamname() : "");
            }
            row.newString(t.spielerNamen() != null ? t.spielerNamen() : "");
        } else {
            row.newEmpty();
            if (teamnameAktiv) {
                row.newEmpty();
            }
            row.newEmpty();
        }
    }

    private void datenBorderSetzen(int anzBloecke, int spaltenProBlock, int zeilenProBlock) throws GenerateException {
        var dataBorder = BorderFactory.from().allThin().toBorder();
        for (int b = 0; b < anzBloecke; b++) {
            int base = b * spaltenProBlock;
            int letzteSpalteImBlock = teamnameAktiv ? base + 2 : base + 1;
            RangePosition cellRange = RangePosition.from(base, ERSTE_DATEN_ZEILE,
                    letzteSpalteImBlock, ERSTE_DATEN_ZEILE + zeilenProBlock - 1);
            sheet.getSheetHelper().setPropertiesInRange(sheet.getXSpreadSheet(), cellRange,
                    CellProperties.from().setBorder(dataBorder).setShrinkToFit(true));
        }
    }

    /**
     * Zebra-Färbung pro Block separat, damit die Leer-Trennspalten zwischen den Blöcken
     * weiß bleiben und nicht in das Streifenmuster gezogen werden.
     */
    private void zebraFarbenSetzen(int anzBloecke, int spaltenProBlock, int zeilenProBlock) throws GenerateException {
        for (int b = 0; b < anzBloecke; b++) {
            int base = b * spaltenProBlock;
            int letzteSpalteImBlock = teamnameAktiv ? base + 2 : base + 1;
            SheetHelper.faerbeZeilenAbwechselnd(sheet,
                    RangePosition.from(base, ERSTE_DATEN_ZEILE, letzteSpalteImBlock,
                            ERSTE_DATEN_ZEILE + zeilenProBlock - 1),
                    geradeFarbe, ungeradeFarbe);
        }
    }
}
