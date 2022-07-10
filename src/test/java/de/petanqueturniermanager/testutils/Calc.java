package de.petanqueturniermanager.testutils;

// Calc.java
// Andrew Davison, ad@fivedots.coe.psu.ac.th, September 2014

/*
 * A growing collection of utility functions to make Office easier to use. They are currently divided into the following groups: document methods sheet methods view methods view data methods
 * insert/remove rows, columns, cells
 * 
 * get/set values in cells, arrays, rows, columns get XCell and XCellRange methods convert cell/cellrange names to positions get cell and range addresses convert cell ranges to strings search
 * 
 * cell decoration scenarios data pilot methods using calc functions solver methods
 * 
 * headers /footers
 */

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XIndexAccess;
import com.sun.star.container.XNameContainer;
import com.sun.star.container.XNamed;
import com.sun.star.frame.XComponentLoader;
import com.sun.star.frame.XController;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XModel;
import com.sun.star.lang.XComponent;
import com.sun.star.sheet.CellDeleteMode;
import com.sun.star.sheet.CellInsertMode;
import com.sun.star.sheet.FillDateMode;
import com.sun.star.sheet.FunctionArgument;
import com.sun.star.sheet.GeneralFunction;
import com.sun.star.sheet.GoalResult;
import com.sun.star.sheet.SolverConstraint;
import com.sun.star.sheet.SolverConstraintOperator;
import com.sun.star.sheet.XCellAddressable;
import com.sun.star.sheet.XCellRangeAddressable;
import com.sun.star.sheet.XCellRangeData;
import com.sun.star.sheet.XCellRangeMovement;
import com.sun.star.sheet.XCellSeries;
import com.sun.star.sheet.XDataPilotTable;
import com.sun.star.sheet.XDataPilotTables;
import com.sun.star.sheet.XDataPilotTablesSupplier;
import com.sun.star.sheet.XFunctionAccess;
import com.sun.star.sheet.XFunctionDescriptions;
import com.sun.star.sheet.XGoalSeek;
import com.sun.star.sheet.XHeaderFooterContent;
import com.sun.star.sheet.XRecentFunctions;
import com.sun.star.sheet.XScenario;
import com.sun.star.sheet.XScenarios;
import com.sun.star.sheet.XScenariosSupplier;
import com.sun.star.sheet.XSheetAnnotation;
import com.sun.star.sheet.XSheetAnnotationAnchor;
import com.sun.star.sheet.XSheetAnnotations;
import com.sun.star.sheet.XSheetAnnotationsSupplier;
import com.sun.star.sheet.XSheetCellCursor;
import com.sun.star.sheet.XSheetCellRange;
import com.sun.star.sheet.XSheetOperation;
import com.sun.star.sheet.XSolver;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.sheet.XSpreadsheetView;
import com.sun.star.sheet.XSpreadsheets;
import com.sun.star.sheet.XUsedAreaCursor;
import com.sun.star.sheet.XViewFreezable;
import com.sun.star.sheet.XViewPane;
import com.sun.star.style.XStyle;
import com.sun.star.table.BorderLine2;
import com.sun.star.table.CellAddress;
import com.sun.star.table.CellContentType;
import com.sun.star.table.CellRangeAddress;
import com.sun.star.table.TableBorder2;
import com.sun.star.table.XCell;
import com.sun.star.table.XCellRange;
import com.sun.star.table.XColumnRowRange;
import com.sun.star.table.XTableColumns;
import com.sun.star.table.XTableRows;
import com.sun.star.text.XText;
import com.sun.star.text.XTextCursor;
import com.sun.star.uno.Exception;
import com.sun.star.util.NumberFormat;
import com.sun.star.util.XNumberFormatTypes;
import com.sun.star.util.XNumberFormats;
import com.sun.star.util.XNumberFormatsSupplier;
import com.sun.star.util.XSearchDescriptor;
import com.sun.star.util.XSearchable;
import com.sun.star.view.DocumentZoomType;

import de.petanqueturniermanager.helper.Lo;

public class Calc {

	// for headers and footers
	public static final int HF_LEFT = 0;
	public static final int HF_CENTER = 1;
	public static final int HF_RIGHT = 2;

	// for zooming
	public static final short OPTIMAL = 0;
	public static final short PAGE_WIDTH = 1;
	public static final short ENTIRE_PAGE = 2;
	// public static final short BY_VALUE = 3;
	public static final short PAGE_WIDTH_EXACT = 4;

	// for border decoration (bitwise composition is possible)
	public static final int TOP_BORDER = 0x01;
	public static final int BOTTOM_BORDER = 0x02;
	public static final int LEFT_BORDER = 0x04;
	public static final int RIGHT_BORDER = 0x08;

	// largest value used in XCellSeries.fillSeries
	public static final int MAX_VALUE = 0x7FFFFFFF;

	// use a better name when date mode doesn't matter
	public static final FillDateMode NO_DATE = FillDateMode.FILL_DATE_DAY;

	// some hex values for commonly used colors
	public static final int BLACK = 0x000000;
	public static final int WHITE = 0xFFFFFF;

	public static final int RED = 0xFF0000;
	public static final int GREEN = 0x00FF00;
	public static final int BLUE = 0x0000FF;
	public static final int YELLOW = 0xFFFF00;
	public static final int ORANGE = 0xFFA500;

	public static final int DARK_BLUE = 0x003399;
	public static final int LIGHT_BLUE = 0x99CCFF;
	public static final int PALE_BLUE = 0xD6EBFF;

	private static final com.sun.star.awt.Point CELL_POS = new com.sun.star.awt.Point(3, 4);

	// --------------- document methods ------------------

	public static XSpreadsheetDocument openDoc(String fnm, XComponentLoader loader) {
		XComponent doc = LoOrg.openDoc(fnm, loader);
		if (doc == null) {
			System.out.println("Document is null");
			return null;
		}
		return getSSDoc(doc);
	} // end of openDoc()

	public static XSpreadsheetDocument getSSDoc(XComponent doc) {
		if (!Info.isDocType(doc, LoOrg.CALC_SERVICE)) {
			System.out.println("Not a spreadsheet doc; closing");
			LoOrg.closeDoc(doc);
			return null;
		}

		XSpreadsheetDocument ssDoc = Lo.qi(XSpreadsheetDocument.class, doc);
		if (ssDoc == null) {
			System.out.println("Not a spreadsheet doc; closing");
			LoOrg.closeDoc(doc);
			return null;
		}
		return ssDoc;
	} // end of getSSDoc()

	public static XSpreadsheetDocument createDoc(XComponentLoader loader) {
		XComponent doc = LoOrg.createDoc("scalc", loader);
		return Lo.qi(XSpreadsheetDocument.class, doc);
		// XSpreadsheetDocument does not inherit XComponent!
	}

	/*
	 * public static void closeDoc(XSpreadsheetDocument doc) { XCloseable closeable = Lo.qi(XCloseable.class, doc); LoOrg.close(closeable); }
	 * 
	 * 
	 * public static void saveDoc(XSpreadsheetDocument doc, String fnm) { // XStorable store = Lo.qi(XStorable.class, doc); XComponent doc = Lo.qi(XComponent.class, doc); LoOrg.saveDoc(doc, fnm); }
	 */

	// ------------------------ sheet methods -------------------------

