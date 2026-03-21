/**
 * Erstellung : 05.04.2018 / Michael Massee
 **/

package de.petanqueturniermanager.supermelee.konfiguration;

import java.util.ArrayList;
import java.util.List;

import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.cellstyle.SpielrundeHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.SpielrundeHintergrundFarbeUnGeradeStyle;
import de.petanqueturniermanager.konfigdialog.AuswahlConfigProperty;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.konfigdialog.ConfigPropertyType;
import de.petanqueturniermanager.konfigdialog.HeaderFooterConfigProperty;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;

public class SuperMeleePropertiesSpalte extends BasePropertiesSpalte implements ISuperMeleePropertiesSpalte {

	public static final List<ConfigProperty<?>> KONFIG_PROPERTIES = new ArrayList<>();

	static {
		ADDBaseProp(KONFIG_PROPERTIES);
	}

	public static final String KONFIG_PROP_NAME_SPIELTAG = "Spieltag";
	public static final String KONFIG_PROP_NAME_SPIELRUNDE = "Spielrunde";

	private static final String KONFIG_PROP_NAME_SPIELRUNDE_NEU_AUSLOSEN = "Neu Auslosen ab Runde";
	private static final String KONFIG_PROP_SPIELRUNDE_COLOR_BACK_GERADE = "Spielrunde Hintergrund Gerade";
	private static final String KONFIG_PROP_SPIELRUNDE_COLOR_BACK_UNGERADE = "Spielrunde Hintergrund Ungerade";
	private static final String KONFIG_PROP_SPIELRUNDE_COLOR_BACK_HEADER = "Spielrunde Header";

	// Endrangliste
	private static final String KONFIG_PROP_STREICH_SPIELTAG_COLOR_BACK_GERADE = "Streich-Spieltag Hintergrund Gerade";
	private static final String KONFIG_PROP_STREICH_SPIELTAG_COLOR_BACK_UNGERADE = "Streich-Spieltag Hintergrund Ungerade";

	private static final String KONFIG_PROP_RANGLISTE_NICHT_GESPIELTE_RND_PLUS = "Nicht gespielte Runde, + Punkte"; // 0
	private static final String KONFIG_PROP_RANGLISTE_NICHT_GESPIELTE_RND_MINUS = "Nicht gespielte Runde, - Punkte"; // 13

	private static final String KONFIG_PROP_SPIELRUNDE_SPIELBAHN = "Spielrunde Spielbahn";
	// anzahl spieltage die bei der neu auslosung eingelesen wird (hat zusammen
	private static final String KONFIG_PROP_ANZ_GESPIELTE_SPIELTAGE = "Anzahl gespielte Spieltage";
	public static final String KONFIG_PROP_ANZ_SPIELER_IN_SPALTE = "Anz. Spieler in Spalte"; // ab wann neue Spalte Speilrundeplan
	private static final int MAX_ANZSPIELER_IN_SPALTE = 30;

	private static final String KONFIG_PROP_SPIELRUNDE_1_HEADER = "Spielrunde, Spieltag in 1. Kopfzeile"; // spieltag in header ?
	public static final String KONFIG_PROP_SUPERMELEE_MODE = "Supermêlée Modus"; // Default Triplette / optional Doublette
	private static final String KONFIG_PROP_SPIELRUNDE_PLAN = "Spielrunde Plan"; // Default false
	private static final String KONFIG_PROP_SETZ_POS = "Setzpositionen beachten"; // Default true
	// wenn nur Doublettes oder Triplettes mögliche dann fragen bei derRunde Generierung. Default false
	public static final String KONFIG_PROP_FRAGE_GLEICHE_PAARUNGEN = "Gleiche Paarungen";
	private static final String KONFIG_PROP_SPIELTAG_KOPFZEILE = "Kopfzeile Spieltag"; // plus spieltagNr
	public static final String KONFIG_PROP_ENDRANGLISTE_SORT_MODE = "Endrangliste Sortiermodus"; // 

