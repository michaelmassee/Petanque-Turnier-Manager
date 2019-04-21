/**
 * Erstellung 21.04.2019 / Michael Massee
 */
package de.petanqueturniermanager.helper.cellvalue;

import java.util.Map;

import de.petanqueturniermanager.helper.position.Position;

/**
 * @author Michael Massee
 *
 */
public interface ICellValue<V> {

	Position getPos();

	Position getEndPosMerge();

	String getComment();

	Map<? extends String, ? extends Object> getCellProperties();

	Map<? extends String, ? extends Object> getColumnProperties();

	Map<? extends String, ? extends Object> getRowProperties();

	boolean isUeberschreiben();

	V getValue();

}
