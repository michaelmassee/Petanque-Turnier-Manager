package de.petanqueturniermanager.triptete.rangliste;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.model.TeamMeldungen;

/**
 * Listener-sicheres Rangliste-Update: Aktualisiert nur den Datenbereich der
 * bereits existierenden Trip-Tête-Rangliste, ohne {@code forceCreate} und damit
 * ohne Race-Condition gegen einen parallelen Vollaufbau.
 * <p>
 * Formatierung (Zebra-Färbung, Rahmen, Fußzeile, Druckbereich) wird trotzdem auf
 * den vollständigen aktuellen Datenbereich angewendet – sonst bleiben nachträglich
 * über die Meldeliste hinzugekommene Teams unformatiert (Bug bei wachsender
 * Teilnehmerzahl, z.B. 15 Teams).
 */
public class TripTeteRanglisteSheetUpdate extends TripTeteRanglisteSheet {

	private static final Logger logger = LogManager.getLogger(TripTeteRanglisteSheetUpdate.class);

	public TripTeteRanglisteSheetUpdate(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
	}

	@Override
	public void doRun() throws GenerateException {
		XSpreadsheet sheet = getXSpreadSheet();
		if (sheet == null) {
			logger.debug("RanglisteUpdate: Trip-Tête-Rangliste nicht vorhanden – vollständiger Erstaufbau");
			upDateSheet();
			return;
		}

		logger.debug("RanglisteUpdate START – Thread='{}'", Thread.currentThread().getName());

		TeamMeldungen meldungen = getMeldeListe().getAlleMeldungen();
		if (!meldungen.isValid()) {
			logger.debug("RanglisteUpdate: ungültige Anzahl Meldungen – übersprungen");
			return;
		}

		TripTeteRanglisteDatenSchreiber.from(this, getMeldeListe(), getWorkingSpreadsheet()).schreibeDaten();
		insertFooter(meldungen.size());
		formatieren(meldungen.size());
		printBereichDefinieren(meldungen.size());

		logger.debug("RanglisteUpdate ENDE");
	}
}
