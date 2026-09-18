/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.onlinesync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

class PtmOnlineApiClientTest {

	private HttpServer server;
	private String baseUrl;

	@BeforeEach
	void setup() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.start();
		baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
	}

	@AfterEach
	void teardown() {
		if (server != null) {
			server.stop(0);
		}
	}

	private PtmOnlineApiClient neuerClient() {
		return new PtmOnlineApiClient("ptm_testkey", baseUrl);
	}

	@Test
	void pruefeVerbindungErfolgreichBeiHttp200() {
		registriereHandler("/api/sync/tournaments", "GET", 200, "[]");

		assertThatCode(() -> neuerClient().pruefeVerbindung()).doesNotThrowAnyException();
	}

	@Test
	void pruefeVerbindungWirftBeiHttpFehlerstatus() {
		registriereHandler("/api/sync/tournaments", "GET", 401, "{\"error\":\"unauthorized\"}");

		assertThatThrownBy(() -> neuerClient().pruefeVerbindung())
				.isInstanceOf(PtmOnlineException.class)
				.hasMessageContaining("401");
	}

	@Test
	void pruefeVerbindungWirftBeiFehlendemApiKey() {
		var client = new PtmOnlineApiClient("", baseUrl);

		assertThatThrownBy(client::pruefeVerbindung)
				.isInstanceOf(PtmOnlineException.class)
				.hasMessageContaining("API-Key");
	}

	@Test
	void pruefeVerbindungWirftBeiFehlenderBasisUrl() {
		var client = new PtmOnlineApiClient("ptm_testkey", "");

		assertThatThrownBy(client::pruefeVerbindung)
				.isInstanceOf(PtmOnlineException.class)
				.hasMessageContaining("Basis-URL");
	}

	@Test
	void pruefeVerbindungWirftBeiNetzwerkfehler() {
		server.stop(0);
		server = null;

		assertThatThrownBy(() -> neuerClient().pruefeVerbindung())
				.isInstanceOf(PtmOnlineException.class)
				.hasMessageContaining("Netzwerkfehler");
	}

	@Test
	void sendetAuthorizationHeaderMitApiKey() throws PtmOnlineException {
		AtomicReference<String> gesendeterHeader = new AtomicReference<>();
		server.createContext("/api/sync/tournaments", exchange -> {
			gesendeterHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
			antwortSenden(exchange, 200, "[]");
		});

		neuerClient().pruefeVerbindung();

		assertThat(gesendeterHeader.get()).isEqualTo("Bearer ptm_testkey");
	}

	private void registriereHandler(String pfad, String erwarteteMethode, int status, String body) {
		server.createContext(pfad, exchange -> {
			if (!erwarteteMethode.equals(exchange.getRequestMethod())) {
				antwortSenden(exchange, 405, "");
				return;
			}
			antwortSenden(exchange, status, body);
		});
	}

	private static void antwortSenden(HttpExchange exchange, int status, String body) throws IOException {
		var bytes = body.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("Content-Type", "application/json");
		exchange.sendResponseHeaders(status, bytes.length);
		try (OutputStream os = exchange.getResponseBody()) {
			os.write(bytes);
		}
	}
}
