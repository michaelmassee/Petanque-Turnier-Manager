/**
 * Erstellung : 10.03.2018 / Michael Massee
 **/

package de.petanqueturniermanager.helper.sheet;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XIndexAccess;
import com.sun.star.container.XNamed;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.sheet.FillDirection;
import com.sun.star.sheet.XCellAddressable;
import com.sun.star.sheet.XCellSeries;
import com.sun.star.sheet.XFunctionAccess;
import com.sun.star.sheet.XSheetAnnotations;
import com.sun.star.sheet.XSheetAnnotationsSupplier;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.sheet.XSpreadsheets;
import com.sun.star.table.CellAddress;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.XCell;
import com.sun.star.table.XCellRange;
import com.sun.star.table.XColumnRowRange;
import com.sun.star.table.XTableColumns;
import com.sun.star.table.XTableRows;
import com.sun.star.text.XText;
import com.sun.star.uno.Any;
import com.sun.star.uno.Exception;
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.XMergeable;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.cellvalue.AbstractCellValueWithSheet;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.RowProperties;
import de.petanqueturniermanager.helper.position.FillAutoPosition;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;

// welcher service ???
// https://www.openoffice.org/api/docs/common/ref/com/sun/star/lang/XComponent-xref.html

public class SheetHelper {

	private static final Logger logger = LogManager.getLogger(SheetHelper.class);

	// http://www.ooowiki.de/DeutschEnglischCalcFunktionen.html
	private static final String[] FORMULA_GERMAN_SEARCH_LIST = new String[] { "ISTNV", "WENNNV", "WENN",
			"ISOKALENDERWOCHE", "ISTZAHL" };
	private static final String[] FORMULA_ENGLISH_REPLACEMENT_LIST = new String[] { "ISNA", "IFNA", "IF", "ISOWEEKNUM",
			"ISNUMBER" };

	private final WorkingSpreadsheet currentSpreadsheet;

	public SheetHelper(WorkingSpreadsheet currentSpreadsheet) {
		this.currentSpreadsheet = checkNotNull(currentSpreadsheet);
	}

	public SheetHelper(XComponentContext xContext, XSpreadsheetDocument xSpreadsheetDocument) {
		WorkingSpreadsheet wkingSpreadsheet = new WorkingSpreadsheet(xContext, xSpreadsheetDocument);
		this.currentSpreadsheet = checkNotNull(wkingSpreadsheet);
	}

	/**
	 * Neuer Name fuer Tabelle.<br>
	 * Hinweis alle verweisse in Formule, etc auf diese Tabelle werden mit umbenant.
	 * 
	 * @param xSheet
	 * @param newName
	 * @return true wenn rename okay
	 */

	public boolean reNameSheet(XSpreadsheet xSheet, String newName) {
		boolean renameOk = false;
		XNamed xNamed = Lo.qi(XNamed.class, xSheet);

		if (xNamed != null) {
			xNamed.setName(newName);
			renameOk = true;
		}
		return renameOk;
	}

	/**
	 * @param sheetName
	 * @return null when not found
	 */
	public XSpreadsheet findByName(String sheetName) {
		checkNotNull(sheetName);
		XSpreadsheet foundSpreadsheet = null;
		XSpreadsheets sheets = getSheets();

		if (sheets != null) {
			try {
				if (sheets.hasByName(sheetName)) {
					Any currentDoc = (Any) sheets.getByName(sheetName);
					foundSpreadsheet = (XSpreadsheet) currentDoc.getObject();
				}
			} catch (NoSuchElementException | WrappedTargetException e) {
				// ignore
			}
		}
		return foundSpreadsheet;
	}

	/**
	 * @param sheetName
	 * @return the index of Sheet by name. -1 when not found
	 */
	public int getIdxByName(String sheetName) {
		int sheetPosition = -1;
		XSpreadsheet xSpreadsheet = findByName(sheetName);
		if (xSpreadsheet != null) {
			sheetPosition = TurnierSheet.from(xSpreadsheet, currentSpreadsheet).getSheetPosition();
		}
		return sheetPosition;
	}

	// return the spreadsheet with the specified index (0-based)
	public XSpreadsheet getSheetByIdx(int index) {
		XSpreadsheets sheets = getSheets();
		XSpreadsheet sheet = null;
		try {
			XIndexAccess xSheetsIdx = Lo.qi(XIndexAccess.class, sheets);
			// must convert since XSpreadsheet is a named container
			sheet = Lo.qi(XSpreadsheet.class, xSheetsIdx.getByIndex(index));
		} catch (Exception e) {

			System.out.println("Could not access spreadsheet: " + index);
		}
		return sheet;
	}

