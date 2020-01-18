/**
 * Erstellung 05.05.2019 / Michael Massee
 */
package de.petanqueturniermanager.comp;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.helper.msgbox.ProcessBox;

/**
 * @author Michael Massee
 *
 */
public class Log4J {
	private static final Logger logger = LogManager.getLogger(Log4J.class);

	// https://logging.apache.org/log4j/2.x/manual/configuration.html
	// 5. If no such file is found the XML ConfigurationFactory will look for log4j2-test.xml in the classpath.
	// <RollingFile name="infofile" fileName="${sys:user.home}/.petanqueturniermanager/info.log" ignoreExceptions="false"
	public static final File LOGFILE = new File(PetanqueTurnierManagerImpl.BASE_INTERNAL_DIR, "info.log");

	public static final void openLogFile() {

		/**
		 * kopie von Wollmux <br>
		 * xdg-open command in the Linux system is used to open a file or URL in the user’s preferred application.<br>
		 * The URL will be opened in the user’s preferred web browser if a URL is provided.<br>
		 * The file will be opened in the preferred application for files of that type if a file is provided.<br>
		 * xdg-open supports ftp, file, https and http URLs.<br>
		 * This can be used inside a desktop session only.<br>
		 * It is not recommended to use xdg-open as root.<br>
		 * Here, the zero is an indication of success while non-zero show the failure.<br>
		 *
		 * ExterneAnwendungen(<br>
		 * (EXT ("pdf", "PDF") DOWNLOAD "true" #ACHTUNG! Acrobat Reader funktioniert NUR mit DOWNLOAD "true"<br>
		 * FILTER "writer_pdf_Export"<br>
		 * PROGRAM (<br>
		 * # Linux<br>
		 * "xdg-open",<br>
		 * # Windows<br>
		 * "start", #S tandard-Programm für PDF-Dateien<br>
		 * )<br>
		 * )<br>
		 * (EXT "http:" DOWNLOAD "false"<br>
		 * PROGRAM (<br>
		 * # Linux<br>
		 * "sensible-browser",<br>
		 * # Windows<br>
		 * "start", # Standard-Programm für Browser-URLs<br>
		 * )<br>
		 * ) <br>
		 */

		try {
			ProcessBox.from().prefix("Log4J").info("Open:" + LOGFILE.getCanonicalPath());
			String cmd = null;
			// java.awt.Desktop => coredump, not working
			// https://stackoverflow.com/questions/526037/how-to-open-user-system-preferred-editor-for-given-file
			String osName = System.getProperty("os.name");
			if (StringUtils.containsIgnoreCase(osName, "win")) {
				cmd = "rundll32 url.dll,FileProtocolHandler " + LOGFILE.getCanonicalPath();
			} else if (StringUtils.containsIgnoreCase(osName, "linux")) {
				cmd = "xdg-open " + LOGFILE.getCanonicalPath();
			} else if (StringUtils.containsIgnoreCase(osName, "mac")) {
				cmd = "open " + LOGFILE.getCanonicalPath();
			}
			if (cmd != null) {
				logger.info(cmd);
				Runtime.getRuntime().exec(cmd);
			}
		} catch (IOException e) {
			logger.error(e);
		}
	}
}
