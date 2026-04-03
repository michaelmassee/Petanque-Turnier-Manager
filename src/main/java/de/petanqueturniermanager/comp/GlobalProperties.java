package de.petanqueturniermanager.comp;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import de.petanqueturniermanager.webserver.PortKonfiguration;
import de.petanqueturniermanager.webserver.SheetResolverFactory;

public class GlobalProperties {

	private static final Logger logger = LogManager.getLogger(GlobalProperties.class);

	private static final String FILENAME = "PetanqueTurnierManager.properties";

	private static final String LOG_LEVEL_PROP = "loglevel";
	private static final String CREATE_BACKUP_PROP = "backup";
	private static final String AUTOSAVE_PROP = "autosave";
	private static final String NEW_VERSION_CHECK_PROP = "newversioncheck";

	private static final String WEBSERVER_AKTIV_PROP = "webserver_aktiv";
	private static final String WEBSERVER_PORTS_PROP = "webserver_ports";
	private static final String WEBSERVER_PORT_SHEET_PREFIX = "webserver_port_";
	private static final String WEBSERVER_PORT_SHEET_SUFFIX = "_sheet";
	private static final String WEBSERVER_PORT_AKTIV_SUFFIX = "_aktiv";
	private static final String WEBSERVER_PORT_ZOOM_SUFFIX = "_zoom";
	private static final String WEBSERVER_PORT_ZENTRIEREN_SUFFIX = "_zentrieren";
	private static final String WEBSERVER_SHEETNAMEN_ANZEIGEN_PROP = "webserver_sheetnamen_anzeigen";

	public static final int DEFAULT_ZOOM = 100;

	// 🔥 zentrale Runtime-Map
	private static final ConcurrentHashMap<String, String> propMap = new ConcurrentHashMap<>();

	private static GlobalProperties instance = null;

	private final ReentrantLock fileLock = new ReentrantLock();

	public record PortEintragRoh(int port, String sheetConfig, boolean aktiv, int zoom, boolean zentrieren) {
		@Override
		public String toString() {
			return port + "=" + sheetConfig;
		}
	}

	public static GlobalProperties get() {
		if (instance == null) {
			try {
				instance = new GlobalProperties();
			} catch (Exception e) {
				logger.error("Initialisierung fehlgeschlagen", e);
				instance = new GlobalProperties(true);
			}
		}
		return instance;
	}

	private GlobalProperties() {
		readProperties();
		safeSetLogLevel();
	}

	private GlobalProperties(boolean fallback) {
		if (fallback) {
			logger.warn("GlobalProperties im Fallback-Modus gestartet (leere Konfiguration)");
		}
	}

	// ----------------------------------------------------
	// Laden / Speichern
	// ----------------------------------------------------
	private void readProperties() {
		fileLock.lock();
		try {
			var path = Path.of(System.getProperty("user.home"), FILENAME);

			try {
				Files.createFile(path);
			} catch (java.nio.file.FileAlreadyExistsException ignored) {
			}

			Properties loaded = new Properties();

			try (var in = Files.newInputStream(path)) {
				loaded.load(in);
			}

			propMap.clear();
			for (String key : loaded.stringPropertyNames()) {
				propMap.put(key, loaded.getProperty(key));
			}

		} catch (Exception e) {
			logger.error("Fehler beim Laden", e);
		} finally {
			fileLock.unlock();
		}
	}

	private void speichernDatei() {

		if (!fileLock.tryLock()) {
			return;
		}

		try {
			var path = Path.of(System.getProperty("user.home"), FILENAME);
			var tmp = path.resolveSibling(FILENAME + ".tmp");

			Properties props = new Properties();
			for (var e : propMap.entrySet()) {
				props.setProperty(e.getKey(), e.getValue());
			}

			try (var out = Files.newOutputStream(tmp)) {
				props.store(out, "Petanque Turnier Manager Konfiguration");
			}

			Files.move(tmp, path,
					java.nio.file.StandardCopyOption.REPLACE_EXISTING,
					java.nio.file.StandardCopyOption.ATOMIC_MOVE);

		} catch (Exception e) {
			logger.error("Fehler beim Speichern", e);
		} finally {
			fileLock.unlock();
		}
	}
	// ----------------------------------------------------
	// Getter
	// ----------------------------------------------------

