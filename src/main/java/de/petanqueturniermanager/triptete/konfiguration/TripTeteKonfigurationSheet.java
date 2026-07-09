package de.petanqueturniermanager.triptete.konfiguration;

import de.petanqueturniermanager.basesheet.konfiguration.BaseKonfigurationSheet;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.basesheet.konfiguration.IPropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.pagestyle.PageStyle;
import de.petanqueturniermanager.helper.pagestyle.PageStyleHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;

/**
 * Konfigurations-Sheet für das Trip-Tête-Turniersystem.
 */
public class TripTeteKonfigurationSheet extends BaseKonfigurationSheet
		implements ITripTetePropertiesSpalte {

	private final TripTetePropertiesSpalte propertiesSpalte;

	public TripTeteKonfigurationSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.TRIPTETE);
		propertiesSpalte = new TripTetePropertiesSpalte(this);
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

	@Override
	protected void initPageStylesTurnierSystem() {
		PageStyleHelper.from(this, PageStyle.PETTURNMNGR).initDefaultFooter()
				.setHeaderLeft(getKopfZeileLinks())
				.setHeaderCenter(getKopfZeileMitte())
				.setHeaderRight(getKopfZeileRechts())
				.create();
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
	public Integer getSpielZiel() {
		return propertiesSpalte.getSpielZiel();
	}

	@Override
	public boolean isMeldeListeTeamnameAnzeigen() {
		return propertiesSpalte.isMeldeListeTeamnameAnzeigen();
	}

	@Override
	public boolean isMeldeListeVereinsnameAnzeigen() {
		return propertiesSpalte.isMeldeListeVereinsnameAnzeigen();
	}
}
