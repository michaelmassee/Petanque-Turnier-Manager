/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.upload;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

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
}
