/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.onlinesync;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

/**
 * HTTP-Client für die PTM-Online-REST-API (Cloudflare-Worker-App, siehe
 * {@code Petanque-Turnier-Manager-Online/SECURITY.md}). Auth per API-Key
 * ({@code Authorization: Bearer ptm_...}).
 * <p>
 * Die genaue JSON-Antwortform der Endpunkte ist außerhalb dieses Repos definiert; dieser Client
 * parst daher defensiv sowohl eine direkte Objekt-/Array-Antwort als auch eine unter einem
 * naheliegenden Schlüssel ({@code tournament}/{@code registrations}) verpackte Antwort.
 */
public class PtmOnlineApiClient {

	private static final Logger logger = LogManager.getLogger(PtmOnlineApiClient.class);

	private static final Gson GSON = new GsonBuilder().create();
	private static final Duration TIMEOUT = Duration.ofSeconds(15);

	private final String apiKey;
	private final String baseUrl;
	private final HttpClient httpClient;

	public PtmOnlineApiClient(String apiKey, String baseUrl) {
		this.apiKey = StringUtils.trimToEmpty(apiKey);
		this.baseUrl = StringUtils.stripEnd(StringUtils.trimToEmpty(baseUrl), "/");
		this.httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
	}

	/** Leichter Verbindungstest: listet die eigenen Turniere; wirft bei Fehler eine {@link PtmOnlineException}. */
	public void pruefeVerbindung() throws PtmOnlineException {
		senden(request("GET", "/api/tournaments", null));
	}

	public TournamentDto createTournament(TournamentDto turnier) throws PtmOnlineException {
		String antwort = senden(request("POST", "/api/tournaments", GSON.toJson(turnier)));
		JsonObject json = parseObject(antwort);
		JsonObject turnierJson = json.has("tournament") && json.get("tournament").isJsonObject()
				? json.getAsJsonObject("tournament")
				: json;
		return GSON.fromJson(turnierJson, TournamentDto.class);
	}

	public List<RegistrationDto> getRegistrationsSince(String tournamentId, Instant since) throws PtmOnlineException {
		String pfad = "/api/sync/tournaments/" + tournamentId + "/registrations";
		if (since != null) {
			pfad += "?since=" + DateTimeFormatter.ISO_INSTANT.format(since);
		}
		String antwort = senden(request("GET", pfad, null));
		JsonElement json = com.google.gson.JsonParser.parseString(antwort);
		JsonArray array = json.isJsonObject() && json.getAsJsonObject().has("registrations")
				? json.getAsJsonObject().getAsJsonArray("registrations")
				: json.getAsJsonArray();
		return GSON.fromJson(array, new com.google.gson.reflect.TypeToken<List<RegistrationDto>>() {
		}.getType());
	}

	public void postResults(String tournamentId, List<ResultUpdateDto> updates) throws PtmOnlineException {
		if (updates == null || updates.isEmpty()) {
			return;
		}
		JsonObject body = new JsonObject();
		body.add("registrations", GSON.toJsonTree(updates));
		senden(request("POST", "/api/sync/tournaments/" + tournamentId + "/results", GSON.toJson(body)));
	}

	private HttpRequest request(String method, String pfad, String jsonBody) throws PtmOnlineException {
		if (baseUrl.isEmpty()) {
			throw new PtmOnlineException("Keine PTM-Online-Basis-URL konfiguriert (Extras > Optionen > PétTurnMngr > PTM Online)");
		}
		if (apiKey.isEmpty()) {
			throw new PtmOnlineException("Kein PTM-Online-API-Key konfiguriert (Extras > Optionen > PétTurnMngr > PTM Online)");
		}
		HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + pfad)).timeout(TIMEOUT)
				.header("Authorization", "Bearer " + apiKey).header("Accept", "application/json");
		if (jsonBody == null) {
			builder.method(method, BodyPublishers.noBody());
		} else {
			builder.header("Content-Type", "application/json")
					.method(method, BodyPublishers.ofString(jsonBody));
		}
		return builder.build();
	}

	private String senden(HttpRequest request) throws PtmOnlineException {
		try {
			HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				throw new PtmOnlineException("PTM Online antwortete mit HTTP " + response.statusCode() + ": "
						+ StringUtils.abbreviate(response.body(), 500));
			}
			return response.body();
		} catch (IOException e) {
			logger.debug("Netzwerkfehler bei PTM-Online-Anfrage {}", request.uri(), e);
			throw new PtmOnlineException("Netzwerkfehler bei Zugriff auf PTM Online: " + e.getMessage(), e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new PtmOnlineException("PTM-Online-Anfrage wurde unterbrochen", e);
		}
	}

	private static JsonObject parseObject(String json) throws PtmOnlineException {
		try {
			return com.google.gson.JsonParser.parseString(json).getAsJsonObject();
		} catch (JsonSyntaxException | IllegalStateException e) {
			throw new PtmOnlineException("Antwort von PTM Online konnte nicht gelesen werden: " + e.getMessage(), e);
		}
	}
}
