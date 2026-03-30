/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.poule.vorrunde;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;

/**
 * Verteilt Teams per Snake-Seeding auf Poule-Gruppen.
 * <p>
 * Einzige Implementierung des Seeding-Algorithmus – wird von
 * {@link PouleVorrundeSheet} und von {@link de.petanqueturniermanager.poule.PouleTurnierTestDaten}
 * gemeinsam genutzt, um DRY-Verstöße zu vermeiden.
 */
public class PouleSeedingService {

    /**
     * Eine Poule-Gruppe mit ihrer fortlaufenden Nummer (1-basiert) und den zugewiesenen Teams.
     *
     * @param pouleNr Nummer der Poule (1-basiert)
     * @param teams   Teams in dieser Poule (Slot-Reihenfolge: bester Slot zuerst)
     */
    public record Poule(int pouleNr, List<Team> teams) {}

    private PouleSeedingService() {
    }

    /**
     * Sortiert die aktiven Meldungen nach Setzposition (absteigend) und verteilt sie
     * per Snake-Seeding auf die angegebenen Gruppen.
     *
     * <p>Snake-Seeding-Beispiel für 4 Gruppen und 16 Teams:
     * <pre>
     *   Runde 0 (vorwärts):  T1→P1, T2→P2, T3→P3, T4→P4
     *   Runde 1 (rückwärts): T5→P4, T6→P3, T7→P2, T8→P1
     *   Runde 2 (vorwärts):  T9→P1, T10→P2, T11→P3, T12→P4
     *   Runde 3 (rückwärts): T13→P4, T14→P3, T15→P2, T16→P1
     * </pre>
     *
     * @param meldungen      aktive Team-Meldungen (Reihenfolge beliebig)
     * @param gruppenGroessen Liste der Gruppengrößen (z.B. [4, 4, 4, 3])
     * @return Liste von Poules in aufsteigender Reihenfolge (Poule 1, 2, 3, …)
     */
    public static List<Poule> verteileTeams(TeamMeldungen meldungen, List<Integer> gruppenGroessen) {
        var sortiertTeams = meldungen.teams().stream()
                .sorted(Comparator.comparingInt(Team::getSetzPos).reversed()
                        .thenComparingInt(Team::getNr))
                .toList();

        int anzGruppen = gruppenGroessen.size();
        List<List<Team>> pouleTeams = new ArrayList<>(anzGruppen);
        for (int i = 0; i < anzGruppen; i++) {
            pouleTeams.add(new ArrayList<>());
        }

        for (int i = 0; i < sortiertTeams.size(); i++) {
            int runde = i / anzGruppen;
            int posInRunde = i % anzGruppen;
            int pouleIdx = (runde % 2 == 0) ? posInRunde : (anzGruppen - 1 - posInRunde);
            pouleTeams.get(pouleIdx).add(sortiertTeams.get(i));
        }

        List<Poule> poules = new ArrayList<>(anzGruppen);
        for (int i = 0; i < anzGruppen; i++) {
            poules.add(new Poule(i + 1, List.copyOf(pouleTeams.get(i))));
        }
        return List.copyOf(poules);
    }
}
