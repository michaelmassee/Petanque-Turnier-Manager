package de.petanqueturniermanager.timer;

/**
 * Listener für Timer-Zustandsänderungen.
 * Wird bei jedem Sekunden-Tick sowie bei Start, Pause, Fortsetzen und Stopp aufgerufen.
 */
@FunctionalInterface
public interface TimerListener {

    /**
     * Wird aufgerufen wenn sich der Timer-Zustand ändert.
     *
     * @param state aktueller unveränderlicher Zustand
     */
    void onChange(TimerState state);
}
