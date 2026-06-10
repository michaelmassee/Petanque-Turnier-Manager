package de.petanqueturniermanager.jedergegenjeden.konfiguration;

import java.util.ArrayList;
import java.util.List;

import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.upload.UploadProtokoll;
import de.petanqueturniermanager.konfigdialog.AuswahlConfigProperty;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.konfigdialog.ConfigPropertyType;
import de.petanqueturniermanager.konfigdialog.HeaderFooterConfigProperty;
import de.petanqueturniermanager.schweizer.konfiguration.SpielplanTeamAnzeige;

/**
 * Erstellung 01.08.2022 / Michael Massee
 */

public class JGJPropertiesSpalte extends BasePropertiesSpalte implements IJGJProperiesSpalte {

	public static final List<ConfigProperty<?>> KONFIG_PROPERTIES = new ArrayList<>();

	public static final String KONFIG_PROP_FREISPIEL_PUNKTE_PLUS  = "Freispiel Punkte +";
	public static final String KONFIG_PROP_FREISPIEL_PUNKTE_MINUS = "Freispiel Punkte -";
	public static final String KONFIG_PROP_GRUPPENGROESSE = "Gruppengroesse";
	public static final String KONFIG_PROP_RUECKRUNDE = "Rueckrunde";

	private static final String KONFIG_PROP_SPIELPLAN_COLOR_BACK_GERADE = "Spielplan Hintergrund Gerade";
	private static final String KONFIG_PROP_SPIELPLAN_COLOR_BACK_UNGERADE = "Spielplan Hintergrund Ungerade";
	private static final String KONFIG_PROP_SPIELPLAN_COLOR_BACK_HEADER = "Spielplan Header";

	private static final String KONFIG_PROP_KOPF_ZEILE_LINKS = "Kopfzeile Links";
	private static final String KONFIG_PROP_KOPF_ZEILE_MITTE = "Kopfzeile Mitte";
	private static final String KONFIG_PROP_KOPF_ZEILE_RECHTS = "Kopfzeile Rechts";

	private static final String KONFIG_PROP_MELDELISTE_FORMATION = "Meldeliste Formation";
	private static final String KONFIG_PROP_MELDELISTE_TEAMNAME = "Meldeliste Teamname";
	private static final String KONFIG_PROP_MELDELISTE_VEREINSNAME = "Meldeliste Vereinsname";
	private static final String KONFIG_PROP_SPIELPLAN_TEAM_ANZEIGE = "Spielplan Team Anzeige";


	static {
		ADDBaseProp(KONFIG_PROPERTIES);
		addCheckinSortProp(KONFIG_PROPERTIES);
		addTeilnehmerListeSortProp(KONFIG_PROPERTIES);
	}

