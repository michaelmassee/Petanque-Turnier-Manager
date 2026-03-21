/**
 * Erstellung 12.04.2020 / Michael Massee
 */
package de.petanqueturniermanager.schweizer.konfiguration;

import java.util.ArrayList;
import java.util.List;

import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.cellstyle.SpielrundeHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.SpielrundeHintergrundFarbeUnGeradeStyle;
import de.petanqueturniermanager.konfigdialog.AuswahlConfigProperty;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.konfigdialog.ConfigPropertyType;
import de.petanqueturniermanager.konfigdialog.HeaderFooterConfigProperty;
import de.petanqueturniermanager.supermelee.SpielRundeNr;

/**
 * @author Michael Massee
 *
 */
public class SchweizerPropertiesSpalte extends BasePropertiesSpalte implements ISchweizerPropertiesSpalte {

	public static final List<ConfigProperty<?>> KONFIG_PROPERTIES = new ArrayList<>();

	static {
		ADDBaseProp(KONFIG_PROPERTIES);
	}

	public static final String KONFIG_PROP_FREISPIEL_PUNKTE_PLUS  = "Freispiel Punkte +";
	public static final String KONFIG_PROP_FREISPIEL_PUNKTE_MINUS = "Freispiel Punkte -";

	private static final String KONFIG_PROP_KOPF_ZEILE_LINKS = "Kopfzeile Links";
	private static final String KONFIG_PROP_KOPF_ZEILE_MITTE = "Kopfzeile Mitte";
	private static final String KONFIG_PROP_KOPF_ZEILE_RECHTS = "Kopfzeile Rechts";
	public static final String KONFIG_PROP_NAME_SPIELRUNDE = "Spielrunde";

	private static final String KONFIG_PROP_SPIELRUNDE_COLOR_BACK_GERADE = "Spielrunde Hintergrund Gerade";
	private static final String KONFIG_PROP_SPIELRUNDE_COLOR_BACK_UNGERADE = "Spielrunde Hintergrund Ungerade";
	private static final String KONFIG_PROP_SPIELRUNDE_COLOR_BACK_HEADER = "Spielrunde Header";
	private static final String KONFIG_PROP_SPIELRUNDE_SPIELBAHN = "Spielrunde Spielbahn";

	private static final String KONFIG_PROP_MELDELISTE_FORMATION = "Meldeliste Formation";
	private static final String KONFIG_PROP_MELDELISTE_TEAMNAME = "Meldeliste Teamname";
	private static final String KONFIG_PROP_MELDELISTE_VEREINSNAME = "Meldeliste Vereinsname";
	private static final String KONFIG_PROP_SPIELPLAN_TEAM_ANZEIGE = "Spielplan Team Anzeige";
	private static final String KONFIG_PROP_RANKING_MODUS = "Schweizer Ranking Modus";

