/**
 * Erstellung 12.04.2020 / Michael Massee
 */
package de.petanqueturniermanager.schweizer.konfiguration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.basesheet.konfiguration.BaseKonfigurationSheet;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.pagestyle.PageStyle;
import de.petanqueturniermanager.helper.pagestyle.PageStyleHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * @author Michael Massee
 */
public class SchweizerKonfigurationSheet extends BaseKonfigurationSheet
		implements ISchweizerPropertiesSpalte, IKonfigurationSheet {

	private static final Logger logger = LogManager.getLogger(SchweizerKonfigurationSheet.class);

	private final SchweizerPropertiesSpalte propertiesSpalte;

	/**
	 * @param workingSpreadsheet
	 */
	SchweizerKonfigurationSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.LIGA);
		propertiesSpalte = new SchweizerPropertiesSpalte(this);
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
		// nur Activ setzten
		TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet()).setActiv();
	}

	@Override
	protected SchweizerPropertiesSpalte getPropertiesSpalte() {
		return propertiesSpalte;
	}

	@Override
	protected void initPageStylesTurnierSystem() throws GenerateException {
		// default page Style
		PageStyleHelper.from(this, PageStyle.PETTURNMNGR).initDefaultFooter().setHeaderLeft(getKopfZeileLinks())
				.setHeaderCenter(getKopfZeileMitte()).setHeaderRight(getKopfZeileRechts()).create();
	}

	@Override
	public String getKopfZeileLinks() throws GenerateException {
		return propertiesSpalte.getKopfZeileLinks();
	}

	@Override
	public String getKopfZeileMitte() throws GenerateException {
		return propertiesSpalte.getKopfZeileMitte();
	}

	@Override
	public String getKopfZeileRechts() throws GenerateException {
		return propertiesSpalte.getKopfZeileRechts();
	}

	@Override
	public final void setAktiveSpielRunde(SpielRundeNr neueSpielrunde) throws GenerateException {
		getPropertiesSpalte().setAktiveSpielRunde(neueSpielrunde);
	}

}
