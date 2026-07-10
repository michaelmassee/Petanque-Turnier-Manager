/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.upload;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FSFontUseCase;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import de.petanqueturniermanager.exception.GenerateException;

/**
 * Konvertiert einen HTML-String in eine PDF-Datei via OpenHTMLToPDF (Apache PDFBox).
 */
public class HtmlZuPdfKonvertierer {

    private static final Logger logger = LogManager.getLogger(HtmlZuPdfKonvertierer.class);
    static final String PDF_FONT_FAMILY = "PTMPdfSans";
    private static final List<FontKandidat> FONT_KANDIDATEN = List.of(
            fallback("C:/Windows/Fonts/arial.ttf", 400),
            fallback("C:/Windows/Fonts/arialbd.ttf", 700),
            fallback("C:/Windows/Fonts/arialuni.ttf", 400),
            fallback("C:/Windows/Fonts/segoeui.ttf", 400),
            fallback("C:/Windows/Fonts/seguisym.ttf", 400),
            fallback("C:/Windows/Fonts/Nirmala.ttf", 400),
            dokument("C:/Windows/Fonts/meiryo.ttc", 400),
            dokument("C:/Windows/Fonts/msgothic.ttc", 400),
            dokument("C:/Windows/Fonts/msyh.ttc", 400),
            dokument("C:/Windows/Fonts/simsun.ttc", 400),
            fallback("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 400),
            fallback("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 700),
            fallback("/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf", 400),
            fallback("/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf", 700),
            fallback("/usr/share/fonts/truetype/freefont/FreeSans.ttf", 400),
            fallback("/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf", 400),
            fallback("/usr/share/fonts/truetype/noto/NotoSansSymbols-Regular.ttf", 400),
            fallback("/usr/share/fonts/truetype/noto/NotoSansSymbols2-Regular.ttf", 400),
            dokument("/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc", 400),
            dokument("/usr/share/fonts/opentype/noto/NotoSerifCJK-Regular.ttc", 400),
            fallback("/Library/Fonts/Arial Unicode.ttf", 400),
            fallback("/System/Library/Fonts/Supplemental/Arial.ttf", 400),
            fallback("/System/Library/Fonts/Supplemental/Arial Bold.ttf", 700),
            fallback("/System/Library/Fonts/Supplemental/Arial Unicode.ttf", 400),
            fallback("/System/Library/Fonts/Supplemental/Apple Symbols.ttf", 400),
            fallback("/System/Library/Fonts/Supplemental/NotoSansArmenian-Regular.ttf", 400),
            fallback("/System/Library/Fonts/Supplemental/NotoSansBengali-Regular.ttf", 400),
            fallback("/System/Library/Fonts/Supplemental/NotoSansDevanagari-Regular.ttf", 400),
            fallback("/System/Library/Fonts/Supplemental/NotoSansGeorgian-Regular.ttf", 400),
            fallback("/System/Library/Fonts/Supplemental/NotoSansGujarati-Regular.ttf", 400),
            fallback("/System/Library/Fonts/Supplemental/NotoSansGurmukhi-Regular.ttf", 400),
            fallback("/System/Library/Fonts/Supplemental/NotoSansHebrew-Regular.ttf", 400),
            fallback("/System/Library/Fonts/Supplemental/NotoSansKannada-Regular.ttf", 400),
            fallback("/System/Library/Fonts/Supplemental/NotoSansKhmer-Regular.ttf", 400),
            fallback("/System/Library/Fonts/Supplemental/NotoSansLao-Regular.ttf", 400),
            fallback("/System/Library/Fonts/Supplemental/NotoSansMyanmar-Regular.ttf", 400),
            fallback("/System/Library/Fonts/Supplemental/NotoSansOriya-Regular.ttf", 400),
            fallback("/System/Library/Fonts/Supplemental/NotoSansSinhala-Regular.ttf", 400),
            fallback("/System/Library/Fonts/Supplemental/NotoSansTamil-Regular.ttf", 400),
            fallback("/System/Library/Fonts/Supplemental/NotoSansTelugu-Regular.ttf", 400),
            fallback("/System/Library/Fonts/Supplemental/NotoSansThai-Regular.ttf", 400),
            fallback("/System/Library/Fonts/Supplemental/NotoSansTibetan-Regular.ttf", 400),
            dokument("/System/Library/Fonts/Supplemental/PingFang.ttc", 400),
            dokument("/System/Library/Fonts/Supplemental/Hiragino Sans GB.ttc", 400),
            dokument("/System/Library/Fonts/ヒラギノ角ゴシック W3.ttc", 400));

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
            registriereUnicodeFonts(builder);
            builder.toStream(os);
            builder.run();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new GenerateException(e.getMessage());
        }
        return zielDatei;
    }

    private static void registriereUnicodeFonts(PdfRendererBuilder builder) {
        for (FontKandidat kandidat : FONT_KANDIDATEN) {
            File font = new File(kandidat.pfad());
            if (font.isFile()) {
                builder.useFont(font, PDF_FONT_FAMILY, kandidat.gewicht(), FontStyle.NORMAL, true);
                if (kandidat.finalerFallback()) {
                    builder.useFont(font, PDF_FONT_FAMILY + "Fallback", kandidat.gewicht(), FontStyle.NORMAL, true,
                            EnumSet.of(FSFontUseCase.FALLBACK_FINAL));
                }
                logger.debug("PDF-Font registriert: {}", font);
            }
        }
    }

    private static FontKandidat fallback(String pfad, int gewicht) {
        return new FontKandidat(pfad, gewicht, true);
    }

    private static FontKandidat dokument(String pfad, int gewicht) {
        return new FontKandidat(pfad, gewicht, false);
    }

    private record FontKandidat(String pfad, int gewicht, boolean finalerFallback) {
    }
}
