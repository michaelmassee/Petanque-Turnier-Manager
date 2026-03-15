package de.petanqueturniermanager.comp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

public class GlobalProperties {
	private static final String FILENAME = "PetanqueTurnierManager.properties";
	private static final String LOG_LEVEL_PROP = "loglevel";
	private static final String CREATE_BACKUP_PROP = "backup"; // create backups before wichtige generierungen
	private static final String AUTOSAVE_PROP = "autosave"; // autosave nach jeden Aktion
	private static final String NEW_VERSION_CHECK_PROP = "newversioncheck"; // immer true liefern beim NewReleaseChecker (Entwicklungsmodus)

	private static final Properties props = new Properties();

	private static GlobalProperties instance = null;

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
		File propFile = new File(System.getProperty("user.home"), FILENAME);

		try {
			propFile.createNewFile(); // create new if not exist
			try (FileInputStream fileInputStream = new FileInputStream(propFile)) {
				props.load(fileInputStream);
			}
			;

		} catch (IOException e) {
			e.printStackTrace();
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

	/** Liefert den konfigurierten Log-Level ("debug", "info" oder leer). */
	public String getLogLevel() {
		return props.getProperty(LOG_LEVEL_PROP, "").trim();
	}

	/**
	 * Speichert alle Plugin-Konfigurationswerte in die Properties-Datei und
	 * wendet den Log-Level sofort an.
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
		File propFile = new File(System.getProperty("user.home"), FILENAME);
		try (FileOutputStream out = new FileOutputStream(propFile)) {
			props.store(out, "Petanque Turnier Manager Konfiguration");
		} catch (IOException e) {
			e.printStackTrace();
		}
		setLogLevel();
	}

	private void setBooleanProp(String key, boolean value) {
		if (value) {
			props.setProperty(key, "true");
		} else {
			props.remove(key);
		}
	}

	private boolean getBoolean(String propKey) {
		boolean ret = false;
		if (props != null && props.containsKey(propKey)) {
			ret = Boolean.parseBoolean(props.getProperty(propKey));
		}
		return ret;
	}

	private void setLogLevel() {
		String logLevel = (String) props.get(LOG_LEVEL_PROP);
		if (logLevel != null && !logLevel.isBlank()) {
			logLevel = logLevel.trim();
			Logger loggerAusconfigDatei = LogManager.getLogger(Log4J.LOGGERNAME);
			if (logLevel.equalsIgnoreCase("debug")) {
				Configurator.setLevel(loggerAusconfigDatei, Level.DEBUG);
			} else if (logLevel.equalsIgnoreCase("info")) {
				Configurator.setLevel(loggerAusconfigDatei, Level.INFO);
			}
		}
	}
}
