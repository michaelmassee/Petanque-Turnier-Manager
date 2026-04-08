package de.petanqueturniermanager.comp;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import com.google.gson.GsonBuilder;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import de.petanqueturniermanager.webserver.CompositeViewKonfiguration;
import de.petanqueturniermanager.webserver.PanelKonfiguration;
import de.petanqueturniermanager.webserver.PortKonfiguration;
import de.petanqueturniermanager.webserver.SheetResolverFactory;
import de.petanqueturniermanager.webserver.SplitKnoten;
import de.petanqueturniermanager.webserver.SplitKnotenAdapter;

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

	private static final String WEBSERVER_COMPOSITE_PORTS_PROP = "webserver_composite_ports";
	private static final String WEBSERVER_COMPOSITE_PREFIX = "webserver_composite_";
	private static final String WEBSERVER_COMPOSITE_AKTIV_SUFFIX = "_aktiv";
	private static final String WEBSERVER_COMPOSITE_ZOOM_SUFFIX = "_zoom";
	private static final String WEBSERVER_COMPOSITE_LAYOUT_SUFFIX = "_layout";
	private static final String WEBSERVER_COMPOSITE_PANEL_COUNT_SUFFIX = "_panel_count";
	private static final String WEBSERVER_COMPOSITE_PANEL_INFIX = "_panel_";
	private static final String WEBSERVER_COMPOSITE_PANEL_SHEET_SUFFIX = "_sheet";
	private static final String WEBSERVER_COMPOSITE_PANEL_ZOOM_SUFFIX = "_zoom";

	private static final String STARTUP_TURNIER_MODUS_PROP = "startup.turnier.modus";

	public static final int DEFAULT_ZOOM = 100;

	// zentrale Runtime-Map
	private static final ConcurrentHashMap<String, String> propMap = new ConcurrentHashMap<>();

	private static volatile GlobalProperties instance = null;

	private static final ReentrantLock fileLock = new ReentrantLock();

	private static final String TABFARBE_PRAEFIX = "tabfarbe.";

	public record PortEintragRoh(int port, String sheetConfig, boolean aktiv, int zoom, boolean zentrieren) {
		@Override
		public String toString() {
			return port + "=" + sheetConfig;
		}
	}

	/**
	 * Rohdaten eines einzelnen Panels in einem Composite View (vor Resolver-Erstellung).
	 */
	public record PanelEintragRoh(String sheetConfig, int zoom) {}

	/**
	 * Rohdaten eines Composite View (vor Resolver-Erstellung).
	 *
	 * @param port       TCP-Port
	 * @param aktiv      ob dieser View aktiv ist
	 * @param zoom       globaler Zoom-Faktor in % (10–500)
	 * @param layoutJson JSON-serialisierter {@link SplitKnoten}-Baum
	 * @param panels     Liste der Panel-Konfigurationen (Reihenfolge = panelIndex)
	 */
	public record CompositeViewEintragRoh(
			int port, boolean aktiv, int zoom,
			String layoutJson,
			List<PanelEintragRoh> panels) {
		@Override
		public String toString() {
			return port + " [composite, panels=" + panels.size() + "]";
		}
	}

	public static GlobalProperties get() {
		if (instance == null) {
			synchronized (GlobalProperties.class) {
				if (instance == null) {
					try {
						instance = new GlobalProperties();
					} catch (Exception e) {
						logger.error("Initialisierung fehlgeschlagen", e);
						instance = new GlobalProperties(true);
					}
				}
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

	private static void speichernDatei() {

		fileLock.lock();

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
		return Boolean.parseBoolean(propMap.getOrDefault(key, "false"));
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

	public boolean isStartupTurnierModus() {
		return getBoolean(STARTUP_TURNIER_MODUS_PROP);
	}

	public void setStartupTurnierModus(boolean aktiv) {
		setBooleanProp(STARTUP_TURNIER_MODUS_PROP, aktiv);
		speichernDatei();
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
			logger.warn("Fehler beim Lesen der Port-Einträge", e);
		}

		return eintraege;
	}

	// ----------------------------------------------------
	// Composite View-Konfiguration
	// ----------------------------------------------------

	/**
	 * Gibt alle gespeicherten Composite View-Einträge zurück (aktiv und inaktiv).
	 */
	public List<CompositeViewEintragRoh> getCompositeViewEintraege() {
		List<CompositeViewEintragRoh> eintraege = new ArrayList<>();
		try {
			var portsStr = propMap.getOrDefault(WEBSERVER_COMPOSITE_PORTS_PROP, "").trim();
			if (portsStr.isEmpty()) return eintraege;

			for (var portStr : portsStr.split(",")) {
				portStr = portStr.trim();
				if (portStr.isEmpty()) continue;
				try {
					int port = Integer.parseInt(portStr);
					boolean aktiv = getBoolean(WEBSERVER_COMPOSITE_PREFIX + port + WEBSERVER_COMPOSITE_AKTIV_SUFFIX);
					int zoom = parseZoom(propMap.get(WEBSERVER_COMPOSITE_PREFIX + port + WEBSERVER_COMPOSITE_ZOOM_SUFFIX));
					String layoutJson = propMap.getOrDefault(WEBSERVER_COMPOSITE_PREFIX + port + WEBSERVER_COMPOSITE_LAYOUT_SUFFIX, "").trim();
					if (layoutJson.isEmpty()) continue;

					int panelCount = 0;
					try {
						panelCount = Integer.parseInt(propMap.getOrDefault(
								WEBSERVER_COMPOSITE_PREFIX + port + WEBSERVER_COMPOSITE_PANEL_COUNT_SUFFIX, "0").trim());
					} catch (NumberFormatException ignored) {
					}

					List<PanelEintragRoh> panels = new ArrayList<>();
					for (int i = 0; i < panelCount; i++) {
						String sheetConfig = propMap.getOrDefault(
								WEBSERVER_COMPOSITE_PREFIX + port + WEBSERVER_COMPOSITE_PANEL_INFIX + i + WEBSERVER_COMPOSITE_PANEL_SHEET_SUFFIX, "").trim();
						if (sheetConfig.isEmpty()) continue;
						int panelZoom = parseZoom(propMap.get(
								WEBSERVER_COMPOSITE_PREFIX + port + WEBSERVER_COMPOSITE_PANEL_INFIX + i + WEBSERVER_COMPOSITE_PANEL_ZOOM_SUFFIX));
						panels.add(new PanelEintragRoh(sheetConfig, panelZoom));
					}
					if (!panels.isEmpty()) {
						eintraege.add(new CompositeViewEintragRoh(port, aktiv, zoom, layoutJson, panels));
					}
				} catch (Exception e) {
					logger.warn("Ungültiger Composite-Port-Eintrag '{}'", portStr, e);
				}
			}
		} catch (Exception e) {
			logger.warn("Fehler beim Lesen der Composite-View-Einträge", e);
		}
		return eintraege;
	}

	/**
	 * Gibt alle aktiven Composite Views als fertige Konfigurationsobjekte zurück.
	 */
	public List<CompositeViewKonfiguration> getCompositeViewKonfigurationen() {
		List<CompositeViewKonfiguration> konfigs = new ArrayList<>();
		var gson = new GsonBuilder()
				.registerTypeAdapter(SplitKnoten.class, new SplitKnotenAdapter())
				.create();

		for (var eintrag : getCompositeViewEintraege()) {
			if (!eintrag.aktiv()) continue;
			try {
				SplitKnoten wurzel = gson.fromJson(eintrag.layoutJson(), SplitKnoten.class);
				if (wurzel == null) {
					logger.warn("Ungültiger Layout-JSON für Port {}", eintrag.port());
					continue;
				}
				List<PanelKonfiguration> panels = new ArrayList<>();
				for (var p : eintrag.panels()) {
					var resolver = SheetResolverFactory.erstellen(p.sheetConfig());
					if (resolver == null) {
						logger.warn("Resolver null für Panel-Config '{}'", p.sheetConfig());
						continue;
					}
					panels.add(new PanelKonfiguration(resolver, p.zoom()));
				}
				if (!panels.isEmpty()) {
					konfigs.add(new CompositeViewKonfiguration(eintrag.port(), eintrag.zoom(), wurzel, panels));
				}
			} catch (Exception e) {
				logger.error("Fehler bei Composite-View-Konfiguration {}", eintrag, e);
			}
		}
		return konfigs;
	}

	/**
	 * Speichert alle Composite View-Einträge in der Properties-Datei.
	 * Löscht zuvor alle alten Composite-Einträge.
	 */
	public void speichernCompositeViews(List<CompositeViewEintragRoh> eintraege) {
		try {
			// Alte Einträge löschen
			for (var alt : getCompositeViewEintraege()) {
				String prefix = WEBSERVER_COMPOSITE_PREFIX + alt.port();
				propMap.remove(prefix + WEBSERVER_COMPOSITE_AKTIV_SUFFIX);
				propMap.remove(prefix + WEBSERVER_COMPOSITE_ZOOM_SUFFIX);
				propMap.remove(prefix + WEBSERVER_COMPOSITE_LAYOUT_SUFFIX);
				propMap.remove(prefix + WEBSERVER_COMPOSITE_PANEL_COUNT_SUFFIX);
				for (int i = 0; i < alt.panels().size(); i++) {
					propMap.remove(prefix + WEBSERVER_COMPOSITE_PANEL_INFIX + i + WEBSERVER_COMPOSITE_PANEL_SHEET_SUFFIX);
					propMap.remove(prefix + WEBSERVER_COMPOSITE_PANEL_INFIX + i + WEBSERVER_COMPOSITE_PANEL_ZOOM_SUFFIX);
				}
			}
			propMap.remove(WEBSERVER_COMPOSITE_PORTS_PROP);

			if (!eintraege.isEmpty()) {
				var ports = new StringBuilder();
				for (var eintrag : eintraege) {
					if (!ports.isEmpty()) ports.append(",");
					ports.append(eintrag.port());
					String prefix = WEBSERVER_COMPOSITE_PREFIX + eintrag.port();
					if (eintrag.aktiv())
						propMap.put(prefix + WEBSERVER_COMPOSITE_AKTIV_SUFFIX, "true");
					if (eintrag.zoom() != DEFAULT_ZOOM)
						propMap.put(prefix + WEBSERVER_COMPOSITE_ZOOM_SUFFIX, String.valueOf(eintrag.zoom()));
					propMap.put(prefix + WEBSERVER_COMPOSITE_LAYOUT_SUFFIX, eintrag.layoutJson());
					propMap.put(prefix + WEBSERVER_COMPOSITE_PANEL_COUNT_SUFFIX, String.valueOf(eintrag.panels().size()));
					for (int i = 0; i < eintrag.panels().size(); i++) {
						var panel = eintrag.panels().get(i);
						propMap.put(prefix + WEBSERVER_COMPOSITE_PANEL_INFIX + i + WEBSERVER_COMPOSITE_PANEL_SHEET_SUFFIX, panel.sheetConfig());
						if (panel.zoom() != DEFAULT_ZOOM)
							propMap.put(prefix + WEBSERVER_COMPOSITE_PANEL_INFIX + i + WEBSERVER_COMPOSITE_PANEL_ZOOM_SUFFIX, String.valueOf(panel.zoom()));
					}
				}
				propMap.put(WEBSERVER_COMPOSITE_PORTS_PROP, ports.toString());
			}
			speichernDatei();
		} catch (Exception e) {
			logger.error("Fehler beim Speichern der Composite Views", e);
		}
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
		return TABFARBE_PRAEFIX + konfigPropKey.toLowerCase()
				.replace("tab-farbe ", "")
				.replace(" ", "_");
	}

	private static void setBooleanProp(String key, boolean value) {
		if (value) propMap.put(key, "true");
		else propMap.remove(key);
	}

	private static int parseZoom(String value) {
		if (value == null || value.isBlank()) return DEFAULT_ZOOM;
		try {
			int zoom = Integer.parseInt(value.trim());
			if (zoom >= 10 && zoom <= 500) return zoom;
			logger.warn("Zoom-Wert außerhalb des erlaubten Bereichs (10-500): {}, verwende Standard {}", zoom, DEFAULT_ZOOM);
			return DEFAULT_ZOOM;
		} catch (NumberFormatException e) {
			logger.warn("Ungültiger Zoom-Wert '{}', verwende Standard {}", value.trim(), DEFAULT_ZOOM);
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


	// nur für Tests
	static void resetForTest() {
		instance = null;
		propMap.clear();
	}
}