package de.petanqueturniermanager.timer;

/**
 * Listener für Bedienaktionen am Timer: Start, Pause, Fortsetzen, Zeitkorrektur und Stopp – nicht für die
 * Sekunden-Ticks. Für Empfänger, die einen Zustand nur bei einer echten Änderung weitergeben (z.&nbsp;B. an
 * PTM-Online).
 * <p>
 * Wird auf dem Thread der Bedienung aufgerufen (meist der LO-Main-Thread); Empfänger dürfen dort nicht blockieren.
 */
@FunctionalInterface
public interface TimerSteuerungListener {

    /**
     * @param state     Zustand unmittelbar nach der Bedienaktion
     * @param steuerung Herkunft und Gesamtdauer des Timers
     */
    void onSteuerung(TimerState state, TimerSteuerung steuerung);
}
