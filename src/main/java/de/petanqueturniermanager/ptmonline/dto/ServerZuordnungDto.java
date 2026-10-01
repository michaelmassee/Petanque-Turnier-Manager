/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.dto;

import java.util.List;

/**
 * Serverseitig bekannte Zuordnung lokale UUID ↔ Online-Anmeldung ({@code GET /api/sync/tournaments/<id>/mapping}),
 * Grundlage für „Zuordnung vom Server wiederherstellen“ (T-21).
 */
public record ServerZuordnungDto(String onlineRegistrationId, String localRegistrationUuid, String status,
        String firstName, String lastName, String partnerFirstName, String partnerLastName, String partner2FirstName,
        String partner2LastName, Integer executionRevision, Boolean overCapacity, Boolean receivedAfterStart,
        List<PersonDto> persons) {

    /** Ausführungsrevision, mindestens 1. */
    public int revision() {
        return executionRevision == null ? 1 : Math.max(1, executionRevision);
    }

    /** Personen-Slots samt Benutzer-IDs; ein älterer Server ohne {@code persons} liefert eine leere Liste. */
    public List<PersonDto> personen() {
        return persons == null ? List.of() : List.copyOf(persons);
    }
}
