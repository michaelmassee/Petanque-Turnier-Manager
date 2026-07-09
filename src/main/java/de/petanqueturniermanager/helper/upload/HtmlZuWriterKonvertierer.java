/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.upload;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.frame.XComponentLoader;
import com.sun.star.frame.XStorable;
import com.sun.star.lang.XComponent;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.comp.OfficeDocumentHelper;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.PropertyValueHelper;

/**
 * Konvertiert einen HTML-String nach DOCX oder ODT, indem LibreOffice selbst
 * das HTML in ein verstecktes Writer-Dokument lädt und mit dem passenden
 * Filter wieder speichert. Keine externe Bibliothek nötig.
 */
public final class HtmlZuWriterKonvertierer {

    private static final Logger logger = LogManager.getLogger(HtmlZuWriterKonvertierer.class);

    static final String FILTER_DOCX = "MS Word 2007 XML";
    static final String FILTER_ODT = "writer8";

    private HtmlZuWriterKonvertierer() {
    }

    public static Path konvertiereNachDocx(XComponentContext xContext, String htmlDokument, Path zielDatei)
            throws GenerateException {
        return konvertiere(xContext, htmlDokument, zielDatei, FILTER_DOCX);
    }

    public static Path konvertiereNachOdt(XComponentContext xContext, String htmlDokument, Path zielDatei)
            throws GenerateException {
        return konvertiere(xContext, htmlDokument, zielDatei, FILTER_ODT);
    }

    private static Path konvertiere(XComponentContext xContext, String htmlDokument, Path zielDatei,
            String filterName) throws GenerateException {
        logger.info("Erstelle {} aus HTML: {}", filterName, zielDatei);
        Path tempHtml;
        try {
            tempHtml = Files.createTempFile("ptm-export-", ".html");
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new GenerateException(e.getMessage());
        }
        XComponent doc = null;
        try {
            Files.writeString(tempHtml, htmlDokument, StandardCharsets.UTF_8);
            XComponentLoader loader = erzeugeLoader(xContext);
            String htmlUrl = tempHtml.toUri().toURL().toExternalForm();
            var ladeProps = PropertyValueHelper.from().add("Hidden", true).propList();
            doc = loader.loadComponentFromURL(htmlUrl, "_blank", 0, ladeProps);
            if (doc == null) {
                throw new GenerateException("HTML konnte nicht geladen werden: " + tempHtml);
            }
            XStorable storable = Lo.qi(XStorable.class, doc);
            if (storable == null) {
                throw new GenerateException("XStorable nicht verfügbar für konvertiertes Dokument");
            }
            String zielUrl = zielDatei.toUri().toURL().toExternalForm();
            var speicherProps = PropertyValueHelper.from().add("FilterName", filterName).propList();
            storable.storeToURL(zielUrl, speicherProps);
            return zielDatei;
        } catch (com.sun.star.uno.Exception | IOException e) {
            logger.error(e.getMessage(), e);
            throw new GenerateException(e.getMessage());
        } finally {
            if (doc != null) {
                OfficeDocumentHelper.closeDoc(doc);
            }
            loeschTempDatei(tempHtml);
        }
    }

    private static XComponentLoader erzeugeLoader(XComponentContext xContext) throws com.sun.star.uno.Exception {
        Object desktop = xContext.getServiceManager().createInstanceWithContext(
                "com.sun.star.frame.Desktop", xContext);
        return Lo.qi(XComponentLoader.class, desktop);
    }

    private static void loeschTempDatei(Path tempHtml) {
        try {
            Files.deleteIfExists(tempHtml);
        } catch (IOException e) {
            logger.warn("Temporäre HTML-Datei konnte nicht gelöscht werden: {}", tempHtml, e);
        }
    }
}
