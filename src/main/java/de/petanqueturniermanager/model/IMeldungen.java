/**
 * Erstellung 01.12.2019 / Michael Massee
 */
package de.petanqueturniermanager.model;

import java.util.List;

import de.petanqueturniermanager.helper.sheet.rangedata.RowData;

/**
 * @author Michael Massee
 *
 */
public interface IMeldungen<T, M> {
	T addNewWennNichtVorhanden(RowData meldungZeile);

	List<IMeldung<M>> getMeldungen();

	public int size();

}
