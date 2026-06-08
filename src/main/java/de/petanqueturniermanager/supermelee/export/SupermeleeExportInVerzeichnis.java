/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.supermelee.export;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.sheet.io.PdfExport;
import de.petanqueturniermanager.helper.upload.AbstractExportInVerzeichnis;
import de.petanqueturniermanager.helper.upload.ExportErgebnis;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SpieltagRanglisteSheet;

public class SupermeleeExportInVerzeichnis extends AbstractExportInVerzeichnis {

    private static final Logger logger = LogManager.getLogger(SupermeleeExportInVerzeichnis.class);

    public SupermeleeExportInVerzeichnis(WorkingSpreadsheet ws, Path zielVerzeichnis) {
        super(ws, TurnierSystem.SUPERMELEE, "Supermelee Export Verzeichnis", zielVerzeichnis);
    }

    @Override
    protected ExportErgebnis exportiereInVerzeichnis(Path zielVerzeichnis) throws GenerateException {
        processBox().info(I18n.get("export.info.pdf"));

        var ws = getWorkingSpreadsheet();
        var konfiguration = new SuperMeleeKonfigurationSheet(ws);
        var spieltagRanglisteSheet = new SpieltagRanglisteSheet(ws, SpielTagNr.from(1));
        int anzahlSpieltage = spieltagRanglisteSheet.countNumberOfRanglisten();

        String baseDownloadUrl = StringUtils.strip(konfiguration.getDownloadUrl());
        String turnierlogoUrl = StringUtils.strip(konfiguration.getTurnierlogoUrl());
        String turniername = StringUtils.strip(konfiguration.getKopfZeileMitte());

        if (StringUtils.isNotEmpty(baseDownloadUrl)) {
            processBox().info(I18n.get("export.info.download.url", baseDownloadUrl));
        }
        if (StringUtils.isEmpty(turnierlogoUrl)) {
            processBox().info(I18n.get("export.warnung.turnierlogo.fehlt"));
        } else {
            processBox().info(I18n.get("export.info.turnierlogo", turnierlogoUrl));
        }

        List<Path> exportierteDateien = new ArrayList<>();
        List<String> spieltagSheetNamen = new ArrayList<>();
        List<String> spieltagPdfUrls = new ArrayList<>();
        List<String> spieltagTitel = new ArrayList<>();

        for (int nr = 1; nr <= anzahlSpieltage; nr++) {
            String sheetName = SheetNamen.spieltagRangliste(nr);
            String titel = I18n.get("export.supermelee.spieltag", nr);
            Path pdf = Path.of(PdfExport.from(ws)
                    .sheetName(sheetName)
                    .prefix1(sheetName)
                    .zielVerzeichnis(zielVerzeichnis)
                    .doExport());
            processBox().info(pdf.toString());
            exportierteDateien.add(pdf);
            spieltagSheetNamen.add(sheetName);
            spieltagTitel.add(titel);
            spieltagPdfUrls.add(buildPdfUrl(baseDownloadUrl, dateiName(pdf)));
        }

        String endranglisteSheetName = SheetNamen.endrangliste();
        Path pdfEndrangliste = Path.of(PdfExport.from(ws)
                .sheetName(endranglisteSheetName)
                .prefix1(endranglisteSheetName)
                .zielVerzeichnis(zielVerzeichnis)
                .doExport());
        processBox().info(pdfEndrangliste.toString());
        exportierteDateien.add(pdfEndrangliste);

        processBox().info(I18n.get("export.info.html"));
        Path htmlDatei = exportiereHtml(zielVerzeichnis, turniername, turnierlogoUrl, baseDownloadUrl,
                pdfEndrangliste, spieltagSheetNamen, spieltagTitel, spieltagPdfUrls);
        exportierteDateien.add(htmlDatei);

        return new ExportErgebnis(exportierteDateien);
    }

    private Path exportiereHtml(Path zielVerzeichnis, String turniername, String logoUrl, String baseDownloadUrl,
            Path pdfEndrangliste, List<String> sheetNamen, List<String> titel, List<String> pdfUrls)
            throws GenerateException {
        try {
            var seite = SupermeleeHtmlExportSeite.from(getWorkingSpreadsheet())
                    .turniername(turniername)
                    .logoUrl(logoUrl)
                    .endranglistePdfUrl(buildPdfUrl(baseDownloadUrl, dateiName(pdfEndrangliste)));

            for (int i = 0; i < sheetNamen.size(); i++) {
                seite.fuegeSpieltagHinzu(sheetNamen.get(i), titel.get(i), pdfUrls.get(i));
            }

            String html = seite.erstelle();
            Path htmlDatei = htmlZieldatei(zielVerzeichnis);
            Files.writeString(htmlDatei, html, StandardCharsets.UTF_8);
            processBox().info(htmlDatei.toString());
            return htmlDatei;
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new GenerateException(e.getMessage());
        }
    }

    private String dateiName(Path pdf) {
        Path name = pdf.getFileName();
        return name != null ? name.toString() : "";
    }

    private String buildPdfUrl(String baseDownloadUrl, String dateiname) {
        if (StringUtils.isNotBlank(baseDownloadUrl)) {
            String base = baseDownloadUrl.endsWith("/") ? baseDownloadUrl : baseDownloadUrl + "/";
            return base + dateiname;
        }
        return StringUtils.isNotBlank(dateiname) ? dateiname : null;
    }

    private Path htmlZieldatei(Path verzeichnis) throws GenerateException {
        var xStorable = getWorkingSpreadsheet().getXStorable();
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
}
