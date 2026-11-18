/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.live;

import java.util.List;

/**
 * Sieger eines KO-Felds (Finale entschieden), zweiter Teil der Maastrichter-Endrangliste neben der
 * Vorrunden-Rangliste.
 *
 * @param feld Bezeichnung des Felds wie die Finalrunde im Dokument (z.&nbsp;B. „A-Finale“)
 * @param team lokale Team-Nr des Siegers
 */
public record LiveKoSieger(String feld, List<Integer> team) {

    public LiveKoSieger {
        team = List.copyOf(team);
    }
}
