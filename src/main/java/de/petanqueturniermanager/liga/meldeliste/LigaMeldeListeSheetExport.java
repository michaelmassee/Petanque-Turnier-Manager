/**
 * Erstellung : 30.06.2022 / Michael Massee
 **/

package de.petanqueturniermanager.liga.meldeliste;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.IMeldeliste;
import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.io.HtmlExport;
import de.petanqueturniermanager.helper.sheet.io.PdfExport;
import de.petanqueturniermanager.liga.konfiguration.LigaKonfigurationSheet;
import de.petanqueturniermanager.liga.rangliste.LigaRanglisteSheet;
import de.petanqueturniermanager.liga.spielplan.LigaSpielPlanSheet;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Exportiert die Tabellen nach pdf und erstelt eine html datei
 *
 */

public class LigaMeldeListeSheetExport extends SheetRunner implements IMeldeliste<TeamMeldungen, Team> {

	private static final Logger logger = LogManager.getLogger(LigaMeldeListeSheetExport.class);
	private static final String PDF_BILDER_VERZEICHNIS = "images";
	private static final String PDF_BILD_DATEINAME = "pdf-download.png";
	private static final String PDF_BILD_RESSOURCE = PDF_BILDER_VERZEICHNIS + "/" + PDF_BILD_DATEINAME;

	private final LigaMeldeListeDelegate delegate;

