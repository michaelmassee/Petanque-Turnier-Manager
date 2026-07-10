/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.upload;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

/**
 * Erzeugt ein minimales HTML-Dokument aus einem {@code <table>}-Fragment
 * für die Konvertierung nach PDF via {@link HtmlZuPdfKonvertierer}.
 */
public class PdfHtmlDokument {

    private PdfHtmlDokument() {
    }

    public static String erstelle(String titel, String tabelleFragment) {
        String sichereTitel = StringEscapeUtils.escapeHtml4(titel);
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
                    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
                <html xmlns="http://www.w3.org/1999/xhtml" lang="de">
                <head>
                <meta charset="UTF-8"/>
                <title>%s</title>
                <style>
                %s
                </style>
                </head>
                <body>
                <h2>%s</h2>
                %s
                %s
                </body>
                </html>
                """.formatted(sichereTitel, css(), sichereTitel, tabelleFragment, ExportFooterHtml.html(false));
    }

    /**
     * Erzeugt ein Dokument mit mehreren Abschnitten (je Titel + Tabellen-HTML-Fragment),
     * mit gemeinsamem Dokumentkopf (Titel + optionalem Turnierlogo).
     * Jeder Abschnitt außer dem ersten beginnt auf einer neuen Seite ({@code page-break-before}).
     */
    public static String erstelle(String dokumentTitel, String logoUrl, List<String> abschnittTitel,
            List<String> tabellenFragmente) {
        String sichererDokumentTitel = StringEscapeUtils.escapeHtml4(dokumentTitel);
        var kopf = new StringBuilder();
        kopf.append("<header class=\"dokument-kopf\">\n<h1>").append(sichererDokumentTitel).append("</h1>\n");
        if (StringUtils.isNotBlank(logoUrl)) {
            kopf.append("<img class=\"dokument-logo\" src=\"").append(StringEscapeUtils.escapeHtml4(logoUrl))
                    .append("\" alt=\"Logo\" />\n");
        }
        kopf.append("</header>\n");

        var abschnitte = new StringBuilder();
        for (int i = 0; i < abschnittTitel.size(); i++) {
            String sichererTitel = StringEscapeUtils.escapeHtml4(abschnittTitel.get(i));
            String pageBreak = i == 0 ? "" : " style=\"page-break-before: always;\"";
            abschnitte.append("<section").append(pageBreak).append(">\n")
                    .append("<h2>").append(sichererTitel).append("</h2>\n")
                    .append(tabellenFragmente.get(i)).append("\n")
                    .append("</section>\n");
        }
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
                    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
                <html xmlns="http://www.w3.org/1999/xhtml" lang="de">
                <head>
                <meta charset="UTF-8"/>
                <title>%s</title>
                <style>
                %s
                </style>
                </head>
                <body>
                %s
                %s
                %s
                </body>
                </html>
                """.formatted(sichererDokumentTitel, css(), kopf, abschnitte, ExportFooterHtml.html(false));
    }

    private static String css() {
        return """
                @page { size: A4 landscape; margin: 1cm; }
                body {
                  font-family: PTMPdfSans, Arial, Helvetica, sans-serif;
                  font-size: 9pt;
                  margin: 0;
                }
                h2 {
                  font-size: 11pt;
                  margin: 0 0 0.4cm 0;
                }
                table {
                  border-collapse: collapse;
                }
                td, th {
                  padding: 2px 4px;
                }
                .dokument-kopf {
                  display: flex;
                  align-items: center;
                  justify-content: space-between;
                  gap: 0.5cm;
                  margin-bottom: 0.4cm;
                }
                .dokument-kopf h1 {
                  font-size: 14pt;
                  margin: 0;
                }
                .dokument-logo {
                  max-height: 1.8cm;
                  max-width: 5cm;
                  object-fit: contain;
                }
                """
                + ExportFooterHtml.pdfCss();
    }
}
