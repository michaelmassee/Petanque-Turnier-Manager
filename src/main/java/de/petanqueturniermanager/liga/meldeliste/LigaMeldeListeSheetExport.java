/*
 * Erstellung : 30.06.2022 / Michael Massee
 **/

package de.petanqueturniermanager.liga.meldeliste;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.frame.XStorable;
import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.IMeldeliste;
import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.io.PdfExport;
import de.petanqueturniermanager.helper.upload.ExportErgebnis;
import de.petanqueturniermanager.liga.konfiguration.LigaKonfigurationSheet;
import de.petanqueturniermanager.liga.rangliste.LigaRanglisteSheet;
import de.petanqueturniermanager.liga.spielplan.LigaSpielPlanSheet;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;

/**
 * Exportiert die Tabellen nach pdf und erstelt eine html datei
 *
 */

public class LigaMeldeListeSheetExport extends SheetRunner implements IMeldeliste<TeamMeldungen, Team> {

	private static final Logger logger = LogManager.getLogger(LigaMeldeListeSheetExport.class);

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
	public int letzteSpielTagSpalte() {
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
		exportiere(elternVerzeichnis());
	}

	public ExportErgebnis exportiere(Path zielVerzeichnis) throws GenerateException {
		delegate.upDateSheet();
		processBox().info(I18n.get("export.info.pdf"));

		var ws = getWorkingSpreadsheet();
		var ligaSpielPlanSheet = new LigaSpielPlanSheet(ws);
		var ligaRanglisteSheet = new LigaRanglisteSheet(ws);

		Path pdfSpielplan = Path.of(PdfExport.from(ws)
				.sheetName(LigaSpielPlanSheet.sheetName())
				.range(ligaSpielPlanSheet.printBereichRangePosition())
				.prefix1(LigaSpielPlanSheet.sheetName())
				.zielVerzeichnis(zielVerzeichnis)
				.doExport());
		processBox().info(pdfSpielplan.toString());

		Path pdfRangliste = Path.of(PdfExport.from(ws)
				.sheetName(SheetNamen.rangliste())
				.range(ligaRanglisteSheet.printBereichRangePosition())
				.prefix1(SheetNamen.rangliste())
				.zielVerzeichnis(zielVerzeichnis)
				.doExport());
		processBox().info(pdfRangliste.toString());

		processBox().info(I18n.get("export.info.html"));
		Path htmlDatei = exportiereHtml(zielVerzeichnis, pdfSpielplan, pdfRangliste);

		return new ExportErgebnis(List.of(pdfSpielplan, pdfRangliste, htmlDatei));
	}

	private Path exportiereHtml(Path zielVerzeichnis, Path pdfSpielplan, Path pdfRangliste)
			throws GenerateException {
		try {
			LigaKonfigurationSheet konfiguration = delegate.getKonfigurationSheet();
			String baseDownloadUrl = StringUtils.strip(konfiguration.getBaseDownloadUrl());
			String turnierlogoUrl = StringUtils.strip(konfiguration.getTurnierlogoUrl());
			String gruppenname = StringUtils.strip(konfiguration.getGruppenname());

			if (StringUtils.isNotEmpty(baseDownloadUrl)) {
				processBox().info(I18n.get("export.info.download.url", baseDownloadUrl));
			}
			if (StringUtils.isEmpty(turnierlogoUrl)) {
				processBox().info(I18n.get("export.warnung.turnierlogo.fehlt"));
			} else {
				processBox().info(I18n.get("export.info.turnierlogo", turnierlogoUrl));
			}

			Path spielplanDateiName = pdfSpielplan.getFileName();
			Path ranglisteDateiName = pdfRangliste.getFileName();
			if (spielplanDateiName == null || ranglisteDateiName == null) {
				throw new GenerateException(I18n.get("export.fehler.dateipfad"));
			}
			String html = LigaHtmlExportSeite.from(getWorkingSpreadsheet())
					.logoUrl(turnierlogoUrl)
					.gruppenname(gruppenname)
					.spielplanPdfUrl(buildPdfUrl(baseDownloadUrl, spielplanDateiName.toString()))
					.ranglistePdfUrl(buildPdfUrl(baseDownloadUrl, ranglisteDateiName.toString()))
					.erstelle();

			Path htmlDatei = htmlZieldatei(zielVerzeichnis);
			Files.writeString(htmlDatei, html, StandardCharsets.UTF_8);
			processBox().info(htmlDatei.toString());
			return htmlDatei;
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new GenerateException(e.getMessage());
		}
	}

	private String buildPdfUrl(String baseDownloadUrl, String dateiname) {
		if (StringUtils.isNotBlank(baseDownloadUrl)) {
			String base = baseDownloadUrl.endsWith("/") ? baseDownloadUrl : baseDownloadUrl + "/";
			return base + dateiname;
		}
		return StringUtils.isNotBlank(dateiname) ? dateiname : null;
	}

	private Path htmlZieldatei(Path verzeichnis) throws GenerateException {
		XStorable xStorable = getWorkingSpreadsheet().getXStorable();
		String location = xStorable != null ? xStorable.getLocation() : null;
		if (StringUtils.isBlank(location)) {
			throw new GenerateException(I18n.get("error.dokument.nicht.gespeichert"));
		}
		try {
			Path dateiname = Path.of(URI.create(location).toURL().toURI()).getFileName();
			if (dateiname == null) {
				throw new GenerateException(I18n.get("error.dokument.nicht.gespeichert"));
			}
			String basisName = FilenameUtils.removeExtension(dateiname.toString());
			return verzeichnis.resolve(basisName + ".html");
		} catch (MalformedURLException | URISyntaxException e) {
			throw new GenerateException(e.getMessage());
		}
	}

	private Path elternVerzeichnis() throws GenerateException {
		XStorable xStorable = getWorkingSpreadsheet().getXStorable();
		String location = xStorable != null ? xStorable.getLocation() : null;
		if (StringUtils.isBlank(location)) {
			throw new GenerateException(I18n.get("error.dokument.nicht.gespeichert"));
		}
		try {
			Path eltern = Path.of(URI.create(location).toURL().toURI()).getParent();
			if (eltern == null) {
				throw new GenerateException(I18n.get("error.dokument.nicht.gespeichert"));
			}
			return eltern;
		} catch (MalformedURLException | URISyntaxException e) {
			throw new GenerateException(e.getMessage());
		}
	}
}
