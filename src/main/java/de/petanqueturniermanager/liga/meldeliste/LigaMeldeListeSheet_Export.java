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
		String fileNamePdfSpielplan = PdfExport.from(getWorkingSpreadsheet()).sheetName(LigaSpielPlanSheet.SHEET_NAMEN)
				.range(ligaSpielPlanSheet.printBereichRangePosition()).prefix1(LigaSpielPlanSheet.SHEET_NAMEN)
				.doExport().toString();
		ProcessBox().info(fileNamePdfSpielplan);

		LigaRanglisteSheet ligaRanglisteSheet = new LigaRanglisteSheet(getWorkingSpreadsheet());
		String fileNamePdfRangliste = PdfExport.from(getWorkingSpreadsheet()).sheetName(LigaRanglisteSheet.SHEETNAME)
				.range(ligaRanglisteSheet.printBereichRangePosition()).prefix1(LigaRanglisteSheet.SHEETNAME).doExport()
				.toString();
		ProcessBox().info(fileNamePdfRangliste);

		ProcessBox().info("Exportiere nach HTML");
		URI htmlExportFile = HtmlExport.from(getWorkingSpreadsheet()).doExport();
		ProcessBox().info(htmlExportFile.toString());
		cleanUpLigaHtml(htmlExportFile, fileNamePdfSpielplan, fileNamePdfRangliste);
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	private void cleanUpLigaHtml(URI htmlExportFileUri, String fileNamePdfSpielplan, String fileNamePdfRangliste) {

		ProcessBox().info("Clean und reformat html");

		String fileNameOnlyPdfSpielplan = FilenameUtils.getName(fileNamePdfSpielplan);
		String fileNameOnlyPdfRangliste = FilenameUtils.getName(fileNamePdfRangliste);

		try {

			String baseDownloadUrl = StringUtils.strip(getKonfigurationSheet().getBaseDownloadUrl());
			if (StringUtils.isEmpty(baseDownloadUrl)) {
				ProcessBox().info("Warning: Download URL Verzeichnis fehlt in der Turnier Konfiguration");
			} else {
				ProcessBox().info("Download URL Verzeichnis: " + baseDownloadUrl);
			}

			String ligaLogoUr = StringUtils.strip(getKonfigurationSheet().getLigaLogoUr());
			if (StringUtils.isEmpty(ligaLogoUr)) {
				ProcessBox().info("Warning: Liga logo fehlt in der Turnier Konfiguration");
			} else {
				ProcessBox().info("Liga logo: " + ligaLogoUr);
			}

			String gruppennamen = StringUtils.strip(getKonfigurationSheet().getGruppennamen());
			if (StringUtils.isEmpty(gruppennamen)) {
				ProcessBox().info("Warning: Gruppennamen fehlt in der Turnier Konfiguration");
			} else {
				ProcessBox().info("Gruppennamen: " + gruppennamen);
			}

			File htmlExportFile = new File(htmlExportFileUri);
			String name = FilenameUtils.getName(htmlExportFile.getCanonicalPath());
			name = StringUtils.replace(name, ".html", ".clean.html");
			File target = new File(FilenameUtils.getFullPath(htmlExportFile.getCanonicalPath()), name);
			File cleanHtml = LigaHtmlCleaner.from(htmlExportFileUri, target).logoUrl(ligaLogoUr)
					.ranglistePdfName(fileNameOnlyPdfRangliste).spielplanPdfName(fileNameOnlyPdfSpielplan)
					.pdfDownloadBaseUrl(baseDownloadUrl).cleanUp();

			ProcessBox().info(cleanHtml.toString());
		} catch (IOException | GenerateException e) {
			logger.error(e.getMessage(), e);
		}

	}
}
