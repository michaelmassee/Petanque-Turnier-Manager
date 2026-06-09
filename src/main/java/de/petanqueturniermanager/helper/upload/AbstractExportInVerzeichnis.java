package de.petanqueturniermanager.helper.upload;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.sheet.io.PdfExport;

public abstract class AbstractExportInVerzeichnis extends SheetRunner {

    private static final Logger logger = LogManager.getLogger(AbstractExportInVerzeichnis.class);

    private final Path zielVerzeichnis;

    protected AbstractExportInVerzeichnis(WorkingSpreadsheet ws, TurnierSystem ts, String name,
            Path zielVerzeichnis) {
        super(ws, ts, name);
        this.zielVerzeichnis = zielVerzeichnis;
    }

    @Override
    protected final void doRun() throws GenerateException {
        var ergebnis = exportiereInVerzeichnis(zielVerzeichnis);
        ergebnis.speichern(getWorkingSpreadsheet());
    }

    @Override
    protected IKonfigurationSheet getKonfigurationSheet() {
        return null;
    }

    protected abstract ExportErgebnis exportiereInVerzeichnis(Path zielVerzeichnis) throws GenerateException;

    protected Path exportierePdfWennTabelleVorhanden(String sheetName, Path zielVerzeichnis)
            throws GenerateException {
        return exportierePdfWennTabelleVorhanden(sheetName, () -> PdfExport.from(getWorkingSpreadsheet())
                .sheetName(sheetName)
                .prefix1(sheetName)
                .zielVerzeichnis(zielVerzeichnis)
                .doExport());
    }

    protected Path exportierePdfWennTabelleVorhanden(String sheetName, PdfExportAktion aktion)
            throws GenerateException {
        if (getSheetHelper().findByName(sheetName) == null) {
            processBox().info(I18n.get("error.tabelle.nicht.vorhanden", sheetName));
            logger.warn("PDF-Export für Sheet '{}' übersprungen: Tabelle nicht vorhanden", sheetName);
            return null;
        }
        Path pdf = Path.of(aktion.exportiere().toString());
        processBox().info(pdf.toString());
        return pdf;
    }

    @FunctionalInterface
    protected interface PdfExportAktion {
        URI exportiere() throws GenerateException;
    }

    protected Path exportiereHtml(Path zielVerzeichnis, String fallbackDateiname, String titel, String logoUrl,
            List<ExportHtmlSeite.Section> sections) throws GenerateException {
        try {
            String html = ExportHtmlSeite.from(getWorkingSpreadsheet())
                    .titel(titel)
                    .logoUrl(logoUrl)
                    .sections(sections)
                    .erstelle();
            Path htmlDatei = htmlZieldatei(zielVerzeichnis, fallbackDateiname);
            Files.writeString(htmlDatei, html, StandardCharsets.UTF_8);
            processBox().info(htmlDatei.toString());
            return htmlDatei;
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new GenerateException(e.getMessage());
        }
    }

    protected List<String> vorhandeneBuchstabenSheets(Function<String, String> sheetNameFunktion)
            throws GenerateException {
        List<String> sheetNamen = new ArrayList<>();
        for (char c = 'A'; c <= 'Z'; c++) {
            String sheetName = sheetNameFunktion.apply(String.valueOf(c));
            if (getSheetHelper().findByName(sheetName) != null) {
                sheetNamen.add(sheetName);
            }
        }
        return sheetNamen;
    }

    protected String buildPdfUrl(String baseDownloadUrl, Path pdf) {
        String dateiname = dateiName(pdf);
        if (StringUtils.isBlank(dateiname)) {
            return null;
        }
        if (StringUtils.isNotBlank(baseDownloadUrl)) {
            String base = baseDownloadUrl.endsWith("/") ? baseDownloadUrl : baseDownloadUrl + "/";
            return base + dateiname;
        }
        return dateiname;
    }

    protected String dateiName(Path pdf) {
        if (pdf == null) {
            return null;
        }
        Path name = pdf.getFileName();
        return name != null ? name.toString() : null;
    }

    protected Path htmlZieldatei(Path verzeichnis, String fallbackDateiname) throws GenerateException {
        var xStorable = getWorkingSpreadsheet().getXStorable();
        String location = xStorable != null ? xStorable.getLocation() : null;
        return htmlZieldatei(verzeichnis, fallbackDateiname, location);
    }

    static Path htmlZieldatei(Path verzeichnis, String fallbackDateiname, String location)
            throws GenerateException {
        if (StringUtils.isBlank(location)) {
            return verzeichnis.resolve(fallbackDateiname);
        }
        try {
            Path dateiname = Path.of(URI.create(location).toURL().toURI()).getFileName();
            if (dateiname == null) {
                return verzeichnis.resolve(fallbackDateiname);
            }
            String basisName = FilenameUtils.removeExtension(dateiname.toString());
            return verzeichnis.resolve(basisName + ".html");
        } catch (MalformedURLException | URISyntaxException e) {
            throw new GenerateException(e.getMessage());
        }
    }
}
