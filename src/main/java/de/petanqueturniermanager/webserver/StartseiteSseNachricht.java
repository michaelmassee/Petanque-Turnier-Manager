package de.petanqueturniermanager.webserver;

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
        String turniername,
        int anzahlAngemeldet,
        int anzahlAktiv) {

    public static StartseiteSseNachricht init(int version, String turnierlogo, String turniername,
            int anzahlAngemeldet, int anzahlAktiv) {
        return new StartseiteSseNachricht("startseite_init", version,
                turnierlogo == null ? "" : turnierlogo,
                turniername == null ? "" : turniername,
                anzahlAngemeldet, anzahlAktiv);
    }

    public static StartseiteSseNachricht update(int version, int anzahlAngemeldet, int anzahlAktiv) {
        return new StartseiteSseNachricht("startseite_update", version, null, null,
                anzahlAngemeldet, anzahlAktiv);
    }
}
