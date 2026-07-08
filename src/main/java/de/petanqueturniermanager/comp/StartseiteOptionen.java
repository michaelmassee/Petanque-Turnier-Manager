/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp;

/**
 * Globale Turnier-Startseite-Optionen, die in der LibreOffice-Konfiguration liegen.
 */
record StartseiteOptionen(
		int port,
		boolean aktiv,
		int zoom) {

	StartseiteOptionen {
		port = GlobalProperties.normierePort(port, GlobalProperties.STARTSEITE_DEFAULT_PORT);
	}
}
