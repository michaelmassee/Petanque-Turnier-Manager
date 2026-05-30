/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.maastrichter.konfiguration;

import java.util.ArrayList;
import java.util.List;

import de.petanqueturniermanager.basesheet.SheetTabFarben;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.konfigdialog.AuswahlConfigProperty;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.konfigdialog.ConfigPropertyType;
import de.petanqueturniermanager.ko.konfiguration.KoPropertiesSpalte;
import de.petanqueturniermanager.ko.konfiguration.KoSpielbaumTeamAnzeige;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerPropertiesSpalte;

/**
 * Konfigurations-Properties für das Maastrichter Turniersystem.
 * Erweitert die Schweizer Properties um die Anzahl der Vorrunden.
 */
public class MaastrichterPropertiesSpalte extends SchweizerPropertiesSpalte {

	public static final List<ConfigProperty<?>> KONFIG_PROPERTIES = new ArrayList<>();

	public static final String KONFIG_PROP_TAB_COLOR_CADRAGE = "Tab-Farbe Cadrage";

	private static final String KONFIG_PROP_ANZ_VORRUNDEN = "Anzahl Vorrunden";
	static final String KONFIG_PROP_GRUPPEN_MODUS = "Gruppen-Modus";

	static {
		KONFIG_PROPERTIES.addAll(SchweizerPropertiesSpalte.KONFIG_PROPERTIES);
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_TAB_COLOR_CADRAGE)
				.setDefaultVal(SheetTabFarben.FORME_CADRAGE).tabFarbe());
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_ANZ_VORRUNDEN)
				.setDefaultVal(3).setDescription("config.desc.maastrichter.anz.vorrunden"));
		KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty
				.from(KONFIG_PROP_GRUPPEN_MODUS)
				.setDefaultVal(MaastrichterGruppenModus.NACH_GROESSE.name())
				.setDescription("config.desc.maastrichter.gruppen.modus"))
				.addAuswahl(MaastrichterGruppenModus.NACH_GROESSE.name(), "Nach Gruppengröße (Standard)")
				.addAuswahl(MaastrichterGruppenModus.NACH_SIEGEN.name(), "Nach Siegen (Alternative)"));
		// KO-Bracket-Properties wiederverwenden (keine Duplikation)
		KoPropertiesSpalte.addKoBracketProperties(KONFIG_PROPERTIES);
		KoPropertiesSpalte.addKoBracketColorProperties(KONFIG_PROPERTIES);
	}

	MaastrichterPropertiesSpalte(ISheet sheet) {
		super(sheet);
	}

	@Override
	protected List<ConfigProperty<?>> getKonfigProperties() {
		return KONFIG_PROPERTIES;
	}

	public int getAnzVorrunden() {
		return readIntProperty(KONFIG_PROP_ANZ_VORRUNDEN);
	}

	public void setAnzVorrunden(int anzVorrunden) {
		writeIntProperty(KONFIG_PROP_ANZ_VORRUNDEN, anzVorrunden);
	}

	public KoSpielbaumTeamAnzeige getSpielbaumTeamAnzeige() {
		return readEnumProperty(KoPropertiesSpalte.KONFIG_PROP_SPIELBAUM_TEAM_ANZEIGE,
				KoSpielbaumTeamAnzeige.class, KoSpielbaumTeamAnzeige.NR);
	}

	public void setSpielbaumTeamAnzeige(KoSpielbaumTeamAnzeige anzeige) {
		setStringProperty(KoPropertiesSpalte.KONFIG_PROP_SPIELBAUM_TEAM_ANZEIGE, anzeige.name());
	}

	public SpielrundeSpielbahn getSpielbaumSpielbahn() {
		return readEnumProperty(KoPropertiesSpalte.KONFIG_PROP_SPIELBAUM_SPIELBAHN,
				SpielrundeSpielbahn.class, SpielrundeSpielbahn.X);
	}

	public void setSpielbaumSpielbahn(SpielrundeSpielbahn spielbahn) {
		setStringProperty(KoPropertiesSpalte.KONFIG_PROP_SPIELBAUM_SPIELBAHN, spielbahn.name());
	}

	public boolean isSpielbaumSpielUmPlatz3() {
		return "J".equalsIgnoreCase(readStringProperty(KoPropertiesSpalte.KONFIG_PROP_SPIELBAUM_PLATZ3));
	}

	public void setSpielbaumSpielUmPlatz3(boolean anzeigen) {
		setStringProperty(KoPropertiesSpalte.KONFIG_PROP_SPIELBAUM_PLATZ3, anzeigen ? "J" : "N");
	}

	public int getGruppenGroesse() {
		return KoPropertiesSpalte.normalisiereGruppenGroesse(
				readStringProperty(KoPropertiesSpalte.KONFIG_PROP_GRUPPEN_GROESSE));
	}

	public void setGruppenGroesse(int gruppenGroesse) {
		setStringProperty(KoPropertiesSpalte.KONFIG_PROP_GRUPPEN_GROESSE,
				Integer.toString(KoPropertiesSpalte.normalisiereGruppenGroesse(gruppenGroesse)));
	}

	public int getMinLetzteGruppeGroesse() {
		return KoPropertiesSpalte.normalisiereMinLetzteGruppeGroesse(
				readStringProperty(KoPropertiesSpalte.KONFIG_PROP_MIN_LETZTE_GRUPPE_GROESSE));
	}

	public void setMinLetzteGruppeGroesse(int wert) {
		setStringProperty(KoPropertiesSpalte.KONFIG_PROP_MIN_LETZTE_GRUPPE_GROESSE,
				Integer.toString(KoPropertiesSpalte.normalisiereMinLetzteGruppeGroesse(wert)));
	}

	public MaastrichterGruppenModus getMaastrichterGruppenModus() {
		return readEnumProperty(KONFIG_PROP_GRUPPEN_MODUS, MaastrichterGruppenModus.class,
				MaastrichterGruppenModus.NACH_GROESSE);
	}

	public void setMaastrichterGruppenModus(MaastrichterGruppenModus modus) {
		setStringProperty(KONFIG_PROP_GRUPPEN_MODUS, modus.name());
	}

	public int getCadrageTabFarbe()             { return readIntProperty(KONFIG_PROP_TAB_COLOR_CADRAGE); }
	public int getKoTurnierbaumTabFarbe()       { return readIntProperty(KoPropertiesSpalte.KONFIG_PROP_TAB_COLOR_KO_TURNIERBAUM); }
	public int getTurnierbaumHeaderFarbe()      { return readIntProperty(KoPropertiesSpalte.KONFIG_PROP_TURNIERBAUM_COLOR_HEADER); }
	public int getTurnierbaumTeamAFarbe()       { return readIntProperty(KoPropertiesSpalte.KONFIG_PROP_TURNIERBAUM_COLOR_TEAM_A); }
	public int getTurnierbaumTeamBFarbe()       { return readIntProperty(KoPropertiesSpalte.KONFIG_PROP_TURNIERBAUM_COLOR_TEAM_B); }
	public int getTurnierbaumSiegerFarbe()      { return readIntProperty(KoPropertiesSpalte.KONFIG_PROP_TURNIERBAUM_COLOR_SIEGER); }
	public int getTurnierbaumBahnFarbe()        { return readIntProperty(KoPropertiesSpalte.KONFIG_PROP_TURNIERBAUM_COLOR_BAHN); }
	public int getTurnierbaumDrittePlatzFarbe() { return readIntProperty(KoPropertiesSpalte.KONFIG_PROP_TURNIERBAUM_COLOR_DRITTE_PLATZ); }

}
