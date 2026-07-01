package de.petanqueturniermanager.jedergegenjeden.konfiguration;

import de.petanqueturniermanager.basesheet.konfiguration.BaseKonfigurationSheet;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.basesheet.konfiguration.IPropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.pagestyle.PageStyle;
import de.petanqueturniermanager.helper.pagestyle.PageStyleHelper;
import de.petanqueturniermanager.helper.upload.UploadProtokoll;
import de.petanqueturniermanager.schweizer.konfiguration.SpielplanTeamAnzeige;

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

	public void setKopfZeileMitte(String text) {
		propertiesSpalte.setKopfZeileMitte(text);
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
	public Integer getFreispielPunktePlus() {
		return propertiesSpalte.getFreispielPunktePlus();
	}

	@Override
	public Integer getFreispielPunkteMinus() {
		return propertiesSpalte.getFreispielPunkteMinus();
	}

	@Override
	public Formation getMeldeListeFormation() {
		return propertiesSpalte.getMeldeListeFormation();
	}

	@Override
	public void setMeldeListeFormation(Formation formation) {
		propertiesSpalte.setMeldeListeFormation(formation);
	}

	@Override
	public boolean isMeldeListeTeamnameAnzeigen() {
		return propertiesSpalte.isMeldeListeTeamnameAnzeigen();
	}

	@Override
	public void setMeldeListeTeamnameAnzeigen(boolean anzeigen) {
		propertiesSpalte.setMeldeListeTeamnameAnzeigen(anzeigen);
	}

	@Override
	public boolean isMeldeListeVereinsnameAnzeigen() {
		return propertiesSpalte.isMeldeListeVereinsnameAnzeigen();
	}

	@Override
	public void setMeldeListeVereinsnameAnzeigen(boolean anzeigen) {
		propertiesSpalte.setMeldeListeVereinsnameAnzeigen(anzeigen);
	}

	@Override
	public SpielplanTeamAnzeige getSpielplanTeamAnzeige() {
		return propertiesSpalte.getSpielplanTeamAnzeige();
	}

	@Override
	public void setSpielplanTeamAnzeige(SpielplanTeamAnzeige anzeige) {
		propertiesSpalte.setSpielplanTeamAnzeige(anzeige);
	}

	@Override
	public int getGruppengroesse() {
		return propertiesSpalte.getGruppengroesse();
	}

	@Override
	public void setGruppengroesse(int groesse) {
		propertiesSpalte.setGruppengroesse(groesse);
	}

	@Override
	public boolean isRueckrunde() {
		return propertiesSpalte.isRueckrunde();
	}

	@Override
	public void setRueckrunde(boolean mitRueckrunde) {
		propertiesSpalte.setRueckrunde(mitRueckrunde);
	}

	@Override
	public JGJGesamtranglisteSortModus getGesamtranglisteSortModus() {
		return propertiesSpalte.getGesamtranglisteSortModus();
	}


	@Override
	public UploadProtokoll getUploadProtokoll() {
		return propertiesSpalte.getUploadProtokoll();
	}

	@Override
	public String getUploadHost() {
		return propertiesSpalte.getUploadHost();
	}

	@Override
	public int getUploadPort() {
		return propertiesSpalte.getUploadPort();
	}

	@Override
	public String getUploadBenutzer() {
		return propertiesSpalte.getUploadBenutzer();
	}

	@Override
	public String getUploadVerzeichnis() {
		return propertiesSpalte.getUploadVerzeichnis();
	}

}
