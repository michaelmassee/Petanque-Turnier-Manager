package de.petanqueturniermanager.webserver;

/**
 * Konfiguration für einen einzelnen Webserver-Port.
 *
 * @param port        TCP-Port, auf dem der Server lauscht
 * @param resolver    Resolver, der das anzuzeigende Sheet ermittelt
 * @param zoom        Zoom-Faktor in Prozent (10–500, Standard 100)
 * @param zentrieren  ob die Tabelle im Browser horizontal zentriert wird
 */
public record PortKonfiguration(int port, SheetResolver resolver, int zoom, boolean zentrieren) {
}
