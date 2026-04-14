package de.petanqueturniermanager.timer;

/**
 * Unveränderlicher Schnappschuss des Timer-Zustands.
 * Wird bei jeder Änderung an alle {@link TimerListener} emittiert.
 *
 * @param anzeige          formatierte Restzeit als "MM:SS" oder "--:--" wenn inaktiv
 * @param sekunden         verbleibende Sekunden (0 wenn inaktiv oder abgelaufen)
 * @param zustand          aktueller {@link TimerZustand}
 * @param bezeichnung      optionaler Rundenname
 * @param hintergrundFarbe Hintergrundfarbe als Hex-String (z.B. "#1a2b3c")
 */
public record TimerState(String anzeige, long sekunden, TimerZustand zustand,
                         String bezeichnung, String hintergrundFarbe) {

    /** Erstellt den Inaktiv-Zustand (kein Timer läuft). */
    public static TimerState inaktiv() {
        return new TimerState("--:--", 0, TimerZustand.INAKTIV, "", "#000000");
    }
}
