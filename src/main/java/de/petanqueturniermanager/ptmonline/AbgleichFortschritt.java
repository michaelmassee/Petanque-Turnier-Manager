/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import de.petanqueturniermanager.exception.GenerateException;

/**
 * Rückmeldung eines PTM-Online-Abgleichs an seinen Aufrufer: Statuszeilen (z.B. für die ProcessBox)
 * und Abbruchpunkte zwischen einzelnen Schritten.
 */
public interface AbgleichFortschritt {

    /** Für Aufrufer ohne Statusanzeige und ohne Abbruchmöglichkeit. */
    AbgleichFortschritt OHNE = new AbgleichFortschritt() {
        @Override
        public void status(String text) {
            // keine Statusanzeige
        }

        @Override
        public void pruefeAbbruch() {
            // nicht abbrechbar
        }
    };

    void status(String text);

    /** Wirft eine {@link GenerateException}, wenn der Anwender den Abgleich abgebrochen hat. */
    void pruefeAbbruch() throws GenerateException;
}
