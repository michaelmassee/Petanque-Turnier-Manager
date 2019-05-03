/**
* Erstellung : 05.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.konfiguration;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.cellvalue.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.IntegerCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.WeakRefHelper;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.meldeliste.Formation;
import de.petanqueturniermanager.supermelee.meldeliste.SpielSystem;

public class PropertiesSpalte {
	// private static final Logger logger = LogManager.getLogger(PropertiesSpalte.class);

	private static final String KONFIG_PROP_NAME_FORMATION = "Formation";
	private static final String KONFIG_PROP_NAME_SPIELSYSTEM = "Spielsystem";
	public static final String KONFIG_PROP_NAME_SPIELTAG = "Spieltag";
	public static final String KONFIG_PROP_NAME_SPIELRUNDE = "Spielrunde";

	private static final String KONFIG_PROP_NAME_SPIELRUNDE_NEU_AUSLOSEN = "Neu Auslosen ab Runde";
	private static final String KONFIG_PROP_SPIELRUNDE_COLOR_BACK_GERADE = "Spielrunde Hintergr. Gerade";
	private static final String KONFIG_PROP_SPIELRUNDE_COLOR_BACK_UNGERADE = "Spielrunde Hintergr. Ungerade";
	private static final String KONFIG_PROP_SPIELRUNDE_COLOR_BACK_HEADER = "Spielrunde Header";

	private static final String KONFIG_PROP_RANGLISTE_COLOR_BACK_GERADE = "Rangliste Hintergr. Gerade";
	private static final String KONFIG_PROP_RANGLISTE_COLOR_BACK_UNGERADE = "Rangliste Hintergr. Ungerade";
	private static final String KONFIG_PROP_RANGLISTE_COLOR_BACK_HEADER = "Rangliste Header";
	// Endrangliste
	private static final String KONFIG_PROP_STREICH_SPIELTAG_COLOR_BACK_GERADE = "Streich-Spieltag Hintergr. Gerade";
	private static final String KONFIG_PROP_STREICH_SPIELTAG_COLOR_BACK_UNGERADE = "Streich-Spieltag  Hintergr. Ungerade";

	private static final String KONFIG_PROP_RANGLISTE_NICHT_GESPIELTE_RND_PLUS = "Nicht gespielte Runde Punkte +"; // 0
	private static final String KONFIG_PROP_RANGLISTE_NICHT_GESPIELTE_RND_MINUS = "Nicht gespielte Runde Punkte -"; // 13

	private static final String KONFIG_PROP_SPIELRUNDE_SPIELBAHN = "Spielrunde Spielbahn";
	private static final String KONFIG_PROP_ANZ_GESPIELTE_SPIELTAGE = "Anz gespielte Spieltage"; // anzahl spieltage die bei der neu auslosung eingelesen wird (hat zusammen
																									// gespielt)

	private static final String KONFIG_PROP_FUSSZEILE_LINKS = "Fußzeile links";
	private static final String KONFIG_PROP_FUSSZEILE_MITTE = "Fußzeile mitte";

	public static final List<ConfigProperty<?>> KONFIG_PROPERTIES = new ArrayList<>();

	static {
		KONFIG_PROPERTIES
				.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_NAME_FORMATION).setDefaultVal(4).setDescription("1=Tête,2=Doublette,3=Triplette,4=Mêlée"));
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_NAME_SPIELSYSTEM).setDefaultVal(1).setDescription("1=Supermêlée"));
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

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_RANGLISTE_COLOR_BACK_GERADE).setDefaultVal(Integer.valueOf("e1e9f7", 16))
				.setDescription("Rangliste Hintergrundfarbe für gerade Zeilen"));
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_RANGLISTE_COLOR_BACK_UNGERADE).setDefaultVal(Integer.valueOf("c0d6f7", 16))
				.setDescription("Rangliste Hintergrundfarbe für ungerade Zeilen"));
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_RANGLISTE_COLOR_BACK_HEADER).setDefaultVal(Integer.valueOf("e6ebf4", 16))
				.setDescription("Rangliste Header-Hintergrundfarbe"));

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

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.STRING, KONFIG_PROP_FUSSZEILE_LINKS).setDefaultVal("").setDescription("Fußzeile Links"));
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.STRING, KONFIG_PROP_FUSSZEILE_MITTE).setDefaultVal("").setDescription("Fußzeile Mitte"));
	}

	private final WeakRefHelper<ISheet> sheetWkRef;
	private int propertiesSpalte;
	private int erstePropertiesZeile;
	private int headerZeile;

	public PropertiesSpalte(int propertiesSpalte, int erstePropertiesZeile, ISheet sheet) {
		checkNotNull(sheet);
		checkArgument(propertiesSpalte > -1, "propertiesSpalte %s<0", propertiesSpalte);
		checkArgument(erstePropertiesZeile > 0, "erstePropertiesZeile %s<1", erstePropertiesZeile);

		this.propertiesSpalte = propertiesSpalte;
		this.erstePropertiesZeile = erstePropertiesZeile;
		headerZeile = erstePropertiesZeile - 1;
		sheetWkRef = new WeakRefHelper<>(sheet);
	}

	/**
	 * call getSheetHelper from ISheet<br>
	 * do not assign to Variable, while getter does SheetRunner.testDoCancelTask(); <br>
	 *
	 * @see SheetRunner#getSheetHelper()
	 *
	 * @return SheetHelper
	 * @throws GenerateException
	 */
	private SheetHelper getSheetHelper() throws GenerateException {
		return sheetWkRef.getObject().getSheetHelper();
	}

	public void doFormat() throws GenerateException {
		XSpreadsheet propSheet = getPropSheet();
		// header
		Position posHeader = Position.from(propertiesSpalte, headerZeile);
		CellProperties columnProperties = CellProperties.from().setWidth(6500);

		StringCellValue headerVal = StringCellValue.from(propSheet, posHeader).addColumnProperties(columnProperties).setValue("Name").setHoriJustify(CellHoriJustify.RIGHT);
		getSheetHelper().setTextInCell(headerVal);

		StringCellValue wertheaderVal = StringCellValue.from(propSheet, posHeader).addColumnProperties(columnProperties.setWidth(1500)).setValue("Wert")
				.setHoriJustify(CellHoriJustify.CENTER).spaltePlusEins();
		getSheetHelper().setTextInCell(wertheaderVal);
	}

	public void updateKonfigBlock() throws GenerateException {
		XSpreadsheet propSheet = getPropSheet();

		for (int idx = 0; idx < KONFIG_PROPERTIES.size(); idx++) {

			ConfigProperty<?> configProp = KONFIG_PROPERTIES.get(idx);

			Position pos = getPropKeyPos(configProp.getKey());
			if (pos == null) {
				// when not found insert new
				pos = Position.from(propertiesSpalte, erstePropertiesZeile + idx);
				StringCellValue celVal = StringCellValue.from(propSheet, pos, configProp.getKey()).setComment(null).setHoriJustify(CellHoriJustify.RIGHT);
				getSheetHelper().setTextInCell(celVal);

				celVal.spaltePlusEins().setComment(configProp.getDescription()).setHoriJustify(CellHoriJustify.CENTER);

				// default Val schreiben
				switch (configProp.getType()) {
				case STRING:
					celVal.setValue((String) configProp.getDefaultVal());
					getSheetHelper().setTextInCell(celVal);
					break;
				case INTEGER:
					IntegerCellValue numberCellValue = IntegerCellValue.from(celVal).setValue((Integer) configProp.getDefaultVal());
					getSheetHelper().setValInCell(numberCellValue);
					break;
				case COLOR:
					writeCellBackColorProperty(configProp.getKey(), (Integer) configProp.getDefaultVal(), configProp.getDescription());
					break;
				default:
				}
			}
		}
	}

	/**
	 *
	 * @param name
	 * @return defaultVal aus ConfigProperty, -1 wenn fehler
	 * @throws GenerateException
	 */
	public int readIntProperty(String key) throws GenerateException {
		XSpreadsheet sheet = getPropSheet();
		Position pos = getPropKeyPos(key);
		int val = -1;
		if (pos != null) {
			val = getSheetHelper().getIntFromCell(sheet, pos.spaltePlusEins());
		}

		if (val == -1) {
			Object defaultVal = getDefaultProp(key);
			if (defaultVal != null && defaultVal instanceof Integer) {
				val = (Integer) defaultVal;
			}
		}
		return val;
	}

	/**
	 * lese von der Zelle im Sheet zu diesen Property, das Property "CellBackColor"
	 *
	 * @param key = property
	 * @return Integer, -1 wenn keine Farbe, null when not found
	 * @throws GenerateException
	 */

	public Integer readCellBackColorProperty(String key) throws GenerateException {
		XSpreadsheet sheet = getPropSheet();
		Position pos = getPropKeyPos(key);
		Integer val = null;
		if (pos != null) {
			Object cellProperty = getSheetHelper().getCellProperty(sheet, pos.spaltePlusEins(), "CellBackColor");
			if (cellProperty != null && cellProperty instanceof Integer) {
				val = (Integer) cellProperty;
			}
		}

		if (val == null) {
			Object defaultVal = getDefaultProp(key);
			if (defaultVal != null && defaultVal instanceof Integer) {
				val = (Integer) defaultVal;
			}
		}
		return val;
	}

	public void writeCellBackColorProperty(String key, Integer val, String comment) throws GenerateException {
		XSpreadsheet sheet = getPropSheet();
		Position pos = getPropKeyPos(key);
		if (pos != null) {
			getSheetHelper().setPropertyInCell(sheet, pos.spaltePlusEins(), "CellBackColor", val);
			getSheetHelper().setTextInCell(StringCellValue.from(sheet, pos, ""));

			if (StringUtils.isNotEmpty(comment)) {
				getSheetHelper().setCommentInCell(sheet, pos, comment);
			}
		}
	}

	/**
	 * @param name
	 * @return defaultVal aus ConfigProperty, -1 wenn fehler
	 * @throws GenerateException
	 */
	public String readStringProperty(String key) throws GenerateException {
		XSpreadsheet sheet = getPropSheet();
		Position pos = getPropKeyPos(key);
		String val = null;
		if (pos != null) {
			val = getSheetHelper().getTextFromCell(sheet, pos.spaltePlusEins());
		}

		if (val == null) {
			Object defaultVal = getDefaultProp(key);
			if (defaultVal != null && defaultVal instanceof String) {
				val = (String) defaultVal;
			}
		}
		return val;
	}

	private Object getDefaultProp(String key) {
		for (ConfigProperty<?> konfigProp : KONFIG_PROPERTIES) {
			if (konfigProp.getKey().equals(key)) {
				return konfigProp.getDefaultVal();
			}
		}
		return null;
	}

	/**
	 *
	 * @param name
	 * @return defaultVal when not found
	 * @throws GenerateException
	 */
	public void writeIntProperty(String name, int newVal) throws GenerateException {
		Position pos = getPropKeyPos(name);
		if (pos != null) {
			getSheetHelper().setValInCell(getPropSheet(), pos.spaltePlusEins(), newVal);
		}
	}

	/**
	 *
	 * @param name
	 * @return null when not found
	 * @throws GenerateException
	 */
	public Position getPropKeyPos(String key) throws GenerateException {
		checkNotNull(key);

		XSpreadsheet sheet = getPropSheet();
		Position pos = Position.from(propertiesSpalte, erstePropertiesZeile);
		// TODO umstellen auf uno find
		for (int idx = 0; idx < KONFIG_PROPERTIES.size(); idx++) {
			String val = getSheetHelper().getTextFromCell(sheet, pos);
			if (StringUtils.isNotBlank(val) && val.trim().equalsIgnoreCase(key.trim())) {
				return pos;
			}
			pos.zeilePlusEins();
		}
		return null;
	}

	public SpielTagNr getAktiveSpieltag() throws GenerateException {
		SpielTagNr spieltag = SpielTagNr.from(readIntProperty(KONFIG_PROP_NAME_SPIELTAG));
		ProcessBox.from().spielTag(spieltag);
		return spieltag;
	}

	public void setAktiveSpieltag(SpielTagNr spieltag) throws GenerateException {
		ProcessBox.from().spielTag(spieltag);
		writeIntProperty(KONFIG_PROP_NAME_SPIELTAG, spieltag.getNr());
	}

	public SpielRundeNr getAktiveSpielRunde() throws GenerateException {
		SpielRundeNr spielrunde = SpielRundeNr.from(readIntProperty(KONFIG_PROP_NAME_SPIELRUNDE));
		ProcessBox.from().spielRunde(spielrunde);
		return spielrunde;
	}

	public void setAktiveSpielRunde(SpielRundeNr spielrunde) throws GenerateException {
		ProcessBox.from().spielRunde(spielrunde);
		writeIntProperty(KONFIG_PROP_NAME_SPIELRUNDE, spielrunde.getNr());
	}

	private final XSpreadsheet getPropSheet() throws GenerateException {
		return sheetWkRef.getObject().getSheet();
	}

	public Integer getSpielRundeHintergrundFarbeGerade() throws GenerateException {
		return readCellBackColorProperty(KONFIG_PROP_SPIELRUNDE_COLOR_BACK_GERADE);
	}

	public Integer getSpielRundeHintergrundFarbeUnGerade() throws GenerateException {
		return readCellBackColorProperty(KONFIG_PROP_SPIELRUNDE_COLOR_BACK_UNGERADE);
	}

	public Integer getSpielRundeHeaderFarbe() throws GenerateException {
		return readCellBackColorProperty(KONFIG_PROP_SPIELRUNDE_COLOR_BACK_HEADER);
	}

	public Integer getRanglisteHintergrundFarbeGerade() throws GenerateException {
		return readCellBackColorProperty(KONFIG_PROP_RANGLISTE_COLOR_BACK_GERADE);
	}

	public Integer getRanglisteHintergrundFarbeUnGerade() throws GenerateException {
		return readCellBackColorProperty(KONFIG_PROP_RANGLISTE_COLOR_BACK_UNGERADE);
	}

	public Integer getRanglisteHintergrundFarbe_StreichSpieltag_Gerade() throws GenerateException {
		return readCellBackColorProperty(KONFIG_PROP_STREICH_SPIELTAG_COLOR_BACK_GERADE);
	}

	public Integer getRanglisteHintergrundFarbe_StreichSpieltag_UnGerade() throws GenerateException {
		return readCellBackColorProperty(KONFIG_PROP_STREICH_SPIELTAG_COLOR_BACK_UNGERADE);
	}

	public Integer getRanglisteHeaderFarbe() throws GenerateException {
		return readCellBackColorProperty(KONFIG_PROP_RANGLISTE_COLOR_BACK_HEADER);
	}

	public Integer getSpielRundeNeuAuslosenAb() throws GenerateException {
		return readIntProperty(KONFIG_PROP_NAME_SPIELRUNDE_NEU_AUSLOSEN);
	}

	public Integer getNichtGespielteRundePlus() throws GenerateException {
		return readIntProperty(KONFIG_PROP_RANGLISTE_NICHT_GESPIELTE_RND_PLUS);
	}

	public Integer getNichtGespielteRundeMinus() throws GenerateException {
		return readIntProperty(KONFIG_PROP_RANGLISTE_NICHT_GESPIELTE_RND_MINUS);
	}

	public String getSpielrundeSpielbahn() throws GenerateException {
		return readStringProperty(KONFIG_PROP_SPIELRUNDE_SPIELBAHN);
	}

	public Formation getFormation() throws GenerateException {
		int formationId = readIntProperty(KONFIG_PROP_NAME_FORMATION);
		if (formationId > -1) {
			return Formation.findById(formationId);
		}
		return null;
	}

	/**
	 * die anzahl der Spieltage die bei der neu auslosung eingelesen werden
	 *
	 * @return
	 * @throws GenerateException
	 */
	public Integer getAnzGespielteSpieltage() throws GenerateException {
		return readIntProperty(KONFIG_PROP_ANZ_GESPIELTE_SPIELTAGE);
	}

	public SpielSystem getSpielSystem() throws GenerateException {
		int spielSystemId = readIntProperty(KONFIG_PROP_NAME_SPIELSYSTEM);
		if (spielSystemId > -1) {
			return SpielSystem.findById(spielSystemId);
		}
		return null;
	}

	public String getFusszeileLinks() throws GenerateException {
		return readStringProperty(KONFIG_PROP_FUSSZEILE_LINKS);
	}

	public String getFusszeileMitte() throws GenerateException {
		return readStringProperty(KONFIG_PROP_FUSSZEILE_MITTE);
	}
}