	public static XSpreadsheet getSheet(XSpreadsheetDocument doc, int index)
	// return the spreadsheet with the specified index (0-based)
	{
		// System.out.println("Accessing spreadsheet " + index) ;
		XSpreadsheets sheets = doc.getSheets();
		XSpreadsheet sheet = null;
		try {
			XIndexAccess xSheetsIdx = Lo.qi(XIndexAccess.class, sheets);
			// must convert since XSpreadsheet is a named container
			sheet = Lo.qi(XSpreadsheet.class, xSheetsIdx.getByIndex(index));
		} catch (Exception e) {
			System.out.println("Could not access spreadsheet: " + index);
		}
		return sheet;
	} // end of getSheet()

	public static XSpreadsheet getSheet(XSpreadsheetDocument doc, String sheetName)
	// return the spreadsheet by name
	{
		// System.out.println("Accessing spreadsheet \"" + sheetName + "\"") ;
		XSpreadsheets sheets = doc.getSheets();
		XSpreadsheet sheet = null;
		try {
			sheet = Lo.qi(XSpreadsheet.class, sheets.getByName(sheetName));
		} catch (Exception e) {
			System.out.println("Could not access spreadsheet: \"" + sheetName + "\"");
		}
		return sheet;
	} // end of getSheet()

	public static XSpreadsheet insertSheet(XSpreadsheetDocument doc, String name, short idx)
	// Inserts a new empty spreadsheet with the specified name
	{
		XSpreadsheets sheets = doc.getSheets();
		XSpreadsheet sheet = null;
		try {
			sheets.insertNewByName(name, idx);
			sheet = Lo.qi(XSpreadsheet.class, sheets.getByName(name));
		} catch (Exception ex) {
			System.out.println("Could not insert sheet: " + ex);
		}
		return sheet;
	} // end of insertSheet()

	public static boolean removeSheet(XSpreadsheetDocument doc, String name) {
		XSpreadsheets sheets = doc.getSheets();
		try {
			sheets.removeByName(name);
			return true;
		} catch (Exception ex) {
			System.out.println("Could not remove sheet: " + name);
			return false;
		}
	} // end of removeSheet()

	public static boolean moveSheet(XSpreadsheetDocument doc, String name, short idx) {
		XSpreadsheets sheets = doc.getSheets();
		int numSheets = sheets.getElementNames().length;
		if ((idx < 0) || (idx >= numSheets)) {
			System.out.println("Index " + idx + " is out of range");
			return false;
		} else {
			sheets.moveByName(name, idx);
			return true;
		}
	} // end of moveSheet()

	public static String[] getSheetNames(XSpreadsheetDocument doc) {
		XSpreadsheets sheets = doc.getSheets();
		return sheets.getElementNames();
	}

	public static String getSheetName(XSpreadsheet sheet) {
		XNamed xNamed = Lo.qi(XNamed.class, sheet);
		if (xNamed == null) {
			System.out.println("Could not access spreadsheet name");
			return null;
		} else
			return xNamed.getName();
	} // end of getSheetName()

	public static void setSheetName(XSpreadsheet sheet, String name) {
		XNamed xNamed = Lo.qi(XNamed.class, sheet);
		if (xNamed == null)
			System.out.println("Could not access spreadsheet");
		else
			xNamed.setName(name);
	} // end of setSheetName()

	// ----------------- view methods --------------------------

	public static XController getController(XSpreadsheetDocument doc) {
		XModel model = Lo.qi(XModel.class, doc);
		return model.getCurrentController();
	}

	public static void zoomValue(XSpreadsheetDocument doc, int value)
	// value constants are defined at the top of Calc
	{
		XController ctrl = getController(doc);
		Props.setProperty(ctrl, "ZoomType", DocumentZoomType.BY_VALUE);
		Props.setProperty(ctrl, "ZoomValue", (short) value);
		// in SpreadsheetViewSettings
	}

	public static void zoom(XSpreadsheetDocument doc, short type) {
		XController ctrl = getController(doc);
		Props.setProperty(ctrl, "ZoomType", type);
	}

	public static XSpreadsheetView getView(XSpreadsheetDocument doc) {
		return Lo.qi(XSpreadsheetView.class, getController(doc));
	}

	public static void setActiveSheet(XSpreadsheetDocument doc, XSpreadsheet sheet)
	// bring the sheet to the foreground
	{
		XSpreadsheetView ssView = getView(doc);
		ssView.setActiveSheet(sheet);
	} // end of setActiveSheet()

	public static XSpreadsheet getActiveSheet(XSpreadsheetDocument doc) {
		return getView(doc).getActiveSheet();
	}

	public static void freezeRows(XSpreadsheetDocument doc, int numRows) {
		freeze(doc, 0, numRows);
	}

	public static void freezeCols(XSpreadsheetDocument doc, int numCols) {
		freeze(doc, numCols, 0);
	}

	public static void freeze(XSpreadsheetDocument doc, int numCols, int numRows) {
		XViewFreezable xFreeze = Lo.qi(XViewFreezable.class, getController(doc));
		xFreeze.freezeAtPosition(numCols, numRows);
	} // end of freeze()

	public static void gotoCell(XSpreadsheetDocument doc, String cellName) {
		XFrame frame = getController(doc).getFrame();
		gotoCell(frame, cellName);
	}

	public static void gotoCell(XFrame frame, String cellName) {
		LoOrg.dispatchCmd(frame, "GoToCell", Props.makeProps("ToPoint", cellName));
	}

	public static void splitWindow(XSpreadsheetDocument doc, String cellName) {
		XFrame frame = getController(doc).getFrame();

		// XViewSplitable viewSplit = Lo.qi(XViewSplitable.class, getController(doc));
		// viewSplit.splitAtPosition(x*100, y*100);
		// deprecated
		gotoCell(frame, cellName);
		LoOrg.dispatchCmd(frame, "SplitWindow", Props.makeProps("ToPoint", cellName));
	} // end of splitWindow()

	public static CellRangeAddress getSelectedAddr(XSpreadsheetDocument doc) {
		XModel model = Lo.qi(XModel.class, doc);
		return getSelectedAddr(model);
	}

	public static CellRangeAddress getSelectedAddr(XModel model) {
		if (model == null) {
			System.out.println("No document model found");
			return null;
		}
		XCellRangeAddressable ra = Lo.qi(XCellRangeAddressable.class, model.getCurrentSelection());
		if (ra != null)
			return ra.getRangeAddress();
		else {
			System.out.println("No range address found");
			return null;
		}
	} // end of getSelectedAddr()

	public static CellAddress getSelectedCellAddr(XSpreadsheetDocument doc)
	// will return null if a range was selected
	{
		CellRangeAddress crAddr = getSelectedAddr(doc);
		CellAddress addr = null;
		if (Calc.isSingleCellRange(crAddr)) {
			XSpreadsheet sheet = getActiveSheet(doc);
			XCell cell = Calc.getCell(sheet, crAddr.StartColumn, crAddr.StartRow);
			addr = Calc.getCellAddress(cell);
		}
		return addr;
	} // end of getSelectedCellAddr()

	// -------------------- view data methods ---------------------------------

	public static XViewPane[] getViewPanes(XSpreadsheetDocument doc) {
		XIndexAccess con = Lo.qi(XIndexAccess.class, getController(doc));
		if (con == null) {
			System.out.println("Could not access the view pane container");
			return null;
		}
		if (con.getCount() == 0) {
			System.out.println("No view panes found");
			return null;
		}

		// System.out.println("No of panes: " + con.getCount());
		XViewPane[] panes = new XViewPane[con.getCount()];
		for (int i = 0; i < con.getCount(); i++) {
			try {
				panes[i] = Lo.qi(XViewPane.class, con.getByIndex(i));
			} catch (com.sun.star.uno.Exception e) {
				System.out.println("Could not get view pane " + i);
			}
		}
		return panes;
	} // end of getViewPanes()

