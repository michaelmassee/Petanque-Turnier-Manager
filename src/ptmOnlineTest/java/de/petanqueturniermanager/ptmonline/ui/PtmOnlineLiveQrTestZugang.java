/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.ui;

import java.util.Optional;
import java.util.Set;

import de.petanqueturniermanager.spielerdb.MeldelisteZiel;

/**
 * Wie der Menüpunkt „QR-Code Live-Ansicht öffnen“ die Meldung einer Zeile findet – ohne Markierung im Blatt und ohne
 * Browser.
 */
public final class PtmOnlineLiveQrTestZugang {

    private PtmOnlineLiveQrTestZugang() {
    }

    /** Online-ID der Meldung in der 1-basierten Zeile; leer ohne Online-Zuordnung. */
    public static Optional<String> onlineIdDerZeile(MeldelisteZiel ziel, int zeile1Basiert)
            throws MeldelisteZiel.MeldelisteSchreibException {
        return PtmOnlineDispatcher.onlineIdDerZeile(ziel.leseLokaleUuids(Set.of(zeile1Basiert)), zeile1Basiert,
                ziel.leseOnlineIdsProUuid());
    }
}
