/**
* Erstellung : 10.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.sheet;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.sheet.CellFlags;
import com.sun.star.sheet.FillDirection;
import com.sun.star.sheet.XCellAddressable;
import com.sun.star.sheet.XCellRangesQuery;
import com.sun.star.sheet.XCellSeries;
import com.sun.star.sheet.XFunctionAccess;
import com.sun.star.sheet.XSheetAnnotations;
import com.sun.star.sheet.XSheetAnnotationsSupplier;
import com.sun.star.sheet.XSheetOperation;
import com.sun.star.sheet.XSpreadsheet;
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
import com.sun.star.uno.UnoRuntime;
import com.sun.star.util.XMergeable;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.cellvalue.AbstractCellValueWithSheet;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.RowProperties;
import de.petanqueturniermanager.helper.position.FillAutoPosition;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;

// welche service ???
// https://www.openoffice.org/api/docs/common/ref/com/sun/star/lang/XComponent-xref.html

public class SheetHelper {

	private static final Logger logger = LogManager.getLogger(SheetHelper.class);

	private final WorkingSpreadsheet currentSpreadsheet;

	public SheetHelper(WorkingSpreadsheet currentSpreadsheet) {
		this.currentSpreadsheet = checkNotNull(currentSpreadsheet);
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

	public void removeAllSheetsExclude(String sheetNameNotToRemove) {
		removeAllSheetsExclude(new String[] { sheetNameNotToRemove });
	}

	public void removeAllSheetsExclude(String[] sheetNamesNotToRemove) {
		XSpreadsheets sheets = getSheets();
		List<String> sheetNamesNotToRemoveList = java.util.Arrays.asList(sheetNamesNotToRemove);
		for (String sheetName : sheets.getElementNames()) {
			if (!sheetNamesNotToRemoveList.contains(sheetName)) {
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
			xCell.setFormula(StringUtils.prependIfMissing(formula.trim(), "="));
		} catch (IndexOutOfBoundsException e) {
			logger.error(e.getMessage(), e);
		}
		return xCell;
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
	public XCell setTextInCell(StringCellValue stringVal) {
		checkNotNull(stringVal);
		XCell xCell = null;
		xCell = setTextInCell(stringVal.getSheet(), stringVal.getPos(), stringVal.getValue(), stringVal.isUeberschreiben());
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
			fillAuto(cellVal.getSheet(), RangePosition.from(cellVal.getPos(), cellVal.getFillAuto()), fillAuto.getFillDirection());
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
			XCellRange xCellRange = sheet.getCellRangeByPosition(rangePos.getStartSpalte(), rangePos.getStartZeile(), rangePos.getEndeSpalte(), rangePos.getEndeZeile());
			XCellSeries xCellSeries = UnoRuntime.queryInterface(XCellSeries.class, xCellRange);
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
		return UnoRuntime.queryInterface(clazz, arg);
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
	XCell setTextInCell(XSpreadsheet sheet, Position pos, String val, boolean ueberschreiben) {
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
			xText = UnoRuntime.queryInterface(XText.class, xCell);
		} catch (IndexOutOfBoundsException e) {
			logger.error(e.getMessage(), e);
		}
		return xText;
	}

	public String getAddressFromCellAsString(XCell xCell) {
		checkNotNull(xCell);

		XCellAddressable xCellAddr = UnoRuntime.queryInterface(XCellAddressable.class, xCell);
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
			Object aFuncInst = currentSpreadsheet.getxContext().getServiceManager().createInstanceWithContext("com.sun.star.sheet.FunctionAccess",
					currentSpreadsheet.getxContext());
			XFunctionAccess xFuncAcc = UnoRuntime.queryInterface(XFunctionAccess.class, aFuncInst);
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
	 * @param zeileNr = 0 = erste Zeile
	 * @param searchStr
	 * @return spalte 0 = erste Spalte, -1 when not found
	 */
	public int findSpalteInZeileNachString(XSpreadsheet sheet, int zeileNr, String searchStr) {
		checkNotNull(sheet);
		checkNotNull(searchStr);

		int spalteNr = -1;

		// Primitiv search
		for (int spalteCntr = 0; spalteCntr < 999; spalteCntr++) {
			String text = getTextFromCell(sheet, Position.from(spalteCntr, zeileNr));
			if (text.equals(searchStr)) {
				spalteNr = spalteCntr;
				break;
			}
		}
		return spalteNr;
	}

	public void setActiveSheet(XSpreadsheet spreadsheet) {
		currentSpreadsheet.getWorkingSpreadsheetView().setActiveSheet(spreadsheet);
	}

	/**
	 * 1. in google nach begriff "color chooser" suchen. -> Color chooser verwenden, hex code ohne #<br>
	 * 2. Color chooser in Zelle verwenden-> hex code kopieren <br>
	 *
	 * @param xSheet
	 * @param hex, 6 stellige farbcode, ohne # oder sonstige vorzeichen !
	 */
	@Deprecated
	public void setTabColor(XSpreadsheet xSheet, String hex) {
		TurnierSheet.from(xSheet).tabColor(hex);
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
		TurnierSheet.from(xSheet).tabColor(color);
	}

	public XCellRange mergeRange(XSpreadsheet sheet, RangePosition rangePosition) {
		XCellRange xCellRange = null;
		try {
			xCellRange = sheet.getCellRangeByPosition(rangePosition.getStartSpalte(), rangePosition.getStartZeile(), rangePosition.getEndeSpalte(), rangePosition.getEndeZeile());
			XMergeable xMerge = UnoRuntime.queryInterface(com.sun.star.util.XMergeable.class, xCellRange);
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
			XColumnRowRange xColRowRange = UnoRuntime.queryInterface(XColumnRowRange.class, xCellRange);
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
			XColumnRowRange xColRowRange = UnoRuntime.queryInterface(XColumnRowRange.class, xCellRange);
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
			xPropSet = UnoRuntime.queryInterface(XPropertySet.class, aRowObj);
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
			xPropSet = UnoRuntime.queryInterface(XPropertySet.class, aColumnObj);
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
		} catch (IllegalArgumentException | UnknownPropertyException | PropertyVetoException | WrappedTargetException e) {
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
			xCellRange = sheet.getCellRangeByPosition(pos.getStartSpalte(), pos.getStartZeile(), pos.getEndeSpalte(), pos.getEndeZeile());

			XPropertySet xPropSet = UnoRuntime.queryInterface(XPropertySet.class, xCellRange);
			setProperties(xPropSet, properties);
		} catch (IndexOutOfBoundsException | IllegalArgumentException e) {
			logger.error(e.getMessage(), e);
		}
		return xCellRange;
	}

	/**
	 * setPropertyInCell(sheet, pos,"CharColor", Integer.valueOf(0x003399));<br>
	 * setPropertyInCell(sheet, pos,"CharHeight", new Float(20.0));<br>
	 * // from styles.ParagraphProperties<br>
	 * setPropertyInCell(sheet, pos,"ParaLeftMargin", Integer.valueOf(500));<br>
	 * // from table.CellProperties<br>
	 * setPropertyInCell(sheet, pos,"IsCellBackgroundTransparent", Boolean.FALSE);<br>
	 * setPropertyInCell(sheet, pos,"CellBackColor", Integer.valueOf(0x99CCFF));
	 *
	 * @param sheet
	 * @param pos
	 * @param Name
	 * @param val
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
			XPropertySet xPropSet = UnoRuntime.queryInterface(XPropertySet.class, xCell);
			xPropSet.setPropertyValue(Name, val);
		} catch (IndexOutOfBoundsException | IllegalArgumentException | UnknownPropertyException | PropertyVetoException | WrappedTargetException e) {
			logger.error("\n***** Fehler beim Property in Zelle:" + Name + "=" + val + " *****\n" + e.getMessage(), e);
		}
		return xCell;
	}

	/**
	 * Horizontal zentrieren und breite, optional ein überschrift
	 *
	 * @param sheet
	 * @param pos
	 * @param header = optional, wenn vorhanden dann die wird die zeile in pos verwendet
	 */
	public void setColumnWidthAndHoriJustifyCenter(XSpreadsheet sheet, Position pos, int width, String header) {
		checkNotNull(sheet);

		if (header != null) {
			setTextInCell(sheet, pos, header, true);
		}
		setColumnCellHoriJustify(sheet, pos, CellHoriJustify.CENTER);
		setColumnWidth(sheet, pos, width);
	}

	public XPropertySet setColumnCellHoriJustify(XSpreadsheet sheet, Position pos, CellHoriJustify cellHoriJustify) {
		return setColumnCellHoriJustify(sheet, pos.getSpalte(), cellHoriJustify);
	}

	// https://api.libreoffice.org/docs/idl/ref/servicecom_1_1sun_1_1star_1_1table_1_1CellProperties.html#ac4ecfad4d3b8fcf60e5205465fb254dd
	/*
	 * use setColumnProperties(sheet, spalte, ColumnProperties.from().setHoriJustify(cellHoriJustify));
	 *
	 * @deprecated
	 */
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
		return setColumnProperty(sheet, spalte, "Width", new Integer(width));
	}

	public XCell getCell(XSpreadsheet xSheet, Position pos) {
		checkNotNull(xSheet);
		checkNotNull(pos);
		XCell xCell = null;
		try {
			xCell = xSheet.getCellByPosition(pos.getColumn(), pos.getRow());
		} catch (IndexOutOfBoundsException e) {
			logger.error(e.getMessage(), e);
		}
		return xCell;
	}

	/**
	 *
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
	 *
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
		return UnoRuntime.queryInterface(XPropertySet.class, xCell);
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
		XCellAddressable xCellAddr = UnoRuntime.queryInterface(XCellAddressable.class, xCell);
		CellAddress aAddress = xCellAddr.getCellAddress();

		// insert an annotation
		XSheetAnnotationsSupplier xAnnotationsSupp = UnoRuntime.queryInterface(XSheetAnnotationsSupplier.class, xSheet);
		XSheetAnnotations xAnnotations = xAnnotationsSupp.getAnnotations();
		xAnnotations.insertNew(aAddress, text);
		// make the annotation visible
		// XSheetAnnotationAnchor xAnnotAnchor = UnoRuntime.queryInterface(XSheetAnnotationAnchor.class, xCell);
		// XSheetAnnotation xAnnotation = xAnnotAnchor.getAnnotation();
		// xAnnotation.setIsVisible(true);
	}

	public XCellRange clearRange(XSpreadsheet xSheet, RangePosition rangePos) {
		checkNotNull(xSheet);
		checkNotNull(rangePos);

		XCellRange xRangetoClear;
		xRangetoClear = getCellRange(xSheet, rangePos);
		if (xRangetoClear != null) {
			// --- Sheet operation. ---
			XSheetOperation xSheetOp = UnoRuntime.queryInterface(com.sun.star.sheet.XSheetOperation.class, xRangetoClear);
			xSheetOp.clearContents(CellFlags.ANNOTATION | CellFlags.DATETIME | CellFlags.EDITATTR | CellFlags.FORMATTED | CellFlags.FORMULA | CellFlags.HARDATTR | CellFlags.OBJECTS
					| CellFlags.STRING | CellFlags.STYLES | CellFlags.VALUE);
		}
		return xRangetoClear;
	}

	public XCellRange getCellRange(XSpreadsheet xSheet, RangePosition rangePos) {
		checkNotNull(xSheet);
		checkNotNull(rangePos);

		XCellRange xCellRange = null;

		try {
			xCellRange = xSheet.getCellRangeByPosition(rangePos.getStartSpalte(), rangePos.getStartZeile(), rangePos.getEndeSpalte(), rangePos.getEndeZeile());
		} catch (IndexOutOfBoundsException e) {
			logger.error(e.getMessage(), e);
		}

		return xCellRange;
	}

	public XCellRangesQuery getCellRangesQuery(XSpreadsheet xSheet, RangePosition rangePos) {
		checkNotNull(xSheet);
		checkNotNull(rangePos);

		XCellRange xCellRange = null;
		XCellRangesQuery xCellRangesQuery = null;

		xCellRange = getCellRange(xSheet, rangePos);
		if (xCellRange != null) {
			xCellRangesQuery = UnoRuntime.queryInterface(com.sun.star.sheet.XCellRangesQuery.class, xCellRange);
		}
		return xCellRangesQuery;
	}

}
