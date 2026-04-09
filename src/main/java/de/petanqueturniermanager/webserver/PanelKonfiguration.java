package de.petanqueturniermanager.webserver;

/**
 * Konfiguration eines einzelnen Panels in einem Composite View.
 *
 * @param resolver          Sheet-Resolver für dieses Panel
 * @param zoom              Zoom-Faktor in Prozent (10–500, Standard 100)
 * @param zentriert         ob der Inhalt dieses Panels horizontal zentriert dargestellt wird
 * @param blattnameAnzeigen ob der Blattname als Kopfzeile im Panel angezeigt wird
 */
public record PanelKonfiguration(SheetResolver resolver, int zoom, boolean zentriert, boolean blattnameAnzeigen) {
}
