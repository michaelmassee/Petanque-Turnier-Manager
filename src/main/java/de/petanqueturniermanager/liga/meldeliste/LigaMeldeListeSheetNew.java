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

public class LigaMeldeListeSheetNew extends AbstractLigaMeldeListeSheet {
	private static final Logger logger = LogManager.getLogger(LigaMeldeListeSheetNew.class);

	public LigaMeldeListeSheetNew(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
	}

	@Override
	protected void doRun() throws GenerateException {
		if (NewSheet.from(this, SHEETNAME).pos(DefaultSheetPos.MELDELISTE).hideGrid().tabColor(SHEET_COLOR)
				.setDocVersionWhenNew().create().isDidCreate()) {
			upDateSheet();
		}
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

}
