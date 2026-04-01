package de.petanqueturniermanager.schweizer.meldeliste;

import com.sun.star.awt.FontWeight;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeHelper;
import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ColorHelper;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.print.PrintArea;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerKonfigurationSheet;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Bereinigte Teilnehmerliste für das Schweizer Turniersystem – als Aushang und Webseite.
 * Listet alle aktiven Teams in einem mehrspaltigem Raster auf.
 */
public class TeilnehmerSheet extends SheetRunner implements ISheet {

    public static final int ERSTE_DATEN_ZEILE = 1;
    public static final int TEAM_NR_SPALTE = 0;
    public static final int TEAM_NAME_SPALTE = 1;
    public static final int ANZAHL_SPALTEN = 3; // nr + name + leer

    private static final String SHEET_COLOR = "4a8fc4";
    private static final int TEAM_NAME_SPALTE_WIDTH = 6000;

    private final SchweizerKonfigurationSheet konfigurationSheet;
    private final SchweizerMeldeListeSheetUpdate meldeliste;

    public TeilnehmerSheet(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet, TurnierSystem.SCHWEIZER, "Schweizer-Teilnehmer");
        konfigurationSheet = new SchweizerKonfigurationSheet(workingSpreadsheet);
        meldeliste = new SchweizerMeldeListeSheetUpdate(workingSpreadsheet);
    }

    @Override
    protected SchweizerKonfigurationSheet getKonfigurationSheet() {
        return konfigurationSheet;
    }

    @Override
    public XSpreadsheet getXSpreadSheet() throws GenerateException {
        return SheetMetadataHelper.findeSheetUndHeile(
                getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
                SheetMetadataHelper.SCHLUESSEL_TEILNEHMER,
                SheetNamen.schweizerTeilnehmer());
    }

    @Override
    public final TurnierSheet getTurnierSheet() throws GenerateException {
        return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
    }

    @Override
    protected void doRun() throws GenerateException {
        meldeliste.upDateSheet();
        generate();
    }

    public void generate() throws GenerateException {
        NewSheet.from(this, SheetNamen.schweizerTeilnehmer(), SheetMetadataHelper.SCHLUESSEL_TEILNEHMER)
                .tabColor(SHEET_COLOR).pos(DefaultSheetPos.SCHWEIZER_WORK)
                .forceCreate().hideGrid().setActiv().create();

        processBoxinfo("processbox.teilnehmer.meldungen.einlesen");
        meldeliste.doSort(meldeliste.getSpielerNameErsteSpalte(), true);
        TeamMeldungen aktiveMeldungen = meldeliste.getAktiveMeldungen();

        if (aktiveMeldungen.size() == 0) {
            MessageBox.from(getWorkingSpreadsheet(), MessageBoxTypeEnum.ERROR_OK)
                    .caption(I18n.get("msg.caption.teilnehmer.fehler"))
                    .message(I18n.get("msg.text.keine.meldungen")).show();
            return;
        }

        ColumnProperties celPropNr = ColumnProperties.from().setHoriJustify(CellHoriJustify.CENTER)
                .setWidth(MeldungenSpalte.DEFAULT_SPALTE_NUMBER_WIDTH);
        NumberCellValue teamNrVal = NumberCellValue.from(getXSpreadSheet(), Position.from(TEAM_NR_SPALTE, ERSTE_DATEN_ZEILE))
                .setBorder(BorderFactory.from().allThin().toBorder()).setCharColor(ColorHelper.CHAR_COLOR_GRAY_SPIELER_NR);

        ColumnProperties celPropName = ColumnProperties.from().setHoriJustify(CellHoriJustify.CENTER)
                .setWidth(TEAM_NAME_SPALTE_WIDTH);
        StringCellValue nameFormula = StringCellValue.from(getXSpreadSheet(), Position.from(TEAM_NAME_SPALTE, ERSTE_DATEN_ZEILE))
                .setBorder(BorderFactory.from().allThin().toBorder()).setShrinkToFit(true);

        int teamCntr = 1;
        int maxAnzTeamsInSpalte = 0;
        int maxAnzTeilnehmerInSpalte = konfigurationSheet.getMaxAnzTeilnehmerInSpalte();
        spalteFormat(teamNrVal, celPropNr, nameFormula, celPropName);

        processBoxinfo("processbox.teilnehmer.meldungen.einfuegen", aktiveMeldungen.size());

        for (Team team : aktiveMeldungen.getTeamList()) {
            teamNrVal.setValue((double) team.getNr());
            nameFormula.setValue(MeldeListeHelper.teamNameVlookup(teamNrVal.getPos().getAddress()));

            getSheetHelper().setNumberValueInCell(teamNrVal);
            getSheetHelper().setFormulaInCell(nameFormula);

            teamNrVal.zeilePlusEins();
            nameFormula.zeilePlusEins();

            if ((teamCntr / maxAnzTeilnehmerInSpalte) * maxAnzTeilnehmerInSpalte == teamCntr) {
                teamNrVal.spalte((teamCntr / maxAnzTeilnehmerInSpalte) * ANZAHL_SPALTEN).zeile(ERSTE_DATEN_ZEILE);
                nameFormula.spalte(teamNrVal.getPos().getSpalte() + 1).zeile(ERSTE_DATEN_ZEILE);
                spalteFormat(teamNrVal, celPropNr, nameFormula, celPropName);
            }
            teamCntr++;
            if (maxAnzTeamsInSpalte < maxAnzTeilnehmerInSpalte) {
                maxAnzTeamsInSpalte++;
            }
        }

        int letzteSpalte = nameFormula.getPos().getSpalte();

        headerSchreiben(letzteSpalte);
        zebrafarbenSchreiben(letzteSpalte, maxAnzTeamsInSpalte);

        StringCellValue footer = StringCellValue.from(getXSpreadSheet(),
                Position.from(TEAM_NR_SPALTE, ERSTE_DATEN_ZEILE + maxAnzTeamsInSpalte)).zeilePlusEins()
                .setValue(I18n.get("teilnehmer.footer.anzahl", aktiveMeldungen.size()))
                .setEndPosMergeSpalte(letzteSpalte).setCharWeight(FontWeight.BOLD).setCharHeight(12)
                .setShrinkToFit(true);
        getSheetHelper().setStringValueInCell(footer);
        printBereichDefinieren(footer.getPos(), letzteSpalte);
    }

    private void headerSchreiben(int letzteSpalte) throws GenerateException {
        var headerFarbe = konfigurationSheet.getMeldeListeHeaderFarbe();
        int anzahlBloecke = letzteSpalte / ANZAHL_SPALTEN + 1;
        for (int block = 0; block < anzahlBloecke; block++) {
            int nrSpalte = block * ANZAHL_SPALTEN + TEAM_NR_SPALTE;
            int nameSpalte = nrSpalte + 1;
            getSheetHelper().setStringValueInCell(StringCellValue
                    .from(getXSpreadSheet(), Position.from(nrSpalte, 0), I18n.get("column.header.nr"))
                    .setBorder(BorderFactory.from().allThin().toBorder())
                    .setCellBackColor(headerFarbe)
                    .setHoriJustify(CellHoriJustify.CENTER));
            getSheetHelper().setStringValueInCell(StringCellValue
                    .from(getXSpreadSheet(), Position.from(nameSpalte, 0), I18n.get("column.header.name"))
                    .setBorder(BorderFactory.from().allThin().toBorder())
                    .setCellBackColor(headerFarbe)
                    .setHoriJustify(CellHoriJustify.CENTER));
        }
    }

    private void zebrafarbenSchreiben(int letzteSpalte, int anzahlZeilen) throws GenerateException {
        if (anzahlZeilen <= 0) {
            return;
        }
        var geradeFarbe = konfigurationSheet.getMeldeListeHintergrundFarbeGerade();
        var ungeradeFarbe = konfigurationSheet.getMeldeListeHintergrundFarbeUnGerade();
        var datenRange = RangePosition.from(TEAM_NR_SPALTE, ERSTE_DATEN_ZEILE,
                letzteSpalte, ERSTE_DATEN_ZEILE + anzahlZeilen - 1);
        SheetHelper.faerbeZeilenAbwechselnd(this, datenRange, geradeFarbe, ungeradeFarbe);
    }

    private void printBereichDefinieren(Position footerPos, int letzteSpalte) throws GenerateException {
        processBoxinfo("processbox.print.bereich");
        Position linksOben = Position.from(TEAM_NR_SPALTE, 0);
        Position rechtsUnten = Position.from(letzteSpalte, footerPos.getZeile());
        PrintArea.from(getXSpreadSheet(), getWorkingSpreadsheet()).setPrintArea(RangePosition.from(linksOben, rechtsUnten));
    }

    private void spalteFormat(NumberCellValue nrVal, ColumnProperties celPropNr,
            StringCellValue nameVal, ColumnProperties celPropName) throws GenerateException {
        getSheetHelper().setColumnProperties(getXSpreadSheet(), nrVal.getPos().getSpalte(), celPropNr);
        getSheetHelper().setColumnProperties(getXSpreadSheet(), nameVal.getPos().getSpalte(), celPropName);
        getSheetHelper().setColumnProperties(getXSpreadSheet(), nameVal.getPos().getSpalte() + 1, celPropNr);
    }
}
