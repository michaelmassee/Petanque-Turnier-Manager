/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.jspecify.annotations.Nullable;

import de.petanqueturniermanager.basesheet.meldeliste.MeleeAnmeldungZeile;
import de.petanqueturniermanager.spielerdb.MeldelisteSpielerDaten;

/**
 * Teilnahme der Mêlée-Einzelanmeldungen beim Rundenstart. Online gibt es nur Einzelspieler; gespielt wird in
 * den lokal gemischten Teams der Meldeliste. Jeder übernommene Mêlée-Spieler erhält die Teilnahme seines
 * Teams, alle anderen gelten als inaktiv.
 * <p>
 * Die Zuordnung Spieler → Team läuft ausschließlich über den Verweis in „Übernommen“: Er trägt die Team-UUID und
 * zeigt die aktuelle Teamnummer ({@link MeleeAnmeldungZeile#teamNr()}); Namen und Nummer dürfen sich danach
 * ändern. Eine Zeile ohne Verweis (z.&nbsp;B. von Hand markiert) bleibt inaktiv – ein Team wird nicht geraten.
 */
final class MeleeTeilnahme {

    private MeleeTeilnahme() {}

    /**
     * @param meleeZeilen  alle Zeilen der Mêlée-Anmeldung
     * @param aktive       Team-Nummern, die in der Runde spielen
     * @param ausgestiegen Team-Nummern, die ausgesetzt haben
     * @return eine Meldung je Mêlée-Zeile, in Sheet-Reihenfolge
     */
    static List<LokaleOnlineMeldung> ermittle(List<MeleeAnmeldungZeile> meleeZeilen, Set<Integer> aktive,
            Set<Integer> ausgestiegen) {
        return meleeZeilen.stream()
                .map(zeile -> new LokaleOnlineMeldung(zeile.zeile() + 1, teilnahme(zeile, aktive, ausgestiegen),
                        zeile.setzPosition() > 0 ? zeile.setzPosition() : null))
                .toList();
    }

    /**
     * Mêlée-Zeilen (1-basiert) je Team-Nr der Meldeliste, Teams aufsteigend, Spieler in Meldelisten-Reihenfolge (KP-18).
     * Spieler ohne Verweis auf ein Team der Meldeliste fehlen – wie bei der Teilnahme wird nichts geraten.
     */
    static Map<Integer, List<Integer>> zeilenProTeam(List<MeleeAnmeldungZeile> meleeZeilen,
            Map<Integer, List<MeldelisteSpielerDaten>> spielerProTeam) {
        Map<Integer, List<Integer>> ergebnis = new TreeMap<>();
        List<MeleeAnmeldungZeile> uebernommene = meleeZeilen.stream().filter(MeleeAnmeldungZeile::uebernommen).toList();
        for (MeleeAnmeldungZeile zeile : uebernommene) {
            Integer teamNr = teamNr(zeile);
            if (teamNr != null && spielerProTeam.containsKey(teamNr)) {
                ergebnis.computeIfAbsent(teamNr, ignored -> new ArrayList<>()).add(zeile.zeile() + 1);
            }
        }
        Map<Integer, MeleeAnmeldungZeile> zeileNachNr = new HashMap<>();
        uebernommene.forEach(zeile -> zeileNachNr.put(zeile.zeile() + 1, zeile));
        ergebnis.forEach((teamNr, zeilen) -> zeilen.sort(Comparator.comparingInt(
                zeile -> positionInTeam(zeileNachNr.get(zeile), spielerProTeam.get(teamNr)))));
        return ergebnis;
    }

    private static OnlineTeilnahme teilnahme(MeleeAnmeldungZeile zeile, Set<Integer> aktive,
            Set<Integer> ausgestiegen) {
        if (!zeile.uebernommen()) {
            return OnlineTeilnahme.INAKTIV;
        }
        Integer teamNr = teamNr(zeile);
        if (teamNr == null) {
            return OnlineTeilnahme.INAKTIV;
        }
        return OnlineTeilnahme.aus(aktive.contains(teamNr), ausgestiegen.contains(teamNr));
    }

    /** Team über den Verweis in „Übernommen“, {@code null} ohne Verweis. */
    private static @Nullable Integer teamNr(MeleeAnmeldungZeile zeile) {
        return zeile.teamNr() > 0 ? zeile.teamNr() : null;
    }

    /** Stelle des Spielers im Team der Meldeliste (über den Namen), unbekannte Namen ans Ende. */
    private static int positionInTeam(MeleeAnmeldungZeile zeile, List<MeldelisteSpielerDaten> spielerListe) {
        String name = OnlineSpielerName.schluessel(zeile.vorname(), zeile.nachname());
        for (int i = 0; i < spielerListe.size(); i++) {
            MeldelisteSpielerDaten spieler = spielerListe.get(i);
            if (name.equals(OnlineSpielerName.schluessel(spieler.vorname(), spieler.nachname()))) {
                return i;
            }
        }
        return spielerListe.size();
    }
}
