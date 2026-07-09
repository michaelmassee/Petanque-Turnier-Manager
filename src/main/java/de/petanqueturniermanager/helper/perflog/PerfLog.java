package de.petanqueturniermanager.helper.perflog;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.comp.GlobalProperties;

/**
 * Zentrales Gate für Performance-/Zeitmessungs-Logs (Tags {@code [WORKER-TIMING]},
 * {@code [STARTUP-TIMING]}, {@code [SIDEBAR-TIMING]}).
 *
 * <p>Aktivierung über die Property {@code performance.logging} in den
 * {@link GlobalProperties}. Default ist deaktiviert.
 *
 * <p>Der Flag-Wert wird in einem {@code volatile boolean}-Feld gecached, damit
 * Hot-Paths nicht bei jedem Aufruf die Properties-Map lesen. Beim Speichern aus
 * dem Konfig-Dialog wird der Cache via {@link #invalidateCache()} verworfen.
 */
public final class PerfLog {

	private static final Logger logger = LogManager.getLogger(PerfLog.class);

	private static volatile Boolean cached = null;

	private PerfLog() {
	}

	public static boolean isEnabled() {
		Boolean lokal = cached;
		if (lokal != null) {
			return lokal;
		}
		boolean aktuell = false;
		try {
			aktuell = GlobalProperties.get().isPerformanceLogging();
		} catch (Exception e) {
			// GlobalProperties noch nicht initialisierbar (sehr früher Startup) -> aus
			logger.debug("Performance-Logging kann noch nicht aus GlobalProperties gelesen werden.", e);
		}
		cached = aktuell;
		return aktuell;
	}

	public static void invalidateCache() {
		cached = null;
	}

	public static void log(Logger logger, String message) {
		if (isEnabled()) {
			logger.info(message);
		}
	}

	public static void log(Logger logger, String format, Object arg) {
		if (isEnabled()) {
			logger.info(format, arg);
		}
	}

	public static void log(Logger logger, String format, Object arg1, Object arg2) {
		if (isEnabled()) {
			logger.info(format, arg1, arg2);
		}
	}

	public static void log(Logger logger, String format, Object... args) {
		if (isEnabled()) {
			logger.info(format, args);
		}
	}
}
