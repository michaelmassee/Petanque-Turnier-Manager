/*
 * Erstellung 12.04.2020 / Michael Massee
 */
package de.petanqueturniermanager.schweizer.konfiguration;

import java.util.ArrayList;
import java.util.List;

import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.StringTools;
import de.petanqueturniermanager.helper.cellstyle.SpielrundeHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.SpielrundeHintergrundFarbeUnGeradeStyle;
import de.petanqueturniermanager.konfigdialog.AuswahlConfigProperty;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.konfigdialog.ConfigPropertyType;
import de.petanqueturniermanager.konfigdialog.HeaderFooterConfigProperty;
import de.petanqueturniermanager.konfigdialog.ZeitplanConfigProperty;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;

/**
 * @author Michael Massee
 *
 */
public class SchweizerPropertiesSpalte extends BasePropertiesSpalte implements ISchweizerPropertiesSpalte {

	public static final List<ConfigProperty<?>> KONFIG_PROPERTIES = new ArrayList<>();

	static {
		ADDBaseProp(KONFIG_PROPERTIES);
		addCheckinSortProp(KONFIG_PROPERTIES);
		addTeilnehmerListeSortProp(KONFIG_PROPERTIES);
	}

	public static final String KONFIG_PROP_FREISPIEL_PUNKTE_PLUS  = "Freispiel Punkte +";
	public static final String KONFIG_PROP_FREISPIEL_PUNKTE_MINUS = "Freispiel Punkte -";

