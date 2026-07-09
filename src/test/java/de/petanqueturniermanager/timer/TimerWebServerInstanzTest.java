package de.petanqueturniermanager.timer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TimerWebServerInstanzTest {

    private TimerWebServerInstanz server;
    private int port;
    private HttpClient client;

    @BeforeEach
    void setUp() throws IOException {
        port = freienPortFinden();
        server = new TimerWebServerInstanz(port);
        client = HttpClient.newHttpClient();
    }

    @AfterEach
    void tearDown() {
        if (server.laeuft()) {
            server.stoppen();
        }
    }

    private static int freienPortFinden() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private HttpResponse<String> get(String pfad) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + pfad))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String pfad) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + pfad))
                .timeout(Duration.ofSeconds(5))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void konstruktor_setztPortUndLaeuftNichtDirekt() {
        assertThat(server.getPort()).isEqualTo(port);
        assertThat(server.laeuft()).isFalse();
    }

    @Test
    void starten_setztLaeuftAufTrue() {
        server.starten();
        assertThat(server.laeuft()).isTrue();
    }

    @Test
    void stoppen_setztLaeuftAufFalse() {
        server.starten();
        server.stoppen();
        assertThat(server.laeuft()).isFalse();
    }

    @Test
    void root_liefertHtmlSeite() throws Exception {
        server.starten();
        HttpResponse<String> response = get("/");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type")).contains("text/html; charset=UTF-8");
        assertThat(response.body()).isNotEmpty();
    }

    @Test
    void root_falscheMethode_liefert405() throws Exception {
        server.starten();
        assertThat(post("/").statusCode()).isEqualTo(405);
    }

    @Test
    void gong_liefertAudioDatei() throws Exception {
        server.starten();
        HttpResponse<String> response = get("/gong.wav");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type")).contains("audio/wav");
        assertThat(response.body()).isNotEmpty();
    }

    @Test
    void gong_falscheMethode_liefert405() throws Exception {
        server.starten();
        assertThat(post("/gong.wav").statusCode()).isEqualTo(405);
    }

    @Test
    void snooze_falscheMethode_liefert405() throws Exception {
        server.starten();
        assertThat(get("/snooze").statusCode()).isEqualTo(405);
    }

    @Test
    void snooze_ohneInitialisiertenTimerManager_liefertTrotzdem204() throws Exception {
        server.starten();
        // TimerManager.get() wirft IllegalStateException wenn nicht initialisiert -
        // handleSnooze faengt das ab und antwortet trotzdem mit 204.
        assertThat(post("/snooze").statusCode()).isEqualTo(204);
    }

    @Test
    void events_liefertServerSentEventsContentType() throws Exception {
        server.starten();
        // Die SSE-Verbindung bleibt absichtlich offen (kein Ende der Antwort) -
        // ein normaler HttpClient.send() mit Body-Read würde daher blockieren.
        // Stattdessen per Rohsocket nur die Header lesen und danach hart trennen.
        try (java.net.Socket socket = new java.net.Socket("localhost", port)) {
            socket.setSoTimeout(3_000);
            try (var out = socket.getOutputStream();
                    var in = new java.io.BufferedReader(
                            new java.io.InputStreamReader(socket.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                out.write(("GET /events HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n")
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8));
                out.flush();

                StringBuilder header = new StringBuilder();
                String zeile;
                while ((zeile = in.readLine()) != null && !zeile.isEmpty()) {
                    header.append(zeile).append('\n');
                }
                assertThat(header.toString()).contains("text/event-stream");
            }
        }
    }

    @Test
    void events_falscheMethode_liefert405() throws Exception {
        server.starten();
        assertThat(post("/events").statusCode()).isEqualTo(405);
    }

    @Test
    void onChange_ohneLaufendenServer_wirdIgnoriert() {
        // Server noch nicht gestartet -> onChange darf nicht crashen
        server.onChange(TimerState.inaktiv());
        assertThat(server.getCachedInitJson()).isNotNull();
    }

    @Test
    void onChange_aktualisiertCachedInitJson() {
        server.starten();
        String vorher = server.getCachedInitJson();

        server.onChange(new TimerState("04:32", 272, TimerZustand.LAEUFT, "Runde 1", "#123456", false));

        assertThat(server.getCachedInitJson()).isNotEqualTo(vorher);
        assertThat(server.getCachedInitJson()).contains("04:32").contains("Runde 1");
    }
}
