/**
* Erstellung : 20.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.meldeliste;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.helper.ColorHelper;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.SpielerSpalte;
import de.petanqueturniermanager.konfiguration.KonfigurationSheet;

public class AnmeldungenSheet extends SheetRunner implements ISheet {
	private static final Logger logger = LogManager.getLogger(AnmeldungenSheet.class);

	public static final String SHEETNAME = "Anmeldungen";

	public static final int ERSTE_DATEN_ZEILE = 0;
	public static final int LFDNR_NR_SPALTE = 0; // Spalte A=0
	public static final int SPIELER_NAME_SPALTE = 1; // Spalte A=0

	private final AbstractMeldeListeSheet meldeliste;
	private final KonfigurationSheet konfigurationSheet;

	public AnmeldungenSheet(XComponentContext xContext) {
		super(xContext);
		this.meldeliste = new MeldeListeSheet_Update(xContext);
		this.konfigurationSheet = new KonfigurationSheet(xContext);
	}

	@Override
	protected Logger getLogger() {
		return logger;
	}

	@Override
	protected void doRun() {

		int spieltagNr = this.konfigurationSheet.getAktuelleSpieltag();
		if (spieltagNr < 1) {
			return;
		}

		getSheetHelper().removeSheet(getSheetName(spieltagNr));
		XSpreadsheet sheet = getSheet();
		getSheetHelper().setActiveSheet(sheet);

		List<String> spielerList = this.meldeliste.getSpielerNamenList();

		CellProperties celPropNr = CellProperties.from().setHoriJustify(CellHoriJustify.CENTER)
				.setWidth(SpielerSpalte.DEFAULT_SPALTE_NUMBER_WIDTH);
		NumberCellValue lfdNrVal = NumberCellValue.from(getSheet(), Position.from(LFDNR_NR_SPALTE, ERSTE_DATEN_ZEILE))
				.setBorder(BorderFactory.from().allThin().toBorder()).setCharColor(ColorHelper.CHAR_COLOR_SPIELER_NR);

		CellProperties celPropName = CellProperties.from().setHoriJustify(CellHoriJustify.CENTER)
				.setWidth(SpielerSpalte.DEFAULT_SPIELER_NAME_WIDTH);

		StringCellValue nameVal = StringCellValue
				.from(getSheet(), Position.from(SPIELER_NAME_SPALTE, ERSTE_DATEN_ZEILE))
				.setBorder(BorderFactory.from().allThin().toBorder()).setShrinkToFit(true);

		StringCellValue chkBox = StringCellValue
				.from(getSheet(), Position.from(SPIELER_NAME_SPALTE + 1, ERSTE_DATEN_ZEILE))
				.setBorder(BorderFactory.from().allThin().toBorder()).setValue(" ");

		int anzSpielerinSpalte = 40;
		int lfndNr = 1;
		spalteFormat(lfdNrVal, celPropNr, nameVal, celPropName, chkBox);

		for (String spielerName : spielerList) {

			lfdNrVal.setValue((double) lfndNr);
			nameVal.setValue(spielerName);

			this.getSheetHelper().setValInCell(lfdNrVal);
			this.getSheetHelper().setTextInCell(nameVal);
			this.getSheetHelper().setTextInCell(chkBox);

			lfdNrVal.zeilePlusEins();
			nameVal.zeilePlusEins();
			chkBox.zeilePlusEins();

			if ((lfndNr / anzSpielerinSpalte) * anzSpielerinSpalte == lfndNr) {
				lfdNrVal.spalte((lfndNr / anzSpielerinSpalte) * 4).zeile(ERSTE_DATEN_ZEILE);
				nameVal.spalte(lfdNrVal.getPos().getSpalte() + 1).zeile(ERSTE_DATEN_ZEILE);
				chkBox.spalte(lfdNrVal.getPos().getSpalte() + 2).zeile(ERSTE_DATEN_ZEILE);
				spalteFormat(lfdNrVal, celPropNr, nameVal, celPropName, chkBox);
			}
			lfndNr++;
		}
	}

	private void spalteFormat(NumberCellValue nrVal, CellProperties celPropNr, StringCellValue nameVal,
			CellProperties celPropName, StringCellValue chkBox) {
		this.getSheetHelper().setColumnProperties(getSheet(), nrVal.getPos().getSpalte(), celPropNr);
		this.getSheetHelper().setColumnProperties(getSheet(), nameVal.getPos().getSpalte(), celPropName);
		this.getSheetHelper().setColumnProperties(getSheet(), chkBox.getPos().getSpalte(), celPropNr);
		// leere spalte breite
		this.getSheetHelper().setColumnProperties(getSheet(), chkBox.getPos().getSpalte() + 1, celPropNr);
	}

	public String getSheetName(int spieltagNr) {
		return spieltagNr + ". Spieltag " + SHEETNAME;
	}

	@Override
	public XSpreadsheet getSheet() {
		int spieltagNr = this.konfigurationSheet.getAktuelleSpieltag();
		return this.getSheetHelper().newIfNotExist(getSheetName(spieltagNr), (short) 1, "98e2d7");
	}

}
