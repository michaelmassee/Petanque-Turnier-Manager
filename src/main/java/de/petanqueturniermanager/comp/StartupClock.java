/*
 * Erstellung : 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp;

import java.lang.management.ManagementFactory;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Liefert die JVM-Uptime in Millisekunden und loggt das erste Vorkommen eines
 * Startup-Ereignisses (z.B. {@code OnTitleChanged}, erster Menü-Dispatch).
 * <p>
 * Mit den Deltas zur JVM-Uptime lässt sich die komplette Calc-Startup-Kette
 * vermessen: vom Prozess-Start (= JVM-Uptime 0) bis zum ersten User-Click.
 * <p>
 * Eigener Loglevel-Prefix: {@code [STARTUP-TIMING]}, Loglevel {@code info}.
 */
public final class StartupClock {

	private static final Logger logger = LogManager.getLogger(StartupClock.class);
	private static final Set<String> BEREITS_GELOGGT = ConcurrentHashMap.newKeySet();

	private StartupClock() {
	}

	/** JVM-Uptime in Millisekunden seit Prozess-Start. */
	public static long uptimeMs() {
		return ManagementFactory.getRuntimeMXBean().getUptime();
	}

	/**
	 * Loggt das erste Vorkommen eines Startup-Ereignisses mit aktueller
	 * JVM-Uptime. Nachfolgende Aufrufe mit demselben {@code schluessel} sind
	 * No-Ops.
	 */
	public static void logErstesVorkommen(String schluessel, String beschreibung) {
		if (BEREITS_GELOGGT.add(schluessel)) {
			logger.info("[STARTUP-TIMING] {} (erstes Vorkommen) jvm-uptime={} ms",
					beschreibung, uptimeMs());
		}
	}
}
