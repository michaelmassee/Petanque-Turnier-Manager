/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.dto;

/** Bestätigte Antwort auf eine turnierspezifische Online-Anmeldefrage. */
public record RegistrationAnswerDto(String participant, String questionId, Boolean checked, String questionLabel) {
}
