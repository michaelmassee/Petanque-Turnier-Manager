/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp.newrelease;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

class GithubReleaseClientTest {

    private HttpServer server;
    private URI baseUri;

    @BeforeEach
    void setup() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        baseUri = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
    }

    @AfterEach
    void teardown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void parstStabilesRelease() {
        registriereHandler("/repos/foo/bar/releases/latest", 200, """
                {
                  "tag_name": "v1.2.3",
                  "name": "v1.2.3",
                  "published_at": "2026-04-01T10:00:00Z",
                  "prerelease": false,
                  "assets": [
                    {
                      "name": "PetanqueTurnierManager-1.2.3.oxt",
                      "browser_download_url": "https://example.com/PetanqueTurnierManager-1.2.3.oxt"
                    }
                  ]
                }
                """);

        var release = neuerClient("foo/bar").ladeLetztesRelease();

        assertThat(release).isPresent();
        assertThat(release.get().tagName()).isEqualTo("v1.2.3");
        assertThat(release.get().prerelease()).isFalse();
        assertThat(release.get().assets()).hasSize(1);
        assertThat(release.get().assets().get(0).downloadUrl())
                .isEqualTo("https://example.com/PetanqueTurnierManager-1.2.3.oxt");
    }

    @Test
    void erkenntPreReleaseFlag() {
        registriereHandler("/repos/foo/bar/releases/latest", 200, """
                {
                  "tag_name": "v1.2.4-rc1",
                  "name": "v1.2.4-rc1",
                  "prerelease": true,
                  "assets": []
                }
                """);

        var release = neuerClient("foo/bar").ladeLetztesRelease();

        assertThat(release).isPresent();
        assertThat(release.get().prerelease()).isTrue();
        assertThat(release.get().assets()).isEmpty();
    }

    @Test
    void liefertEmptyBeiHttp404() {
        registriereHandler("/repos/foo/bar/releases/latest", 404, "{\"message\":\"Not Found\"}");

        assertThat(neuerClient("foo/bar").ladeLetztesRelease()).isEmpty();
    }

    @Test
    void liefertEmptyBeiKaputtemJson() {
        registriereHandler("/repos/foo/bar/releases/latest", 200, "{ kaputt");

        assertThat(neuerClient("foo/bar").ladeLetztesRelease()).isEmpty();
    }

    @Test
    void liefertEmptyBeiFehlendemTagName() {
        registriereHandler("/repos/foo/bar/releases/latest", 200, "{\"name\": \"foo\"}");

        assertThat(neuerClient("foo/bar").ladeLetztesRelease()).isEmpty();
    }

    @Test
    void respektiertReadTimeout() {
        server.createContext("/repos/foo/bar/releases/latest", exchange -> {
            try {
                Thread.sleep(2_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            antwortSenden(exchange, 200, "{}");
        });

        var client = new GithubReleaseClient("foo/bar", baseUri,
                Duration.ofMillis(500), Duration.ofMillis(300));

        assertThat(client.ladeLetztesRelease()).isEmpty();
    }

    @Test
    void leereAssetsWerdenAlsLeereListeGeliefert() {
        registriereHandler("/repos/foo/bar/releases/latest", 200, """
                {
                  "tag_name": "v1.0",
                  "prerelease": false
                }
                """);

        var release = neuerClient("foo/bar").ladeLetztesRelease();

        assertThat(release).isPresent();
        assertThat(release.get().assets()).isEmpty();
        assertThat(release.get().name()).isEqualTo("v1.0");
    }

    private GithubReleaseClient neuerClient(String repository) {
        return new GithubReleaseClient(repository, baseUri,
                Duration.ofSeconds(2), Duration.ofSeconds(2));
    }

    private void registriereHandler(String pfad, int status, String body) {
        server.createContext(pfad, exchange -> antwortSenden(exchange, status, body));
    }

    private static void antwortSenden(HttpExchange exchange, int status, String body) throws IOException {
        var bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    @SuppressWarnings("unused")
    private static HttpHandler nullHandler() {
        return exchange -> antwortSenden(exchange, 200, "{}");
    }
}
