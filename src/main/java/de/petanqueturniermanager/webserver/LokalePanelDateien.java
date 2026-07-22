package de.petanqueturniermanager.webserver;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.sun.net.httpserver.HttpExchange;

final class LokalePanelDateien {

    private LokalePanelDateien() {
    }

    static void servieren(HttpExchange exchange, CompositeViewKonfiguration konfiguration, String panelPfad)
            throws IOException {
        String panelIdText = panelPfad;
        String relativerDateiPfad = "";
        int trenner = panelPfad.indexOf('/');
        if (trenner >= 0) {
            panelIdText = panelPfad.substring(0, trenner);
            relativerDateiPfad = panelPfad.substring(trenner + 1);
        }
        int panelId;
        try {
            panelId = Integer.parseInt(panelIdText);
        } catch (NumberFormatException e) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }
        var panels = konfiguration.panels();
        if (panelId < 0 || panelId >= panels.size()) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }
        var panel = panels.get(panelId);
        if (panel.typ() != PanelTyp.STATISCHE_DATEI) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }
        Path htmlDatei;
        try {
            String quelle = panel.externeUrl() == null ? "" : panel.externeUrl().trim();
            htmlDatei = quelle.startsWith("file:")
                    ? Paths.get(URI.create(quelle))
                    : Paths.get(quelle);
        } catch (IllegalArgumentException e) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }
        if (!Files.isRegularFile(htmlDatei) || !Files.isReadable(htmlDatei)) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }
        Path datei;
        try {
            datei = lokalePanelDateiAufloesen(htmlDatei, relativerDateiPfad);
        } catch (IOException | IllegalArgumentException e) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }
        if (!Files.isRegularFile(datei) || !Files.isReadable(datei)) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }
        byte[] body = Files.readAllBytes(datei);
        Path dateiname = datei.getFileName();
        var headers = exchange.getResponseHeaders();
        headers.set("Content-Type", WebContentType.fuerDateiname(dateiname != null ? dateiname.toString() : ""));
        headers.set("Cache-Control", "no-cache");
        headers.set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    private static Path lokalePanelDateiAufloesen(Path htmlDatei, String relativerDateiPfad) throws IOException {
        Path echteHtmlDatei = htmlDatei.toRealPath();
        Path echteWurzel = echteHtmlDatei.getParent();
        if (echteWurzel == null) {
            throw new IOException("Lokale Panel-Datei hat kein Verzeichnis: " + htmlDatei);
        }
        if (relativerDateiPfad == null || relativerDateiPfad.isEmpty()) {
            return echteHtmlDatei;
        }
        Path kandidat = echteWurzel.resolve(relativerDateiPfad).normalize().toRealPath();
        if (!kandidat.startsWith(echteWurzel)) {
            throw new IOException("Lokale Panel-Ressource liegt außerhalb des HTML-Verzeichnisses: " + kandidat);
        }
        return kandidat;
    }
}
