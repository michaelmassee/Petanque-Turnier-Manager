/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.whatsapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class WhatsAppBridgeManager {

	private static final Logger logger = LogManager.getLogger(WhatsAppBridgeManager.class);

	private static final int PORT = Integer.getInteger("ptm.whatsapp.bridge.port", 9215);

	/** Anzahl der zuletzt vom Bridge-Prozess gelesenen Ausgabezeilen, die für Fehlermeldungen vorgehalten werden. */
	private static final int MAX_AUSGABE_ZEILEN = 40;

	private static volatile Process process;

	/** stdout/stderr (zusammengeführt) des zuletzt gestarteten Bridge-Prozesses, für Diagnose bei Fehlschlag. */
	private static final Deque<String> letzteAusgabe = new ArrayDeque<>();

	private WhatsAppBridgeManager() {
	}

	public static WhatsAppBridgeClient client() {
		return new WhatsAppBridgeClient(URI.create("http://127.0.0.1:" + PORT));
	}

	public static WhatsAppBridgeClient starteOderVerbinde() throws WhatsAppBridgeException {
		var client = client();
		if (istErreichbar(client)) {
			return client;
		}
		starten();
		Process gestarteterProzess = process;
		long ende = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < ende) {
			if (istErreichbar(client)) {
				return client;
			}
			if (!gestarteterProzess.isAlive()) {
				throw new WhatsAppBridgeException("WhatsApp-Bridge-Prozess wurde beendet (Exit-Code "
						+ gestarteterProzess.exitValue() + "), bevor Port " + PORT + " erreichbar war."
						+ ausgabeAnhang());
			}
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new WhatsAppBridgeException("WhatsApp-Bridge-Start wurde unterbrochen", e);
			}
		}
		throw new WhatsAppBridgeException("WhatsApp-Bridge wurde gestartet, antwortet aber nicht auf Port "
				+ PORT + "." + ausgabeAnhang());
	}

	private static String ausgabeAnhang() {
		String ausgabe;
		synchronized (letzteAusgabe) {
			ausgabe = String.join("\n", letzteAusgabe);
		}
		return ausgabe.isBlank() ? "" : "\nAusgabe der Bridge:\n" + ausgabe;
	}

	private static boolean istErreichbar(WhatsAppBridgeClient client) {
		try {
			client.status();
			return true;
		} catch (WhatsAppBridgeException e) {
			return false;
		}
	}

	private static synchronized void starten() throws WhatsAppBridgeException {
		if (process != null && process.isAlive()) {
			return;
		}
		Path script = bridgeScript();
		if (!Files.isRegularFile(script)) {
			throw new WhatsAppBridgeException("WhatsApp-Bridge-Skript nicht gefunden: " + script);
		}
		try {
			Path sessionDir = userConfigDir().resolve("whatsapp").resolve("session");
			Files.createDirectories(sessionDir);
			ProcessBuilder builder = new ProcessBuilder("node", script.toString());
			builder.environment().put("PTM_WA_PORT", String.valueOf(PORT));
			builder.environment().put("PTM_WA_SESSION_DIR", sessionDir.toString());
			Path scriptDir = script.getParent();
			if (scriptDir != null) {
				builder.directory(scriptDir.toFile());
			}
			builder.redirectErrorStream(true);
			process = builder.start();
			synchronized (letzteAusgabe) {
				letzteAusgabe.clear();
			}
			leseAusgabeImHintergrund(process);
			logger.info("WhatsApp-Bridge gestartet: {}", script);
		} catch (IOException e) {
			throw new WhatsAppBridgeException("WhatsApp-Bridge konnte nicht gestartet werden. Bitte Node.js und die Bridge-Abhängigkeiten installieren: "
					+ e.getMessage(), e);
		}
	}

	/**
	 * Liest stdout/stderr des Bridge-Prozesses (per {@code redirectErrorStream} zusammengeführt) fortlaufend
	 * in einem Daemon-Thread mit. Ohne diesen Reader verschwindet die Ausgabe stillschweigend im Pipe-Puffer
	 * des Betriebssystems – bei einem Absturz (z.B. fehlende node_modules) blieb dem Anwender bisher nur der
	 * nichtssagende Timeout in {@link #starteOderVerbinde()}, ohne die eigentliche Fehlerursache.
	 */
	private static void leseAusgabeImHintergrund(Process bridgeProcess) {
		Thread thread = new Thread(() -> {
			try (var reader = new BufferedReader(
					new InputStreamReader(bridgeProcess.getInputStream(), StandardCharsets.UTF_8))) {
				String zeile;
				while ((zeile = reader.readLine()) != null) {
					logger.debug("WhatsApp-Bridge: {}", zeile);
					synchronized (letzteAusgabe) {
						letzteAusgabe.addLast(zeile);
						while (letzteAusgabe.size() > MAX_AUSGABE_ZEILEN) {
							letzteAusgabe.removeFirst();
						}
					}
				}
			} catch (IOException e) {
				logger.debug("WhatsApp-Bridge-Ausgabe konnte nicht mehr gelesen werden (Prozess vermutlich beendet)", e);
			}
		}, "PTM-WhatsApp-Bridge-Ausgabe");
		thread.setDaemon(true);
		thread.start();
	}

	private static Path bridgeScript() {
		String override = System.getProperty("ptm.whatsapp.bridge.script", "");
		if (!override.isBlank()) {
			return Path.of(override);
		}
		Path projektPfad = Path.of("tools", "whatsapp-bridge", "server.js").toAbsolutePath().normalize();
		if (Files.isRegularFile(projektPfad)) {
			return projektPfad;
		}
		Path installPfad = installationsVerzeichnis().resolve("tools").resolve("whatsapp-bridge").resolve("server.js");
		return installPfad.toAbsolutePath().normalize();
	}

	private static Path installationsVerzeichnis() {
		try {
			URI codeSource = WhatsAppBridgeManager.class.getProtectionDomain()
					.getCodeSource()
					.getLocation()
					.toURI();
			Path pfad = Path.of(codeSource);
			Path parent = Files.isRegularFile(pfad) ? pfad.getParent() : pfad;
			return parent == null ? Path.of(".").toAbsolutePath() : parent;
		} catch (URISyntaxException | RuntimeException e) {
			logger.debug("Installationsverzeichnis der WhatsApp-Bridge konnte nicht ermittelt werden", e);
			return Path.of(".").toAbsolutePath();
		}
	}

	private static Path userConfigDir() {
		String home = System.getProperty("user.home", ".");
		String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		if (os.contains("win")) {
			String appData = System.getenv("APPDATA");
			if (appData != null && !appData.isBlank()) {
				return Path.of(appData, "PetanqueTurnierManager");
			}
		}
		return Path.of(home, ".petanqueturniermanager");
	}
}
