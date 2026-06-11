/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.upload;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import de.petanqueturniermanager.exception.GenerateException;

/**
 * Konvertiert einen HTML-String in eine PDF-Datei via OpenHTMLToPDF (Apache PDFBox).
 */
public class HtmlZuPdfKonvertierer {

    private static final Logger logger = LogManager.getLogger(HtmlZuPdfKonvertierer.class);

    private HtmlZuPdfKonvertierer() {
    }

    public static Path konvertiere(String htmlDokument, Path zielDatei) throws GenerateException {
        logger.info("Erstelle PDF aus HTML: {}", zielDatei);
        try (OutputStream os = Files.newOutputStream(zielDatei)) {
            var builder = new PdfRendererBuilder();
            builder.useFastMode();
            Path elternPfad = zielDatei.getParent();
            String baseUri = elternPfad != null ? elternPfad.toUri().toString() : zielDatei.toUri().toString();
            builder.withHtmlContent(htmlDokument, baseUri);
            builder.toStream(os);
            builder.run();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new GenerateException(e.getMessage());
        }
        return zielDatei;
    }
}
