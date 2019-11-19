/**
 * Erstellung 10.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.konfiguration;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.sun.star.awt.FontWeight;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.IntegerCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.WeakRefHelper;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.konfigdialog.ConfigPropertyType;

/**
 * @author Michael Massee
 *
 */
abstract public class BasePropertiesSpalte implements IPropertiesSpalte {

	private static final int MAX_LINE = 9999; // max anzahl properties
	private static final int SPALTE_WERT_WIDTH = 1500;
	private static final int SPALTE_NAME_WIDTH = 7000;

	public static final String KONFIG_PROP_NAME_TURNIERSYSTEM = "Turniersystem";
	private static final String KONFIG_PROP_MELDELISTE_COLOR_BACK_GERADE = "Meldeliste Hintergrund Gerade";
	private static final String KONFIG_PROP_MELDELISTE_COLOR_BACK_UNGERADE = "Meldeliste Hintergrund Ungerade";
	private static final String KONFIG_PROP_MELDELISTE_COLOR_BACK_HEADER = "Meldeliste Header";

	private static final String KONFIG_PROP_FUSSZEILE_LINKS = "Fußzeile links";
	private static final String KONFIG_PROP_FUSSZEILE_MITTE = "Fußzeile mitte";

	private static final String KONFIG_PROP_ZEIGE_ARBEITS_SPALTEN = "Zeige Arbeitsspalten"; // diesen Daten werden nur intern gebraucht, default = false

	protected final WeakRefHelper<ISheet> sheetWkRef;
	protected final int propertiesSpalte;
	protected final int erstePropertiesZeile;
	protected final int headerZeile;

