package de.petanqueturniermanager.webserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.petanqueturniermanager.comp.GlobalProperties;

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
        String hinweisText,
        String seitenTitel,
        String kopfzeileLinks,
        String kopfzeileMitte,
        String kopfzeileRechts,
        String fusszeileLinks,
        String fusszeileMitte,
        String fusszeileRechts,
        int zoom,
        boolean zentrieren) {

    /** Vollständiger Tabellenzustand für neue/reconnectende Verbindungen. */
    static SseNachricht init(int version, TabelleModel modell, String seitenTitel, int zoom, boolean zentrieren) {
        return new SseNachricht(
                "init", version,
                modell.getZeilen(), modell.getSpalten(),
                modell.getGitter(),
                new ArrayList<>(modell.getZellen().values()),
                modell.getSpaltenBreiten(), modell.getZeilenHoehen(),
                null, null,
                seitenTitel,
                modell.getKopfzeileLinks(), modell.getKopfzeileMitte(), modell.getKopfzeileRechts(),
                modell.getFusszeileLinks(), modell.getFusszeileMitte(), modell.getFusszeileRechts(),
                zoom, zentrieren);
    }

    /** Nur geänderte Zellen; Gitter, Dimensionen, Titel und Kopf-/Fußzeile immer aus dem neuen Modell. */
    static SseNachricht diff(int version, TabelleModel diffModell, String seitenTitel, int zoom, boolean zentrieren) {
        return new SseNachricht(
                "diff", version,
                diffModell.getZeilen(), diffModell.getSpalten(),
                diffModell.getGitter(),
                new ArrayList<>(diffModell.getZellen().values()),
                diffModell.getSpaltenBreiten(), diffModell.getZeilenHoehen(),
                null, null,
                seitenTitel,
                diffModell.getKopfzeileLinks(), diffModell.getKopfzeileMitte(), diffModell.getKopfzeileRechts(),
                diffModell.getFusszeileLinks(), diffModell.getFusszeileMitte(), diffModell.getFusszeileRechts(),
                zoom, zentrieren);
    }

    /** Hinweismeldung, wenn das konfigurierte Sheet noch nicht existiert. */
    static SseNachricht hinweis(String titel, String text) {
        return new SseNachricht("hinweis", null, null, null, null, null, null, null, titel, text,
                null, null, null, null, null, null, null,
                GlobalProperties.DEFAULT_ZOOM, false);
    }
}
