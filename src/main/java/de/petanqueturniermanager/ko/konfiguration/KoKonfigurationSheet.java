/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ko.konfiguration;

import de.petanqueturniermanager.basesheet.konfiguration.BaseKonfigurationSheet;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.basesheet.konfiguration.IPropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
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

	public boolean isMeldeListeVereinsnameAnzeigen() {
		return propertiesSpalte.isMeldeListeVereinsnameAnzeigen();
	}

	public void setMeldeListeVereinsnameAnzeigen(boolean anzeigen) {
		propertiesSpalte.setMeldeListeVereinsnameAnzeigen(anzeigen);
	}

	public KoSpielbaumTeamAnzeige getSpielbaumTeamAnzeige() {
		return propertiesSpalte.getSpielbaumTeamAnzeige();
	}

	public void setSpielbaumTeamAnzeige(KoSpielbaumTeamAnzeige anzeige) {
		propertiesSpalte.setSpielbaumTeamAnzeige(anzeige);
	}

	public SpielrundeSpielbahn getSpielbaumSpielbahn() {
		return propertiesSpalte.getSpielbaumSpielbahn();
	}

	public void setSpielbaumSpielbahn(SpielrundeSpielbahn spielbahn) {
		propertiesSpalte.setSpielbaumSpielbahn(spielbahn);
	}

	public boolean isSpielbaumSpielUmPlatz3() {
		return propertiesSpalte.isSpielbaumSpielUmPlatz3();
	}

	public void setSpielbaumSpielUmPlatz3(boolean anzeigen) {
		propertiesSpalte.setSpielbaumSpielUmPlatz3(anzeigen);
	}

	public int getGruppenGroesse() {
		return propertiesSpalte.getGruppenGroesse();
	}

	public void setGruppenGroesse(int gruppenGroesse) {
		propertiesSpalte.setGruppenGroesse(gruppenGroesse);
	}

	public int getMinRestGroesse() {
		return propertiesSpalte.getMinRestGroesse();
	}

	public void setMinRestGroesse(int minRestGroesse) {
		propertiesSpalte.setMinRestGroesse(minRestGroesse);
	}

	public int getTurnierbaumHeaderFarbe()      { return propertiesSpalte.getTurnierbaumHeaderFarbe(); }
	public int getTurnierbaumTeamAFarbe()       { return propertiesSpalte.getTurnierbaumTeamAFarbe(); }
	public int getTurnierbaumTeamBFarbe()       { return propertiesSpalte.getTurnierbaumTeamBFarbe(); }
	public int getTurnierbaumScoreFarbe()       { return propertiesSpalte.getTurnierbaumScoreFarbe(); }
	public int getTurnierbaumSiegerFarbe()      { return propertiesSpalte.getTurnierbaumSiegerFarbe(); }
	public int getTurnierbaumBahnFarbe()        { return propertiesSpalte.getTurnierbaumBahnFarbe(); }
	public int getTurnierbaumDrittePlatzFarbe() { return propertiesSpalte.getTurnierbaumDrittePlatzFarbe(); }
}
