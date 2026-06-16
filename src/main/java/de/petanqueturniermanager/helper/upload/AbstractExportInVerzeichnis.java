package de.petanqueturniermanager.helper.upload;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
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
    private final TabelleHtmlRenderer pdfTabelleHtmlRenderer = TabelleHtmlRenderer.fuerPdf();

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
        var tabelleFragment = pdfTabelleHtmlRenderer.render(model);
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

    protected HtmlExportErgebnis exportiereHtml(Path zielVerzeichnis, String fallbackDateiname, String titel, String logoUrl,
            List<ExportHtmlSeite.Section> sections) throws GenerateException {
        try {
            var logo = bereiteTurnierlogoVor(zielVerzeichnis, logoUrl);
            logo.kopierteDatei().ifPresent(datei -> processBox().info(datei.toString()));
            String html = ExportHtmlSeite.from(getWorkingSpreadsheet())
                    .titel(titel)
                    .logoUrl(logo.logoUrl())
                    .sections(nurVorhandeneSections(sections))
                    .erstelle();
            Path htmlDatei = htmlZieldatei(zielVerzeichnis, fallbackDateiname);
            Files.writeString(htmlDatei, html, StandardCharsets.UTF_8);
            processBox().info(htmlDatei.toString());
            return new HtmlExportErgebnis(htmlDatei, logo.kopierteDatei());
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new GenerateException(e.getMessage());
        }
    }

    /**
     * Filtert Abschnitte, deren Sheet nicht vorhanden ist. Für jedes fehlende Sheet wird ein Hinweis
     * in die ProcessBox und eine Warnung ins Log geschrieben; der Abschnitt entfällt komplett im HTML.
     */
    private List<ExportHtmlSeite.Section> nurVorhandeneSections(List<ExportHtmlSeite.Section> sections)
            throws GenerateException {
        var vorhandene = new ArrayList<ExportHtmlSeite.Section>();
        for (var section : sections) {
            if (getSheetHelper().findByName(section.sheetName()) == null) {
                processBox().info(I18n.get("error.tabelle.nicht.vorhanden", section.sheetName()));
                logger.warn("HTML-Export: Sheet '{}' übersprungen, Tabelle nicht vorhanden", section.sheetName());
                continue;
            }
            vorhandene.add(section);
        }
        return vorhandene;
    }

    protected record HtmlExportErgebnis(Path htmlDatei, Optional<Path> logoDatei) {
        public void addTo(List<Path> exportierteDateien) {
            exportierteDateien.add(htmlDatei);
            logoDatei.ifPresent(exportierteDateien::add);
        }
    }

    record TurnierlogoExport(String logoUrl, Optional<Path> kopierteDatei) {
    }

    static TurnierlogoExport bereiteTurnierlogoVor(Path zielVerzeichnis, String logoUrl)
            throws IOException, GenerateException {
        String quelle = StringUtils.stripToEmpty(logoUrl);
        if (StringUtils.isBlank(quelle) || istUnveraenderteLogoQuelle(quelle)) {
            return new TurnierlogoExport(quelle, Optional.empty());
        }

        Path logoDatei = lokalerLogoPfad(quelle);
        if (!Files.isRegularFile(logoDatei) || !Files.isReadable(logoDatei)) {
            throw new GenerateException(I18n.get("export.fehler.turnierlogo.nicht.lesbar", logoDatei));
        }

        Path zielDatei = zielVerzeichnis.resolve("turnier-logo" + logoEndung(logoDatei));
        Files.copy(logoDatei, zielDatei, StandardCopyOption.REPLACE_EXISTING);
        return new TurnierlogoExport(zielDatei.getFileName().toString(), Optional.of(zielDatei));
    }

    private static boolean istUnveraenderteLogoQuelle(String quelle) {
        String lower = quelle.toLowerCase(Locale.ROOT);
        return lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("data:");
    }

    private static Path lokalerLogoPfad(String quelle) throws GenerateException {
        try {
            URI uri = URI.create(quelle);
            if ("file".equalsIgnoreCase(uri.getScheme())) {
                return Path.of(uri);
            }
        } catch (IllegalArgumentException e) {
            // Kein URI, sondern normaler lokaler Pfad.
        }
        return Path.of(quelle);
    }

    private static String logoEndung(Path logoDatei) {
        String dateiname = logoDatei.getFileName() != null ? logoDatei.getFileName().toString() : "";
        String extension = FilenameUtils.getExtension(dateiname);
        return StringUtils.isBlank(extension) ? "" : "." + extension;
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
    protected HtmlExportErgebnis exportiereHtmlMitMeldelisteDruckbereich(
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