	public static String getViewData(XSpreadsheetDocument doc) {
		XController ctrl = getController(doc);
		return (String) ctrl.getViewData();
	}

	public static void setViewData(XSpreadsheetDocument doc, String viewData) {
		XController ctrl = getController(doc);
		ctrl.restoreViewData(viewData);
	}

	public static ViewState[] getViewStates(XSpreadsheetDocument doc)
	/*
	 * Extract the view states for all the sheets from the view data. The states are returned as an array of ViewState objects.
	 * 
	 * The view data string has the format: 100/60/0;0;tw:879;0/4998/0/1/0/218/2/0/0/4988/4998
	 * 
	 * The view state info starts after the third ";", the fourth entry. The view state for each sheet is separated by ";"s
	 * 
	 * Based on a post by user Hanya to: https://forum.openoffice.org/en/forum/viewtopic.php? f=45&t=29195&p=133202&hilit=getViewData#p133202
	 */
	{
		XController ctrl = getController(doc);
		String viewData = (String) ctrl.getViewData();
		String[] viewParts = viewData.split(";");
		if (viewParts.length < 4) {
			System.out.println("No sheet view states found in view data");
			return null;
		}
		ViewState[] states = new ViewState[viewParts.length - 3];
		for (int i = 3; i < viewParts.length; i++)
			states[i - 3] = new ViewState(viewParts[i]);
		return states;
	} // end of getViewStates()

	public static void setViewStates(XSpreadsheetDocument doc, ViewState[] states)
	/*
	 * Update the sheet state part of the view data, which starts as the 4th entry in the view data string
	 */
	{
		XController ctrl = getController(doc);
		String viewData = (String) ctrl.getViewData();
		String[] viewParts = viewData.split(";");
		if (viewParts.length < 4) {
			System.out.println("No sheet states found in view data");
			return;
		}
		StringBuilder vdNew = new StringBuilder();
		for (int i = 0; i < 3; i++)
			vdNew.append(viewParts[i]).append(";"); // copy over unchanged

		for (int i = 0; i < states.length; i++) {
			vdNew.append(states[i].toString()); // update states
			if (i != states.length - 1)
				vdNew.append(";");
		}

		// System.out.println("New view data: \"" + vdNew + "\"");
		ctrl.restoreViewData(vdNew.toString());
	} // end of setViewStates()

	// ----------- insert/remove rows, columns, cells ---------------

	public static void insertRow(XSpreadsheet sheet, int idx) {
		XColumnRowRange crRange = Lo.qi(XColumnRowRange.class, sheet);
		XTableRows rows = crRange.getRows();
		rows.insertByIndex(idx, 1); // add 1 row at idx position
	}

	public static void deleteRow(XSpreadsheet sheet, int idx) {
		XColumnRowRange crRange = Lo.qi(XColumnRowRange.class, sheet);
		XTableRows rows = crRange.getRows();
		rows.removeByIndex(idx, 1); // remove 1 row at idx position
	}

	public static void insertColumn(XSpreadsheet sheet, int idx) {
		XColumnRowRange crRange = Lo.qi(XColumnRowRange.class, sheet);
		XTableColumns cols = crRange.getColumns();
		cols.insertByIndex(idx, 1); // add 1 column at idx position
	}

	public static void deleteColumn(XSpreadsheet sheet, int idx) {
		XColumnRowRange crRange = Lo.qi(XColumnRowRange.class, sheet);
		XTableColumns cols = crRange.getColumns();
		cols.removeByIndex(idx, 1); // remove 1 row at idx position
	}

	public static void insertCells(XSpreadsheet sheet, XCellRange cellRange, boolean isShiftRight) {
		XCellRangeMovement mover = Lo.qi(XCellRangeMovement.class, sheet);
		CellRangeAddress addr = getAddress(cellRange);
		if (isShiftRight)
			mover.insertCells(addr, CellInsertMode.RIGHT);
		else // move old cells down
			mover.insertCells(addr, CellInsertMode.DOWN);
	} // end of insertCells()

	public static void deleteCells(XSpreadsheet sheet, XCellRange cellRange, boolean isShiftLeft) {
		XCellRangeMovement mover = Lo.qi(XCellRangeMovement.class, sheet);
		CellRangeAddress addr = getAddress(cellRange);
		if (isShiftLeft)
			mover.removeRange(addr, CellDeleteMode.LEFT);
		else // move old cells up
			mover.removeRange(addr, CellDeleteMode.UP);
	} // end of deleteCells()

	// ----------- set/get values in cells ------------------

	public static void setVal(XSpreadsheet sheet, String cellName, Object value) {
		Point pos = getCellPosition(cellName);
		setVal(sheet, pos.x, pos.y, value); // column, row
	}

	public static void setVal(XSpreadsheet sheet, int column, int row, Object value) {
		XCell cell = getCell(sheet, column, row);
		setVal(cell, value);
	}

	public static void setVal(XCell cell, Object value) {
		if (value instanceof Number)
			cell.setValue(convertToDouble(value));
		else if (value instanceof String)
			cell.setFormula((String) value);
		else
			System.out.println("Value is not a number or string: " + value);
	} // end of setVal()

	public static double convertToDouble(Object val) {
		if (val == null) {
			System.out.println("Value is null; using 0");
			return 0;
		}
		try {
			if (val instanceof Integer)
				return ((Integer) val).intValue();
			else
				return (Double) val;
		} catch (ClassCastException e) {
			System.out.println("Could not convert " + val + " to double; using 0");
			return 0;
		}
	} // end of convertToDouble()

	public static String getTypeString(XCell cell) {
		CellContentType type = cell.getType();
		if (type == CellContentType.EMPTY)
			return "EMPTY";
		else if (type == CellContentType.VALUE)
			return "VALUE";
		else if (type == CellContentType.TEXT)
			return "TEXT";
		else if (type == CellContentType.FORMULA)
			return "FORMULA";
		else {
			System.out.println("Unknown cell type");
			return "??";
		}
	} // end of getTypeString()

	public static Object getVal(XSpreadsheet sheet, CellAddress addr) {
		if (addr == null)
			return null;
		return getVal(sheet, addr.Column, addr.Row);
	} // end of getVal()

	public static Object getVal(XSpreadsheet sheet, String cellName) {
		Point pos = getCellPosition(cellName);
		return getVal(sheet, pos.x, pos.y); // column, row
	} // end of getVal()

	public static Object getVal(XSpreadsheet sheet, int column, int row) {
		XCell xCell = getCell(sheet, column, row);
		return getVal(xCell, column, row);
	} // end of getVal()

	public static Object getVal(XCell cell, int column, int row) {
		CellContentType type = cell.getType();
		if (type == CellContentType.EMPTY)
			return null;
		else if (type == CellContentType.VALUE)
			return new Double(cell.getValue());
		else if ((type == CellContentType.TEXT) || (type == CellContentType.FORMULA))
			return cell.getFormula();
		else {
			System.out.println("Unknown cell type; returning null");
			return null;
		}
	} // end of getVal()

	public static double getNum(XSpreadsheet sheet, String cellName) {
		return convertToDouble(getVal(sheet, cellName));
	}

	public static double getNum(XSpreadsheet sheet, CellAddress addr) {
		return convertToDouble(getVal(sheet, addr));
	}

