/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.onlinesync;

/**
 * Signalisiert einen Fehler bei der Kommunikation mit PTM Online (Netzwerk, HTTP-Fehlerstatus,
 * ungültige Konfiguration oder ein nicht auswertbares JSON-Antwortformat).
 */
public class PtmOnlineException extends Exception {

	private static final long serialVersionUID = 1L;

	public PtmOnlineException(String message) {
		super(message);
	}

	public PtmOnlineException(String message, Throwable cause) {
		super(message, cause);
	}
}
