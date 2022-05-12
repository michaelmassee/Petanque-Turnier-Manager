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
		return booleanProp ? "J" : "N";
	}

	public static boolean stringToBoolean(String booleanProp) {
		if (StringUtils.containsIgnoreCase(booleanProp, "J")) {
			return true;
		}
		return Boolean.parseBoolean(booleanProp);
	}

}