	public static double getNum(XSpreadsheet sheet, int column, int row) {
		return convertToDouble(getVal(sheet, column, row));
	}

	public static String getString(XSpreadsheet sheet, String cellName) {
		return (String) getVal(sheet, cellName);
	}

	public static String getString(XSpreadsheet sheet, CellAddress addr) {
		return (String) getVal(sheet, addr);
	}

	public static String getString(XSpreadsheet sheet, int column, int row) {
		return (String) getVal(sheet, column, row);
	}

	// ----------- set/get values in 2D array ------------------

	public static void setArray(XSpreadsheet sheet, String name, Object[][] values) {
		if (isCellRangeName(name))
			setArrayRange(sheet, name, values);
		else // is a cell name
			setArrayCell(sheet, name, values);
	} // end of setArray()

	public static void setArrayRange(XSpreadsheet sheet, String rangeName, Object[][] values) {
		XCellRange cellRange = getCellRange(sheet, rangeName);
		setCellRangeArray(cellRange, values);
	} // end of setArrayRange()

	public static void setCellRangeArray(XCellRange cellRange, Object[][] values) {
		XCellRangeData crData = Lo.qi(XCellRangeData.class, cellRange);
		crData.setDataArray(values);
	} // end of setCellRangeArray()

	public static void setArrayCell(XSpreadsheet sheet, String cellName, Object[][] values) {
		Point pos = getCellPosition(cellName);
		int colEnd = pos.x + values[0].length - 1;
		int rowEnd = pos.y + values.length - 1;
		XCellRange cellRange = getCellRange(sheet, pos.x, pos.y, colEnd, rowEnd);
		setCellRangeArray(cellRange, values);
	} // end of setArrayCell()

	public static Object[][] getArray(XSpreadsheet sheet, String rangeName) {
		XCellRange cellRange = getCellRange(sheet, rangeName);
		XCellRangeData crData = Lo.qi(XCellRangeData.class, cellRange);
		return crData.getDataArray();
	} // end of getArray()

	public static Object[][] getCellRangeArray(XCellRange cellRange) {
		XCellRangeData crData = Lo.qi(XCellRangeData.class, cellRange);
		return crData.getDataArray();
	}

	public static void printArray(Object[][] vals) {
		System.out.println("Row x Column size: " + vals.length + " x " + (vals[0].length));
		for (int row = 0; row < vals.length; row++) {
			for (int col = 0; col < vals[row].length; col++)
				System.out.print("  " + vals[row][col]);
			System.out.println();
		}
		System.out.println();
	} // end of printArray()

	public static double[][] getDoublesArray(XSpreadsheet sheet, String rangeName) {
		return convertToDoubles(getArray(sheet, rangeName));
	}

	public static double[][] convertToDoubles(Object[][] vals) {
		int rowSize = vals.length;
		int colSize = vals[0].length; // assuming all columns are this length
		// System.out.println("Row x Column size: " + rowSize + " x " + colSize);
		double[][] doubles = new double[rowSize][colSize];
		for (int row = 0; row < rowSize; row++)
			for (int col = 0; col < colSize; col++)
				doubles[row][col] = convertToDouble(vals[row][col]);
		return doubles;
	} // end of convertToDoubles()

	public static void printArray(double[][] vals)
	// repeated code but for printing doubles array
	{
		System.out.println("Row x Column size: " + vals.length + " x " + (vals[0].length));
		for (int row = 0; row < vals.length; row++) {
			for (int col = 0; col < vals[row].length; col++)
				System.out.print("  " + vals[row][col]);
			System.out.println();
		}
		System.out.println();
	} // end of printArray()

	// ---------- set/get rows and columns -------------------------

	public static void setCol(XSpreadsheet sheet, String cellName, Object[] values) {
		Point pos = getCellPosition(cellName);
		setCol(sheet, pos.x, pos.y, values); // column, row
	}

	public static void setCol(XSpreadsheet sheet, int colStart, int rowStart, Object[] values)
	// add values down a single column starting at (colstart, rowstart)
	{
		XCellRange cellRange = getCellRange(sheet, colStart, rowStart, colStart, rowStart + values.length - 1);
		XCell xCell = null;
		for (int i = 0; i < values.length; i++) {
			xCell = getCell(cellRange, 0, i); // column -- row
			setVal(xCell, values[i]);
		}
	} // end of setCol()

	public static void setRow(XSpreadsheet sheet, String cellName, Object[] values) {
		Point pos = getCellPosition(cellName);
		setRow(sheet, pos.x, pos.y, values); // column, row
	}

	public static void setRow(XSpreadsheet sheet, int colStart, int rowStart, Object[] values)
	// add values along a single row starting at (colstart, rowstart)
	{
		XCellRange cellRange = getCellRange(sheet, colStart, rowStart, colStart + values.length - 1, rowStart);

		XCellRangeData crData = Lo.qi(XCellRangeData.class, cellRange);
		crData.setDataArray(new Object[][] { values }); // 1-row 2D array
	} // end of setRow()

	public static Object[] getRow(XSpreadsheet sheet, String rangeName) {
		Object[][] vals = getArray(sheet, rangeName);
		return extractRow(vals, 0); // assumes user wants 1st row
	} // end of getRow()

	public static Object[] extractRow(Object[][] vals, int rowIdx) {
		int rowSize = vals.length;
		if ((rowIdx < 0) || (rowIdx > rowSize - 1)) {
			System.out.println("Row index out of range");
			return null;
		} else
			return vals[rowIdx];
	} // end of extractRow()

	public static Object[] getCol(XSpreadsheet sheet, String rangeName) {
		Object[][] vals = getArray(sheet, rangeName);
		return extractCol(vals, 0); // assumes user wants 1st column
	} // end of getCol()

	public static Object[] extractCol(Object[][] vals, int colIdx) {
		int rowSize = vals.length;
		int colSize = vals[0].length; // assuming all columns are this length

		if ((colIdx < 0) || (colIdx > colSize - 1)) {
			System.out.println("Column index out of range");
			return null;
		} else {
			Object[] colVals = new Object[rowSize];
			for (int row = 0; row < rowSize; row++)
				colVals[row] = vals[row][colIdx];
			return colVals;
		}
	} // end of extractCol()

	public static double[] convertToDoubles(Object[] vals) {
		int size = vals.length;
		double[] doubles = new double[size];
		for (int i = 0; i < size; i++)
			doubles[i] = convertToDouble(vals[i]);
		return doubles;
	} // end of convertToDoubles()

	// ----------------- special cell types ---------------------

	public static void setDate(XSpreadsheet sheet, String cellName, int day, int month, int year)
	// Writes a date with standard date format into a spreadsheet
	{
		XCell xCell = getCell(sheet, cellName);
		xCell.setFormula(month + "/" + day + "/" + year);

		XNumberFormatsSupplier nfsSupplier = LoOrg.createInstanceMCF(XNumberFormatsSupplier.class,
				"com.sun.star.util.NumberFormatsSupplier");
		XNumberFormats numberFormats = nfsSupplier.getNumberFormats();
		XNumberFormatTypes xFormatTypes = Lo.qi(XNumberFormatTypes.class, numberFormats);

		com.sun.star.lang.Locale aLocale = new com.sun.star.lang.Locale();
		// aLocale.Country = "GB";
		// aLocale.Language = "en";
		int nFormat = xFormatTypes.getStandardFormat(NumberFormat.DATE, aLocale);
		// NumberFormat.DATETIME

		Props.setProperty(xCell, "NumberFormat", nFormat);
	} // end of setDate()

