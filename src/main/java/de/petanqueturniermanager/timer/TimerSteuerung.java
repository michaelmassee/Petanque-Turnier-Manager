package de.petanqueturniermanager.timer;

import java.util.Optional;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;

/**
 * Rahmen des laufenden Timers für {@link TimerSteuerungListener}.
 *
 * @param ursprung      Dokument, aus dem der Timer gestartet wurde; leer, wenn er ohne Dokument gestartet wurde
 * @param dauerSekunden Gesamtdauer beim Start
 */
public record TimerSteuerung(Optional<WorkingSpreadsheet> ursprung, long dauerSekunden) {

    static final TimerSteuerung OHNE_URSPRUNG = new TimerSteuerung(Optional.empty(), 0);
}
