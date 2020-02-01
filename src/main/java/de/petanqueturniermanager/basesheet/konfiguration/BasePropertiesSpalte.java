/**
 * Erstellung 10.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.konfiguration;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.regex.Pattern;

import com.sun.star.awt.FontWeight;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;
import com.sun.star.table.TableBorder2;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.StringTools;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.IntegerCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.RowProperties;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.SearchHelper;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.WeakRefHelper;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.konfigdialog.ConfigPropertyType;
import de.petanqueturniermanager.konfigdialog.HeaderFooterConfigProperty;

/**
 * @author Michael Massee
 *
 */
abstract public class BasePropertiesSpalte implements IPropertiesSpalte {

	public static final int PROP_CELL_MARGIN = 150;
	public static final int HEADER_HEIGHT = 800;
	public static final TableBorder2 HEADER_BORDER = BorderFactory.from().allThin().boldLn().forBottom().toBorder();
	public static final String HEADER_BACK_COLOR = "#dedbd3";
	public static final Integer DEFAULT_GERADE_BACK_COLOR = Integer.valueOf("e1e9f7", 16);
	public static final Integer DEFAULT_UNGERADE__BACK_COLOR = Integer.valueOf("c0d6f7", 16);
	public static final Integer DEFAULT_HEADER__BACK_COLOR = Integer.valueOf("e6ebf4", 16);
	private static final int MAX_LINE = 9999; // max anzahl properties
	private static final int SPALTE_WERT_WIDTH = 1500;
	private static final int SPALTE_NAME_WIDTH = 7000;

	public static final String KONFIG_PROP_NAME_TURNIERSYSTEM = "Turniersystem";
	private static final String KONFIG_PROP_MELDELISTE_COLOR_BACK_GERADE = "Meldeliste Hintergrund Gerade";
	private static final String KONFIG_PROP_MELDELISTE_COLOR_BACK_UNGERADE = "Meldeliste Hintergrund Ungerade";
	private static final String KONFIG_PROP_MELDELISTE_COLOR_BACK_HEADER = "Meldeliste Header";

	private static final String KONFIG_PROP_RANGLISTE_COLOR_BACK_GERADE = "Rangliste Hintergrund Gerade";
	private static final String KONFIG_PROP_RANGLISTE_COLOR_BACK_UNGERADE = "Rangliste Hintergrund Ungerade";
	private static final String KONFIG_PROP_RANGLISTE_COLOR_BACK_HEADER = "Rangliste Header";

	private static final String KONFIG_PROP_FUSSZEILE_LINKS = "Fußzeile links";
	private static final String KONFIG_PROP_FUSSZEILE_MITTE = "Fußzeile mitte";

	private static final String KONFIG_PROP_ZEIGE_ARBEITS_SPALTEN = "Zeige Arbeitsspalten"; // diesen Daten werden nur intern gebraucht, default = false

	protected final WeakRefHelper<ISheet> sheetWkRef;
	protected final int propertiesSpalte;
	protected final int erstePropertiesZeile;
	protected final int headerZeile;

