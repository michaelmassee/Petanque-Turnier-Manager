package de.petanqueturniermanager.supermelee.endrangliste;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;

public class EndranglisteSheet_Sort extends EndranglisteSheet {

	public EndranglisteSheet_Sort(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
	}

	@Override
	protected void doRun() throws GenerateException {
		XSpreadsheet sheet = getXSpreadSheet();
		TurnierSheet.from(sheet, getWorkingSpreadsheet()).setActiv();
		// test
		getRangListeSorter().doSort();
	}

}
