/*
 * Erstellung 26.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.helper;

import java.util.Locale;

public class StringTools {

	private StringTools() {
	}

	public static String booleanToString(boolean booleanProp) {
		return booleanProp ? "J" : "N";
	}

	public static boolean stringToBoolean(String booleanProp) {
		if (containsIgnoreCase(booleanProp, "J")) {
			return true;
		}
		return Boolean.parseBoolean(booleanProp);
	}

	/** Null-sicher: beide null → true; sonst {@code a.equalsIgnoreCase(b)}. */
	public static boolean equalsIgnoreCase(String a, String b) {
		if (a == null) {
			return b == null;
		}
		if (b == null) {
			return false;
		}
		return a.equalsIgnoreCase(b);
	}

	/** Null-sicher: liefert {@code false}, wenn einer der Werte null ist. */
	public static boolean containsIgnoreCase(String haystack, String needle) {
		if (haystack == null || needle == null) {
			return false;
		}
		return haystack.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
	}

	/** Apache-kompatibel: hängt das Suffix nur an, wenn es nicht bereits vorhanden ist. {@code null} bleibt {@code null}. */
	public static String appendIfMissing(String text, String suffix) {
		if (text == null || suffix == null || suffix.isEmpty() || text.endsWith(suffix)) {
			return text;
		}
		return text + suffix;
	}
}
