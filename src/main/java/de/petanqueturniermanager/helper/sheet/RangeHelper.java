/**
 * Erstellung 20.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.helper.sheet;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.beans.XPropertySet;
import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.sheet.CellFlags;
import com.sun.star.sheet.XArrayFormulaRange;
import com.sun.star.sheet.XCellRangeData;
import com.sun.star.sheet.XCellRangesQuery;
import com.sun.star.sheet.XSheetOperation;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.table.XCellRange;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.cellvalue.properties.RangeProperties;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;

/**
 * @author Michael Massee
 *
 */
public class RangeHelper {

	private static final Logger logger = LogManager.getLogger(RangeHelper.class);
	private RangePosition rangePos;
	private final XSpreadsheet xSpreadsheet;
	private final XSpreadsheetDocument workingSpreadsheetDocument;

	private RangeHelper(XSpreadsheet xSpreadsheet, XSpreadsheetDocument workingSpreadsheetDocument,
			RangePosition rangePos) {
		this.xSpreadsheet = checkNotNull(xSpreadsheet);
		this.workingSpreadsheetDocument = checkNotNull(workingSpreadsheetDocument);
		this.rangePos = RangePosition.from(rangePos);
	}

	private RangeHelper(ISheet sheet, RangePosition rangePos) throws GenerateException {
		this(sheet.getXSpreadSheet(), sheet.getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), rangePos);
	}

	public static RangeHelper from(XSpreadsheet xSpreadsheet, XSpreadsheetDocument workingSpreadsheetDocument,
			RangePosition rangePos) throws GenerateException {
		return new RangeHelper(xSpreadsheet, workingSpreadsheetDocument, rangePos);
	}

	public static RangeHelper from(XSpreadsheet xSpreadsheet, XSpreadsheetDocument workingSpreadsheetDocument,
			int startSpalte, int startZeile) throws GenerateException {
		return from(xSpreadsheet, workingSpreadsheetDocument, RangePosition.from(startSpalte, startZeile));
	}

	public static RangeHelper from(ISheet sheet, RangePosition rangePos) throws GenerateException {
		return new RangeHelper(sheet, rangePos);
	}

	/**
	 * @param wkRefxSpreadsheet
	 * @param rangePosition
	 * @throws GenerateException
	 */
	public static RangeHelper from(WeakRefHelper<ISheet> wkRefISheet, RangePosition rangePos) throws GenerateException {
		return new RangeHelper(checkNotNull(wkRefISheet).get(), rangePos);
	}

	public static RangeHelper from(ISheet sheet, int ersteSpalte, int ersteZeile, int letzteSpalte, int letzteZeile)
			throws GenerateException {
		return new RangeHelper(sheet, RangePosition.from(ersteSpalte, ersteZeile, letzteSpalte, letzteZeile));
	}

	/**
	 * Alles ! wegputzen
	 *
	 * @return
	 * @throws GenerateException
	 */
	public RangeHelper clearRange() throws GenerateException {
		checkNotNull(rangePos);

		XCellRange xRangetoClear;
		xRangetoClear = getCellRange();
		if (xRangetoClear != null) {
			// --- Sheet operation. ---
			XSheetOperation xSheetOp = Lo.qi(com.sun.star.sheet.XSheetOperation.class, xRangetoClear);
			xSheetOp.clearContents(CellFlags.ANNOTATION | CellFlags.DATETIME | CellFlags.EDITATTR | CellFlags.FORMATTED
					| CellFlags.FORMULA | CellFlags.HARDATTR | CellFlags.OBJECTS | CellFlags.STRING | CellFlags.STYLES
					| CellFlags.VALUE);
		}
		return this;
	}

	private XCellRangeData getXCellRangeData() throws GenerateException {
		XCellRangeData xCellRangeData = null;
		try {
			XCellRange xCellRange = getXSpreadSheet().getCellRangeByPosition(rangePos.getStartSpalte(),
					rangePos.getStartZeile(), rangePos.getEndeSpalte(), rangePos.getEndeZeile());
			if (xCellRange != null) {
				xCellRangeData = Lo.qi(XCellRangeData.class, xCellRange);
			}
		} catch (IndexOutOfBoundsException e) {
			logger.error(e.getMessage(), e);
		}
		return xCellRangeData;
	}

	private XCellRange getXSpreadSheet() {
		return xSpreadsheet;
	}

	/**
	 * @return null when error
	 * @throws GenerateException
	 */
	public RangeData getDataFromRange() throws GenerateException {
		Object[][] data = new Object[][] {};
		XCellRangeData xCellRangeData = getXCellRangeData();
		if (xCellRangeData != null) {
			data = xCellRangeData.getDataArray();
		}
		return new RangeData(data);
	}

	public RangeHelper setDataInRange(Object[][] rangeData, boolean adjustEndPosToDataRange) throws GenerateException {
		return setDataInRange(new RangeData(rangeData), adjustEndPosToDataRange);
	}

	public RangeHelper setDataInRange(RangeData rangeData) throws GenerateException {
		return setDataInRange(rangeData, false);
	}

	/**
	 * grid in sheet schnell ! mit daten füllen<br>
	 * !! wenn autorange = false, rangeData muss genau mit rangepos übereinstimmen<br>
	 * wenn rangedata leer dann pasiert nichts<br>
	 * adjustEndPosToDataRange ist true dan wird der endpos beim setDataInRange an der size der daten angepasst
	 *
	 * @return null when error
	 * @throws GenerateException fehler wenn die größe nicht übereinstimt
	 */
	public RangeHelper setDataInRange(RangeData rangeData, boolean adjustEndPosToDataRange) throws GenerateException {

		// adjust endpos to dataArray size
		if (adjustEndPosToDataRange) {
			this.rangePos = rangeData.getRangePosition(rangePos.getStart());
		}

		XCellRangeData xCellRangeData = getXCellRangeData();
		if (xCellRangeData != null) {
			Object[][] dataArray = rangeData.toDataArray();

			if (dataArray.length == 0 || dataArray[0].length == 0) {
				// keine Daten
				return this;
			}

			// array muss genau mit rangepos übereinstimmen
			if (rangePos.getAnzahlZeilen() != dataArray.length) {
				throw new GenerateException("Anzahl Zeilen stimmen nicht überein. range:" + rangePos.getAnzahlZeilen()
						+ " array:" + dataArray.length);
			}

			if (rangePos.getAnzahlSpalten() != dataArray[0].length) {
				throw new GenerateException("Anzahl Spalten stimmen nicht überein. range:" + rangePos.getAnzahlSpalten()
						+ " array:" + dataArray[0].length);
			}

			xCellRangeData.setDataArray(dataArray);
		}
		return this;
	}

	public XCellRange getCellRange() throws GenerateException {

		checkNotNull(rangePos);

		XCellRange xCellRange = null;

		try {
			xCellRange = getXSpreadSheet().getCellRangeByPosition(rangePos.getStartSpalte(), rangePos.getStartZeile(),
					rangePos.getEndeSpalte(), rangePos.getEndeZeile());
		} catch (IndexOutOfBoundsException e) {
			logger.error(e.getMessage(), e);
		}

		return xCellRange;
	}

	/**
	 * not used
	 *
	 * @return
	 * @throws GenerateException
	 */
	@SuppressWarnings("unused")
	private XCellRangesQuery getCellRangesQuery() throws GenerateException {
		checkNotNull(rangePos);

		XCellRange xCellRange = null;
		XCellRangesQuery xCellRangesQuery = null;

		xCellRange = getCellRange();
		if (xCellRange != null) {
			xCellRangesQuery = Lo.qi(XCellRangesQuery.class, xCellRange);
		}
		return xCellRangesQuery;
	}

	public RangeHelper setRangeProperties(RangeProperties rangeProp) throws GenerateException {
		XPropertyHelper.from(getCellRange(), getWorkingSpreadsheetDocument()).setProperties(rangeProp);
		return this;
	}

	private XSpreadsheetDocument getWorkingSpreadsheetDocument() {
		return workingSpreadsheetDocument;
	}

	public RangeHelper setArrayFormula(String formula) throws GenerateException {
		XCellRange xCellRange = getCellRange();
		XArrayFormulaRange xArrayFormula = Lo.qi(XArrayFormulaRange.class, xCellRange);
		xArrayFormula.setArrayFormula(formula);
		return this;
	}

	public XPropertySet getPropertySet() throws GenerateException {
		XPropertySet xPropSet = null;
		XCellRange xCellRange = getCellRange();
		if (xCellRange != null) {
			xPropSet = Lo.qi(XPropertySet.class, xCellRange);
		}
		return xPropSet;
	}

	public RangePosition getRangePos() {
		return rangePos;
	}

}
