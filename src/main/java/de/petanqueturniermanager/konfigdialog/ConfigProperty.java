/*
* Erstellung : 03.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.konfigdialog;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.function.Consumer;
import java.util.function.Predicate;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.i18n.I18n;

public class ConfigProperty<V> {

	private final ConfigPropertyType type;
	private final String key;
	private V defaultVal;
	private String description;
	private Object[] descriptionArgs;
	private boolean tabFarbe;    // Tab-Farben-Dialog
	private boolean intern;      // interner Zustand – nicht in Dialogen anzeigen
	private boolean exportKonfig; // Export/Upload-Konfigurationsdialog
	private boolean kompaktesTextfeld; // STRING: schmales Textfeld ohne Textarea-Edit-Button (z.B. Uhrzeit)
	private Predicate<String> validator; // STRING: optionale Format-Validierung (z.B. HH:MM)
	private Consumer<WorkingSpreadsheet> nachSpeichernAktion;

	protected ConfigProperty(ConfigPropertyType type, String key) {
		this.type = checkNotNull(type);
		this.key = checkNotNull(key);
		tabFarbe = false;
		intern = false;
		exportKonfig = false;
	}

	@SuppressWarnings("unchecked")
	public static <V> ConfigProperty<V> from(ConfigPropertyType type, String key) {
		return (ConfigProperty<V>) new ConfigProperty<>(type, key);
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
		return descriptionArgs == null ? I18n.get(this.description) : I18n.get(this.description, descriptionArgs);
	}

	public ConfigProperty<V> setDescription(String description) {
		this.description = description;
		return this;
	}

	public ConfigProperty<V> setDescription(String description, Object... args) {
		this.description = description;
		this.descriptionArgs = args;
		return this;
	}

	public ConfigPropertyType getType() {
		return this.type;
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

	public ConfigProperty<V> exportKonfig() {
		this.exportKonfig = true;
		return this;
	}

	public final boolean isExportKonfig() {
		return exportKonfig;
	}

	/**
	 * Markiert eine {@link ConfigPropertyType#STRING}-Property für die Darstellung als schmales
	 * einzeiliges Textfeld ohne Textarea-Edit-Button (statt der Standard-Darstellung mit Button).
	 */
	public ConfigProperty<V> kompaktesTextfeld() {
		this.kompaktesTextfeld = true;
		return this;
	}

	public final boolean isKompaktesTextfeld() {
		return kompaktesTextfeld;
	}

	/**
	 * Registriert eine Format-Validierung für {@link ConfigPropertyType#STRING}-Properties
	 * (z.B. {@code HH:MM}-Uhrzeitformat). Ungültige Eingaben werden vom Dialog beim Tippen
	 * stillschweigend verworfen (nicht persistiert) statt gemeldet — siehe
	 * {@code SimpleTextConfigElement} für die Begründung (kein Fokus-Listener/MessageBox wegen
	 * Freeze-Risiko bei synchronen modalen Dialogen aus UNO-Event-Callbacks).
	 */
	public ConfigProperty<V> validierung(Predicate<String> validator) {
		this.validator = checkNotNull(validator);
		return this;
	}

	public final Predicate<String> getValidator() {
		return validator;
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
