/**
* Erstellung : 05.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.konfiguration;

import static com.google.common.base.Preconditions.*;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.cellvalue.IntegerCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.WeakRefHelper;
import de.petanqueturniermanager.meldeliste.IMeldeliste;

public class PropertiesSpalte {
	// private static final Logger logger = LogManager.getLogger(PropertiesSpalte.class);

	private static final String KONFIG_PROP_NAME_SPIELTAG = "Spieltag";
	private static final String KONFIG_PROP_NAME_SPIELRUNDE = "Spielrunde";

	public static final String KONFIG_PROP_NAME_SPIELRUNDE_NEU_AUSLOSEN = "NeuAuslosenAb";
	private static final String KONFIG_PROP_SPIELRUNDE_COLOR_BACK_GERADE = "Hintergr. Spielr. G";
	private static final String KONFIG_PROP_SPIELRUNDE_COLOR_BACK_UNGERADE = "Hintergr. Spielr. U";
	private static final String KONFIG_PROP_SPIELRUNDE_COLOR_BACK_HEADER = "Spielrunde-Header";

	private static final String KONFIG_PROP_RANGLISTE_COLOR_BACK_GERADE = "Hintergr. Rangliste G";
	private static final String KONFIG_PROP_RANGLISTE_COLOR_BACK_UNGERADE = "Hintergr. Rangliste U";
	private static final String KONFIG_PROP_RANGLISTE_COLOR_BACK_HEADER = "Rangliste-Header";

	public static final List<ConfigProperty<?>> KONFIG_PROPERTIES = new ArrayList<>();

	static {
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_NAME_SPIELTAG)
				.setDefaultVal(1).setDescription("Aktuelle Spieltag"));
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_NAME_SPIELRUNDE)
				.setDefaultVal(1).setDescription("Aktuelle Spielrunde"));

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_NAME_SPIELRUNDE_NEU_AUSLOSEN)
				.setDefaultVal(0).setDescription("Neu auslosen ab Spielrunde"));

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_SPIELRUNDE_COLOR_BACK_GERADE)
				.setDefaultVal(Integer.valueOf("e1e9f7", 16))
				.setDescription("Spielrunde Hintergrundfarbe für gerade Zeilen"));
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_SPIELRUNDE_COLOR_BACK_UNGERADE)
				.setDefaultVal(Integer.valueOf("c0d6f7", 16))
				.setDescription("Spielrunde Hintergrundfarbe für ungerade Zeilen"));
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_SPIELRUNDE_COLOR_BACK_HEADER)
				.setDefaultVal(Integer.valueOf("e6ebf4", 16)).setDescription("Spielrunde Header-Hintergrundfarbe"));

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_RANGLISTE_COLOR_BACK_GERADE)
				.setDefaultVal(Integer.valueOf("e1e9f7", 16))
				.setDescription("Rangliste Hintergrundfarbe für gerade Zeilen"));
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_RANGLISTE_COLOR_BACK_UNGERADE)
				.setDefaultVal(Integer.valueOf("c0d6f7", 16))
				.setDescription("Rangliste Hintergrundfarbe für ungerade Zeilen"));
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_RANGLISTE_COLOR_BACK_HEADER)
				.setDefaultVal(Integer.valueOf("e6ebf4", 16)).setDescription("Rangliste Header-Hintergrundfarbe"));

	}

	private final WeakRefHelper<ISheet> sheetWkRef;
	private final SheetHelper sheetHelper;
	private int propertiesSpalte;
	private int erstePropertiesZeile;

	public PropertiesSpalte(XComponentContext xContext, int propertiesSpalte, int erstePropertiesZeile, ISheet sheet,
			IMeldeliste meldeliste) {
		checkNotNull(xContext);
		checkNotNull(sheet);
		checkNotNull(meldeliste);
		checkArgument(propertiesSpalte > -1);

		this.propertiesSpalte = propertiesSpalte;
		this.erstePropertiesZeile = erstePropertiesZeile;
		this.sheetWkRef = new WeakRefHelper<ISheet>(sheet);
		this.sheetHelper = new SheetHelper(xContext);
	}

	public void updateKonfigBlock() {
		XSpreadsheet propSheet = this.getPropSheet();

		for (int idx = 0; idx < KONFIG_PROPERTIES.size(); idx++) {

			ConfigProperty<?> configProp = KONFIG_PROPERTIES.get(idx);

			Position pos = getPropKeyPos(configProp.getKey());
			if (pos == null) {
				// when not found insert new
				pos = Position.from(this.propertiesSpalte, this.erstePropertiesZeile + idx);
				StringCellValue celVal = StringCellValue.from(propSheet, pos, configProp.getKey())
						.setComment(configProp.getDescription()).setHoriJustify(CellHoriJustify.RIGHT);
				this.sheetHelper.setTextInCell(celVal);

				celVal.spaltePlusEins().setComment(null).setHoriJustify(CellHoriJustify.CENTER);

				// default Val schreiben
				switch (configProp.getType()) {
				case STRING:
					celVal.setValue((String) configProp.getDefaultVal());
					this.sheetHelper.setTextInCell(celVal);
					break;
				case INTEGER:
					IntegerCellValue numberCellValue = IntegerCellValue.from(celVal)
							.setValue((Integer) configProp.getDefaultVal());
					this.sheetHelper.setValInCell(numberCellValue);
					break;
				case COLOR:
					writeCellBackColorProperty(configProp.getKey(), (Integer) configProp.getDefaultVal());
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
	 */
	public int readIntProperty(String key) {
		XSpreadsheet sheet = getPropSheet();
		Position pos = getPropKeyPos(key);
		int val = -1;
		if (pos != null) {
			val = this.sheetHelper.getIntFromCell(sheet, pos.spaltePlusEins());
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
	 */

	public Integer readCellBackColorProperty(String key) {
		XSpreadsheet sheet = getPropSheet();
		Position pos = getPropKeyPos(key);
		Integer val = null;
		if (pos != null) {
			Object cellProperty = this.sheetHelper.getCellProperty(sheet, pos.spaltePlusEins(), "CellBackColor");
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

	public void writeCellBackColorProperty(String key, Integer val) {
		XSpreadsheet sheet = getPropSheet();
		Position pos = getPropKeyPos(key);
		if (pos != null) {
			this.sheetHelper.setPropertyInCell(sheet, pos.spaltePlusEins(), "CellBackColor", val);
		}
	}

	/**
	 * @param name
	 * @return defaultVal aus ConfigProperty, -1 wenn fehler
	 */
	public String readStringProperty(String key) {
		XSpreadsheet sheet = getPropSheet();
		Position pos = getPropKeyPos(key);
		String val = null;
		if (pos != null) {
			val = this.sheetHelper.getTextFromCell(sheet, pos.spaltePlusEins());
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
	 */
	public void writeIntProperty(String name, int newVal) {
		Position pos = getPropKeyPos(name);
		if (pos != null) {
			this.sheetHelper.setValInCell(getPropSheet(), pos.spaltePlusEins(), newVal);
		}
	}

	/**
	 *
	 * @param name
	 * @return null when not found
	 */
	public Position getPropKeyPos(String key) {
		checkNotNull(key);

		XSpreadsheet sheet = getPropSheet();
		Position pos = Position.from(this.propertiesSpalte, this.erstePropertiesZeile);
		// TODO umstellen auf uno find
		for (int idx = 0; idx < KONFIG_PROPERTIES.size(); idx++) {
			String val = this.sheetHelper.getTextFromCell(sheet, pos);
			if (StringUtils.isNotBlank(val) && val.trim().equalsIgnoreCase(key.trim())) {
				return pos;
			}
			pos.zeilePlusEins();
		}
		return null;
	}

	public int getSpieltag() {
		return readIntProperty(KONFIG_PROP_NAME_SPIELTAG);
	}

	public int getSpielRunde() {
		return readIntProperty(KONFIG_PROP_NAME_SPIELRUNDE);
	}

	public void setSpielRunde(int spielrunde) {
		writeIntProperty(KONFIG_PROP_NAME_SPIELRUNDE, spielrunde);
	}

	private final XSpreadsheet getPropSheet() {
		return this.sheetWkRef.getObject().getSheet();
	}

	public Integer getSpielRundeHintergrundFarbeGerade() {
		return readCellBackColorProperty(KONFIG_PROP_SPIELRUNDE_COLOR_BACK_GERADE);
	}

	public Integer getSpielRundeHintergrundFarbeUnGerade() {
		return readCellBackColorProperty(KONFIG_PROP_SPIELRUNDE_COLOR_BACK_UNGERADE);
	}

	public Integer getSpielRundeHeaderFarbe() {
		return readCellBackColorProperty(KONFIG_PROP_SPIELRUNDE_COLOR_BACK_HEADER);
	}

	public Integer getRanglisteHintergrundFarbeGerade() {
		return readCellBackColorProperty(KONFIG_PROP_RANGLISTE_COLOR_BACK_GERADE);
	}

	public Integer getRanglisteHintergrundFarbeUnGerade() {
		return readCellBackColorProperty(KONFIG_PROP_RANGLISTE_COLOR_BACK_UNGERADE);
	}

	public Integer getRanglisteHeaderFarbe() {
		return readCellBackColorProperty(KONFIG_PROP_RANGLISTE_COLOR_BACK_HEADER);
	}

	public Integer getSpielRundeNeuAuslosenAb() {
		return readIntProperty(KONFIG_PROP_NAME_SPIELRUNDE_NEU_AUSLOSEN);
	}
}
