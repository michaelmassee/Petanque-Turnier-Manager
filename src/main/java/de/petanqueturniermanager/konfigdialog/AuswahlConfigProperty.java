/**
 * Erstellung 31.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.konfigdialog;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Michael Massee
 *
 */
public class AuswahlConfigProperty extends ConfigProperty<String> {

	private final List<ComboBoxItem> auswahl;

	/**
	 * @param type
	 * @param key
	 */
	protected AuswahlConfigProperty(String key) {
		super(ConfigPropertyType.STRING, key);
		auswahl = new ArrayList<>();
	}

	public static AuswahlConfigProperty from(String key) {
		return new AuswahlConfigProperty(key);
	}

	public AuswahlConfigProperty addAuswahl(String key, String text) {
		auswahl.add(new ComboBoxItem(key, text));
		return this;
	}

	/**
	 * @return the auswahl
	 */
	public List<ComboBoxItem> getAuswahl() {
		return auswahl;
	}

}