	protected static void ADDBaseProp(List<ConfigProperty<?>> KONFIG_PROPERTIES) {
		KONFIG_PROPERTIES.add(HeaderFooterConfigProperty.from(KONFIG_PROP_FUSSZEILE_LINKS).setDescription("Fußzeile Links").inSideBar());
		KONFIG_PROPERTIES.add(HeaderFooterConfigProperty.from(KONFIG_PROP_FUSSZEILE_MITTE).setDescription("Fußzeile Mitte").inSideBar());

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_MELDELISTE_COLOR_BACK_GERADE).setDefaultVal(DEFAULT_GERADE_BACK_COLOR)
				.setDescription("Meldeliste Hintergrundfarbe für gerade Zeilen").inSideBar());
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_MELDELISTE_COLOR_BACK_UNGERADE).setDefaultVal(DEFAULT_UNGERADE__BACK_COLOR)
				.setDescription("Meldeliste Hintergrundfarbe für ungerade Zeilen").inSideBar());
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_MELDELISTE_COLOR_BACK_HEADER).setDefaultVal(DEFAULT_HEADER__BACK_COLOR)
				.setDescription("Meldeliste Hintergrundfarbe für die Tabelle-Kopfzeilen").inSideBar());

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_RANGLISTE_COLOR_BACK_GERADE).setDefaultVal(DEFAULT_GERADE_BACK_COLOR)
				.setDescription("Rangliste Hintergrundfarbe für gerade Zeilen").inSideBar());
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_RANGLISTE_COLOR_BACK_UNGERADE).setDefaultVal(DEFAULT_UNGERADE__BACK_COLOR)
				.setDescription("Rangliste Hintergrundfarbe für ungerade Zeilen").inSideBar());
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_RANGLISTE_COLOR_BACK_HEADER).setDefaultVal(DEFAULT_HEADER__BACK_COLOR)
				.setDescription("Rangliste Hintergrundfarbe für die Tabelle-Kopfzeilen").inSideBar());

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.BOOLEAN, KONFIG_PROP_ZEIGE_ARBEITS_SPALTEN).setDefaultVal(false)
				.setDescription("Zeige Arbeitsdaten (N/J),Nur fuer fortgeschrittene Benutzer empfohlen. Default = N").inSideBar());
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

	protected WorkingSpreadsheet getWorkingSpreadsheet() {
		return sheetWkRef.get().getWorkingSpreadsheet();
	}

	@Override
	public void doFormat() throws GenerateException {
		XSpreadsheet propSheet = getPropSheet();
		// header
		Position posHeader = Position.from(propertiesSpalte, headerZeile);
		ColumnProperties columnProperties = ColumnProperties.from().setWidth(SPALTE_NAME_WIDTH).margin(PROP_CELL_MARGIN);
		RowProperties rowProperties = RowProperties.from().setHeight(HEADER_HEIGHT).setVertJustify(CellVertJustify2.CENTER);

		StringCellValue headerVal = StringCellValue.from(propSheet, posHeader).addColumnProperties(columnProperties).setValue("Name").setHoriJustify(CellHoriJustify.RIGHT)
				.setCharWeight(FontWeight.BOLD).setBorder(HEADER_BORDER).addRowProperties(rowProperties).setCellBackColor(HEADER_BACK_COLOR);
		getSheetHelper().setStringValueInCell(headerVal);

		StringCellValue wertheaderVal = StringCellValue.from(propSheet, posHeader).addColumnProperties(columnProperties.setWidth(SPALTE_WERT_WIDTH)).setValue("Wert")
				.setHoriJustify(CellHoriJustify.CENTER).spaltePlusEins().setCharWeight(FontWeight.BOLD).setBorder(HEADER_BORDER).setCellBackColor(HEADER_BACK_COLOR);
		getSheetHelper().setStringValueInCell(wertheaderVal);

		// Rand erste Spalte A etwas schmaller
		getSheetHelper().setColumnProperties(getPropSheet(), 0, ColumnProperties.from().setWidth(600));

	}

	@Override
	public void updateKonfigBlock() throws GenerateException {
		XSpreadsheet propSheet = getPropSheet();

		Position nextFreepos = SearchHelper.from(sheetWkRef, RangePosition.from(propertiesSpalte, erstePropertiesZeile, propertiesSpalte, MAX_LINE)).searchLastEmptyInSpalte();
		TableBorder2 border = BorderFactory.from().allThin().toBorder();
		StringCellValue celValKey = StringCellValue.from(propSheet).setComment(null).setHoriJustify(CellHoriJustify.RIGHT).setBorder(border)
				.addRowProperties(RowProperties.from().setHeight(600)).setVertJustify(CellVertJustify2.CENTER);

		StringCellValue celValWert = StringCellValue.from(celValKey).setHoriJustify(CellHoriJustify.CENTER);

		for (ConfigProperty<?> configProp : getKonfigProperties()) {

			if (configProp.isInSideBar()) {
				continue;
			}

			Position pos = getPropKeyPos(configProp.getKey());
			if (pos == null) {
				// when not found insert new
				celValKey.setPos(nextFreepos).setValue(configProp.getKey());
				getSheetHelper().setStringValueInCell(celValKey);

				celValWert.setPos(nextFreepos).spaltePlusEins().setComment(configProp.getDescription());

				// default Val schreiben
				switch (configProp.getType()) {
				case STRING:
					celValWert.setValue((String) configProp.getDefaultVal());
					getSheetHelper().setStringValueInCell(celValWert);
					break;
				case INTEGER:
					IntegerCellValue numberCellValue = IntegerCellValue.from(celValWert).setValue((Integer) configProp.getDefaultVal());
					getSheetHelper().setValInCell(numberCellValue);
					break;
				case COLOR:
					writeCellBackColorProperty(configProp.getKey(), (Integer) configProp.getDefaultVal(), configProp.getDescription());
					break;
				case BOOLEAN:
					celValWert.setValue(StringTools.booleanToString((Boolean) configProp.getDefaultVal()));
					getSheetHelper().setStringValueInCell(celValWert);
					break;
				default:
				}
				nextFreepos.zeilePlusEins();
			}
		}

		// Sortieren
		// Funktioniert nicht richtig !
		// RangePosition allPropRange = RangePosition.from(propertiesSpalte, erstePropertiesZeile, nextFreepos.zeilePlus(-1).spalte(propertiesSpalte + 1));
		// SortHelper.from(sheetWkRef.get(), allPropRange).aufSteigendSortieren().bindFormatsToContent().doSort();
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
		Integer val = null;

		if (isInSideBar(key)) {
			// value aus Document properties lesen
			Object defaultVal = getDefaultProp(key);
			DocumentPropertiesHelper docPropHelper = new DocumentPropertiesHelper(sheetWkRef.get().getWorkingSpreadsheet());
			val = docPropHelper.getIntProperty(key, (defaultVal == null) ? 0 : (Integer) defaultVal);
		} else {
			XSpreadsheet sheet = getPropSheet();
			Position pos = getPropKeyPos(key);
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
		}
		return val;
	}

	public void writeCellBackColorProperty(String key, Integer val, String comment) throws GenerateException {
		XSpreadsheet sheet = getPropSheet();
		Position pos = getPropKeyPos(key);
		if (pos != null) {
			pos.spaltePlusEins();
			StringCellValue strVal = StringCellValue.from(sheet, pos, "").setCellBackColor(val).setComment(comment).setBorder(BorderFactory.from().allThin().toBorder());
			getSheetHelper().setStringValueInCell(strVal);
		}
	}

	/**
	 * @param name
	 * @return defaultVal aus ConfigProperty, -1 wenn fehler
	 * @throws GenerateException
	 */
	public String readStringProperty(String key) throws GenerateException {
		String val = null;
		// übergang
		if (isInSideBar(key)) {
			// value aus Document properties lesen
			Object defaultVal = getDefaultProp(key);
			DocumentPropertiesHelper docPropHelper = new DocumentPropertiesHelper(sheetWkRef.get().getWorkingSpreadsheet());
			val = docPropHelper.getStringProperty(key, true, ((defaultVal == null) ? "" : defaultVal.toString()));
		} else {
			// Deprecated
			XSpreadsheet sheet = getPropSheet();
			Position pos = getPropKeyPos(key);
			if (pos != null) {
				val = getSheetHelper().getTextFromCell(sheet, pos.spaltePlusEins());
			}

			if (val == null) {
				Object defaultVal = getDefaultProp(key);
				if (defaultVal != null && defaultVal instanceof String) {
					val = (String) defaultVal;
				}
			}
		}
		return val;
	}

	public Boolean readBooleanProperty(String key) throws GenerateException {
		return StringTools.stringToBoolean(readStringProperty(key));
	}

	private boolean isInSideBar(String key) {
		for (ConfigProperty<?> konfigProp : getKonfigProperties()) {
			if (konfigProp.getKey().equals(key)) {
				return konfigProp.isInSideBar();
			}
		}
		return false;
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
	 * Bereich mit alle Properties.<br>
	 * Wird für Formula gebraucht
	 *
	 * @return
	 */
	@Override
	public String suchMatrixProperty() {
		Position start = Position.from(IKonfigurationKonstanten.NAME_PROPERTIES_SPALTE, IKonfigurationKonstanten.ERSTE_ZEILE_PROPERTIES);
		Position end = Position.from(start).spaltePlusEins().zeile(MAX_LINE);
		return start.getAddressWith$() + ":" + end.getAddressWith$();
	}

	/**
	 * @param name
	 * @return null when not found
	 * @throws GenerateException
	 */
	public Position getPropKeyPos(String key) throws GenerateException {
		checkNotNull(key);
		RangePosition searchRange = RangePosition.from(propertiesSpalte, erstePropertiesZeile, propertiesSpalte, MAX_LINE);
		// die komplette zelle inhalt muss uebreinstimmen, deswegen ^ und $
		// alle sonderzeichen escapen
		return SearchHelper.from(sheetWkRef, searchRange).searchNachRegExprInSpalte("(?i)^" + Pattern.quote(key) + "$"); // ignore case
	}

	private final XSpreadsheet getPropSheet() throws GenerateException {
		return sheetWkRef.get().getXSpreadSheet();
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

	@Override
	public Integer getRanglisteHeaderFarbe() throws GenerateException {
		return readCellBackColorProperty(KONFIG_PROP_RANGLISTE_COLOR_BACK_HEADER);
	}

	@Override
	public Integer getRanglisteHintergrundFarbeGerade() throws GenerateException {
		return readCellBackColorProperty(KONFIG_PROP_RANGLISTE_COLOR_BACK_GERADE);
	}

	@Override
	public Integer getRanglisteHintergrundFarbeUnGerade() throws GenerateException {
		return readCellBackColorProperty(KONFIG_PROP_RANGLISTE_COLOR_BACK_UNGERADE);
	}

}