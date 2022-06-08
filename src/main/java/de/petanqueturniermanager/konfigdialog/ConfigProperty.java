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
	private boolean inSideBarInfoPanel; // obere Panel, read only felder

	protected ConfigProperty(ConfigPropertyType type, String key) {
		this.type = checkNotNull(type);
		this.key = checkNotNull(key);
		inSideBar = false;
		inSideBarInfoPanel = false;
	}

	public static <V> ConfigProperty<V> from(ConfigPropertyType type, String key) {
		ConfigProperty<V> ret = null;
		switch (type) {
		case INTEGER:
		case COLOR:
			ret = (ConfigProperty<V>) new ConfigProperty<Integer>(type, key);
			break;
		case BOOLEAN:
			ret = (ConfigProperty<V>) new ConfigProperty<Boolean>(type, key);
			break;
		default:
			ret = (ConfigProperty<V>) new ConfigProperty<String>(type, key);
			break;
		}

		return ret;
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

	/**
	 * Übergang, beiden flags werden gesetzt<br>
	 * inSideBarInfoPanel und inSideBar= true
	 *
	 * @return
	 */

	public ConfigProperty<V> inSideBarInfoPanel() {
		this.inSideBarInfoPanel = true;
		this.inSideBar = true;
		return this;
	}

	public final boolean isInSideBarInfoPanel() {
		return inSideBarInfoPanel;
	}

}
