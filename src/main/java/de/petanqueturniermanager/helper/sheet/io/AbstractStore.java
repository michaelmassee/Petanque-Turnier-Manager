package de.petanqueturniermanager.helper.sheet.io;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;

import com.sun.star.frame.XStorable;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.sheet.SheetHelper;

/**
 * Speichern und Exportieren <br>
 * https://wiki.openoffice.org/wiki/Documentation/DevGuide/OfficeDev/Storing_Documents<br>
 * https://www.openoffice.org/api/docs/common/ref/com/sun/star/frame/XStorable.html<br>
 * https://wiki.openoffice.org/wiki/Framework/Article/Filter/FilterList_OOo_2_1<br>
 * https://wiki.openoffice.org/wiki/Documentation/DevGuide/Spreadsheets/Saving_Spreadsheet_Documents<br>
 *
 */
public abstract class AbstractStore<T> {

	protected static final String SAVE_PROP_FILTER_NAME = "FilterName";
	protected static final String SAVE_PROP_OVERWRITE = "Overwrite";

	protected static final String SAVE_PROP_FILTERDATA = "FilterData";
	protected static final String SAVE_PROP_FILTER_PAGERANGE = "PageRange";
	protected static final String SAVE_PROP_FILTER_SELECTION = "Selection";
	protected static final String SAVE_PROP_FILTER_EXPORTBOOKMARKS = "ExportBookmarks";
	protected static final String SAVE_PROP_FILTER_EXPORTNOTES = "ExportNotes";

	private final XStorable xStorable;
	private final String location;
	private final SheetHelper sheetHelper;
	private final WorkingSpreadsheet workingSpreadsheet;

	private String filePrefix1;
	private String filePrefix2;

	protected AbstractStore(WorkingSpreadsheet workingSpreadsheet) {
		this.workingSpreadsheet = workingSpreadsheet;
		xStorable = workingSpreadsheet.getXStorable();
		location = xStorable.getLocation();
		sheetHelper = new SheetHelper(workingSpreadsheet);
	}

	public final T prefix1(String filePrefix1) {
		this.filePrefix1 = filePrefix1;
		return (T) this;
	}

	public final T prefix2(String filePrefix2) {
		this.filePrefix2 = filePrefix2;
		return (T) this;
	}

	protected final String newFileName(String prefix) throws MalformedURLException, URISyntaxException {
		return (StringUtils.isEmpty(prefix) ? "" : prefix + "_")
				+ (StringUtils.isEmpty(filePrefix1) ? "" : filePrefix1 + "_")
				+ (StringUtils.isEmpty(filePrefix2) ? "" : filePrefix2 + "_") + orgFileName().toString();
	}

	protected final URI newLocationInSameDir(String FileName) throws MalformedURLException, URISyntaxException {
		return parentDir().resolve(FileName).toUri();
	}

	protected final Path parentDir() throws MalformedURLException, URISyntaxException {
		URL docUrl = new URL(getLocation());
		Path path = Path.of(docUrl.toURI());
		return path.getParent();
	}

	protected final Path orgFileName() throws MalformedURLException, URISyntaxException {
		URL docUrl = new URL(getLocation());
		Path path = Path.of(docUrl.toURI());
		return path.getFileName();
	}

	protected final boolean istGespeichertMitWarnmeldung() {
		boolean istGespeichert = istGespeichert();
		if (!istGespeichert) {
			MessageBox.from(workingSpreadsheet.getxContext(), MessageBoxTypeEnum.WARN_OK)
					.caption("Datei nicht gespeichert").message("Das aktuelle Dokument ist noch nicht gespeichert.")
					.show();
		}
		return istGespeichert;
	}

	protected final boolean istGespeichert() {
		return !StringUtils.isAllBlank(getLocation());

	}

	protected final XStorable getxStorable() {
		return xStorable;
	}

	protected final String getLocation() {
		return location;
	}

	protected SheetHelper getSheetHelper() {
		return sheetHelper;
	}

	protected WorkingSpreadsheet getWorkingSpreadsheet() {
		return workingSpreadsheet;
	}

}
