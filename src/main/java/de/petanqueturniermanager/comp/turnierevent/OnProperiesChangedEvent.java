/**
 * Erstellung 17.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.comp.turnierevent;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.sun.star.sheet.XSpreadsheetDocument;

/**
 * @author Michael Massee
 *
 */
public class OnProperiesChangedEvent implements ITurnierEvent {

	private final XSpreadsheetDocument xSpreadsheetDocument;
	private HashMap<String, List> changed = new HashMap<>();

	public OnProperiesChangedEvent(XSpreadsheetDocument xSpreadsheetDocument) {
		this.xSpreadsheetDocument = checkNotNull(xSpreadsheetDocument);
	}

	public OnProperiesChangedEvent addChanged(String propName, String oldVal, String newVal) {
		changed.put(propName, Arrays.asList(oldVal, newVal));
		return this;
	}

	@Override
	public XSpreadsheetDocument getWorkingSpreadsheetDocument() {
		return xSpreadsheetDocument;
	}

}
