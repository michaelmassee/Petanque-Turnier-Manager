/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ko.konfiguration;

import java.util.ArrayList;
import java.util.List;

import de.petanqueturniermanager.basesheet.SheetTabFarben;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.konfigdialog.AuswahlConfigProperty;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.konfigdialog.ConfigPropertyType;
import de.petanqueturniermanager.konfigdialog.HeaderFooterConfigProperty;

/**
 * Konfigurationseigenschaften für das K.-O.-Turniersystem.
 */
public class KoPropertiesSpalte extends BasePropertiesSpalte {

	public static final List<ConfigProperty<?>> KONFIG_PROPERTIES = new ArrayList<>();

	static {
		ADDBaseProp(KONFIG_PROPERTIES, false);
	}

	private static final String KONFIG_PROP_TAB_COLOR_KO_TURNIERBAUM       = "Tab-Farbe KO-Turnierbaum";

	private static final String KONFIG_PROP_TURNIERBAUM_COLOR_HEADER       = "Turnierbaum Header Farbe";
	private static final String KONFIG_PROP_TURNIERBAUM_COLOR_TEAM_A       = "Turnierbaum Team A Farbe";
	private static final String KONFIG_PROP_TURNIERBAUM_COLOR_TEAM_B       = "Turnierbaum Team B Farbe";
	private static final String KONFIG_PROP_TURNIERBAUM_COLOR_SCORE        = "Turnierbaum Score Farbe";
	private static final String KONFIG_PROP_TURNIERBAUM_COLOR_SIEGER       = "Turnierbaum Sieger Farbe";
	private static final String KONFIG_PROP_TURNIERBAUM_COLOR_BAHN         = "Turnierbaum Bahn Farbe";
	private static final String KONFIG_PROP_TURNIERBAUM_COLOR_DRITTE_PLATZ = "Turnierbaum 3. Platz Farbe";

	private static final String KONFIG_PROP_KOPF_ZEILE_LINKS = "Kopfzeile Links";
	private static final String KONFIG_PROP_KOPF_ZEILE_MITTE = "Kopfzeile Mitte";
	private static final String KONFIG_PROP_KOPF_ZEILE_RECHTS = "Kopfzeile Rechts";
	private static final String KONFIG_PROP_MELDELISTE_FORMATION = "Meldeliste Formation";
	private static final String KONFIG_PROP_MELDELISTE_TEAMNAME = "Meldeliste Teamname";
	private static final String KONFIG_PROP_MELDELISTE_VEREINSNAME = "Meldeliste Vereinsname";
	public static final String KONFIG_PROP_SPIELBAUM_TEAM_ANZEIGE = "Spielbaum Team Anzeige";
	public static final String KONFIG_PROP_SPIELBAUM_SPIELBAHN = "Spielbaum Spielbahn";
	public static final String KONFIG_PROP_SPIELBAUM_PLATZ3 = "Spielbaum Spiel um Platz 3";

	public static final String KONFIG_PROP_GRUPPEN_GROESSE = "Turnierbaum Gruppen Größe";
	public static final String KONFIG_PROP_MIN_REST_GROESSE = "Turnierbaum Min. Rest-Größe";

