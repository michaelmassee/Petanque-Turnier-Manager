/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.onlinesync.speicher;

import java.util.Optional;

/**
 * Lesbare Angaben einer bestehenden Zuordnung für {@link PtmOnlineSyncSpeicher#setAnzeigen}. Fehlen die
 * Online-Details (Anmeldung online nicht mehr gefunden), bleiben Tarife, Fragen und Roh-Status unverändert.
 */
public record ZuordnungsAnzeige(String lokaleBezeichnung, String onlineStatus, Optional<OnlineDetails> details) {

    /** Lesbare Tarife und Anmeldefragen sowie der unübersetzte Anmeldestatus. */
    public record OnlineDetails(String tarife, String fragen, String rohStatus) {
    }
}
