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

	public static void start(WorkingSpreadsheet workingSpreadsheet) {
		// geht nur hier weil SuperMeleeKonfigurationSheet package
		new SuperMeleeKonfigurationSheet(workingSpreadsheet).start();
	}

}
