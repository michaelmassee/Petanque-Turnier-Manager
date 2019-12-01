/**
 * Erstellung 01.12.2019 / Michael Massee
 */
package de.petanqueturniermanager.model;

import de.petanqueturniermanager.helper.sheet.rangedata.RowData;

/**
 * @author Michael Massee
 *
 */
public interface IMeldungen<T> {
	T addNewWennNichtVorhanden(RowData meldungZeile);

}
