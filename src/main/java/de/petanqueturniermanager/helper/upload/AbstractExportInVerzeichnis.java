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
import com.sun.star.sheet.XSpreadsheetDocument;
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
import de.petanqueturniermanager.helper.sheetsync.EingabeSignatur;
import de.petanqueturniermanager.helper.sheetsync.SheetSyncSignaturStore;
import de.petanqueturniermanager.helper.sheetsync.SignaturErgebnis;
import de.petanqueturniermanager.webserver.TabelleHtmlRenderer;
import de.petanqueturniermanager.webserver.TabelleMarkdownRenderer;
import de.petanqueturniermanager.webserver.TabelleModel;
import de.petanqueturniermanager.webserver.TabellenMapper;

public abstract class AbstractExportInVerzeichnis extends SheetRunner {

    private static final Logger logger = LogManager.getLogger(AbstractExportInVerzeichnis.class);
    private static final String MELDELISTE_SECTION_ID = "meldeliste";

    private final Path zielVerzeichnis;
    private final ExportFormat format;
    private final TabellenMapper tabellenMapper = new TabellenMapper();
    private final TabelleHtmlRenderer pdfTabelleHtmlRenderer = TabelleHtmlRenderer.fuerPdf();
    private final TabelleMarkdownRenderer markdownTabelleRenderer = new TabelleMarkdownRenderer();

    protected AbstractExportInVerzeichnis(WorkingSpreadsheet ws, TurnierSystem ts, String name,
            Path zielVerzeichnis, ExportFormat format) {
        super(ws, ts, name);
        this.zielVerzeichnis = zielVerzeichnis;
        this.format = format;
    }

    protected final ExportFormat getFormat() {
        return format;
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

    @FunctionalInterface
    protected interface ExportUpdateAktion {
        void aktualisieren() throws GenerateException;
    }

    protected void aktualisiereExportSheetWennDirty(String persistenzSchluessel,
            EingabeSignatur signatur, ExportUpdateAktion updateAktion) throws GenerateException {
        aktualisiereExportSheetWennDirty(persistenzSchluessel, signatur,
                exportSheetFehlt(persistenzSchluessel), updateAktion);
    }

    protected void aktualisiereExportSheetWennDirty(String persistenzSchluessel,
            EingabeSignatur signatur, boolean ausgabeFehlt, ExportUpdateAktion updateAktion)
            throws GenerateException {
        var doc = getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
        SignaturErgebnis ergebnis = signatur.berechne(doc, 1);

        if (ergebnis instanceof SignaturErgebnis.Ok ok) {
            var gespeichert = SheetSyncSignaturStore.ladeHash(doc, persistenzSchluessel);
            if (!ausgabeFehlt && gespeichert.isPresent() && gespeichert.get().equals(ok.hash())) {
                SheetSyncSignaturStore.aktualisiereVerifyZeit(doc, persistenzSchluessel);
                logger.debug("Export-Update übersprungen, Signatur unverändert (key={})", persistenzSchluessel);
                return;
            }
            updateAktion.aktualisieren();
            String grund = ausgabeFehlt ? "exportMissingOutput" : "exportBeforeBuild";
            SheetSyncSignaturStore.speichereNachRebuild(doc, persistenzSchluessel, ok.hash(), grund);
            return;
        }

        if (ausgabeFehlt) {
            updateAktion.aktualisieren();
            return;
        }

        logger.warn("Export-Update übersprungen, Signatur konnte nicht berechnet werden (key={}): {}",
                persistenzSchluessel, ergebnis);
    }

    protected boolean exportSheetFehlt(String schluessel) {
        return SheetMetadataHelper.findeSheet(getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), schluessel)
                .isEmpty();
    }

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
        Path zielDateiname = zielDatei.getFileName();
        return new TurnierlogoExport(zielDateiname != null ? zielDateiname.toString() : "", Optional.of(zielDatei));
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
        Path dateinamePfad = logoDatei.getFileName();
        String dateiname = dateinamePfad != null ? dateinamePfad.toString() : "";
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

