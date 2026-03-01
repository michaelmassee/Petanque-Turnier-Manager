package de.petanqueturniermanager.schweizer.meldeliste;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
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
 * Erzeugt eine Schweizer Meldeliste mit Testdaten (ohne Dialog).
 * Formation: Doublette, kein Teamname, kein Vereinsname.
 */
public class SchweizerMeldeListeSheetTestDaten extends AbstractSchweizerMeldeListeSheet {

	private static final Logger logger = LogManager.getLogger(SchweizerMeldeListeSheetTestDaten.class);

	private static final int ANZ_TEAMS = 16;
	private static final Formation TEST_FORMATION = Formation.DOUBLETTE;

	private final SchweizerMeldeListeSheetNew meldeListe;
	private final TestnamenLoader testnamenLoader;

	public SchweizerMeldeListeSheetTestDaten(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
		meldeListe = new SchweizerMeldeListeSheetNew(workingSpreadsheet);
		testnamenLoader = new TestnamenLoader();
	}

	@Override
	public void doRun() throws GenerateException {
		if (!NewTestDatenValidator.from(getWorkingSpreadsheet(), getSheetHelper(), SchweizerSheet.TURNIERSYSTEM)
				.prefix(getLogPrefix()).validate()) {
			return;
		}

		getSheetHelper().removeAllSheetsExclude();
		setAktiveSpielRunde(SpielRundeNr.from(1));

		// Meldeliste mit Standardparametern anlegen (kein Dialog)
		meldeListe.createMeldelisteWithParams(TEST_FORMATION, false, false);

		testNamenEinfuegen();
	}

	private void testNamenEinfuegen() throws GenerateException {
		XSpreadsheet meldelisteSheet = meldeListe.getXSpreadSheet();
		getSheetHelper().setActiveSheet(meldelisteSheet);

		int anzSpielerProTeam = TEST_FORMATION.getAnzSpieler();
		List<String> testNamen = testnamenLoader.listeMitTestNamen(ANZ_TEAMS * anzSpielerProTeam);

		int ersteSpielerSpalte = meldeListe.getSpielerNameErsteSpalte();

		int nameIdx = 0;
		for (int team = 0; team < ANZ_TEAMS && nameIdx < testNamen.size(); team++) {
			SheetRunner.testDoCancelTask();
			int zeile = MeldeListeKonstanten.ERSTE_DATEN_ZEILE + team;
			for (int s = 0; s < anzSpielerProTeam && nameIdx < testNamen.size(); s++) {
				int spalte = ersteSpielerSpalte + s;
				getSheetHelper().setStringValueInCell(
						StringCellValue.from(meldelisteSheet, Position.from(spalte, zeile),
								testNamen.get(nameIdx++)));
			}
		}

		meldeListe.upDateSheet();
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

}
