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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * HTTP-Client für den PTM-Online-Verbindungstest (Extras &gt; Optionen &gt; PétTurnMngr &gt; PTM
 * Online). Auth per API-Key ({@code Authorization: Bearer ptm_...}). Die eigentliche
 * Turnier-Synchronisation (Liste laden, verbinden, Anmeldungen/Ergebnisse abgleichen) läuft über
 * {@link de.petanqueturniermanager.ptmonline.TournamentSyncClient}.
 */
public class PtmOnlineApiClient {

	private static final Logger logger = LogManager.getLogger(PtmOnlineApiClient.class);

	private static final Duration TIMEOUT = Duration.ofSeconds(15);

	private final String apiKey;
	private final String baseUrl;
	private final HttpClient httpClient;

	public PtmOnlineApiClient(String apiKey, String baseUrl) {
		this.apiKey = StringUtils.trimToEmpty(apiKey);
		this.baseUrl = StringUtils.stripEnd(StringUtils.trimToEmpty(baseUrl), "/");
		this.httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
	}

	/**
	 * Leichter Verbindungstest: listet die zum API-Key gehörenden Turniere; wirft bei fehlendem/
	 * ungültigem Key oder Netzwerkfehler eine {@link PtmOnlineException}.
	 */
	public void pruefeVerbindung() throws PtmOnlineException {
		senden(request("GET", "/api/sync/tournaments", null));
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
}
