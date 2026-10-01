/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.dto;

import java.util.ArrayList;
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
        Integer executionRevision,
        Boolean overCapacity,
        Boolean receivedAfterStart,
        List<PersonDto> persons,
        Boolean accountConflict,
        Boolean possibleDuplicate,
        Boolean incomplete,
        String meleeTeamUuid) {

    /** Nachmeldung der Turnierleitung über die Online-Kapazität hinaus (T-24). */
    public boolean istUeberKapazitaet() {
        return Boolean.TRUE.equals(overCapacity);
    }

    /** Online nach dem lokalen Turnierstart eingegangen; wird nie automatisch importiert (KP-05). */
    public boolean istNachTurnierstartEingegangen() {
        return Boolean.TRUE.equals(receivedAfterStart);
    }

    /** Dieselbe Benutzer-ID steht online in einer weiteren aktiven Anmeldung (KP-06 b). */
    public boolean istKontoKonflikt() {
        return Boolean.TRUE.equals(accountConflict);
    }

    /** Formée-Team mit weniger Personen als die Formation verlangt (E-20, A-10). */
    public boolean istUnvollstaendig() {
        return Boolean.TRUE.equals(incomplete);
    }

    /**
     * Personen-Slots in Slot-Reihenfolge. Ein älterer Server ohne {@code persons} liefert nur die festen Namensfelder;
     * daraus werden Slots ohne Benutzer-ID gebildet.
     */
    public List<PersonDto> personen() {
        if (persons != null) {
            return List.copyOf(persons);
        }
        List<PersonDto> ausNamen = new ArrayList<>();
        fuegeHinzu(ausNamen, 1, firstName, lastName, licenseNr);
        fuegeHinzu(ausNamen, 2, partnerFirstName, partnerLastName, partnerLicenseNr);
        fuegeHinzu(ausNamen, 3, partner2FirstName, partner2LastName, partner2LicenseNr);
        return List.copyOf(ausNamen);
    }

    private static void fuegeHinzu(List<PersonDto> personen, int slot, String vorname, String nachname,
            String lizenz) {
        boolean leer = (vorname == null || vorname.isBlank()) && (nachname == null || nachname.isBlank());
        if (!leer) {
            personen.add(new PersonDto(slot, vorname, nachname, lizenz, null));
        }
    }
}
