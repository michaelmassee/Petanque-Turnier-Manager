package de.petanqueturniermanager.comp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

public class GlobalProperties {
	private static final String FILENAME = "PetanqueTurnierManager.properties";
	private static final String LOG_LEVEL_PROP = "loglevel";
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
