/**
 * Erstellung 20.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.helper.sheet;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.beans.PropertyValue;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.TableSortField;
import com.sun.star.table.XCellRange;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.util.XSortable;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.position.RangePosition;

/**
 * @author Michael Massee
 *
 */
public class SortHelper {

	private static final Logger logger = LogManager.getLogger(SortHelper.class);
	private final WeakRefHelper<XSpreadsheet> wkRefxSpreadsheet;
	private final RangePosition rangePosition;

	private int sortSpalte = 1; // 1 = erste spalte
	private boolean aufSteigendSortieren = true;
	private boolean caseSensitive = false;
	private boolean bindFormatsToContent = false;

	private SortHelper(XSpreadsheet xSpreadsheet, RangePosition rangePosition) {
		wkRefxSpreadsheet = new WeakRefHelper<>(checkNotNull(xSpreadsheet));
		this.rangePosition = checkNotNull(rangePosition);
	}

	public static SortHelper from(XSpreadsheet xSpreadsheet, RangePosition rangePosition) {
		return new SortHelper(xSpreadsheet, rangePosition);
	}

	public static SortHelper from(ISheet iSheet, RangePosition rangePosition) throws GenerateException {
		return new SortHelper(checkNotNull(iSheet).getSheet(), rangePosition);
	}

	public static SortHelper from(WeakRefHelper<ISheet> sheetWkRef, RangePosition rangePosition) throws GenerateException {
		return new SortHelper(checkNotNull(sheetWkRef).get().getSheet(), rangePosition);
	}

	/**
	 * default = 1 = erste Spalte
	 *
	 * @param sortSpalte
	 * @return
	 */
	public SortHelper spalteToSort(int sortSpalte) {
		checkArgument(sortSpalte > 0);
		this.sortSpalte = sortSpalte;
		return this;
	}

	/**
	 * ist default
	 *
	 * @return
	 */
	public SortHelper aufSteigendSortieren() {
		aufSteigendSortieren = true;
		return this;
	}

	public SortHelper abSteigendSortieren() {
		aufSteigendSortieren = false;
		return this;
	}

	/**
	 * Groß- / Kleinschreibung beachten, default = false
	 */
	public SortHelper caseSensitive() {
		caseSensitive = true;
		return this;
	}

	/**
	 * specifies if cell formats are moved with the contents they belong to.<br>
	 * Formatierung mit sortieren ? <br>
	 * default = false
	 *
	 * @return
	 */

	public SortHelper bindFormatsToContent() {
		bindFormatsToContent = true;
		return this;
	}

	/**
	 * Nicht Groß- / Kleinschreibung beachten, default = true
	 */
	public SortHelper notCaseSensitive() {
		checkArgument(sortSpalte > 0);
		caseSensitive = false;
		return this;
	}

	public SortHelper doSort() {

		XCellRange xCellRange = RangeHelper.from(wkRefxSpreadsheet, rangePosition).getCellRange();
		if (xCellRange == null) {
			return this;
		}

		XSortable xSortable = UnoRuntime.queryInterface(XSortable.class, xCellRange);

		// Note – The FieldType member, that is used to select textual or numeric sorting in
		// text documents is ignored in the spreadsheet application. In a spreadsheet, a cell
		// always has a known type of text or value, which is used for sorting, with numbers
		// sorted before text cells.

		TableSortField[] aSortFields = new TableSortField[1];
		TableSortField field1 = new TableSortField();
		field1.Field = (sortSpalte - 1); // 0 = erste spalte, nur eine Spalte sortieren
		field1.IsAscending = aufSteigendSortieren;
		field1.IsCaseSensitive = caseSensitive;
		aSortFields[0] = field1;

		PropertyValue[] aSortDesc = new PropertyValue[2];
		PropertyValue propVal = new PropertyValue();
		propVal.Name = "SortFields";
		propVal.Value = aSortFields;
		aSortDesc[0] = propVal;

		// specifies if cell formats are moved with the contents they belong to.
		propVal = new PropertyValue();
		propVal.Name = "BindFormatsToContent";
		propVal.Value = bindFormatsToContent;
		aSortDesc[1] = propVal;

		xSortable.sort(aSortDesc);

		return this;
	}

}
