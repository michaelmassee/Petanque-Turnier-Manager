package de.petanqueturniermanager.webserver;

/**
 * Konfiguration eines einzelnen Panels in einem Composite View.
 *
 * @param resolver Sheet-Resolver für dieses Panel
 * @param zoom     Zoom-Faktor in Prozent (10–500, Standard 100)
 */
public record PanelKonfiguration(SheetResolver resolver, int zoom) {
}
