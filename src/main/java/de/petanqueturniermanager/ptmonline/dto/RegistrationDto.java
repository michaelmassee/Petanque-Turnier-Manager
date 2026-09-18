/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.dto;

/**
 * Spiegelt das JSON-Feld {@code registration} / {@code registrations[]} der PTM-Online REST-API
 * (siehe {@code toPublicRegistration()} in {@code src/worker.js}).
 */
public record RegistrationDto(
        String id,
        String tournamentId,
        String firstName,
        String lastName,
        String email,
        String club,
        String licenseNr,
        String partnerFirstName,
        String partnerLastName,
        String partnerEmail,
        String partner2FirstName,
        String partner2LastName,
        String partner2Email,
        String teamName,
        Integer seedingPosition,
        String status,
        String registeredAt,
        String confirmedAt,
        String createdAt,
        String updatedAt) {
}
