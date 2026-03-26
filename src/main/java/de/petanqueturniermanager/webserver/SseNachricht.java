package de.petanqueturniermanager.webserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SSE-Nachricht, die als JSON an den Browser gesendet wird.
 * <ul>
 *   <li>{@code "init"} – vollständiger Tabellenzustand (beim ersten Connect oder Reconnect)</li>
 *   <li>{@code "diff"} – nur geänderte Zellen (nach Sheet-Update)</li>
 *   <li>{@code "hinweis"} – Hinweismeldung, wenn kein Sheet verfügbar ist</li>
 * </ul>
 * Null-Felder werden von Gson nicht serialisiert (Standardverhalten).
 */
public record SseNachricht(
        String typ,
        Integer version,
        Integer zeilen,
        Integer spalten,
        List<List<String>> gitter,
        List<ZelleModel> zellen,
        Map<Integer, Integer> spaltenBreiten,
        Map<Integer, Integer> zeilenHoehen,
        String hinweisTitel,
        String hinweisText) {

    /** Vollständiger Tabellenzustand für neue/reconnectende Verbindungen. */
    static SseNachricht init(int version, TabelleModel modell) {
        return new SseNachricht(
                "init", version,
                modell.getZeilen(), modell.getSpalten(),
                modell.getGitter(),
                new ArrayList<>(modell.getZellen().values()),
                modell.getSpaltenBreiten(), modell.getZeilenHoehen(),
                null, null);
    }

    /** Nur geänderte Zellen; Gitter und Dimensionen immer aus dem neuen Modell. */
    static SseNachricht diff(int version, TabelleModel diffModell) {
        return new SseNachricht(
                "diff", version,
                diffModell.getZeilen(), diffModell.getSpalten(),
                diffModell.getGitter(),
                new ArrayList<>(diffModell.getZellen().values()),
                diffModell.getSpaltenBreiten(), diffModell.getZeilenHoehen(),
                null, null);
    }

    /** Hinweismeldung, wenn das konfigurierte Sheet noch nicht existiert. */
    static SseNachricht hinweis(String titel, String text) {
        return new SseNachricht("hinweis", null, null, null, null, null, null, null, titel, text);
    }
}
