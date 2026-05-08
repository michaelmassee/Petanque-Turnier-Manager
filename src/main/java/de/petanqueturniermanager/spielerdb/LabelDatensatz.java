package de.petanqueturniermanager.spielerdb;

import org.jspecify.annotations.Nullable;

/**
 * Stammdaten eines Labels (freie Markierung für Spieler, 1:n analog Verein).
 * {@code nr} ist {@code null} für noch nicht persistierte Datensätze.
 */
public record LabelDatensatz(@Nullable Integer nr, String name) {

    public static LabelDatensatz neu(String name) {
        return new LabelDatensatz(null, name);
    }
}