	public static void addAnnotation(XSpreadsheet sheet, String cellName, String msg) {
		// add the annotation
		CellAddress addr = getCellAddress(sheet, cellName);
		XSheetAnnotationsSupplier annsSupp = Lo.qi(XSheetAnnotationsSupplier.class, sheet);
		XSheetAnnotations anns = annsSupp.getAnnotations();
		anns.insertNew(addr, msg);

		// get a reference to the annotation
		XCell xCell = getCell(sheet, cellName);
		XSheetAnnotationAnchor annAnchor = Lo.qi(XSheetAnnotationAnchor.class, xCell);
		XSheetAnnotation ann = annAnchor.getAnnotation();

		ann.setIsVisible(true);
	} // end of addAnnotation()

	// ----------------- get XCell and XCellRange methods ---------------------------

	public static XCell getCell(XSpreadsheet sheet, int column, int row) {
		try {
			return sheet.getCellByPosition(column, row);
		} catch (Exception e) {
			System.out.println("Could not access cell at: " + column + " - " + row);
			return null;
		}
	} // end of getCell()

	public static XCell getCell(XSpreadsheet sheet, CellAddress addr) {
		return getCell(sheet, addr.Column, addr.Row);
	} // not using Sheet value in addr

	public static XCell getCell(XCellRange cellRange, int column, int row) {
		try {
			return cellRange.getCellByPosition(column, row);
		} catch (Exception e) {
			System.out.println("Could not access cell in cellrange at: " + column + " - " + row);
			return null;
		}
	} // end of getCell()

	public static XCell getCell(XSpreadsheet sheet, String cellName) {
		XCellRange cellRange = sheet.getCellRangeByName(cellName);
		return getCell(cellRange, 0, 0);
	}

	public static boolean isCellRangeName(String s) {
		return s.contains(":");
	}

	public static XCellRange getCellRange(XSpreadsheet sheet, CellRangeAddress addr) {
		return getCellRange(sheet, addr.StartColumn, addr.StartRow, addr.EndColumn, addr.EndRow);
	}
	// not using Sheet value in addr

	public static boolean isSingleCellRange(CellRangeAddress addr) {
		return ((addr.StartColumn == addr.EndColumn) && (addr.StartRow == addr.EndRow));
	}

	public static XCellRange getCellRange(XSpreadsheet sheet, int colStart, int rowStart, int colEnd, int rowEnd) {
		try {
			return sheet.getCellRangeByPosition(colStart, rowStart, colEnd, rowEnd);
		} catch (Exception e) {
			System.out.println("Could not access cell range : (" + colStart + ", " + rowStart + ") to (" + colEnd + ", "
					+ rowEnd + ")");
			return null;
		}
	} // end of getCellRange()

	public static XCellRange getCellRange(XSpreadsheet sheet, String rangeName)
	// no need to wrap getCellRangeByName(), but have one anyway
	{
		XCellRange cellRange = sheet.getCellRangeByName(rangeName);
		if (cellRange == null)
			System.out.println("Could not access cell range : \"" + rangeName + "\"");
		return cellRange;
	}

	public static XCellRange findUsedRange(XSpreadsheet sheet, String cellName) {
		XCellRange xRange = getCellRange(sheet, cellName);
		XSheetCellRange cellRange = Lo.qi(XSheetCellRange.class, xRange);
		XSheetCellCursor cursor = sheet.createCursorByRange(cellRange);
		return findUsedCursor(cursor);
	}

	public static XCellRange findUsedRange(XSpreadsheet sheet) {
		XSheetCellCursor cursor = sheet.createCursor();
		return findUsedCursor(cursor);
	}

	public static XCellRange findUsedCursor(XSheetCellCursor cursor) {
		// find the used area
		XUsedAreaCursor uaCursor = Lo.qi(XUsedAreaCursor.class, cursor);
		uaCursor.gotoStartOfUsedArea(false);
		uaCursor.gotoEndOfUsedArea(true);

		XCellRange usedRange = Lo.qi(XCellRange.class, uaCursor);
		// System.out.println("The used area is: " + getRangeStr(usedRange) + "\n");
		return usedRange;
	} // end of findUsedCursor()

	public static XCellRange getColRange(XSpreadsheet sheet, int idx) {
		XColumnRowRange crRange = Lo.qi(XColumnRowRange.class, sheet);
		XTableColumns cols = crRange.getColumns();
		XIndexAccess con = Lo.qi(XIndexAccess.class, cols);
		try {
			return Lo.qi(XCellRange.class, con.getByIndex(idx));
		} catch (Exception e) {
			System.out.println("Could not access range for column position " + idx);
			return null;
		}
	} // end of getColRange()

	public static XCellRange getRowRange(XSpreadsheet sheet, int idx) {
		XColumnRowRange crRange = Lo.qi(XColumnRowRange.class, sheet);
		XTableRows rows = crRange.getRows();
		XIndexAccess con = Lo.qi(XIndexAccess.class, rows);
		try {
			return Lo.qi(XCellRange.class, con.getByIndex(idx));
		} catch (Exception e) {
			System.out.println("Could not access range for row position " + idx);
			return null;
		}
	} // end of getRowRange()

	// ----- convert cell/cellrange names to positions ----------------

	public static Point[] getCellRangePositions(String cellRange) {
		String[] cellNames = cellRange.split(":");
		if (cellNames.length != 2) {
			System.out.println("Cell range not found in " + cellRange);
			return null;
		} else {
			Point startPos = getCellPosition(cellNames[0]);
			Point endPos = getCellPosition(cellNames[1]);
			return new Point[] { startPos, endPos };
		}
	} // end of getCellRangePositions()

	public static Point getCellPosition(String cellName) {
		Pattern p = Pattern.compile("([a-zA-Z]+)([0-9]+)");
		Matcher m = p.matcher(cellName);
		if (m.matches()) {
			// System.out.println(m.group(1) + " - " + m.group(2));
			int nColumn = columnStringToNumber(m.group(1).toUpperCase());
			int nRow = rowStringToNumber(m.group(2));
			// System.out.println("Column position: " + nColumn);
			// System.out.println("Row position: " + nRow);
			return new Point(nColumn, nRow);
		} else {
			System.out.println("No match found");
			return null;
		}
	} // end of getCellPosition()

	public static com.sun.star.awt.Point getCellPos(XSpreadsheet sheet, String cellName) {
		XCell xCell = getCell(sheet, cellName);
		com.sun.star.awt.Point pos = (com.sun.star.awt.Point) Props.getProperty(xCell, "Position");
		if (pos == null) {
			System.out.println("Could not determine position of cell " + cellName);
			pos = CELL_POS;
		}
		return pos;
	} // end of getCellPos();

	public static int columnStringToNumber(String colStr) {
		int col = 0;
		int len = colStr.length();
		for (int i = 0; i < len; i++) {
			int val = colStr.charAt(i) - 'A' + 1;
			col = col * 26 + val;
		}
		return col - 1;
	} // end of columnStringToNumber()

	public static int rowStringToNumber(String rowStr)
	// extract a number or return 0
	{
		int num = 0;
		try {
			num = Integer.parseInt(rowStr) - 1;
		} catch (NumberFormatException ex) {
			System.out.println("Incorrect format for " + rowStr);
		}
		return num;
	} // end of rowStringToNumber()

	// ---------------- get cell and cell range addresses --------------------

	public static CellAddress getCellAddress(XCell cell) {
		XCellAddressable addr = Lo.qi(XCellAddressable.class, cell);
		return addr.getCellAddress();
	} // end of getCellAddress()

