/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp;

import java.util.Locale;

/**
 * Globale Plugin-Optionen, die im LibreOffice-Optionsdialog bearbeitet werden.
 */
public record PluginOptionen(
		boolean autosave,
		boolean backup,
		boolean newVersionCheck,
		boolean prozessBoxAutomatischAnzeigen,
		boolean prozessBoxAutomatischSchliessen,
		boolean performanceLogging,
		String logLevel,
		boolean autoUpdateDialogBeimStart) {

	public PluginOptionen {
		logLevel = logLevel == null ? "" : logLevel.trim().toLowerCase(Locale.ROOT);
	}
}
