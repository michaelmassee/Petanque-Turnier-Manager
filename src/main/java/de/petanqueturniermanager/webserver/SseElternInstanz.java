package de.petanqueturniermanager.webserver;

/**
 * Abstraktion der Eltern-Instanz einer {@link SseVerbindung}.
 * <p>
 * Ermöglicht {@link CompositeViewInstanz} und {@link de.petanqueturniermanager.timer.TimerWebServerInstanz},
 * SSE-Verbindungen über dieselbe {@link SseVerbindung}-Klasse zu verwalten.
 */
public interface SseElternInstanz {

    /** Gibt den zuletzt gecachten Init-State zurück (für neue Verbindungen). */
    String getCachedInitJson();

    /** Gibt optionale Zusatz-Nachrichten zurück, die direkt nach dem Init gesendet werden. */
    default String[] getInitZusatzJsons() {
        return new String[0];
    }

    /** Entfernt eine abgebrochene SSE-Verbindung aus der internen Liste. */
    void verbindungEntfernen(SseVerbindung verbindung);
}
