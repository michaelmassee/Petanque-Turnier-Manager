package de.petanqueturniermanager.webserver;

import java.util.Map;

/**
 * SSE-Nachricht für die Turnier-Startseite. Zwei Typen:
 * <ul>
 *   <li>{@code startseite_init} – voller Zustand (Logo, Name + Zahlen) beim Verbindungsaufbau.</li>
 *   <li>{@code startseite_update} – schlanke Update-Nachricht, die nur die Teilnehmerzahlen ändert.</li>
 * </ul>
 * Bewusst flacher Record, JSON-serialisiert via Gson.
 */
public record StartseiteSseNachricht(
        String typ,
        int version,
        String turnierlogo,
        String turnierbeschreibung,
        String beschreibungAnimation,
        String hintergrundfarbe,
        int anzahlAngemeldet,
        int anzahlAktiv,
        String labelAngemeldet,
        String labelAktiv,
        String tagline,
        Map<String, String> i18n) {

    public static StartseiteSseNachricht init(int version, String turnierlogo, String turnierbeschreibung,
            String beschreibungAnimation,
            String hintergrundfarbe,
            int anzahlAngemeldet, int anzahlAktiv,
            String labelAngemeldet, String labelAktiv, String tagline) {
        return new StartseiteSseNachricht("startseite_init", version,
                turnierlogo == null ? "" : turnierlogo,
                turnierbeschreibung == null ? "" : turnierbeschreibung,
                beschreibungAnimation == null || beschreibungAnimation.isBlank() ? "keine" : beschreibungAnimation,
                hintergrundfarbe == null ? "" : hintergrundfarbe,
                anzahlAngemeldet, anzahlAktiv,
                labelAngemeldet == null ? "" : labelAngemeldet,
                labelAktiv == null ? "" : labelAktiv,
                tagline == null ? "" : tagline,
                UiTexte.aktuelle());
    }

    /**
     * Update-Nachricht trägt alle Felder mit — Reducer im Frontend mergt sie, sodass
     * Beschreibung/Logo/Tagline/Hintergrundfarbe auch dann erhalten bleiben, wenn die
     * initiale Verbindung vor einer späteren Property-Änderung aufgebaut wurde.
     */
    public static StartseiteSseNachricht update(int version, String turnierlogo, String turnierbeschreibung,
            String beschreibungAnimation,
            String hintergrundfarbe,
            int anzahlAngemeldet, int anzahlAktiv,
            String labelAngemeldet, String labelAktiv, String tagline) {
        return new StartseiteSseNachricht("startseite_update", version,
                turnierlogo == null ? "" : turnierlogo,
                turnierbeschreibung == null ? "" : turnierbeschreibung,
                beschreibungAnimation == null || beschreibungAnimation.isBlank() ? "keine" : beschreibungAnimation,
                hintergrundfarbe == null ? "" : hintergrundfarbe,
                anzahlAngemeldet, anzahlAktiv,
                labelAngemeldet == null ? "" : labelAngemeldet,
                labelAktiv == null ? "" : labelAktiv,
                tagline == null ? "" : tagline,
                null);
    }
}
