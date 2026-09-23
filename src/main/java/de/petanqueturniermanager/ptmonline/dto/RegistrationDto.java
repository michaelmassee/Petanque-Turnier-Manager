/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.dto;

import java.util.List;

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
        String partnerLicenseNr,
        String partner2FirstName,
        String partner2LastName,
        String partner2Email,
        String partner2LicenseNr,
        String teamName,
        Integer seedingPosition,
        String status,
        Boolean noEmail,
        Boolean isVip,
        String participation,
        List<RegistrationFeeDto> feeSelections,
        Integer feeTotalCents,
        List<RegistrationAnswerDto> registrationAnswers,
        String organizerMessage,
        String language,
        String registeredAt,
        String confirmedAt,
        String createdAt,
        String updatedAt,
        String localRegistrationUuid,
        Integer registrationRevision,
        Integer executionRevision) {
}
