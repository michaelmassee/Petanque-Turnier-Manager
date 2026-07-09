/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.upload;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.apache.commons.text.StringEscapeUtils;
import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.helper.i18n.I18n;

class PdfHtmlDokumentTest {

    @Test
    void enthaeltGemeinsamenFooterMitTextLinkUndLogo() {
        var html = PdfHtmlDokument.erstelle("Rangliste", "<table><tbody><tr><td>1</td></tr></tbody></table>");

        assertThat(html)
                .contains(StringEscapeUtils.escapeHtml4(I18n.get("export.liga.footer.text")))
                .contains("href=\"https://michaelmassee.github.io/Petanque-Turnier-Manager/\"")
                .contains("<img class=\"ptm-footer-logo\" src=\"data:image/png;base64,")
                .containsPattern("Erstellt am \\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2}");
    }

    @Test
    void footerLogoIstXhtmlKompatibel() {
        var html = PdfHtmlDokument.erstelle("Rangliste", "<table><tbody><tr><td>1</td></tr></tbody></table>");

        assertThat(html)
                .contains("alt=\"Pétanque Turnier Manager\" />")
                .doesNotContain("target=\"_blank\"");
    }

    @Test
    void mehrfachAbschnitt_ErstesAbschnittOhnePageBreak_WeitereMitPageBreak() {
        var html = PdfHtmlDokument.erstelle("Turnier",
                List.of("Spielplan", "Rangliste"),
                List.of("<table><tbody><tr><td>1</td></tr></tbody></table>",
                        "<table><tbody><tr><td>2</td></tr></tbody></table>"));

        assertThat(html)
                .contains("<h2>Spielplan</h2>")
                .contains("<h2>Rangliste</h2>")
                .contains("page-break-before: always");

        int ersterAbschnitt = html.indexOf("<section>");
        int zweiterAbschnitt = html.indexOf("<section style=\"page-break-before: always;\">");
        assertThat(ersterAbschnitt).isGreaterThanOrEqualTo(0);
        assertThat(zweiterAbschnitt).isGreaterThan(ersterAbschnitt);
    }

    @Test
    void mehrfachAbschnitt_EscaptTitel() {
        var html = PdfHtmlDokument.erstelle("<script>", List.of("<b>x</b>"), List.of("<table></table>"));
        assertThat(html).doesNotContain("<script>").contains("&lt;script&gt;").contains("&lt;b&gt;x&lt;/b&gt;");
    }
}
