/**
* Erstellung : 15.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.cellvalue.properties;

import static com.google.common.base.Preconditions.checkNotNull;

// Range/ Bereich
public class RangeProperties extends CommonProperties<RangeProperties> {

	RangeProperties() {
	}

	public static RangeProperties from() {
		return new RangeProperties();
	}

	public static RangeProperties from(String key, Object value) {
		checkNotNull(key);
		checkNotNull(value);
		return RangeProperties.from().put(key, value);
	}

	public RangeProperties setWidth(int width) {
		return put(WIDTH, width);
	}

}
