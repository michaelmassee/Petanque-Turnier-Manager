package de.petanqueturniermanager.supermelee.endrangliste;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.exception.GenerateException;

public class EndranglisteSheet_Sort extends EndranglisteSheet {

	public EndranglisteSheet_Sort(XComponentContext xContext) {
		super(xContext);
	}

	@Override
	protected void doRun() throws GenerateException {
		XSpreadsheet sheet = getSheet();
		getSheetHelper().setActiveSheet(sheet);
		this.getRangListeSorter().doSort();
	}

}
