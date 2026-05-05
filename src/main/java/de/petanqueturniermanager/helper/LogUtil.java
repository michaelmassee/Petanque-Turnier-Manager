package de.petanqueturniermanager.helper;

import org.apache.logging.log4j.Logger;

/**
 * Logging-Hilfsmethoden, die Kontextverlust bei {@code Throwable.getMessage() == null}
 * verhindern (z.B. NPE) und einen leeren Kontext sichtbar markieren.
 */
public final class LogUtil {

	private static final String KONTEXT_FALLBACK = "Kein Kontext";

	private LogUtil() {
	}

	public static void error(Logger logger, String kontext, Throwable t) {
		logger.error("{} | {}", aufbereiteKontext(kontext), aufbereiteException(t), t);
	}

	public static void warn(Logger logger, String kontext, Throwable t) {
		logger.warn("{} | {}", aufbereiteKontext(kontext), aufbereiteException(t), t);
	}

	private static String aufbereiteKontext(String kontext) {
		return (kontext != null && !kontext.isBlank()) ? kontext : KONTEXT_FALLBACK;
	}

	private static String aufbereiteException(Throwable t) {
		return (t.getMessage() != null) ? t.getMessage() : t.getClass().getSimpleName();
	}
}
