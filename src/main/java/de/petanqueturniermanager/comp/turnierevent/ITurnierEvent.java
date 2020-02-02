/**
 * Erstellung 17.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.comp.turnierevent;

import com.sun.star.sheet.XSpreadsheetDocument;

/**
 * @author Michael Massee
 *
 */
public interface ITurnierEvent {

	XSpreadsheetDocument getWorkingSpreadsheetDocument(); // von welchen Document wurde der Event ausgelöst

}
