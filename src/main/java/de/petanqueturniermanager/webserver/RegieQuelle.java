package de.petanqueturniermanager.webserver;

/**
 * Laufzeit-Abstraktion einer Anzeigequelle, die von der Webserver-Regie unter
 * stabilen Ziel-URLs gespiegelt werden kann.
 */
public interface RegieQuelle extends SseElternInstanz {

    String REGIE_ROLLE = "regie";

    String getViewId();

    String getAnzeigeName();

    int getPort();

    boolean laeuft();

    void regieVerbindungHinzufuegen(SseVerbindung verbindung);

    void regieVerbindungEntfernen(SseVerbindung verbindung);
}
