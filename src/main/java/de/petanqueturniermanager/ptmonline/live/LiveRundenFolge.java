/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.live;

import java.util.ArrayList;
import java.util.List;

/**
 * Setzt die Runden eines Turniers für die Live-Ansicht zusammen und nummeriert sie fortlaufend ab 1. Phasen
 * (z.&nbsp;B. Vorrunde, danach KO) folgen aufeinander; gleichzeitig gespielte Gruppen oder Turnierbäume werden
 * Runde für Runde zu einer gemeinsamen Online-Runde zusammengeführt.
 */
final class LiveRundenFolge {

    private final List<List<LivePartie>> runden = new ArrayList<>();

    /** Hängt eine Phase an; jede innere Liste ist eine Runde. */
    LiveRundenFolge anhaengen(List<List<LivePartie>> phase) {
        phase.forEach(runde -> runden.add(new ArrayList<>(runde)));
        return this;
    }

    /** Hängt gleichzeitig gespielte Phasen (Gruppen, Turnierbäume) als gemeinsame Runden an. */
    LiveRundenFolge parallelAnhaengen(List<List<List<LivePartie>>> phasen) {
        int anzahl = phasen.stream().mapToInt(List::size).max().orElse(0);
        for (int i = 0; i < anzahl; i++) {
            List<LivePartie> runde = new ArrayList<>();
            for (List<List<LivePartie>> phase : phasen) {
                if (i < phase.size()) {
                    runde.addAll(phase.get(i));
                }
            }
            runden.add(runde);
        }
        return this;
    }

    /** Runden ohne jede Partie (z.&nbsp;B. noch nicht feststehende KO-Runden) am Ende entfallen. */
    List<LiveRunde> runden() {
        int ende = runden.size();
        while (ende > 0 && runden.get(ende - 1).isEmpty()) {
            ende--;
        }
        List<LiveRunde> ergebnis = new ArrayList<>();
        for (int i = 0; i < ende; i++) {
            ergebnis.add(new LiveRunde(i + 1, runden.get(i)));
        }
        return ergebnis;
    }
}
