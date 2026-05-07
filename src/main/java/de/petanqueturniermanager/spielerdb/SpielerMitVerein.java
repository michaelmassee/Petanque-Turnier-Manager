package de.petanqueturniermanager.spielerdb;

import org.jspecify.annotations.Nullable;

/**
 * Anzeige-Record für die Trefferliste der Suche: Spielerfelder denormalisiert
 * mit Vereinsname (aus dem JOIN), um N+1-Queries zu vermeiden.
 */
public record SpielerMitVerein(
        int nr,
        String vorname,
        String nachname,
        @Nullable Integer vereinNr,
        @Nullable String vereinName,
        @Nullable String lizenznr) {

    /** Anzeigeform für die Meldeliste-Übernahme (Default: "Vorname Nachname"). */
    public String spielernameVollstaendig() {
        return vorname + " " + nachname;
    }
}
