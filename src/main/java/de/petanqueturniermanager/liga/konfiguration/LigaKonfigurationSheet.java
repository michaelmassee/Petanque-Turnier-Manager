/**
 * Erstellung 09.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.liga.konfiguration;

import de.petanqueturniermanager.basesheet.konfiguration.BaseKonfigurationSheet;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.basesheet.konfiguration.IPropertiesSpalte;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.pagestyle.PageStyle;
import de.petanqueturniermanager.helper.pagestyle.PageStyleHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * @author Michael Massee
 */
public class LigaKonfigurationSheet extends BaseKonfigurationSheet
		implements ILigaPropertiesSpalte, IKonfigurationSheet {

	public static final int LIGA_MELDUNG_NAME_WIDTH = 8000;

	private final LigaPropertiesSpalte propertiesSpalte;

	/**
	 * @param workingSpreadsheet
	 */
	public LigaKonfigurationSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.LIGA);
		propertiesSpalte = new LigaPropertiesSpalte(this);
	}

	@Override
	protected IKonfigurationSheet getKonfigurationSheet() {
		return this;
	}

	@Override
	protected void doRun() throws GenerateException {
		// nur Activ setzten
		TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet()).setActiv();
	}

	@Override
	public IPropertiesSpalte getPropertiesSpalte() {
		return propertiesSpalte;
	}

	@Override
	protected void initPageStylesTurnierSystem() {
		// default page Style
		PageStyleHelper.from(this, PageStyle.PETTURNMNGR).initDefaultFooter().setHeaderLeft(getKopfZeileLinks())
				.setHeaderCenter(getKopfZeileMitte()).setHeaderRight(getKopfZeileRechts()).create();
	}

	@Override
	public Integer getSpielPlanHeaderFarbe() {
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
	public String getGruppenname() {
		return propertiesSpalte.getGruppenname();
	}

	@Override
	public void setGruppenname(String name) {
		propertiesSpalte.setGruppenname(name);
	}

	@Override
	public String getBaseDownloadUrl() {
		return propertiesSpalte.getBaseDownloadUrl();
	}

	@Override
	public String getLigaLogoUr() {
		return propertiesSpalte.getLigaLogoUr();
	}

	@Override
	public String getPdfImageUr() {
		return propertiesSpalte.getPdfImageUr();
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
