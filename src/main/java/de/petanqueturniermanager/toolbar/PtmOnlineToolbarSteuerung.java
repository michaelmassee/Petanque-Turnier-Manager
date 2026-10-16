/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.toolbar;

import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.comp.PtmOnlineAktivierung;

/**
 * Blendet die PTM-Online-Toolbar ({@code Addons_Z6_PtmOnlineToolbar.xcu}) ein, solange ein PTM-Online-Zugang
 * hinterlegt ist ({@link PtmOnlineAktivierung}), sonst aus – wie das Untermenü PTM-Online. Aufruf beim Start und nach
 * einer Änderung des Zugangs, jeweils auf dem LO-Main-Thread.
 */
public final class PtmOnlineToolbarSteuerung {

    static final String PTMONLINE_TOOLBAR_URL =
            "private:resource/toolbar/addon_de.petanqueturniermanager.toolbar.ptmonline";

    private PtmOnlineToolbarSteuerung() {
    }

    public static void aktualisiereInAllenFrames(XComponentContext xContext) {
        boolean aktiv = PtmOnlineAktivierung.istAktiv();
        AddonToolbarSichtbarkeit.setzeInAllenFrames(xContext, PTMONLINE_TOOLBAR_URL, frame -> aktiv);
    }
}
