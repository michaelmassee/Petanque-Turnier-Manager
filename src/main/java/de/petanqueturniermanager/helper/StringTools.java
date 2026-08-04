/*
 * Erstellung 26.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.helper;

import java.util.Locale;
import java.util.regex.Pattern;

public class StringTools {

	/**
	 * Striktes 24h-Uhrzeitformat {@code HH:MM} (00–23:00–59), zweistellig mit führender Null.
	 * Bewusst fix und nicht lokalisiert (kein AM/PM, keine landesspezifischen Trennzeichen) —
	 * konsistent mit den ODF-Formeln {@code TEXT(...;"HH:MM")}/{@code TIMEVALUE(...)}, die dasselbe
	 * feste Format sprachunabhängig verwenden (siehe Rundenzeitplanung).
	 */
	private static final Pattern UHRZEIT_HH_MM = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$");

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

	/** Siehe {@link #UHRZEIT_HH_MM} für die Format-Definition. {@code null} ist ungültig. */
	public static boolean isValidUhrzeitHhMm(String wert) {
		return wert != null && UHRZEIT_HH_MM.matcher(wert).matches();
	}
}
