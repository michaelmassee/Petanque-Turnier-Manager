/**
 * Erstellung 26.12.2019 / Michael Massee
 */
package de.petanqueturniermanager.helper.sheet;

import static com.google.common.base.Preconditions.checkNotNull;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;

/**
 * @author Michael Massee
 *
 */
public abstract class BaseHelper {

	private final WeakRefHelper<ISheet> wkRefISheet;

	public BaseHelper(ISheet iSheet) {
		wkRefISheet = new WeakRefHelper<>(checkNotNull(iSheet));
	}

	protected final ISheet getISheet() {
		return wkRefISheet.get();
	}

	protected final SheetHelper getSheetHelper() throws GenerateException {
		return wkRefISheet.get().getSheetHelper();
	}

	protected final XSpreadsheet getXSpreadSheet() throws GenerateException {
		return wkRefISheet.get().getXSpreadSheet();
	}

	protected final WorkingSpreadsheet getWorkingSpreadsheet() {
		return wkRefISheet.get().getWorkingSpreadsheet();
	}

	protected final XSpreadsheetDocument getWorkingSpreadsheetDocument() {
		return getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
	}
}
