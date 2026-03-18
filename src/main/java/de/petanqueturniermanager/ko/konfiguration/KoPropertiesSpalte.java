/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ko.konfiguration;

import java.util.ArrayList;
import java.util.List;

import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.konfigdialog.AuswahlConfigProperty;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.konfigdialog.HeaderFooterConfigProperty;

/**
 * Konfigurationseigenschaften für das K.-O.-Turniersystem.
 */
public class KoPropertiesSpalte extends BasePropertiesSpalte {

	public static final List<ConfigProperty<?>> KONFIG_PROPERTIES = new ArrayList<>();

	static {
		ADDBaseProp(KONFIG_PROPERTIES);
	}

	private static final String KONFIG_PROP_KOPF_ZEILE_LINKS = "Kopfzeile Links";
	private static final String KONFIG_PROP_KOPF_ZEILE_MITTE = "Kopfzeile Mitte";
	private static final String KONFIG_PROP_KOPF_ZEILE_RECHTS = "Kopfzeile Rechts";
	private static final String KONFIG_PROP_MELDELISTE_FORMATION = "Meldeliste Formation";
	private static final String KONFIG_PROP_MELDELISTE_TEAMNAME = "Meldeliste Teamname";
	private static final String KONFIG_PROP_MELDELISTE_VEREINSNAME = "Meldeliste Vereinsname";
	private static final String KONFIG_PROP_SPIELBAUM_TEAM_ANZEIGE = "Spielbaum Team Anzeige";
	private static final String KONFIG_PROP_SPIELBAUM_SPIELBAHN = "Spielbaum Spielbahn";

