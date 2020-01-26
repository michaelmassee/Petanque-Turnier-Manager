/**
* Erstellung : 03.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.konfigdialog;

import static com.google.common.base.Preconditions.checkNotNull;

public class ConfigProperty<V> {

	private final ConfigPropertyType type;
	private final String key;
	private V defaultVal;
	private String description;
	private boolean inSideBar; // uebergang properties nach sidebar

	private ConfigProperty(ConfigPropertyType type, String key) {
		this.type = checkNotNull(type);
		this.key = checkNotNull(key);
		inSideBar = false;
	}

	public static <V> ConfigProperty<V> from(ConfigPropertyType type, String key) {
		return new ConfigProperty<>(type, key);
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

	public ConfigProperty<V> inSideBar() {
		this.inSideBar = true;
		return this;
	}

	public final boolean isInSideBar() {
		return inSideBar;
	}

}
