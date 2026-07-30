/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.whatsapp;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import de.petanqueturniermanager.helper.i18n.I18n;

public class WhatsAppBridgeClient {

	private static final Gson GSON = new Gson();
	private static final Duration TIMEOUT = Duration.ofSeconds(15);

	private final HttpClient httpClient;
	private final URI baseUri;
	private final boolean lokaleUri;

	public WhatsAppBridgeClient(URI baseUri) {
		this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(), baseUri);
	}

	WhatsAppBridgeClient(HttpClient httpClient, URI baseUri) {
		this.httpClient = httpClient;
		this.baseUri = baseUri == null ? URI.create("http://127.0.0.1:0") : baseUri;
		this.lokaleUri = istLokaleUri(this.baseUri);
	}

	public WhatsAppBridgeStatus status() throws WhatsAppBridgeException {
		JsonObject json = getJson("/status");
		return new WhatsAppBridgeStatus(string(json, "status"), string(json, "qr"));
	}

	public List<WhatsAppBridgeChat> chats() throws WhatsAppBridgeException {
		JsonObject json = getJson("/groups");
		JsonArray chats = json.has("chats") && json.get("chats").isJsonArray()
				? json.getAsJsonArray("chats")
				: new JsonArray();
		return chats.asList().stream()
				.filter(e -> e.isJsonObject())
				.map(e -> e.getAsJsonObject())
				.map(o -> new WhatsAppBridgeChat(string(o, "id"), string(o, "name"), string(o, "type")))
				.filter(c -> !c.id().isBlank())
				.toList();
	}

	public void sendeBild(String chatId, String caption, byte[] pngBytes) throws WhatsAppBridgeException {
		JsonObject payload = new JsonObject();
		payload.addProperty("chatId", chatId);
		payload.addProperty("caption", caption == null ? "" : caption);
		payload.addProperty("imageBase64", Base64.getEncoder().encodeToString(pngBytes));
		postJson("/send-image", payload);
	}

	public URI qrCodeUri() {
		return baseUri.resolve("/qr");
	}

	private JsonObject getJson(String path) throws WhatsAppBridgeException {
		pruefeLokaleUri();
		HttpRequest request = HttpRequest.newBuilder(baseUri.resolve(path))
				.timeout(TIMEOUT)
				.GET()
				.build();
		return send(request);
	}

	private void postJson(String path, JsonObject payload) throws WhatsAppBridgeException {
		pruefeLokaleUri();
		HttpRequest request = HttpRequest.newBuilder(baseUri.resolve(path))
				.timeout(TIMEOUT)
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload)))
				.build();
		send(request);
	}

	private JsonObject send(HttpRequest request) throws WhatsAppBridgeException {
		try {
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() == 409) {
				throw new WhatsAppBridgeException(
						I18n.get("whatsapp.bridge.nicht.bereit", statusAusFehlerBody(response.body())));
			}
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				throw new WhatsAppBridgeException("WhatsApp-Bridge HTTP " + response.statusCode() + ": "
						+ kuerze(response.body()));
			}
			return JsonParser.parseString(response.body()).getAsJsonObject();
		} catch (IOException e) {
			throw new WhatsAppBridgeException("WhatsApp-Bridge nicht erreichbar: " + e.getMessage(), e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new WhatsAppBridgeException("WhatsApp-Bridge-Aufruf wurde unterbrochen", e);
		} catch (RuntimeException e) {
			throw new WhatsAppBridgeException("WhatsApp-Bridge-Antwort ist ungültig: " + e.getMessage(), e);
		}
	}

	private void pruefeLokaleUri() throws WhatsAppBridgeException {
		if (!lokaleUri) {
			throw new WhatsAppBridgeException("WhatsApp-Bridge darf nur lokal angebunden werden: " + baseUri);
		}
	}

	private static boolean istLokaleUri(URI uri) {
		String host = uri.getHost();
		return "127.0.0.1".equals(host) || "localhost".equalsIgnoreCase(host);
	}

	private static String string(JsonObject json, String name) {
		return json.has(name) && !json.get(name).isJsonNull() ? json.get(name).getAsString() : "";
	}

	private static String kuerze(String body) {
		if (body == null) {
			return "";
		}
		String einzeilig = body.replace("\r", " ").replace("\n", " ");
		return einzeilig.length() <= 200 ? einzeilig : einzeilig.substring(0, 200);
	}

	private static String statusAusFehlerBody(String body) {
		try {
			return string(JsonParser.parseString(body).getAsJsonObject(), "status");
		} catch (JsonSyntaxException | IllegalStateException e) {
			return "unbekannt";
		}
	}
}
