/**
 * Erstellung 13.04.2020 / Michael Massee
 */
package de.petanqueturniermanager.schweizer.meldeliste;

import de.petanqueturniermanager.basesheet.meldeliste.IMeldeliste;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.model.SpielerMeldungen;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerSheet;

/**
 * @author Michael Massee
 *
 */
public abstract class AbstractSchweizerMeldeListeSheet extends SchweizerSheet implements IMeldeliste<SpielerMeldungen, Spieler> {

	/**
	 * @param workingSpreadsheet
	 */
	public AbstractSchweizerMeldeListeSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, "Schweizer-Meldeliste");
	}

}
