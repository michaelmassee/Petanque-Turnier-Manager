package de.petanqueturniermanager.webserver;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.net.httpserver.HttpExchange;

/**
 * Regie-Quelle für ein Nicht-Master-Dokument.
 * <p>
 * Sie besitzt keinen eigenen HTTP-Port. Die Webserver-Regie hält stabile Ziel-URLs und
 * bindet deren SSE-Verbindungen an diese Quelle; der gerenderte Zustand liegt vollständig
 * im Cache dieser Instanz.
 */
final class DokumentRegieQuelle implements RegieQuelle {

    private static final Logger logger = LogManager.getLogger(DokumentRegieQuelle.class);

    private final String viewId;
    private final String anzeigename;
    private final int port;
    private final CompositeViewKonfiguration compositeKonfiguration;
    private final CopyOnWriteArrayList<SseVerbindung> sseVerbindungen = new CopyOnWriteArrayList<>();

    private volatile String cachedInitJson;
    private volatile boolean laeuft = true;
    private volatile String logoQuelle = "";

    DokumentRegieQuelle(String viewId, String anzeigename, int port,
            CompositeViewKonfiguration compositeKonfiguration) {
        this.viewId = viewId;
        this.anzeigename = anzeigename;
        this.port = port;
        this.compositeKonfiguration = compositeKonfiguration;
    }

    @Override
    public String getViewId() {
        return viewId;
    }

    @Override
    public String getAnzeigeName() {
        return anzeigename;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public boolean laeuft() {
        return laeuft;
    }

    void stoppen() {
        laeuft = false;
        sseVerbindungen.forEach(SseVerbindung::schliessen);
        sseVerbindungen.clear();
    }

    void setCachedInitJson(String json) {
        cachedInitJson = json;
    }

    void setLogoQuelle(String quelle) {
        logoQuelle = quelle == null ? "" : quelle.trim();
    }

    void sseNachrichtPushen(String json) {
        sseVerbindungen.forEach(v -> v.senden(json));
    }

    boolean hatTimerPanels() {
        return compositeKonfiguration != null
                && compositeKonfiguration.panels().stream().anyMatch(p -> p.typ() == PanelTyp.TIMER);
    }

    CompositeViewKonfiguration getKonfiguration() {
        return compositeKonfiguration;
    }

    @Override
    public String getCachedInitJson() {
        return cachedInitJson;
    }

    @Override
    public void verbindungEntfernen(SseVerbindung verbindung) {
        sseVerbindungen.remove(verbindung);
    }

    @Override
    public void regieVerbindungHinzufuegen(SseVerbindung verbindung) {
        sseVerbindungen.add(verbindung);
    }

    @Override
    public void regieVerbindungEntfernen(SseVerbindung verbindung) {
        sseVerbindungen.remove(verbindung);
    }

    @Override
    public void handleTurnierlogo(HttpExchange exchange) throws IOException {
        String quelle = logoQuelle;
        if (quelle.isEmpty()) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }
        if (quelle.startsWith("http://") || quelle.startsWith("https://")) {
            exchange.getResponseHeaders().set("Location", quelle);
            exchange.sendResponseHeaders(302, -1);
            return;
        }
        Path datei;
        try {
            datei = quelle.startsWith("file:")
                    ? Paths.get(URI.create(quelle))
                    : Paths.get(quelle);
        } catch (IllegalArgumentException e) {
            logger.warn("Ungültige Logo-Quelle für {}: {}", viewId, quelle, e);
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
        try (var os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    @Override
    public void serviereLokalePanelDatei(HttpExchange exchange, String panelPfad) throws IOException {
        if (compositeKonfiguration == null) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }
        LokalePanelDateien.servieren(exchange, compositeKonfiguration, panelPfad);
    }
}
