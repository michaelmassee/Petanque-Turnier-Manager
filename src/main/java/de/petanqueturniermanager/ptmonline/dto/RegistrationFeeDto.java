/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.dto;

/** Eine gewählte Tarifposition aus einer PTM-Online-Anmeldung. */
public record RegistrationFeeDto(String id, String name, Integer amountCents) {
}
