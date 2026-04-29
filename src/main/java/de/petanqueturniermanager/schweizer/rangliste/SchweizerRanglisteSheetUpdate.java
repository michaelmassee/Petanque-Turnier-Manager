package de.petanqueturniermanager.schweizer.rangliste;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.rangliste.RanglisteUpdateHelper;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzManager;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzRegistry;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetUpdate;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;
import de.petanqueturniermanager.toolbar.TurnierModus;

/**
 * Aktualisiert die Schweizer Rangliste ohne das Sheet neu zu erstellen.
 * <p>
 * Im Gegensatz zu {@link SchweizerRanglisteSheet} (vollständiger Neuaufbau mit
 * {@code NewSheet.forceCreate()}) schreibt diese Klasse nur den Datenbereich neu.
 * Header, Spaltenbreiten und Metadaten bleiben unverändert.
 * <p>
 * Wird vom {@link de.petanqueturniermanager.helper.rangliste.RanglisteRefreshListener}
 * verwendet, um bei einem Tab-Wechsel zur Rangliste nur die Daten zu aktualisieren –
 * ohne das Sheet zu löschen und neu anzulegen.
 * <p>
 * Fallback: Wenn das Rangliste-Sheet noch nicht existiert, wird automatisch
 * {@link SchweizerRanglisteSheet#doRun()} ausgelöst (vollständiger Erstaufbau).
 */
public class SchweizerRanglisteSheetUpdate extends SchweizerRanglisteSheet {

	private static final Logger logger = LogManager.getLogger(SchweizerRanglisteSheetUpdate.class);

	public SchweizerRanglisteSheetUpdate(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
	}

	protected SchweizerRanglisteSheetUpdate(WorkingSpreadsheet workingSpreadsheet, TurnierSystem ts) {
		super(workingSpreadsheet, ts);
	}

	@Override
	public void doRun() throws GenerateException {
		if (TurnierModus.get().istAktiv()) {
			BlattschutzRegistry.fuer(getTurnierSystem())
					.ifPresent(k -> BlattschutzManager.get().entsperren(k, getWorkingSpreadsheet()));
		}

		XSpreadsheet sheet = getXSpreadSheet();
		if (sheet == null) {
			logger.debug("RanglisteUpdate: Sheet '{}' nicht vorhanden – vollständiger Erstaufbau",
					getRanglistenSheetName());
			erstelleNeuAufbauSheet().doRun();
			// Schutz wird im rekursiven doRun() von SchweizerRanglisteSheet gesetzt
			return;
		}

		logger.debug("RanglisteUpdate START – Thread='{}'", Thread.currentThread().getName());
		processBoxinfo("processbox.rangliste.aktualisieren");

		SchweizerMeldeListeSheetUpdate meldeliste = erstelleMeldeListeSheet();
		TeamMeldungen aktiveMeldungen = meldeliste.getAktiveMeldungen();
		if (aktiveMeldungen == null || aktiveMeldungen.size() == 0) {
			processBoxinfo("processbox.abbruch");
			if (TurnierModus.get().istAktiv()) {
				BlattschutzRegistry.fuer(getTurnierSystem())
						.ifPresent(k -> BlattschutzManager.get().schuetzen(k, getWorkingSpreadsheet()));
			}
			return;
		}

		RanglisteUpdateHelper.loescheDatenzeilen(this, sheet, aktiveMeldungen.size());
		berechnungUndSchreiben(sheet, meldeliste, aktiveMeldungen);

		if (TurnierModus.get().istAktiv()) {
			BlattschutzRegistry.fuer(getTurnierSystem())
					.ifPresent(k -> BlattschutzManager.get().schuetzen(k, getWorkingSpreadsheet()));
		}

		// Bewusst KEIN setActiveSheet(sheet): Im Listener-Pfad ist der User schon auf der
		// Rangliste; ein zusätzliches setActiveSheet aus dem selectionChanged-Handler heraus
		// kollidiert mit LO-internem Tab-Klick-Handling und revertiert den Tab-Wechsel.
		// Bei programmatischen Aufrufen übernimmt der aufrufende Parent-Runner die Aktivierung.
		logger.debug("RanglisteUpdate ENDE – Thread='{}'", Thread.currentThread().getName());
	}

	/**
	 * Erzeugt das Sheet-Objekt für den vollständigen Erstaufbau.
	 * Überschreibbar für Subklassen (z.B. Maastrichter).
	 */
	protected SchweizerRanglisteSheet erstelleNeuAufbauSheet() {
		return new SchweizerRanglisteSheet(getWorkingSpreadsheet());
	}

}
