package de.petanqueturniermanager.schweizer.meldeliste;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.NewTestDatenValidator;
import de.petanqueturniermanager.helper.TestnamenLoader;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerSheet;
import de.petanqueturniermanager.supermelee.SpielRundeNr;

/**
 * Erstellung 05.03.2024 / Michael Massee
 */

public class SchweizerMeldeListeSheetTestDaten extends AbstractSchweizerMeldeListeSheet {

	private static final Logger logger = LogManager.getLogger(SchweizerMeldeListeSheetTestDaten.class);
	private SchweizerMeldeListeSheetNew meldeListe;
	private final TestnamenLoader testnamenLoader;

	public SchweizerMeldeListeSheetTestDaten(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
		meldeListe = new SchweizerMeldeListeSheetNew(workingSpreadsheet);
		testnamenLoader = new TestnamenLoader();
	}

	@Override
	protected void doRun() throws GenerateException {
		if (!NewTestDatenValidator.from(getWorkingSpreadsheet(), getSheetHelper(), SchweizerSheet.TURNIERSYSTEM)
				.prefix(getLogPrefix()).validate()) {
			return;
		}

		getSheetHelper().removeAllSheetsExclude();
		setAktiveSpielRunde(SpielRundeNr.from(1));
		testNamenEinfuegen();

	}

	private void testNamenEinfuegen() throws GenerateException {
		XSpreadsheet meldelisteSheet = meldeListe.getXSpreadSheet();
		getSheetHelper().setActiveSheet(meldelisteSheet);

		List<String> testNamen = testnamenLoader.listeMitTestNamen(30); // Triplette 10 Teams

		Position posSpielerName1 = Position.from(meldeListe.getSpielerNameErsteSpalte(),
				MeldeListeKonstanten.ERSTE_DATEN_ZEILE - 1);
		Position posSpielerName2 = Position.from(meldeListe.getSpielerNameErsteSpalte() + 1,
				MeldeListeKonstanten.ERSTE_DATEN_ZEILE - 1);
		Position posSpielerName3 = Position.from(meldeListe.getSpielerNameErsteSpalte() + 2,
				MeldeListeKonstanten.ERSTE_DATEN_ZEILE - 1);
		StringCellValue spielrNamen1 = StringCellValue.from(meldelisteSheet, posSpielerName1);
		StringCellValue spielrNamen2 = StringCellValue.from(meldelisteSheet, posSpielerName2);
		StringCellValue spielrNamen3 = StringCellValue.from(meldelisteSheet, posSpielerName3);

		for (int spielerCntr = 0; spielerCntr < testNamen.size(); spielerCntr += 3) {
			SheetRunner.testDoCancelTask();
			posSpielerName1.zeilePlusEins();
			posSpielerName2.zeilePlusEins();
			posSpielerName3.zeilePlusEins();
			String textFromCell = getSheetHelper().getTextFromCell(meldelisteSheet, posSpielerName1);

			if (StringUtils.isNotEmpty(textFromCell)) {
				throw new GenerateException(
						"Fehler beim füllen von Testdaten in Meldesheet. Es dürfen keine Daten vorhanden sein");
			}

			getSheetHelper()
					.setStringValueInCell(spielrNamen1.setPos(posSpielerName1).setValue(testNamen.get(spielerCntr)));
			getSheetHelper().setStringValueInCell(
					spielrNamen2.setPos(posSpielerName2).setValue(testNamen.get(spielerCntr + 1)));
			getSheetHelper().setStringValueInCell(
					spielrNamen3.setPos(posSpielerName3).setValue(testNamen.get(spielerCntr + 2)));

		}

		meldeListe.upDateSheet();
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

}