	private boolean getBoolean(String key) {
		try {
			return Boolean.parseBoolean(propMap.getOrDefault(key, "false"));
		} catch (Exception e) {
			logger.warn("Fehler beim Lesen Boolean {}", key, e);
			return false;
		}
	}

	public boolean isAutoSave() {
		return getBoolean(AUTOSAVE_PROP);
	}

	public boolean isCreateBackup() {
		return getBoolean(CREATE_BACKUP_PROP);
	}

	public boolean isNewVersionCheckImmerTrue() {
		return getBoolean(NEW_VERSION_CHECK_PROP);
	}

	public boolean isWebserverAktiv() {
		return getBoolean(WEBSERVER_AKTIV_PROP);
	}

	public boolean isSheetnamenKopfzeileAnzeigen() {
		return getBoolean(WEBSERVER_SHEETNAMEN_ANZEIGEN_PROP);
	}

	public String getLogLevel() {
		return propMap.getOrDefault(LOG_LEVEL_PROP, "").trim();
	}

	// ----------------------------------------------------
	// Port-Konfiguration
	// ----------------------------------------------------

	public List<PortKonfiguration> getPortKonfigurationen() {
		List<PortKonfiguration> konfigs = new ArrayList<>();

		for (var eintrag : getPortEintraege()) {
			if (!eintrag.aktiv()) continue;

			try {
				var resolver = SheetResolverFactory.erstellen(eintrag.sheetConfig());
				if (resolver == null) {
					logger.warn("Resolver null für {}", eintrag);
					continue;
				}

				konfigs.add(new PortKonfiguration(
						eintrag.port(),
						resolver,
						eintrag.zoom(),
						eintrag.zentrieren()));

			} catch (Exception e) {
				logger.error("Fehler bei Port-Konfiguration {}", eintrag, e);
			}
		}

		return konfigs;
	}

	public List<PortEintragRoh> getPortEintraege() {
		List<PortEintragRoh> eintraege = new ArrayList<>();

		try {
			var portsStr = propMap.getOrDefault(WEBSERVER_PORTS_PROP, "").trim();
			if (portsStr.isEmpty()) return eintraege;

			for (var portStr : portsStr.split(",")) {
				try {
					portStr = portStr.trim();
					if (portStr.isEmpty()) continue;

					int port = Integer.parseInt(portStr);

					var sheetConfig = propMap.getOrDefault(
							WEBSERVER_PORT_SHEET_PREFIX + port + WEBSERVER_PORT_SHEET_SUFFIX, "").trim();

					if (sheetConfig.isEmpty()) continue;

					boolean aktiv = getBoolean(WEBSERVER_PORT_SHEET_PREFIX + port + WEBSERVER_PORT_AKTIV_SUFFIX);
					int zoom = parseZoom(propMap.get(WEBSERVER_PORT_SHEET_PREFIX + port + WEBSERVER_PORT_ZOOM_SUFFIX));
					boolean zentrieren = getBoolean(WEBSERVER_PORT_SHEET_PREFIX + port + WEBSERVER_PORT_ZENTRIEREN_SUFFIX);

					eintraege.add(new PortEintragRoh(port, sheetConfig, aktiv, zoom, zentrieren));

				} catch (Exception e) {
					logger.warn("Ungültiger Port-Eintrag '{}'", portStr, e);
				}
			}

		} catch (Exception e) {
			logger.error("Fehler beim Lesen der Port-Einträge", e);
		}

		return eintraege;
	}

	// ----------------------------------------------------
	// Speichern
	// ----------------------------------------------------

	public void speichern(boolean autosave, boolean backup, boolean newVersionCheck, String logLevel) {
		try {
			setBooleanProp(AUTOSAVE_PROP, autosave);
			setBooleanProp(CREATE_BACKUP_PROP, backup);
			setBooleanProp(NEW_VERSION_CHECK_PROP, newVersionCheck);

			if (logLevel != null && !logLevel.isBlank()) {
				propMap.put(LOG_LEVEL_PROP, logLevel.trim().toLowerCase());
			} else {
				propMap.remove(LOG_LEVEL_PROP);
			}

			speichernDatei();
			safeSetLogLevel();

		} catch (Exception e) {
			logger.error("Fehler beim Speichern", e);
		}
	}

