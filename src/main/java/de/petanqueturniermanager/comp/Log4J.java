/**
 * Erstellung 05.05.2019 / Michael Massee
 */
package de.petanqueturniermanager.comp;

import java.io.File;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;

import de.petanqueturniermanager.helper.msgbox.ProcessBox;

/**
 * @author Michael Massee
 *
 */
public class Log4J {
	private static final Logger logger = LogManager.getLogger(Log4J.class);

	// https://logging.apache.org/log4j/2.x/manual/configuration.html
	// 5. If no such file is found the XML ConfigurationFactory will look for
	// log4j2-test.xml in the classpath.
	// <RollingFile name="infofile"
	// fileName="${sys:user.home}/.petanqueturniermanager/info.log"
	// ignoreExceptions="false"

	// public static final File logFile = new
	// File(PetanqueTurnierManagerImpl.BASE_INTERNAL_DIR, "info.log");
	public static final String LOGGERNAME = "de.petanqueturniermanager";

	private static File logFile = null;

	static {
		// logfile aus configuration lesen
		final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		final Configuration config = ctx.getConfiguration();
		Map<String, Appender> appenders = config.getAppenders();

		RollingFileAppender fileAppender = (RollingFileAppender) appenders.entrySet().stream()
				.filter(e -> e.getValue() instanceof RollingFileAppender).map(Map.Entry::getValue).findFirst()
				.orElse(null);

		if (fileAppender != null) {
			logFile = new File(fileAppender.getFileName());
		}
	}

	public static final void openLogFile() {

		Logger loggerAusconfigDatei = LogManager.getLogger(LOGGERNAME);
		Configurator.setLevel(loggerAusconfigDatei, Level.DEBUG);

		if (logFile != null) {
			try {
				ProcessBox.from().prefix("Log4J").info("Open:" + logFile.getCanonicalPath());
				String cmd = null;
				// java.awt.Desktop => coredump, not working
				// https://stackoverflow.com/questions/526037/how-to-open-user-system-preferred-editor-for-given-file
				String osName = System.getProperty("os.name");
				if (StringUtils.containsIgnoreCase(osName, "win")) {
					cmd = "rundll32 url.dll,FileProtocolHandler " + logFile.getCanonicalPath();
				} else if (StringUtils.containsIgnoreCase(osName, "linux")) {
					cmd = "xdg-open " + logFile.getCanonicalPath();
				} else if (StringUtils.containsIgnoreCase(osName, "mac")) {
					cmd = "open " + logFile.getCanonicalPath();
				}
				if (cmd != null) {
					logger.info(cmd);
					Runtime.getRuntime().exec(cmd);
				}
			} catch (IOException e) {
				logger.error(e);
			}
		} else {
			ProcessBox.from().prefix("Log4J").fehler("Kann Logdatei nicht ermitteln");
		}
	}
}
