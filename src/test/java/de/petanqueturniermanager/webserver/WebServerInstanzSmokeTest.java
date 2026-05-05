package de.petanqueturniermanager.webserver;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;

/**
 * Block C3 — Smoke-Test für {@link WebServerInstanz}.
 * <p>
 * Verifiziert Start/Stop, eine HTTP-Anfrage und das JSON-Schema des
 * Diagnose-Endpunkts {@code /debug/sse}. Läuft ohne LibreOffice — der
 * {@link SheetResolver} ist ein Stub, da der Diagnose-Pfad keinen Sheet-Zugriff
 * benötigt.
 */
public class WebServerInstanzSmokeTest {

    private WebServerInstanz instanz;
    private int port;
    private HttpClient httpClient;

    @BeforeEach
    public void starteServer() throws IOException {
        port = ermittleFreienPort();
        instanz = new WebServerInstanz(new PortKonfiguration(port, new StubResolver(), 100, false));
        instanz.starten();
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @AfterEach
    public void stoppeServer() {
        if (instanz != null && instanz.laeuft()) {
            instanz.stoppen();
        }
    }

    @Test
    public void serverIstNachStartLaufendAufKonfiguriertemPort() {
        assertThat(instanz.laeuft()).isTrue();
        assertThat(instanz.getPort()).isEqualTo(port);
    }

    @Test
    public void debugSseLiefertFallbackJsonOhneCache() throws Exception {
        HttpResponse<String> response = get("/debug/sse");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type"))
                .hasValue("application/json; charset=UTF-8");

        JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
        assertThat(body.has("info"))
                .as("Fallback-JSON enthält Info-Feld wenn kein cachedInitJson gesetzt")
                .isTrue();
    }

    @Test
    public void debugSseLiefertGecachtesJsonNachSetCachedInitJson() throws Exception {
        String erwartetesJson = "{\"typ\":\"init\",\"version\":1,\"titel\":\"Smoketest\"}";
        instanz.setCachedInitJson(erwartetesJson);

        HttpResponse<String> response = get("/debug/sse");

        assertThat(response.statusCode()).isEqualTo(200);
        JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
        assertThat(body.get("typ").getAsString()).isEqualTo("init");
        assertThat(body.get("version").getAsInt()).isEqualTo(1);
        assertThat(body.get("titel").getAsString()).isEqualTo("Smoketest");
    }

    @Test
    public void unbekannterPfadLiefert404() throws Exception {
        HttpResponse<String> response = get("/gibt-es-nicht");
        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    public void postAufDebugSseLiefert405() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/debug/sse"))
                .timeout(Duration.ofSeconds(2))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(405);
    }

    @Test
    public void serverLaeuftNichtMehrNachStop() {
        instanz.stoppen();
        assertThat(instanz.laeuft()).isFalse();
    }

    private HttpResponse<String> get(String pfad) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + pfad))
                .timeout(Duration.ofSeconds(2))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static int ermittleFreienPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    /** Minimaler Resolver-Stub — Diagnose-Pfad braucht keinen Sheet-Zugriff. */
    private static final class StubResolver implements SheetResolver {
        @Override
        public Optional<XSpreadsheet> resolve(WorkingSpreadsheet ws) {
            return Optional.empty();
        }

        @Override
        public String getAnzeigeName() {
            return "Smoketest";
        }

        @Override
        public Optional<Integer> getNummer(XSpreadsheet sheet) {
            return Optional.empty();
        }
    }
}