	public int getAnzSheets() {
		XSpreadsheets sheets = getSheets();
		return sheets.getElementNames().length;
	}

	public void removeAllSheetsExclude(String sheetNameNotToRemove) {
		removeAllSheetsExclude(new String[] { sheetNameNotToRemove });
	}

	public void removeAllSheetsExclude(String[] sheetNamesNotToRemove) {
		XSpreadsheets sheets = getSheets();
		List<String> sheetNamesNotToRemoveList = java.util.Arrays.asList(sheetNamesNotToRemove);

		List<String> notToRemoveVorhanden = sheetNamesNotToRemoveList.stream()
				.filter(shName -> findByName(shName) != null).collect(Collectors.toList());

		// Mindestens ein Sheet muss stehen bleiben
		if (notToRemoveVorhanden.isEmpty()) {
			notToRemoveVorhanden = java.util.Arrays.asList(new String[] { "leer" });
			newIfNotExist("leer", (short) 1);
		}

		for (String sheetName : sheets.getElementNames()) {
			if (!notToRemoveVorhanden.contains(sheetName)) {
				removeSheet(sheetName);
			}
		}
	}

	public void removeSheet(String sheetName) {
		checkNotNull(sheetName);
		XSpreadsheets sheets = getSheets();
		try {
			sheets.removeByName(sheetName);
		} catch (NoSuchElementException | WrappedTargetException e) {
			// ignore
		}
	}

	public XSpreadsheet newIfNotExist(String sheetName, int pos) {
		return newIfNotExist(sheetName, (short) pos, null);
	}

	public XSpreadsheet newIfNotExist(String sheetName, short pos) {
		return newIfNotExist(sheetName, pos, null);
	}

	public XSpreadsheet newIfNotExist(String sheetName, short pos, String tabColor) {
		checkNotNull(sheetName);
		XSpreadsheet currSpreadsheet = null;
		XSpreadsheets sheets = getSheets();

		if (sheets != null) {
			if (!sheets.hasByName(sheetName)) {
				sheets.insertNewByName(sheetName, pos);

				if (tabColor != null) {
					currSpreadsheet = findByName(sheetName);
					setTabColor(currSpreadsheet, tabColor);
				}
			}
			currSpreadsheet = findByName(sheetName);
		}
		return currSpreadsheet;
	}

	/**
	 * @return null wenn kein getCurrentSpreadsheetDocument
	 */

	public XSpreadsheets getSheets() {
		checkNotNull(currentSpreadsheet, "currentSpreadsheet==null");
		//		checkArgument(currentSpreadsheet.isPresent(), "currentSpreadsheet fehlt");
		checkNotNull(currentSpreadsheet.getWorkingSpreadsheetDocument(), "WorkingSpreadsheetDocument==null");

		return currentSpreadsheet.getWorkingSpreadsheetDocument().getSheets();
	}

	public XCell setValInCell(NumberCellValue numberCellValue) {
		checkNotNull(numberCellValue);
		XCell xCell = setValInCell(numberCellValue.getSheet(), numberCellValue.getPos(), numberCellValue.getValue());
		handleAbstractCellValue(numberCellValue, xCell);
		return xCell;
	}

	public XCell setValInCell(XSpreadsheet sheet, Position pos, int val) {
		return setValInCell(sheet, pos, (double) val);
	}

	public XCell setValInCell(XSpreadsheet sheet, Position pos, double val) {
		checkNotNull(sheet, "Sheet = null");
		checkNotNull(pos);
		XCell xCell = null;
		try {
			xCell = sheet.getCellByPosition(pos.getSpalte(), pos.getZeile());
			xCell.setValue(val);
		} catch (IndexOutOfBoundsException e) {
			logger.error(e.getMessage(), e);
		}
		return xCell;
	}

	public XCell setValInCell(XSpreadsheet sheet, Position pos, NumberCellValue cellValue) {
		checkNotNull(sheet);
		checkNotNull(pos);
		XCell xCell = setValInCell(sheet, pos, cellValue.getValue());
		return xCell;
	}