	static {

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_NAME_SPIELTAG)
				.setDefaultVal(1).setDescription("config.desc.supermelee.spieltag").inSideBarInfoPanel());
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_NAME_SPIELRUNDE)
				.setDefaultVal(1).setDescription("config.desc.supermelee.spielrunde").inSideBarInfoPanel());

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_NAME_SPIELRUNDE_NEU_AUSLOSEN)
				.setDefaultVal(0).setDescription("config.desc.supermelee.spielrunde.neu.auslosen").inSideBar());

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_SPIELRUNDE_COLOR_BACK_GERADE)
				.setDefaultVal(DEFAULT_GERADE_BACK_COLOR)
				.setDescription("config.desc.spielrunde.gerade").inSideBar());
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_SPIELRUNDE_COLOR_BACK_UNGERADE)
				.setDefaultVal(DEFAULT_UNGERADE_BACK_COLOR)
				.setDescription("config.desc.spielrunde.ungerade").inSideBar());
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_SPIELRUNDE_COLOR_BACK_HEADER)
				.setDefaultVal(DEFAULT_HEADER_BACK_COLOR).setDescription("config.desc.spielrunde.header")
				.inSideBar());

		KONFIG_PROPERTIES.add(ConfigProperty
				.from(ConfigPropertyType.COLOR, KONFIG_PROP_STREICH_SPIELTAG_COLOR_BACK_GERADE).setDefaultVal(14540253)
				.setDescription("config.desc.supermelee.streich.gerade").inSideBar());
		KONFIG_PROPERTIES
				.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_STREICH_SPIELTAG_COLOR_BACK_UNGERADE)
						.setDefaultVal(Integer.valueOf("ccc8c1", 16))
						.setDescription("config.desc.supermelee.streich.ungerade").inSideBar());

		KONFIG_PROPERTIES
				.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_RANGLISTE_NICHT_GESPIELTE_RND_PLUS)
						.setDefaultVal(0).setDescription("config.desc.supermelee.nicht.gespielt.plus").inSideBar());
		KONFIG_PROPERTIES
				.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_RANGLISTE_NICHT_GESPIELTE_RND_MINUS)
						.setDefaultVal(13).setDescription("config.desc.supermelee.nicht.gespielt.minus").inSideBar());

		KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_SPIELRUNDE_SPIELBAHN)
				.setDefaultVal(SpielrundeSpielbahn.X.name()).setDescription("config.desc.spielbahn"))
				.addAuswahl(SpielrundeSpielbahn.X.name(), "Keine Spalte")
				.addAuswahl(SpielrundeSpielbahn.L.name(), "Leere Spalte")
				.addAuswahl(SpielrundeSpielbahn.N.name(), "Durchnummerieren (1-n)")
				.addAuswahl(SpielrundeSpielbahn.R.name(), "Zufällig vergeben").inSideBar());

		KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_SUPERMELEE_MODE)
				.setDefaultVal(SuperMeleeMode.Triplette.getKey())
				.setDescription("config.desc.supermelee.modus"))
				.addAuswahl(SuperMeleeMode.Triplette.getKey(), "Triplette")
				.addAuswahl(SuperMeleeMode.Doublette.getKey(), "Doublette").inSideBar());

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_ANZ_GESPIELTE_SPIELTAGE)
				.setDefaultVal(99)
				.setDescription("config.desc.supermelee.anz.gespielte.spieltage")
				.inSideBar());

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_ANZ_SPIELER_IN_SPALTE)
				.setDefaultVal(MAX_ANZSPIELER_IN_SPALTE)
				.setDescription("config.desc.supermelee.anz.spieler.spalte").inSideBar());

		KONFIG_PROPERTIES.add(
				ConfigProperty.from(ConfigPropertyType.BOOLEAN, KONFIG_PROP_SPIELRUNDE_1_HEADER).setDefaultVal(false)
						.inSideBar().setDescription("config.desc.supermelee.spielrunde.1.header"));

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.BOOLEAN, KONFIG_PROP_SPIELRUNDE_PLAN)
				.setDefaultVal(false).inSideBar()
				.setDescription("config.desc.supermelee.spielrunde.plan"));

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.BOOLEAN, KONFIG_PROP_SETZ_POS).setDefaultVal(true)
				.inSideBar().setDescription("config.desc.supermelee.setz.pos"));

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.BOOLEAN, KONFIG_PROP_FRAGE_GLEICHE_PAARUNGEN)
				.setDefaultVal(false).inSideBar().setDescription("config.desc.supermelee.gleiche.paarungen"));

		KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_ENDRANGLISTE_SORT_MODE)
				.setDefaultVal(SuprMleEndranglisteSortMode.DEFAULT.getKey()).setDescription("config.desc.supermelee.endrangliste.sort.modus"))
				.addAuswahl(SuprMleEndranglisteSortMode.DEFAULT.getKey(), "Default,Sp+,SpΔ,PktΔ,Pkt+")
				.addAuswahl(SuprMleEndranglisteSortMode.ANZTAGE.getKey(), "Sp+,AnzTage,SpΔ,PktΔ,Pkt+"));

		// Spieltag Header
		for (int spieltagcntr = 1; spieltagcntr <= SuperMeleeKonfigurationSheet.MAX_SPIELTAG; spieltagcntr++) {
			KONFIG_PROPERTIES.add(HeaderFooterConfigProperty.from(PROP_SPIELTAG_KOPFZEILE(spieltagcntr))
					.setDefaultVal(spieltagcntr + ". Spieltag").setDescription("Kopfzeile für Spieltag " + spieltagcntr)
					.inSideBar());
		}
	}

	public static final String PROP_SPIELTAG_KOPFZEILE(int spielTagNr) {
		return KONFIG_PROP_SPIELTAG_KOPFZEILE + " " + spielTagNr;
	}

	/**
	 * @param propertiesSpalte
	 * @param erstePropertiesZeile
	 * @param sheet
	 */
	SuperMeleePropertiesSpalte(ISheet sheet) {
		super(sheet);
	}

	@Override
	public Integer getSpielRundeHintergrundFarbeGerade() {
		return readCellBackColorProperty(KONFIG_PROP_SPIELRUNDE_COLOR_BACK_GERADE);
	}

	@Override
	public Integer getSpielRundeHintergrundFarbeUnGerade() {
		return readCellBackColorProperty(KONFIG_PROP_SPIELRUNDE_COLOR_BACK_UNGERADE);
	}

	@Override
	public Integer getSpielRundeHeaderFarbe() {
		return readCellBackColorProperty(KONFIG_PROP_SPIELRUNDE_COLOR_BACK_HEADER);
	}

	@Override
	public Integer getRanglisteHintergrundFarbeStreichSpieltagGerade() {
		return readCellBackColorProperty(KONFIG_PROP_STREICH_SPIELTAG_COLOR_BACK_GERADE);
	}

	@Override
	public Integer getRanglisteHintergrundFarbeStreichSpieltagUnGerade() {
		return readCellBackColorProperty(KONFIG_PROP_STREICH_SPIELTAG_COLOR_BACK_UNGERADE);
	}

	@Override
	public Integer getSpielRundeNeuAuslosenAb() {
		return readIntProperty(KONFIG_PROP_NAME_SPIELRUNDE_NEU_AUSLOSEN);
	}

	@Override
	public Integer getNichtGespielteRundePlus() {
		return readIntProperty(KONFIG_PROP_RANGLISTE_NICHT_GESPIELTE_RND_PLUS);
	}

	@Override
	public Integer getNichtGespielteRundeMinus() {
		return readIntProperty(KONFIG_PROP_RANGLISTE_NICHT_GESPIELTE_RND_MINUS);
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
	public SuperMeleeMode getSuperMeleeMode() {
		String prop = readStringProperty(KONFIG_PROP_SUPERMELEE_MODE);
		if (null != prop && prop.trim().equalsIgnoreCase("d")) {
			return SuperMeleeMode.Doublette;
		}
		return SuperMeleeMode.Triplette;
	}

	@Override
	public void setSuperMeleeMode(SuperMeleeMode mode) {
		setStringProperty(KONFIG_PROP_SUPERMELEE_MODE, mode.getKey());
	}

	/**
	 * die anzahl der Spieltage die bei der neu auslosung eingelesen werden
	 *
	 * @return
	 * @throws GenerateException
	 */
	@Override
	public Integer getMaxAnzGespielteSpieltage() {
		return readIntProperty(KONFIG_PROP_ANZ_GESPIELTE_SPIELTAGE);
	}

	@Override
	public Integer getMaxAnzSpielerInSpalte() {
		return readIntProperty(KONFIG_PROP_ANZ_SPIELER_IN_SPALTE);
	}

	/**
	 * spieltag in header ?
	 */
	@Override
	public boolean getSpielrunde1Header() {
		return readBooleanProperty(KONFIG_PROP_SPIELRUNDE_1_HEADER);
	}

	/**
	 * @return
	 * @throws GenerateException
	 */
	@Override
	public boolean getSpielrundePlan() {
		return readBooleanProperty(KONFIG_PROP_SPIELRUNDE_PLAN);
	}

	@Override
	protected List<ConfigProperty<?>> getKonfigProperties() {
		return KONFIG_PROPERTIES;
	}

	@Override
	public boolean getSetzPositionenAktiv() {
		return readBooleanProperty(KONFIG_PROP_SETZ_POS);
	}

	@Override
	public boolean getGleichePaarungenAktiv() {
		return readBooleanProperty(KONFIG_PROP_FRAGE_GLEICHE_PAARUNGEN);
	}

	@Override
	public final void setAktiveSpielRunde(SpielRundeNr spielrunde) {
		writeIntProperty(KONFIG_PROP_NAME_SPIELRUNDE, spielrunde.getNr());
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
	public SpielRundeNr getAktiveSpielRunde() {
		return SpielRundeNr.from(readIntProperty(KONFIG_PROP_NAME_SPIELRUNDE));
	}

	@Override
	public SuprMleEndranglisteSortMode getSuprMleEndranglisteSortMode() {

		String prop = readStringProperty(KONFIG_PROP_ENDRANGLISTE_SORT_MODE);
		if (null != prop && prop.trim().equalsIgnoreCase(SuprMleEndranglisteSortMode.ANZTAGE.getKey())) {
			return SuprMleEndranglisteSortMode.ANZTAGE;
		}
		return SuprMleEndranglisteSortMode.DEFAULT;

	}

	@Override
	public SpielrundeHintergrundFarbeGeradeStyle getSpielRundeHintergrundFarbeGeradeStyle() {
		return new SpielrundeHintergrundFarbeGeradeStyle(getSpielRundeHintergrundFarbeGerade());
	}

	@Override
	public SpielrundeHintergrundFarbeUnGeradeStyle getSpielRundeHintergrundFarbeUnGeradeStyle() {
		return new SpielrundeHintergrundFarbeUnGeradeStyle(getSpielRundeHintergrundFarbeUnGerade());
	}

}
