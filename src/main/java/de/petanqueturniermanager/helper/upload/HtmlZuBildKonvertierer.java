/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.upload;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.openhtmltopdf.java2d.api.BufferedImagePageProcessor;
import com.openhtmltopdf.java2d.api.Java2DRendererBuilder;

import de.petanqueturniermanager.exception.GenerateException;

/**
 * Konvertiert einen HTML-String direkt zu PNG-Bild-Bytes via OpenHTMLToPDF/Java2D – ohne
 * PDF-Zwischenschritt.
 * <p>
 * Die Java2D-Engine im Single-Page-Modus passt im Gegensatz zur Höhe die Breite NICHT automatisch
 * an den Inhalt an, sondern rendert exakt auf die im HTML per CSS-{@code @page}-Regel
 * angeforderte Breite. Damit der Aufrufer den Inhalt trotzdem nicht selbst vermessen/berechnen
 * muss, wird hier bewusst grosszügig überbreit gerendert ({@link #GROSSZUEGIGE_SEITENBREITE_PX})
 * und das Ergebnis anschliessend 1:1 auf die tatsächliche Inhaltsgrösse zugeschnitten
 * ({@link #aufInhaltZuschneiden}) – das Bild wird also unabhängig von jeder Breiten-/
 * Höhenschätzung immer exakt so groß wie der gerenderte Inhalt.
 */
public class HtmlZuBildKonvertierer {

    private static final Logger logger = LogManager.getLogger(HtmlZuBildKonvertierer.class);

    /** Skalierungsfaktor gegenüber CSS-px (~96dpi); grob vergleichbar zur bisherigen 150-DPI-Rasterung. */
    private static final double BILD_SKALIERUNG = 1.75;

    /**
     * Großzügige @page-Breite in CSS-px, weit über jedem realistischen Druckbereich (z.B. A0
     * hochkant sind ~3179 CSS-px) – wird nach dem Rendern wieder auf den Inhalt zugeschnitten,
     * die genaue Größe spielt daher keine Rolle, solange sie ausreicht. Nur die Breite braucht
     * diesen Puffer: die Java2D-Engine im Single-Page-Modus bestimmt die Rasterhöhe bereits aus
     * dem tatsächlichen Inhalt (siehe Klassen-Doku), unabhängig von diesem Wert.
     */
    private static final int GROSSZUEGIGE_SEITENBREITE_PX = 3500;

    private HtmlZuBildKonvertierer() {
    }

    /**
     * @param htmlFragment HTML-Inhalt (z.B. {@code <table>}-Fragment), wird in ein minimales
     *                      Dokument mit großzügiger Seitenbreite eingebettet.
     */
    public static byte[] konvertiere(String htmlFragment, String baseUri) throws GenerateException {
        String htmlDokument = htmlDokument(htmlFragment);
        var builder = new Java2DRendererBuilder();
        builder.useFastMode();
        builder.useEnvironmentFonts(true);
        builder.withHtmlContent(htmlDokument, baseUri);
        HtmlZuPdfKonvertierer.registriereUnicodeFonts(builder);
        var processor = new BufferedImagePageProcessor(BufferedImage.TYPE_INT_RGB, BILD_SKALIERUNG);
        builder.toSinglePage(processor);
        try {
            builder.runFirstPage();
        } catch (IOException e) {
            logger.error("HTML konnte nicht zu Bild gerendert werden: {}", e.getMessage(), e);
            throw new GenerateException(e.getMessage());
        }
        var seiten = processor.getPageImages();
        if (seiten.isEmpty()) {
            throw new GenerateException("HTML-Rendering lieferte keine Seite");
        }
        BufferedImage bild = aufInhaltZuschneiden(seiten.get(0));
        try (var os = new ByteArrayOutputStream()) {
            ImageIO.write(bild, "png", os);
            return os.toByteArray();
        } catch (IOException e) {
            logger.error("Gerastertes Bild konnte nicht als PNG kodiert werden: {}", e.getMessage(), e);
            throw new GenerateException(e.getMessage());
        }
    }

    private static String htmlDokument(String htmlFragment) {
        return """
                <html>
                <head>
                <meta charset="UTF-8"/>
                <style>
                @page { size: %dpx %dpx; margin: 0; }
                body { margin: 0; font-family: PTMPdfSans, Arial, Helvetica, sans-serif; font-size: 9pt; }
                table { border-collapse: collapse; }
                td, th { padding: 2px 4px; }
                </style>
                </head>
                <body>
                %s
                </body>
                </html>
                """.formatted(GROSSZUEGIGE_SEITENBREITE_PX, GROSSZUEGIGE_SEITENBREITE_PX, htmlFragment);
    }

    /**
     * Schneidet das Bild links-oben-verankert auf die tatsächliche Inhaltsgröße zu (letzte
     * Spalte/Zeile mit einem nicht-weißen Pixel) – reduziert die großzügig überbreite/-hohe Seite
     * auf den echten Inhalt, egal wie groß dieser tatsächlich ist.
     * <p>
     * Die Pixel werden einmalig per Bulk-{@code getRGB} in ein Array gelesen statt pro Pixel
     * einzeln über {@link BufferedImage#getRGB(int, int)} – bei nahezu leeren/sehr großen Bildern
     * (Worst-Case-Scan über die gesamte Fläche) vermeidet das Millionen einzelner Methodenaufrufe.
     */
    private static BufferedImage aufInhaltZuschneiden(BufferedImage bild) {
        int width = bild.getWidth();
        int height = bild.getHeight();
        int[] pixel = bild.getRGB(0, 0, width, height, null, 0, width);
        int breite = inhaltsbreite(pixel, width, height);
        int hoehe = inhaltshoehe(pixel, width, height, breite);
        if (breite <= 0 || hoehe <= 0 || (breite >= width && hoehe >= height)) {
            return bild;
        }
        return bild.getSubimage(0, 0, Math.min(breite, width), Math.min(hoehe, height));
    }

    /** Letzte (von rechts gesehene) Spalte mit mindestens einem nicht-weißen Pixel, oder 0. */
    private static int inhaltsbreite(int[] pixel, int width, int height) {
        for (int x = width - 1; x >= 0; x--) {
            for (int y = 0; y < height; y++) {
                if (istNichtWeiss(pixel[y * width + x])) {
                    return x + 1;
                }
            }
        }
        return 0;
    }

    /** Letzte (von unten gesehene) Zeile mit mindestens einem nicht-weißen Pixel, oder 0. */
    private static int inhaltshoehe(int[] pixel, int width, int height, int breite) {
        int maxX = Math.min(breite, width);
        for (int y = height - 1; y >= 0; y--) {
            int zeilenOffset = y * width;
            for (int x = 0; x < maxX; x++) {
                if (istNichtWeiss(pixel[zeilenOffset + x])) {
                    return y + 1;
                }
            }
        }
        return 0;
    }

    private static boolean istNichtWeiss(int rgb) {
        return (rgb & 0xFFFFFF) != 0xFFFFFF;
    }
}
