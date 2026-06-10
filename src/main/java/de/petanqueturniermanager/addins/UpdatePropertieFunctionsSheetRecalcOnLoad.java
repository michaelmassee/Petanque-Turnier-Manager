package de.petanqueturniermanager.addins;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.frame.XModel;
import com.sun.star.sheet.XCalculatable;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.comp.adapter.IGlobalEventListener;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

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
		if (source == null) return;

		XModel xModel = Lo.qi(XModel.class, source);
		if (xModel == null) return;

		XSpreadsheetDocument xSpreadsheetDocument = Lo.qi(XSpreadsheetDocument.class, xModel);
		DocumentPropertiesHelper hlpr = new DocumentPropertiesHelper(xSpreadsheetDocument);
		boolean istPtmDokument = hlpr.getTurnierSystemAusDocument() != TurnierSystem.KEIN;

		if (istPtmDokument) {
			SheetMetadataHelper.bereinigeVerwaisteMetadaten(xSpreadsheetDocument);
		}

		boolean warDirty = GlobalImpl.getAndSetDirty(false);
		if (istPtmDokument) {
			// Beim Laden kann Calc AddIn-Formeln auswerten, während noch ein anderes
			// Dokument fokussiert ist. Der dokumentgebundene Recalc überschreibt diese
			// Werte mit dem tatsächlichen Ladedokument als Kontext.
			XCalculatable xCal = Lo.qi(XCalculatable.class, xSpreadsheetDocument);
			if (xCal != null) {
				logger.debug("onload calculateAll mit Dokument-Kontext (dirty={})", warDirty);
				GlobalImpl.mitDokumentKontext(xSpreadsheetDocument, xCal::calculateAll);
				GlobalImpl.getAndSetDirty(false);
			}
		} else if (warDirty) {
			logger.debug("set dirty false");
		}
	}

}
