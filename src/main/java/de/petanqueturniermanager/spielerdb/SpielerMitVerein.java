package de.petanqueturniermanager.spielerdb;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Anzeige-Record für die Trefferliste der Suche: Spielerfelder denormalisiert
 * mit Vereinsname und Liste der Label-Namen (aus den JOINs), um
 * N+1-Queries zu vermeiden.
 */
public record SpielerMitVerein(
        int nr,
        String vorname,
        String nachname,
        @Nullable Integer vereinNr,
        @Nullable String vereinName,
        List<Integer> labelNrs,
        List<String> labelNamen,
        @Nullable String lizenznr) {

    public SpielerMitVerein {
        labelNrs = labelNrs == null ? List.of() : List.copyOf(labelNrs);
        labelNamen = labelNamen == null ? List.of() : List.copyOf(labelNamen);
    }

    /** Anzeigeform für die Meldeliste-Übernahme (Default: "Vorname Nachname"). */
    public String spielernameVollstaendig() {
        return vorname + " " + nachname;
    }
}
