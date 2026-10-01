/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.dto;

/**
 * Ein Personen-Slot einer Online-Anmeldung (Feld {@code persons[]} der Sync-API, T-17/T-18). Die Benutzer-ID ist der
 * Schlüssel einer registrierten Person (E-19); PTM erhält sie nur aus PTM-Online und sendet sie unverändert zurück.
 *
 * @param slot   1-basierte Position der Person in der Anmeldung
 * @param userId Benutzer-ID des verknüpften Kontos, {@code null} bei Gästen
 */
public record PersonDto(int slot, String firstName, String lastName, String licenseNr, String userId) {
}
