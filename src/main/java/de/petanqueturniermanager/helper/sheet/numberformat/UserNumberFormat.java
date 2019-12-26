/**
 * Erstellung 26.12.2019 / Michael Massee
 */
package de.petanqueturniermanager.helper.sheet.numberformat;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Michael Massee
 *
 */
public enum UserNumberFormat {
	WOCHEN_TAG("TTT");

	private final String pattern;

	UserNumberFormat(String pattern) {
		this.pattern = checkNotNull(pattern);
	}

	public final String getPattern() {
		return pattern;
	}

}
