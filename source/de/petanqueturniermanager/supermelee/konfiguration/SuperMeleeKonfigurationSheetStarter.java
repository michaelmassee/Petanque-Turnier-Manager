/**
 * Erstellung 09.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.supermelee.konfiguration;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;

/**
 * @author Michael Massee
 *
 */
public class SuperMeleeKonfigurationSheetStarter {

	public void start(WorkingSpreadsheet workingSpreadsheet) {
		new SuperMeleeKonfigurationSheet(workingSpreadsheet).start();
	}

}
