/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ko.konfiguration;

import de.petanqueturniermanager.basesheet.konfiguration.BaseKonfigurationSheet;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.basesheet.konfiguration.IPropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.pagestyle.PageStyle;
import de.petanqueturniermanager.helper.pagestyle.PageStyleHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Konfigurationssheet für das K.-O.-Turniersystem.
 */
public class KoKonfigurationSheet extends BaseKonfigurationSheet {

	public static final String SHEETNAME = "KO Konfiguration";
	private static final String SHEET_COLOR = "c12439";

	private final KoPropertiesSpalte propertiesSpalte;

	public KoKonfigurationSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.KO);
		propertiesSpalte = new KoPropertiesSpalte(this);
	}

	@Override
	protected IKonfigurationSheet getKonfigurationSheet() {
		return this;
	}

	@Override
	protected void doRun() throws GenerateException {
		TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet()).setActiv();
	}

	@Override
	public IPropertiesSpalte getPropertiesSpalte() {
		return propertiesSpalte;
	}

	public KoPropertiesSpalte getKoPropertiesSpalte() {
		return propertiesSpalte;
	}

	@Override
	protected void initPageStylesTurnierSystem() {
		PageStyleHelper.from(this, PageStyle.PETTURNMNGR).initDefaultFooter()
				.setHeaderLeft(getKopfZeileLinks())
				.setHeaderCenter(getKopfZeileMitte())
				.setHeaderRight(getKopfZeileRechts())
				.create();
	}

	public String getKopfZeileLinks() {
		return propertiesSpalte.getKopfZeileLinks();
	}

	public String getKopfZeileMitte() {
		return propertiesSpalte.getKopfZeileMitte();
	}

	public String getKopfZeileRechts() {
		return propertiesSpalte.getKopfZeileRechts();
	}

	public Formation getMeldeListeFormation() {
		return propertiesSpalte.getMeldeListeFormation();
	}

	public void setMeldeListeFormation(Formation formation) {
		propertiesSpalte.setMeldeListeFormation(formation);
	}

	public boolean isMeldeListeTeamnameAnzeigen() {
		return propertiesSpalte.isMeldeListeTeamnameAnzeigen();
	}

	public void setMeldeListeTeamnameAnzeigen(boolean anzeigen) {
		propertiesSpalte.setMeldeListeTeamnameAnzeigen(anzeigen);
	}
}
