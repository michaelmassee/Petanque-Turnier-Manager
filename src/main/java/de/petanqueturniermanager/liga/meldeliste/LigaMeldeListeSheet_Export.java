/**
* Erstellung : 30.06.2022 / Michael Massee
**/

package de.petanqueturniermanager.liga.meldeliste;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.sheet.io.HtmlExport;
import de.petanqueturniermanager.helper.sheet.io.PdfExport;
import de.petanqueturniermanager.liga.rangliste.LigaRanglisteSheet;
import de.petanqueturniermanager.liga.spielplan.LigaSpielPlanSheet;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Exportiert die Tabellen nach pdf und erstelt eine html datei
 *
 */

public class LigaMeldeListeSheet_Export extends AbstractLigaMeldeListeSheet {

	// TODO get this from properties
	private static String PDF_IMAGE = "<img src=\"http://bc-linden.de/images/bclinden/pdf-download.png\" align=\"right\" style=\"width:50px;\">";
	private static String BFL_IMAGE = "<img src=\"http://bc-linden.de/oeffentlich/bfl/logo.png\" align=\"right\" style=\"width:80px;\">";
	private static String PDF_BASE_URL = "http://bc-linden.de/oeffentlich/bfl/";

	private static final Logger logger = LogManager.getLogger(LigaMeldeListeSheet_Export.class);

	public LigaMeldeListeSheet_Export(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, "Liga Export");
	}

	@Override
	protected void doRun() throws GenerateException {

		if (getTurnierSystem() != TurnierSystem.LIGA) {
			throw new GenerateException("Kein oder falsches Turniersystem. " + getTurnierSystem());
		}

		upDateSheet();
		ProcessBox().info("Exportiere nach PDF");

		LigaSpielPlanSheet ligaSpielPlanSheet = new LigaSpielPlanSheet(getWorkingSpreadsheet());
		ProcessBox().info(PdfExport.from(getWorkingSpreadsheet()).sheetName(LigaSpielPlanSheet.SHEET_NAMEN)
				.range(ligaSpielPlanSheet.printBereichRangePosition()).prefix1(LigaSpielPlanSheet.SHEET_NAMEN)
				.doExport().toString());

		LigaRanglisteSheet ligaRanglisteSheet = new LigaRanglisteSheet(getWorkingSpreadsheet());
		ProcessBox().info(PdfExport.from(getWorkingSpreadsheet()).sheetName(LigaRanglisteSheet.SHEETNAME)
				.range(ligaRanglisteSheet.printBereichRangePosition()).prefix1(LigaRanglisteSheet.SHEETNAME).doExport()
				.toString());

		ProcessBox().info("Exportiere nach HTML");
		URI htmlExportFile = HtmlExport.from(getWorkingSpreadsheet()).doExport();
		ProcessBox().info(htmlExportFile.toString());
		cleanUpLigaHtml(htmlExportFile);
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	private void cleanUpLigaHtml(URI htmlExportFileUri) {
		try {
			File htmlExportFile = new File(htmlExportFileUri);
			String name = FilenameUtils.getName(htmlExportFile.getCanonicalPath());
			name = StringUtils.replace(name, ".html", ".clean.html");
			File target = new File(FilenameUtils.getFullPath(htmlExportFile.getCanonicalPath()), name);
			LigaHtmlCleaner.from(htmlExportFileUri, target).logoUrl(BFL_IMAGE).gruppe("Gruppe 1").pdfImageUrl(PDF_IMAGE)
					.pdfDownloadBaseUrl(PDF_BASE_URL).cleanUp();
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}

	}
}