	public XCell setFormulaInCell(StringCellValue stringVal) {
		checkNotNull(stringVal);

		XCell xCell = null;
		xCell = setFormulaInCell(stringVal.getSheet(), stringVal.getPos(), stringVal.getValue());

		handleAbstractCellValue(stringVal, xCell);
		return xCell;
	}

	public XCell setFormulaInCell(XSpreadsheet sheet, Position pos, String formula) {
		checkNotNull(sheet);
		checkNotNull(pos);
		checkNotNull(formula);

		XCell xCell = null;
		try {
			xCell = sheet.getCellByPosition(pos.getSpalte(), pos.getZeile());

			// Deutsch nach Enlisch
			// http://www.ooowiki.de/DeutschEnglischCalcFunktionen.html
			formula = StringUtils.replaceEach(formula.trim(), FORMULA_GERMAN_SEARCH_LIST,
					FORMULA_ENGLISH_REPLACEMENT_LIST);
			xCell.setFormula(StringUtils.prependIfMissing(formula, "="));
		} catch (IndexOutOfBoundsException e) {
			logger.error(e.getMessage(), e);
		}
		return xCell;
	}

	public String getFormulaFromCell(XSpreadsheet sheet, Position pos) {
		checkNotNull(sheet);
		checkNotNull(pos);
		String ret = null;

		XCell xCell = null;
		try {
			xCell = sheet.getCellByPosition(pos.getSpalte(), pos.getZeile());
			ret = xCell.getFormula();
		} catch (IndexOutOfBoundsException e) {
			logger.error(e.getMessage(), e);
		}
		return ret;
	}

	/**
	 * nur die Zelle formatieren
	 *
	 * @param cellVal
	 * @return
	 */
	public XCell setFormatInCell(AbstractCellValueWithSheet<?, ?> cellVal) {
		checkNotNull(cellVal);
		checkNotNull(cellVal.getSheet());
		checkNotNull(cellVal.getPos());

		XCell xCell = null;
		try {
			// --- Get cell B3 by position - (column, row) ---
			// xCell = xSheet.getCellByPosition(1, 2);
			xCell = cellVal.getSheet().getCellByPosition(cellVal.getPos().getSpalte(), cellVal.getPos().getZeile());
			handleAbstractCellValue(cellVal, xCell);
		} catch (IndexOutOfBoundsException | IllegalArgumentException e) {
			logger.error(e.getMessage(), e);
		}
		return xCell;
	}

	// StringCellValue
	public XCell setStringValueInCell(StringCellValue stringVal) {
		checkNotNull(stringVal);
		XCell xCell = null;
		xCell = setStringValueInCell(stringVal.getSheet(), stringVal.getPos(), stringVal.getValue(),
				stringVal.isUeberschreiben());
		handleAbstractCellValue(stringVal, xCell);

		return xCell;
	}

	private void handleAbstractCellValue(AbstractCellValueWithSheet<?, ?> cellVal, XCell xCell) {
		checkNotNull(cellVal);
		checkNotNull(xCell);

		// zellen merge ?
		if (cellVal.getPos() != null && cellVal.getEndPosMerge() != null) {
			mergeRange(cellVal.getSheet(), RangePosition.from(cellVal.getPos(), cellVal.getEndPosMerge()));
		}

		// kommentar ?
		if (cellVal.getPos() != null && cellVal.hasComment()) {
			setCommentInCell(cellVal.getSheet(), xCell, cellVal.getComment());
		}

		// fill
		if (cellVal.getFillAuto() != null) {
			FillAutoPosition fillAuto = cellVal.getFillAuto();
			fillAuto(cellVal.getSheet(), RangePosition.from(cellVal.getPos(), cellVal.getFillAuto()),
					fillAuto.getFillDirection());
		}

		// Achtung: reihenfolge nicht ändern !
		// 1. Spalte Properties ?
		if (!cellVal.getColumnProperties().isEmpty()) {
			setColumnProperties(cellVal.getSheet(), cellVal.getPos().getSpalte(), cellVal.getColumnProperties());
		}

		// 2. zeile Properties ?
		if (!cellVal.getRowProperties().isEmpty()) {
			setRowProperties(cellVal.getSheet(), cellVal.getPos().getZeile(), cellVal.getRowProperties());
		}

		// 3. Zellen Properties ?
		if (!cellVal.getCellProperties().isEmpty()) {
			XPropertySet xPropSetCell = getCellPropertySet(xCell);
			setProperties(xPropSetCell, cellVal.getCellProperties());
		}
	}

