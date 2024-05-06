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

	public static final TurnierSystem TURNIERSYSTEM = TurnierSystem.SCHWEIZER;
	protected static final int MELDUNG_NAME_WIDTH = 8000;
	private SchweizerKonfigurationSheet konfigurationSheet;

	/**
	 * @param workingSpreadsheet
	 */
	protected SchweizerSheet(WorkingSpreadsheet workingSpreadsheet) {
		this(workingSpreadsheet, null);
	}

	protected SchweizerSheet(WorkingSpreadsheet workingSpreadsheet, String logPrefix) {
		super(workingSpreadsheet, TURNIERSYSTEM, logPrefix);
		konfigurationSheet = new SchweizerKonfigurationSheet(workingSpreadsheet);
	}

	@Override
	public SchweizerKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}
}
