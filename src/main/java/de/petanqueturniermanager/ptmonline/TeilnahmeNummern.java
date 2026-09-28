/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.util.HashSet;
import java.util.Set;

import de.petanqueturniermanager.spielerdb.MeldelisteSpielerDaten;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel;

/**
 * Team-/Spieler-Nummern der Meldeliste nach Teilnahme, wie sie {@link PtmOnlineSpielrundeSync#lokaleMeldungen}
 * erwartet.
 *
 * @param alle       alle Nummern der Meldeliste
 * @param aktive     Teilmenge: nimmt teil (eingecheckt)
 * @param ausgesetzt Teilmenge: ausgesetzt; weder aktiv noch ausgesetzt gilt als inaktiv
 */
record TeilnahmeNummern(Set<Integer> alle, Set<Integer> aktive, Set<Integer> ausgesetzt) {

    TeilnahmeNummern {
        alle = Set.copyOf(alle);
        aktive = Set.copyOf(aktive);
        ausgesetzt = Set.copyOf(ausgesetzt);
    }

    /** Liest die Aktiv-Spalte der Meldeliste (Supermelee: die des aktiven Spieltags). */
    static TeilnahmeNummern ausAktivSpalte(MeldelisteZiel meldeliste) {
        Set<Integer> alle = new HashSet<>();
        Set<Integer> aktive = new HashSet<>();
        Set<Integer> ausgesetzt = new HashSet<>();
        for (int zeile : meldeliste.leseAlleSpielerRoh().stream().map(MeldelisteSpielerDaten::zeile1Basiert)
                .distinct().toList()) {
            int teamNr = meldeliste.getTeamNrAusZeile(zeile);
            if (teamNr <= 0) {
                continue;
            }
            alle.add(teamNr);
            switch (OnlineTeilnahme.ausAktivWert(meldeliste.getAktivWertAusZeile(zeile))) {
                case AKTIV -> aktive.add(teamNr);
                case AUSGESETZT -> ausgesetzt.add(teamNr);
                case INAKTIV -> { /* weder aktiv noch ausgesetzt */ }
            }
        }
        return new TeilnahmeNummern(alle, aktive, ausgesetzt);
    }
}