	static {

		KONFIG_PROPERTIES.add(HeaderFooterConfigProperty.from(KONFIG_PROP_KOPF_ZEILE_LINKS)
				.setDescription("config.desc.header.links").inSideBar());
		KONFIG_PROPERTIES.add(HeaderFooterConfigProperty.from(KONFIG_PROP_KOPF_ZEILE_MITTE)
				.setDescription("config.desc.header.mitte").inSideBar());
		KONFIG_PROPERTIES.add(HeaderFooterConfigProperty.from(KONFIG_PROP_KOPF_ZEILE_RECHTS)
				.setDescription("config.desc.header.rechts").inSideBar());

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_NAME_SPIELRUNDE)
				.setDefaultVal(1).setDescription("config.desc.aktuelle.spielrunde").inSideBarInfoPanel());

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_SPIELRUNDE_COLOR_BACK_GERADE)
				.setDefaultVal(DEFAULT_GERADE_BACK_COLOR)
				.setDescription("config.desc.spielrunde.gerade").inSideBar());
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_SPIELRUNDE_COLOR_BACK_UNGERADE)
				.setDefaultVal(DEFAULT_UNGERADE_BACK_COLOR)
				.setDescription("config.desc.spielrunde.ungerade").inSideBar());
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_SPIELRUNDE_COLOR_BACK_HEADER)
				.setDefaultVal(DEFAULT_HEADER_BACK_COLOR).setDescription("config.desc.spielrunde.header")
				.inSideBar());

		KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_SPIELRUNDE_SPIELBAHN)
				.setDefaultVal(SpielrundeSpielbahn.X.name()).setDescription("config.desc.spielbahn"))
				.addAuswahl(SpielrundeSpielbahn.X.name(), "Keine Spalte")
				.addAuswahl(SpielrundeSpielbahn.L.name(), "Leere Spalte")
				.addAuswahl(SpielrundeSpielbahn.N.name(), "Durchnummerieren (1-n)")
				.addAuswahl(SpielrundeSpielbahn.R.name(), "Zufällig vergeben").inSideBar());

		KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_MELDELISTE_FORMATION)
				.setDefaultVal(Formation.TRIPLETTE.name())
				.setDescription("config.desc.meldeliste.formation"))
				.addAuswahl(Formation.TETE.name(), Formation.TETE.getBezeichnung())
				.addAuswahl(Formation.DOUBLETTE.name(), Formation.DOUBLETTE.getBezeichnung())
				.addAuswahl(Formation.TRIPLETTE.name(), Formation.TRIPLETTE.getBezeichnung()).inSideBar());

		KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_MELDELISTE_TEAMNAME)
				.setDefaultVal("J").setDescription("config.desc.meldeliste.teamname"))
				.addAuswahl("J", "Ja").addAuswahl("N", "Nein").inSideBar());

		KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_MELDELISTE_VEREINSNAME)
				.setDefaultVal("N").setDescription("config.desc.schweizer.vereinsname"))
				.addAuswahl("J", "Ja").addAuswahl("N", "Nein").inSideBar());

		KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_SPIELPLAN_TEAM_ANZEIGE)
				.setDefaultVal(SpielplanTeamAnzeige.NR.name())
				.setDescription("config.desc.schweizer.spielplan.team.anzeige"))
				.addAuswahl(SpielplanTeamAnzeige.NR.name(), "Teamnummer")
				.addAuswahl(SpielplanTeamAnzeige.NAME.name(), "Teamname").inSideBar());

		KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_RANKING_MODUS)
				.setDefaultVal(SchweizerRankingModus.MIT_BUCHHOLZ.name())
				.setDescription("config.desc.schweizer.ranking.modus"))
				.addAuswahl(SchweizerRankingModus.MIT_BUCHHOLZ.name(), "Mit Buchholz (Standard)")
				.addAuswahl(SchweizerRankingModus.OHNE_BUCHHOLZ.name(), "Ohne Buchholz").inSideBar());

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_FREISPIEL_PUNKTE_PLUS)
				.setDefaultVal(13).setDescription("config.desc.freispiel.punkte.plus").inSideBar());
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_FREISPIEL_PUNKTE_MINUS)
				.setDefaultVal(7).setDescription("config.desc.freispiel.punkte.minus").inSideBar());

	}

	/**
	 * @param propertiesSpalte
	 * @param erstePropertiesZeile
	 * @param sheet
	 */
	protected SchweizerPropertiesSpalte(ISheet sheet) {
		super(sheet);
	}

	@Override
	protected List<ConfigProperty<?>> getKonfigProperties() {
		return KONFIG_PROPERTIES;
	}

	@Override
	public String getKopfZeileLinks() {
		return readStringProperty(KONFIG_PROP_KOPF_ZEILE_LINKS);
	}

	@Override
	public String getKopfZeileMitte() {
		return readStringProperty(KONFIG_PROP_KOPF_ZEILE_MITTE);
	}

	@Override
	public String getKopfZeileRechts() {
		return readStringProperty(KONFIG_PROP_KOPF_ZEILE_RECHTS);
	}

	@Override
	public final void setAktiveSpielRunde(SpielRundeNr spielrunde) {
		writeIntProperty(KONFIG_PROP_NAME_SPIELRUNDE, spielrunde.getNr());
	}

	@Override
	public SpielRundeNr getAktiveSpielRunde() {
		return SpielRundeNr.from(readIntProperty(KONFIG_PROP_NAME_SPIELRUNDE));
	}

	@Override
	public SpielrundeSpielbahn getSpielrundeSpielbahn() {
		return SpielrundeSpielbahn.valueOf(readStringProperty(KONFIG_PROP_SPIELRUNDE_SPIELBAHN));
	}

	@Override
	public void setSpielrundeSpielbahn(SpielrundeSpielbahn option) {
		setStringProperty(KONFIG_PROP_SPIELRUNDE_SPIELBAHN, option.name());
	}

	@Override
	public Integer getSpielRundeHintergrundFarbeGerade() {
		return readCellBackColorProperty(KONFIG_PROP_SPIELRUNDE_COLOR_BACK_GERADE);
	}

	@Override
	public SpielrundeHintergrundFarbeGeradeStyle getSpielRundeHintergrundFarbeGeradeStyle() {
		return new SpielrundeHintergrundFarbeGeradeStyle(getSpielRundeHintergrundFarbeGerade());
	}

	@Override
	public Integer getSpielRundeHintergrundFarbeUnGerade() {
		return readCellBackColorProperty(KONFIG_PROP_SPIELRUNDE_COLOR_BACK_UNGERADE);
	}

	@Override
	public SpielrundeHintergrundFarbeUnGeradeStyle getSpielRundeHintergrundFarbeUnGeradeStyle() {
		return new SpielrundeHintergrundFarbeUnGeradeStyle(getSpielRundeHintergrundFarbeUnGerade());
	}

	@Override
	public Integer getSpielRundeHeaderFarbe() {
		return readCellBackColorProperty(KONFIG_PROP_SPIELRUNDE_COLOR_BACK_HEADER);
	}

	@Override
	public Formation getMeldeListeFormation() {
		String val = readStringProperty(KONFIG_PROP_MELDELISTE_FORMATION);
		Formation formation = Formation.valueOf(val);
		return formation != null ? formation : Formation.TRIPLETTE;
	}

	@Override
	public boolean isMeldeListeTeamnameAnzeigen() {
		return "J".equalsIgnoreCase(readStringProperty(KONFIG_PROP_MELDELISTE_TEAMNAME));
	}

	@Override
	public boolean isMeldeListeVereinsnameAnzeigen() {
		return "J".equalsIgnoreCase(readStringProperty(KONFIG_PROP_MELDELISTE_VEREINSNAME));
	}

	@Override
	public void setMeldeListeFormation(Formation formation) {
		setStringProperty(KONFIG_PROP_MELDELISTE_FORMATION, formation.name());
	}

	@Override
	public void setMeldeListeTeamnameAnzeigen(boolean anzeigen) {
		setStringProperty(KONFIG_PROP_MELDELISTE_TEAMNAME, anzeigen ? "J" : "N");
	}

	@Override
	public void setMeldeListeVereinsnameAnzeigen(boolean anzeigen) {
		setStringProperty(KONFIG_PROP_MELDELISTE_VEREINSNAME, anzeigen ? "J" : "N");
	}

	@Override
	public SpielplanTeamAnzeige getSpielplanTeamAnzeige() {
		String val = readStringProperty(KONFIG_PROP_SPIELPLAN_TEAM_ANZEIGE);
		try {
			return SpielplanTeamAnzeige.valueOf(val);
		} catch (IllegalArgumentException | NullPointerException e) {
			return SpielplanTeamAnzeige.NR;
		}
	}

	@Override
	public void setSpielplanTeamAnzeige(SpielplanTeamAnzeige anzeige) {
		setStringProperty(KONFIG_PROP_SPIELPLAN_TEAM_ANZEIGE, anzeige.name());
	}

	@Override
	public SchweizerRankingModus getRankingModus() {
		String val = readStringProperty(KONFIG_PROP_RANKING_MODUS);
		try {
			return SchweizerRankingModus.valueOf(val);
		} catch (IllegalArgumentException | NullPointerException e) {
			return SchweizerRankingModus.MIT_BUCHHOLZ;
		}
	}

	@Override
	public void setRankingModus(SchweizerRankingModus modus) {
		setStringProperty(KONFIG_PROP_RANKING_MODUS, modus.name());
	}

	@Override
	public Integer getFreispielPunktePlus() {
		return readIntProperty(KONFIG_PROP_FREISPIEL_PUNKTE_PLUS);
	}

	@Override
	public Integer getFreispielPunkteMinus() {
		return readIntProperty(KONFIG_PROP_FREISPIEL_PUNKTE_MINUS);
	}

}
