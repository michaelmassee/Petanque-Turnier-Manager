/**
 * Erstellung 20.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.helper.sheet.search;

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
import de.petanqueturniermanager.helper.sheet.WeakRefHelper;

/**
 * @author Michael Massee
 *
 */
public class SheetSearchHelper {

	private static final Logger logger = LogManager.getLogger(SheetSearchHelper.class);
	private final XSpreadsheet xSpreadsheet;

	private SheetSearchHelper(XSpreadsheet xSpreadsheet) {
		this.xSpreadsheet = xSpreadsheet;
	}

	/**
	 * TODO noch baustelle
	 * 
	 * @param sheetWkRef
	 * @return
	 * @throws GenerateException
	 */

	public static SheetSearchHelper from(WeakRefHelper<ISheet> sheetWkRef) throws GenerateException {
		return new SheetSearchHelper(checkNotNull(sheetWkRef).get().getXSpreadSheet());
	}

	/**
	 * suche in der 1 Spalte von Range, nach regExpr.<br>
	 * Achtung: findet teil match in den zellen ! <br>
	 * wenn suche nach werte fuer komplette Zelle dann mit ^ und $<br>
	 * eigentliche String mit Pattern.quote \Qxxxx\E
	 *
	 * @param rangePos Range mit Spalte
	 * @return wenn gefunden dann erste treffer, sonnst Null
	 * @throws GenerateException
	 */

	/**
	 * TODO noch baustelle
	 */

	public Position searchNachRegExpr(String regExpr) throws GenerateException {
		checkNotNull(regExpr);

		Position result = null;
		try {
			XSearchable xSearchableFromSheet = getXSearchableFromSheet();
			XSearchDescriptor searchDescriptor = xSearchableFromSheet.createSearchDescriptor();
			searchDescriptor.setSearchString(regExpr);
			// properties
			// https://api.libreoffice.org/docs/idl/ref/servicecom_1_1sun_1_1star_1_1util_1_1SearchDescriptor.html
			// https://www.openoffice.org/api/docs/common/ref/com/sun/star/util/XSearchable-xref.html
			searchDescriptor.setPropertyValue("SearchBackwards", false);
			searchDescriptor.setPropertyValue("SearchRegularExpression", true);
			result = getRangePositionFromResult(xSearchableFromSheet, searchDescriptor);
		} catch (IllegalArgumentException | UnknownPropertyException | PropertyVetoException
				| WrappedTargetException e) {
			logger.fatal(e);
		}
		return result;
	}

	private XSearchable getXSearchableFromSheet() throws GenerateException {
		return UnoRuntime.queryInterface(XSearchable.class, xSpreadsheet);
	}

	private Position getRangePositionFromResult(XSearchable xSearchableFromRange, XSearchDescriptor searchDescriptor) {
		Position result = null;
		Object findFirstResult = xSearchableFromRange.findFirst(searchDescriptor);
		XCellRange xCellRangeResult = UnoRuntime.queryInterface(XCellRange.class, findFirstResult);
		if (xCellRangeResult != null) {
			XCellRangeAddressable xCellRangeAddressable = UnoRuntime.queryInterface(XCellRangeAddressable.class,
					xCellRangeResult);
			CellRangeAddress cellRangeAddress = xCellRangeAddressable.getRangeAddress();
			result = Position.from(cellRangeAddress.StartColumn, cellRangeAddress.StartRow);
		}
		return result;
	}

}
