package de.petanqueturniermanager.webserver;

import java.util.List;
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
        String beschreibungTextfarbe,
        int anzahlAngemeldet,
        int anzahlAktiv,
        String labelAngemeldet,
        String labelAktiv,
        String tagline,
        String turniersystem,
        String turnierStatus,
        List<String> sprueche,
        int zoom,
        Map<String, String> i18n) {

    public static StartseiteSseNachricht init(int version, String turnierlogo, String turnierbeschreibung,
            String beschreibungAnimation, String beschreibungTextfarbe,
            int anzahlAngemeldet, int anzahlAktiv,
            String labelAngemeldet, String labelAktiv, String tagline,
            String turniersystem, String turnierStatus, List<String> sprueche, int zoom) {
        return new StartseiteSseNachricht("startseite_init", version,
                turnierlogo == null ? "" : turnierlogo,
                turnierbeschreibung == null ? "" : turnierbeschreibung,
                beschreibungAnimation == null || beschreibungAnimation.isBlank() ? "keine" : beschreibungAnimation,
                beschreibungTextfarbe == null ? "" : beschreibungTextfarbe,
                anzahlAngemeldet, anzahlAktiv,
                labelAngemeldet == null ? "" : labelAngemeldet,
                labelAktiv == null ? "" : labelAktiv,
                tagline == null ? "" : tagline,
                turniersystem == null ? "" : turniersystem,
                turnierStatus == null ? "" : turnierStatus,
                sprueche == null ? List.of() : List.copyOf(sprueche),
                zoom,
                UiTexte.aktuelle());
    }

    /**
     * Update-Nachricht trägt alle Felder mit — Reducer im Frontend mergt sie, sodass
     * Beschreibung/Logo/Tagline auch dann erhalten bleiben, wenn die initiale Verbindung
     * vor einer späteren Property-Änderung aufgebaut wurde.
     */
    public static StartseiteSseNachricht update(int version, String turnierlogo, String turnierbeschreibung,
            String beschreibungAnimation, String beschreibungTextfarbe,
            int anzahlAngemeldet, int anzahlAktiv,
            String labelAngemeldet, String labelAktiv, String tagline,
            String turniersystem, String turnierStatus, List<String> sprueche, int zoom) {
        return new StartseiteSseNachricht("startseite_update", version,
                turnierlogo == null ? "" : turnierlogo,
                turnierbeschreibung == null ? "" : turnierbeschreibung,
                beschreibungAnimation == null || beschreibungAnimation.isBlank() ? "keine" : beschreibungAnimation,
                beschreibungTextfarbe == null ? "" : beschreibungTextfarbe,
                anzahlAngemeldet, anzahlAktiv,
                labelAngemeldet == null ? "" : labelAngemeldet,
                labelAktiv == null ? "" : labelAktiv,
                tagline == null ? "" : tagline,
                turniersystem == null ? "" : turniersystem,
                turnierStatus == null ? "" : turnierStatus,
                sprueche == null ? List.of() : List.copyOf(sprueche),
                zoom,
                null);
    }
}
