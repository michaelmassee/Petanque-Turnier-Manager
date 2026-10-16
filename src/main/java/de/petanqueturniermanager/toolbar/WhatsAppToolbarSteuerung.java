/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.toolbar;

import com.sun.star.uno.XComponentContext;

/**
 * Blendet die WhatsApp-Toolbar in allen Calc-Fenstern ein (beim Start, auf dem LO-Main-Thread).
 */
public final class WhatsAppToolbarSteuerung {

	static final String WHATSAPP_TOOLBAR_URL =
			"private:resource/toolbar/addon_de.petanqueturniermanager.toolbar.whatsapp";

	private WhatsAppToolbarSteuerung() {
	}

	public static void anzeigenInAllenFrames(XComponentContext xContext) {
		AddonToolbarSichtbarkeit.setzeInAllenFrames(xContext, WHATSAPP_TOOLBAR_URL, frame -> true);
	}
}
