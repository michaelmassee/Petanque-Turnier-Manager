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

import com.sun.star.container.XNamed;
import com.sun.star.sheet.XPrintAreas;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellContentType;
import com.sun.star.table.CellRangeAddress;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.io.PdfExport;
import de.petanqueturniermanager.webserver.TabelleHtmlRenderer;
import de.petanqueturniermanager.webserver.TabellenMapper;

public abstract class AbstractExportInVerzeichnis extends SheetRunner {

    private static final Logger logger = LogManager.getLogger(AbstractExportInVerzeichnis.class);

    private final Path zielVerzeichnis;
    private final TabellenMapper tabellenMapper = new TabellenMapper();
    private final TabelleHtmlRenderer tabelleHtmlRenderer = new TabelleHtmlRenderer();

    protected AbstractExportInVerzeichnis(WorkingSpreadsheet ws, TurnierSystem ts, String name,
            Path zielVerzeichnis) {
        super(ws, ts, name);
        this.zielVerzeichnis = zielVerzeichnis;
    }

    @Override
    protected final void doRun() throws GenerateException {
        var ergebnis = exportiere();
        ergebnis.speichern(getWorkingSpreadsheet());
    }

    public final ExportErgebnis exportiere() throws GenerateException {
        return exportiereInVerzeichnis(zielVerzeichnis);
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

    protected Path exportierePdfAusHtml(String sheetName, String abschnittTitel, Path zielVerzeichnis)
            throws GenerateException {
        var sheet = getSheetHelper().findByName(sheetName);
        if (sheet == null) {
            processBox().info(I18n.get("error.tabelle.nicht.vorhanden", sheetName));
            logger.warn("PDF-Export für Sheet '{}' übersprungen: Tabelle nicht vorhanden", sheetName);
            return null;
        }
        var doc = getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
        var model = tabellenMapper.map(sheet, doc);
        var tabelleFragment = tabelleHtmlRenderer.render(model);
        var html = PdfHtmlDokument.erstelle(abschnittTitel, tabelleFragment);
        var zieldatei = pdfZieldatei(sheetName, zielVerzeichnis);
        var pdf = HtmlZuPdfKonvertierer.konvertiere(html, zieldatei);
        processBox().info(pdf.toString());
        return pdf;
    }

    protected Path pdfZieldatei(String sheetName, Path zielVerzeichnis) throws GenerateException {
        var xStorable = getWorkingSpreadsheet().getXStorable();
        String location = xStorable != null ? xStorable.getLocation() : null;
        return pdfZieldatei(sheetName, zielVerzeichnis, location);
    }

    static Path pdfZieldatei(String sheetName, Path zielVerzeichnis, String location) throws GenerateException {
        String fallback = sheetName + ".pdf";
        if (StringUtils.isBlank(location)) {
            return zielVerzeichnis.resolve(fallback);
        }
        try {
            Path dateiname = Path.of(URI.create(location).toURL().toURI()).getFileName();
            if (dateiname == null) {
                return zielVerzeichnis.resolve(fallback);
            }
            String basisName = FilenameUtils.removeExtension(dateiname.toString());
            return zielVerzeichnis.resolve(sheetName + "_" + basisName + ".pdf");
        } catch (IllegalArgumentException | MalformedURLException | URISyntaxException e) {
            throw new GenerateException(e.getMessage());
        }
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

    /**
     * Löst den aktuellen Sheet-Namen über den Metadaten-Schlüssel auf.
     * Gibt den Fallback-Namen zurück, wenn kein Sheet gefunden wird.
     */
    protected String sheetNamePerSchluessel(String schluessel, String fallbackName) {
        var xSheet = SheetMetadataHelper.findeSheetUndHeile(
                getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), schluessel, fallbackName);
        return xSheet != null ? Lo.qi(XNamed.class, xSheet).getName() : fallbackName;
    }

    /**
     * Iteriert A–Z und liefert alle Sheets zurück, die über Metadaten-Schlüssel oder Fallback-Name gefunden werden.
     */
    protected List<SheetEintrag> buchstabenSheetEintraegePerSchluessel(
            Function<String, String> schluesselFunktion, Function<String, String> fallbackFunktion) {
        var xDoc = getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
        var result = new ArrayList<SheetEintrag>();
        for (char c = 'A'; c <= 'Z'; c++) {
            var buchstabe = String.valueOf(c);
            var schluessel = schluesselFunktion.apply(buchstabe);
            var xSheet = SheetMetadataHelper.findeSheetUndHeile(xDoc, schluessel, fallbackFunktion.apply(buchstabe));
            if (xSheet != null) {
                result.add(new SheetEintrag(buchstabe, schluessel, Lo.qi(XNamed.class, xSheet).getName()));
            }
        }
        return result;
    }

    public record SheetEintrag(String buchstabe, String schluessel, String sheetName) {}

    protected String buildPdfUrl(Path pdf) {
        return dateiName(pdf);
    }

    protected String dateiName(Path pdf) {
        if (pdf == null) {
            return null;
        }
        Path name = pdf.getFileName();
        return name != null ? name.toString() : null;
    }

    /**
     * Exportiert die HTML-Seite mit temporär angepasstem Druckbereich der Meldeliste.
     * Überspringt Zeile 0 (Überschrift) und schneidet leere Zeilen am Ende ab.
     * Der originale Druckbereich wird nach dem Export wiederhergestellt.
     */
    protected Path exportiereHtmlMitMeldelisteDruckbereich(
            boolean meldelisteExportieren, String meldelisteSheetName,
            Path zielVerzeichnis, String fallbackDateiname, String titel, String logoUrl,
            List<ExportHtmlSeite.Section> sections) throws GenerateException {

        if (!meldelisteExportieren || StringUtils.isBlank(meldelisteSheetName)) {
            return exportiereHtml(zielVerzeichnis, fallbackDateiname, titel, logoUrl, sections);
        }

        var sheet = getSheetHelper().findByName(meldelisteSheetName);
        if (sheet == null) {
            return exportiereHtml(zielVerzeichnis, fallbackDateiname, titel, logoUrl, sections);
        }
        var printAreas = Lo.qi(XPrintAreas.class, sheet);
        if (printAreas == null) {
            return exportiereHtml(zielVerzeichnis, fallbackDateiname, titel, logoUrl, sections);
        }
        var originalBereiche = printAreas.getPrintAreas();
        if (originalBereiche == null || originalBereiche.length == 0) {
            return exportiereHtml(zielVerzeichnis, fallbackDateiname, titel, logoUrl, sections);
        }

        var original = originalBereiche[0];
        var neuerBereich = new CellRangeAddress();
        neuerBereich.Sheet = original.Sheet;
        neuerBereich.StartRow = MeldeListeKonstanten.ZWEITE_HEADER_ZEILE;
        neuerBereich.StartColumn = original.StartColumn;
        neuerBereich.EndRow = letzteZeileMitDaten(sheet, original.EndRow);
        neuerBereich.EndColumn = original.EndColumn;
        printAreas.setPrintAreas(new CellRangeAddress[] { neuerBereich });
        try {
            return exportiereHtml(zielVerzeichnis, fallbackDateiname, titel, logoUrl, sections);
        } finally {
            printAreas.setPrintAreas(originalBereiche);
        }
    }

    private int letzteZeileMitDaten(XSpreadsheet sheet, int maxZeile) {
        for (int zeile = maxZeile; zeile >= MeldeListeKonstanten.ERSTE_DATEN_ZEILE; zeile--) {
            try {
                var zelle = sheet.getCellByPosition(MeldeListeKonstanten.SPIELER_NR_SPALTE, zeile);
                if (zelle.getType() == CellContentType.VALUE && zelle.getValue() > 0) {
                    return zeile;
                }
            } catch (Exception e) {
                logger.debug("Fehler beim Prüfen von Zeile {}", zeile, e);
            }
        }
        return MeldeListeKonstanten.ZWEITE_HEADER_ZEILE;
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
        } catch (IllegalArgumentException | MalformedURLException | URISyntaxException e) {
            throw new GenerateException(e.getMessage());
        }
    }
}