	static {
		KONFIG_PROPERTIES.add(HeaderFooterConfigProperty.from(KONFIG_PROP_KOPF_ZEILE_LINKS)
				.setDescription("config.desc.header.links").inSideBar());
		KONFIG_PROPERTIES.add(HeaderFooterConfigProperty.from(KONFIG_PROP_KOPF_ZEILE_MITTE)
				.setDescription("config.desc.header.mitte").inSideBar());
		KONFIG_PROPERTIES.add(HeaderFooterConfigProperty.from(KONFIG_PROP_KOPF_ZEILE_RECHTS)
				.setDescription("config.desc.header.rechts").inSideBar());

		KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_MELDELISTE_FORMATION)
				.setDefaultVal(Formation.DOUBLETTE.name())
				.setDescription("config.desc.ko.formation"))
				.addAuswahl(Formation.TETE.name(), Formation.TETE.getBezeichnung())
				.addAuswahl(Formation.DOUBLETTE.name(), Formation.DOUBLETTE.getBezeichnung())
				.addAuswahl(Formation.TRIPLETTE.name(), Formation.TRIPLETTE.getBezeichnung()).inSideBar());

		KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_MELDELISTE_TEAMNAME)
				.setDefaultVal("J").setDescription("config.desc.meldeliste.teamname"))
				.addAuswahl("J", "Ja").addAuswahl("N", "Nein").inSideBar());

		KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_MELDELISTE_VEREINSNAME)
				.setDefaultVal("N").setDescription("config.desc.ko.vereinsname"))
				.addAuswahl("J", "Ja").addAuswahl("N", "Nein").inSideBar());

		KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty
				.from(KONFIG_PROP_SPIELBAUM_TEAM_ANZEIGE)
				.setDefaultVal(KoSpielbaumTeamAnzeige.NR.name())
				.setDescription("config.desc.ko.spielbaum.team.anzeige"))
				.addAuswahl(KoSpielbaumTeamAnzeige.NR.name(), "Teamnummer")
				.addAuswahl(KoSpielbaumTeamAnzeige.NAME.name(), "Teamname").inSideBar());

		KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_SPIELBAUM_SPIELBAHN)
				.setDefaultVal(SpielrundeSpielbahn.X.name())
				.setDescription("config.desc.ko.spielbaum.spielbahn"))
				.addAuswahl(SpielrundeSpielbahn.X.name(), "Keine Spalte")
				.addAuswahl(SpielrundeSpielbahn.L.name(), "Leere Spalte")
				.addAuswahl(SpielrundeSpielbahn.N.name(), "Durchnummerieren (1-n)")
				.addAuswahl(SpielrundeSpielbahn.R.name(), "Zufällig vergeben").inSideBar());

		KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_SPIELBAUM_PLATZ3)
				.setDefaultVal("N")
				.setDescription("config.desc.ko.spielbaum.platz3"))
				.addAuswahl("J", "Ja").addAuswahl("N", "Nein").inSideBar());

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_TAB_COLOR_KO_TURNIERBAUM)
				.setDefaultVal(SheetTabFarben.KO_TURNIERBAUM)
				.setDescription("config.desc.tab.farbe.ko.turnierbaum").tabFarbe());

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_TURNIERBAUM_COLOR_HEADER)
				.setDefaultVal(0x2544DD).setDescription("config.desc.ko.turnierbaum.header").inSideBar());
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_TURNIERBAUM_COLOR_TEAM_A)
				.setDefaultVal(0xDCEEFA).setDescription("config.desc.ko.turnierbaum.team.a").inSideBar());
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_TURNIERBAUM_COLOR_TEAM_B)
				.setDefaultVal(0xF0F7FF).setDescription("config.desc.ko.turnierbaum.team.b").inSideBar());
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_TURNIERBAUM_COLOR_SCORE)
				.setDefaultVal(0xFFFDE7).setDescription("config.desc.ko.turnierbaum.score").inSideBar());
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_TURNIERBAUM_COLOR_SIEGER)
				.setDefaultVal(0xFFD700).setDescription("config.desc.ko.turnierbaum.sieger").inSideBar());
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_TURNIERBAUM_COLOR_BAHN)
				.setDefaultVal(0xEEEEEE).setDescription("config.desc.ko.turnierbaum.bahn").inSideBar());
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_TURNIERBAUM_COLOR_DRITTE_PLATZ)
				.setDefaultVal(0xCD7F32).setDescription("config.desc.ko.turnierbaum.dritte.platz").inSideBar());

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_GRUPPEN_GROESSE)
				.setDefaultVal(16).setDescription("config.desc.ko.gruppen.groesse")
				.inSideBar());

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_MIN_REST_GROESSE)
				.setDefaultVal(16)
				.setDescription("config.desc.ko.min.rest.groesse")
				.inSideBar());
	}

	/**
	 * Fügt die KO-Bracket-spezifischen Properties (spielbahn, teamAnzeige, spielUmPlatz3,
	 * gruppenGroesse, minRestGroesse) zur angegebenen Liste hinzu.
	 * Ermöglicht Wiederverwendung in anderen Turniersystemen (z.B. Maastrichter).
	 */
	public static void addKoBracketProperties(List<ConfigProperty<?>> props) {
		props.add(((AuswahlConfigProperty) AuswahlConfigProperty
				.from(KONFIG_PROP_SPIELBAUM_TEAM_ANZEIGE)
				.setDefaultVal(KoSpielbaumTeamAnzeige.NR.name())
				.setDescription("Team-Anzeige im Spielbaum.\r\nNR=Teamnummer\r\nNAME=Teamname"))
				.addAuswahl(KoSpielbaumTeamAnzeige.NR.name(), "Teamnummer")
				.addAuswahl(KoSpielbaumTeamAnzeige.NAME.name(), "Teamname").inSideBar());

		props.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_SPIELBAUM_SPIELBAHN)
				.setDefaultVal(SpielrundeSpielbahn.X.name())
				.setDescription("Spielbahn im Spielbaum.\r\nX=Keine Spalte\r\nL=Leere Spalte\r\nN=Durchnummerieren (1-n)\r\nR=Zufällig vergeben"))
				.addAuswahl(SpielrundeSpielbahn.X.name(), "Keine Spalte")
				.addAuswahl(SpielrundeSpielbahn.L.name(), "Leere Spalte")
				.addAuswahl(SpielrundeSpielbahn.N.name(), "Durchnummerieren (1-n)")
				.addAuswahl(SpielrundeSpielbahn.R.name(), "Zufällig vergeben").inSideBar());

		props.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_SPIELBAUM_PLATZ3)
				.setDefaultVal("N")
				.setDescription("Spiel um Platz 3 und 4 im Spielbaum anzeigen.\r\nJ=Ja\r\nN=Nein"))
				.addAuswahl("J", "Ja").addAuswahl("N", "Nein").inSideBar());

		props.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_GRUPPEN_GROESSE)
				.setDefaultVal(16).setDescription(
						"Maximale Teamanzahl pro Gruppe.\r\nBei mehr Teams werden mehrere Gruppen A, B, C … erstellt.\r\nEmpfehlung: Zweierpotenz (4, 8, 16, 32), damit volle Gruppen kein Cadrage benötigen.")
				.inSideBar());

		props.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_MIN_REST_GROESSE)
				.setDefaultVal(16)
				.setDescription(
						"Mindestanzahl Teams im Rest für ein eigenes Folgeturnier (Zweierpotenz: 4, 8, 16, 32 …).\r\nRest < diesem Wert wird in die letzte Gruppe gefaltet.")
				.inSideBar());
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

	public void setKopfZeileMitte(String text) {
		setStringProperty(KONFIG_PROP_KOPF_ZEILE_MITTE, text);
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

	public boolean isSpielbaumSpielUmPlatz3() {
		return "J".equalsIgnoreCase(readStringProperty(KONFIG_PROP_SPIELBAUM_PLATZ3));
	}

	public void setSpielbaumSpielUmPlatz3(boolean anzeigen) {
		setStringProperty(KONFIG_PROP_SPIELBAUM_PLATZ3, anzeigen ? "J" : "N");
	}

	/**
	 * Maximale Teamanzahl pro Gruppe (Default 16). Volle Gruppen benötigen kein Cadrage,
	 * wenn dieser Wert eine Zweierpotenz ist. Die letzte Gruppe kann kleiner sein.
	 */
	public int getGruppenGroesse() {
		int val = readIntProperty(KONFIG_PROP_GRUPPEN_GROESSE);
		return val > 1 ? val : 16;
	}

	public void setGruppenGroesse(int gruppenGroesse) {
		writeIntProperty(KONFIG_PROP_GRUPPEN_GROESSE, Math.max(2, gruppenGroesse));
	}

	/**
	 * Mindestzahl Teams im Rest für ein eigenes Folgeturnier (Default 16, Zweierpotenz).
	 * Rest kleiner als dieser Wert wird in die letzte volle Gruppe gefaltet.
	 */
	public int getMinRestGroesse() {
		int val = readIntProperty(KONFIG_PROP_MIN_REST_GROESSE);
		return val > 0 ? val : 16;
	}

	public void setMinRestGroesse(int minRestGroesse) {
		writeIntProperty(KONFIG_PROP_MIN_REST_GROESSE, Math.max(1, minRestGroesse));
	}

	public int getKoTurnierbaumTabFarbe() {
		return readIntProperty(KONFIG_PROP_TAB_COLOR_KO_TURNIERBAUM);
	}

	public int getTurnierbaumHeaderFarbe()      { return readIntProperty(KONFIG_PROP_TURNIERBAUM_COLOR_HEADER); }
	public int getTurnierbaumTeamAFarbe()       { return readIntProperty(KONFIG_PROP_TURNIERBAUM_COLOR_TEAM_A); }
	public int getTurnierbaumTeamBFarbe()       { return readIntProperty(KONFIG_PROP_TURNIERBAUM_COLOR_TEAM_B); }
	public int getTurnierbaumScoreFarbe()       { return readIntProperty(KONFIG_PROP_TURNIERBAUM_COLOR_SCORE); }
	public int getTurnierbaumSiegerFarbe()      { return readIntProperty(KONFIG_PROP_TURNIERBAUM_COLOR_SIEGER); }
	public int getTurnierbaumBahnFarbe()        { return readIntProperty(KONFIG_PROP_TURNIERBAUM_COLOR_BAHN); }
	public int getTurnierbaumDrittePlatzFarbe() { return readIntProperty(KONFIG_PROP_TURNIERBAUM_COLOR_DRITTE_PLATZ); }
}
