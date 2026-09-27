/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.live;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Teilt einen „Jeder gegen Jeden“-Spielplan (JGJ, Trip-Tête), der alle Partien einer Gruppe untereinander
 * führt, in Runden: Jedes Team spielt pro Runde genau einmal (ungerade Teamzahl: eine Partie ist das Freilos),
 * eine Runde umfasst also ⌈Teams / 2⌉ aufeinanderfolgende Partien.
 */
final class RundenBloecke {

    private RundenBloecke() {}

    static List<List<LivePartie>> inRundenTeilen(List<LivePartie> partien) {
        Set<Integer> teams = new HashSet<>();
        partien.forEach(partie -> {
            teams.addAll(partie.teamA());
            teams.addAll(partie.teamB());
        });
        int proRunde = Math.max((teams.size() + 1) / 2, 1);
        List<List<LivePartie>> runden = new ArrayList<>();
        for (int start = 0; start < partien.size(); start += proRunde) {
            runden.add(List.copyOf(partien.subList(start, Math.min(start + proRunde, partien.size()))));
        }
        return runden;
    }
}
