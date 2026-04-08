package de.petanqueturniermanager.webserver;

import java.util.List;

import de.petanqueturniermanager.comp.GlobalProperties;

/**
 * SSE-Nachricht für Composite Views (mehrere Panels auf einer Seite).
 * <ul>
 *   <li>{@code "composite_init"} – vollständiger Zustand aller Panels</li>
 *   <li>{@code "composite_diff"} – nur geänderte Zellen der betroffenen Panels</li>
 *   <li>{@code "hinweis"} – Fehlermeldung, wenn ein Sheet nicht verfügbar ist</li>
 * </ul>
 * Null-Felder werden von Gson nicht serialisiert.
 *
 * @param typ          Nachrichtentyp
 * @param version      monoton steigende Versionsnummer (null bei hinweis)
 * @param panels       Panel-Daten (null bei hinweis)
 * @param layout       serialisierter {@link SplitKnoten}-Baum als Objekt (null bei hinweis)
 * @param zoom         globaler Zoom-Faktor in % (0 bei hinweis)
 * @param hinweisTitel Titel der Hinweismeldung (null außer bei hinweis)
 * @param hinweisText  Text der Hinweismeldung (null außer bei hinweis)
 */
public record CompositeSseNachricht(
        String typ,
        Integer version,
        List<CompositePanelNachricht> panels,
        Object layout,
        int zoom,
        String hinweisTitel,
        String hinweisText) {

    /** Vollständiger Zustand aller Panels für neue/reconnectende Verbindungen. */
    static CompositeSseNachricht init(int version, List<CompositePanelNachricht> panels,
            SplitKnoten layout, int zoom) {
        return new CompositeSseNachricht("composite_init", version, panels, layout, zoom, null, null);
    }

    /** Nur geänderte Panels; Layout wird immer mitgesendet (für Reconnects). */
    static CompositeSseNachricht diff(int version, List<CompositePanelNachricht> panels,
            SplitKnoten layout, int zoom) {
        return new CompositeSseNachricht("composite_diff", version, panels, layout, zoom, null, null);
    }

    /** Hinweismeldung, wenn ein Sheet nicht verfügbar ist. */
    static CompositeSseNachricht hinweis(String titel, String text) {
        return new CompositeSseNachricht("hinweis", null, null, null,
                GlobalProperties.DEFAULT_ZOOM, titel, text);
    }
}