	private static final String KONFIG_PROP_KOPF_ZEILE_LINKS = "Kopfzeile Links";
	private static final String KONFIG_PROP_KOPF_ZEILE_MITTE = "Kopfzeile Mitte";
	private static final String KONFIG_PROP_KOPF_ZEILE_RECHTS = "Kopfzeile Rechts";
	public static final String KONFIG_PROP_NAME_SPIELRUNDE = "Spielrunde";
	/** Aktueller Spieltag einer Turnierserie (analog SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELTAG). */
	public static final String KONFIG_PROP_NAME_SPIELTAG = "Spieltag";

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
				.setDescription("config.desc.header.links"));
		KONFIG_PROPERTIES.add(HeaderFooterConfigProperty.from(KONFIG_PROP_KOPF_ZEILE_MITTE)
				.setDescription("config.desc.header.mitte"));
		KONFIG_PROPERTIES.add(HeaderFooterConfigProperty.from(KONFIG_PROP_KOPF_ZEILE_RECHTS)
				.setDescription("config.desc.header.rechts"));

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_NAME_SPIELRUNDE)
				.setDefaultVal(1).setDescription("config.desc.aktuelle.spielrunde"));
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_NAME_SPIELTAG)
				.setDefaultVal(1).setDescription("config.desc.schweizer.spieltag"));

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_SPIELRUNDE_COLOR_BACK_GERADE)
				.setDefaultVal(DEFAULT_GERADE_BACK_COLOR)
				.setDescription("config.desc.spielrunde.gerade"));
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_SPIELRUNDE_COLOR_BACK_UNGERADE)
				.setDefaultVal(DEFAULT_UNGERADE_BACK_COLOR)
				.setDescription("config.desc.spielrunde.ungerade"));
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_SPIELRUNDE_COLOR_BACK_HEADER)
				.setDefaultVal(DEFAULT_HEADER_BACK_COLOR).setDescription("config.desc.spielrunde.header"));

		KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_SPIELRUNDE_SPIELBAHN)
				.setDefaultVal(SpielrundeSpielbahn.X.name()).setDescription("config.desc.spielbahn"))
				.addAuswahl(SpielrundeSpielbahn.X.name(), "Keine Spalte")
				.addAuswahl(SpielrundeSpielbahn.L.name(), "Leere Spalte")
				.addAuswahl(SpielrundeSpielbahn.N.name(), "Durchnummerieren (1-n)")
				.addAuswahl(SpielrundeSpielbahn.R.name(), "Zufällig vergeben"));

		// Formation/Teamname/Vereinsname sind nach Meldeliste-Erstellung nicht mehr änderbar
		// (nur noch über den Turnier-Parameter-Dialog beim Anlegen) - daher intern(), nicht im Options-Dialog.
		KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_MELDELISTE_FORMATION)
				.setDefaultVal(Formation.TRIPLETTE.name())
				.setDescription("config.desc.meldeliste.formation"))
				.addAuswahl(Formation.TETE.name(), Formation.TETE.getBezeichnung())
				.addAuswahl(Formation.DOUBLETTE.name(), Formation.DOUBLETTE.getBezeichnung())
				.addAuswahl(Formation.TRIPLETTE.name(), Formation.TRIPLETTE.getBezeichnung())
				.addAuswahl(Formation.NUR_TEAMNAME.name(), Formation.NUR_TEAMNAME.getBezeichnung())
				.intern());

		KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_MELDELISTE_TEAMNAME)
				.setDefaultVal("J").setDescription("config.desc.meldeliste.teamname"))
				.addAuswahl("J", "Ja").addAuswahl("N", "Nein")
				.intern());

		KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_MELDELISTE_VEREINSNAME)
				.setDefaultVal("N").setDescription("config.desc.schweizer.vereinsname"))
				.addAuswahl("J", "Ja").addAuswahl("N", "Nein")
				.intern());

		KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_SPIELPLAN_TEAM_ANZEIGE)
				.setDefaultVal(SpielplanTeamAnzeige.NR.name())
				.setDescription("config.desc.schweizer.spielplan.team.anzeige"))
				.addAuswahl(SpielplanTeamAnzeige.NR.name(), "Teamnummer")
				.addAuswahl(SpielplanTeamAnzeige.NAME.name(), "Teamname"));

		KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_RANKING_MODUS)
				.setDefaultVal(SchweizerRankingModus.MIT_BUCHHOLZ.name())
				.setDescription("config.desc.schweizer.ranking.modus"))
				.addAuswahl(SchweizerRankingModus.MIT_BUCHHOLZ.name(), "Mit Buchholz (Standard)")
				.addAuswahl(SchweizerRankingModus.OHNE_BUCHHOLZ.name(), "Ohne Buchholz"));

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_FREISPIEL_PUNKTE_PLUS)
				.setDefaultVal(13).setDescription("config.desc.freispiel.punkte.plus"));
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_FREISPIEL_PUNKTE_MINUS)
				.setDefaultVal(7).setDescription("config.desc.freispiel.punkte.minus"));

		KONFIG_PROPERTIES.add(ZeitplanConfigProperty.<Boolean>from(ConfigPropertyType.BOOLEAN, KONFIG_PROP_ZEITPLAN_AKTIV)
				.setDefaultVal(false).setDescription("config.desc.zeitplan.aktiv"));
		KONFIG_PROPERTIES.add(ZeitplanConfigProperty.<Integer>from(ConfigPropertyType.INTEGER, KONFIG_PROP_ZEITPLAN_ANZAHL_BAHNEN)
				.setDefaultVal(0).setDescription("config.desc.zeitplan.anzahl.bahnen"));
		KONFIG_PROPERTIES.add(ZeitplanConfigProperty.<Integer>from(ConfigPropertyType.INTEGER, KONFIG_PROP_ZEITPLAN_ZEITLIMIT_MINUTEN)
				.setDefaultVal(15).setDescription("config.desc.zeitplan.zeitlimit"));
		KONFIG_PROPERTIES.add(ZeitplanConfigProperty.<Integer>from(ConfigPropertyType.INTEGER, KONFIG_PROP_ZEITPLAN_DURCHGANG_PAUSE_MINUTEN)
				.setDefaultVal(5).setDescription("config.desc.zeitplan.durchgang.pause"));
		KONFIG_PROPERTIES.add(ZeitplanConfigProperty.<Integer>from(ConfigPropertyType.INTEGER, KONFIG_PROP_ZEITPLAN_RUNDEN_PAUSE_MINUTEN)
				.setDefaultVal(10).setDescription("config.desc.zeitplan.runden.pause"));
		KONFIG_PROPERTIES.add(ZeitplanConfigProperty.<String>from(ConfigPropertyType.STRING, KONFIG_PROP_ZEITPLAN_TURNIER_STARTZEIT)
				.setDefaultVal("09:00").setDescription("config.desc.zeitplan.turnier.startzeit").kompaktesTextfeld()
				.validierung(StringTools::isValidUhrzeitHhMm));

		ADDUploadProp(KONFIG_PROPERTIES);
		ADDSpielrundenExportProp(KONFIG_PROPERTIES);
		ADDTeilnehmerlisteExportProp(KONFIG_PROPERTIES);
		ADDAbschlussSheetExportProp(KONFIG_PROPERTIES);

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

	public void setKopfZeileMitte(String text) {
		setStringProperty(KONFIG_PROP_KOPF_ZEILE_MITTE, text);
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
	public final void setAktiveSpieltag(SpielTagNr spieltag) {
		writeIntProperty(KONFIG_PROP_NAME_SPIELTAG, spieltag.getNr());
	}

	@Override
	public SpielTagNr getAktiveSpieltag() {
		return SpielTagNr.from(readIntProperty(KONFIG_PROP_NAME_SPIELTAG));
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
		return readEnumProperty(KONFIG_PROP_MELDELISTE_FORMATION, Formation.class, Formation.TRIPLETTE);
	}

	@Override
	public boolean isMeldeListeTeamnameAnzeigen() {
		// Nur Teamname: Teamname-Anzeige ist die einzige Team-Identität und daher zwingend aktiv,
		// unabhängig vom gespeicherten Rohwert (z.B. bei älteren Dateien).
		if (getMeldeListeFormation() == Formation.NUR_TEAMNAME) {
			return true;
		}
		return "J".equalsIgnoreCase(readStringProperty(KONFIG_PROP_MELDELISTE_TEAMNAME));
	}

	@Override
	public boolean isMeldeListeVereinsnameAnzeigen() {
		return "J".equalsIgnoreCase(readStringProperty(KONFIG_PROP_MELDELISTE_VEREINSNAME));
	}

	@Override
	public void setMeldeListeFormation(Formation formation) {
		setStringProperty(KONFIG_PROP_MELDELISTE_FORMATION, formation.name());
		if (formation == Formation.NUR_TEAMNAME) {
			setMeldeListeTeamnameAnzeigen(true); // persistiert den erzwungenen Zustand
		}
	}

	@Override
	public void setMeldeListeTeamnameAnzeigen(boolean anzeigen) {
		// Rohen Property-Wert prüfen (nicht getSpielplanTeamAnzeige(): dessen defensiver Fallback
		// würde nach dem Deaktivieren immer NR liefern und den Reset in Zeile unten verhindern —
		// der gespeicherte Wert bliebe fälschlich auf NAME stehen).
		if (!anzeigen && readRawSpielplanTeamAnzeige() == SpielplanTeamAnzeige.NAME) {
			setStringProperty(KONFIG_PROP_SPIELPLAN_TEAM_ANZEIGE, SpielplanTeamAnzeige.NR.name());
		}
		setStringProperty(KONFIG_PROP_MELDELISTE_TEAMNAME, anzeigen ? "J" : "N");
	}

	@Override
	public void setMeldeListeVereinsnameAnzeigen(boolean anzeigen) {
		setStringProperty(KONFIG_PROP_MELDELISTE_VEREINSNAME, anzeigen ? "J" : "N");
	}

	@Override
	public SpielplanTeamAnzeige getSpielplanTeamAnzeige() {
		SpielplanTeamAnzeige anzeige = readRawSpielplanTeamAnzeige();
		// Defensive Absicherung (z.B. bei älteren Dateien mit inkonsistent gesetzten Properties):
		// ohne Meldeliste-Teamname gibt es keine Team-Namen zum Auflösen, siehe setMeldeListeTeamnameAnzeigen.
		if (anzeige == SpielplanTeamAnzeige.NAME && !isMeldeListeTeamnameAnzeigen()) {
			return SpielplanTeamAnzeige.NR;
		}
		return anzeige;
	}

	private SpielplanTeamAnzeige readRawSpielplanTeamAnzeige() {
		return readEnumProperty(KONFIG_PROP_SPIELPLAN_TEAM_ANZEIGE, SpielplanTeamAnzeige.class, SpielplanTeamAnzeige.NR);
	}

	@Override
	public void setSpielplanTeamAnzeige(SpielplanTeamAnzeige anzeige) {
		setStringProperty(KONFIG_PROP_SPIELPLAN_TEAM_ANZEIGE, anzeige.name());
		if (anzeige == SpielplanTeamAnzeige.NAME && !isMeldeListeTeamnameAnzeigen()) {
			setMeldeListeTeamnameAnzeigen(true);
		}
	}

	@Override
	public SchweizerRankingModus getRankingModus() {
		return readEnumProperty(KONFIG_PROP_RANKING_MODUS, SchweizerRankingModus.class,
				SchweizerRankingModus.MIT_BUCHHOLZ);
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

	@Override
	public boolean isZeitplanAktiv() {
		return Boolean.TRUE.equals(readBooleanProperty(KONFIG_PROP_ZEITPLAN_AKTIV));
	}

	@Override
	public void setZeitplanAktiv(boolean aktiv) {
		setStringProperty(KONFIG_PROP_ZEITPLAN_AKTIV, StringTools.booleanToString(aktiv));
	}

	@Override
	public int getZeitplanAnzahlBahnen() {
		return readIntProperty(KONFIG_PROP_ZEITPLAN_ANZAHL_BAHNEN);
	}

	@Override
	public void setZeitplanAnzahlBahnen(int bahnen) {
		writeIntProperty(KONFIG_PROP_ZEITPLAN_ANZAHL_BAHNEN, bahnen);
	}

	@Override
	public boolean isDurchgangAufteilungWirksam() {
		return isZeitplanAktiv() && getZeitplanAnzahlBahnen() > 0;
	}

	@Override
	public int getZeitplanZeitlimitMinuten() {
		return readIntProperty(KONFIG_PROP_ZEITPLAN_ZEITLIMIT_MINUTEN);
	}

	@Override
	public void setZeitplanZeitlimitMinuten(int minuten) {
		writeIntProperty(KONFIG_PROP_ZEITPLAN_ZEITLIMIT_MINUTEN, minuten);
	}

	@Override
	public int getZeitplanDurchgangPauseMinuten() {
		return readIntProperty(KONFIG_PROP_ZEITPLAN_DURCHGANG_PAUSE_MINUTEN);
	}

	@Override
	public void setZeitplanDurchgangPauseMinuten(int minuten) {
		writeIntProperty(KONFIG_PROP_ZEITPLAN_DURCHGANG_PAUSE_MINUTEN, minuten);
	}

	@Override
	public int getZeitplanRundenPauseMinuten() {
		return readIntProperty(KONFIG_PROP_ZEITPLAN_RUNDEN_PAUSE_MINUTEN);
	}

	@Override
	public void setZeitplanRundenPauseMinuten(int minuten) {
		writeIntProperty(KONFIG_PROP_ZEITPLAN_RUNDEN_PAUSE_MINUTEN, minuten);
	}

	@Override
	public String getZeitplanTurnierStartzeit() {
		return readStringProperty(KONFIG_PROP_ZEITPLAN_TURNIER_STARTZEIT);
	}

	@Override
	public void setZeitplanTurnierStartzeit(String hhMm) {
		setStringProperty(KONFIG_PROP_ZEITPLAN_TURNIER_STARTZEIT, hhMm);
	}

}
