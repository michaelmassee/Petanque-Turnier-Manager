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
        boolean checkinListenAnzeigen,
        List<String> angemeldetNichtEingecheckt,
        List<String> eingecheckt,
        List<String> neueEintraege,
        Map<String, String> i18n) {

    public static StartseiteSseNachricht init(int version, String turnierlogo, String turnierbeschreibung,
            String beschreibungAnimation, String beschreibungTextfarbe,
            int anzahlAngemeldet, int anzahlAktiv,
            String labelAngemeldet, String labelAktiv, String tagline,
            String turniersystem, String turnierStatus, List<String> sprueche, int zoom,
            boolean checkinListenAnzeigen, List<String> angemeldetNichtEingecheckt, List<String> eingecheckt) {
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
                checkinListenAnzeigen,
                angemeldetNichtEingecheckt == null ? List.of() : List.copyOf(angemeldetNichtEingecheckt),
                eingecheckt == null ? List.of() : List.copyOf(eingecheckt),
                List.of(),
                UiTexte.aktuelle());
    }

    /**
     * Update-Nachricht trägt alle Felder mit — Reducer im Frontend mergt sie, sodass
     * Beschreibung/Logo/Tagline auch dann erhalten bleiben, wenn die initiale Verbindung
     * vor einer späteren Property-Änderung aufgebaut wurde. {@code neueEintraege} enthält nur die
     * seit dem letzten Push neu hinzugekommenen Namen (aus beiden Listen zusammen) – das Frontend
     * animiert ausschließlich diese, nicht die komplette Liste.
     */
    public static StartseiteSseNachricht update(int version, String turnierlogo, String turnierbeschreibung,
            String beschreibungAnimation, String beschreibungTextfarbe,
            int anzahlAngemeldet, int anzahlAktiv,
            String labelAngemeldet, String labelAktiv, String tagline,
            String turniersystem, String turnierStatus, List<String> sprueche, int zoom,
            boolean checkinListenAnzeigen, List<String> angemeldetNichtEingecheckt, List<String> eingecheckt,
            List<String> neueEintraege) {
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
                checkinListenAnzeigen,
                angemeldetNichtEingecheckt == null ? List.of() : List.copyOf(angemeldetNichtEingecheckt),
                eingecheckt == null ? List.of() : List.copyOf(eingecheckt),
                neueEintraege == null ? List.of() : List.copyOf(neueEintraege),
                null);
    }
}
