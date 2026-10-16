/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.dto;

import java.util.List;

/**
 * Offene Ergebnismeldung eines Spielers aus der Live-Ansicht ({@code GET …/result-reports}), die auf die Entscheidung
 * der Turnierleitung im Dokument wartet (E-27, E-28). Die Partie ist über Rundennummer und die Online-IDs ihrer Seiten
 * bestimmt, so wie das Dokument sie übertragen hat.
 *
 * @param scoreA     Punkte von Seite A
 * @param scoreB     Punkte von Seite B
 * @param reportedBy Bezeichnung der meldenden Anmeldung
 */
public record ErgebnismeldungDto(String id, int roundNumber, List<String> teamA, List<String> teamB, int scoreA,
        int scoreB, String reportedBy, String createdAt) {

    public ErgebnismeldungDto {
        teamA = teamA == null ? List.of() : List.copyOf(teamA);
        teamB = teamB == null ? List.of() : List.copyOf(teamB);
        reportedBy = reportedBy == null ? "" : reportedBy;
        createdAt = createdAt == null ? "" : createdAt;
    }
}