	public LigaMeldeListeSheetExport(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.LIGA, "Liga Export");
		delegate = new LigaMeldeListeDelegate(this, workingSpreadsheet, TurnierSystem.LIGA,
				SheetMetadataHelper.SCHLUESSEL_LIGA_MELDELISTE);
	}

	@Override
	protected LigaKonfigurationSheet getKonfigurationSheet() {
		return delegate.getKonfigurationSheet();
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return getSheetHelper().findByName(SheetNamen.meldeliste());
	}

	@Override
	public final TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	@Override
	public MeldungenSpalte<TeamMeldungen, Team> getMeldungenSpalte() {
		return delegate.getMeldungenSpalte();
	}

	@Override
	public String formulaSverweisSpielernamen(String spielrNrAdresse) {
		return delegate.formulaSverweisSpielernamen(spielrNrAdresse);
	}

	@Override
	public TeamMeldungen getAktiveUndAusgesetztMeldungen() throws GenerateException {
		return getAlleMeldungen();
	}

	@Override
	public TeamMeldungen getAktiveMeldungen() throws GenerateException {
		return getAlleMeldungen();
	}

	@Override
	public TeamMeldungen getInAktiveMeldungen() throws GenerateException {
		return new TeamMeldungen();
	}

	@Override
	public TeamMeldungen getAlleMeldungen() throws GenerateException {
		return delegate.getAlleMeldungen();
	}

	@Override
	public int letzteSpielTagSpalte() throws GenerateException {
		return delegate.letzteSpielTagSpalte();
	}

	@Override
	public int getSpielerNameErsteSpalte() {
		return delegate.getSpielerNameErsteSpalte();
	}

	@Override
	public int getLetzteDatenZeileUseMin() throws GenerateException {
		return delegate.getLetzteDatenZeileUseMin();
	}

	@Override
	public int getSpielerZeileNr(int spielerNr) throws GenerateException {
		return delegate.getSpielerZeileNr(spielerNr);
	}

	@Override
	public int naechsteFreieDatenZeileInSpielerNrSpalte() throws GenerateException {
		return delegate.naechsteFreieDatenZeileInSpielerNrSpalte();
	}

	@Override
	public int getLetzteMitDatenZeileInSpielerNrSpalte() throws GenerateException {
		return delegate.getLetzteMitDatenZeileInSpielerNrSpalte();
	}

	@Override
	public int getErsteDatenZiele() {
		return delegate.getErsteDatenZiele();
	}

	@Override
	public List<String> getSpielerNamenList() throws GenerateException {
		return delegate.getSpielerNamenList();
	}

	@Override
	public List<Integer> getSpielerNrList() throws GenerateException {
		return delegate.getSpielerNrList();
	}

	@Override
	public int letzteZeileMitSpielerName() throws GenerateException {
		return delegate.letzteZeileMitSpielerName();
	}

	@Override
	protected void doRun() throws GenerateException {

		if (getTurnierSystem() != TurnierSystem.LIGA) {
			throw new GenerateException(I18n.get("error.turniersystem.falsch", getTurnierSystem()));
		}

		delegate.upDateSheet();
		processBox().info("Exportiere nach PDF");

		LigaSpielPlanSheet ligaSpielPlanSheet = new LigaSpielPlanSheet(getWorkingSpreadsheet());
		String fileNamePdfSpielplan = PdfExport.from(getWorkingSpreadsheet()).sheetName(LigaSpielPlanSheet.sheetName())
				.range(ligaSpielPlanSheet.printBereichRangePosition()).prefix1(LigaSpielPlanSheet.sheetName())
				.doExport().toString();
		processBox().info(fileNamePdfSpielplan);

		LigaRanglisteSheet ligaRanglisteSheet = new LigaRanglisteSheet(getWorkingSpreadsheet());
		String fileNamePdfRangliste = PdfExport.from(getWorkingSpreadsheet()).sheetName(SheetNamen.rangliste())
				.range(ligaRanglisteSheet.printBereichRangePosition()).prefix1(SheetNamen.rangliste()).doExport()
				.toString();
		processBox().info(fileNamePdfRangliste);

		processBox().info("Exportiere nach HTML");
		URI htmlExportFile = HtmlExport.from(getWorkingSpreadsheet()).doExport();
		processBox().info(htmlExportFile.toString());
		cleanUpLigaHtml(htmlExportFile, fileNamePdfSpielplan, fileNamePdfRangliste);
	}

	private void cleanUpLigaHtml(URI htmlExportFileUri, String fileNamePdfSpielplan, String fileNamePdfRangliste) {

		processBox().info("Clean und reformat html");

		String fileNameOnlyPdfSpielplan = FilenameUtils.getName(fileNamePdfSpielplan);
		String fileNameOnlyPdfRangliste = FilenameUtils.getName(fileNamePdfRangliste);

		try {

			String baseDownloadUrl = StringUtils.strip(delegate.getKonfigurationSheet().getBaseDownloadUrl());
			if (StringUtils.isNotEmpty(baseDownloadUrl)) {
				processBox().info("Download URL Verzeichnis: " + baseDownloadUrl);
			}

			String turnierlogoUrl = StringUtils.strip(delegate.getKonfigurationSheet().getTurnierlogoUrl());
			if (StringUtils.isEmpty(turnierlogoUrl)) {
				processBox().info(I18n.get("export.warnung.turnierlogo.fehlt"));
			} else {
				processBox().info(I18n.get("export.info.turnierlogo", turnierlogoUrl));
			}

			File htmlExportFile = new File(htmlExportFileUri);
			File htmlExportVerzeichnis = htmlExportFile.getParentFile();

			String pdfImgUr = pdfBildInExportVerzeichnisKopieren(htmlExportVerzeichnis);
			if (pdfImgUr != null) {
				processBox().info(I18n.get("liga.export.pdf.bild.kopiert"));
			}

			String gruppenname = StringUtils.strip(delegate.getKonfigurationSheet().getGruppenname());
			if (StringUtils.isEmpty(gruppenname)) {
				processBox().info("Warnung: Gruppenname fehlt in der Turnierkonfiguration");
			} else {
				processBox().info("Gruppenname: " + gruppenname);
			}

			String name = FilenameUtils.getName(htmlExportFile.getCanonicalPath());
			name = StringUtils.replace(name, ".html", ".clean.html");
			File target = new File(FilenameUtils.getFullPath(htmlExportFile.getCanonicalPath()), name);
			File cleanHtml = LigaHtmlCleaner.from(htmlExportFileUri, target).logoUrl(turnierlogoUrl)
					.ranglistePdfName(fileNameOnlyPdfRangliste).spielplanPdfName(fileNameOnlyPdfSpielplan)
					.pdfImageUrl(pdfImgUr).pdfDownloadBaseUrl(baseDownloadUrl).cleanUp();

			processBox().info(cleanHtml.toString());
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}

	private String pdfBildInExportVerzeichnisKopieren(File htmlExportVerzeichnis) throws IOException {
		File bilderVerzeichnis = new File(htmlExportVerzeichnis, PDF_BILDER_VERZEICHNIS);

		if (!bilderVerzeichnis.exists() && !bilderVerzeichnis.mkdirs()) {
			throw new IOException("Konnte Bilderverzeichnis nicht erstellen: " + bilderVerzeichnis);
		}

		File zielDatei = new File(bilderVerzeichnis, PDF_BILD_DATEINAME);
		if (!zielDatei.exists()) {
			try (InputStream in = LigaMeldeListeSheetExport.class.getResourceAsStream(PDF_BILD_RESSOURCE)) {
				if (in == null) {
					logger.error("Classpath-Ressource nicht gefunden: {}", PDF_BILD_RESSOURCE);
					processBox().info(I18n.get("liga.export.pdf.bild.ressource.fehler"));
					return null;
				}
				try (var out = new FileOutputStream(zielDatei)) {
					in.transferTo(out);
				}
			}
		}
		return PDF_BILD_RESSOURCE;
	}
}
