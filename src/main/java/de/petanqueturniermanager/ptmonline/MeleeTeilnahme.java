/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.petanqueturniermanager.basesheet.meldeliste.MeleeAnmeldungZeile;
import de.petanqueturniermanager.spielerdb.MeldelisteSpielerDaten;

/**
 * Teilnahme der Mêlée-Einzelanmeldungen beim Rundenstart. Online gibt es nur Einzelspieler; gespielt wird in
 * den lokal gemischten Teams der Meldeliste. Jeder übernommene Mêlée-Spieler erhält die Teilnahme seines
 * Teams, alle anderen gelten als inaktiv.
 * <p>
 * Die Zuordnung Spieler → Team läuft über den Namen ({@link OnlineSpielerName}): „Mêlée übernehmen“
 * schreibt die Namen unverändert in die Meldeliste, und die Meldeliste lässt keine doppelten Namen zu.
 * Steht ein Name trotzdem in mehreren Teams, bleibt der Spieler inaktiv, statt geraten zu werden.
 */
final class MeleeTeilnahme {

    private MeleeTeilnahme() {}

    /**
     * @param meleeZeilen    alle Zeilen der Mêlée-Anmeldung
     * @param spielerProTeam Spieler der Meldeliste je Team-Nr
     * @param aktive         Team-Nummern, die in der Runde spielen
     * @param ausgestiegen   Team-Nummern, die ausgesetzt haben
     * @return eine Meldung je Mêlée-Zeile, in Sheet-Reihenfolge
     */
    static List<LokaleOnlineMeldung> ermittle(List<MeleeAnmeldungZeile> meleeZeilen,
            Map<Integer, List<MeldelisteSpielerDaten>> spielerProTeam, Set<Integer> aktive, Set<Integer> ausgestiegen) {
        Map<String, Integer> teamProName = teamProName(spielerProTeam);
        return meleeZeilen.stream()
                .map(zeile -> new LokaleOnlineMeldung(zeile.zeile() + 1,
                        teilnahme(zeile, teamProName, aktive, ausgestiegen),
                        zeile.setzPosition() > 0 ? zeile.setzPosition() : null))
                .toList();
    }

    private static OnlineTeilnahme teilnahme(MeleeAnmeldungZeile zeile, Map<String, Integer> teamProName,
            Set<Integer> aktive, Set<Integer> ausgestiegen) {
        if (!zeile.uebernommen()) {
            return OnlineTeilnahme.INAKTIV;
        }
        Integer teamNr = teamProName.get(OnlineSpielerName.schluessel(zeile.vorname(), zeile.nachname()));
        if (teamNr == null) {
            return OnlineTeilnahme.INAKTIV;
        }
        return OnlineTeilnahme.aus(aktive.contains(teamNr), ausgestiegen.contains(teamNr));
    }

    /** Eindeutige Namen → Team-Nr; mehrdeutige Namen fehlen in der Map. */
    private static Map<String, Integer> teamProName(Map<Integer, List<MeldelisteSpielerDaten>> spielerProTeam) {
        Map<String, Integer> teamProName = new HashMap<>();
        Set<String> mehrdeutig = new HashSet<>();
        spielerProTeam.forEach((teamNr, spielerListe) -> spielerListe.forEach(spieler -> {
            String name = OnlineSpielerName.schluessel(spieler.vorname(), spieler.nachname());
            Integer bisher = teamProName.putIfAbsent(name, teamNr);
            if (bisher != null && !bisher.equals(teamNr)) {
                mehrdeutig.add(name);
            }
        }));
        mehrdeutig.forEach(teamProName::remove);
        return teamProName;
    }
}
