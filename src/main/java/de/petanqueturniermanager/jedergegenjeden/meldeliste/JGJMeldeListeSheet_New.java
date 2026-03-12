package de.petanqueturniermanager.jedergegenjeden.meldeliste;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;

/**
 * Erstellung 01.08.2022 / Michael Massee
 */

public class JGJMeldeListeSheet_New extends AbstractJGJMeldeListeSheet {

	public JGJMeldeListeSheet_New(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
	}

	@Override
	protected void doRun() throws GenerateException {
		if (NewSheet.from(this, SHEETNAME).pos(DefaultSheetPos.MELDELISTE).hideGrid().tabColor(SHEET_COLOR)
				.setDocVersionWhenNew().create().isDidCreate()) {

			upDateSheet();
		}
	}


}
