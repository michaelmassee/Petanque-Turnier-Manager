/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.dto;

import java.util.List;

/**
 * Request-Payload fuer {@code POST /api/sync/tournaments/{id}/registrations}: legt eine neue
 * Anmeldung fuer ein lokal (im Turnierdokument) erfasstes, noch nicht online bekanntes Team an.
 * Server-seitig wird dafuer dieselbe {@code createRegistration}-Logik wie fuer die manuelle
 * Erfassung durch den Turnierleiter in der Web-UI genutzt (Bearer-Auth statt Session-Cookie).
 * {@code noEmail=true}: keine E-Mail-Adresse bekannt, Server erzeugt eine Platzhalter-Adresse.
 * {@code confirmImmediately=true}: Anmeldung direkt als {@code confirmed} anlegen statt
 * {@code pending}/Warteliste.
 * <p>
 * {@code feeSelections} MUSS als (ggf. leeres) Array mitgeschickt werden - {@code resolveFeeSelections}
 * im Worker verlangt bei neu angelegten Registrierungen (kein {@code existing}) zwingend ein Array,
 * sonst schlaegt der Aufruf mit „Ungueltige Startgeld-Auswahl" fehl. Startgeld ist fuer lokal (im
 * Turnierdokument) erfasste Teams kein Pflichtfeld, daher immer leer.
 */
public record NeueOnlineAnmeldung(
        String firstName,
        String lastName,
        String club,
        String licenseNr,
        String partnerFirstName,
        String partnerLastName,
        String partner2FirstName,
        String partner2LastName,
        String teamName,
        boolean noEmail,
        boolean confirmImmediately,
        List<Object> feeSelections) {
}
