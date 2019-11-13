/**
 * Erstellung 09.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.liga.konfiguration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.basesheet.konfiguration.BaseKonfigurationSheet;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.basesheet.konfiguration.IPropertiesSpalte;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * @author Michael Massee
 *
 */
public class LigaKonfigurationSheet extends BaseKonfigurationSheet implements IKonfigurationSheet {

	private static final Logger logger = LogManager.getLogger(LigaKonfigurationSheet.class);

	private final LigaPropertiesSpalte propertiesSpalte;

	/**
	 * @param workingSpreadsheet
	 */
	LigaKonfigurationSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.LIGA);
		propertiesSpalte = new LigaPropertiesSpalte(PROPERTIESSPALTE, ERSTE_ZEILE_PROPERTIES, this);
	}

	@Override
	protected IKonfigurationSheet getKonfigurationSheet() {
		return this;
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	protected void doRun() throws GenerateException {
		getSheetHelper().setActiveSheet(getSheet());
	}

	@Override
	protected IPropertiesSpalte getPropertiesSpalte() {
		return propertiesSpalte;
	}

	@Override
	protected void updateSpielSystemKonfiguration() throws GenerateException {
		// TODO noch nichts hier !!!!
	}

}
