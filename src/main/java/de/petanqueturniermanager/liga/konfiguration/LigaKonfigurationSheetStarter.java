/**
 * Erstellung 09.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.liga.konfiguration;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;

/**
 * @author Michael Massee
 *
 */
public class LigaKonfigurationSheetStarter {

	public static void start(WorkingSpreadsheet workingSpreadsheet) {
		// geht nur hier weil LigaKonfigurationSheet package
		new LigaKonfigurationSheet(workingSpreadsheet).start();
	}

}
