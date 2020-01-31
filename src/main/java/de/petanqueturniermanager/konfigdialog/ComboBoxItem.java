/**
 * Erstellung 31.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.konfigdialog;

import static com.google.common.base.Preconditions.checkArgument;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Michael Massee
 *
 */
public class ComboBoxItem {
	private final String key;
	private final String text;

	public ComboBoxItem(String key, String text) {
		checkArgument(StringUtils.isNotEmpty(key));
		checkArgument(StringUtils.isNotEmpty(text));
		this.key = key;
		this.text = text;
	}

	public String getKey() {
		return key;
	}

	public String getText() {
		return text;
	}

}
