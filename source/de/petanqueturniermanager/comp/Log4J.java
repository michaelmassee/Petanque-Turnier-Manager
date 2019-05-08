/**
 * Erstellung 05.05.2019 / Michael Massee
 */
package de.petanqueturniermanager.comp;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Michael Massee
 *
 */
public class Log4J {
	private static final Logger logger = LogManager.getLogger(Log4J.class);

	// https://logging.apache.org/log4j/2.x/manual/configuration.html
	// 5. If no such file is found the XML ConfigurationFactory will look for log4j2-test.xml in the classpath.
	// <RollingFile name="infofile" fileName="${sys:user.home}/.petanqueturniermanager/info.log" ignoreExceptions="false"
	public static final String LOGFILE_NAME = System.getProperty("user.home") + "/.petanqueturniermanager/info.log";
	public static final File LOGFILE = new File(System.getProperty("user.home"), "/.petanqueturniermanager/info.log");

	public static final void openLogFile() {
		try {
			// java.awt.Desktop => coredump, not working
			// https://stackoverflow.com/questions/526037/how-to-open-user-system-preferred-editor-for-given-file
			String osName = System.getProperty("os.name");
			if (StringUtils.containsIgnoreCase(osName, "windows")) {
				String cmd = "rundll32 url.dll,FileProtocolHandler " + LOGFILE.getCanonicalPath();
				Runtime.getRuntime().exec(cmd);
			} else if (StringUtils.containsIgnoreCase(osName, "linux")) {
				String cmd = "gedit " + LOGFILE.getCanonicalPath();
				Runtime.getRuntime().exec(cmd);
			}
		} catch (IOException e) {
			logger.error(e);
		}
	}
}