	public static CellRangeAddress getAddress(XCellRange cellRange) {
		XCellRangeAddressable addr = Lo.qi(XCellRangeAddressable.class, cellRange);
		return addr.getRangeAddress();
	} // end of getAddress()

	public static CellAddress getCellAddress(XSpreadsheet sheet, String cellName)
	// Create cellAddress and initialize it with the given range
	{
		XCellRange cellRange = sheet.getCellRangeByName(cellName);
		XCell startCell = getCell(cellRange, 0, 0);
		return getCellAddress(startCell);
	} // end of getCellAddress()

	public static CellRangeAddress getAddress(XSpreadsheet sheet, String rangeName) {
		return getAddress(getCellRange(sheet, rangeName));
	}

	public static void printCellAddress(XCell cell) {
		printCellAddress(getCellAddress(cell));
	}

	public static void printCellAddress(CellAddress addr) {
		System.out.println("Cell: Sheet" + (addr.Sheet + 1) + "." + getCellStr(addr));
	}

	public static void printAddress(XCellRange cellRange) {
		printAddress(getAddress(cellRange));
	}

	public static void printAddress(CellRangeAddress crAddr) {
		System.out.println("Range: Sheet" + (crAddr.Sheet + 1) + "." + getCellStr(crAddr.StartColumn, crAddr.StartRow)
				+ ":" + getCellStr(crAddr.EndColumn, crAddr.EndRow));
	} // end of printAddress()

	public static void printAddresses(CellRangeAddress[] crAddrs) {
		System.out.println("No of cellrange addresses: " + crAddrs.length);
		for (CellRangeAddress crAddr : crAddrs)
			printAddress(crAddr);
		System.out.println();
	} // end of printAddresses()

	public static XCellSeries getCellSeries(XSpreadsheet sheet, String rangeName)
	// Returns the XCellSeries interface of a cell range
	{
		XCellRange cellRange = sheet.getCellRangeByName(rangeName);
		return Lo.qi(XCellSeries.class, cellRange);
	}

	public static boolean isEqualAddresses(CellRangeAddress addr1, CellRangeAddress addr2) {
		if ((addr1 == null) || (addr2 == null))
			return false;
		return ((addr1.Sheet == addr2.Sheet) && (addr1.StartColumn == addr2.StartColumn)
				&& (addr1.StartRow == addr2.StartRow) && (addr1.EndColumn == addr2.EndColumn)
				&& (addr1.EndRow == addr2.EndRow));
	} // end of isEqualAddresses()

	public static boolean isEqualAddresses(CellAddress addr1, CellAddress addr2) {
		if ((addr1 == null) || (addr2 == null))
			return false;
		return ((addr1.Sheet == addr2.Sheet) && (addr1.Column == addr2.Column) && (addr1.Row == addr2.Row));
	} // end of isEqualAddresses()

	// ------- convert cell range address to string ------------------

	public static String getRangeStr(XCellRange cellRange, XSpreadsheet sheet) {
		return getRangeStr(getAddress(cellRange), sheet);
	}

	public static String getRangeStr(CellRangeAddress crAddr, XSpreadsheet sheet)
	// Returns the cell range as a string
	// Using the name taken from the sheet works, when Sheet<number> does not
	{
		XNamed xNamed = Lo.qi(XNamed.class, sheet);
		return xNamed.getName() + "." + getRangeStr(crAddr);
	} // end of getRangeStr()

	public static String getRangeStr(XCellRange cellRange) {
		return getRangeStr(getAddress(cellRange));
	}

	public static String getRangeStr(CellRangeAddress crAddr)
	// Returns the cell range as a string
	{
		return getCellStr(crAddr.StartColumn, crAddr.StartRow) + ":" + getCellStr(crAddr.EndColumn, crAddr.EndRow);
	} // end of getRangeStr()

	public static String getRangeStr(int startCol, int startRow, int endCol, int endRow)
	// Returns the cell range as a string
	{
		return getCellStr(startCol, startRow) + ":" + getCellStr(endCol, endRow);
	} // end of getRangeStr()

	public static String getCellStr(XCell cell) {
		return getCellStr(getCellAddress(cell));
	}

	public static String getCellStr(CellAddress addr) {
		return getCellStr(addr.Column, addr.Row);
	}

	public static String getCellStr(int nColumn, int nRow)
	// Returns the text address of the cell
	{
		if ((nColumn < 0) || (nRow < 0)) {
			System.out.println("Cell position is negative; using A1");
			return "A1";
		} else
			return (columnNumberStr(nColumn) + (nRow + 1));
	} // end of getCellStr()

	public static String columnNumberStr(int nColumn)
	/*
	 * Columns are numbered starting at 0 where 0 corresponds to A They run as A-Z, AA-AZ, BA-BZ, ..., IV i.e. convert a base 10 number to base 26.
	 */
	{
		String colStr = "";
		while (nColumn >= 0) {
			colStr += (char) ('A' + nColumn % 26);
			nColumn = nColumn / 26 - 1;
		}
		return colStr;
	} // end of columnNumberStr()

	// --------------------- search -----------------------------

	public static XCellRange[] findAll(XSearchable srch, XSearchDescriptor sd) {
		XIndexAccess con = srch.findAll(sd);
		if (con == null) {
			System.out.println("Match result is null");
			return null;
		}
		if (con.getCount() == 0) {
			System.out.println("No matches found");
			return null;
		}

		XCellRange[] crs = new XCellRange[con.getCount()];
		for (int i = 0; i < con.getCount(); i++) {
			try {
				crs[i] = Lo.qi(XCellRange.class, con.getByIndex(i));
			} catch (Exception e) {
				System.out.println("Could not access match index " + i);
			}
		}
		return crs;
	} // end of findAll()

	// ---------------------------- cell decoration ------------------------

	public static XStyle createCellStyle(XSpreadsheetDocument doc, String styleName) {
		XComponent compDoc = Lo.qi(XComponent.class, doc);
		XNameContainer styleFamilies = Info.getStyleContainer(compDoc, "CellStyles");

		XStyle style = LoOrg.createInstanceMSF(XStyle.class, "com.sun.star.style.CellStyle");
		// "com.sun.star.sheet.TableCellStyle"); // crashes insertByName() ??

		try {
			styleFamilies.insertByName(styleName, style);
			return style;
		} catch (Exception e) {
			System.out.println("Unable to create style: " + styleName);
			return null;
		}
	} // end of createCellStyle()

	public static void changeStyle(XSpreadsheet sheet, String rangeName, String styleName) {
		XCellRange cellRange = Calc.getCellRange(sheet, rangeName);
		Props.setProperty(cellRange, "CellStyle", styleName);
	} // end of changeStyle()

	public static void changeStyle(XSpreadsheet sheet, int x1, int y1, int x2, int y2, String styleName) {
		XCellRange cellRange = Calc.getCellRange(sheet, x1, y1, x2, y2);
		Props.setProperty(cellRange, "CellStyle", styleName);
	} // end of changeStyle()

	public static void addBorder(XSpreadsheet sheet, String rangeName) {
		addBorder(sheet, rangeName, 0);
	} // black

	public static void addBorder(XSpreadsheet sheet, String rangeName, int color)
	// Draw a colored border around the range
	{
		addBorder(sheet, rangeName, Calc.LEFT_BORDER | Calc.RIGHT_BORDER | Calc.TOP_BORDER | Calc.BOTTOM_BORDER, color);
	}

