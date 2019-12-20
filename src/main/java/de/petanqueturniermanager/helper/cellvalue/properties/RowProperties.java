/**
* Erstellung : 15.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.cellvalue.properties;

import static com.google.common.base.Preconditions.checkNotNull;

// Zeile
public class RowProperties extends CommonProperties<RowProperties> {

	public static final String ISVISIBLE = ColumnProperties.ISVISIBLE;

	RowProperties() {
	}

	public static RowProperties from() {
		return new RowProperties();
	}

	public static RowProperties from(String key, Object value) {
		checkNotNull(key);
		checkNotNull(value);
		return RowProperties.from().put(key, value);
	}

	public RowProperties isVisible(boolean visible) {
		return put(ISVISIBLE, visible);
	}

	public RowProperties setHeight(int height) {
		return put(HEIGHT, height);
	}

}
