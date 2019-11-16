/**
 * Erstellung 21.04.2019 / Michael Massee
 */
package de.petanqueturniermanager.helper.cellvalue;

import de.petanqueturniermanager.helper.cellvalue.properties.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.RowProperties;
import de.petanqueturniermanager.helper.position.Position;

/**
 * @author Michael Massee
 *
 */
public interface ICellValue<V> {

	Position getPos();

	Position getEndPosMerge();

	String getComment();

	CellProperties getCellProperties();

	ColumnProperties getColumnProperties();

	RowProperties getRowProperties();

	boolean isUeberschreiben();

	V getValue();

}
