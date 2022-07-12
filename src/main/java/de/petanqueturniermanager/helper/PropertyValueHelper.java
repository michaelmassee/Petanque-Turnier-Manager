package de.petanqueturniermanager.helper;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.Map;

import com.sun.star.beans.PropertyValue;

/**
 * Erstellung 12.07.2022 / Michael Massee
 */

public class PropertyValueHelper {

	private final Map<String, Object> propsMap;

	private PropertyValueHelper() {
		propsMap = new HashMap<>();
	}

	public static final PropertyValueHelper from() {
		return new PropertyValueHelper();
	}

	public PropertyValueHelper add(String name, Object value) {
		propsMap.put(checkNotNull(name), value);
		return this;
	}

	public PropertyValueHelper clear() {
		propsMap.clear();
		return this;
	}

	public PropertyValue[] propList() {
		return PropertyValueHelper.map2Proplist(propsMap);
	}

	public static final PropertyValue[] map2Proplist(Map<String, Object> propsMap) {
		checkNotNull(propsMap, "parameter propsMap=null");
		PropertyValue[] newProperties = new PropertyValue[propsMap.size()];
		int idx = 0;
		for (Map.Entry<String, Object> element : propsMap.entrySet()) {
			newProperties[idx] = new com.sun.star.beans.PropertyValue();
			newProperties[idx].Name = element.getKey();
			newProperties[idx].Value = element.getValue();
			idx++;
		}
		return newProperties;
	}

}
