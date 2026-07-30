package de.petanqueturniermanager.helper.sheetsync;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;

/**
 * Signatur-basiertes On-Demand-Rebuild eines abgeleiteten Ausgabe-Sheets (z.B. Rangliste),
 * damit Export-Pfade (Verzeichnis-Export, WhatsApp-Post) niemals ein fehlendes oder
 * veraltetes Sheet ausliefern. Gemeinsame Basis für
 * {@link de.petanqueturniermanager.helper.upload.AbstractExportInVerzeichnis} und den
 * WhatsApp-Export.
 */
public final class SheetSyncRebuild {

	private static final Logger logger = LogManager.getLogger(SheetSyncRebuild.class);

	private SheetSyncRebuild() {
	}

	@FunctionalInterface
	public interface RebuildAktion {
		void aktualisieren() throws GenerateException;
	}

	public static boolean sheetFehlt(XSpreadsheetDocument doc, String schluessel) {
		return SheetMetadataHelper.findeSheet(doc, schluessel).isEmpty();
	}

	public static void aktualisiereWennDirty(XSpreadsheetDocument doc, String persistenzSchluessel,
			EingabeSignatur signatur, RebuildAktion aktion) throws GenerateException {
		aktualisiereWennDirty(doc, persistenzSchluessel, signatur, sheetFehlt(doc, persistenzSchluessel), aktion);
	}

	public static void aktualisiereWennDirty(XSpreadsheetDocument doc, String persistenzSchluessel,
			EingabeSignatur signatur, boolean ausgabeFehlt, RebuildAktion aktion) throws GenerateException {
		SignaturErgebnis ergebnis = signatur.berechne(doc, 1);

		if (ergebnis instanceof SignaturErgebnis.Ok ok) {
			var gespeichert = SheetSyncSignaturStore.ladeHash(doc, persistenzSchluessel);
			if (!ausgabeFehlt && gespeichert.isPresent() && gespeichert.get().equals(ok.hash())) {
				SheetSyncSignaturStore.aktualisiereVerifyZeit(doc, persistenzSchluessel);
				logger.debug("Rebuild übersprungen, Signatur unverändert (key={})", persistenzSchluessel);
				return;
			}
			aktion.aktualisieren();
			String grund = ausgabeFehlt ? "exportMissingOutput" : "exportBeforeBuild";
			SheetSyncSignaturStore.speichereNachRebuild(doc, persistenzSchluessel, ok.hash(), grund);
			return;
		}

		if (ausgabeFehlt) {
			aktion.aktualisieren();
			return;
		}

		logger.warn("Rebuild übersprungen, Signatur konnte nicht berechnet werden (key={}): {}",
				persistenzSchluessel, ergebnis);
	}
}
