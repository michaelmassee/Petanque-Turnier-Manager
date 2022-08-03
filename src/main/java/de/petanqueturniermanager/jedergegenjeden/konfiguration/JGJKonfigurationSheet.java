package de.petanqueturniermanager.jedergegenjeden.konfiguration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.basesheet.konfiguration.BaseKonfigurationSheet;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.basesheet.konfiguration.IPropertiesSpalte;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.pagestyle.PageStyle;
import de.petanqueturniermanager.helper.pagestyle.PageStyleHelper;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Erstellung 01.08.2022 / Michael Massee
 */

public class JGJKonfigurationSheet extends BaseKonfigurationSheet implements IJGJProperiesSpalte, IKonfigurationSheet {

	private static final Logger logger = LogManager.getLogger(JGJKonfigurationSheet.class);

	private final JGJPropertiesSpalte propertiesSpalte;

	/**
	 * @param workingSpreadsheet
	 */
	JGJKonfigurationSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.JGJ);
		propertiesSpalte = new JGJPropertiesSpalte(this);
	}

	@Override
	public Integer getSpielPlanHeaderFarbe() throws GenerateException {
		return propertiesSpalte.getSpielPlanHeaderFarbe();
	}

	@Override
	public Integer getSpielPlanHintergrundFarbeUnGerade() throws GenerateException {
		return propertiesSpalte.getSpielPlanHintergrundFarbeUnGerade();
	}

	@Override
	public Integer getSpielPlanHintergrundFarbeGerade() throws GenerateException {
		return propertiesSpalte.getSpielPlanHintergrundFarbeGerade();
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
	protected void initPageStylesTurnierSystem() throws GenerateException {
		PageStyleHelper.from(this, PageStyle.PETTURNMNGR).initDefaultFooter().setHeaderLeft(getKopfZeileLinks())
				.setHeaderCenter(getKopfZeileMitte()).setHeaderRight(getKopfZeileRechts()).create();
	}

	@Override
	protected IPropertiesSpalte getPropertiesSpalte() {
		return propertiesSpalte;
	}

	@Override
	protected IKonfigurationSheet getKonfigurationSheet() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	protected void doRun() throws GenerateException {
	}

	@Override
	public String getGruppennamen() throws GenerateException {
		return propertiesSpalte.getGruppennamen();
	}

	@Override
	public void setGruppennamen(String name) throws GenerateException {
		propertiesSpalte.setGruppennamen(name);
	}

}
