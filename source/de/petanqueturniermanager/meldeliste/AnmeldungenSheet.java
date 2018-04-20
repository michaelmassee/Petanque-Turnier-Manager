/**
* Erstellung : 20.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.meldeliste;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.Position;

public class AnmeldungenSheet extends SheetRunner implements ISheet {
	private static final Logger logger = LogManager.getLogger(AnmeldungenSheet.class);

	public static final String SHEETNAME = "Anmeldungen";

	public static final int ERSTE_DATEN_ZEILE = 0;
	public static final int LFDNR_NR_SPALTE = 0; // Spalte A=0
	public static final int SPIELER_NAME_SPALTE = 1; // Spalte A=0

	private final MeldeListeSheet meldeliste;

	public AnmeldungenSheet(XComponentContext xContext) {
		super(xContext);
		this.meldeliste = new MeldeListeSheet(xContext);
	}

	@Override
	protected Logger getLogger() {
		return logger;
	}

	@Override
	protected void doRun() {

		int spieltagNr = this.meldeliste.aktuelleSpieltag();
		if (spieltagNr < 1) {
			return;
		}

		getSheetHelper().removeSheet(getSheetName(spieltagNr));
		XSpreadsheet sheet = getSheet();
		getSheetHelper().setActiveSheet(sheet);

		List<String> spielerList = this.meldeliste.getSpielerNamenList();

		StringCellValue strVal = StringCellValue.from(getSheet(),
				Position.from(SPIELER_NAME_SPALTE, ERSTE_DATEN_ZEILE));

		NumberCellValue nrVal = NumberCellValue.from(getSheet(), Position.from(LFDNR_NR_SPALTE, ERSTE_DATEN_ZEILE));

		int anzSpielerinSpalte = 30;
		int lfndNr = 1;

		for (String spielerName : spielerList) {
			this.getSheetHelper().setValInCell(nrVal.zeilePlusEins().setValue((double) lfndNr));
			this.getSheetHelper().setTextInCell(strVal.zeilePlusEins().setValue(spielerName));

			if ((lfndNr / anzSpielerinSpalte) * anzSpielerinSpalte == lfndNr) {
				nrVal.spaltePlus(3).zeile(ERSTE_DATEN_ZEILE);
				strVal.spaltePlus(3).zeile(ERSTE_DATEN_ZEILE);
			}
			lfndNr++;
		}
	}

	public String getSheetName(int spieltagNr) {
		return spieltagNr + ". " + SHEETNAME;
	}

	@Override
	public XSpreadsheet getSheet() {
		int spieltagNr = this.meldeliste.aktuelleSpieltag();
		return this.getSheetHelper().newIfNotExist(getSheetName(spieltagNr), (short) 1, "98e2d7");
	}

}
