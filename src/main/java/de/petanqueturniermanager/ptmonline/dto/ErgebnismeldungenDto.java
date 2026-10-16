/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.dto;

import java.util.List;

/**
 * Antwort auf {@code GET …/result-reports}: ob Spieler Ergebnisse melden dürfen, der Turnierstatus und die offenen
 * Meldungen (leer, solange die Ergebnismeldung abgeschaltet ist).
 */
public record ErgebnismeldungenDto(boolean enabled, String status, List<ErgebnismeldungDto> reports) {

    public ErgebnismeldungenDto {
        status = status == null ? "" : status;
        reports = reports == null ? List.of() : List.copyOf(reports);
    }

    /** Nur dann lohnt sich das regelmäßige Abholen: Meldung eingeschaltet und Turnier läuft. */
    public boolean istAktiv() {
        return enabled && "running".equals(status);
    }
}
