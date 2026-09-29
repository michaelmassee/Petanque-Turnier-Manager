/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.io.IOException;

import de.petanqueturniermanager.helper.i18n.I18n;

/** Anwendertext für fehlgeschlagene PTM-Online-Serveraufrufe – einheitlich für alle Menüaktionen. */
public final class PtmOnlineFehlerText {

    private static final int HTTP_NICHT_AUTORISIERT = 401;

    private PtmOnlineFehlerText() {}

    public static String fuer(IOException e) {
        if (e instanceof PtmOnlineHttpException http && http.getStatusCode() == HTTP_NICHT_AUTORISIERT) {
            return I18n.get("ptmonline.fehler.nicht_freigeschaltet");
        }
        if (e instanceof PtmOnlineHttpException http && http.istBindungAbgeloest()) {
            return I18n.get("ptmonline.fehler.bindung_abgeloest");
        }
        String meldung = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        return I18n.get("ptmonline.fehler.netzwerk", meldung);
    }
}
