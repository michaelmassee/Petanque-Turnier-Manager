/*
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
		addCheckinSortProp(KONFIG_PROPERTIES);
	}

	public static final String KONFIG_PROP_TAB_COLOR_KO_TURNIERBAUM       = "Tab-Farbe KO-Turnierbaum";

	public static final String KONFIG_PROP_TURNIERBAUM_COLOR_HEADER       = "Turnierbaum Header Farbe";
	public static final String KONFIG_PROP_TURNIERBAUM_COLOR_TEAM_A       = "Turnierbaum Team A Farbe";
	public static final String KONFIG_PROP_TURNIERBAUM_COLOR_TEAM_B       = "Turnierbaum Team B Farbe";
	public static final String KONFIG_PROP_TURNIERBAUM_COLOR_SIEGER       = "Turnierbaum Sieger Farbe";
	public static final String KONFIG_PROP_TURNIERBAUM_COLOR_BAHN         = "Turnierbaum Bahn Farbe";
	public static final String KONFIG_PROP_TURNIERBAUM_COLOR_DRITTE_PLATZ = "Turnierbaum 3. Platz Farbe";

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

	/**
	 * Erlaubte Werte für {@link #KONFIG_PROP_GRUPPEN_GROESSE}.
	 * Ausschließlich Zweierpotenzen — nur dann benötigen volle Gruppen kein Cadrage.
	 */
	private static final List<Integer> ERLAUBTE_GRUPPEN_GROESSEN = List.of(4, 8, 16, 32, 64, 128, 256);

	private static final int DEFAULT_GRUPPEN_GROESSE = 16;

	/**
	 * Liste der für {@link #KONFIG_PROP_GRUPPEN_GROESSE} erlaubten Werte (Zweierpotenzen).
	 * Wird von Dialog-Komponenten benötigt, die eine ListBox mit denselben Werten füllen.
	 */
	public static List<Integer> getErlaubteGruppenGroessen() {
		return ERLAUBTE_GRUPPEN_GROESSEN;
	}

	/**
	 * Liefert den Index in {@link #getErlaubteGruppenGroessen()}, der dem gegebenen Wert
	 * (nach Normalisierung) entspricht. Für ListBox-Vorauswahl.
	 */
	public static int indexAusGruppenGroesse(int wert) {
		int snapped = normalisiereGruppenGroesse(wert);
		return ERLAUBTE_GRUPPEN_GROESSEN.indexOf(snapped);
	}

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
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_TURNIERBAUM_COLOR_SIEGER)
				.setDefaultVal(0xFFD700).setDescription("config.desc.ko.turnierbaum.sieger").inSideBar());
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_TURNIERBAUM_COLOR_BAHN)
				.setDefaultVal(0xEEEEEE).setDescription("config.desc.ko.turnierbaum.bahn").inSideBar());
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_TURNIERBAUM_COLOR_DRITTE_PLATZ)
				.setDefaultVal(0xCD7F32).setDescription("config.desc.ko.turnierbaum.dritte.platz").inSideBar());

		KONFIG_PROPERTIES.add(buildGruppenGroesseProperty());
	}

	private static AuswahlConfigProperty buildGruppenGroesseProperty() {
		AuswahlConfigProperty prop = (AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_GRUPPEN_GROESSE)
				.setDefaultVal(Integer.toString(DEFAULT_GRUPPEN_GROESSE))
				.setDescription("config.desc.ko.gruppen.groesse");
		for (Integer val : ERLAUBTE_GRUPPEN_GROESSEN) {
			String s = val.toString();
			prop.addAuswahl(s, s);
		}
		return (AuswahlConfigProperty) prop.inSideBar();
	}

	/**
	 * Fügt die KO-Bracket-spezifischen Properties (spielbahn, teamAnzeige, spielUmPlatz3,
	 * gruppenGroesse) zur angegebenen Liste hinzu.
	 * Ermöglicht Wiederverwendung in anderen Turniersystemen (z.B. Maastrichter).
	 */
	public static void addKoBracketProperties(List<ConfigProperty<?>> props) {
		props.add(((AuswahlConfigProperty) AuswahlConfigProperty
				.from(KONFIG_PROP_SPIELBAUM_TEAM_ANZEIGE)
				.setDefaultVal(KoSpielbaumTeamAnzeige.NR.name())
				.setDescription("config.desc.ko.spielbaum.team.anzeige"))
				.addAuswahl(KoSpielbaumTeamAnzeige.NR.name(), "Teamnummer")
				.addAuswahl(KoSpielbaumTeamAnzeige.NAME.name(), "Teamname").inSideBar());

		props.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_SPIELBAUM_SPIELBAHN)
				.setDefaultVal(SpielrundeSpielbahn.X.name())
				.setDescription("config.desc.ko.spielbaum.spielbahn"))
				.addAuswahl(SpielrundeSpielbahn.X.name(), "Keine Spalte")
				.addAuswahl(SpielrundeSpielbahn.L.name(), "Leere Spalte")
				.addAuswahl(SpielrundeSpielbahn.N.name(), "Durchnummerieren (1-n)")
				.addAuswahl(SpielrundeSpielbahn.R.name(), "Zufällig vergeben").inSideBar());

		props.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_SPIELBAUM_PLATZ3)
				.setDefaultVal("N")
				.setDescription("config.desc.ko.spielbaum.platz3"))
				.addAuswahl("J", "Ja").addAuswahl("N", "Nein").inSideBar());

		props.add(buildGruppenGroesseProperty());
	}

	/**
	 * Fügt die KO-Bracket-Farbproperties (Tab-Farbe, Header, Team A/B, Sieger, Bahn, 3. Platz)
	 * zur angegebenen Liste hinzu. Ermöglicht Wiederverwendung in anderen Turniersystemen.
	 */
	public static void addKoBracketColorProperties(List<ConfigProperty<?>> props) {
		props.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_TAB_COLOR_KO_TURNIERBAUM)
				.setDefaultVal(SheetTabFarben.KO_TURNIERBAUM)
				.setDescription("config.desc.tab.farbe.ko.turnierbaum").tabFarbe());
		props.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_TURNIERBAUM_COLOR_HEADER)
				.setDefaultVal(0x2544DD).setDescription("config.desc.ko.turnierbaum.header").inSideBar());
		props.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_TURNIERBAUM_COLOR_TEAM_A)
				.setDefaultVal(0xDCEEFA).setDescription("config.desc.ko.turnierbaum.team.a").inSideBar());
		props.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_TURNIERBAUM_COLOR_TEAM_B)
				.setDefaultVal(0xF0F7FF).setDescription("config.desc.ko.turnierbaum.team.b").inSideBar());
		props.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_TURNIERBAUM_COLOR_SIEGER)
				.setDefaultVal(0xFFD700).setDescription("config.desc.ko.turnierbaum.sieger").inSideBar());
		props.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_TURNIERBAUM_COLOR_BAHN)
				.setDefaultVal(0xEEEEEE).setDescription("config.desc.ko.turnierbaum.bahn").inSideBar());
		props.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_TURNIERBAUM_COLOR_DRITTE_PLATZ)
				.setDefaultVal(0xCD7F32).setDescription("config.desc.ko.turnierbaum.dritte.platz").inSideBar());
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
		return readEnumProperty(KONFIG_PROP_MELDELISTE_FORMATION, Formation.class, Formation.DOUBLETTE);
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
		return readEnumProperty(KONFIG_PROP_SPIELBAUM_TEAM_ANZEIGE, KoSpielbaumTeamAnzeige.class, KoSpielbaumTeamAnzeige.NR);
	}

	public void setSpielbaumTeamAnzeige(KoSpielbaumTeamAnzeige anzeige) {
		setStringProperty(KONFIG_PROP_SPIELBAUM_TEAM_ANZEIGE, anzeige.name());
	}

	public SpielrundeSpielbahn getSpielbaumSpielbahn() {
		return readEnumProperty(KONFIG_PROP_SPIELBAUM_SPIELBAHN, SpielrundeSpielbahn.class, SpielrundeSpielbahn.X);
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
	 * Maximale Teamanzahl pro Gruppe (Default 16). Erlaubte Werte sind ausschließlich
	 * Zweierpotenzen aus {@link #ERLAUBTE_GRUPPEN_GROESSEN}; nur dann benötigen volle
	 * Gruppen kein Cadrage. Die letzte Gruppe kann kleiner sein.
	 */
	public int getGruppenGroesse() {
		return normalisiereGruppenGroesse(readStringProperty(KONFIG_PROP_GRUPPEN_GROESSE));
	}

	public void setGruppenGroesse(int gruppenGroesse) {
		setStringProperty(KONFIG_PROP_GRUPPEN_GROESSE,
				Integer.toString(normalisiereGruppenGroesse(gruppenGroesse)));
	}

	/**
	 * Snapped einen beliebigen Integer auf die nächst-höhere erlaubte Gruppengröße
	 * aus {@link #ERLAUBTE_GRUPPEN_GROESSEN}. Werte ≤ 0 ergeben den Default 16,
	 * Werte über 256 werden auf 256 gekappt.
	 */
	public static int normalisiereGruppenGroesse(int wert) {
		if (wert <= 0) {
			return DEFAULT_GRUPPEN_GROESSE;
		}
		for (Integer erlaubt : ERLAUBTE_GRUPPEN_GROESSEN) {
			if (wert <= erlaubt) {
				return erlaubt;
			}
		}
		return ERLAUBTE_GRUPPEN_GROESSEN.get(ERLAUBTE_GRUPPEN_GROESSEN.size() - 1);
	}

	/**
	 * Robust gegen Alt-Werte: parst auch Float-Strings ("16.0"), tolerant gegenüber
	 * leeren/ungültigen Eingaben (→ Default 16). Snapped anschließend auf erlaubte Werte.
	 */
	public static int normalisiereGruppenGroesse(String wert) {
		if (wert == null || wert.isBlank()) {
			return DEFAULT_GRUPPEN_GROESSE;
		}
		try {
			return normalisiereGruppenGroesse((int) Math.round(Double.parseDouble(wert.trim())));
		} catch (NumberFormatException e) {
			return DEFAULT_GRUPPEN_GROESSE;
		}
	}

	public int getKoTurnierbaumTabFarbe() {
		return readIntProperty(KONFIG_PROP_TAB_COLOR_KO_TURNIERBAUM);
	}

	public int getTurnierbaumHeaderFarbe()      { return readIntProperty(KONFIG_PROP_TURNIERBAUM_COLOR_HEADER); }
	public int getTurnierbaumTeamAFarbe()       { return readIntProperty(KONFIG_PROP_TURNIERBAUM_COLOR_TEAM_A); }
	public int getTurnierbaumTeamBFarbe()       { return readIntProperty(KONFIG_PROP_TURNIERBAUM_COLOR_TEAM_B); }
	public int getTurnierbaumSiegerFarbe()      { return readIntProperty(KONFIG_PROP_TURNIERBAUM_COLOR_SIEGER); }
	public int getTurnierbaumBahnFarbe()        { return readIntProperty(KONFIG_PROP_TURNIERBAUM_COLOR_BAHN); }
	public int getTurnierbaumDrittePlatzFarbe() { return readIntProperty(KONFIG_PROP_TURNIERBAUM_COLOR_DRITTE_PLATZ); }
}
