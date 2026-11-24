/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * Online gespeicherter Stand einer bestätigten Anmeldung, mit dem der manuelle Abgleich den Check-in-Stand des
 * Dokuments vergleicht.
 *
 * @param teilnahme   Online-Teilnahme ({@link OnlineTeilnahme#apiWert()})
 * @param teamNummer  online gespeicherte Nr. der Meldeliste, {@code null} = keine
 * @param setzposition online gespeicherte Setzposition, {@code null} = keine
 */
record OnlineMeldungsStand(String teilnahme, @Nullable Integer teamNummer, @Nullable Integer setzposition) {

    /**
     * Ob die Setzposition online anders lautet. Anders als bei der Teamnummer zählt auch eine lokal fehlende: Ein
     * Teilnahme-Auftrag ohne Setzposition löscht sie online, das Dokument ist Master.
     */
    static boolean setzpositionAbweichend(@Nullable Integer lokal, @Nullable Integer online) {
        return !Objects.equals(lokal, online);
    }

    /**
     * Ob die lokale Teamnummer online fehlt oder anders lautet. Ohne lokale Nummer gibt es nichts zu melden: PTM-Online
     * behält dann die gespeicherte ({@link PtmOnlineStatusAuftrag.Eintrag#teamNummer()}).
     */
    static boolean teamNummerAbweichend(@Nullable Integer lokal, @Nullable Integer online) {
        return lokal != null && !lokal.equals(online);
    }
}
