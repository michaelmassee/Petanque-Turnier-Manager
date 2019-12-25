/**
* Erstellung : 05.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.konfiguration;

import java.util.ArrayList;
import java.util.List;

import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.konfigdialog.ConfigPropertyType;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;

public class SuperMeleePropertiesSpalte extends BasePropertiesSpalte implements ISuperMeleePropertiesSpalte {

	private static final List<ConfigProperty<?>> KONFIG_PROPERTIES = new ArrayList<>();

	static {
		// ADDSpielsystemProp(TurnierSystem.SUPERMELEE, KONFIG_PROPERTIES);
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

	private static final String KONFIG_PROP_RANGLISTE_NICHT_GESPIELTE_RND_PLUS = "Nicht gespielte Runde Punkte +"; // 0
	private static final String KONFIG_PROP_RANGLISTE_NICHT_GESPIELTE_RND_MINUS = "Nicht gespielte Runde Punkte -"; // 13

	private static final String KONFIG_PROP_SPIELRUNDE_SPIELBAHN = "Spielrunde Spielbahn";
	private static final String KONFIG_PROP_ANZ_GESPIELTE_SPIELTAGE = "Anzahl gespielte Spieltage"; // anzahl spieltage die bei der neu auslosung eingelesen wird (hat zusammen

	private static final String KONFIG_PROP_SPIELRUNDE_1_HEADER = "Spielrunde, Spieltag in 1e Kopfzeile"; // spieltag in header ?

	public static final String KONFIG_PROP_SUPERMELEE_MODE = "Supermêlée Modus"; // Default Triplette / optional Doublette
	private static final String KONFIG_PROP_SPIELRUNDE_PLAN = "Spielrunde Plan"; // Default false

	static {
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_NAME_SPIELTAG).setDefaultVal(1).setDescription("Aktuelle Spieltag"));
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_NAME_SPIELRUNDE).setDefaultVal(1).setDescription("Aktuelle Spielrunde"));

		KONFIG_PROPERTIES
				.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_NAME_SPIELRUNDE_NEU_AUSLOSEN).setDefaultVal(0).setDescription("Neu auslosen ab Spielrunde"));

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_SPIELRUNDE_COLOR_BACK_GERADE).setDefaultVal(Integer.valueOf("e1e9f7", 16))
				.setDescription("Spielrunde Hintergrundfarbe für gerade Zeilen"));
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_SPIELRUNDE_COLOR_BACK_UNGERADE).setDefaultVal(Integer.valueOf("c0d6f7", 16))
				.setDescription("Spielrunde Hintergrundfarbe für ungerade Zeilen"));
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_SPIELRUNDE_COLOR_BACK_HEADER).setDefaultVal(Integer.valueOf("e6ebf4", 16))
				.setDescription("Spielrunde Header-Hintergrundfarbe"));

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_STREICH_SPIELTAG_COLOR_BACK_GERADE).setDefaultVal(14540253)
				.setDescription("Rangliste Hintergrundfarbe für Streich-Spieltag gerade Zeilen"));
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_STREICH_SPIELTAG_COLOR_BACK_UNGERADE).setDefaultVal(Integer.valueOf("ccc8c1", 16))
				.setDescription("Rangliste Hintergrundfarbe für Streich-Spieltag ungerade Zeilen"));

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_RANGLISTE_NICHT_GESPIELTE_RND_PLUS).setDefaultVal(0)
				.setDescription("Pluspunkte nicht gespielte Runde"));
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_RANGLISTE_NICHT_GESPIELTE_RND_MINUS).setDefaultVal(13)
				.setDescription("Minuspunkte nicht gespielte Runde"));

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.STRING, KONFIG_PROP_SPIELRUNDE_SPIELBAHN).setDefaultVal("X").setDescription(
				"Spalte-Spielbahn in Spielrunde.\r\nX=Keine Spalte\r\nL=Leere Spalte (händisch ausfüllen)\r\nN=1-n durchnummerieren\r\nR(x)=Random (optional x)=letzte spielbahn"));

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_ANZ_GESPIELTE_SPIELTAGE).setDefaultVal(99)
				.setDescription("Die Anzahl vergangene Spieltage die bei der Auslosung von neuen Spielrunden eingelesen werden. (Hat zusammen gespielt mit)"));

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.BOOLEAN, KONFIG_PROP_SPIELRUNDE_1_HEADER).setDefaultVal(false)
				.setDescription("Spielrunde, 1. Headerzeile mit Spieltag Info\r\nN/J"));
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.STRING, KONFIG_PROP_SUPERMELEE_MODE).setDefaultVal("T").setDescription("Modus\r\nT=Triplette\r\nD=Doublette"));

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.BOOLEAN, KONFIG_PROP_SPIELRUNDE_PLAN).setDefaultVal(false)
				.setDescription("Erstelle ein Spielrunde Plan zur jeder Spielrunde\r\nN/J"));
	}

	/**
	 * @param propertiesSpalte
	 * @param erstePropertiesZeile
	 * @param sheet
	 */
	SuperMeleePropertiesSpalte(int propertiesSpalte, int erstePropertiesZeile, ISheet sheet) {
		super(propertiesSpalte, erstePropertiesZeile, sheet);
	}

	@Override
	public SpielTagNr getAktiveSpieltag() throws GenerateException {
		SpielTagNr spieltag = SpielTagNr.from(readIntProperty(KONFIG_PROP_NAME_SPIELTAG));
		ProcessBox.from().spielTag(spieltag);
		return spieltag;
	}

	@Override
	public void setAktiveSpieltag(SpielTagNr spieltag) throws GenerateException {
		ProcessBox.from().spielTag(spieltag);
		writeIntProperty(KONFIG_PROP_NAME_SPIELTAG, spieltag.getNr());
	}

	@Override
	public SpielRundeNr getAktiveSpielRunde() throws GenerateException {
		SpielRundeNr spielrunde = SpielRundeNr.from(readIntProperty(KONFIG_PROP_NAME_SPIELRUNDE));
		ProcessBox.from().spielRunde(spielrunde);
		return spielrunde;
	}

	@Override
	public void setAktiveSpielRunde(SpielRundeNr spielrunde) throws GenerateException {
		ProcessBox.from().spielRunde(spielrunde);
		writeIntProperty(KONFIG_PROP_NAME_SPIELRUNDE, spielrunde.getNr());
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
	public Integer getAnzGespielteSpieltage() throws GenerateException {
		return readIntProperty(KONFIG_PROP_ANZ_GESPIELTE_SPIELTAGE);
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

}