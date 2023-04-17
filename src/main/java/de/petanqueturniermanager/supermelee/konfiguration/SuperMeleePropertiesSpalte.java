/**
 * Erstellung : 05.04.2018 / Michael Massee
 **/

package de.petanqueturniermanager.supermelee.konfiguration;

import java.util.ArrayList;
import java.util.List;

import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
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
				.setDefaultVal(1).setDescription("Aktuelle Spieltag").inSideBarInfoPanel());
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_NAME_SPIELRUNDE)
				.setDefaultVal(1).setDescription("Aktuelle Spielrunde").inSideBarInfoPanel());

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_NAME_SPIELRUNDE_NEU_AUSLOSEN)
				.setDefaultVal(0).setDescription("Neu auslosen ab Spielrunde").inSideBar());

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_SPIELRUNDE_COLOR_BACK_GERADE)
				.setDefaultVal(DEFAULT_GERADE_BACK_COLOR)
				.setDescription("Spielrunde Hintergrundfarbe für gerade Zeilen").inSideBar());
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_SPIELRUNDE_COLOR_BACK_UNGERADE)
				.setDefaultVal(DEFAULT_UNGERADE_BACK_COLOR)
				.setDescription("Spielrunde Hintergrundfarbe für ungerade Zeilen").inSideBar());
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_SPIELRUNDE_COLOR_BACK_HEADER)
				.setDefaultVal(DEFAULT_HEADER_BACK_COLOR).setDescription("Spielrunde Header-Hintergrundfarbe")
				.inSideBar());

		KONFIG_PROPERTIES.add(ConfigProperty
				.from(ConfigPropertyType.COLOR, KONFIG_PROP_STREICH_SPIELTAG_COLOR_BACK_GERADE).setDefaultVal(14540253)
				.setDescription("Rangliste Hintergrundfarbe für Streich-Spieltag gerade Zeilen").inSideBar());
		KONFIG_PROPERTIES
				.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_STREICH_SPIELTAG_COLOR_BACK_UNGERADE)
						.setDefaultVal(Integer.valueOf("ccc8c1", 16))
						.setDescription("Rangliste Hintergrundfarbe für Streich-Spieltag ungerade Zeilen").inSideBar());

		KONFIG_PROPERTIES
				.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_RANGLISTE_NICHT_GESPIELTE_RND_PLUS)
						.setDefaultVal(0).setDescription("Pluspunkte nicht gespielte Runde").inSideBar());
		KONFIG_PROPERTIES
				.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_RANGLISTE_NICHT_GESPIELTE_RND_MINUS)
						.setDefaultVal(13).setDescription("Minuspunkte nicht gespielte Runde").inSideBar());

		KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_SPIELRUNDE_SPIELBAHN)
				.setDefaultVal("X").setDescription(
						"Spalte Spielbahn in Spielrunde.\r\nX=Keine Spalte\r\nL=Leere Spalte (händisch ausfüllen)\r\nN=Durchnummerieren\r\nR=Random"))
				.addAuswahl("X", "Keine Spalte").addAuswahl("L", "Leere Spalte")
				.addAuswahl("N", "Durchnummerieren (1-n)").addAuswahl("R", "Zufällig vergeben").inSideBar());

		KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_SUPERMELEE_MODE)
				.setDefaultVal(SuperMeleeMode.Triplette.getKey())
				.setDescription("Spielrunden Modus\r\nT=Triplette\r\nD=Doublette"))
				.addAuswahl(SuperMeleeMode.Triplette.getKey(), "Triplette")
				.addAuswahl(SuperMeleeMode.Doublette.getKey(), "Doublette").inSideBar());

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_ANZ_GESPIELTE_SPIELTAGE)
				.setDefaultVal(99)
				.setDescription(
						"Die Anzahl vergangene Spieltage die bei der Auslosung von neuen Spielrunden eingelesen werden. (Hat zusammen gespielt mit)")
				.inSideBar());

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_ANZ_SPIELER_IN_SPALTE)
				.setDefaultVal(MAX_ANZSPIELER_IN_SPALTE)
				.setDescription("Ab wann (Anzahl Spieler) neue Spalte in Spielplan.").inSideBar());

		KONFIG_PROPERTIES.add(
				ConfigProperty.from(ConfigPropertyType.BOOLEAN, KONFIG_PROP_SPIELRUNDE_1_HEADER).setDefaultVal(false)
						.inSideBar().setDescription("Spielrunde, 1. Headerzeile mit Spieltag Info\r\nN/J (default=N)"));

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.BOOLEAN, KONFIG_PROP_SPIELRUNDE_PLAN)
				.setDefaultVal(false).inSideBar()
				.setDescription("Erstelle ein Spielrunde Plan zu jeder Spielrunde\r\nN/J (default=N)"));

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.BOOLEAN, KONFIG_PROP_SETZ_POS).setDefaultVal(true)
				.inSideBar().setDescription(
						"Bei der Generierung von Spielrunden, Setzpositionen aus der Meldeliste beachten. Default=J"));

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.BOOLEAN, KONFIG_PROP_FRAGE_GLEICHE_PAARUNGEN)
				.setDefaultVal(false).inSideBar().setDescription(
						"Wenn true, und wenn die Anzahl der Meldungen, nur eine Doublettes oder Triplettes Runde ermöglichen, dann bei der Generierung von Spielrunden Fragen."));

		KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_ENDRANGLISTE_SORT_MODE)
				.setDefaultVal(SuprMleEndranglisteSortMode.DEFAULT.getKey()).setDescription(
						"Endrangliste Sortiermodus\r\nD=Spiele +, Spiele Div, ...\r\nT=Spiele +, Anzahl gespielte Spieltage, ..."))
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
	public Integer getSpielRundeHintergrundFarbeGerade() throws GenerateException {
		return readCellBackColorProperty(KONFIG_PROP_SPIELRUNDE_COLOR_BACK_GERADE);
	}

	@Override
	public Integer getSpielRundeHintergrundFarbeUnGerade() throws GenerateException {
		return readCellBackColorProperty(KONFIG_PROP_SPIELRUNDE_COLOR_BACK_UNGERADE);
	}

	@Override
	public Integer getSpielRundeHeaderFarbe() throws GenerateException {
		return readCellBackColorProperty(KONFIG_PROP_SPIELRUNDE_COLOR_BACK_HEADER);
	}

	@Override
	public Integer getRanglisteHintergrundFarbe_StreichSpieltag_Gerade() throws GenerateException {
		return readCellBackColorProperty(KONFIG_PROP_STREICH_SPIELTAG_COLOR_BACK_GERADE);
	}

	@Override
	public Integer getRanglisteHintergrundFarbe_StreichSpieltag_UnGerade() throws GenerateException {
		return readCellBackColorProperty(KONFIG_PROP_STREICH_SPIELTAG_COLOR_BACK_UNGERADE);
	}

	@Override
	public Integer getSpielRundeNeuAuslosenAb() throws GenerateException {
		return readIntProperty(KONFIG_PROP_NAME_SPIELRUNDE_NEU_AUSLOSEN);
	}

	@Override
	public Integer getNichtGespielteRundePlus() throws GenerateException {
		return readIntProperty(KONFIG_PROP_RANGLISTE_NICHT_GESPIELTE_RND_PLUS);
	}

	@Override
	public Integer getNichtGespielteRundeMinus() throws GenerateException {
		return readIntProperty(KONFIG_PROP_RANGLISTE_NICHT_GESPIELTE_RND_MINUS);
	}

	@Override
	public String getSpielrundeSpielbahn() throws GenerateException {
		return readStringProperty(KONFIG_PROP_SPIELRUNDE_SPIELBAHN);
	}

	@Override
	public SuperMeleeMode getSuperMeleeMode() throws GenerateException {
		String prop = readStringProperty(KONFIG_PROP_SUPERMELEE_MODE);
		if (null != prop && prop.trim().equalsIgnoreCase("d")) {
			return SuperMeleeMode.Doublette;
		}
		return SuperMeleeMode.Triplette;
	}

	/**
	 * die anzahl der Spieltage die bei der neu auslosung eingelesen werden
	 *
	 * @return
	 * @throws GenerateException
	 */
	@Override
	public Integer getMaxAnzGespielteSpieltage() throws GenerateException {
		return readIntProperty(KONFIG_PROP_ANZ_GESPIELTE_SPIELTAGE);
	}

	@Override
	public Integer getMaxAnzSpielerInSpalte() throws GenerateException {
		return readIntProperty(KONFIG_PROP_ANZ_SPIELER_IN_SPALTE);
	}

	/**
	 * spieltag in header ?
	 */
	@Override
	public boolean getSpielrunde1Header() throws GenerateException {
		return readBooleanProperty(KONFIG_PROP_SPIELRUNDE_1_HEADER);
	}

	/**
	 * @return
	 * @throws GenerateException
	 */
	@Override
	public boolean getSpielrundePlan() throws GenerateException {
		return readBooleanProperty(KONFIG_PROP_SPIELRUNDE_PLAN);
	}

	@Override
	protected List<ConfigProperty<?>> getKonfigProperties() {
		return KONFIG_PROPERTIES;
	}

	@Override
	public boolean getSetzPositionenAktiv() throws GenerateException {
		return readBooleanProperty(KONFIG_PROP_SETZ_POS);
	}

	@Override
	public boolean getGleichePaarungenAktiv() throws GenerateException {
		return readBooleanProperty(KONFIG_PROP_FRAGE_GLEICHE_PAARUNGEN);
	}

	@Override
	public final void setAktiveSpielRunde(SpielRundeNr spielrunde) throws GenerateException {
		writeIntProperty(KONFIG_PROP_NAME_SPIELRUNDE, spielrunde.getNr());
	}

	@Override
	public final void setAktiveSpieltag(SpielTagNr spieltag) throws GenerateException {
		writeIntProperty(KONFIG_PROP_NAME_SPIELTAG, spieltag.getNr());
	}

	@Override
	public SpielTagNr getAktiveSpieltag() throws GenerateException {
		return SpielTagNr.from(readIntProperty(KONFIG_PROP_NAME_SPIELTAG));
	}

	@Override
	public SpielRundeNr getAktiveSpielRunde() throws GenerateException {
		return SpielRundeNr.from(readIntProperty(KONFIG_PROP_NAME_SPIELRUNDE));
	}

	@Override
	public SuprMleEndranglisteSortMode getSuprMleEndranglisteSortMode() throws GenerateException {

		String prop = readStringProperty(KONFIG_PROP_ENDRANGLISTE_SORT_MODE);
		if (null != prop && prop.trim().equalsIgnoreCase(SuprMleEndranglisteSortMode.ANZTAGE.getKey())) {
			return SuprMleEndranglisteSortMode.ANZTAGE;
		}
		return SuprMleEndranglisteSortMode.DEFAULT;

	}

}
