/**
 * Erstellung 20.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.helper.sheet;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.sheet.CellFlags;
import com.sun.star.sheet.XCellRangesQuery;
import com.sun.star.sheet.XSheetOperation;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.XCellRange;
import com.sun.star.uno.UnoRuntime;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.position.RangePosition;

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

	public XCellRangesQuery getCellRangesQuery() {
		checkNotNull(rangePos);

		XCellRange xCellRange = null;
		XCellRangesQuery xCellRangesQuery = null;

		xCellRange = getCellRange();
		if (xCellRange != null) {
			xCellRangesQuery = UnoRuntime.queryInterface(com.sun.star.sheet.XCellRangesQuery.class, xCellRange);
		}
		return xCellRangesQuery;
	}

}
