package de.petanqueturniermanager.webserver;

/**
 * Konfiguration für einen einzelnen Webserver-Port.
 *
 * @param port     TCP-Port, auf dem der Server lauscht
 * @param resolver Resolver, der das anzuzeigende Sheet ermittelt
 */
public record PortKonfiguration(int port, SheetResolver resolver) {
}
