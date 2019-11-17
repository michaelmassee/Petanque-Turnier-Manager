/**
 * Erstellung 08.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.supermelee.konfiguration;

import static com.google.common.base.Preconditions.checkNotNull;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * @author Michael Massee
 *
 */
public abstract class SuperMeleeSheet extends SheetRunner {

	private SuperMeleeKonfigurationSheet konfigurationSheet;

	/**
	 * @param workingSpreadsheet
	 */
	public SuperMeleeSheet(WorkingSpreadsheet workingSpreadsheet) {
		this(workingSpreadsheet, null);
	}

	public SuperMeleeSheet(WorkingSpreadsheet workingSpreadsheet, String logPrefix) {
		super(checkNotNull(workingSpreadsheet), TurnierSystem.SUPERMELEE, logPrefix);
		konfigurationSheet = new SuperMeleeKonfigurationSheet(workingSpreadsheet);
	}

	@Override
	protected SuperMeleeKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

}
