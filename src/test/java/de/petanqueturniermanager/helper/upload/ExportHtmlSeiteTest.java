/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.upload;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class ExportHtmlSeiteTest {

    private static final String MELDELISTE_HTML = "<table><tbody><tr><td>Spieler</td></tr></tbody></table>";
    private static final String SPIELPLAN_HTML = "<table><tbody><tr><td>KW</td></tr></tbody></table>";
    private static final String RANGLISTE_HTML = "<table><tbody><tr><td>Platz</td></tr></tbody></table>";
    private static final String DIREKTVERGLEICH_HTML = "<table><tbody><tr><td>DV</td></tr></tbody></table>";

    @Test
    void sectionIdsVorhanden() {
        var html = seite().erstelleAusRendertHtml(tabellenHtml());
        assertThat(html).contains("id=\"meldeliste\"");
        assertThat(html).contains("id=\"spielplan\"");
        assertThat(html).contains("id=\"rangliste\"");
        assertThat(html).contains("id=\"direktvergleich\"");
    }

    @Test
    void pdfLinkErscheint_wennUrlGesetzt() {
        var html = seiteMitPdf()
                .erstelleAusRendertHtml(tabellenHtml());
        assertThat(html).contains("href=\"https://example.com/spielplan.pdf\"");
        assertThat(html).contains("class=\"pdf-btn\"");
    }

    @Test
    void keinPdfLink_wennUrlFehlt() {
        var html = seite().erstelleAusRendertHtml(tabellenHtml());
        assertThat(html).doesNotContain("<a class=\"pdf-btn\"");
    }

    @Test
    void logoUrlWirdEscaped() {
        var html = seite()
                .logoUrl("https://example.com/logo.png?a=1&b=2")
                .erstelleAusRendertHtml(tabellenHtml());
        assertThat(html).contains("src=\"https://example.com/logo.png?a=1&amp;b=2\"");
        assertThat(html).doesNotContain("src=\"https://example.com/logo.png?a=1&b=2\"");
    }

    @Test
    void titelWirdEscaped() {
        var html = seite()
                .titel("<Gruppe A>")
                .erstelleAusRendertHtml(tabellenHtml());
        assertThat(html).contains("&lt;Gruppe A&gt;");
        assertThat(html).doesNotContain("<Gruppe A>");
    }

    @Test
    void tabellenHtmlWirdEingebettet() {
        var html = seite().erstelleAusRendertHtml(tabellenHtml());
        assertThat(html).contains(MELDELISTE_HTML);
        assertThat(html).contains(SPIELPLAN_HTML);
        assertThat(html).contains(RANGLISTE_HTML);
        assertThat(html).contains(DIREKTVERGLEICH_HTML);
    }

    @Test
    void navLinksVorhanden() {
        var html = seite().erstelleAusRendertHtml(tabellenHtml());
        assertThat(html).contains("href=\"#meldeliste\"");
        assertThat(html).contains("href=\"#spielplan\"");
        assertThat(html).contains("href=\"#rangliste\"");
        assertThat(html).contains("href=\"#direktvergleich\"");
    }

    @Test
    void pdfUrlWirdEscaped() {
        var html = ExportHtmlSeite.nurFuerTests()
                .sections(List.of(new ExportHtmlSeite.Section("spielplan", "Spielplan", "Spielplan",
                        "https://example.com/datei?name=a&b=c")))
                .erstelleAusRendertHtml(List.of(SPIELPLAN_HTML));
        assertThat(html).contains("href=\"https://example.com/datei?name=a&amp;b=c\"");
    }

    @Test
    void fehlendeTabelleHtmlNutztI18nHinweis() {
        assertThat(ExportHtmlSeite.fehlendeTabelleHtml("Spielplan")).contains("<p><em>");
    }

    private ExportHtmlSeite seite() {
        return ExportHtmlSeite.nurFuerTests()
                .titel("Liga")
                .sections(sections(null));
    }

    private ExportHtmlSeite seiteMitPdf() {
        return ExportHtmlSeite.nurFuerTests()
                .titel("Liga")
                .sections(sections("https://example.com/spielplan.pdf"));
    }

    private List<ExportHtmlSeite.Section> sections(String spielplanPdfUrl) {
        return List.of(
                new ExportHtmlSeite.Section("meldeliste", "Meldeliste", "Meldeliste", null),
                new ExportHtmlSeite.Section("spielplan", "Spielplan", "Spielplan", spielplanPdfUrl),
                new ExportHtmlSeite.Section("rangliste", "Rangliste", "Rangliste", null),
                new ExportHtmlSeite.Section("direktvergleich", "Direktvergleich", "Direktvergleich", null));
    }

    private List<String> tabellenHtml() {
        return List.of(MELDELISTE_HTML, SPIELPLAN_HTML, RANGLISTE_HTML, DIREKTVERGLEICH_HTML);
    }
}
