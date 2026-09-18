/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.onlinesync;

/**
 * Fehler bei der Kommunikation mit der PTM-Online-REST-API (siehe {@link PtmOnlineApiClient}).
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
