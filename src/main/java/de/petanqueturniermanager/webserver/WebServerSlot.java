package de.petanqueturniermanager.webserver;

/**
 * Gemeinsame Abstraktion für Webserver-Einträge im Menü (URL-Slots).
 * <p>
 * Wird sowohl von {@link WebServerInstanz} (Einzel-Sheet) als auch von
 * {@link CompositeViewInstanz} (mehrere Panels) implementiert, damit
 * beide Typen gleichartig in der Menü-URL-Liste erscheinen.
 */
public interface WebServerSlot {

    /** Gibt zurück ob dieser Server läuft. */
    boolean laeuft();

    /** Gibt den TCP-Port zurück, auf dem dieser Server lauscht. */
    int getPort();

    /** Gibt den Anzeigenamen für den Menü-Eintrag zurück. */
    String getAnzeigeName();
}
