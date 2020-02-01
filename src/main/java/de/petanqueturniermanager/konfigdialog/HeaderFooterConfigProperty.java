/**
 * Erstellung 31.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.konfigdialog;

/**
 * @author Michael Massee
 *
 */
public class HeaderFooterConfigProperty extends ConfigProperty<String> {

	/**
	 * @param type
	 * @param key
	 */
	protected HeaderFooterConfigProperty(String key) {
		super(ConfigPropertyType.STRING, key);
	}

	public static HeaderFooterConfigProperty from(String key) {
		return (HeaderFooterConfigProperty) new HeaderFooterConfigProperty(key).setDefaultVal("");
	}

}
