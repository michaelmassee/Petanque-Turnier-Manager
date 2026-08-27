/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.dto;

/**
 * Rueckschreib-Payload fuer {@code POST /api/sync/tournaments/{id}/results}: aktualisiert nur
 * Status und Ranglisten-Position einer bestehenden Anmeldung, legt keine neue an.
 */
public record RegistrationResultDto(String id, String status, Integer seedingPosition) {
}
