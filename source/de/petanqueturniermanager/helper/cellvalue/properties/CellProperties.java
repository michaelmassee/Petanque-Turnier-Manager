/**
* Erstellung : 15.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.cellvalue.properties;

import static com.google.common.base.Preconditions.checkNotNull;

public class CellProperties extends CommonProperties<CellProperties> {

	private CellProperties() {
	}

	public static CellProperties from() {
		return new CellProperties();
	}

	public CellProperties setWidth(int width) {
		return put(WIDTH, width);
	}

	public CellProperties setHeight(int height) {
		return put(HEIGHT, height);
	}

	public static CellProperties from(String key, Object value) {
		checkNotNull(key);
		checkNotNull(value);
		return CellProperties.from().put(key, value);
	}

}