	public void speichernWebserver(boolean aktiv, boolean sheetnamenAnzeigen, List<PortEintragRoh> eintraege) {
		try {
			for (var alt : getPortEintraege()) {
				propMap.remove(WEBSERVER_PORT_SHEET_PREFIX + alt.port() + WEBSERVER_PORT_SHEET_SUFFIX);
				propMap.remove(WEBSERVER_PORT_SHEET_PREFIX + alt.port() + WEBSERVER_PORT_AKTIV_SUFFIX);
				propMap.remove(WEBSERVER_PORT_SHEET_PREFIX + alt.port() + WEBSERVER_PORT_ZOOM_SUFFIX);
				propMap.remove(WEBSERVER_PORT_SHEET_PREFIX + alt.port() + WEBSERVER_PORT_ZENTRIEREN_SUFFIX);
			}

			propMap.remove(WEBSERVER_PORTS_PROP);

			setBooleanProp(WEBSERVER_AKTIV_PROP, aktiv);
			setBooleanProp(WEBSERVER_SHEETNAMEN_ANZEIGEN_PROP, sheetnamenAnzeigen);

			if (!eintraege.isEmpty()) {
				var ports = new StringBuilder();

				for (var eintrag : eintraege) {
					if (!ports.isEmpty()) ports.append(",");
					ports.append(eintrag.port());

					propMap.put(WEBSERVER_PORT_SHEET_PREFIX + eintrag.port() + WEBSERVER_PORT_SHEET_SUFFIX, eintrag.sheetConfig());

					if (eintrag.aktiv())
						propMap.put(WEBSERVER_PORT_SHEET_PREFIX + eintrag.port() + WEBSERVER_PORT_AKTIV_SUFFIX, "true");

					if (eintrag.zoom() != DEFAULT_ZOOM)
						propMap.put(WEBSERVER_PORT_SHEET_PREFIX + eintrag.port() + WEBSERVER_PORT_ZOOM_SUFFIX, String.valueOf(eintrag.zoom()));

					if (eintrag.zentrieren())
						propMap.put(WEBSERVER_PORT_SHEET_PREFIX + eintrag.port() + WEBSERVER_PORT_ZENTRIEREN_SUFFIX, "true");
				}

				propMap.put(WEBSERVER_PORTS_PROP, ports.toString());
			}

			speichernDatei();

		} catch (Exception e) {
			logger.error("Fehler beim Speichern Webserver", e);
		}
	}

	// ----------------------------------------------------
	// Extras
	// ----------------------------------------------------

	public int getTabFarbe(String konfigPropKey, int defaultVal) {
		try {
			var key = toTabFarbGlobalKey(konfigPropKey);
			var val = propMap.get(key);

			if (val == null || val.isBlank()) return defaultVal;

			return Integer.parseInt(val.trim(), 16);

		} catch (Exception e) {
			logger.warn("Fehler bei TabFarbe {}", konfigPropKey, e);
			return defaultVal;
		}
	}

	private static String toTabFarbGlobalKey(String konfigPropKey) {
		return "tabfarbe." + konfigPropKey.toLowerCase()
				.replace("tab-farbe ", "")
				.replace(" ", "_");
	}

	private void setBooleanProp(String key, boolean value) {
		try {
			if (value) propMap.put(key, "true");
			else propMap.remove(key);
		} catch (Exception e) {
			logger.warn("Fehler beim Setzen {}", key, e);
		}
	}

	private static int parseZoom(String value) {
		try {
			if (value == null || value.isBlank()) return DEFAULT_ZOOM;
			int zoom = Integer.parseInt(value.trim());
			return (zoom >= 10 && zoom <= 500) ? zoom : DEFAULT_ZOOM;
		} catch (Exception e) {
			return DEFAULT_ZOOM;
		}
	}

	private void safeSetLogLevel() {
		try {
			var logLevel = getLogLevel();
			if (logLevel.isBlank()) return;

			var loggerCfg = LogManager.getLogger(Log4J.LOGGERNAME);

			switch (logLevel) {
				case "debug" -> Configurator.setLevel(loggerCfg, Level.DEBUG);
				case "info" -> Configurator.setLevel(loggerCfg, Level.INFO);
				default -> logger.warn("Unbekanntes LogLevel: {}", logLevel);
			}

		} catch (Exception e) {
			logger.error("Fehler beim Setzen des LogLevels", e);
		}
	}


	static void resetForTest() {
		instance = null;
		propMap.clear();
	}
}