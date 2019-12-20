/**
* Erstellung : 10.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.spieltagrangliste;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;

public class SpieltagRanglisteSheet_SortOnly extends SpieltagRanglisteSheet {

	public SpieltagRanglisteSheet_SortOnly(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
	}

	@Override
	protected void doRun() throws GenerateException {
		setSpieltagNr(getKonfigurationSheet().getAktiveSpieltag());
		XSpreadsheet sheet = getXSpreadSheet();
		getSheetHelper().setActiveSheet(sheet);
		getRangListeSorter().doSort();
	}

}