	static {
		KONFIG_PROPERTIES.add(HeaderFooterConfigProperty.from(KONFIG_PROP_KOPF_ZEILE_LINKS)
				.setDescription(KONFIG_PROP_KOPF_ZEILE_LINKS).inSideBar());
		KONFIG_PROPERTIES.add(HeaderFooterConfigProperty.from(KONFIG_PROP_KOPF_ZEILE_MITTE)
				.setDescription(KONFIG_PROP_KOPF_ZEILE_MITTE).inSideBar());
		KONFIG_PROPERTIES.add(HeaderFooterConfigProperty.from(KONFIG_PROP_KOPF_ZEILE_RECHTS)
				.setDescription(KONFIG_PROP_KOPF_ZEILE_RECHTS).inSideBar());

		KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_MELDELISTE_FORMATION)
				.setDefaultVal(Formation.DOUBLETTE.name())
				.setDescription("Formation.\r\nTETE=1 Spieler\r\nDOUBLETTE=2 Spieler\r\nTRIPLETTE=3 Spieler"))
				.addAuswahl(Formation.TETE.name(), Formation.TETE.getBezeichnung())
				.addAuswahl(Formation.DOUBLETTE.name(), Formation.DOUBLETTE.getBezeichnung())
				.addAuswahl(Formation.TRIPLETTE.name(), Formation.TRIPLETTE.getBezeichnung()).inSideBar());

		KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_MELDELISTE_TEAMNAME)
				.setDefaultVal("J").setDescription("Teamname-Spalte in Meldeliste anzeigen.\r\nJ=Ja\r\nN=Nein"))
				.addAuswahl("J", "Ja").addAuswahl("N", "Nein").inSideBar());

		KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_MELDELISTE_VEREINSNAME)
				.setDefaultVal("N").setDescription("Vereinsname-Spalte in Meldeliste anzeigen.\r\nJ=Ja\r\nN=Nein"))
				.addAuswahl("J", "Ja").addAuswahl("N", "Nein").inSideBar());

		KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty
				.from(KONFIG_PROP_SPIELBAUM_TEAM_ANZEIGE)
				.setDefaultVal(KoSpielbaumTeamAnzeige.NR.name())
				.setDescription("Team-Anzeige im Spielbaum.\r\nNR=Teamnummer\r\nNAME=Teamname"))
				.addAuswahl(KoSpielbaumTeamAnzeige.NR.name(), "Teamnummer")
				.addAuswahl(KoSpielbaumTeamAnzeige.NAME.name(), "Teamname").inSideBar());

		KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_SPIELBAUM_SPIELBAHN)
				.setDefaultVal(SpielrundeSpielbahn.X.name())
				.setDescription("Spielbahn im Spielbaum.\r\nX=Keine Spalte\r\nL=Leere Spalte\r\nN=Durchnummerieren (1-n)\r\nR=Zufällig vergeben"))
				.addAuswahl(SpielrundeSpielbahn.X.name(), "Keine Spalte")
				.addAuswahl(SpielrundeSpielbahn.L.name(), "Leere Spalte")
				.addAuswahl(SpielrundeSpielbahn.N.name(), "Durchnummerieren (1-n)")
				.addAuswahl(SpielrundeSpielbahn.R.name(), "Zufällig vergeben").inSideBar());
	}

	KoPropertiesSpalte(ISheet sheet) {
		super(sheet);
	}

	@Override
	protected List<ConfigProperty<?>> getKonfigProperties() {
		return KONFIG_PROPERTIES;
	}

	public String getKopfZeileLinks() {
		return readStringProperty(KONFIG_PROP_KOPF_ZEILE_LINKS);
	}

	public String getKopfZeileMitte() {
		return readStringProperty(KONFIG_PROP_KOPF_ZEILE_MITTE);
	}

	public String getKopfZeileRechts() {
		return readStringProperty(KONFIG_PROP_KOPF_ZEILE_RECHTS);
	}

	public Formation getMeldeListeFormation() {
		String val = readStringProperty(KONFIG_PROP_MELDELISTE_FORMATION);
		try {
			return Formation.valueOf(val);
		} catch (IllegalArgumentException | NullPointerException e) {
			return Formation.DOUBLETTE;
		}
	}

	public void setMeldeListeFormation(Formation formation) {
		setStringProperty(KONFIG_PROP_MELDELISTE_FORMATION, formation.name());
	}

	public boolean isMeldeListeTeamnameAnzeigen() {
		return "J".equalsIgnoreCase(readStringProperty(KONFIG_PROP_MELDELISTE_TEAMNAME));
	}

	public void setMeldeListeTeamnameAnzeigen(boolean anzeigen) {
		setStringProperty(KONFIG_PROP_MELDELISTE_TEAMNAME, anzeigen ? "J" : "N");
	}

	public boolean isMeldeListeVereinsnameAnzeigen() {
		return "J".equalsIgnoreCase(readStringProperty(KONFIG_PROP_MELDELISTE_VEREINSNAME));
	}

	public void setMeldeListeVereinsnameAnzeigen(boolean anzeigen) {
		setStringProperty(KONFIG_PROP_MELDELISTE_VEREINSNAME, anzeigen ? "J" : "N");
	}

	public KoSpielbaumTeamAnzeige getSpielbaumTeamAnzeige() {
		String val = readStringProperty(KONFIG_PROP_SPIELBAUM_TEAM_ANZEIGE);
		try {
			return KoSpielbaumTeamAnzeige.valueOf(val);
		} catch (IllegalArgumentException | NullPointerException e) {
			return KoSpielbaumTeamAnzeige.NR;
		}
	}

	public void setSpielbaumTeamAnzeige(KoSpielbaumTeamAnzeige anzeige) {
		setStringProperty(KONFIG_PROP_SPIELBAUM_TEAM_ANZEIGE, anzeige.name());
	}

	public SpielrundeSpielbahn getSpielbaumSpielbahn() {
		String val = readStringProperty(KONFIG_PROP_SPIELBAUM_SPIELBAHN);
		try {
			return SpielrundeSpielbahn.valueOf(val);
		} catch (IllegalArgumentException | NullPointerException e) {
			return SpielrundeSpielbahn.X;
		}
	}

	public void setSpielbaumSpielbahn(SpielrundeSpielbahn spielbahn) {
		setStringProperty(KONFIG_PROP_SPIELBAUM_SPIELBAHN, spielbahn.name());
	}
}
