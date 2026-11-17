/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.whatsapp;

/**
 * Die laufende Bridge kennt einen Endpunkt nicht (HTTP 404): Sie stammt aus einer älteren Plugin-Version und wurde
 * nach einem Update noch nicht neu gestartet.
 */
public class WhatsAppBridgeEndpunktFehltException extends WhatsAppBridgeException {

	private static final long serialVersionUID = 1L;

	public WhatsAppBridgeEndpunktFehltException(String message) {
		super(message);
	}
}
