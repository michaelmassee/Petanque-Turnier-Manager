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
 */
public class LigaKonfigurationSheet extends BaseKonfigurationSheet implements IKonfigurationSheet {

	private static final Logger logger = LogManager.getLogger(LigaKonfigurationSheet.class);

	private final LigaPropertiesSpalte propertiesSpalte;

	/**
	 * @param workingSpreadsheet
	 */
	LigaKonfigurationSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.LIGA);
		propertiesSpalte = new LigaPropertiesSpalte(NAME_PROPERTIES_SPALTE, ERSTE_ZEILE_PROPERTIES, this);
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
		getSheetHelper().setActiveSheet(getXSpreadSheet());
	}

	@Override
	protected IPropertiesSpalte getPropertiesSpalte() {
		return propertiesSpalte;
	}

	@Override
	protected void updateTurnierSystemKonfiguration() throws GenerateException {
		// TODO noch nichts hier !!!!
	}

	@Override
	protected void updateTurnierSystemKonfigBlock() throws GenerateException {
		propertiesSpalte.updateKonfigBlock(); // Liga + Allgemeine properties
	}

	@Override
	protected void initPageStylesTurnierSystem() throws GenerateException {
		// TODO noch keine
	}

}