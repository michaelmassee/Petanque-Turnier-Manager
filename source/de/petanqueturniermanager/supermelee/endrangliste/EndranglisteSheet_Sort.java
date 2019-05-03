package de.petanqueturniermanager.supermelee.endrangliste;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;

public class EndranglisteSheet_Sort extends EndranglisteSheet {

	public EndranglisteSheet_Sort(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
	}

	@Override
	protected void doRun() throws GenerateException {
		XSpreadsheet sheet = getSheet();
		getSheetHelper().setActiveSheet(sheet);
		this.getRangListeSorter().doSort();
	}

}
