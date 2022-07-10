package de.petanqueturniermanager.addins;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.frame.XModel;
import com.sun.star.sheet.XCalculatable;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.comp.adapter.IGlobalEventListener;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Erstellung 10.06.2022 / Michael Massee<br>
 * Properties Funktions update wenn Dirty On Document load
 */

public class UpdatePropertieFunctionsSheetRecalcOnLoad implements IGlobalEventListener {

	private static final Logger logger = LogManager.getLogger(UpdatePropertieFunctionsSheetRecalcOnLoad.class);

	@Override
	public void onNew(Object source) {
		logger.debug("set dirty false");
		GlobalImpl.getAndSetDirty(false);
	}

	/**
	 * Die Formule in GlobalImpl koenen erst dann ihre werte ermittlen wenn das Document volstaendig geladen ist
	 */

	@Override
	public void onLoad(Object source) {

		// propertie funktions failed on load Document?
		if (source != null && GlobalImpl.getAndSetDirty(false)) {
			XModel xModel = Lo.qi(XModel.class, source);
			if (xModel != null) {
				XSpreadsheetDocument xSpreadsheetDocument = Lo.qi(XSpreadsheetDocument.class, xModel);
				DocumentPropertiesHelper hlpr = new DocumentPropertiesHelper(xSpreadsheetDocument);
				if (hlpr.getTurnierSystemAusDocument() != TurnierSystem.KEIN) {
					// just do a global recalc
					XCalculatable xCal = Lo.qi(XCalculatable.class, xSpreadsheetDocument);
					if (xCal != null) {
						logger.debug("onload calculateAll weil IsDirty Propertie-Funktions");
						// nachteil das wird beim laden doppelt gemacht
						xCal.calculateAll();
						GlobalImpl.getAndSetDirty(false); // weil es sein kann das wir ein leeres document laden mit propertie funktionen
					}
				} else {
					logger.debug("set dirty false");
					GlobalImpl.getAndSetDirty(false); // weil es sein kann das wir ein leeres document laden mit propertie funktionen
				}
			}
		}
		//		XSpreadsheetView xSpreadsheetView = Lo.qi(XSpreadsheetView.class,
		//				xModel.getCurrentController());
		//
		//		DocumentPropertiesHelper hlpr = new DocumentPropertiesHelper(xSpreadsheetDocument);
		//		if (hlpr.getTurnierSystemAusDocument() != TurnierSystem.KEIN) {
		//
		//			//
		//
		//			// just do a global recalc
		//			XCalculatable xCal = Lo.qi(XCalculatable.class, xSpreadsheetDocument);
		//			if (xCal != null) {
		//				// nachteil das wird beim laden doppelt gemcht
		//				xCal.calculateAll();
		//			}
		//
		////			// XComponentContext xContext
		////			WorkingSpreadsheet wkSheet = new WorkingSpreadsheet(weakRefContext.get());
		////			SheetHelper shHlpr = new SheetHelper(wkSheet); // WorkingSpreadsheet
		////			XSpreadsheet anmeldungen = shHlpr.findByName(MeldeListeKonstanten.SHEETNAME);
		////			if (anmeldungen != null) {
		////				// do recalc
		////				XCalculatable xCal = Lo.qi(XCalculatable.class, xSpreadsheetDocument);
		////				if (xCal != null) {
		////					xCal.calculateAll();
		////				}
		////			}
		//		}
	}

}
