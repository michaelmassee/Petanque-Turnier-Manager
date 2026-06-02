package de.petanqueturniermanager.liga.rangliste;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.rangliste.RanglisteUpdateHelper;
import de.petanqueturniermanager.model.TeamMeldungen;

/**
 * Aktualisiert die Liga-Rangliste ohne das Sheet neu zu erstellen.
 * <p>
 * Im Gegensatz zu {@link LigaRanglisteSheet} (vollständiger Neuaufbau mit
 * {@code NewSheet.forceCreate()}) schreibt diese Klasse nur den Datenbereich neu.
 * Header, Spaltenbreiten und Metadaten bleiben unverändert.
 * <p>
 * Fallback: Wenn das Rangliste-Sheet noch nicht existiert, wird automatisch
 * {@link LigaRanglisteSheet#upDateSheet()} ausgelöst (vollständiger Erstaufbau).
 */
public class LigaRanglisteSheetUpdate extends LigaRanglisteSheet {

	private static final Logger logger = LogManager.getLogger(LigaRanglisteSheetUpdate.class);

	public LigaRanglisteSheetUpdate(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
	}

	@Override
	protected void doRun() throws GenerateException {
		XSpreadsheet sheet = getXSpreadSheet();
		if (sheet == null) {
			logger.debug("RanglisteUpdate: Sheet nicht vorhanden – vollständiger Erstaufbau");
			upDateSheet();
			return;
		}

		logger.debug("RanglisteUpdate START – Thread='{}'", Thread.currentThread().getName());
		processBoxinfo("processbox.rangliste.aktualisieren");

		getMeldeListe().upDateSheet();
		TeamMeldungen aktiveMeldungen = getAlleMeldungen();
		if (!aktiveMeldungen.isValid()) {
			processBoxinfo("processbox.abbruch");
			return;
		}

		RanglisteUpdateHelper.loescheDatenzeilen(this, sheet, aktiveMeldungen.size());
		berechnungUndSchreiben(sheet, getMeldeListe(), aktiveMeldungen);

		logger.debug("RanglisteUpdate ENDE – Thread='{}'", Thread.currentThread().getName());
	}
}
