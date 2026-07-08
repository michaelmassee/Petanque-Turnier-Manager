/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp;

/**
 * Globale Composite-Webserver-Views-Optionen, die in der LibreOffice-Konfiguration liegen.
 * {@code eintraegeJson} enthält die komplette Liste der Views inkl. Panels als JSON
 * (siehe {@link GlobalProperties.CompositeViewEintragRoh}).
 */
record CompositeViewsOptionen(
		boolean aktiv,
		String eintraegeJson) {

	CompositeViewsOptionen {
		eintraegeJson = eintraegeJson == null ? "" : eintraegeJson.trim();
	}
}
