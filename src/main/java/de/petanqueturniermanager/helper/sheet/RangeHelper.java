/**
 * Erstellung 20.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.helper.sheet;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.sheet.CellFlags;
import com.sun.star.sheet.XArrayFormulaRange;
import com.sun.star.sheet.XCellRangeData;
import com.sun.star.sheet.XCellRangesQuery;
import com.sun.star.sheet.XSheetOperation;
import com.sun.star.sheet.XSpreadsheet;
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
public class RangeHelper {

	private static final Logger logger = LogManager.getLogger(RangeHelper.class);
	private final WeakRefHelper<XSpreadsheet> wkRefxSpreadsheet;
	private final RangePosition rangePos;

	private RangeHelper(XSpreadsheet xSpreadsheet, RangePosition rangePos) {
		wkRefxSpreadsheet = new WeakRefHelper<>(checkNotNull(xSpreadsheet));
		this.rangePos = checkNotNull(rangePos);
	}

	/**
	 * @param sheet
	 * @param rangePos
	 * @return
	 * @throws GenerateException
	 */
	public static RangeHelper from(ISheet sheet, RangePosition rangePos) throws GenerateException {
		return new RangeHelper(checkNotNull(sheet).getXSpreadSheet(), rangePos);
	}

	public static RangeHelper from(XSpreadsheet xSpreadsheet, RangePosition rangePos) {
		return new RangeHelper(xSpreadsheet, rangePos);
	}

	/**
	 * @param wkRefxSpreadsheet
	 * @param rangePosition
	 */
	public static RangeHelper from(WeakRefHelper<XSpreadsheet> wkRefxSpreadsheet, RangePosition rangePos) {
		return new RangeHelper(checkNotNull(wkRefxSpreadsheet).get(), rangePos);
	}

	/**
	 * Alles ! wegputzen
	 *
	 * @return
	 */
	public RangeHelper clearRange() {
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

	private XCellRangeData getXCellRangeData() {
		XCellRangeData xCellRangeData = null;
		try {
			XCellRange xCellRange = wkRefxSpreadsheet.get().getCellRangeByPosition(rangePos.getStartSpalte(), rangePos.getStartZeile(), rangePos.getEndeSpalte(),
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
	 */
	public RangeData getDataFromRange() {
		Object[][] data = null;
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

	public XCellRange getCellRange() {

		checkNotNull(rangePos);

		XCellRange xCellRange = null;

		try {
			xCellRange = wkRefxSpreadsheet.get().getCellRangeByPosition(rangePos.getStartSpalte(), rangePos.getStartZeile(), rangePos.getEndeSpalte(), rangePos.getEndeZeile());
		} catch (IndexOutOfBoundsException e) {
			logger.error(e.getMessage(), e);
		}

		return xCellRange;
	}

	/**
	 * not used
	 *
	 * @return
	 */
	@SuppressWarnings("unused")
	private XCellRangesQuery getCellRangesQuery() {
		checkNotNull(rangePos);

		XCellRange xCellRange = null;
		XCellRangesQuery xCellRangesQuery = null;

		xCellRange = getCellRange();
		if (xCellRange != null) {
			xCellRangesQuery = UnoRuntime.queryInterface(XCellRangesQuery.class, xCellRange);
		}
		return xCellRangesQuery;
	}

	public RangeHelper setRangeProperties(RangeProperties rangeProp) {
		XPropertySet xPropSet = getPropertySet();
		rangeProp.forEach((key, value) -> {
			setProperty(xPropSet, key, value);
		});
		return this;
	}

	public RangeHelper setArrayFormula(String formula) {
		XCellRange xCellRange = getCellRange();
		XArrayFormulaRange xArrayFormula = UnoRuntime.queryInterface(XArrayFormulaRange.class, xCellRange);
		xArrayFormula.setArrayFormula(formula);
		return this;
	}

	private void setProperty(XPropertySet xPropSet, String key, Object val) {
		checkNotNull(key);
		checkNotNull(val);
		checkNotNull(xPropSet);

		try {
			xPropSet.setPropertyValue(key, val);
		} catch (IllegalArgumentException | UnknownPropertyException | PropertyVetoException | WrappedTargetException e) {
			logger.error("Property '" + key + "' = '" + val + "'\r" + e.getMessage(), e);
		}
	}

	public XPropertySet getPropertySet() {
		XPropertySet xPropSet = null;
		XCellRange xCellRange = getCellRange();
		if (xCellRange != null) {
			xPropSet = UnoRuntime.queryInterface(XPropertySet.class, xCellRange);
		}
		return xPropSet;
	}

}
