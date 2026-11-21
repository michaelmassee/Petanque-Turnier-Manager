/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import org.jspecify.annotations.Nullable;

/**
 * Online gespeicherter Stand einer bestätigten Anmeldung, mit dem der manuelle Abgleich den Check-in-Stand des
 * Dokuments vergleicht.
 *
 * @param teilnahme  Online-Teilnahme ({@link OnlineTeilnahme#apiWert()})
 * @param teamNummer online gespeicherte Nr. der Meldeliste, {@code null} = keine
 */
record OnlineMeldungsStand(String teilnahme, @Nullable Integer teamNummer) {

    /**
     * Ob die lokale Teamnummer online fehlt oder anders lautet. Ohne lokale Nummer gibt es nichts zu melden: PTM-Online
     * behält dann die gespeicherte ({@link PtmOnlineStatusAuftrag.Eintrag#teamNummer()}).
     */
    static boolean teamNummerAbweichend(@Nullable Integer lokal, @Nullable Integer online) {
        return lokal != null && !lokal.equals(online);
    }
}
