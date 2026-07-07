package de.petanqueturniermanager.webserver;

import java.util.List;

/**
 * Fertige Konfiguration eines Composite Views (nach Resolver-Erstellung).
 *
 * @param port             TCP-Port, auf dem der Server lauscht
 * @param name             optionaler Anzeigename des Views (leer = kein benutzerdefinierter Name)
 * @param zoom             globaler Zoom-Faktor in Prozent (10–500, Standard 100)
 * @param wurzel           Wurzelknoten des Split-Baums
 * @param panels           Liste der Panel-Konfigurationen (Index = Panel-ID im Baum)
 * @param mitHeaderFooter  ob Document-Header/Footer einmal global (aus Panel 0) gerendert werden;
 *                         {@code false} = überhaupt keine Header/Footer anzeigen
 * @param rand             Konfiguration des Gesamtrahmens (Dicke/Art/Farbe/Transparenz/Animation)
 */
public record CompositeViewKonfiguration(
        int port,
        String name,
        int zoom,
        SplitKnoten wurzel,
        List<PanelKonfiguration> panels,
        boolean mitHeaderFooter,
        RandKonfiguration rand) {
    public CompositeViewKonfiguration {
        name = name == null ? "" : name;
        rand = rand == null ? RandKonfiguration.KEINER : rand;
    }
}
