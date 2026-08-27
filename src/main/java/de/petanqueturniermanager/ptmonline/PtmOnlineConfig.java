/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Speichert Basis-URL und (freigeschalteten) API-Schluessel fuer PTM-Online lokal im
 * Benutzerverzeichnis, ausserhalb der .ods-Datei (die Datei wird oft geteilt/versioniert, ein
 * darin abgelegter Schluessel waere ungewollt oeffentlich).
 */
public class PtmOnlineConfig {

    private static final Logger logger = LogManager.getLogger(PtmOnlineConfig.class);

    private static final String CONFIG_DIR = ".petanqueturniermanager";
    private static final String CONFIG_FILE = "ptm-online.properties";
    private static final String KEY_BASE_URL = "baseUrl";
    private static final String KEY_API_KEY = "apiKey";

    private final Path configPath;
    private final Properties props;

    public PtmOnlineConfig() {
        this(Paths.get(System.getProperty("user.home"), CONFIG_DIR, CONFIG_FILE));
    }

    PtmOnlineConfig(Path configPath) {
        this.configPath = configPath;
        this.props = loadOrEmpty(configPath);
    }

    public String getBaseUrl() {
        return props.getProperty(KEY_BASE_URL, "");
    }

    public String getApiKey() {
        return props.getProperty(KEY_API_KEY, "");
    }

    public boolean isConfigured() {
        return !getBaseUrl().isBlank() && !getApiKey().isBlank();
    }

    public void save(String baseUrl, String apiKey) throws IOException {
        props.setProperty(KEY_BASE_URL, baseUrl == null ? "" : baseUrl.trim());
        props.setProperty(KEY_API_KEY, apiKey == null ? "" : apiKey.trim());

        Path parent = configPath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (OutputStream out = Files.newOutputStream(configPath)) {
            props.store(out, "PTM-Online Zugangsdaten - nicht in Turnierdokumente kopieren");
        }
        restrictToOwner();
    }

    private static Properties loadOrEmpty(Path path) {
        Properties props = new Properties();
        if (!Files.isRegularFile(path)) {
            return props;
        }
        try (InputStream in = Files.newInputStream(path)) {
            props.load(in);
        } catch (IOException e) {
            logger.warn("PTM-Online Konfiguration konnte nicht gelesen werden: {}", path, e);
        }
        return props;
    }

    private void restrictToOwner() throws IOException {
        try {
            Files.setPosixFilePermissions(configPath, EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        } catch (UnsupportedOperationException e) {
            // Nicht-POSIX-Dateisystem (z.B. Windows) - Verzeichnis bleibt benutzerspezifisch, aber ohne chmod.
        }
    }
}
