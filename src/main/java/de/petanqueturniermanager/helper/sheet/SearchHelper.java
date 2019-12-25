/**
 * Erstellung 20.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.helper.sheet;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.sheet.XCellRangeAddressable;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellRangeAddress;
import com.sun.star.table.XCellRange;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.util.XSearchDescriptor;
import com.sun.star.util.XSearchable;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;

/**
 * @author Michael Massee
 *
 */
public class SearchHelper {

	private static final Logger logger = LogManager.getLogger(SearchHelper.class);
	private final WeakRefHelper<XSpreadsheet> wkRefxSpreadsheet;
	private final RangePosition rangePos;

	private SearchHelper(XSpreadsheet xSpreadsheet, RangePosition rangePos) {
		wkRefxSpreadsheet = new WeakRefHelper<>(checkNotNull(xSpreadsheet));
		checkNotNull(rangePos.getStart());
		checkNotNull(rangePos.getEnde());
		this.rangePos = checkNotNull(rangePos);
	}

	public static SearchHelper from(XSpreadsheet xSpreadsheet, RangePosition rangePos) {
		return new SearchHelper(xSpreadsheet, rangePos);
	}

	public static SearchHelper from(ISheet iSheet, RangePosition rangePos) throws GenerateException {
		return new SearchHelper(checkNotNull(iSheet).getXSpreadSheet(), rangePos);
	}

	public static SearchHelper from(WeakRefHelper<ISheet> sheetWkRef, RangePosition rangePos) throws GenerateException {
		return new SearchHelper(checkNotNull(sheetWkRef).get().getXSpreadSheet(), rangePos);
	}

	/**
	 * suche in der 1 Spalte von Range, nach regExpr.<br>
	 * Achtung: findet teil match in den zellen ! <br>
	 * wenn suche nach werte fuer komplette Zelle dann mit ^ und $
	 *
	 * @param rangePos Range mit Spalte
	 * @return wenn gefunden dann erste treffer, sonnst Null
	 */

	public Position searchNachRegExprInSpalte(String regExpr) {
		checkNotNull(regExpr);

		Position result = null;
		try {
			XSearchable xSearchableFromRange = getXSearchableFromRange(rangePos);
			XSearchDescriptor searchDescriptor = xSearchableFromRange.createSearchDescriptor();
			searchDescriptor.setSearchString(regExpr);
			// properties
			// https://api.libreoffice.org/docs/idl/ref/servicecom_1_1sun_1_1star_1_1util_1_1SearchDescriptor.html
			searchDescriptor.setPropertyValue("SearchBackwards", false);
			searchDescriptor.setPropertyValue("SearchRegularExpression", true);
			result = getRangePositionFromResult(xSearchableFromRange, searchDescriptor);
		} catch (IllegalArgumentException | UnknownPropertyException | PropertyVetoException | WrappedTargetException e) {
			logger.fatal(e);
		}
		return result;
	}

	/**
	 * suche nach der letzte leere Zelle in die 1 Spalte von Range
	 *
	 * @param rangePos Range mit Spalte
	 * @return wenn gefunden dann letzte Zelle, sonnst erste Zelle aus Range
	 */

	public Position searchLastEmptyInSpalte() {
		Position result = searchLastNotEmptyInSpalte();
		if (result != null) {
			result.zeilePlusEins();
		} else {
			result = Position.from(rangePos.getStartSpalte(), rangePos.getStartZeile());
		}
		return result;
	}

	/**
	 * suche nach der letzte nicht leere Zelle in die 1 Spalte von Range
	 *
	 * @param rangePos Range mit Spalte
	 * @return wenn gefunden dann letzte Zelle, sonnst Null
	 */

	public Position searchLastNotEmptyInSpalte() {

		Position result = null;
		try {
			XSearchable xSearchableFromRange = getXSearchableFromRange(rangePos);
			XSearchDescriptor searchDescriptor = xSearchableFromRange.createSearchDescriptor();
			searchDescriptor.setSearchString(".*");
			// properties
			// https://api.libreoffice.org/docs/idl/ref/servicecom_1_1sun_1_1star_1_1util_1_1SearchDescriptor.html
			// searchDescriptor.setPropertyValue("SearchWords", true);
			searchDescriptor.setPropertyValue("SearchBackwards", true); // letzte eintrag suchen
			searchDescriptor.setPropertyValue("SearchRegularExpression", true);
			result = getRangePositionFromResult(xSearchableFromRange, searchDescriptor);

		} catch (IllegalArgumentException | UnknownPropertyException | PropertyVetoException | WrappedTargetException e) {
			logger.fatal(e);
		}
		return result;
	}

	private Position getRangePositionFromResult(XSearchable xSearchableFromRange, XSearchDescriptor searchDescriptor) {
		Position result = null;
		Object findFirstResult = xSearchableFromRange.findFirst(searchDescriptor);
		XCellRange xCellRangeResult = UnoRuntime.queryInterface(XCellRange.class, findFirstResult);
		if (xCellRangeResult != null) {
			XCellRangeAddressable xCellRangeAddressable = UnoRuntime.queryInterface(XCellRangeAddressable.class, xCellRangeResult);
			CellRangeAddress cellRangeAddress = xCellRangeAddressable.getRangeAddress();
			result = Position.from(cellRangeAddress.StartColumn, cellRangeAddress.StartRow);
		}
		return result;
	}

	private XSearchable getXSearchableFromRange(RangePosition rangePos) {
		checkNotNull(rangePos);
		XCellRange xCellRange = RangeHelper.from(wkRefxSpreadsheet.get(), rangePos).getCellRange();
		XSearchable xSearchable = null;
		if (xCellRange != null) {
			xSearchable = UnoRuntime.queryInterface(XSearchable.class, xCellRange);
		}
		return xSearchable;
	}

}
