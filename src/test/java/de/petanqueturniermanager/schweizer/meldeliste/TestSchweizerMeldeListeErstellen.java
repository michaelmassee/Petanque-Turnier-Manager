package de.petanqueturniermanager.schweizer.meldeliste;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;

/**
 * Erstellung 04.03.2024 / Michael Massee
 */

public class TestSchweizerMeldeListeErstellen {

	private static final Logger logger = LogManager.getLogger(TestSchweizerMeldeListeErstellen.class);

	private final WorkingSpreadsheet wkingSpreadsheet;
	private final XSpreadsheetDocument doc;
	private final SchweizerMeldeListeSheetTestDaten schweizerMeldeListeSheetTestDaten;

	public TestSchweizerMeldeListeErstellen(WorkingSpreadsheet wkingSpreadsheet, XSpreadsheetDocument doc) {
		this.wkingSpreadsheet = wkingSpreadsheet;
		this.doc = doc;
		this.schweizerMeldeListeSheetTestDaten = new SchweizerMeldeListeSheetTestDaten(wkingSpreadsheet);
	}

	public int run() throws GenerateException {
		schweizerMeldeListeSheetTestDaten.run(); // do not start a Thread ! 
		return 0;
	}

}
