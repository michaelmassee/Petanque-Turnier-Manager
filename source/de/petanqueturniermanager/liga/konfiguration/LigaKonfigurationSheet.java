/**
 * Erstellung 09.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.liga.konfiguration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.basesheet.konfiguration.BaseKonfigurationSheet;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.basesheet.konfiguration.IPropertiesSpalte;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;

/**
 * @author Michael Massee
 *
 */
public class LigaKonfigurationSheet extends BaseKonfigurationSheet implements IKonfigurationSheet {

	private static final Logger logger = LogManager.getLogger(LigaKonfigurationSheet.class);

	/**
	 * @param workingSpreadsheet
	 */
	public LigaKonfigurationSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
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
	public void update() throws GenerateException {
		// TODO Auto-generated method stub
	}

	@Override
	public XSpreadsheet getSheet() throws GenerateException {
		return getSheetHelper().newIfNotExist(SHEETNAME, DefaultSheetPos.KONFIGURATION, SHEET_COLOR);
	}

	@Override
	protected IPropertiesSpalte getPropertiesSpalte() {
		// TODO Auto-generated method stub
		return null;
	}

}
