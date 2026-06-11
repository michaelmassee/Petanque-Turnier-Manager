/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp;

import java.util.Locale;

/**
 * Verhindert den AWT/AppKit-Deadlock der Extension im LibreOffice-Prozess auf macOS.<br>
 * <br>
 * Jede AWT-/Swing-Berührung (z.B. {@code UIManager.setLookAndFeel} oder {@code java.awt.Desktop})
 * initialisiert das AWT-Toolkit. Auf macOS wartet dessen
 * {@code LWCToolkit.initAppkit} ({@code +[AWTStarter start:]}) darauf, dass der AppKit-Main-Thread den
 * Init-Block ausführt — diesen Thread besitzt aber LibreOffice selbst ({@code NSApplication run}). Der
 * aufrufende UNO-Request-Thread blockiert dadurch für immer, und alle weiteren UNO-Aufrufe stauen sich
 * dahinter (nachgewiesen per nativem sample-Stack im macOS-Smoketest, Workflow-Run 27332838964).<br>
 * <br>
 * Mit {@code java.awt.headless=true} überspringt das JDK die AppKit-Initialisierung komplett
 * (HeadlessToolkit). Swing-Dialoge werfen dann eine {@code HeadlessException} statt zu deadlocken —
 * ein Fail-fast, denn funktioniert haben sie auf macOS im LO-Prozess nie. Auf anderen Plattformen
 * bleibt alles unverändert.
 */
public final class MacAwtHeadlessSchutz {

	private MacAwtHeadlessSchutz() {
		// nur statisch
	}

	/**
	 * Setzt {@code java.awt.headless=true}, wenn die JVM auf macOS läuft und das Property nicht bereits
	 * explizit gesetzt wurde. MUSS vor der ersten AWT-/Swing-Klasseninitialisierung laufen.
	 */
	public static void aktiviereFallsMacOS() {
		boolean istMacOS = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac");
		if (istMacOS && System.getProperty("java.awt.headless") == null) {
			System.setProperty("java.awt.headless", "true");
		}
	}
}
