/**
* Erstellung : 10.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.spieltagrangliste;

import com.sun.star.uno.XComponentContext;

public class SpieltagRanglisteSheetSortOnly extends SpieltagRanglisteSheet {

	public SpieltagRanglisteSheetSortOnly(XComponentContext xContext) {
		super(xContext);
	}

	@Override
	protected void doRun() {
		doSortmitBestehende();
	}

}
