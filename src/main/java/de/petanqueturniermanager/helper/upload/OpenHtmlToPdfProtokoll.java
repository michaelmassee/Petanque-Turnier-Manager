/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.upload;

import java.util.logging.Level;

import com.openhtmltopdf.util.XRLog;

/**
 * Dämpft die Protokollausgabe von OpenHTMLToPDF: Die Bibliothek schreibt bei jedem Rendern
 * INFO-Meldungen (Parser, Ladezeiten, CSS-Matcher) auf die Konsole. Warnungen und Fehler
 * bleiben sichtbar.
 */
final class OpenHtmlToPdfProtokoll {

    private OpenHtmlToPdfProtokoll() {
    }

    /** Setzt alle OpenHTMLToPDF-Logger auf {@link Level#WARNING}. Mehrfacher Aufruf ist unschädlich. */
    static void nurWarnungenUndFehler() {
        XRLog.listRegisteredLoggers().forEach(name -> XRLog.setLevel(name, Level.WARNING));
    }
}
