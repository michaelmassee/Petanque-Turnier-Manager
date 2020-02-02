/**
 * Erstellung 17.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.comp.turnierevent;

import static com.google.common.base.Preconditions.checkNotNull;

import com.sun.star.sheet.XSpreadsheetDocument;

/**
 * @author Michael Massee
 *
 */
public class OnProperiesChangedEvent implements ITurnierEvent {

	private final XSpreadsheetDocument xSpreadsheetDocument;

	public OnProperiesChangedEvent(XSpreadsheetDocument xSpreadsheetDocument) {
		this.xSpreadsheetDocument = checkNotNull(xSpreadsheetDocument);
	}

	@Override
	public XSpreadsheetDocument getWorkingSpreadsheetDocument() {
		return xSpreadsheetDocument;
	}

}
