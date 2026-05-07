package de.petanqueturniermanager.spielerdb;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Stammdaten eines Spielers. {@code nr} ist {@code null} für noch nicht
 * persistierte Datensätze. {@code vereinNr} und {@code lizenznr} sind optional;
 * {@code labelNrs} ist die Liste zugewiesener Label-IDs (0..n).
 */
public record SpielerDatensatz(
        @Nullable Integer nr,
        String vorname,
        String nachname,
        @Nullable Integer vereinNr,
        List<Integer> labelNrs,
        @Nullable String lizenznr) {

    public SpielerDatensatz {
        labelNrs = labelNrs == null ? List.of() : List.copyOf(labelNrs);
    }

    public static SpielerDatensatz neu(String vorname, String nachname,
            @Nullable Integer vereinNr, List<Integer> labelNrs, @Nullable String lizenznr) {
        return new SpielerDatensatz(null, vorname, nachname, vereinNr, labelNrs, lizenznr);
    }
}
