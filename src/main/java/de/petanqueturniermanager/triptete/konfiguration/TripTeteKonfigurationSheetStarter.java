package de.petanqueturniermanager.triptete.konfiguration;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;

/**
 * Starter für das Trip-Tête-Konfigurationssheet.
 */
public final class TripTeteKonfigurationSheetStarter {

	private TripTeteKonfigurationSheetStarter() {
	}

	public static void start(WorkingSpreadsheet workingSpreadsheet) {
		new TripTeteKonfigurationSheet(workingSpreadsheet).start();
	}
}
