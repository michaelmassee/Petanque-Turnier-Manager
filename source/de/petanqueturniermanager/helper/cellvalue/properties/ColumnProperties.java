/**
* Erstellung : 15.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.cellvalue.properties;

import static com.google.common.base.Preconditions.checkNotNull;

// Spalte
public class ColumnProperties extends CommonProperties<ColumnProperties> {

	public static final String ISVISIBLE = "IsVisible";

	ColumnProperties() {
	}

	public static ColumnProperties from() {
		return new ColumnProperties();
	}

	public static ColumnProperties from(String key, Object value) {
		checkNotNull(key);
		checkNotNull(value);
		return ColumnProperties.from().put(key, value);
	}

	public ColumnProperties setWidth(int width) {
		return put(WIDTH, width);
	}

	/**
	 * Achtung: Bug ? Wenn spalten ausgeblendet, funktioniert kein filldown mehr !
	 *
	 * @param visible
	 * @return
	 */

	public ColumnProperties isVisible(boolean visible) {
		return put(ISVISIBLE, visible);
	}

}
