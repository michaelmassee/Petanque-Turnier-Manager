/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.dto;

/**
 * Request-Payload fuer {@code POST /api/tournaments} (siehe {@code normalizeTournamentInput()} in
 * {@code src/worker.js}). {@code name}, {@code date}, {@code location} sind Pflichtfelder; alle
 * anderen Felder duerfen {@code null} sein und werden serverseitig auf sinnvolle Defaults gesetzt.
 */
public record CreateTournamentDto(
        String name,
        String date,
        String startTime,
        String location,
        String description,
        String type,
        String formation,
        String status,
        String visibility) {
}