	public static void addBorder(XSpreadsheet sheet, String rangeName, int borderVals, int color)
	/* borderVals is a bitwise combination of border constants */
	{
		BorderLine2 line = new BorderLine2(); // create the border line
		line.Color = color;
		line.InnerLineWidth = line.LineDistance = 0;
		line.OuterLineWidth = 100;

		TableBorder2 border = new TableBorder2();

		if ((borderVals & Calc.TOP_BORDER) == Calc.TOP_BORDER) {
			border.TopLine = line;
			border.IsTopLineValid = true;
		}

		if ((borderVals & Calc.BOTTOM_BORDER) == Calc.BOTTOM_BORDER) {
			border.BottomLine = line;
			border.IsBottomLineValid = true;
		}

		if ((borderVals & Calc.LEFT_BORDER) == Calc.LEFT_BORDER) {
			border.LeftLine = line;
			border.IsLeftLineValid = true;
		}

		if ((borderVals & Calc.RIGHT_BORDER) == Calc.RIGHT_BORDER) {
			border.RightLine = line;
			border.IsRightLineValid = true;
		}

		XCellRange cellRange = sheet.getCellRangeByName(rangeName);
		Props.setProperty(cellRange, "TableBorder2", border);
	} // end of addBorder()

	public static void highlightRange(XSpreadsheet sheet, String rangeName, String headline)
	/*
	 * Draw a colored border around the range and write a headline in the top-left cell of the range.
	 */
	{
		Calc.addBorder(sheet, rangeName, LIGHT_BLUE);

		// color the headline
		CellRangeAddress addr = Calc.getAddress(sheet, rangeName);
		XCellRange headerRange = getCellRange(sheet, addr.StartColumn, addr.StartRow, addr.EndColumn, addr.StartRow);
		Props.setProperty(headerRange, "CellBackColor", LIGHT_BLUE);

		// add headline text to the first cell of the range
		XCell firstCell = getCell(headerRange, 0, 0); // location is relative to range
		setVal(firstCell, headline);
		Props.setProperty(firstCell, "CharColor", DARK_BLUE); // dark blue text
		Props.setProperty(firstCell, "CharWeight", com.sun.star.awt.FontWeight.BOLD);
	} // end of highlightRange()

	public static void setColWidth(XSpreadsheet sheet, int idx, int width)
	// e.g. width is in mm, e.g. 6
	{
		XCellRange cellRange = getColRange(sheet, idx);
		Props.setProperty(cellRange, "Width", width * 100);
	} // end of setColWidth()

	public static void setRowHeight(XSpreadsheet sheet, int idx, int height)
	// e.g. height is in mm, e.g. 6
	{
		XCellRange cellRange = getRowRange(sheet, idx);
		Info.showServices("Cell range for a row", cellRange);
		Props.setProperty(cellRange, "Height", height * 100);
	} // end of setRowHeight()

	// --------------------------- scenarios -------------------------------

	public static void insertScenario(XSpreadsheet sheet, String rangeStr, Object[][] vals, String scenName,
			String comment)
	/*
	 * Inserts a scenario containing one cell range into a sheet and applies the value array
	 */
	{
		// get the cell range with the given address
		XCellRange cellRange = sheet.getCellRangeByName(rangeStr);

		// create the range address sequence
		XCellRangeAddressable addr = Lo.qi(XCellRangeAddressable.class, cellRange);
		CellRangeAddress[] crAddr = new CellRangeAddress[1];
		crAddr[0] = addr.getRangeAddress();

		// create the scenario
		XScenariosSupplier supp = Lo.qi(XScenariosSupplier.class, sheet);
		XScenarios scens = supp.getScenarios();
		scens.addNewByName(scenName, crAddr, comment);

		// insert the values into the range
		XCellRangeData crData = Lo.qi(XCellRangeData.class, cellRange);
		crData.setDataArray(vals);
	} // end of insertScenario()

	public static void applyScenario(XSpreadsheet sheet, String name) {
		try {
			// get the scenario set
			XScenariosSupplier supp = Lo.qi(XScenariosSupplier.class, sheet);
			XScenarios scenarios = supp.getScenarios();

			// get the scenario and activate it
			XScenario scenario = Lo.qi(XScenario.class, scenarios.getByName(name));
			scenario.apply();
		} catch (Exception e) {
			System.out.println("Scenario could not be applied: " + e);
		}
	} // end of applyScenario()

	// ---------------- data pilot methods --------------------------

	public static XDataPilotTables getPilotTables(XSpreadsheet sheet) {
		XDataPilotTablesSupplier dpSupp = Lo.qi(XDataPilotTablesSupplier.class, sheet);
		XDataPilotTables dpTables = dpSupp.getDataPilotTables();
		if (dpTables == null)
			System.out.println("No data pilot tables found");
		return dpTables;
	} // end of getPilotTables()

	public static XDataPilotTable getPilotTable(XDataPilotTables dpTables, String name) {
		// refresh needed for third example using a column field
		try {
			Object oTable = dpTables.getByName(name);
			if (oTable == null) {
				System.out.println("Did not find data pilot table \"" + name + "\"");
				return null;
			} else
				return Lo.qi(XDataPilotTable.class, oTable); // this conversion fails ??
		} catch (Exception e) {
			System.out.println("Pilot table lookup failed for \"" + name + "\": " + e);
			return null;
		}
	} // end of getPilotTable()

	// ------------------ using calc functions --------------------------

	public static double computeFunction(GeneralFunction fn, XCellRange cellRange) {
		try {
			XSheetOperation sheetOp = Lo.qi(XSheetOperation.class, cellRange);
			return sheetOp.computeFunction(fn);
		} catch (Exception e) {
			System.out.println("Compute function failed: " + e);
			return 0;
		}
	} // end of computeFunction()

	public static Object callFun(String funcName, Object arg) {
		return callFun(funcName, new Object[] { arg });
	}

	public static Object callFun(String funcName, Object[] args) {
		try {
			XFunctionAccess fa = LoOrg.createInstanceMCF(XFunctionAccess.class, "com.sun.star.sheet.FunctionAccess");
			return fa.callFunction(funcName, args);
		} catch (Exception e) {
			System.out.println("Could not invoke function \"" + funcName + "\"");
			return null;
		}
	} // end of callFun()

	public static String[] getFunctionNames() {
		XFunctionDescriptions funcsDesc = LoOrg.createInstanceMCF(XFunctionDescriptions.class,
				"com.sun.star.sheet.FunctionDescriptions");
		if (funcsDesc == null) {
			System.out.println("No function descriptions were found");
			return null;
		}

		ArrayList<String> nms = new ArrayList<String>();
		int numFuncs = funcsDesc.getCount();
		for (int i = 0; i < numFuncs; i++) {
			try {
				PropertyValue[] props = (PropertyValue[]) funcsDesc.getByIndex(i);
				for (int p = 0; p < props.length; p++) {
					if (props[p].Name.equals("Name")) {
						nms.add((String) props[p].Value);
						break;
					}
				}
			} catch (Exception e) {
			}
		}
		int numNames = nms.size();
		if (numNames == 0) {
			System.out.println("No function names were found");
			return null;
		}

		String[] nmsArr = new String[numNames];
		nmsArr = nms.toArray(nmsArr);
		Arrays.sort(nmsArr);
		return nmsArr;
	} // end of getFunctionNames()

