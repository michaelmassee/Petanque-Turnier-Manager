/**
* Erstellung : 30.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.liga.meldeliste;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;

public class LigaMeldeListeSheet_New extends AbstractLigaMeldeListeSheet {
	private static final Logger logger = LogManager.getLogger(LigaMeldeListeSheet_New.class);

	public LigaMeldeListeSheet_New(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
	}

	@Override
	protected void doRun() throws GenerateException {
		if (NewSheet.from(getWorkingSpreadsheet(), SHEETNAME).pos(DefaultSheetPos.MELDELISTE).tabColor(SHEET_COLOR).create().isDidCreate()) {
			// getKonfigurationSheet().setAktiveSpielRunde(SpielRundeNr.from(1));
			upDateSheet();
		}
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

}