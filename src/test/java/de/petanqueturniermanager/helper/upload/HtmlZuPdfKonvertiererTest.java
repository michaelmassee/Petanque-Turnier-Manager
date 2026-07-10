/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.upload;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HtmlZuPdfKonvertiererTest {

    @TempDir
    Path tempDir;

    @Test
    void konvertiertPdfKompatibleRotation() throws Exception {
        String tabelle = """
                <table><tbody><tr>
                <td style="border:1px solid #000000;">
                <span style="display:inline-block;white-space:nowrap;transform:rotate(90deg);transform-origin:center center;">Kopf</span>
                </td>
                </tr></tbody></table>
                """;
        Path pdf = HtmlZuPdfKonvertierer.konvertiere(
                PdfHtmlDokument.erstelle("Rotation", tabelle),
                tempDir.resolve("rotation.pdf"));

        assertThat(pdf).isRegularFile();
        assertThat(Files.readString(pdf, java.nio.charset.StandardCharsets.ISO_8859_1))
                .startsWith("%PDF-");
    }

    @Test
    void pdfExportErhaeltUmlauteUndSonderzeichen() throws Exception {
        String tabelle = """
                <table><tbody><tr>
                <td>Müller ÄÖÜ äöü ß Pétanque € ✓ Привет Ελληνικά</td>
                </tr></tbody></table>
                """;
        Path pdf = HtmlZuPdfKonvertierer.konvertiere(
                PdfHtmlDokument.erstelle("Umlaute", tabelle),
                tempDir.resolve("umlaute.pdf"));

        try (var dokument = PDDocument.load(pdf.toFile())) {
            String text = new PDFTextStripper().getText(dokument);
            assertThat(text).contains("Müller ÄÖÜ äöü ß Pétanque € ✓ Привет Ελληνικά");
        }
    }
}
