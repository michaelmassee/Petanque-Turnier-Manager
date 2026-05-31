package de.petanqueturniermanager.triptete.rangliste;

import com.google.common.annotations.VisibleForTesting;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.awt.FontWeight;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.RangeProperties;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.print.PrintArea;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.RanglisteGeradeUngeradeFormatHelper;
import de.petanqueturniermanager.helper.sheet.SheetFreeze;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.triptete.konfiguration.TripTeteKonfigurationSheet;
import de.petanqueturniermanager.triptete.meldeliste.TripTeteMeldeListeSheetUpdate;

/**
 * Trip-Tête-Rangliste – schreibt pro Team Begegnungssiege (Punkte), Partiensiege (Siege),
 * SpPunkte+ und SpPunkte-Differenz direkt als Werte (kein SUMIF).
 */
public class TripTeteRanglisteSheet extends SheetRunner implements ISheet {

	private static final String METADATA_SCHLUESSEL = SheetMetadataHelper.SCHLUESSEL_TRIPTETE_RANGLISTE;

	public static final int ERSTE_DATEN_ZEILE = 1;

	public static final int TEAM_NR_SPALTE    = 0;
	public static final int NAME_SPALTE       = 1;
	public static final int RANG_SPALTE       = 2;
	public static final int PUNKTE_SPALTE     = 3;
	public static final int SIEGE_SPALTE      = 4;
	public static final int SP_PUNKTE_SPALTE  = 5;
	public static final int SP_PUNKTE_DIFF_SPALTE = 6;
	public static final int LETZTE_SPALTE         = SP_PUNKTE_DIFF_SPALTE;

	private final TripTeteKonfigurationSheet konfigurationSheet;
	private final TripTeteMeldeListeSheetUpdate meldeListe;

