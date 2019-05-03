/**
 * Erstellung 03.05.2019 / Michael Massee
 */
package de.petanqueturniermanager.comp;

import static com.google.common.base.Preconditions.checkNotNull;

import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.sheet.XSpreadsheetView;
import com.sun.star.uno.XComponentContext;

/**
 * @author Michael Massee <br>
 * beim start active Dokumenten merken
 */
public class WorkingSpreadsheet {

	private final XComponentContext xContext;
	private final XSpreadsheetDocument workingpreadsheetDocument;
	private final XSpreadsheetView workingSpreadsheetView;

	public WorkingSpreadsheet(XComponentContext xContext) {
		this.xContext = checkNotNull(xContext);
		// Save the current Aktiv Documents
		workingpreadsheetDocument = DocumentHelper.getCurrentSpreadsheetDocument(xContext);
		workingSpreadsheetView = DocumentHelper.getCurrentSpreadsheetView(xContext);
	}

	/**
	 * @return the xContext
	 */
	public XComponentContext getxContext() {
		return xContext;
	}

	/**
	 * @return the currentSpreadsheetDocument
	 */
	public XSpreadsheetDocument getWorkingSpreadsheetDocument() {
		return workingpreadsheetDocument;
	}

	/**
	 * @return the currentSpreadsheetView
	 */
	public XSpreadsheetView getWorkingSpreadsheetView() {
		return workingSpreadsheetView;
	}

}
