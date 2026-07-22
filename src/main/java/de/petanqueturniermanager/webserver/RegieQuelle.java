package de.petanqueturniermanager.webserver;

import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;

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

    default void handleTurnierlogo(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(404, -1);
    }

    default void serviereLokalePanelDatei(HttpExchange exchange, String panelPfad) throws IOException {
        exchange.sendResponseHeaders(404, -1);
    }
}
