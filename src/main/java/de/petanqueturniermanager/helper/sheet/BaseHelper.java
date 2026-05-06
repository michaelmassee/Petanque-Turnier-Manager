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

	// Strong-Ref: BaseHelper-Subklassen sind kurzlebige Operations-Helper.
	// Der ISheet (i.d.R. SheetRunner) wird ohnehin extern strong gehalten.
	private final ISheet iSheet;

	protected BaseHelper(ISheet iSheet) {
		this.iSheet = checkNotNull(iSheet);
	}

	protected final ISheet getISheet() {
		return iSheet;
	}

	protected final SheetHelper getSheetHelper() throws GenerateException {
		return iSheet.getSheetHelper();
	}

	protected final XSpreadsheet getXSpreadSheet() throws GenerateException {
		return iSheet.getXSpreadSheet();
	}

	protected final WorkingSpreadsheet getWorkingSpreadsheet() {
		return iSheet.getWorkingSpreadsheet();
	}

	protected final XSpreadsheetDocument getWorkingSpreadsheetDocument() {
		return getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
	}
}
