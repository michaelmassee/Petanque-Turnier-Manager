/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.dto;

/** Vollständige, vom Turnierdokument geführte Stammdaten eines PTM-Online-Turniers. */
public record TournamentMetadataDto(
        String name, String date, String startTime, String location, String description,
        String type, String formation, String status, int maxRegistrations,
        String registrationDeadline, int entryFeeCents, String contactName, String contactEmail,
        String contactPhone, String visibility, String internalNotes,
        boolean participantsPublic, boolean licenseRequired) {
}
