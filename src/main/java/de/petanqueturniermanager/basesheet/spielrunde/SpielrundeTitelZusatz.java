/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.spielrunde;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Zusatz im Titel einer nicht gewerteten Spielrunde (z.B. „1. Spielrunde – NICHT GEWERTET“).
 * <p>
 * Der Zusatz wird direkt in die Titelzelle geschrieben, damit er auch im HTML-, Webserver-, PDF-
 * und WhatsApp-Export sichtbar ist – dort geht bedingte Formatierung verloren.
 */
final class SpielrundeTitelZusatz {

	private SpielrundeTitelZusatz() {
		// Utility-Klasse – keine Instanzen
	}

	/** Hängt den Zusatz an, sofern er noch nicht am Ende steht. */
	static String mitZusatz(String titel, String zusatz) {
		checkNotNull(titel);
		checkNotNull(zusatz);
		return titel.endsWith(zusatz) ? titel : titel + zusatz;
	}

	/** Entfernt den Zusatz am Ende, sofern vorhanden. */
	static String ohneZusatz(String titel, String zusatz) {
		checkNotNull(titel);
		checkNotNull(zusatz);
		return !zusatz.isEmpty() && titel.endsWith(zusatz) ? titel.substring(0, titel.length() - zusatz.length())
				: titel;
	}
}
