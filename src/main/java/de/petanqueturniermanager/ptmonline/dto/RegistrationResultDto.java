/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.dto;

/**
 * Rueckschreib-Payload fuer {@code POST /api/sync/tournaments/{id}/results}: aktualisiert nur
 * Anmeldestatus, Ranglisten-Position und Teilnahme einer bestehenden Anmeldung, legt keine neue an.
 * {@code status} und {@code participation} sind {@code null} = unveraendert (server-seitiges
 * COALESCE). {@code participation} ist ein {@link de.petanqueturniermanager.ptmonline.OnlineTeilnahme#apiWert()}.
 */
public record RegistrationResultDto(String id, String status, Integer seedingPosition, String participation,
        Integer expectedExecutionRevision) {
}
