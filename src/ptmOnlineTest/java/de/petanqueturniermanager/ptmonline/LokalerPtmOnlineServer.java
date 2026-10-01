/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.petanqueturniermanager.comp.LibreOfficePtmOnlineSpeicher;

/**
 * Echter PTM-Online-Worker ({@code wrangler dev} aus dem Online-Repo), lokal und vollständig abgeschottet:
 * <ul>
 * <li>eigene, bei jedem Lauf frische D1 unter {@code build/ptmonline-e2e/state} – die Entwicklungsdatenbank des
 * Online-Repos bleibt unberührt,</li>
 * <li>eigene Wrangler-Konfiguration ohne Routen und Crons, ohne {@code .dev.vars}: kein Mailversand, keine
 * Geokodierung, keine externen Dienste,</li>
 * <li>nur auf 127.0.0.1 an einem freien Port.</li>
 * </ul>
 * Konten, Session und API-Schlüssel des Organisators schreibt der Starter direkt in die lokale D1 – Zufallswerte je
 * Lauf, nirgends gespeichert. Der Worker läuft einmal pro Test-JVM und endet mit ihr.
 */
final class LokalerPtmOnlineServer {

    private static final Logger logger = LogManager.getLogger(LokalerPtmOnlineServer.class);
    private static final Duration START_TIMEOUT = Duration.ofSeconds(120);
    /** Verifizierte Spielerkonten {@code e2e-spieler-<n>@example.test}, n = 1..ANZAHL_SPIELERKONTEN. */
    static final int ANZAHL_SPIELERKONTEN = 12;
    static final String ORGANISATOR_ID = "e2e-leitung";

    private static LokalerPtmOnlineServer instanz;

    private final Process prozess;
    private final String baseUrl;
    private final String apiKey;
    private final String sessionId;

    private LokalerPtmOnlineServer(Process prozess, String baseUrl, String apiKey, String sessionId) {
        this.prozess = prozess;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.sessionId = sessionId;
    }

