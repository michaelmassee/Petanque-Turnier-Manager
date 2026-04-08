package de.petanqueturniermanager.webserver;

import java.util.List;

/**
 * Fertige Konfiguration eines Composite Views (nach Resolver-Erstellung).
 *
 * @param port    TCP-Port, auf dem der Server lauscht
 * @param zoom    globaler Zoom-Faktor in Prozent (10–500, Standard 100)
 * @param wurzel  Wurzelknoten des Split-Baums
 * @param panels  Liste der Panel-Konfigurationen (Index = Panel-ID im Baum)
 */
public record CompositeViewKonfiguration(
        int port,
        int zoom,
        SplitKnoten wurzel,
        List<PanelKonfiguration> panels) {
}
