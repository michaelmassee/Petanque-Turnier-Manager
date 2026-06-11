/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.upload;

import static org.assertj.core.api.Assertions.assertThat;

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
                .contains("<img class=\"ptm-footer-logo\" src=\"data:image/png;base64,");
    }

    @Test
    void footerLogoIstXhtmlKompatibel() {
        var html = PdfHtmlDokument.erstelle("Rangliste", "<table><tbody><tr><td>1</td></tr></tbody></table>");

        assertThat(html)
                .contains("alt=\"Pétanque Turnier Manager\" />")
                .doesNotContain("target=\"_blank\"");
    }
}
