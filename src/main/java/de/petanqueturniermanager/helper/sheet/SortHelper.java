/**
 * Erstellung 20.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.helper.sheet;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

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

	// private static final Logger logger = LogManager.getLogger(SortHelper.class);
	private final WeakRefHelper<XSpreadsheet> wkRefxSpreadsheet;
	private final RangePosition rangePositionToSort;

	// private int sortSpalte = 0; // 0 = erste spalte
	private boolean aufSteigendSortieren = true;
	private boolean caseSensitive = false;
	private boolean bindFormatsToContent = false;
	private int[] sortSpalten = new int[] { 0 }; // default erste spalte

	private SortHelper(XSpreadsheet xSpreadsheet, RangePosition rangePosition) {
		wkRefxSpreadsheet = new WeakRefHelper<>(checkNotNull(xSpreadsheet));
		rangePositionToSort = checkNotNull(rangePosition);
	}

	public static SortHelper from(XSpreadsheet xSpreadsheet, RangePosition rangePosition) {
		return new SortHelper(xSpreadsheet, rangePosition);
	}

	public static SortHelper from(ISheet iSheet, RangePosition rangePosition) throws GenerateException {
		return new SortHelper(checkNotNull(iSheet).getXSpreadSheet(), rangePosition);
	}

	public static SortHelper from(WeakRefHelper<ISheet> sheetWkRef, RangePosition rangePosition) throws GenerateException {
		return new SortHelper(checkNotNull(sheetWkRef).get().getXSpreadSheet(), rangePosition);
	}

	/**
	 * default = 0 = erste Spalte
	 *
	 * @param sortSpalte
	 * @return
	 */
	public SortHelper spalteToSort(int sortSpalte) {
		checkArgument(sortSpalte > -1);
		sortSpalten = new int[] { sortSpalte };
		return this;
	}

	/**
	 * default = 0 = erste Spalte
	 *
	 * @param sortSpalte
	 * @return
	 */
	public SortHelper spaltenToSort(int[] sortSpalten) {
		checkArgument(sortSpalten.length > 0);
		this.sortSpalten = sortSpalten;
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

	public SortHelper aufSteigendSortieren(boolean ja) {
		aufSteigendSortieren = ja;
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
		caseSensitive = false;
		return this;
	}

	public SortHelper doSort() {
		checkNotNull(sortSpalten);
		checkArgument(sortSpalten.length > 0);

		XCellRange xCellRangeToSort = RangeHelper.from(wkRefxSpreadsheet, rangePositionToSort).getCellRange();
		if (xCellRangeToSort == null) {
			return this;
		}

		XSortable xSortable = UnoRuntime.queryInterface(XSortable.class, xCellRangeToSort);

		// Note – The FieldType member, that is used to select textual or numeric sorting in
		// text documents is ignored in the spreadsheet application. In a spreadsheet, a cell
		// always has a known type of text or value, which is used for sorting, with numbers
		// sorted before text cells.

		TableSortField[] sortFields = new TableSortField[sortSpalten.length];

		for (int sortSpalteIdx = 0; sortSpalteIdx < sortSpalten.length; sortSpalteIdx++) {
			TableSortField field1 = new TableSortField();
			field1.Field = sortSpalten[sortSpalteIdx]; // 0 = erste spalte
			field1.IsAscending = aufSteigendSortieren;
			field1.IsCaseSensitive = caseSensitive;
			sortFields[sortSpalteIdx] = field1;
		}

		PropertyValue[] aSortDesc = new PropertyValue[2];
		PropertyValue propVal = new PropertyValue();
		propVal.Name = "SortFields";
		propVal.Value = sortFields;
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
