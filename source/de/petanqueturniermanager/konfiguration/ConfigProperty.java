/**
* Erstellung : 03.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.konfiguration;

import static com.google.common.base.Preconditions.*;

public class ConfigProperty<V> {

	private final ConfigPropertyType type;
	private final String key;
	private V defaultVal;
	private String description;

	private ConfigProperty(ConfigPropertyType type, String key) {
		checkNotNull(type);
		checkNotNull(key);
		this.type = type;
		this.key = key;
	}

	public static <V> ConfigProperty<V> from(ConfigPropertyType type, String key) {
		return new ConfigProperty<V>(type, key);
	}

	public String getKey() {
		return this.key;
	}

	public V getDefaultVal() {
		return this.defaultVal;
	}

	public ConfigProperty<V> setDefaultVal(V defaultVal) {
		this.defaultVal = defaultVal;
		return this;
	}

	public String getDescription() {
		return this.description;
	}

	public ConfigProperty<V> setDescription(String description) {
		this.description = description;
		return this;
	}

	public ConfigPropertyType getType() {
		return this.type;
	}

}
