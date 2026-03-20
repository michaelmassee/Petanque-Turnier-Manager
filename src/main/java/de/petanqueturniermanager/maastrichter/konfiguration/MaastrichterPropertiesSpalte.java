/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.maastrichter.konfiguration;

import java.util.ArrayList;
import java.util.List;

import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.helper.ISheet;
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

	private static final String KONFIG_PROP_ANZ_VORRUNDEN = "Anzahl Vorrunden";

	static {
		KONFIG_PROPERTIES.addAll(SchweizerPropertiesSpalte.KONFIG_PROPERTIES);
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_ANZ_VORRUNDEN)
				.setDefaultVal(3).setDescription("Anzahl Vorrunden im Schweizer System (2-5)").inSideBar());
		// KO-Bracket-Properties wiederverwenden (keine Duplikation)
		KoPropertiesSpalte.addKoBracketProperties(KONFIG_PROPERTIES);
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
		String val = readStringProperty(KoPropertiesSpalte.KONFIG_PROP_SPIELBAUM_TEAM_ANZEIGE);
		try {
			return KoSpielbaumTeamAnzeige.valueOf(val);
		} catch (IllegalArgumentException | NullPointerException e) {
			return KoSpielbaumTeamAnzeige.NR;
		}
	}

	public void setSpielbaumTeamAnzeige(KoSpielbaumTeamAnzeige anzeige) {
		setStringProperty(KoPropertiesSpalte.KONFIG_PROP_SPIELBAUM_TEAM_ANZEIGE, anzeige.name());
	}

	public SpielrundeSpielbahn getSpielbaumSpielbahn() {
		String val = readStringProperty(KoPropertiesSpalte.KONFIG_PROP_SPIELBAUM_SPIELBAHN);
		try {
			return SpielrundeSpielbahn.valueOf(val);
		} catch (IllegalArgumentException | NullPointerException e) {
			return SpielrundeSpielbahn.X;
		}
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
		int val = readIntProperty(KoPropertiesSpalte.KONFIG_PROP_GRUPPEN_GROESSE);
		return val > 1 ? val : 16;
	}

	public void setGruppenGroesse(int gruppenGroesse) {
		writeIntProperty(KoPropertiesSpalte.KONFIG_PROP_GRUPPEN_GROESSE, Math.max(2, gruppenGroesse));
	}

	public int getMinRestGroesse() {
		int val = readIntProperty(KoPropertiesSpalte.KONFIG_PROP_MIN_REST_GROESSE);
		return val > 0 ? val : 16;
	}

	public void setMinRestGroesse(int minRestGroesse) {
		writeIntProperty(KoPropertiesSpalte.KONFIG_PROP_MIN_REST_GROESSE, Math.max(1, minRestGroesse));
	}

}
