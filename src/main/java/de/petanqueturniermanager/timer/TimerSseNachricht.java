package de.petanqueturniermanager.timer;

/**
 * SSE-Nachricht für den Timer-Webserver.
 * Wird als JSON an verbundene Browser-Clients gesendet.
 *
 * @param anzeige          formatierte Restzeit z.B. "04:32"
 * @param sekunden         verbleibende Sekunden
 * @param zustand          Zustand als String (z.B. "LAEUFT")
 * @param statusText       lokalisierter Status-Text (z.B. "Läuft", "Pausiert", "Zeit abgelaufen!")
 * @param bezeichnung      optionaler Rundenname
 * @param hintergrundFarbe Hintergrundfarbe als Hex-String (z.B. "#1a2b3c")
 * @param logoUrl          Browser-taugliche URL zum Turnierlogo, oder {@code null} wenn keines konfiguriert
 */
public record TimerSseNachricht(
        String anzeige,
        long sekunden,
        String zustand,
        String statusText,
        String bezeichnung,
        String hintergrundFarbe,
        String logoUrl) {
}
