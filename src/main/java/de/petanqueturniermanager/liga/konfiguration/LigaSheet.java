/**
 * Erstellung 09.11.2019 / Michael Massee

 */
package de.petanqueturniermanager.liga.konfiguration;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * @author Michael Massee
 *
 */
public abstract class LigaSheet extends SheetRunner {

	protected static final int LIGA_MELDUNG_NAME_WIDTH = 8000;

	private LigaKonfigurationSheet konfigurationSheet;

	/**
	 * @param workingSpreadsheet
	 */
	public LigaSheet(WorkingSpreadsheet workingSpreadsheet) {
		this(workingSpreadsheet, null);
	}

	public LigaSheet(WorkingSpreadsheet workingSpreadsheet, String logPrefix) {
		super(workingSpreadsheet, TurnierSystem.LIGA, logPrefix);
		konfigurationSheet = new LigaKonfigurationSheet(workingSpreadsheet);
	}

	@Override
	protected LigaKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

}