	protected static void ADDBaseProp(List<ConfigProperty<?>> KONFIG_PROPERTIES) {
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_MELDELISTE_COLOR_BACK_GERADE).setDefaultVal(Integer.valueOf("e1e9f7", 16))
				.setDescription("Spielrunde Hintergrundfarbe für gerade Zeilen"));
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_MELDELISTE_COLOR_BACK_UNGERADE).setDefaultVal(Integer.valueOf("c0d6f7", 16))
				.setDescription("Spielrunde Hintergrundfarbe für ungerade Zeilen"));
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_MELDELISTE_COLOR_BACK_HEADER).setDefaultVal(Integer.valueOf("e6ebf4", 16))
				.setDescription("Spielrunde Header-Hintergrundfarbe"));

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.STRING, KONFIG_PROP_FUSSZEILE_LINKS).setDefaultVal("").setDescription("Fußzeile Links"));
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.STRING, KONFIG_PROP_FUSSZEILE_MITTE).setDefaultVal("").setDescription("Fußzeile Mitte"));
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.BOOLEAN, KONFIG_PROP_ZEIGE_ARBEITS_SPALTEN).setDefaultVal(false)
				.setDescription("Zeige Arbeitsdaten (N/J),Nur fuer fortgeschrittene. Default = N"));
	}

	protected BasePropertiesSpalte(int propertiesSpalte, int erstePropertiesZeile, ISheet sheet) {
		sheetWkRef = new WeakRefHelper<>(sheet);
		checkNotNull(sheet);
		checkArgument(propertiesSpalte > -1, "propertiesSpalte %s<0", propertiesSpalte);
		checkArgument(erstePropertiesZeile > 0, "erstePropertiesZeile %s<1", erstePropertiesZeile);

		this.propertiesSpalte = propertiesSpalte;
		this.erstePropertiesZeile = erstePropertiesZeile;
		headerZeile = erstePropertiesZeile - 1;
	}

	abstract protected List<ConfigProperty<?>> getKonfigProperties();

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
		return sheetWkRef.get().getSheetHelper();
	}

	@Override
	public void doFormat() throws GenerateException {
		XSpreadsheet propSheet = getPropSheet();
		// header
		Position posHeader = Position.from(propertiesSpalte, headerZeile);
		ColumnProperties columnProperties = ColumnProperties.from().setWidth(SPALTE_NAME_WIDTH);

		StringCellValue headerVal = StringCellValue.from(propSheet, posHeader).addColumnProperties(columnProperties).setValue("Name").setHoriJustify(CellHoriJustify.RIGHT)
				.setCharWeight(FontWeight.BOLD).setBorder(BorderFactory.from().allThin().toBorder());
		getSheetHelper().setTextInCell(headerVal);

		StringCellValue wertheaderVal = StringCellValue.from(propSheet, posHeader).addColumnProperties(columnProperties.setWidth(SPALTE_WERT_WIDTH)).setValue("Wert")
				.setHoriJustify(CellHoriJustify.CENTER).spaltePlusEins().setCharWeight(FontWeight.BOLD).setBorder(BorderFactory.from().allThin().toBorder());
		getSheetHelper().setTextInCell(wertheaderVal);
	}

	@Override
	public void updateKonfigBlock() throws GenerateException {
		XSpreadsheet propSheet = getPropSheet();

		Position nextFreepos = Position.from(propertiesSpalte, erstePropertiesZeile);
		// TODO umstellen auf uno find
		for (int idx = 0; idx < MAX_LINE; idx++) {
			String val = getSheetHelper().getTextFromCell(propSheet, nextFreepos);
			if (!StringUtils.isNotBlank(val)) {
				break;
			}
			nextFreepos.zeilePlusEins();
		}

		for (ConfigProperty<?> configProp : getKonfigProperties()) {
			Position pos = getPropKeyPos(configProp.getKey());
			if (pos == null) {
				// when not found insert new
				StringCellValue celVal = StringCellValue.from(propSheet, nextFreepos, configProp.getKey()).setComment(null).setHoriJustify(CellHoriJustify.RIGHT)
						.setBorder(BorderFactory.from().allThin().toBorder());
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
				case BOOLEAN:
					celVal.setValue(booleanToString((Boolean) configProp.getDefaultVal()));
					getSheetHelper().setTextInCell(celVal);
					break;
				default:
				}
				nextFreepos.zeilePlusEins();
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
			pos.spaltePlusEins();
			StringCellValue strVal = StringCellValue.from(sheet, pos, "").setCellBackColor(val).setComment(comment).setBorder(BorderFactory.from().allThin().toBorder());
			getSheetHelper().setTextInCell(strVal);
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

	public Boolean readBooleanProperty(String key) throws GenerateException {
		return stringToBoolean(readStringProperty(key));
	}

	private String booleanToString(boolean booleanProp) {
		if (booleanProp) {
			return "J";
		}
		return "N";
	}

	private boolean stringToBoolean(String booleanProp) {
		if (StringUtils.isBlank(booleanProp) || StringUtils.containsIgnoreCase(booleanProp, "N")) {
			return false;
		}
		return true;
	}

	private Object getDefaultProp(String key) {
		for (ConfigProperty<?> konfigProp : getKonfigProperties()) {
			if (konfigProp.getKey().equals(key)) {
				return konfigProp.getDefaultVal();
			}
		}
		return null;
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
		for (int idx = 0; idx < MAX_LINE; idx++) {
			String val = getSheetHelper().getTextFromCell(sheet, pos);
			if (StringUtils.isNotBlank(val) && val.trim().equalsIgnoreCase(key.trim())) {
				return pos;
			}
			if (StringUtils.isBlank(val)) {
				break;
			}
			pos.zeilePlusEins();
		}
		return null;
	}

	private final XSpreadsheet getPropSheet() throws GenerateException {
		return sheetWkRef.get().getSheet();
	}

	@Override
	public final Integer getMeldeListeHintergrundFarbeGerade() throws GenerateException {
		return readCellBackColorProperty(KONFIG_PROP_MELDELISTE_COLOR_BACK_GERADE);
	}

	@Override
	public final Integer getMeldeListeHintergrundFarbeUnGerade() throws GenerateException {
		return readCellBackColorProperty(KONFIG_PROP_MELDELISTE_COLOR_BACK_UNGERADE);
	}

	@Override
	public Integer getMeldeListeHeaderFarbe() throws GenerateException {
		return readCellBackColorProperty(KONFIG_PROP_MELDELISTE_COLOR_BACK_HEADER);
	}

	@Override
	public final String getFusszeileLinks() throws GenerateException {
		return readStringProperty(KONFIG_PROP_FUSSZEILE_LINKS);
	}

	@Override
	public final String getFusszeileMitte() throws GenerateException {
		return readStringProperty(KONFIG_PROP_FUSSZEILE_MITTE);
	}

	@Override
	public final boolean zeigeArbeitsSpalten() throws GenerateException {
		return readBooleanProperty(KONFIG_PROP_ZEIGE_ARBEITS_SPALTEN);
	}

}