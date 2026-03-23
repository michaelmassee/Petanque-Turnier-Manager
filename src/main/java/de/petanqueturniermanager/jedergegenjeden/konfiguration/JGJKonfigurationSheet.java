package de.petanqueturniermanager.jedergegenjeden.konfiguration;

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

	public static final int MELDUNG_NAME_WIDTH = 8000;

	private final JGJPropertiesSpalte propertiesSpalte;

	/**
	 * @param workingSpreadsheet
	 */
	public JGJKonfigurationSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.JGJ);
		propertiesSpalte = new JGJPropertiesSpalte(this);
	}

	@Override
	public Integer getSpielPlanHeaderFarbe() throws GenerateException {
		return propertiesSpalte.getSpielPlanHeaderFarbe();
	}

	@Override
	public Integer getSpielPlanHintergrundFarbeUnGerade() {
		return propertiesSpalte.getSpielPlanHintergrundFarbeUnGerade();
	}

	@Override
	public Integer getSpielPlanHintergrundFarbeGerade() {
		return propertiesSpalte.getSpielPlanHintergrundFarbeGerade();
	}

	@Override
	public String getKopfZeileLinks() {
		return propertiesSpalte.getKopfZeileLinks();
	}

	@Override
	public String getKopfZeileMitte() {
		return propertiesSpalte.getKopfZeileMitte();
	}

	@Override
	public String getKopfZeileRechts() {
		return propertiesSpalte.getKopfZeileRechts();

	}

	@Override
	protected void initPageStylesTurnierSystem() {
		PageStyleHelper.from(this, PageStyle.PETTURNMNGR).initDefaultFooter().setHeaderLeft(getKopfZeileLinks())
				.setHeaderCenter(getKopfZeileMitte()).setHeaderRight(getKopfZeileRechts()).create();
	}

	@Override
	public IPropertiesSpalte getPropertiesSpalte() {
		return propertiesSpalte;
	}

	@Override
	protected IKonfigurationSheet getKonfigurationSheet() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void doRun() throws GenerateException {
	}

	@Override
	public String getGruppenname() throws GenerateException {
		return propertiesSpalte.getGruppenname();
	}

	@Override
	public void setGruppenname(String name) throws GenerateException {
		propertiesSpalte.setGruppenname(name);
	}

	@Override
	public Integer getFreispielPunktePlus() {
		return propertiesSpalte.getFreispielPunktePlus();
	}

	@Override
	public Integer getFreispielPunkteMinus() {
		return propertiesSpalte.getFreispielPunkteMinus();
	}

}
