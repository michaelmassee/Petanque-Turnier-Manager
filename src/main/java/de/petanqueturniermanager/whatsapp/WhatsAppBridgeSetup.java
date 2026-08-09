/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.whatsapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class WhatsAppBridgeSetup {

	private static final Logger logger = LogManager.getLogger(WhatsAppBridgeSetup.class);

	static final String NODE_VERSION = System.getProperty("ptm.whatsapp.node.version", "22.22.2");
	// von @whiskeysockets/baileys per "engines" in package-lock.json gefordert
	private static final int MIN_NODE_MAJOR_VERSION = 20;
	private static final String DOWNLOAD_BASE_URL = System.getProperty("ptm.whatsapp.node.download.baseUrl",
			"https://nodejs.org/dist");
	private static final Duration VERSION_CHECK_TIMEOUT = Duration.ofSeconds(5);
	private static final Duration NPM_INSTALL_TIMEOUT = Duration.ofMinutes(5);
	private static final Duration TAR_ENTPACKEN_TIMEOUT = Duration.ofSeconds(60);
	private static final Duration TAR_PRUEFEN_TIMEOUT = Duration.ofSeconds(30);
	private static final Duration DOWNLOAD_VERBINDUNGS_TIMEOUT = Duration.ofSeconds(10);
	private static final Duration DOWNLOAD_TIMEOUT = Duration.ofMinutes(5);

	public enum Schritt {
		NODE_DOWNLOAD, NODE_INSTALL, BRIDGE_INSTALL, FERTIG
	}

	record NodePaket(String os, String arch, String dateiname, String rootVerzeichnis, Path executableRelativ) {
		String url() {
			return DOWNLOAD_BASE_URL + "/v" + NODE_VERSION + "/" + dateiname;
		}
	}

	private WhatsAppBridgeSetup() {
	}

	static Path nodePfadOderSetupNoetig() throws WhatsAppBridgeException {
		return nodePfadOderSetupNoetig(WhatsAppBridgeManager.userConfigDir(), System.getenv("PATH"));
	}

	static Path nodePfadOderSetupNoetig(Path userConfigDir, String pathEnv) throws WhatsAppBridgeException {
		try {
			Path node = findeNode(userConfigDir, pathEnv);
			if (node != null) {
				return node;
			}
		} catch (IllegalStateException e) {
			throw new WhatsAppBridgeException(e.getMessage(), e);
		}
		throw new WhatsAppBridgeSetupRequiredException(
				"Node.js wurde nicht gefunden. Bitte WhatsApp-Unterstützung einrichten.");
	}

	public static void vorbereitenWennMoeglich(Consumer<Schritt> fortschritt) throws WhatsAppBridgeException {
		Path node = nodePfadOderSetupNoetig();
		bereitstellenBridge(node, fortschritt);
	}

	public static void installieren(Consumer<Schritt> fortschritt) throws WhatsAppBridgeException {
		Path node = installierePortableNode(fortschritt);
		bereitstellenBridge(node, fortschritt);
		fortschritt(fortschritt, Schritt.FERTIG);
	}

	static Path installierePortableNode(Consumer<Schritt> fortschritt) throws WhatsAppBridgeException {
		try {
			Path vorhandenerNode = findeNodeInRuntime(WhatsAppBridgeManager.userConfigDir());
			if (vorhandenerNode != null) {
				return vorhandenerNode;
			}
			NodePaket paket = paketFuerAktuellePlattform();
			Path runtimeDir = runtimeDir();
			Path zielDir = runtimeDir.resolve(paket.rootVerzeichnis());
			Path node = zielDir.resolve(paket.executableRelativ());
			Files.createDirectories(runtimeDir);
			Path download = runtimeDir.resolve(paket.dateiname());
			fortschritt(fortschritt, Schritt.NODE_DOWNLOAD);
			download(paket.url(), download);
			pruefePruefsumme(download, paket.dateiname());
			fortschritt(fortschritt, Schritt.NODE_INSTALL);
			if (paket.dateiname().endsWith(".zip")) {
				entpackeZip(download, runtimeDir);
			} else {
				entpackeTar(download, runtimeDir);
			}
			setzeAusfuehrbar(node);
			if (!istAusfuehrbarerNode(node)) {
				throw new WhatsAppBridgeException("Portable Node.js wurde installiert, kann aber nicht gestartet werden: " + node);
			}
			return node;
		} catch (IOException e) {
			throw new WhatsAppBridgeException("Portable Node.js konnte nicht installiert werden: " + e.getMessage(), e);
		} catch (IllegalStateException e) {
			throw new WhatsAppBridgeException(e.getMessage(), e);
		}
	}

	static Path bridgeScriptBereitstellen(Path node, Consumer<Schritt> fortschritt) throws WhatsAppBridgeException {
		return bereitstellenBridge(node, fortschritt).resolve("server.js");
	}

	private static Path bereitstellenBridge(Path node, Consumer<Schritt> fortschritt) throws WhatsAppBridgeException {
		Path sourceDir = WhatsAppBridgeManager.bridgeSourceDir();
		if (!Files.isRegularFile(sourceDir.resolve("server.js"))) {
			throw new WhatsAppBridgeException("WhatsApp-Bridge-Skript nicht gefunden: " + sourceDir.resolve("server.js"));
		}
		if (bridgeAbhaengigkeitenVorhanden(sourceDir)) {
			return sourceDir;
		}
		Path runtimeBridgeDir = WhatsAppBridgeManager.userConfigDir().resolve("whatsapp").resolve("bridge");
		try {
			Files.createDirectories(runtimeBridgeDir);
			kopiereWennVorhanden(sourceDir.resolve("server.js"), runtimeBridgeDir.resolve("server.js"));
			kopiereWennVorhanden(sourceDir.resolve("package.json"), runtimeBridgeDir.resolve("package.json"));
			kopiereWennVorhanden(sourceDir.resolve("package-lock.json"), runtimeBridgeDir.resolve("package-lock.json"), false);
		} catch (IOException e) {
			throw new WhatsAppBridgeException("WhatsApp-Bridge konnte nicht in den Benutzerordner kopiert werden: "
					+ e.getMessage(), e);
		}
		if (!bridgeAbhaengigkeitenVorhanden(runtimeBridgeDir)) {
			fortschritt(fortschritt, Schritt.BRIDGE_INSTALL);
			installiereBridgeAbhaengigkeiten(node, runtimeBridgeDir);
		}
		if (!bridgeAbhaengigkeitenVorhanden(runtimeBridgeDir)) {
			throw new WhatsAppBridgeException("WhatsApp-Bridge-Abhängigkeiten wurden nicht vollständig installiert: "
					+ runtimeBridgeDir.resolve("node_modules"));
		}
		return runtimeBridgeDir;
	}

	private static void installiereBridgeAbhaengigkeiten(Path node, Path bridgeDir) throws WhatsAppBridgeException {
		Path npm = findeNpm(node);
		if (npm == null) {
			throw new WhatsAppBridgeException("npm wurde nicht gefunden. Die WhatsApp-Bridge kann nicht installiert werden.");
		}
		ProcessBuilder builder = new ProcessBuilder(npm.toString(), "install", "--omit=dev");
		builder.directory(bridgeDir.toFile());
		builder.redirectErrorStream(true);
		try {
			Process process = builder.start();
			TimeoutWaechter waechter = new TimeoutWaechter(process, NPM_INSTALL_TIMEOUT);
			String ausgabe = liesAusgabe(process.getInputStream());
			process.waitFor();
			waechter.stoppen();
			if (waechter.zeitUeberschritten()) {
				throw new WhatsAppBridgeException("npm install für die WhatsApp-Bridge hat zu lange gedauert.");
			}
			if (process.exitValue() != 0) {
				throw new WhatsAppBridgeException("WhatsApp-Bridge-Abhängigkeiten konnten nicht installiert werden (Exit-Code "
						+ process.exitValue() + ").\n" + ausgabe);
			}
		} catch (IOException e) {
			throw new WhatsAppBridgeException("npm konnte nicht gestartet werden: " + e.getMessage(), e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new WhatsAppBridgeException("Installation der WhatsApp-Bridge wurde unterbrochen", e);
		}
	}

	private static Path findeNode(Path userConfigDir, String pathEnv) {
		String override = System.getProperty("ptm.whatsapp.node.path", "");
		if (!override.isBlank() && istAusfuehrbarerNode(Path.of(override))) {
			return Path.of(override);
		}
		String envOverride = System.getenv("PTM_WHATSAPP_NODE");
		if (envOverride != null && !envOverride.isBlank() && istAusfuehrbarerNode(Path.of(envOverride))) {
			return Path.of(envOverride);
		}
		Path runtimeNode = findeNodeInRuntime(userConfigDir);
		if (runtimeNode != null) {
			return runtimeNode;
		}
		return findeInPath("node", pathEnv);
	}

	private static Path findeNodeInRuntime(Path userConfigDir) {
		Path runtime = userConfigDir.resolve("whatsapp").resolve("runtime");
		NodePaket paket = paketFuerAktuellePlattform();
		Path node = runtime.resolve(paket.rootVerzeichnis()).resolve(paket.executableRelativ());
		return istAusfuehrbarerNode(node) ? node : null;
	}

	private static Path findeNpm(Path node) {
		Path parent = node.toAbsolutePath().getParent();
		if (parent != null) {
			String npmName = istWindows() ? "npm.cmd" : "npm";
			Path nebenNode = parent.resolve(npmName);
			if (Files.isRegularFile(nebenNode)) {
				return nebenNode;
			}
		}
		return findeInPath("npm", System.getenv("PATH"));
	}

	private static Path findeInPath(String name, String pathEnv) {
		if (pathEnv == null || pathEnv.isBlank()) {
			return null;
		}
		String[] namen = istWindows() ? new String[] { name + ".exe", name + ".cmd", name + ".bat", name }
				: new String[] { name };
		for (String verzeichnis : pathEnv.split(java.io.File.pathSeparator)) {
			if (verzeichnis.isBlank()) {
				continue;
			}
			for (String kandidatName : namen) {
				Path kandidat = Path.of(verzeichnis, kandidatName);
				if (istAusfuehrbarerNodeName(name, kandidatName) && istAusfuehrbarerNode(kandidat)) {
					return kandidat;
				}
				if (!"node".equals(name) && Files.isRegularFile(kandidat)) {
					return kandidat;
				}
			}
		}
		return null;
	}

	private static boolean istAusfuehrbarerNodeName(String name, String kandidatName) {
		return "node".equals(name) && kandidatName.toLowerCase(Locale.ROOT).startsWith("node");
	}

	private static boolean istAusfuehrbarerNode(Path node) {
		if (!Files.isRegularFile(node)) {
			return false;
		}
		ProcessBuilder builder = new ProcessBuilder(node.toString(), "--version");
		builder.redirectErrorStream(true);
		try {
			Process process = builder.start();
			TimeoutWaechter waechter = new TimeoutWaechter(process, VERSION_CHECK_TIMEOUT);
			String ausgabe = liesAusgabe(process.getInputStream());
			process.waitFor();
			waechter.stoppen();
			if (waechter.zeitUeberschritten() || process.exitValue() != 0) {
				return false;
			}
			return erfuelltNodeMindestversion(ausgabe);
		} catch (IOException e) {
			logger.debug("Node-Kandidat konnte nicht gestartet werden: {}", node, e);
			return false;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	static boolean erfuelltNodeMindestversion(String versionsAusgabe) {
		String ersteZeile = versionsAusgabe.lines().findFirst().orElse("").trim();
		String ohnePraefix = ersteZeile.startsWith("v") ? ersteZeile.substring(1) : ersteZeile;
		int punkt = ohnePraefix.indexOf('.');
		String major = punkt >= 0 ? ohnePraefix.substring(0, punkt) : ohnePraefix;
		try {
			return Integer.parseInt(major) >= MIN_NODE_MAJOR_VERSION;
		} catch (NumberFormatException e) {
			logger.debug("Node-Version konnte nicht ermittelt werden aus: '{}'", versionsAusgabe);
			return false;
		}
	}

	static boolean bridgeAbhaengigkeitenVorhanden(Path bridgeDir) {
		Path nodeModules = bridgeDir.resolve("node_modules");
		return Files.isDirectory(nodeModules.resolve("@whiskeysockets").resolve("baileys"))
				&& Files.isDirectory(nodeModules.resolve("express"))
				&& Files.isDirectory(nodeModules.resolve("qrcode"));
	}

	static NodePaket paketFuerAktuellePlattform() {
		String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		String arch = normalisierteArchitektur(System.getProperty("os.arch", ""));
		try {
			if (os.contains("win")) {
				return paket("win", arch, ".zip", Path.of("node.exe"));
			}
			if (os.contains("mac") || os.contains("darwin")) {
				return paket("darwin", arch, ".tar.gz", Path.of("bin", "node"));
			}
			if (os.contains("linux")) {
				return paket("linux", arch, ".tar.xz", Path.of("bin", "node"));
			}
			throw new IllegalStateException("Nicht unterstützte WhatsApp-Node-Plattform: " + os + " / " + arch);
		} catch (IllegalStateException e) {
			throw new IllegalStateException("WhatsApp-Unterstützung kann für diese Plattform nicht automatisch eingerichtet werden: "
					+ os + " / " + arch, e);
		}
	}

	private static NodePaket paket(String os, String arch, String endung, Path executableRelativ) {
		String root = "node-v" + NODE_VERSION + "-" + os + "-" + arch;
		return new NodePaket(os, arch, root + endung, root, executableRelativ);
	}

	private static String normalisierteArchitektur(String arch) {
		String wert = arch.toLowerCase(Locale.ROOT);
		if (wert.equals("amd64") || wert.equals("x86_64")) {
			return "x64";
		}
		if (wert.equals("aarch64") || wert.equals("arm64")) {
			return "arm64";
		}
		throw new IllegalStateException("Nicht unterstützte WhatsApp-Node-Architektur: " + arch);
	}

	private static void download(String url, Path ziel) throws IOException {
		HttpClient client = HttpClient.newBuilder().connectTimeout(DOWNLOAD_VERBINDUNGS_TIMEOUT).build();
		HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(DOWNLOAD_TIMEOUT).GET().build();
		try {
			HttpResponse<Path> response = client.send(request,
					HttpResponse.BodyHandlers.ofFile(ziel, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
			if (response.statusCode() != 200) {
				throw new IOException("Download fehlgeschlagen (HTTP " + response.statusCode() + "): " + url);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Download wurde unterbrochen: " + url, e);
		}
	}

	private static void pruefePruefsumme(Path datei, String dateiname) throws IOException, WhatsAppBridgeException {
		String checksummenUrl = DOWNLOAD_BASE_URL + "/v" + NODE_VERSION + "/SHASUMS256.txt";
		String erwarteterHash = ladeErwarteteChecksumme(checksummenUrl, dateiname);
		String tatsaechlicherHash = sha256Hex(datei);
		if (!erwarteterHash.equalsIgnoreCase(tatsaechlicherHash)) {
			Files.deleteIfExists(datei);
			throw new WhatsAppBridgeException(
					"Prüfsumme von " + dateiname + " stimmt nicht überein. Download wurde verworfen.");
		}
	}

	private static String ladeErwarteteChecksumme(String url, String dateiname) throws IOException {
		HttpClient client = HttpClient.newBuilder().connectTimeout(DOWNLOAD_VERBINDUNGS_TIMEOUT).build();
		HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(DOWNLOAD_TIMEOUT).GET().build();
		HttpResponse<String> response;
		try {
			response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Laden der Prüfsummen wurde unterbrochen: " + url, e);
		}
		if (response.statusCode() != 200) {
			throw new IOException("Prüfsummen konnten nicht geladen werden (HTTP " + response.statusCode() + "): " + url);
		}
		return response.body().lines()
				.map(String::trim)
				.filter(zeile -> zeile.endsWith(dateiname))
				.map(zeile -> zeile.split("\\s+")[0])
				.findFirst()
				.orElseThrow(() -> new IOException("Keine Prüfsumme für " + dateiname + " gefunden in " + url));
	}

	private static String sha256Hex(Path datei) throws IOException {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			try (InputStream in = Files.newInputStream(datei)) {
				byte[] puffer = new byte[8192];
				int gelesen;
				while ((gelesen = in.read(puffer)) != -1) {
					digest.update(puffer, 0, gelesen);
				}
			}
			return HexFormat.of().formatHex(digest.digest());
		} catch (NoSuchAlgorithmException e) {
			throw new IOException("SHA-256 ist nicht verfügbar", e);
		}
	}

	static void entpackeZip(Path zip, Path zielDir) throws IOException {
		try (ZipInputStream zipInput = new ZipInputStream(Files.newInputStream(zip))) {
			ZipEntry entry;
			while ((entry = zipInput.getNextEntry()) != null) {
				Path ziel = sicheresZiel(zielDir, entry.getName());
				if (entry.isDirectory()) {
					Files.createDirectories(ziel);
				} else {
					Path parent = ziel.getParent();
					if (parent != null) {
						Files.createDirectories(parent);
					}
					Files.copy(zipInput, ziel, StandardCopyOption.REPLACE_EXISTING);
				}
			}
		}
	}

	private static void entpackeTar(Path archiv, Path zielDir) throws IOException, WhatsAppBridgeException {
		pruefeTarEintraege(archiv, zielDir);
		ProcessBuilder builder = new ProcessBuilder("tar", "-xf", archiv.toString(), "-C", zielDir.toString());
		builder.redirectErrorStream(true);
		try {
			Process process = builder.start();
			TimeoutWaechter waechter = new TimeoutWaechter(process, TAR_ENTPACKEN_TIMEOUT);
			String ausgabe = liesAusgabe(process.getInputStream());
			process.waitFor();
			waechter.stoppen();
			if (waechter.zeitUeberschritten()) {
				throw new WhatsAppBridgeException("Node.js-Archiv konnte nicht entpackt werden: tar hat zu lange gedauert.");
			}
			if (process.exitValue() != 0) {
				throw new WhatsAppBridgeException("Node.js-Archiv konnte nicht entpackt werden (tar Exit-Code "
						+ process.exitValue() + ").\n" + ausgabe);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new WhatsAppBridgeException("Entpacken der Node.js-Runtime wurde unterbrochen", e);
		}
	}

	private static void pruefeTarEintraege(Path archiv, Path zielDir) throws IOException, WhatsAppBridgeException {
		ProcessBuilder builder = new ProcessBuilder("tar", "-tf", archiv.toString());
		builder.redirectErrorStream(true);
		try {
			Process process = builder.start();
			TimeoutWaechter waechter = new TimeoutWaechter(process, TAR_PRUEFEN_TIMEOUT);
			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
				String zeile;
				while ((zeile = reader.readLine()) != null) {
					if (!zeile.isBlank()) {
						sicheresZiel(zielDir, zeile);
					}
				}
			}
			process.waitFor();
			waechter.stoppen();
			if (waechter.zeitUeberschritten() || process.exitValue() != 0) {
				throw new WhatsAppBridgeException("Node.js-Archiv konnte nicht geprüft werden.");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new WhatsAppBridgeException("Prüfung der Node.js-Runtime wurde unterbrochen", e);
		}
	}

	private static Path sicheresZiel(Path zielDir, String entryName) throws IOException {
		Path ziel = zielDir.resolve(entryName).normalize();
		if (!ziel.startsWith(zielDir.normalize()) || Path.of(entryName).isAbsolute() || entryName.contains("..")) {
			throw new IOException("Unsicherer Archiv-Eintrag: " + entryName);
		}
		return ziel;
	}

	private static void setzeAusfuehrbar(Path node) {
		try {
			if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
				Files.setPosixFilePermissions(node, EnumSet.of(PosixFilePermission.OWNER_READ,
						PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE,
						PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_EXECUTE,
						PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_EXECUTE));
			}
		} catch (IOException | UnsupportedOperationException e) {
			logger.debug("Ausführbar-Bit für portable Node.js konnte nicht gesetzt werden: {}", node, e);
		}
	}

	private static void kopiereWennVorhanden(Path quelle, Path ziel) throws IOException {
		kopiereWennVorhanden(quelle, ziel, true);
	}

	private static void kopiereWennVorhanden(Path quelle, Path ziel, boolean erforderlich) throws IOException {
		if (!Files.isRegularFile(quelle)) {
			if (erforderlich) {
				throw new IOException("Datei fehlt: " + quelle);
			}
			return;
		}
		Files.copy(quelle, ziel, StandardCopyOption.REPLACE_EXISTING);
	}

	private static boolean istWindows() {
		return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
	}

	private static Path runtimeDir() {
		return WhatsAppBridgeManager.userConfigDir().resolve("whatsapp").resolve("runtime");
	}

	/**
	 * Erzwingt einen Prozess-Timeout unabhängig vom (potenziell blockierenden) Lesen der Prozessausgabe:
	 * ein Wächter-Thread wartet parallel mit {@link Process#waitFor(long, TimeUnit)} und beendet den
	 * Prozess bei Überschreitung zwangsweise, damit ein blockierendes {@code readLine()} im Aufrufer-Thread
	 * nicht das komplette Timeout aushebelt.
	 */
	private static final class TimeoutWaechter {
		private final Thread thread;
		private volatile boolean zeitUeberschritten;

		TimeoutWaechter(Process process, Duration timeout) {
			this.thread = new Thread(() -> {
				try {
					if (!process.waitFor(timeout.toSeconds(), TimeUnit.SECONDS)) {
						zeitUeberschritten = true;
						process.destroyForcibly();
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}, "whatsapp-setup-watchdog");
			thread.setDaemon(true);
			thread.start();
		}

		boolean zeitUeberschritten() {
			return zeitUeberschritten;
		}

		void stoppen() {
			thread.interrupt();
		}
	}

	private static void fortschritt(Consumer<Schritt> fortschritt, Schritt schritt) {
		if (fortschritt != null) {
			fortschritt.accept(Objects.requireNonNull(schritt));
		}
	}

	private static String liesAusgabe(InputStream inputStream) throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
			StringBuilder builder = new StringBuilder();
			String zeile;
			while ((zeile = reader.readLine()) != null) {
				if (builder.length() < 8000) {
					builder.append(zeile).append('\n');
				}
			}
			return builder.toString();
		}
	}
}
