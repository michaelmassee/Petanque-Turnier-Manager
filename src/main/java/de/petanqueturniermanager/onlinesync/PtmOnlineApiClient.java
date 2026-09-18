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
 * HTTP-Client für die PTM-Online-REST-API (Cloudflare-Worker-App, siehe
 * {@code Petanque-Turnier-Manager-Online/SECURITY.md}). Auth per API-Key
 * ({@code Authorization: Bearer ptm_...}).
 * <p>
 * Deckt hier nur den Verbindungstest der Optionsseite ab (siehe
 * {@code de.petanqueturniermanager.comp.PtmOnlineOptionsEventHandler}), nicht die Turnier-Synchronisation.
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

	/** Leichter Verbindungstest: listet die eigenen Turniere; wirft bei Fehler eine {@link PtmOnlineException}. */
	public void pruefeVerbindung() throws PtmOnlineException {
		senden(request("/api/sync/tournaments"));
	}

	private HttpRequest request(String pfad) throws PtmOnlineException {
		if (baseUrl.isEmpty()) {
			throw new PtmOnlineException("Keine PTM-Online-Basis-URL konfiguriert (Extras > Optionen > PétTurnMngr > PTM Online)");
		}
		if (apiKey.isEmpty()) {
			throw new PtmOnlineException("Kein PTM-Online-API-Key konfiguriert (Extras > Optionen > PétTurnMngr > PTM Online)");
		}
		return HttpRequest.newBuilder(URI.create(baseUrl + pfad)).timeout(TIMEOUT)
				.header("Authorization", "Bearer " + apiKey).header("Accept", "application/json")
				.method("GET", BodyPublishers.noBody()).build();
	}

	private void senden(HttpRequest request) throws PtmOnlineException {
		try {
			HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				throw new PtmOnlineException("PTM Online antwortete mit HTTP " + response.statusCode() + ": "
						+ StringUtils.abbreviate(response.body(), 500));
			}
		} catch (IOException e) {
			logger.debug("Netzwerkfehler bei PTM-Online-Anfrage {}", request.uri(), e);
			throw new PtmOnlineException("Netzwerkfehler bei Zugriff auf PTM Online: " + e.getMessage(), e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new PtmOnlineException("PTM-Online-Anfrage wurde unterbrochen", e);
		}
	}
}
