/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.maastrichter.konfiguration;

import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.pagestyle.PageStyle;
import de.petanqueturniermanager.helper.pagestyle.PageStyleHelper;
import de.petanqueturniermanager.ko.konfiguration.IKoBracketKonfiguration;
import de.petanqueturniermanager.ko.konfiguration.KoSpielbaumTeamAnzeige;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerKonfigurationSheet;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerPropertiesSpalte;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Konfigurations-Sheet für das Maastrichter Turniersystem.
 * Erweitert das Schweizer Konfigurations-Sheet um die Anzahl Vorrunden.
 */
public class MaastrichterKonfigurationSheet extends SchweizerKonfigurationSheet implements IKoBracketKonfiguration {

	public MaastrichterKonfigurationSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.MAASTRICHTER);
	}

	@Override
	protected SchweizerPropertiesSpalte erstellePropertiesSpalte() {
		return new MaastrichterPropertiesSpalte(this);
	}

	@Override
	public MaastrichterPropertiesSpalte getPropertiesSpalte() {
		return (MaastrichterPropertiesSpalte) super.getPropertiesSpalte();
	}

	@Override
	protected void initPageStylesTurnierSystem() {
		PageStyleHelper.from(this, PageStyle.PETTURNMNGR).initDefaultFooter()
				.setHeaderLeft(getKopfZeileLinks())
				.setHeaderCenter(getKopfZeileMitte())
				.setHeaderRight(getKopfZeileRechts()).create();
	}

	public int getAnzVorrunden() {
		return getPropertiesSpalte().getAnzVorrunden();
	}

	public void setAnzVorrunden(int anzVorrunden) {
		getPropertiesSpalte().setAnzVorrunden(anzVorrunden);
	}

	@Override
	public int getGruppenGroesse() {
		return getPropertiesSpalte().getGruppenGroesse();
	}

	public void setGruppenGroesse(int gruppenGroesse) {
		getPropertiesSpalte().setGruppenGroesse(gruppenGroesse);
	}

	@Override
	public int getMinRestGroesse() {
		return getPropertiesSpalte().getMinRestGroesse();
	}

	public void setMinRestGroesse(int minRestGroesse) {
		getPropertiesSpalte().setMinRestGroesse(minRestGroesse);
	}

	@Override
	public KoSpielbaumTeamAnzeige getSpielbaumTeamAnzeige() {
		return getPropertiesSpalte().getSpielbaumTeamAnzeige();
	}

	public void setSpielbaumTeamAnzeige(KoSpielbaumTeamAnzeige anzeige) {
		getPropertiesSpalte().setSpielbaumTeamAnzeige(anzeige);
	}

	@Override
	public SpielrundeSpielbahn getSpielbaumSpielbahn() {
		return getPropertiesSpalte().getSpielbaumSpielbahn();
	}

	public void setSpielbaumSpielbahn(SpielrundeSpielbahn spielbahn) {
		getPropertiesSpalte().setSpielbaumSpielbahn(spielbahn);
	}

	@Override
	public boolean isSpielbaumSpielUmPlatz3() {
		return getPropertiesSpalte().isSpielbaumSpielUmPlatz3();
	}

	public void setSpielbaumSpielUmPlatz3(boolean anzeigen) {
		getPropertiesSpalte().setSpielbaumSpielUmPlatz3(anzeigen);
	}

	public MaastrichterGruppenModus getMaastrichterGruppenModus() {
		return getPropertiesSpalte().getMaastrichterGruppenModus();
	}

	public void setMaastrichterGruppenModus(MaastrichterGruppenModus modus) {
		getPropertiesSpalte().setMaastrichterGruppenModus(modus);
	}

	@Override public int getKoTurnierbaumTabFarbe()       { return getPropertiesSpalte().getKoTurnierbaumTabFarbe(); }
	@Override public int getTurnierbaumHeaderFarbe()      { return getPropertiesSpalte().getTurnierbaumHeaderFarbe(); }
	@Override public int getTurnierbaumTeamAFarbe()       { return getPropertiesSpalte().getTurnierbaumTeamAFarbe(); }
	@Override public int getTurnierbaumTeamBFarbe()       { return getPropertiesSpalte().getTurnierbaumTeamBFarbe(); }
	@Override public int getTurnierbaumSiegerFarbe()      { return getPropertiesSpalte().getTurnierbaumSiegerFarbe(); }
	@Override public int getTurnierbaumBahnFarbe()        { return getPropertiesSpalte().getTurnierbaumBahnFarbe(); }
	@Override public int getTurnierbaumDrittePlatzFarbe() { return getPropertiesSpalte().getTurnierbaumDrittePlatzFarbe(); }

}
