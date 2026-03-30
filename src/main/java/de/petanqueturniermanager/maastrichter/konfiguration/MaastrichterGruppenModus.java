/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.maastrichter.konfiguration;

/**
 * Bestimmt, wie Teams nach der Schweizer Vorrunde in Finalgruppen eingeteilt werden.
 */
public enum MaastrichterGruppenModus {
    /** Spec-konform: A = max. Siege, B = max-1, C = max-2, D = Rest. */
    NACH_SIEGEN,
    /** Quantitativ: gleichmäßige Gruppen nach Rang via {@code GruppenAufteilungRechner}. */
    NACH_GROESSE
}
