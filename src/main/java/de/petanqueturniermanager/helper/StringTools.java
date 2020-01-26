/**
 * Erstellung 26.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.helper;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Michael Massee
 *
 */
public class StringTools {

	public static String booleanToString(boolean booleanProp) {
		if (booleanProp) {
			return "J";
		}
		return "N";
	}

	public static boolean stringToBoolean(String booleanProp) {
		if (StringUtils.isBlank(booleanProp)) {
			return false;
		}

		if (StringUtils.equalsIgnoreCase(booleanProp, "true")) {
			return true;
		}

		if (StringUtils.equalsIgnoreCase(booleanProp, "false")) {
			return false;
		}

		if (StringUtils.containsIgnoreCase(booleanProp, "N")) {
			return false;
		}
		return true;
	}

}
