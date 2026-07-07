/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp;

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
		String logLevel) {

	public PluginOptionen {
		logLevel = logLevel == null ? "" : logLevel.trim().toLowerCase();
	}
}
