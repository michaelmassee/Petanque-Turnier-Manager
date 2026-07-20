package de.petanqueturniermanager.webserver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;

class CompositeViewInstanzTest {

    @TempDir
    Path tempDir;

    @Test
    void lokalePanelDateiServiertHtmlDatei() throws IOException {
        Path htmlDatei = testPanelDateienAnlegen();
        var instanz = compositeViewMitLokalerDatei(htmlDatei);

        var antwort = lokalePanelAntwort(instanz, "0/");

        assertThat(antwort.status()).isEqualTo(200);
        assertThat(antwort.contentType()).isEqualTo(WebContentType.HTML);
        assertThat(antwort.body()).isEqualTo("<h1>Panel</h1>");
    }

    @Test
    void lokalePanelDateiServiertSiblingCssRelativZurHtmlDatei() throws IOException {
        Path htmlDatei = testPanelDateienAnlegen();
        var instanz = compositeViewMitLokalerDatei(htmlDatei);

        var antwort = lokalePanelAntwort(instanz, "0/style.css");

        assertThat(antwort.status()).isEqualTo(200);
        assertThat(antwort.contentType()).isEqualTo("text/css; charset=UTF-8");
        assertThat(antwort.body()).isEqualTo("body { color: red; }");
    }

    @Test
    void lokalePanelDateiServiertVerschachteltesAssetRelativZurHtmlDatei() throws IOException {
        Path htmlDatei = testPanelDateienAnlegen();
        var instanz = compositeViewMitLokalerDatei(htmlDatei);

        var antwort = lokalePanelAntwort(instanz, "0/images/logo.png");

        assertThat(antwort.status()).isEqualTo(200);
        assertThat(antwort.contentType()).isEqualTo("image/png");
        assertThat(antwort.bytes()).containsExactly(1, 2, 3);
    }

    @Test
    void lokalePanelDateiBlockiertTraversalAusDemHtmlVerzeichnis() throws IOException {
        Path htmlDatei = testPanelDateienAnlegen();
        Files.writeString(tempDir.resolve("secret.txt"), "secret");
        var instanz = compositeViewMitLokalerDatei(htmlDatei);

        var antwort = lokalePanelAntwort(instanz, "0/../secret.txt");

        assertThat(antwort.status()).isEqualTo(404);
    }

    @Test
    void lokalePanelDateiBlockiertSymlinkAusDemHtmlVerzeichnis() throws IOException {
        Path htmlDatei = testPanelDateienAnlegen();
        Path geheimeDatei = tempDir.resolve("secret.txt");
        Files.writeString(geheimeDatei, "secret");
        try {
            Files.createSymbolicLink(htmlDatei.getParent().resolve("link.txt"), geheimeDatei);
        } catch (UnsupportedOperationException | IOException | SecurityException e) {
            assumeTrue(false, "Symlinks im Test-Dateisystem nicht verfügbar: " + e.getMessage());
        }
        var instanz = compositeViewMitLokalerDatei(htmlDatei);

        var antwort = lokalePanelAntwort(instanz, "0/link.txt");

        assertThat(antwort.status()).isEqualTo(404);
    }

    @Test
    void lokalePanelDateiIgnoriertUngueltigePanelId() throws IOException {
        Path htmlDatei = testPanelDateienAnlegen();
        var instanz = compositeViewMitLokalerDatei(htmlDatei);

        var antwort = lokalePanelAntwort(instanz, "foo.png");

        assertThat(antwort.status()).isEqualTo(404);
    }

    private Path testPanelDateienAnlegen() throws IOException {
        Path verzeichnis = tempDir.resolve("panel");
        Files.createDirectories(verzeichnis.resolve("images"));
        Path htmlDatei = verzeichnis.resolve("panel.html");
        Files.writeString(htmlDatei, "<h1>Panel</h1>");
        Files.writeString(verzeichnis.resolve("style.css"), "body { color: red; }");
        Files.write(verzeichnis.resolve("images").resolve("logo.png"), new byte[] {1, 2, 3});
        return htmlDatei;
    }

    private CompositeViewInstanz compositeViewMitLokalerDatei(Path htmlDatei) throws IOException {
        var panel = new PanelKonfiguration(PanelTyp.STATISCHE_DATEI, "", null, 100,
                "kein", "kein", false, htmlDatei.toString());
        var konfiguration = new CompositeViewKonfiguration(0, "", 100,
                new SplitBlatt(0), List.of(panel), false, RandKonfiguration.KEINER);
        return new CompositeViewInstanz(konfiguration);
    }

    private Antwort lokalePanelAntwort(CompositeViewInstanz instanz, String panelPfad) throws IOException {
        var exchange = new FakeHttpExchange();
        try {
            instanz.serviereLokalePanelDatei(exchange, panelPfad);
            return new Antwort(
                    exchange.status,
                    exchange.responseHeaders.getFirst("Content-Type"),
                    exchange.responseBody.toByteArray());
        } finally {
            instanz.stoppen();
        }
    }

    private record Antwort(int status, String contentType, byte[] bytes) {
        String body() {
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    private static final class FakeHttpExchange extends HttpExchange {

        private final Headers requestHeaders = new Headers();
        private final Headers responseHeaders = new Headers();
        private final ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
        private int status = -1;

        @Override
        public Headers getRequestHeaders() {
            return requestHeaders;
        }

        @Override
        public Headers getResponseHeaders() {
            return responseHeaders;
        }

        @Override
        public URI getRequestURI() {
            return URI.create("/");
        }

        @Override
        public String getRequestMethod() {
            return "GET";
        }

        @Override
        public HttpContext getHttpContext() {
            return null;
        }

        @Override
        public void close() {
            // Test-Dummy.
        }

        @Override
        public InputStream getRequestBody() {
            return new ByteArrayInputStream(new byte[0]);
        }

        @Override
        public OutputStream getResponseBody() {
            return responseBody;
        }

        @Override
        public void sendResponseHeaders(int rCode, long responseLength) {
            status = rCode;
        }

        @Override
        public int getResponseCode() {
            return status;
        }

        @Override
        public InetSocketAddress getRemoteAddress() {
            return new InetSocketAddress(0);
        }

        @Override
        public InetSocketAddress getLocalAddress() {
            return new InetSocketAddress(0);
        }

        @Override
        public String getProtocol() {
            return "HTTP/1.1";
        }

        @Override
        public Object getAttribute(String name) {
            return null;
        }

        @Override
        public void setAttribute(String name, Object value) {
            // Test-Dummy.
        }

        @Override
        public void setStreams(InputStream i, OutputStream o) {
            // Test-Dummy.
        }

        @Override
        public HttpPrincipal getPrincipal() {
            return null;
        }
    }
}
