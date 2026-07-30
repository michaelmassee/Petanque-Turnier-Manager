/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.whatsapp;

public class WhatsAppBridgeException extends Exception {

	private static final long serialVersionUID = 1L;

	public WhatsAppBridgeException(String message) {
		super(message);
	}

	public WhatsAppBridgeException(String message, Throwable cause) {
		super(message, cause);
	}
}