	public TripTeteRanglisteSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.TRIPTETE, "Trip-Tête-RanglisteSheet");
		konfigurationSheet = new TripTeteKonfigurationSheet(workingSpreadsheet);
		meldeListe = initMeldeListeSheet(workingSpreadsheet);
	}

	@Override
	protected TripTeteKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

	@VisibleForTesting
	TripTeteMeldeListeSheetUpdate initMeldeListeSheet(WorkingSpreadsheet workingSpreadsheet) {
		return new TripTeteMeldeListeSheetUpdate(workingSpreadsheet);
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return SheetMetadataHelper.findeSheetUndHeile(
				getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), METADATA_SCHLUESSEL,
				SheetNamen.LEGACY_RANGLISTE);
	}

	@Override
	public TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	@Override
	protected void doRun() throws GenerateException {
		upDateSheet();
	}

	public void upDateSheet() throws GenerateException {
		meldeListe.upDateSheet();

		getxCalculatable().enableAutomaticCalculation(false);
		try {
			TeamMeldungen meldungen = meldeListe.getAlleMeldungen();
			if (!meldungen.isValid()) {
				processBoxinfo("processbox.abbruch");
				MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK)
						.caption(I18n.get("msg.caption.triptete.rangliste"))
						.message(I18n.get("msg.text.ungueltige.anzahl.meldungen")).show();
				return;
			}

			if (!NewSheet.from(this, SheetNamen.rangliste(), METADATA_SCHLUESSEL)
					.pos(DefaultSheetPos.TRIPTETE_ENDRANGLISTE).setForceCreate(true).setActiv()
					.hideGrid().tabColor(konfigurationSheet.getRanglisteTabFarbe()).create().isDidCreate()) {
				ProcessBox.from().info("Abbruch vom Benutzer, Trip-Tête-Rangliste wurde nicht erstellt");
				return;
			}

			insertHeader();
			TripTeteRanglisteDatenSchreiber.from(this, meldeListe, getWorkingSpreadsheet()).schreibeDaten();
			insertFooter(meldungen.size());
			formatieren(meldungen.size());
			printBereichDefinieren(meldungen.size());
			SheetFreeze.from(getTurnierSheet()).anzZeilen(ERSTE_DATEN_ZEILE).anzSpalten(3).doFreeze();
		} finally {
			getxCalculatable().enableAutomaticCalculation(true);
		}
	}

	private void insertHeader() throws GenerateException {
		int headerBackColor = konfigurationSheet.getRanglisteHeaderFarbe();
		int headerZeile = ERSTE_DATEN_ZEILE - 1;

		ColumnProperties colSchmal = ColumnProperties.from().setWidth(900).centerJustify();
		StringCellValue stVal = StringCellValue.from(getXSpreadSheet(), Position.from(TEAM_NR_SPALTE, headerZeile))
				.setColumnProperties(colSchmal);
		getSheetHelper().setStringValueInCell(stVal.setValue(I18n.get("column.header.nr")).spalte(TEAM_NR_SPALTE));

		ColumnProperties colName = ColumnProperties.from().setWidth(6000);
		getSheetHelper().setColumnProperties(getXSpreadSheet(), NAME_SPALTE, colName);
		getSheetHelper().setStringValueInCell(
				stVal.setColumnProperties(ColumnProperties.from()).setValue(I18n.get("column.header.teamname"))
						.spalte(NAME_SPALTE));

		getSheetHelper().setStringValueInCell(
				stVal.setColumnProperties(colSchmal).setValue(I18n.get("column.header.punkte")).spalte(PUNKTE_SPALTE));
		getSheetHelper().setStringValueInCell(
				stVal.setValue(I18n.get("column.header.siege")).spalte(SIEGE_SPALTE));
		getSheetHelper().setStringValueInCell(
				stVal.setValue(I18n.get("column.header.sp.punkte")).spalte(SP_PUNKTE_SPALTE));
		getSheetHelper().setStringValueInCell(
				stVal.setValue(I18n.get("column.header.sp.punkte.diff")).spalte(SP_PUNKTE_DIFF_SPALTE));

		RangePosition headerRange = RangePosition.from(TEAM_NR_SPALTE, headerZeile, LETZTE_SPALTE, headerZeile);
		RangeHelper.from(this, headerRange).setRangeProperties(
				RangeProperties.from()
						.setCellBackColor(headerBackColor)
						.centerJustify()
						.setBorder(BorderFactory.from().allThin().boldLn().forBottom().toBorder())
						.margin(120)
						.setShrinkToFit(true));

		getSheetHelper().setStringValueInCell(
				StringCellValue.from(getXSpreadSheet(), Position.from(RANG_SPALTE, headerZeile))
						.setColumnProperties(colSchmal)
						.setValue(I18n.get("column.header.platz"))
						.setRotate90()
						.setVertJustify(CellVertJustify2.CENTER)
						.setCellBackColor(headerBackColor)
						.setBorder(BorderFactory.from().allThin().boldLn().forBottom().toBorder())
						.setShrinkToFit(true)
						.setCharWeight(FontWeight.BOLD));
	}

	private void insertFooter(int anzTeams) throws GenerateException {
		int footerZeile = ERSTE_DATEN_ZEILE + anzTeams + 1;
		StringCellValue stVal = StringCellValue.from(this, Position.from(TEAM_NR_SPALTE, footerZeile))
				.setHoriJustify(CellHoriJustify.LEFT).setCharHeight(8);
		getSheetHelper().setStringValueInCell(
				stVal.setValue(I18n.get("triptete.rangliste.reihenfolge.platzierung")));
	}

	private void formatieren(int anzTeams) throws GenerateException {
		int letzteDatenZeile = ERSTE_DATEN_ZEILE + anzTeams - 1;
		RangePosition daten = RangePosition.from(TEAM_NR_SPALTE, ERSTE_DATEN_ZEILE, LETZTE_SPALTE, letzteDatenZeile);
		RangeHelper.from(this, daten).setRangeProperties(
				RangeProperties.from().setBorder(BorderFactory.from().allThin().boldLn().forTop().toBorder())
						.centerJustify().margin(120).setShrinkToFit(true));

		Integer farbeGerade = konfigurationSheet.getRanglisteHintergrundFarbeGerade();
		Integer farbeUngerade = konfigurationSheet.getRanglisteHintergrundFarbeUnGerade();
		RanglisteGeradeUngeradeFormatHelper.from(this, daten).geradeFarbe(farbeGerade)
				.ungeradeFarbe(farbeUngerade).apply();
	}

	private void printBereichDefinieren(int anzTeams) throws GenerateException {
		PrintArea.from(getXSpreadSheet(), getWorkingSpreadsheet()).setPrintArea(
				RangePosition.from(0, 0, LETZTE_SPALTE, ERSTE_DATEN_ZEILE + anzTeams + 1));
	}

	public TripTeteMeldeListeSheetUpdate getMeldeListe() {
		return meldeListe;
	}
}
