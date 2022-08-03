package de.petanqueturniermanager.jedergegenjeden.konfiguration;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Erstellung 01.08.2022 / Michael Massee
 */

public abstract class JGJSheet extends SheetRunner {

	protected static final int MELDUNG_NAME_WIDTH = 8000;

	private JGJKonfigurationSheet konfigurationSheet;

	/**
	 * @param workingSpreadsheet
	 */
	public JGJSheet(WorkingSpreadsheet workingSpreadsheet) {
		this(workingSpreadsheet, null);
	}

	public JGJSheet(WorkingSpreadsheet workingSpreadsheet, String logPrefix) {
		super(workingSpreadsheet, TurnierSystem.JGJ, logPrefix);
		konfigurationSheet = new JGJKonfigurationSheet(workingSpreadsheet);
	}

	@Override
	protected JGJKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

}