        if (!meldelisteExportieren) {
            return exportiereHtml(zielVerzeichnis, fallbackDateiname, titel, logoUrl, sections);
        }
        return mitAngepasstemMeldelisteDruckbereich(meldelisteSheetName,
                () -> exportiereHtml(zielVerzeichnis, fallbackDateiname, titel, logoUrl, sections));
    }

    /**
     * Führt {@code aktion} mit temporär auf den Datenbereich (ohne Überschrift, ohne leere
     * Endzeilen) verengtem Druckbereich der Meldeliste aus. Greift bei fehlendem Sheet,
     * fehlenden {@link XPrintAreas} oder leerem Druckbereich unverändert durch. Der originale
     * Druckbereich wird nach {@code aktion} in jedem Fall wiederhergestellt.
     */
    private <T> T mitAngepasstemMeldelisteDruckbereich(String meldelisteSheetName, ExportAktion<T> aktion)
            throws GenerateException {
        if (StringUtils.isBlank(meldelisteSheetName)) {
            return aktion.ausfuehren();
        }
        var sheet = getSheetHelper().findByName(meldelisteSheetName);
        if (sheet == null) {
            return aktion.ausfuehren();
        }
        var printAreas = Lo.qi(XPrintAreas.class, sheet);
        if (printAreas == null) {
            return aktion.ausfuehren();
        }
        var originalBereiche = printAreas.getPrintAreas();
        if (originalBereiche == null || originalBereiche.length == 0) {
            return aktion.ausfuehren();
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
            return aktion.ausfuehren();
        } finally {
            printAreas.setPrintAreas(originalBereiche);
        }
    }

    @FunctionalInterface
    private interface ExportAktion<T> {
        T ausfuehren() throws GenerateException;
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

    /**
     * Exportiert alle vorhandenen Sections als ein einziges kombiniertes Dokument
     * im gewünschten Format (PDF, DOCX, ODT oder Markdown), inklusive Turnierlogo
     * (sofern vorhanden). {@code format} darf nicht {@link ExportFormat#HTML_UND_PDFS} sein.
     */
    protected Path exportiereEinDokument(Path zielVerzeichnis, String fallbackBasisName, String titel,
            String logoUrl, ExportFormat format, List<ExportHtmlSeite.Section> sections) throws GenerateException {
        var vorhandeneSections = nurVorhandeneSections(sections);
        if (vorhandeneSections.isEmpty()) {
            return null;
        }
        Path zieldatei = einDokumentZieldatei(zielVerzeichnis, fallbackBasisName, format.dateiEndung());
        Path ergebnis = switch (format) {
            case EIN_DOKUMENT_PDF -> HtmlZuPdfKonvertierer.konvertiere(
                    baueEinDokumentHtml(zielVerzeichnis, titel, logoUrl, vorhandeneSections), zieldatei);
            case EIN_DOKUMENT_DOCX -> HtmlZuWriterKonvertierer.konvertiereNachDocx(getWorkingSpreadsheet().getxContext(),
                    baueEinDokumentHtml(zielVerzeichnis, titel, logoUrl, vorhandeneSections), zieldatei);
            case EIN_DOKUMENT_ODT -> HtmlZuWriterKonvertierer.konvertiereNachOdt(getWorkingSpreadsheet().getxContext(),
                    baueEinDokumentHtml(zielVerzeichnis, titel, logoUrl, vorhandeneSections), zieldatei);
            case EIN_DOKUMENT_MD -> exportiereEinDokumentMarkdown(titel, vorhandeneSections, zieldatei);
            case HTML_UND_PDFS -> throw new GenerateException(
                    "exportiereEinDokument darf nicht mit HTML_UND_PDFS aufgerufen werden");
        };
        processBox().info(ergebnis.toString());
        return ergebnis;
    }

    /**
     * Baut das gemeinsame Mehrfach-Abschnitt-HTML für PDF/DOCX/ODT (identische Quelle,
     * nur die nachgelagerte Konvertierung unterscheidet sich).
     */
    private String baueEinDokumentHtml(Path zielVerzeichnis, String titel, String logoUrl,
            List<ExportHtmlSeite.Section> sections) throws GenerateException {
        String logoUrlFuerDokument = logoUrlFuerEinDokument(zielVerzeichnis, logoUrl);
        return PdfHtmlDokument.erstelle(titel, logoUrlFuerDokument, abschnittTitel(sections), tabellenFragmente(sections));
    }

    /**
     * Bereitet die Logo-URL für ein kombiniertes Dokument vor: lokale Dateien werden
     * (wie beim HTML-Export) ins Zielverzeichnis kopiert, aber als absolute {@code file:}-URI
     * zurückgegeben, da das Dokument (insb. bei DOCX/ODT) nicht zwingend im Zielverzeichnis liegt.
     */
    private String logoUrlFuerEinDokument(Path zielVerzeichnis, String logoUrl) throws GenerateException {
        if (StringUtils.isBlank(logoUrl)) {
            return null;
        }
        try {
            var logo = bereiteTurnierlogoVor(zielVerzeichnis, logoUrl);
            if (logo.kopierteDatei().isPresent()) {
                processBox().info(logo.kopierteDatei().get().toString());
                return logo.kopierteDatei().get().toUri().toString();
            }
            return logo.logoUrl();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new GenerateException(e.getMessage());
        }
    }

    private Path exportiereEinDokumentMarkdown(String titel, List<ExportHtmlSeite.Section> sections, Path zieldatei)
            throws GenerateException {
        var doc = getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
        var sb = new StringBuilder(4096);
        sb.append("# ").append(titel).append("\n\n");
        for (var section : sections) {
            var model = mappeTabelle(section, doc);
            sb.append("## ").append(section.titel()).append("\n\n");
            sb.append(markdownTabelleRenderer.render(model)).append("\n");
        }
        sb.append(ExportFooterHtml.markdownZeitstempel()).append("\n");
        try {
            Files.writeString(zieldatei, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new GenerateException(e.getMessage());
        }
        return zieldatei;
    }

    private List<String> abschnittTitel(List<ExportHtmlSeite.Section> sections) {
        return sections.stream().map(ExportHtmlSeite.Section::titel).toList();
    }

    private List<String> tabellenFragmente(List<ExportHtmlSeite.Section> sections) throws GenerateException {
        var doc = getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
        var fragmente = new ArrayList<String>();
        for (var section : sections) {
            fragmente.add(pdfTabelleHtmlRenderer.render(mappeTabelle(section, doc)));
        }
        return fragmente;
    }

    private TabelleModel mappeTabelle(ExportHtmlSeite.Section section, XSpreadsheetDocument doc)
            throws GenerateException {
        if (MELDELISTE_SECTION_ID.equals(section.id())) {
            return mitAngepasstemMeldelisteDruckbereich(section.sheetName(),
                    () -> tabellenMapper.map(getSheetHelper().findByName(section.sheetName()), doc));
        }
        return tabellenMapper.map(getSheetHelper().findByName(section.sheetName()), doc);
    }

    /**
     * Baut die Standard-Sections für Turniersysteme mit optionaler Meldeliste und
     * genau einer Rangliste-Section (Schweizer, FormuleX, Poule).
     */
    protected static List<ExportHtmlSeite.Section> sectionsMitOptionalerMeldelisteUndRangliste(
            String meldelisteSheetName, boolean meldelisteExportieren,
            String ranglisteTitel, String ranglisteSheetName, String ranglistePdfUrl) {
        List<ExportHtmlSeite.Section> sections = new ArrayList<>();
        if (meldelisteExportieren) {
            sections.add(new ExportHtmlSeite.Section(MELDELISTE_SECTION_ID, I18n.get("export.nav.meldeliste"), meldelisteSheetName, null));
        }
        sections.add(new ExportHtmlSeite.Section("rangliste", ranglisteTitel, ranglisteSheetName, ranglistePdfUrl));
        return sections;
    }

    protected Path einDokumentZieldatei(Path verzeichnis, String fallbackBasisName, String endung)
            throws GenerateException {
        var xStorable = getWorkingSpreadsheet().getXStorable();
        String location = xStorable != null ? xStorable.getLocation() : null;
        return einDokumentZieldatei(verzeichnis, fallbackBasisName, endung, location);
    }

    static Path einDokumentZieldatei(Path verzeichnis, String fallbackBasisName, String endung, String location)
            throws GenerateException {
        String fallback = fallbackBasisName + "." + endung;
        if (StringUtils.isBlank(location)) {
            return verzeichnis.resolve(fallback);
        }
        try {
            Path dateiname = Path.of(URI.create(location).toURL().toURI()).getFileName();
            if (dateiname == null) {
                return verzeichnis.resolve(fallback);
            }
            String basisName = FilenameUtils.removeExtension(dateiname.toString());
            return verzeichnis.resolve(basisName + "." + endung);
        } catch (IllegalArgumentException | MalformedURLException | URISyntaxException e) {
            throw new GenerateException(e.getMessage());
        }
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
