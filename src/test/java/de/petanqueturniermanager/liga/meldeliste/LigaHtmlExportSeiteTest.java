/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.liga.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LigaHtmlExportSeiteTest {

    private static final String SPIELPLAN_HTML = "<table><tbody><tr><td>KW</td></tr></tbody></table>";
    private static final String RANGLISTE_HTML = "<table><tbody><tr><td>Platz</td></tr></tbody></table>";
    private static final String DIREKTVERGLEICH_HTML = "<table><tbody><tr><td>DV</td></tr></tbody></table>";

    @Test
    void dreiSectionIdsVorhandenKeineMeldeliste() {
        var html = seite().erstelleAusRendertHtml(SPIELPLAN_HTML, RANGLISTE_HTML, DIREKTVERGLEICH_HTML);
        assertThat(html).doesNotContain("id=\"meldeliste\"");
        assertThat(html).contains("id=\"spielplan\"");
        assertThat(html).contains("id=\"rangliste\"");
        assertThat(html).contains("id=\"direktvergleich\"");
    }

    @Test
    void pdfLinkErscheint_wennSpielplanUrlGesetzt() {
        var html = seite()
                .spielplanPdfUrl("https://example.com/spielplan.pdf")
                .erstelleAusRendertHtml(SPIELPLAN_HTML, RANGLISTE_HTML, DIREKTVERGLEICH_HTML);
        assertThat(html).contains("href=\"https://example.com/spielplan.pdf\"");
        assertThat(html).contains("class=\"pdf-btn\"");
    }

    @Test
    void keinPdfLink_wennUrlFehlt() {
        var html = seite().erstelleAusRendertHtml(SPIELPLAN_HTML, RANGLISTE_HTML, DIREKTVERGLEICH_HTML);
        assertThat(html).doesNotContain("<a class=\"pdf-btn\"");
    }

    @Test
    void pdfLinkErscheint_wennRanglisteUrlGesetzt() {
        var html = seite()
                .ranglistePdfUrl("https://example.com/rangliste.pdf")
                .erstelleAusRendertHtml(SPIELPLAN_HTML, RANGLISTE_HTML, DIREKTVERGLEICH_HTML);
        assertThat(html).contains("href=\"https://example.com/rangliste.pdf\"");
    }

    @Test
    void logoUrlWirdEscaped() {
        var html = seite()
                .logoUrl("https://example.com/logo.png?a=1&b=2")
                .erstelleAusRendertHtml(SPIELPLAN_HTML, RANGLISTE_HTML, DIREKTVERGLEICH_HTML);
        assertThat(html).contains("src=\"https://example.com/logo.png?a=1&amp;b=2\"");
        assertThat(html).doesNotContain("src=\"https://example.com/logo.png?a=1&b=2\"");
    }

    @Test
    void grOuppennameWirdEscaped() {
        var html = seite()
                .gruppenname("<Gruppe A>")
                .erstelleAusRendertHtml(SPIELPLAN_HTML, RANGLISTE_HTML, DIREKTVERGLEICH_HTML);
        assertThat(html).contains("&lt;Gruppe A&gt;");
        assertThat(html).doesNotContain("<Gruppe A>");
    }

    @Test
    void tabellenHtmlWirdEingebettet() {
        var html = seite().erstelleAusRendertHtml(SPIELPLAN_HTML, RANGLISTE_HTML, DIREKTVERGLEICH_HTML);
        assertThat(html).contains(SPIELPLAN_HTML);
        assertThat(html).contains(RANGLISTE_HTML);
        assertThat(html).contains(DIREKTVERGLEICH_HTML);
    }

    @Test
    void navLinksVorhanden() {
        var html = seite().erstelleAusRendertHtml(SPIELPLAN_HTML, RANGLISTE_HTML, DIREKTVERGLEICH_HTML);
        assertThat(html).doesNotContain("href=\"#meldeliste\"");
        assertThat(html).contains("href=\"#spielplan\"");
        assertThat(html).contains("href=\"#rangliste\"");
        assertThat(html).contains("href=\"#direktvergleich\"");
    }

    @Test
    void pdfUrlWirdEscaped() {
        var html = seite()
                .spielplanPdfUrl("https://example.com/datei?name=a&b=c")
                .erstelleAusRendertHtml(SPIELPLAN_HTML, RANGLISTE_HTML, DIREKTVERGLEICH_HTML);
        assertThat(html).contains("href=\"https://example.com/datei?name=a&amp;b=c\"");
    }

    private LigaHtmlExportSeite seite() {
        return LigaHtmlExportSeite.nurFuerTests();
    }
}
