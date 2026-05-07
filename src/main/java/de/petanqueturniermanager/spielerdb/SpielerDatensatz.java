package de.petanqueturniermanager.spielerdb;

import org.jspecify.annotations.Nullable;

/**
 * Stammdaten eines Spielers. {@code nr} ist {@code null} für noch nicht
 * persistierte Datensätze. {@code vereinNr} und {@code lizenznr} sind optional.
 */
public record SpielerDatensatz(
        @Nullable Integer nr,
        String vorname,
        String nachname,
        @Nullable Integer vereinNr,
        @Nullable String lizenznr) {

    public static SpielerDatensatz neu(String vorname, String nachname,
            @Nullable Integer vereinNr, @Nullable String lizenznr) {
        return new SpielerDatensatz(null, vorname, nachname, vereinNr, lizenznr);
    }
}