	public static PropertyValue[] findFunction(String funcNm) {
		XFunctionDescriptions funcsDesc = LoOrg.createInstanceMCF(XFunctionDescriptions.class,
				"com.sun.star.sheet.FunctionDescriptions");
		if (funcsDesc == null) {
			System.out.println("No function descriptions were found");
			return null;
		}

		int numFuncs = funcsDesc.getCount();
		for (int i = 0; i < numFuncs; i++) {
			try {
				PropertyValue[] props = (PropertyValue[]) funcsDesc.getByIndex(i);
				for (int p = 0; p < props.length; p++) {
					if ((props[p].Name.equals("Name")) && (props[p].Value.equals(funcNm)))
						return props;
				}
			} catch (Exception e) {
			}
		}

		System.out.println("Function \"" + funcNm + "\" not found");
		return null;
	} // end of findFunction()

	public static PropertyValue[] findFunction(int idx) {
		XFunctionDescriptions funcsDesc = LoOrg.createInstanceMCF(XFunctionDescriptions.class,
				"com.sun.star.sheet.FunctionDescriptions");
		if (funcsDesc == null) {
			System.out.println("No function descriptions were found");
			return null;
		}

		try {
			return (PropertyValue[]) funcsDesc.getByIndex(idx);
		} catch (Exception e) {
			System.out.println("Could not access function description " + idx);
			return null;
		}
	} // end of findFunction()

	public static void printFunctionInfo(String funcName) {
		PropertyValue[] propVals = findFunction(funcName);
		Props.showProps(funcName, propVals);
		printFunArguments(propVals);
		System.out.println();
	} // end of printFunctionInfo()

	public static void printFunArguments(PropertyValue[] propVals) {
		FunctionArgument[] fargs = (FunctionArgument[]) Props.getValue("Arguments", propVals);
		if (fargs == null) {
			System.out.println("No arguments found");
			return;
		}
		System.out.println("No. of arguments: " + fargs.length);
		for (int i = 0; i < fargs.length; i++)
			printFunArgument(i, fargs[i]);
	} // end of printFunArguments()

	public static void printFunArgument(int i, FunctionArgument fa) {
		System.out.println((i + 1) + ". Argument name: " + fa.Name);
		System.out.println("   Description: \"" + fa.Description + "\"");
		System.out.println("   Is optional?: " + fa.IsOptional + "\n");

	} // end of printFunArgument()

	public static int[] getRecentFunctions() {
		XRecentFunctions recentFuncs = LoOrg.createInstanceMCF(XRecentFunctions.class,
				"com.sun.star.sheet.RecentFunctions");
		if (recentFuncs == null) {
			System.out.println("No recent functions found");
			return null;
		}
		return recentFuncs.getRecentFunctionIds();
	} // end of getRecentFunctions()

	// ------------------------ solver methods --------------------------

	public static double goalSeek(XGoalSeek gs, XSpreadsheet sheet, String xCellName, String formulaCellName,
			double result)
	// find x in formula when it equals result
	{
		CellAddress xPos = Calc.getCellAddress(sheet, xCellName);
		CellAddress formulaPos = Calc.getCellAddress(sheet, formulaCellName);

		GoalResult goalResult = gs.seekGoal(formulaPos, xPos, "" + result);
		if (goalResult.Divergence >= 0.1)
			System.out.println("NO result; divergence: " + goalResult.Divergence);
		return goalResult.Result;
	} // end of goalSeek()

	public static void listSolvers()
	/*
	 * On LO 5 reports: com.sun.star.comp.Calc.CoinMPSolver com.sun.star.comp.Calc.LpsolveSolver com.sun.star.comp.Calc.NLPSolver.DEPSSolverImpl com.sun.star.comp.Calc.NLPSolver.SCOSolverImpl
	 */
	{
		System.out.println("Services offered by the solver:");
		String[] nms = Info.getServiceNames("com.sun.star.sheet.Solver");
		if (nms == null)
			System.out.println("  none");
		else {
			for (String service : nms)
				System.out.println("  " + service);
			System.out.println();
		}
	} // end of listSolvers()

	public static SolverConstraint makeConstraint(XSpreadsheet sheet, String cellName, String op, double d) {
		return makeConstraint(Calc.getCellAddress(sheet, cellName), op, d);
	}

	public static SolverConstraint makeConstraint(CellAddress addr, String op, double d) {
		return makeConstraint(addr, toConstraintOp(op), d);
	}

	public static SolverConstraintOperator toConstraintOp(String op) {
		if (op.equals("=") || op.equals("=="))
			return SolverConstraintOperator.EQUAL;
		else if (op.equals("<=") || op.equals("=<"))
			return SolverConstraintOperator.LESS_EQUAL;
		else if (op.equals(">=") || op.equals("=>"))
			return SolverConstraintOperator.GREATER_EQUAL;
		else {
			System.out.println("Do not recognise op: " + op + "; using EQUAL");
			return SolverConstraintOperator.EQUAL;
		}
	} // end of toConstraintOp()

	public static SolverConstraint makeConstraint(XSpreadsheet sheet, String cellName, SolverConstraintOperator op,
			double d) {
		return makeConstraint(Calc.getCellAddress(sheet, cellName), op, d);
	}

	public static SolverConstraint makeConstraint(CellAddress addr, SolverConstraintOperator op, double d) {
		SolverConstraint sc = new SolverConstraint();
		sc.Left = addr;
		sc.Operator = op;
		sc.Right = d;
		return sc;
	} // end of makeConstraint()

	public static void solverReport(XSolver solver) {
		boolean isSuccessful = solver.getSuccess();
		if (isSuccessful) {
			String cellName = getCellStr(solver.getObjective());
			System.out.println("Solver result: ");
			System.out.printf("  %s == %.4f\n", cellName, solver.getResultValue());
			CellAddress[] addrs = solver.getVariables();
			double[] solns = solver.getSolution();
			System.out.println("Solver variables: ");
			for (int i = 0; i < solns.length; i++) {
				cellName = getCellStr(addrs[i]);
				System.out.printf("  %s == %.4f\n", cellName, solns[i]);
			}
			System.out.println();
		} else
			System.out.println("Solver FAILED");
	} // end of solverReport()

	// ------------------ headers /footers --------------------------

	public static XHeaderFooterContent getHeadFoot(XPropertySet props, String content) {
		return Lo.qi(XHeaderFooterContent.class, Props.getProperty(props, content));
	}

	public static void printHeadFoot(String title, XHeaderFooterContent hfc) {
		XText left = hfc.getLeftText();
		XText center = hfc.getCenterText();
		XText right = hfc.getRightText();
		System.out.println(title + ": \"" + left.getString() + "\" : \"" + center.getString() + "\" : \""
				+ right.getString() + "\"");
	} // end of showHeadFoot()

	public static void setHeadFoot(XHeaderFooterContent hfc, int region, String text) {
		XText xText = getRegion(hfc, region);
		if (xText == null) {
			System.out.println("Could not set text");
			return;
		}
		XTextCursor headerCursor = xText.createTextCursor();
		headerCursor.gotoStart(false);
		headerCursor.gotoEnd(true);
		headerCursor.setString(text);
	} // end of setHeadFoot()

	public static XText getRegion(XHeaderFooterContent hfc, int region) {
		if (hfc == null) {
			System.out.println("Header/footer content is null");
			return null;
		}
		if (region == HF_LEFT)
			return hfc.getLeftText();
		else if (region == HF_CENTER)
			return hfc.getCenterText();
		else if (region == HF_RIGHT)
			return hfc.getRightText();
		else {
			System.out.println("Unknown header/footer region");
			return null;
		}
	} // end of getRegion()

} // end of Calc class
