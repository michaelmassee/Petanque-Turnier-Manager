package de.petanqueturniermanager.supermelee.meldeliste;

import com.sun.star.frame.XModel;
import com.sun.star.sheet.XCalculatable;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.comp.adapter.IGlobalEventListener;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.sheet.WeakRefHelper;

/**
 * Erstellung 10.06.2022 / Michael Massee<br>
 * Properties in Meldeliste updaten wenn change
 */

public class TurnierSheetRecalcOnLoad implements IGlobalEventListener {
	private final WeakRefHelper<XComponentContext> weakRefContext;

	public TurnierSheetRecalcOnLoad(XComponentContext context) {
		this.weakRefContext = new WeakRefHelper<XComponentContext>(context);
	}

	/**
	 * Die Formule in GlobalImpl koenen erst dann ihre werte ermittlen wenn das Document volstaendig geladen ist
	 */

	@Override
	public void onLoad(Object source) {

		XModel xModel = UnoRuntime.queryInterface(XModel.class, source);
		XSpreadsheetDocument xSpreadsheetDocument = UnoRuntime.queryInterface(XSpreadsheetDocument.class, xModel);
//		XSpreadsheetView xSpreadsheetView = UnoRuntime.queryInterface(XSpreadsheetView.class,
//				xModel.getCurrentController());

		DocumentPropertiesHelper hlpr = new DocumentPropertiesHelper(xSpreadsheetDocument);
		if (hlpr.getTurnierSystemAusDocument() != TurnierSystem.KEIN) {

			//

			// just do a global recalc
			XCalculatable xCal = UnoRuntime.queryInterface(XCalculatable.class, xSpreadsheetDocument);
			if (xCal != null) {
				// nachteil das wird beim laden doppelt gemcht
				xCal.calculateAll();
			}

//			// XComponentContext xContext
//			WorkingSpreadsheet wkSheet = new WorkingSpreadsheet(weakRefContext.get());
//			SheetHelper shHlpr = new SheetHelper(wkSheet); // WorkingSpreadsheet
//			XSpreadsheet anmeldungen = shHlpr.findByName(MeldeListeKonstanten.SHEETNAME);
//			if (anmeldungen != null) {
//				// do recalc
//				XCalculatable xCal = UnoRuntime.queryInterface(XCalculatable.class, xSpreadsheetDocument);
//				if (xCal != null) {
//					xCal.calculateAll();
//				}
//			}
		}
	}

}