	static {
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_SPIELPLAN_COLOR_BACK_GERADE)
				.setDefaultVal(DEFAULT_GERADE_BACK_COLOR).setDescription("config.desc.jgj.spielplan.gerade"));
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_SPIELPLAN_COLOR_BACK_UNGERADE)
				.setDefaultVal(DEFAULT_UNGERADE_BACK_COLOR)
				.setDescription("config.desc.jgj.spielplan.ungerade"));
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_SPIELPLAN_COLOR_BACK_HEADER)
				.setDefaultVal(DEFAULT_HEADER_BACK_COLOR).setDescription("config.desc.jgj.spielplan.header"));

		KONFIG_PROPERTIES.add(HeaderFooterConfigProperty.from(KONFIG_PROP_KOPF_ZEILE_LINKS)
				.setDescription("config.desc.header.links"));
		KONFIG_PROPERTIES.add(HeaderFooterConfigProperty.from(KONFIG_PROP_KOPF_ZEILE_MITTE)
				.setDescription("config.desc.header.mitte"));
		KONFIG_PROPERTIES.add(HeaderFooterConfigProperty.from(KONFIG_PROP_KOPF_ZEILE_RECHTS)
				.setDescription("config.desc.header.rechts"));

		KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_MELDELISTE_FORMATION)
				.setDefaultVal(Formation.TETE.name())
				.setDescription("config.desc.meldeliste.formation"))
				.addAuswahl(Formation.TETE.name(), Formation.TETE.getBezeichnung())
				.addAuswahl(Formation.DOUBLETTE.name(), Formation.DOUBLETTE.getBezeichnung())
				.addAuswahl(Formation.TRIPLETTE.name(), Formation.TRIPLETTE.getBezeichnung()));

		KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_MELDELISTE_TEAMNAME)
				.setDefaultVal("N").setDescription("config.desc.meldeliste.teamname"))
				.addAuswahl("J", "Ja").addAuswahl("N", "Nein"));

		KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_MELDELISTE_VEREINSNAME)
				.setDefaultVal("N").setDescription("config.desc.jgj.vereinsname"))
				.addAuswahl("J", "Ja").addAuswahl("N", "Nein"));

		KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_SPIELPLAN_TEAM_ANZEIGE)
				.setDefaultVal(SpielplanTeamAnzeige.NR.name())
				.setDescription("config.desc.jgj.spielplan.team.anzeige"))
				.addAuswahl(SpielplanTeamAnzeige.NR.name(), "Teamnummer")
				.addAuswahl(SpielplanTeamAnzeige.NAME.name(), "Teamname"));

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_FREISPIEL_PUNKTE_PLUS)
				.setDefaultVal(13).setDescription("config.desc.freispiel.punkte.plus"));
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_FREISPIEL_PUNKTE_MINUS)
				.setDefaultVal(7).setDescription("config.desc.freispiel.punkte.minus"));
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_GRUPPENGROESSE)
				.setDefaultVal(0).setDescription("config.desc.jgj.gruppengroesse"));
		KONFIG_PROPERTIES.add(ConfigProperty.<Boolean>from(ConfigPropertyType.BOOLEAN, KONFIG_PROP_RUECKRUNDE)
				.setDefaultVal(Boolean.FALSE).setDescription("config.desc.jgj.rueckrunde"));

		ADDUploadProp(KONFIG_PROPERTIES);
	}

	/**
	 * @param propertiesSpalte
	 * @param erstePropertiesZeile
	 * @param sheet
	 */
	JGJPropertiesSpalte(ISheet sheet) {
		super(sheet);
	}

	@Override
	protected List<ConfigProperty<?>> getKonfigProperties() {
		return KONFIG_PROPERTIES;
	}

	@Override
	public Integer getSpielPlanHintergrundFarbeGerade() {
		return readCellBackColorProperty(KONFIG_PROP_SPIELPLAN_COLOR_BACK_GERADE);
	}

	@Override
	public Integer getSpielPlanHintergrundFarbeUnGerade() {
		return readCellBackColorProperty(KONFIG_PROP_SPIELPLAN_COLOR_BACK_UNGERADE);
	}

	@Override
	public Integer getSpielPlanHeaderFarbe() {
		return readCellBackColorProperty(KONFIG_PROP_SPIELPLAN_COLOR_BACK_HEADER);
	}

	@Override
	public String getKopfZeileLinks() {
		return readStringProperty(KONFIG_PROP_KOPF_ZEILE_LINKS);
	}

	@Override
	public String getKopfZeileMitte() {
		return readStringProperty(KONFIG_PROP_KOPF_ZEILE_MITTE);
	}

	public void setKopfZeileMitte(String text) {
		setStringProperty(KONFIG_PROP_KOPF_ZEILE_MITTE, text);
	}

	@Override
	public String getKopfZeileRechts() {
		return readStringProperty(KONFIG_PROP_KOPF_ZEILE_RECHTS);
	}

	@Override
	public Integer getFreispielPunktePlus() {
		return readIntProperty(KONFIG_PROP_FREISPIEL_PUNKTE_PLUS);
	}

	@Override
	public Integer getFreispielPunkteMinus() {
		return readIntProperty(KONFIG_PROP_FREISPIEL_PUNKTE_MINUS);
	}

	@Override
	public Formation getMeldeListeFormation() {
		return readEnumProperty(KONFIG_PROP_MELDELISTE_FORMATION, Formation.class, Formation.TETE);
	}

	@Override
	public void setMeldeListeFormation(Formation formation) {
		setStringProperty(KONFIG_PROP_MELDELISTE_FORMATION, formation.name());
	}

	@Override
	public boolean isMeldeListeTeamnameAnzeigen() {
		return "J".equalsIgnoreCase(readStringProperty(KONFIG_PROP_MELDELISTE_TEAMNAME));
	}

	@Override
	public void setMeldeListeTeamnameAnzeigen(boolean anzeigen) {
		setStringProperty(KONFIG_PROP_MELDELISTE_TEAMNAME, anzeigen ? "J" : "N");
	}

	@Override
	public boolean isMeldeListeVereinsnameAnzeigen() {
		return "J".equalsIgnoreCase(readStringProperty(KONFIG_PROP_MELDELISTE_VEREINSNAME));
	}

	@Override
	public void setMeldeListeVereinsnameAnzeigen(boolean anzeigen) {
		setStringProperty(KONFIG_PROP_MELDELISTE_VEREINSNAME, anzeigen ? "J" : "N");
	}

	@Override
	public SpielplanTeamAnzeige getSpielplanTeamAnzeige() {
		return readEnumProperty(KONFIG_PROP_SPIELPLAN_TEAM_ANZEIGE, SpielplanTeamAnzeige.class, SpielplanTeamAnzeige.NR);
	}

	@Override
	public void setSpielplanTeamAnzeige(SpielplanTeamAnzeige anzeige) {
		setStringProperty(KONFIG_PROP_SPIELPLAN_TEAM_ANZEIGE, anzeige.name());
	}

	@Override
	public int getGruppengroesse() {
		return Math.max(0, readIntProperty(KONFIG_PROP_GRUPPENGROESSE));
	}

	@Override
	public void setGruppengroesse(int groesse) {
		writeIntProperty(KONFIG_PROP_GRUPPENGROESSE, Math.max(0, groesse));
	}

	@Override
	public boolean isRueckrunde() {
		Boolean val = readBooleanProperty(KONFIG_PROP_RUECKRUNDE);
		return val != null && val;
	}

	@Override
	public void setRueckrunde(boolean mitRueckrunde) {
		setStringProperty(KONFIG_PROP_RUECKRUNDE, de.petanqueturniermanager.helper.StringTools.booleanToString(mitRueckrunde));
	}

	@Override
	public UploadProtokoll getUploadProtokoll() {
		return UploadProtokoll.vonString(readStringProperty(KONFIG_PROP_UPLOAD_PROTOKOLL));
	}

	@Override
	public String getUploadHost() {
		return readStringProperty(KONFIG_PROP_UPLOAD_HOST);
	}

	@Override
	public int getUploadPort() {
		Integer port = readIntProperty(KONFIG_PROP_UPLOAD_PORT);
		return port != null ? port : 21;
	}

	@Override
	public String getUploadBenutzer() {
		return readStringProperty(KONFIG_PROP_UPLOAD_BENUTZER);
	}

	@Override
	public String getUploadVerzeichnis() {
		return readStringProperty(KONFIG_PROP_UPLOAD_VERZEICHNIS);
	}

}
