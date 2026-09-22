/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.dto;

/**
 * Rueckschreib-Payload fuer {@code POST /api/sync/tournaments/{id}/results}: aktualisiert nur
 * Status, Ranglisten-Position und Aktiv-Status einer bestehenden Anmeldung, legt keine neue an.
 * {@code status} und {@code active} sind {@code null} = unveraendert (server-seitiges COALESCE).
 */
public record RegistrationResultDto(String id, String status, Integer seedingPosition, Boolean active,
        Integer expectedExecutionRevision) {
}
