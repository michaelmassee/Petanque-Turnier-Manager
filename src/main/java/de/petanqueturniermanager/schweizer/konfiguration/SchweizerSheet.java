/**
 * Erstellung 12.04.2020 / Michael Massee
 * 
 */
package de.petanqueturniermanager.schweizer.konfiguration;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * @author Michael Massee
 *
 */
public abstract class SchweizerSheet extends SheetRunner {

	protected static final int LIGA_MELDUNG_NAME_WIDTH = 8000;

	private SchweizerKonfigurationSheet konfigurationSheet;

	/**
	 * @param workingSpreadsheet
	 */
	protected SchweizerSheet(WorkingSpreadsheet workingSpreadsheet) {
		this(workingSpreadsheet, null);
	}

	protected SchweizerSheet(WorkingSpreadsheet workingSpreadsheet, String logPrefix) {
		super(workingSpreadsheet, TurnierSystem.SCHWEIZER, logPrefix);
		konfigurationSheet = new SchweizerKonfigurationSheet(workingSpreadsheet);
	}

	@Override
	protected SchweizerKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}
}