	public void fillAuto(XSpreadsheet sheet, RangePosition rangePos, FillDirection fillDirection) {
		try {
			XCellRange xCellRange = sheet.getCellRangeByPosition(rangePos.getStartSpalte(), rangePos.getStartZeile(),
					rangePos.getEndeSpalte(), rangePos.getEndeZeile());
			XCellSeries xCellSeries = Lo.qi(XCellSeries.class, xCellRange);
			xCellSeries.fillAuto(fillDirection, 1);
		} catch (IndexOutOfBoundsException e) {
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * for Junit Mock
	 */
	@VisibleForTesting
	<C> C queryInterface(Class<C> clazz, Object arg) {
		return Lo.qi(clazz, arg);
	}

	/**
	 * @param sheet
	 * @param spalte,column, 0 = erste spalte = A
	 * @param zeile,row, 0 = erste zeile
	 * @param ueberschreiben, true = dann wird der vorhandene inhalt überschrieben, false = nur wenn leer dann wert schreiben. <br>
	 * leer = null, leer string, oder nur leerzeichen
	 * @return XCell
	 */
	@VisibleForTesting
	XCell setStringValueInCell(XSpreadsheet sheet, Position pos, String val, boolean ueberschreiben) {
		checkNotNull(sheet);
		checkNotNull(pos);
		checkNotNull(val);

		XCell xCell = null;
		try {
			// --- Get cell B3 by position - (column, row) ---
			// xCell = xSheet.getCellByPosition(1, 2);
			xCell = sheet.getCellByPosition(pos.getSpalte(), pos.getZeile());
			XText xText = queryInterface(XText.class, xCell);
			if (ueberschreiben) {
				xText.setString(val);
			} else if (StringUtils.isBlank(xText.getString())) {
				// Checks if a CharSequence is empty (""), null or whitespace only.
				xText.setString(val);
			}
		} catch (IndexOutOfBoundsException | IllegalArgumentException e) {
			logger.error(e.getMessage(), e);
		}
		return xCell;
	}

	/**
	 * @param sheet
	 * @param spalte,column, 0 = erste spalte = A
	 * @param zeile,row, 0 = erste zeile
	 * @return -1 when not found
	 */
	public Integer getIntFromCell(XSpreadsheet sheet, Position pos) {
		checkNotNull(sheet);
		checkNotNull(pos);

		// Convert a String to an int, returning a default value if the conversion fails.
		return NumberUtils.toInt(getTextFromCell(sheet, pos), -1);
	}

	/**
	 * @param sheet
	 * @param spalte,column, 0 = erste spalte = A
	 * @param zeile,row, 0 = erste zeile
	 * @return timed textval, null when not found
	 */
	public String getTextFromCell(XSpreadsheet sheet, Position pos) {
		checkNotNull(sheet);
		checkNotNull(pos);

		XText xText = getXTextFromCell(sheet, pos);
		if (xText != null && xText.getString() != null) {
			return xText.getString().trim();
		}
		return null;
	}

	public XText getXTextFromCell(XSpreadsheet sheet, Position pos) {
		checkNotNull(sheet);
		checkNotNull(pos);

		XText xText = null;
		try {
			XCell xCell = sheet.getCellByPosition(pos.getSpalte(), pos.getZeile());
			xText = Lo.qi(XText.class, xCell);
		} catch (IndexOutOfBoundsException e) {
			logger.error(e.getMessage(), e);
		}
		return xText;
	}

	public String getAddressFromCellAsString(XCell xCell) {
		checkNotNull(xCell);

		XCellAddressable xCellAddr = Lo.qi(XCellAddressable.class, xCell);
		CellAddress aAddress = xCellAddr.getCellAddress();
		return getAddressFromColumnRow(Position.from(aAddress.Column, aAddress.Row));
	}

	/**
	 * @param column = spalte, erste spalte = 0
	 * @param row = zeile, erste zeile = 0
	 * @return "A2"
	 */
	public String getAddressFromColumnRow(Position pos) {
		checkNotNull(pos);
		try {
			XFunctionAccess xFuncAcc = currentSpreadsheet.createInstanceMCF(XFunctionAccess.class,
					"com.sun.star.sheet.FunctionAccess");
			// https://wiki.openoffice.org/wiki/Documentation/How_Tos/Calc:_ADDRESS_function
			// put the data in a array
			Object[] data = { pos.getRow() + 1, pos.getColumn() + 1, 4, 1 };
			Object addressString = xFuncAcc.callFunction("ADDRESS", data);
			if (addressString != null) {
				return addressString.toString();
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	/**
	 * @see TurnierSheet.from(xSheet, currentSpreadsheet).setActiv()
	 * @param spreadsheet
	 */

	public void setActiveSheet(XSpreadsheet spreadsheet) {
		TurnierSheet.from(spreadsheet, currentSpreadsheet).setActiv();
	}

	/**
	 * 1. in google nach begriff "color chooser" suchen. -> Color chooser verwenden, hex code ohne #<br>
	 * 2. Color chooser in Zelle verwenden-> hex code kopieren <br>
	 *
	 * @see TurnierSheet.from(xSheet, currentSpreadsheet).tabColor
	 * @param xSheet
	 * @param hex, 6 stellige farbcode, ohne # oder sonstige vorzeichen !
	 */

	public void setTabColor(XSpreadsheet xSheet, String hex) {
		TurnierSheet.from(xSheet, currentSpreadsheet).tabColor(hex);
	}

	/**
	 * 1. in google nach begriff "color chooser" suchen. -> Color chooser verwenden, hex code ohne #<br>
	 * 2. Color chooser in Zelle verwenden-> hex code kopieren und <br>
	 * setTabColor(XSpreadsheet xSheet, String hex) verwenden <br>
	 * <br>
	 * Property TabColor in Sheet <br>
	 * list of properties <br>
	 * https://api.libreoffice.org/docs/idl/ref/servicecom_1_1sun_1_1star_1_1sheet_1_1Spreadsheet.html#details
	 *
	 * @param xSheet
	 * @param color int val. convert from hex z.b. Integer.valueOf(0x003399), Integer.parseInt("003399", 16)
	 */
	@Deprecated
	public void setTabColor(XSpreadsheet xSheet, int color) {
		TurnierSheet.from(xSheet, currentSpreadsheet).tabColor(color);
	}

	public XCellRange mergeRange(XSpreadsheet sheet, RangePosition rangePosition) {
		XCellRange xCellRange = null;
		try {
			xCellRange = sheet.getCellRangeByPosition(rangePosition.getStartSpalte(), rangePosition.getStartZeile(),
					rangePosition.getEndeSpalte(), rangePosition.getEndeZeile());
			XMergeable xMerge = Lo.qi(com.sun.star.util.XMergeable.class, xCellRange);
			xMerge.merge(true);
		} catch (IndexOutOfBoundsException e) {
			logger.error(e.getMessage(), e);
		}
		return xCellRange;
	}

	// zeile
	public Object getRow(XSpreadsheet sheet, int zeile) {
		checkNotNull(sheet);
		Object aColumnObj = null;
		try {
			// spalte, zeile
			XCellRange xCellRange = sheet.getCellRangeByPosition(0, zeile, 1, zeile);
			XColumnRowRange xColRowRange = Lo.qi(XColumnRowRange.class, xCellRange);
			XTableRows rows = xColRowRange.getRows();
			aColumnObj = rows.getByIndex(0);
		} catch (IndexOutOfBoundsException | WrappedTargetException | IllegalArgumentException e) {
			logger.error(e.getMessage(), e);
		}
		return aColumnObj;
	}

	// spalte
	public Object getColumn(XSpreadsheet sheet, int spalte) {
		checkNotNull(sheet);
		Object aColumnObj = null;
		try {
			// spalte, zeile
			XCellRange xCellRange = sheet.getCellRangeByPosition(spalte, 0, spalte, 1);
			XColumnRowRange xColRowRange = Lo.qi(XColumnRowRange.class, xCellRange);
			XTableColumns columns = xColRowRange.getColumns();
			aColumnObj = columns.getByIndex(0);
		} catch (IndexOutOfBoundsException | WrappedTargetException | IllegalArgumentException e) {
			logger.error(e.getMessage(), e);
		}
		return aColumnObj;
	}

	public XPropertySet setRowProperties(XSpreadsheet sheet, int spalte, RowProperties properties) {
		checkNotNull(sheet);
		checkNotNull(properties);
		XPropertySet xPropSet = getRowPropertySet(sheet, spalte);
		setProperties(xPropSet, properties);
		return xPropSet;
	}

	// zeile
	public XPropertySet setRowProperty(XSpreadsheet sheet, int zeile, String key, Object val) {
		XPropertySet xPropSet = getRowPropertySet(sheet, zeile);
		if (xPropSet != null) {
			setProperty(xPropSet, key, val);
		}
		return xPropSet;
	}

	// zeile
	public XPropertySet getRowPropertySet(XSpreadsheet sheet, int zeile) {
		checkNotNull(sheet);
		Object aRowObj = getRow(sheet, zeile);
		XPropertySet xPropSet = null;
		if (aRowObj != null) {
			xPropSet = Lo.qi(XPropertySet.class, aRowObj);
		}
		return xPropSet;
	}

	// ColumnProperties
	public XPropertySet setColumnProperties(XSpreadsheet sheet, int spalte, ColumnProperties properties) {
		checkNotNull(sheet);
		checkNotNull(properties);
		XPropertySet xPropSet = getColumnPropertySet(sheet, spalte);
		setProperties(xPropSet, properties);
		return xPropSet;
	}

	// spalte
	public XPropertySet setColumnProperty(XSpreadsheet sheet, int spalte, String key, Object val) {
		checkNotNull(sheet);
		XPropertySet xPropSet = getColumnPropertySet(sheet, spalte);
		if (xPropSet != null) {
			setProperty(xPropSet, key, val);
		}
		return xPropSet;
	}

	// spalte
	public XPropertySet getColumnPropertySet(XSpreadsheet sheet, int spalte) {
		checkNotNull(sheet);
		Object aColumnObj = getColumn(sheet, spalte);
		XPropertySet xPropSet = null;
		if (aColumnObj != null) {
			xPropSet = Lo.qi(XPropertySet.class, aColumnObj);
		}
		return xPropSet;
	}

	public void setProperties(XPropertySet xPropSet, CellProperties properties) {
		properties.forEach((key, value) -> {
			setProperty(xPropSet, key, value);
		});
	}

	public void setProperties(XPropertySet xPropSet, HashMap<String, Object> properties) {
		properties.forEach((key, value) -> {
			setProperty(xPropSet, key, value);
		});
	}

	public void setProperty(XPropertySet xPropSet, String key, Object val) {
		checkNotNull(key);
		checkNotNull(val);
		checkNotNull(xPropSet);

		try {
			xPropSet.setPropertyValue(key, val);
		} catch (IllegalArgumentException | UnknownPropertyException | PropertyVetoException
				| WrappedTargetException e) {
			logger.error("Property '" + key + "' = '" + val + "'\r" + e.getMessage(), e);
		}
	}

	/**
	 * example properties <br>
	 *
	 * @see #setPropertyInCell(XSpreadsheet, Position, String, Object)
	 * @param sheet
	 * @param pos
	 * @param name
	 * @param val
	 * @return xcellrange, null when error
	 */

	public XCellRange setPropertyInRange(XSpreadsheet sheet, RangePosition pos, String key, Object val) {
		checkNotNull(sheet);
		checkNotNull(pos);
		checkNotNull(key);
		checkNotNull(val);
		CellProperties properties = CellProperties.from();
		properties.put(key, val);
		return setPropertiesInRange(sheet, pos, properties);
	}

	public XCellRange setPropertiesInRange(XSpreadsheet sheet, RangePosition pos, CellProperties properties) {
		checkNotNull(sheet);
		checkNotNull(pos);
		checkNotNull(properties);

		XCellRange xCellRange = null;
		try {
			// // spalte, zeile,spalte, zeile
			xCellRange = sheet.getCellRangeByPosition(pos.getStartSpalte(), pos.getStartZeile(), pos.getEndeSpalte(),
					pos.getEndeZeile());

			XPropertySet xPropSet = Lo.qi(XPropertySet.class, xCellRange);
			setProperties(xPropSet, properties);
		} catch (IndexOutOfBoundsException | IllegalArgumentException e) {
			logger.error(e.getMessage(), e);
		}
		return xCellRange;
	}

	/**
	 * @return xcell, null whenn error or not found
	 */
	public XCell setPropertyInCell(XSpreadsheet sheet, Position pos, String Name, Object val) {
		checkNotNull(sheet);
		checkNotNull(pos);
		checkNotNull(Name);
		checkNotNull(val);

		XCell xCell = null;
		try {
			xCell = sheet.getCellByPosition(pos.getSpalte(), pos.getZeile());
			XPropertySet xPropSet = Lo.qi(XPropertySet.class, xCell);
			xPropSet.setPropertyValue(Name, val);
		} catch (IndexOutOfBoundsException | IllegalArgumentException | UnknownPropertyException | PropertyVetoException
				| WrappedTargetException e) {
			logger.error("\n***** Fehler beim Property in Zelle:" + Name + "=" + val + " *****\n" + e.getMessage(), e);
		}
		return xCell;
	}

	public XPropertySet setColumnCellHoriJustify(XSpreadsheet sheet, Position pos, CellHoriJustify cellHoriJustify) {
		return setColumnCellHoriJustify(sheet, pos.getSpalte(), cellHoriJustify);
	}

	/*
	 * use setColumnProperties(sheet, spalte, ColumnProperties.from().setHoriJustify(cellHoriJustify));
	 *
	 * @deprecated
	 */
	@Deprecated
	public XPropertySet setColumnCellHoriJustify(XSpreadsheet sheet, int spalte, CellHoriJustify cellHoriJustify) {
		checkNotNull(sheet);
		// HoriJustify ,VertJustify ,Orientation
		return setColumnProperties(sheet, spalte, ColumnProperties.from().setHoriJustify(cellHoriJustify));
	}

	public XPropertySet setColumnWidth(XSpreadsheet sheet, Position pos, int width) {
		checkNotNull(sheet);
		return setColumnWidth(sheet, pos.getSpalte(), width);
	}

	public XPropertySet setColumnWidth(XSpreadsheet sheet, int spalte, int width) {
		checkNotNull(sheet);
		return setColumnProperty(sheet, spalte, "Width", Integer.valueOf(width));
	}

	public XCell getCell(XSpreadsheet xSheet, Position pos) {
		checkNotNull(xSheet);
		checkNotNull(pos);
		return TurnierSheet.from(xSheet, currentSpreadsheet).getCell(pos);
	}

	/**
	 * @param xCell
	 * @param key
	 * @return null when not found
	 */

	public Object getCellProperty(XSpreadsheet xSheet, Position pos, String key) {
		checkNotNull(xSheet);
		checkNotNull(pos);
		checkNotNull(key);
		Object val = null;
		XCell cell = getCell(xSheet, pos);

		if (cell != null) {
			val = getCellProperty(cell, key);
		}

		return val;
	}

	/**
	 * @param xCell
	 * @param key
	 * @return null when not found
	 */
	public Object getCellProperty(XCell xCell, String key) {
		checkNotNull(xCell);
		checkNotNull(key);
		Object val = null;
		XPropertySet cellPropertySet = getCellPropertySet(xCell);
		try {
			val = cellPropertySet.getPropertyValue(key);
		} catch (UnknownPropertyException | WrappedTargetException e) {
			logger.error(e.getMessage(), e);
		}
		return val;
	}

	public XPropertySet getCellPropertySet(XCell xCell) {
		checkNotNull(xCell);
		return Lo.qi(XPropertySet.class, xCell);
	}

	public void setCommentInCell(XSpreadsheet xSheet, Position pos, String text) {
		XCell xCell = getCell(xSheet, pos);
		if (xCell != null) {
			setCommentInCell(xSheet, xCell, text);
		}
	}

	public void setCommentInCell(XSpreadsheet xSheet, XCell xCell, String text) {
		checkNotNull(xSheet);
		checkNotNull(text);
		checkNotNull(xCell);

		// XCell xCell = getCell( xSheet,pos);

		// create the CellAddress struct
		XCellAddressable xCellAddr = Lo.qi(XCellAddressable.class, xCell);
		CellAddress aAddress = xCellAddr.getCellAddress();

		// insert an annotation
		XSheetAnnotationsSupplier xAnnotationsSupp = Lo.qi(XSheetAnnotationsSupplier.class, xSheet);
		XSheetAnnotations xAnnotations = xAnnotationsSupp.getAnnotations();
		xAnnotations.insertNew(aAddress, text);
		// make the annotation visible
		// XSheetAnnotationAnchor xAnnotAnchor = Lo.qi(XSheetAnnotationAnchor.class, xCell);
		// XSheetAnnotation xAnnotation = xAnnotAnchor.getAnnotation();
		// xAnnotation.setIsVisible(true);
	}

	/**
	 * Cellinhalt löschen
	 *
	 * @param xSheet
	 * @param from
	 */
	public void clearValInCell(XSpreadsheet sheet, Position pos) {
		checkNotNull(sheet, "Sheet = null");
		checkNotNull(pos);
		setStringValueInCell(sheet, pos, "", true);
	}

}
