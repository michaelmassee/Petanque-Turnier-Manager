/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.liga.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.upload.ExportHtmlSeite;
import de.petanqueturniermanager.liga.spielplan.LigaSpielPlanSheet;

class LigaExportHtmlTest {

    private static final String REFERENZ_DATEI = "/de/petanqueturniermanager/liga/meldeliste/LigaExportHtml_ref.html";

    @BeforeAll
    static void initI18n() {
        I18n.initFuerTest(Locale.GERMAN);
    }

    @Test
    void htmlExport_entsprichtReferenz() throws IOException {
        String html = ExportHtmlSeite.nurFuerTests()
                .titel("Liga Test")
                .logoUrl("https://example.org/logo.png?a=1&b=2")
                .sections(LigaExportInVerzeichnis.htmlSections(
                        SheetNamen.meldeliste(), LigaSpielPlanSheet.sheetName(),
                        "https://download.example/Spielplan.pdf",
                        List.of("Boule Biebertal", "Boule-Freunde Fernwald"),
                        SheetNamen.rangliste(), "https://download.example/Rangliste.pdf",
                        SheetNamen.direktvergleich(), true))
                .erstelleAusRendertHtml(List.of(
                        "<table><tbody><tr><td>Meldeliste</td></tr></tbody></table>",
                        "<table><tbody><tr><td>Spielplan</td></tr></tbody></table>",
                        "<table><tbody><tr><td>Boule Biebertal</td></tr></tbody></table>",
                        "<table><tbody><tr><td>Boule-Freunde Fernwald</td></tr></tbody></table>",
                        "<table><tbody><tr><td>Rangliste</td></tr></tbody></table>",
                        "<table><tbody><tr><td>Direktvergleich</td></tr></tbody></table>"));

        // writeToReferenz(html); // auskommentieren nach Erfassung
        assertThat(normalisiere(html)).isEqualTo(normalisiere(ladeReferenz()));
    }

    @SuppressWarnings("unused")
    private void writeToReferenz(String html) throws IOException {
        Files.writeString(Path.of(System.getProperty("user.home"), "LigaExportHtml_ref.html"),
                normalisiere(html), StandardCharsets.UTF_8);
    }

    private String ladeReferenz() throws IOException {
        try (var stream = LigaExportHtmlTest.class.getResourceAsStream(REFERENZ_DATEI)) {
            assertThat(stream).as("Referenzdatei fehlt: %s", REFERENZ_DATEI).isNotNull();
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String normalisiere(String html) {
        return html.replace("\r\n", "\n")
                .replaceAll("src=\"data:image/png;base64,[^\"]+\"", "src=\"data:image/png;base64,...\"")
                .trim();
    }
}
