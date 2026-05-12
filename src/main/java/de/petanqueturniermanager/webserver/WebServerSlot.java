package de.petanqueturniermanager.webserver;

/**
 * Gemeinsame Abstraktion für Webserver-Einträge im Menü (URL-Slots).
 * Implementiert von {@link CompositeViewInstanz} und {@link TurnierStartseiteWebServerInstanz}.
 */
public interface WebServerSlot {

    /** Gibt zurück ob dieser Server läuft. */
    boolean laeuft();

    /** Gibt den TCP-Port zurück, auf dem dieser Server lauscht. */
    int getPort();

    /** Gibt den Anzeigenamen für den Menü-Eintrag zurück. */
    String getAnzeigeName();
}
