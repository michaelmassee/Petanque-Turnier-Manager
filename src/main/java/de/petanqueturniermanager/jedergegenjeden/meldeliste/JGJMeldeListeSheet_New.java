package de.petanqueturniermanager.jedergegenjeden.meldeliste;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;

/**
 * Erstellung 01.08.2022 / Michael Massee
 */

public class JGJMeldeListeSheet_New extends AbstractJGJMeldeListeSheet {
	private static final Logger logger = LogManager.getLogger(JGJMeldeListeSheet_New.class);

	public JGJMeldeListeSheet_New(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
	}

	@Override
	protected void doRun() throws GenerateException {
		if (NewSheet.from(this, SHEETNAME).pos(DefaultSheetPos.MELDELISTE).hideGrid().tabColor(SHEET_COLOR).create()
				.setDocVersionWhenNew().isDidCreate()) {

			upDateSheet();
		}
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

}
