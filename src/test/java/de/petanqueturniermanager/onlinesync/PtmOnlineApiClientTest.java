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
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
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

	// ---- pruefeVerbindung ----

	@Test
	void pruefeVerbindungErfolgreichBeiHttp200() {
		registriereHandler("/api/tournaments", "GET", 200, "[]");

		assertThatCode(() -> neuerClient().pruefeVerbindung()).doesNotThrowAnyException();
	}

	@Test
	void pruefeVerbindungWirftBeiHttpFehlerstatus() {
		registriereHandler("/api/tournaments", "GET", 401, "{\"error\":\"unauthorized\"}");

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
		server.createContext("/api/tournaments", exchange -> {
			gesendeterHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
			antwortSenden(exchange, 200, "[]");
		});

		neuerClient().pruefeVerbindung();

		assertThat(gesendeterHeader.get()).isEqualTo("Bearer ptm_testkey");
	}

	// ---- createTournament ----

	@Test
	void createTournamentSendetJsonUndParstDirekteAntwort() throws PtmOnlineException {
		AtomicReference<String> gesendeterBody = new AtomicReference<>();
		AtomicReference<String> methode = new AtomicReference<>();
		server.createContext("/api/tournaments", exchange -> {
			methode.set(exchange.getRequestMethod());
			gesendeterBody.set(neueLesenAlsString(exchange));
			antwortSenden(exchange, 200, """
					{ "id": "t-1", "name": "Herbstturnier", "type": "supermelee" }
					""");
		});

		TournamentDto neu = new TournamentDto();
		neu.name = "Herbstturnier";
		neu.type = "supermelee";

		TournamentDto angelegt = neuerClient().createTournament(neu);

		assertThat(methode.get()).isEqualTo("POST");
		assertThat(gesendeterBody.get()).contains("\"name\":\"Herbstturnier\"").contains("\"type\":\"supermelee\"");
		assertThat(angelegt.id).isEqualTo("t-1");
		assertThat(angelegt.name).isEqualTo("Herbstturnier");
	}

	@Test
	void createTournamentParstInTournamentSchluesselVerpackteAntwort() throws PtmOnlineException {
		registriereHandler("/api/tournaments", "POST", 200, """
				{ "tournament": { "id": "t-2", "name": "Winterturnier" } }
				""");

		TournamentDto angelegt = neuerClient().createTournament(new TournamentDto());

		assertThat(angelegt.id).isEqualTo("t-2");
		assertThat(angelegt.name).isEqualTo("Winterturnier");
	}

	@Test
	void createTournamentWirftBeiKaputtemJson() {
		registriereHandler("/api/tournaments", "POST", 200, "{ kaputt");

		assertThatThrownBy(() -> neuerClient().createTournament(new TournamentDto()))
				.isInstanceOf(PtmOnlineException.class);
	}

	// ---- getRegistrationsSince ----

	@Test
	void getRegistrationsSinceParstDirekteArrayAntwort() throws PtmOnlineException {
		registriereHandler("/api/sync/tournaments/t-1/registrations", "GET", 200, """
				[
				  { "id": "r-1", "firstName": "Michael", "lastName": "Massee", "status": "confirmed" }
				]
				""");

		List<RegistrationDto> registrierungen = neuerClient().getRegistrationsSince("t-1", null);

		assertThat(registrierungen).hasSize(1);
		assertThat(registrierungen.get(0).id).isEqualTo("r-1");
		assertThat(registrierungen.get(0).firstName).isEqualTo("Michael");
		assertThat(registrierungen.get(0).status).isEqualTo("confirmed");
	}

	@Test
	void getRegistrationsSinceParstInRegistrationsSchluesselVerpackteAntwort() throws PtmOnlineException {
		registriereHandler("/api/sync/tournaments/t-1/registrations", "GET", 200, """
				{ "registrations": [ { "id": "r-2" } ] }
				""");

		List<RegistrationDto> registrierungen = neuerClient().getRegistrationsSince("t-1", null);

		assertThat(registrierungen).hasSize(1);
		assertThat(registrierungen.get(0).id).isEqualTo("r-2");
	}

	@Test
	void getRegistrationsSinceLiefertLeereListeBeiLeeremArray() throws PtmOnlineException {
		registriereHandler("/api/sync/tournaments/t-1/registrations", "GET", 200, "[]");

		assertThat(neuerClient().getRegistrationsSince("t-1", null)).isEmpty();
	}

	@Test
	void getRegistrationsSinceHaengtSinceParameterAn() throws PtmOnlineException {
		AtomicReference<String> query = new AtomicReference<>();
		server.createContext("/api/sync/tournaments/t-1/registrations", exchange -> {
			query.set(exchange.getRequestURI().getQuery());
			antwortSenden(exchange, 200, "[]");
		});

		neuerClient().getRegistrationsSince("t-1", Instant.parse("2026-01-01T00:00:00Z"));

		assertThat(query.get()).isEqualTo("since=2026-01-01T00:00:00Z");
	}

	@Test
	void getRegistrationsSinceOhneZeitpunktHatKeinenQueryParameter() throws PtmOnlineException {
		AtomicReference<String> query = new AtomicReference<>();
		server.createContext("/api/sync/tournaments/t-1/registrations", exchange -> {
			query.set(exchange.getRequestURI().getQuery());
			antwortSenden(exchange, 200, "[]");
		});

		neuerClient().getRegistrationsSince("t-1", null);

		assertThat(query.get()).isNull();
	}

	// ---- postResults ----

	@Test
	void postResultsSendetRegistrationsWrapper() throws PtmOnlineException {
		AtomicReference<String> gesendeterBody = new AtomicReference<>();
		server.createContext("/api/sync/tournaments/t-1/results", exchange -> {
			gesendeterBody.set(neueLesenAlsString(exchange));
			antwortSenden(exchange, 200, "{}");
		});

		neuerClient().postResults("t-1", List.of(new ResultUpdateDto("r-1", "confirmed", 3)));

		assertThat(gesendeterBody.get()).contains("\"registrations\"").contains("\"id\":\"r-1\"")
				.contains("\"status\":\"confirmed\"").contains("\"seedingPosition\":3");
	}

	@Test
	void postResultsMitLeererListeMachtKeinenHttpAufruf() throws PtmOnlineException {
		AtomicReference<Boolean> aufgerufen = new AtomicReference<>(false);
		server.createContext("/api/sync/tournaments/t-1/results", exchange -> {
			aufgerufen.set(true);
			antwortSenden(exchange, 200, "{}");
		});

		neuerClient().postResults("t-1", List.of());

		assertThat(aufgerufen.get()).isFalse();
	}

	@Test
	void postResultsWirftBeiHttpFehlerstatus() {
		registriereHandler("/api/sync/tournaments/t-1/results", "POST", 500, "{\"error\":\"boom\"}");

		assertThatThrownBy(() -> neuerClient().postResults("t-1", List.of(new ResultUpdateDto("r-1", "confirmed", null))))
				.isInstanceOf(PtmOnlineException.class)
				.hasMessageContaining("500");
	}

	// ---- Hilfsmethoden ----

	private void registriereHandler(String pfad, String erwarteteMethode, int status, String body) {
		server.createContext(pfad, exchange -> {
			if (!erwarteteMethode.equals(exchange.getRequestMethod())) {
				antwortSenden(exchange, 405, "");
				return;
			}
			antwortSenden(exchange, status, body);
		});
	}

	private static String neueLesenAlsString(HttpExchange exchange) throws IOException {
		return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
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
