/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp;

/**
 * Globale Webserver-Regie-Optionen, die in der LibreOffice-Konfiguration liegen.
 */
record WebserverRegieOptionen(
		boolean aktiv,
		int port,
		String zieleJson) {

	WebserverRegieOptionen {
		zieleJson = zieleJson == null ? "" : zieleJson.trim();
	}
}
