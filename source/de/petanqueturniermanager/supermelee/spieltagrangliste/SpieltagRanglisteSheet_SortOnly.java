/**
* Erstellung : 10.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.spieltagrangliste;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.exception.GenerateException;

public class SpieltagRanglisteSheet_SortOnly extends SpieltagRanglisteSheet {

	public SpieltagRanglisteSheet_SortOnly(XComponentContext xContext) {
		super(xContext);
	}

	@Override
	protected void doRun() throws GenerateException {
		setSpieltagNr(getKonfigurationSheet().getAktiveSpieltag());
		XSpreadsheet sheet = getSheet();
		getSheetHelper().setActiveSheet(sheet);
		getRangListeSorter().doSort();
	}

}
