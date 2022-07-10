package de.petanqueturniermanager.helper.sheet.io;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.io.IOException;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;

/**
 * Speichern und Exportieren <br>
 * https://wiki.openoffice.org/wiki/Documentation/DevGuide/OfficeDev/Storing_Documents<br>
 * https://www.openoffice.org/api/docs/common/ref/com/sun/star/frame/XStorable.html<br>
 * https://wiki.openoffice.org/wiki/Framework/Article/Filter/FilterList_OOo_2_1<br>
 * https://wiki.openoffice.org/wiki/Documentation/DevGuide/Spreadsheets/Saving_Spreadsheet_Documents<br>
 * 
 * @author michael
 *
 */
public class BackUp extends AbstractStore<BackUp> {

	private static final Logger logger = LogManager.getLogger(BackUp.class);

	protected static final String SAVE_PROP_FILTER_NAME = "FilterName";
	protected static final String SAVE_PROP_OVERWRITE = "Overwrite";
	protected static final String SAVE_PROP_PAGERANGE = "PageRange";
	protected static final String SAVE_PROP_FILTERDATA = "FilterData";

	private BackUp(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
	}

	public static BackUp from(WorkingSpreadsheet workingSpreadsheet) {
		return new BackUp(workingSpreadsheet);
	}

	public BackUp doSave() {
		if (istGespeichert()) {
			logger.info("Speichern :" + getLocation());
			try {
				getxStorable().store();
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
		}
		return this;
	}

	public BackUp doBackUp() {
		try {
			if (istGespeichert()) {
				// doc wurde bereits gespeichert
				// generate file name
				String dateStmp = DateFormatUtils.format(new Date(), "ddMMyyyy_HHmmss");
				String newFileName = newFileName(dateStmp);
				URI newLocation = newLocationInSameDir(newFileName);
				logger.info("Erstelle Backup :" + newLocation);

				Map<String, Object> savprops = new HashMap<>();
				savprops.put(SAVE_PROP_OVERWRITE, Boolean.TRUE);
				// stores the object's persistent data to a URL and continues to be a representation of the old URL
				getxStorable().storeToURL(newLocation.toString(), map2Proplist(savprops));
			}
		} catch (MalformedURLException | URISyntaxException | IOException e) {
			logger.error(e.getMessage(), e);
		}

		return this;
	}

	public BackUp doExportToHtml() {

//		XStorable xStorableHTML = (XStorable) Lo.qi(XStorable.class, xSpreadsheetComponent);
//		PropertyValue[] storeHTMLProps = new PropertyValue[1];
//		storeHTMLProps[0] = new PropertyValue();
//		storeHTMLProps[0].Name = "FilterName";
//		storeHTMLProps[0].Value = "HTML (StarCalc)";
//
//		// In the case that the extension of the file is unknown, we add
//		// the extension
//		if (!(path.endsWith(".html") || path.endsWith(".HTML"))) {
//			path = path.concat(".html");
//		}
//
//		xStorableHTML.storeToURL("file:///" + path, storeHTMLProps);		

		return this;
	}

}
