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
import com.sun.star.table.XCellRange;
import com.sun.star.uno.UnoRuntime;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.cellvalue.properties.RangeProperties;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;

/**
 * @author Michael Massee
 *
 */
public class RangeHelper extends BaseHelper {

	private static final Logger logger = LogManager.getLogger(RangeHelper.class);
	private final RangePosition rangePos;

	private RangeHelper(ISheet iSheet, RangePosition rangePos) {
		super(iSheet);
		this.rangePos = RangePosition.from(rangePos);
	}

	public static RangeHelper from(ISheet sheet, RangePosition rangePos) {
		return new RangeHelper(sheet, rangePos);
	}

	/**
	 * @param wkRefxSpreadsheet
	 * @param rangePosition
	 */
	public static RangeHelper from(WeakRefHelper<ISheet> wkRefISheet, RangePosition rangePos) {
		return new RangeHelper(checkNotNull(wkRefISheet).get(), rangePos);
	}

	public static RangeHelper from(ISheet sheet, int ersteSpalte, int ersteZeile, int letzteSpalte, int letzteZeile) {
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
			XSheetOperation xSheetOp = UnoRuntime.queryInterface(com.sun.star.sheet.XSheetOperation.class, xRangetoClear);
			xSheetOp.clearContents(CellFlags.ANNOTATION | CellFlags.DATETIME | CellFlags.EDITATTR | CellFlags.FORMATTED | CellFlags.FORMULA | CellFlags.HARDATTR | CellFlags.OBJECTS
					| CellFlags.STRING | CellFlags.STYLES | CellFlags.VALUE);
		}
		return this;
	}

	private XCellRangeData getXCellRangeData() throws GenerateException {
		XCellRangeData xCellRangeData = null;
		try {
			XCellRange xCellRange = getXSpreadSheet().getCellRangeByPosition(rangePos.getStartSpalte(), rangePos.getStartZeile(), rangePos.getEndeSpalte(),
					rangePos.getEndeZeile());
			if (xCellRange != null) {
				xCellRangeData = UnoRuntime.queryInterface(XCellRangeData.class, xCellRange);
			}
		} catch (IndexOutOfBoundsException e) {
			logger.error(e.getMessage(), e);
		}
		return xCellRangeData;
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

	/**
	 * grid in sheet schnell ! mit daten füllen<br>
	 * !! rangeData muss genau mit rangepos übereinstimmen<br>
	 * wenn rangedata leer dann pasiert nichts
	 *
	 * @return null when error
	 * @throws GenerateException fehler wenn die größe nicht übereinstimt
	 */
	public RangeHelper setDataInRange(RangeData rangeData) throws GenerateException {

		XCellRangeData xCellRangeData = getXCellRangeData();
		if (xCellRangeData != null) {
			Object[][] dataArray = rangeData.toDataArray();

			if (dataArray.length == 0 || dataArray[0].length == 0) {
				// keine Daten
				return this;
			}

			// array muss genau mit rangepos übereinstimmen
			if (rangePos.getAnzahlZeilen() != dataArray.length) {
				throw new GenerateException("Anzahl Zeilen stimmen nicht überein. range:" + rangePos.getAnzahlZeilen() + " array:" + dataArray.length);
			}

			if (rangePos.getAnzahlSpalten() != dataArray[0].length) {
				throw new GenerateException("Anzahl Spalten stimmen nicht überein. range:" + rangePos.getAnzahlSpalten() + " array:" + dataArray[0].length);
			}
			xCellRangeData.setDataArray(dataArray);
		}
		return this;
	}

	public XCellRange getCellRange() throws GenerateException {

		checkNotNull(rangePos);

		XCellRange xCellRange = null;

		try {
			xCellRange = getXSpreadSheet().getCellRangeByPosition(rangePos.getStartSpalte(), rangePos.getStartZeile(), rangePos.getEndeSpalte(), rangePos.getEndeZeile());
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
			xCellRangesQuery = UnoRuntime.queryInterface(XCellRangesQuery.class, xCellRange);
		}
		return xCellRangesQuery;
	}

	public RangeHelper setRangeProperties(RangeProperties rangeProp) throws GenerateException {
		XPropertyHelper.from(getCellRange(), getISheet()).setProperties(rangeProp);
		return this;
	}

	public RangeHelper setArrayFormula(String formula) throws GenerateException {
		XCellRange xCellRange = getCellRange();
		XArrayFormulaRange xArrayFormula = UnoRuntime.queryInterface(XArrayFormulaRange.class, xCellRange);
		xArrayFormula.setArrayFormula(formula);
		return this;
	}

	public XPropertySet getPropertySet() throws GenerateException {
		XPropertySet xPropSet = null;
		XCellRange xCellRange = getCellRange();
		if (xCellRange != null) {
			xPropSet = UnoRuntime.queryInterface(XPropertySet.class, xCellRange);
		}
		return xPropSet;
	}

}
