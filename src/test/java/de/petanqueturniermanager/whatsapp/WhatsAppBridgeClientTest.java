package de.petanqueturniermanager.whatsapp;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

class WhatsAppBridgeClientTest {

	private HttpServer server;

	@AfterEach
	void stopServer() {
		if (server != null) {
			server.stop(0);
		}
	}

	@Test
	void liestStatusUndChatsUndSendetBild() throws Exception {
		var gesendeterBody = new StringBuilder();
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/status", exchange -> antwort(exchange, "{\"status\":\"ready\",\"qr\":\"\"}"));
		server.createContext("/groups", exchange -> antwort(exchange,
				"{\"chats\":[{\"id\":\"123@g.us\",\"name\":\"Turnier\",\"type\":\"Gruppe\"}]}"));
		server.createContext("/send-image", exchange -> {
			gesendeterBody.append(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
			antwort(exchange, "{\"ok\":true}");
		});
		server.start();

		var client = new WhatsAppBridgeClient(URI.create("http://127.0.0.1:" + server.getAddress().getPort()));

		assertThat(client.status().verbunden()).isTrue();
		assertThat(client.chats())
				.extracting(WhatsAppBridgeChat::id)
				.containsExactly("123@g.us");

		client.sendeBild("123@g.us", "Rangliste", new byte[] { 1, 2, 3 });

		assertThat(gesendeterBody.toString())
				.contains("\"chatId\":\"123@g.us\"")
				.contains("\"caption\":\"Rangliste\"")
				.contains("\"imageBase64\":\"AQID\"");
	}

	private static void antwort(com.sun.net.httpserver.HttpExchange exchange, String json) throws IOException {
		byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().set("Content-Type", "application/json");
		exchange.sendResponseHeaders(200, bytes.length);
		exchange.getResponseBody().write(bytes);
		exchange.close();
	}
}
