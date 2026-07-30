/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.toolbar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.beans.XPropertySet;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XFramesSupplier;
import com.sun.star.frame.XLayoutManager;
import com.sun.star.sheet.XSpreadsheetView;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.comp.DocumentHelper;
import de.petanqueturniermanager.helper.Lo;

/**
 * Blendet die WhatsApp-Toolbar ein.
 */
public final class WhatsAppToolbarSteuerung {

	private static final Logger logger = LogManager.getLogger(WhatsAppToolbarSteuerung.class);

	static final String WHATSAPP_TOOLBAR_URL =
			"private:resource/toolbar/addon_de.petanqueturniermanager.toolbar.whatsapp";

	private final XComponentContext xContext;

	public WhatsAppToolbarSteuerung(XComponentContext xContext) {
		this.xContext = xContext;
	}

	public static void anzeigenInAllenFrames(XComponentContext xContext) {
		new WhatsAppToolbarSteuerung(xContext).zeigeToolbarInAllenFrames();
	}

	private void zeigeToolbarInAllenFrames() {
		try {
			var xDesktop = DocumentHelper.getCurrentDesktop(xContext);
			if (xDesktop == null) {
				return;
			}
			var xFramesSupplier = Lo.qi(XFramesSupplier.class, xDesktop);
			if (xFramesSupplier == null) {
				return;
			}
			var xFrames = xFramesSupplier.getFrames();
			if (xFrames == null) {
				return;
			}
			for (int i = 0; i < xFrames.getCount(); i++) {
				try {
					zeigeToolbarInFrame(Lo.qi(XFrame.class, xFrames.getByIndex(i)));
				} catch (Exception e) {
					logger.error("Fehler beim Einblenden der WhatsApp-Toolbar in Frame {}", i, e);
				}
			}
		} catch (Exception e) {
			logger.error("Fehler beim Durchsuchen aller Frames für WhatsApp-Toolbar", e);
		}
	}

	private void zeigeToolbarInFrame(XFrame xFrame) {
		if (xFrame == null) {
			return;
		}
		try {
			if (Lo.qi(XSpreadsheetView.class, xFrame.getController()) == null) {
				return;
			}
			var xFrameProps = Lo.qi(XPropertySet.class, xFrame);
			if (xFrameProps == null) {
				return;
			}
			var xLayoutManager = Lo.qi(XLayoutManager.class, xFrameProps.getPropertyValue("LayoutManager"));
			if (xLayoutManager == null) {
				return;
			}
			xLayoutManager.requestElement(WHATSAPP_TOOLBAR_URL);
			xLayoutManager.showElement(WHATSAPP_TOOLBAR_URL);
			logger.debug("zeigeToolbarInFrame '{}': WhatsApp-Toolbar eingeblendet", xFrame.getName());
		} catch (Exception e) {
			logger.error("Fehler beim Einblenden der WhatsApp-Toolbar in Frame '{}'", xFrame.getName(), e);
		}
	}
}
