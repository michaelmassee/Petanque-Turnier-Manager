/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.onlinesync;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.helper.i18n.I18n;

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
	private static final Set<String> ERLAUBTE_SCHEMATA = Set.of("https", "http");
	private static final int HTTP_ERFOLG_MIN = 200;
	private static final int HTTP_ERFOLG_MAX = 299;
	private static final int MAX_LAENGE_ANTWORT = 500;

	private final String apiKey;
	private final String baseUrl;
	/** Basis-URL wie eingetragen, nur für Fehlermeldungen. */
	private final String eingetrageneBaseUrl;
	private final HttpClient httpClient;

	public PtmOnlineApiClient(String apiKey, String baseUrl) {
		this.apiKey = StringUtils.trimToEmpty(apiKey);
		this.eingetrageneBaseUrl = StringUtils.trimToEmpty(baseUrl);
		this.baseUrl = StringUtils.stripEnd(eingetrageneBaseUrl, "/");
		this.httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
	}

	/** Leichter Verbindungstest: listet die eigenen Turniere; wirft bei Fehler eine {@link PtmOnlineException}. */
	public void pruefeVerbindung() throws PtmOnlineException {
		senden(request("/api/sync/tournaments"));
	}

	private HttpRequest request(String pfad) throws PtmOnlineException {
		if (baseUrl.isEmpty()) {
			throw new PtmOnlineException(I18n.get("ptmonline.api.fehler.keine_basis_url"));
		}
		if (apiKey.isEmpty()) {
			throw new PtmOnlineException(I18n.get("ptmonline.api.fehler.kein_apikey"));
		}
		return HttpRequest.newBuilder(zielAdresse(pfad)).timeout(TIMEOUT)
				.header("Authorization", "Bearer " + apiKey).header("Accept", "application/json")
				.method("GET", BodyPublishers.noBody()).build();
	}

	/**
	 * Absolute http(s)-Adresse mit Host. {@link HttpRequest#newBuilder(URI)} wirft sonst eine ungeprüfte
	 * {@link IllegalArgumentException} – z.&nbsp;B. für „ptmonline.org“ ohne Protokoll.
	 */
	private URI zielAdresse(String pfad) throws PtmOnlineException {
		try {
			URI adresse = new URI(baseUrl + pfad);
			String schema = adresse.getScheme() == null ? "" : adresse.getScheme().toLowerCase(Locale.ROOT);
			if (ERLAUBTE_SCHEMATA.contains(schema) && adresse.getHost() != null) {
				return adresse;
			}
		} catch (URISyntaxException e) {
			logger.debug("Ungültige PTM-Online-Basis-URL {}", baseUrl, e);
		}
		throw new PtmOnlineException(I18n.get("ptmonline.api.fehler.ungueltige_basis_url", eingetrageneBaseUrl));
	}

	private void senden(HttpRequest request) throws PtmOnlineException {
		try {
			HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
			if (response.statusCode() < HTTP_ERFOLG_MIN || response.statusCode() > HTTP_ERFOLG_MAX) {
				throw new PtmOnlineException(I18n.get("ptmonline.api.fehler.http", response.statusCode(),
						StringUtils.abbreviate(response.body(), MAX_LAENGE_ANTWORT)));
			}
		} catch (IOException e) {
			logger.debug("Netzwerkfehler bei PTM-Online-Anfrage {}", request.uri(), e);
			throw new PtmOnlineException(I18n.get("ptmonline.api.fehler.netzwerk", e.getMessage()), e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new PtmOnlineException(I18n.get("ptmonline.api.fehler.unterbrochen"), e);
		}
	}
}
