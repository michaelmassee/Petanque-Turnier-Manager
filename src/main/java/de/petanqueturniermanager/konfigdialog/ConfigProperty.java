/**
* Erstellung : 03.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.konfigdialog;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.function.Consumer;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.i18n.I18n;

public class ConfigProperty<V> {

	private final ConfigPropertyType type;
	private final String key;
	private V defaultVal;
	private String description;
	private boolean inSideBar; // uebergang properties nach sidebar
	private boolean inSideBarInfoPanel; // obere Panel, read only felder
	private boolean tabFarbe; // Tab-Farben-Dialog
	private boolean intern; // interner Zustand – nicht in Dialogen anzeigen
	private Consumer<WorkingSpreadsheet> nachSpeichernAktion;

	protected ConfigProperty(ConfigPropertyType type, String key) {
		this.type = checkNotNull(type);
		this.key = checkNotNull(key);
		inSideBar = false;
		inSideBarInfoPanel = false;
		tabFarbe = false;
		intern = false;
	}

	@SuppressWarnings("unchecked")
	public static <V> ConfigProperty<V> from(ConfigPropertyType type, String key) {
		return switch (type) {
			case INTEGER, COLOR -> (ConfigProperty<V>) new ConfigProperty<Integer>(type, key);
			case BOOLEAN -> (ConfigProperty<V>) new ConfigProperty<Boolean>(type, key);
			default -> (ConfigProperty<V>) new ConfigProperty<String>(type, key);
		};
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
		if (this.description == null) return null;
		return I18n.get(this.description);
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

	public ConfigProperty<V> tabFarbe() {
		this.tabFarbe = true;
		return this;
	}

	public final boolean isTabFarbe() {
		return tabFarbe;
	}

	/**
	 * Markiert diese Property als internen Zustandswert, der nicht in
	 * Konfigurationsdialogen angezeigt werden soll.
	 */
	public ConfigProperty<V> intern() {
		this.intern = true;
		return this;
	}

	public final boolean isIntern() {
		return intern;
	}

	public ConfigProperty<V> mitNachSpeichernAktion(Consumer<WorkingSpreadsheet> aktion) {
		this.nachSpeichernAktion = aktion;
		return this;
	}

	public void invokeNachSpeichernAktion(WorkingSpreadsheet ws) {
		if (nachSpeichernAktion != null) {
			nachSpeichernAktion.accept(ws);
		}
	}

}
