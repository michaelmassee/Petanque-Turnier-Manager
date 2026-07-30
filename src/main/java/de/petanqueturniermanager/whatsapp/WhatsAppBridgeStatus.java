/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.whatsapp;

public record WhatsAppBridgeStatus(String status, String qr) {

	public boolean verbunden() {
		return "ready".equalsIgnoreCase(status) || "connected".equalsIgnoreCase(status);
	}

	public boolean brauchtQrCode() {
		return qr != null && !qr.isBlank();
	}
}
