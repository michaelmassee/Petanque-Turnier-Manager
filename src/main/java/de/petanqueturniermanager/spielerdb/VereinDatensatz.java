package de.petanqueturniermanager.spielerdb;

import org.jspecify.annotations.Nullable;

/**
 * Stammdaten eines Vereins. {@code nr} ist {@code null} für noch nicht
 * persistierte Datensätze (Insert-Vorbereitung).
 */
public record VereinDatensatz(@Nullable Integer nr, String name) {

    public static VereinDatensatz neu(String name) {
        return new VereinDatensatz(null, name);
    }
}
