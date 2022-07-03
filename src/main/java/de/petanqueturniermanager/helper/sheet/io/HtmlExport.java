package de.petanqueturniermanager.helper.sheet.io;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.io.IOException;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;

/**
 * Erstellung 03.07.2022 / Michael Massee
 */

/**
 * save all to html
 */

public class HtmlExport extends AbstractStore<HtmlExport> {

	private static final String CALC_HTML_EXPORT_FILTER = "HTML (StarCalc)";

	private static final Logger logger = LogManager.getLogger(HtmlExport.class);

	private HtmlExport(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
	}

	public static HtmlExport from(WorkingSpreadsheet workingSpreadsheet) {
		return new HtmlExport(workingSpreadsheet);
	}

	public URI doExport() throws GenerateException {
		URI htmlFile = null;

		if (!istGespeichert()) {
			logger.warn("Dokument wurde noch nicht gespeichert, Dateiname fehlt.");
			throw new GenerateException("Dokument wurde noch nicht gespeichert, Dateiname fehlt.");
		}

		String newFileName;
		try {
			newFileName = newFileName(null);
			newFileName = FilenameUtils.removeExtension(newFileName);
			htmlFile = newLocationInSameDir(newFileName + ".html");

			logger.info("Erstelle HTML :" + htmlFile);
			// Map<String, Object> filterData = new HashMap<>();

			Map<String, Object> saveprops = new HashMap<>();
			saveprops.put(SAVE_PROP_OVERWRITE, Boolean.TRUE);
			saveprops.put(SAVE_PROP_FILTER_NAME, CALC_HTML_EXPORT_FILTER);

			// stores the object's persistent data to a URL and continues to be a representation of the old URL
			getxStorable().storeToURL(htmlFile.toString(), map2Proplist(saveprops));
		} catch (MalformedURLException | URISyntaxException | IOException e) {
			logger.error(e.getMessage(), e);
		}

		return htmlFile;

	}

}