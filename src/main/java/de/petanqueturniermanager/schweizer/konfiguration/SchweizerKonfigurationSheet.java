/**
 * Erstellung 12.04.2020 / Michael Massee
 */
package de.petanqueturniermanager.schweizer.konfiguration;

import de.petanqueturniermanager.basesheet.konfiguration.BaseKonfigurationSheet;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.cellstyle.SpielrundeHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.SpielrundeHintergrundFarbeUnGeradeStyle;
import de.petanqueturniermanager.helper.pagestyle.PageStyle;
import de.petanqueturniermanager.helper.pagestyle.PageStyleHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * @author Michael Massee
 */
public class SchweizerKonfigurationSheet extends BaseKonfigurationSheet
		implements ISchweizerPropertiesSpalte {

	private final SchweizerPropertiesSpalte propertiesSpalte;

	/**
	 * @param workingSpreadsheet
	 */
	public SchweizerKonfigurationSheet(WorkingSpreadsheet workingSpreadsheet) {
		this(workingSpreadsheet, TurnierSystem.SCHWEIZER);
	}

	protected SchweizerKonfigurationSheet(WorkingSpreadsheet workingSpreadsheet, TurnierSystem ts) {
		super(workingSpreadsheet, ts);
		propertiesSpalte = erstellePropertiesSpalte();
	}

	protected SchweizerPropertiesSpalte erstellePropertiesSpalte() {
		return new SchweizerPropertiesSpalte(this);
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
	public SchweizerPropertiesSpalte getPropertiesSpalte() {
		return propertiesSpalte;
	}

	@Override
	protected void initPageStylesTurnierSystem() {
		// default page Style
		PageStyleHelper.from(this, PageStyle.PETTURNMNGR).initDefaultFooter().setHeaderLeft(getKopfZeileLinks())
				.setHeaderCenter(getKopfZeileMitte()).setHeaderRight(getKopfZeileRechts()).create();
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
	public final void setAktiveSpielRunde(SpielRundeNr neueSpielrunde) {
		getPropertiesSpalte().setAktiveSpielRunde(neueSpielrunde);
	}

	@Override
	public final SpielRundeNr getAktiveSpielRunde() {
		return getPropertiesSpalte().getAktiveSpielRunde();
	}

	@Override
	public Integer getSpielRundeHintergrundFarbeGerade() {
		return getPropertiesSpalte().getSpielRundeHintergrundFarbeGerade();
	}

	@Override
	public SpielrundeHintergrundFarbeGeradeStyle getSpielRundeHintergrundFarbeGeradeStyle() {
		return getPropertiesSpalte().getSpielRundeHintergrundFarbeGeradeStyle();
	}

	@Override
	public Integer getSpielRundeHintergrundFarbeUnGerade() {
		return getPropertiesSpalte().getSpielRundeHintergrundFarbeUnGerade();
	}

	@Override
	public SpielrundeHintergrundFarbeUnGeradeStyle getSpielRundeHintergrundFarbeUnGeradeStyle() {
		return getPropertiesSpalte().getSpielRundeHintergrundFarbeUnGeradeStyle();
	}

	@Override
	public Integer getSpielRundeHeaderFarbe() {
		return getPropertiesSpalte().getSpielRundeHeaderFarbe();
	}

	@Override
	public SpielrundeSpielbahn getSpielrundeSpielbahn() {
		return propertiesSpalte.getSpielrundeSpielbahn();
	}

	@Override
	public void setSpielrundeSpielbahn(SpielrundeSpielbahn option) {
		propertiesSpalte.setSpielrundeSpielbahn(option);
	}

	@Override
	public Formation getMeldeListeFormation() {
		return propertiesSpalte.getMeldeListeFormation();
	}

	@Override
	public boolean isMeldeListeTeamnameAnzeigen() {
		return propertiesSpalte.isMeldeListeTeamnameAnzeigen();
	}

	@Override
	public boolean isMeldeListeVereinsnameAnzeigen() {
		return propertiesSpalte.isMeldeListeVereinsnameAnzeigen();
	}

	@Override
	public void setMeldeListeFormation(Formation formation) {
		propertiesSpalte.setMeldeListeFormation(formation);
	}

	@Override
	public void setMeldeListeTeamnameAnzeigen(boolean anzeigen) {
		propertiesSpalte.setMeldeListeTeamnameAnzeigen(anzeigen);
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
	public SchweizerRankingModus getRankingModus() {
		return propertiesSpalte.getRankingModus();
	}

	@Override
	public void setRankingModus(SchweizerRankingModus modus) {
		propertiesSpalte.setRankingModus(modus);
	}

	@Override
	public Integer getFreispielPunktePlus() {
		return getPropertiesSpalte().getFreispielPunktePlus();
	}

	@Override
	public Integer getFreispielPunkteMinus() {
		return getPropertiesSpalte().getFreispielPunkteMinus();
	}

}