    /** Startet den Worker beim ersten Aufruf; alle Testklassen der JVM teilen ihn. */
    static synchronized LokalerPtmOnlineServer get() {
        if (instanz == null) {
            try {
                instanz = starten();
            } catch (IOException e) {
                throw new IllegalStateException("Lokaler PTM-Online-Worker nicht startbar: " + e.getMessage(), e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Start des lokalen PTM-Online-Workers unterbrochen", e);
            }
            Runtime.getRuntime().addShutdownHook(new Thread(instanz::beenden, "ptmonline-e2e-stop"));
        }
        return instanz;
    }

    String baseUrl() {
        return baseUrl;
    }

    /** API-Schlüssel des Organisators (Bearer), wie ihn PTM in den Optionen hinterlegt. */
    String apiKey() {
        return apiKey;
    }

    /** Session des Organisators für Verwaltungsaufrufe, die nur mit Anmeldung gehen. */
    String sessionId() {
        return sessionId;
    }

    LibreOfficePtmOnlineSpeicher.Zugangsdaten zugangsdaten() {
        return new LibreOfficePtmOnlineSpeicher.Zugangsdaten(apiKey, baseUrl);
    }

    static String spielerEmail(int n) {
        return "e2e-spieler-" + n + "@example.test";
    }

    private static LokalerPtmOnlineServer starten() throws IOException, InterruptedException {
        File repo = new File(System.getProperty("ptmonline.repo", "../Petanque-Turnier-Manager-Online"))
                .getCanonicalFile();
        File wrangler = new File(repo, "node_modules/.bin/wrangler");
        pruefeVoraussetzung(new File(repo, "src/worker.js").isFile(),
                "Online-Repo nicht gefunden: " + repo + " (Gradle-Property -Pptmonline.repo=...)");
        pruefeVoraussetzung(wrangler.canExecute(), "wrangler fehlt: im Online-Repo 'npm install' ausführen");
        pruefeVoraussetzung(new File(repo, "dist/index.html").isFile(),
                "dist/ fehlt: im Online-Repo 'npm run build' ausführen");

        File verzeichnis = new File(System.getProperty("ptmonline.e2e.dir", "build/ptmonline-e2e")).getCanonicalFile();
        File zustand = new File(verzeichnis, "state");
        FileUtils.deleteDirectory(zustand);
        Files.createDirectories(zustand.toPath());
        File config = new File(verzeichnis, "wrangler.jsonc");
        Files.writeString(config.toPath(), konfiguration(repo), StandardCharsets.UTF_8);
        // Ohne eigene .dev.vars neben der Konfiguration greift keine fremde (die des Online-Repos enthält Zugänge).
        Files.writeString(new File(verzeichnis, ".dev.vars").toPath(), "# bewusst leer: keine externen Dienste\n",
                StandardCharsets.UTF_8);

        List<String> basis = List.of(wrangler.getAbsolutePath());
        List<String> d1 = List.of("--local", "--config", config.getAbsolutePath(), "--persist-to",
                zustand.getAbsolutePath());
        ausfuehren(verzeichnis, verbinde(basis, List.of("d1", "migrations", "apply", "DB"), d1));

        String apiKey = "ptm_e2e" + UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
        String sessionId = UUID.randomUUID().toString();
        File seed = new File(verzeichnis, "seed.sql");
        Files.writeString(seed.toPath(), seedSql(apiKey, sessionId), StandardCharsets.UTF_8);
        ausfuehren(verzeichnis, verbinde(basis, List.of("d1", "execute", "DB", "--file", seed.getAbsolutePath()), d1));

        int port = freierPort();
        File log = new File(verzeichnis, "wrangler-dev.log");
        ProcessBuilder builder = new ProcessBuilder(verbinde(basis,
                List.of("dev", "--ip", "127.0.0.1", "--port", Integer.toString(port), "--show-interactive-dev-session=false",
                        "--log-level", "warn"),
                List.of("--config", config.getAbsolutePath(), "--persist-to", zustand.getAbsolutePath())))
                .directory(verzeichnis).redirectErrorStream(true).redirectOutput(log);
        umgebung(builder.environment());
        Process prozess = builder.start();
        String baseUrl = "http://127.0.0.1:" + port;
        LokalerPtmOnlineServer server = new LokalerPtmOnlineServer(prozess, baseUrl, apiKey, sessionId);
        try {
            warteBisBereit(prozess, baseUrl, log);
        } catch (IOException | RuntimeException e) {
            server.beenden();
            throw e;
        }
        logger.info("Lokaler PTM-Online-Worker bereit: {} (Zustand {})", baseUrl, zustand);
        return server;
    }

    /**
     * Konfiguration des Online-Repos mit absoluten Pfaden, ohne Routen, Crons und Absenderadressen; die D1 hat einen
     * eigenen Namen, damit sie nie mit der Entwicklungsdatenbank verwechselt wird.
     */
    private static String konfiguration(File repo) throws IOException {
        JsonObject config = JsonParser.parseString(
                Files.readString(new File(repo, "wrangler.jsonc").toPath(), StandardCharsets.UTF_8)).getAsJsonObject();
        config.remove("$schema");
        config.remove("routes");
        config.remove("triggers");
        config.addProperty("name", "ptm-online-e2e");
        config.addProperty("main", new File(repo, "src/worker.js").getAbsolutePath());
        config.getAsJsonObject("assets").addProperty("directory", new File(repo, "dist").getAbsolutePath());
        JsonObject datenbank = new JsonObject();
        datenbank.addProperty("binding", "DB");
        datenbank.addProperty("database_name", "ptm-online-e2e");
        datenbank.addProperty("database_id", "00000000-0000-4000-8000-0000000000e2");
        datenbank.addProperty("migrations_dir", new File(repo, "migrations").getAbsolutePath());
        JsonArray datenbanken = new JsonArray();
        datenbanken.add(datenbank);
        config.add("d1_databases", datenbanken);
        config.add("vars", new JsonObject());
        return new GsonBuilder().setPrettyPrinting().create().toJson(config);
    }

    /** Organisator (verifiziert, mit Session und freigeschaltetem API-Schlüssel) und verifizierte Spielerkonten. */
    private static String seedSql(String apiKey, String sessionId) {
        String jetzt = Instant.now().toString();
        String ablauf = Instant.now().plus(2, ChronoUnit.DAYS).toString();
        StringBuilder sql = new StringBuilder();
        sql.append(konto(ORGANISATOR_ID, "e2e-leitung@example.test", "E2E", "Leitung", jetzt));
        for (int n = 1; n <= ANZAHL_SPIELERKONTEN; n++) {
            sql.append(konto("e2e-spieler-" + n, spielerEmail(n), "Konto", "Spieler" + n, jetzt));
        }
        sql.append(String.format("UPDATE users SET tournament_limit = 1000 WHERE id = '%s';%n", ORGANISATOR_ID));
        sql.append(String.format("INSERT INTO sessions (id, user_id, expires_at, created_at) VALUES ('%s', '%s', '%s', '%s');%n",
                sessionId, ORGANISATOR_ID, ablauf, jetzt));
        sql.append(String.format("INSERT INTO api_keys (id, user_id, key_hash, label, status, requested_at, approved_at, "
                + "created_at, updated_at) VALUES ('e2e-key', '%s', '%s', 'PTM E2E', 'approved', '%s', '%s', '%s', '%s');%n",
                ORGANISATOR_ID, sha256Hex(apiKey), jetzt, jetzt, jetzt, jetzt));
        return sql.toString();
    }

    private static String konto(String id, String email, String vorname, String nachname, String jetzt) {
        // Kein Passwort: password_hash ist kein gültiger Hash, eine Anmeldung per Passwort ist unmöglich.
        return String.format("INSERT INTO users (id, email, first_name, last_name, role, password_salt, password_hash, "
                + "created_at, updated_at, email_verified_at) VALUES ('%s', '%s', '%s', '%s', 'user', '-', '-', '%s', '%s', "
                + "'%s');%n", id, email, vorname, nachname, jetzt, jetzt, jetzt);
    }

    static String sha256Hex(String wert) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(wert.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void ausfuehren(File verzeichnis, List<String> befehl) throws IOException, InterruptedException {
        File log = new File(verzeichnis, "wrangler-setup.log");
        ProcessBuilder builder = new ProcessBuilder(befehl).directory(verzeichnis).redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.appendTo(log));
        umgebung(builder.environment());
        Process prozess = builder.start();
        if (!prozess.waitFor(3, TimeUnit.MINUTES)) {
            prozess.destroyForcibly();
            throw new IOException("Zeitüberschreitung: " + String.join(" ", befehl));
        }
        if (prozess.exitValue() != 0) {
            throw new IOException("Fehlgeschlagen (Exit " + prozess.exitValue() + "): " + String.join(" ", befehl)
                    + "\n" + ende(log));
        }
    }

    private static void umgebung(Map<String, String> env) {
        // Keine Rückfragen, keine Telemetrie.
        env.put("CI", "true");
        env.put("WRANGLER_SEND_METRICS", "false");
    }

    private static void warteBisBereit(Process prozess, String baseUrl, File log)
            throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        HttpRequest anfrage = HttpRequest.newBuilder(URI.create(baseUrl + "/api/tournaments"))
                .timeout(Duration.ofSeconds(5)).GET().build();
        Instant frist = Instant.now().plus(START_TIMEOUT);
        while (Instant.now().isBefore(frist)) {
            if (!prozess.isAlive()) {
                throw new IOException("wrangler dev beendet (Exit " + prozess.exitValue() + ")\n" + ende(log));
            }
            try {
                if (client.send(anfrage, HttpResponse.BodyHandlers.discarding()).statusCode() == 200) {
                    return;
                }
            } catch (IOException e) {
                // noch nicht bereit
            }
            Thread.sleep(500);
        }
        throw new IOException("wrangler dev nicht bereit nach " + START_TIMEOUT.toSeconds() + " s\n" + ende(log));
    }

    private void beenden() {
        prozess.descendants().forEach(ProcessHandle::destroy);
        prozess.destroy();
        try {
            if (!prozess.waitFor(10, TimeUnit.SECONDS)) {
                prozess.descendants().forEach(ProcessHandle::destroyForcibly);
                prozess.destroyForcibly();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static int freierPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static List<String> verbinde(List<String> a, List<String> b, List<String> c) {
        List<String> alle = new ArrayList<>(a);
        alle.addAll(b);
        alle.addAll(c);
        return alle;
    }

    private static String ende(File log) {
        try {
            List<String> zeilen = Files.readAllLines(log.toPath(), StandardCharsets.UTF_8);
            return String.join("\n", zeilen.subList(Math.max(0, zeilen.size() - 40), zeilen.size()));
        } catch (IOException e) {
            return "(Log nicht lesbar: " + log + ")";
        }
    }

    private static void pruefeVoraussetzung(boolean erfuellt, String meldung) throws IOException {
        if (!erfuellt) {
            throw new IOException(meldung);
        }
    }
}
