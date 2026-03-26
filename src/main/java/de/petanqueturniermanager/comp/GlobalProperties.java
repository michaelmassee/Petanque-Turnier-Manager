package de.petanqueturniermanager.comp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

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

	private static final Properties props = new Properties();

	private static GlobalProperties instance = null;

	/**
	 * Roher Port-Eintrag für den Konfigurationsdialog (ohne aufgelösten SheetResolver).
	 *
	 * @param port        TCP-Port
	 * @param sheetConfig Config-String (z.B. "SCHWEIZER_RANGLISTE" oder Sheet-Tab-Name)
	 * @param aktiv       ob dieser Port beim Webserver-Start aktiv ist
	 */
	public record PortEintragRoh(int port, String sheetConfig, boolean aktiv) {
		@Override
		public String toString() {
			return port + "=" + sheetConfig;
		}
	}

	public static GlobalProperties get() {
		if (instance == null) {
			GlobalProperties.instance = new GlobalProperties();
		}
		return instance;
	}

	private GlobalProperties() {
		readProperties();
		setLogLevel();
	}

	private void readProperties() {
		var propFile = new File(System.getProperty("user.home"), FILENAME);
		try {
			propFile.createNewFile();
			try (var fileInputStream = new FileInputStream(propFile)) {
				props.load(fileInputStream);
			}
		} catch (IOException e) {
			logger.error("Fehler beim Laden der properties-Datei: {}", e.getMessage(), e);
		}
	}

	public boolean isAutoSave() {
		return getBoolean(AUTOSAVE_PROP);
	}

	public boolean isCreateBackup() {
		return getBoolean(CREATE_BACKUP_PROP);
	}

	/** Wenn true, liefert NewReleaseChecker immer "neue Version verfügbar" (Entwicklungsmodus). */
	public boolean isNewVersionCheckImmerTrue() {
		return getBoolean(NEW_VERSION_CHECK_PROP);
	}

	/** Ob der eingebettete Webserver beim Start automatisch aktiviert werden soll. */
	public boolean isWebserverAktiv() {
		return getBoolean(WEBSERVER_AKTIV_PROP);
	}

	/**
	 * Liefert alle konfigurierten Port-Konfigurationen mit aufgelösten SheetResolvern.
	 * <p>
	 * Format in der properties-Datei:
	 * <pre>
	 *   webserver.ports=8081,8082
	 *   webserver.port.8081.sheet=SCHWEIZER_RANGLISTE
	 *   webserver.port.8082.sheet=SCHWEIZER_SPIELRUNDE
	 * </pre>
	 *
	 * @return Liste der Port-Konfigurationen (nie null, ggf. leer)
	 */
	public List<PortKonfiguration> getPortKonfigurationen() {
		List<PortKonfiguration> konfigs = new ArrayList<>();
		for (var eintrag : getPortEintraege()) {
			if (eintrag.aktiv()) {
				konfigs.add(new PortKonfiguration(eintrag.port(), SheetResolverFactory.erstellen(eintrag.sheetConfig())));
			}
		}
		return konfigs;
	}

	/**
	 * Liefert alle konfigurierten Ports als rohe Einträge (ohne SheetResolver).
	 * Wird vom Konfigurationsdialog verwendet.
	 *
	 * @return Liste der Port-Einträge (nie null, ggf. leer)
	 */
	public List<PortEintragRoh> getPortEintraege() {
		List<PortEintragRoh> eintraege = new ArrayList<>();
		var portsStr = props.getProperty(WEBSERVER_PORTS_PROP, "").trim();
		if (portsStr.isEmpty()) {
			return eintraege;
		}
		for (var portStr : portsStr.split(",")) {
			portStr = portStr.trim();
			if (portStr.isEmpty()) {
				continue;
			}
			try {
				int port = Integer.parseInt(portStr);
				var sheetConfig = props.getProperty(WEBSERVER_PORT_SHEET_PREFIX + port + WEBSERVER_PORT_SHEET_SUFFIX, "").trim();
				if (!sheetConfig.isEmpty()) {
					boolean aktiv = Boolean.parseBoolean(
							props.getProperty(WEBSERVER_PORT_SHEET_PREFIX + port + WEBSERVER_PORT_AKTIV_SUFFIX, "false"));
					eintraege.add(new PortEintragRoh(port, sheetConfig, aktiv));
				}
			} catch (NumberFormatException e) {
				logger.warn("Ungültige Port-Nummer in {}: '{}'", WEBSERVER_PORTS_PROP, portStr);
			}
		}
		return eintraege;
	}

	/** Liefert den konfigurierten Log-Level ("debug", "info" oder leer). */
	public String getLogLevel() {
		return props.getProperty(LOG_LEVEL_PROP, "").trim();
	}

	/**
	 * Speichert Plugin-Basiskonfiguration (ohne Webserver-Einstellungen).
	 */
	public void speichern(boolean autosave, boolean backup, boolean newVersionCheck, String logLevel) {
		setBooleanProp(AUTOSAVE_PROP, autosave);
		setBooleanProp(CREATE_BACKUP_PROP, backup);
		setBooleanProp(NEW_VERSION_CHECK_PROP, newVersionCheck);
		if (logLevel != null && !logLevel.isBlank()) {
			props.setProperty(LOG_LEVEL_PROP, logLevel.trim().toLowerCase());
		} else {
			props.remove(LOG_LEVEL_PROP);
		}
		speichernDatei();
		setLogLevel();
	}

	/**
	 * Speichert Webserver-Konfiguration.
	 * Löscht zuerst alle vorhandenen {@code webserver.port.*}-Einträge, dann
	 * schreibt die neuen Werte.
	 *
	 * @param aktiv       ob Webserver beim Start aktiviert werden soll
	 * @param eintraege   Port-Einträge (PORT=SHEET_TYP)
	 */
	public void speichernWebserver(boolean aktiv, List<PortEintragRoh> eintraege) {
		// Bekannte Port-Einträge kontrolliert löschen (kein blindes removeIf)
		for (var alt : getPortEintraege()) {
			props.remove(WEBSERVER_PORT_SHEET_PREFIX + alt.port() + WEBSERVER_PORT_SHEET_SUFFIX);
			props.remove(WEBSERVER_PORT_SHEET_PREFIX + alt.port() + WEBSERVER_PORT_AKTIV_SUFFIX);
		}
		props.remove(WEBSERVER_PORTS_PROP);
		setBooleanProp(WEBSERVER_AKTIV_PROP, aktiv);

		if (!eintraege.isEmpty()) {
			var ports = new StringBuilder();
			for (var eintrag : eintraege) {
				if (!ports.isEmpty()) {
					ports.append(",");
				}
				ports.append(eintrag.port());
				props.setProperty(WEBSERVER_PORT_SHEET_PREFIX + eintrag.port() + WEBSERVER_PORT_SHEET_SUFFIX,
						eintrag.sheetConfig());
				if (eintrag.aktiv()) {
					props.setProperty(WEBSERVER_PORT_SHEET_PREFIX + eintrag.port() + WEBSERVER_PORT_AKTIV_SUFFIX, "true");
				}
			}
			props.setProperty(WEBSERVER_PORTS_PROP, ports.toString());
		}
		speichernDatei();
	}

	private void speichernDatei() {
		var propFile = new File(System.getProperty("user.home"), FILENAME);
		try (var out = new FileOutputStream(propFile)) {
			props.store(out, "Petanque Turnier Manager Konfiguration");
		} catch (IOException e) {
			logger.error("Fehler beim Speichern der properties-Datei: {}", e.getMessage(), e);
		}
	}

	private void setBooleanProp(String key, boolean value) {
		if (value) {
			props.setProperty(key, "true");
		} else {
			props.remove(key);
		}
	}

	private boolean getBoolean(String propKey) {
		if (props != null && props.containsKey(propKey)) {
			return Boolean.parseBoolean(props.getProperty(propKey));
		}
		return false;
	}

	private void setLogLevel() {
		var logLevel = (String) props.get(LOG_LEVEL_PROP);
		if (logLevel != null && !logLevel.isBlank()) {
			logLevel = logLevel.trim();
			var loggerAusConfigDatei = LogManager.getLogger(Log4J.LOGGERNAME);
			if (logLevel.equalsIgnoreCase("debug")) {
				Configurator.setLevel(loggerAusConfigDatei, Level.DEBUG);
			} else if (logLevel.equalsIgnoreCase("info")) {
				Configurator.setLevel(loggerAusConfigDatei, Level.INFO);
			}
		}
	}
}
