/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.upload;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.webserver.TabelleHtmlRenderer;
import de.petanqueturniermanager.webserver.TabelleModel;
import de.petanqueturniermanager.webserver.TabellenMapper;

/**
 * Einheitliche HTML-Exportseite für Turniersystem-Exports.
 */
public class ExportHtmlSeite {

    static final String PTM_URL = "https://michaelmassee.github.io/Petanque-Turnier-Manager/";
    static final String PTM_EXPORT_LOGO_DATA_URI = "data:image/png;base64,"
            + "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";

    private final WorkingSpreadsheet workingSpreadsheet;
    private final SheetHelper sheetHelper;
    private final TabellenMapper tabellenMapper = new TabellenMapper();
    private final TabelleHtmlRenderer tabelleHtmlRenderer = new TabelleHtmlRenderer();

    private String titel;
    private String logoUrl;
    private final List<Section> sections = new ArrayList<>();

    public record Section(String id, String titel, String sheetName, String pdfUrl) {
    }

    private ExportHtmlSeite(WorkingSpreadsheet workingSpreadsheet) {
        this.workingSpreadsheet = workingSpreadsheet;
        this.sheetHelper = workingSpreadsheet != null ? new SheetHelper(workingSpreadsheet) : null;
    }

    public static ExportHtmlSeite from(WorkingSpreadsheet workingSpreadsheet) {
        return new ExportHtmlSeite(workingSpreadsheet);
    }

    public ExportHtmlSeite titel(String titel) {
        this.titel = titel;
        return this;
    }

    public ExportHtmlSeite logoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
        return this;
    }

    public ExportHtmlSeite sections(List<Section> sections) {
        this.sections.clear();
        this.sections.addAll(sections);
        return this;
    }

    public String erstelle() throws GenerateException {
        XSpreadsheetDocument doc = workingSpreadsheet.getWorkingSpreadsheetDocument();
        var tabellenHtml = new ArrayList<String>();
        for (var section : sections) {
            tabellenHtml.add(renderSheet(section.sheetName(), doc));
        }
        return assembliere(tabellenHtml);
    }

    public String erstelleAusRendertHtml(List<String> tabellenHtml) {
        return assembliere(tabellenHtml);
    }

    public static ExportHtmlSeite nurFuerTests() {
        return new ExportHtmlSeite(null);
    }

    private String renderSheet(String sheetName, XSpreadsheetDocument doc) throws GenerateException {
        var sheet = sheetHelper.findByName(sheetName);
        if (sheet == null) {
            return fehlendeTabelleHtml(sheetName);
        }
        TabelleModel model = tabellenMapper.map(sheet, doc);
        return tabelleHtmlRenderer.render(model);
    }

    public static String fehlendeTabelleHtml(String sheetName) {
        return "<p><em>" + StringEscapeUtils.escapeHtml4(I18n.get("export.html.tabelle.fehlt", sheetName))
                + "</em></p>";
    }

    private String assembliere(List<String> tabellenHtml) {
        var sb = new StringBuilder(16384);
        String seitentitel = StringUtils.defaultIfBlank(titel, "Export");

        sb.append("<!DOCTYPE html>\n<html lang=\"de\">\n<head>\n");
        sb.append("<meta charset=\"UTF-8\">\n");
        sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        sb.append("<title>").append(StringEscapeUtils.escapeHtml4(seitentitel)).append("</title>\n");
        sb.append("<style>\n").append(css()).append("</style>\n");
        sb.append("</head>\n<body>\n");

        sb.append("<header class=\"page-header\">\n");
        sb.append("<div class=\"header-text\">");
        sb.append("<h1>").append(StringEscapeUtils.escapeHtml4(seitentitel)).append("</h1>");
        sb.append("<div class=\"ptm-subline\">Pétanque Turnier Manager</div>");
        sb.append("</div>");
        if (StringUtils.isNotBlank(logoUrl)) {
            sb.append("<img class=\"turnier-logo\" src=\"").append(StringEscapeUtils.escapeHtml4(logoUrl))
                    .append("\" alt=\"Logo\">");
        }
        sb.append("\n</header>\n");

        sb.append("<nav class=\"section-nav\" aria-label=\"Abschnitte\">\n");
        for (var section : sections) {
            sb.append(navLink(section.id(), section.titel()));
        }
        sb.append("\n</nav>\n");

        sb.append("<main>\n");
        for (int i = 0; i < sections.size(); i++) {
            var section = sections.get(i);
            appendSection(sb, section.id(), section.titel(), section.pdfUrl(), tabellenHtml.get(i));
        }
        sb.append("</main>\n");

        sb.append(ExportFooterHtml.html(true));

        sb.append("</body>\n</html>");
        return sb.toString();
    }

    private void appendSection(StringBuilder sb, String id, String titel, String pdfUrl, String tabelleHtml) {
        sb.append("<section class=\"export-section\" id=\"").append(StringEscapeUtils.escapeHtml4(id)).append("\">\n");
        sb.append("<div class=\"section-heading\">\n");
        sb.append("<h2>").append(StringEscapeUtils.escapeHtml4(titel)).append("</h2>\n");
        if (StringUtils.isNotBlank(pdfUrl)) {
            sb.append("<a class=\"pdf-btn\" href=\"").append(StringEscapeUtils.escapeHtml4(pdfUrl))
                    .append("\" download>&#128462; ")
                    .append(StringEscapeUtils.escapeHtml4(I18n.get("export.liga.pdf.herunterladen")))
                    .append("</a>\n");
        }
        sb.append("</div>\n");
        sb.append("<div class=\"tbl-scroll\">\n").append(tabelleHtml).append("\n</div>\n");
        sb.append("</section>\n");
    }

    private String navLink(String id, String label) {
        return "<a href=\"#" + StringEscapeUtils.escapeHtml4(id) + "\">"
                + StringEscapeUtils.escapeHtml4(label) + "</a>\n";
    }

    private String css() {
        return """
                *, *::before, *::after { box-sizing: border-box; }
                :root {
                  color-scheme: light;
                  --page-bg: #f6f7f9;
                  --surface: #ffffff;
                  --surface-soft: #eef3f8;
                  --text: #172033;
                  --muted: #667085;
                  --line: #d8dee8;
                  --accent: #0b6bcb;
                  --accent-strong: #084f96;
                  --accent-soft: #e7f1fc;
                  --shadow: 0 10px 24px rgba(23, 32, 51, 0.08);
                }
                html { scroll-behavior: smooth; }
                body {
                  margin: 0;
                  background: var(--page-bg);
                  color: var(--text);
                  font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
                  line-height: 1.45;
                }
                .page-header {
                  display: flex;
                  align-items: center;
                  gap: 1rem;
                  padding: 1.25rem clamp(1rem, 4vw, 2.5rem);
                  background: var(--surface);
                  border-bottom: 1px solid var(--line);
                }
                .header-text { flex: 1; min-width: 0; }
                .page-header h1 {
                  margin: 0;
                  font-size: 1.7rem;
                  line-height: 1.15;
                  font-weight: 750;
                  overflow-wrap: anywhere;
                }
                .ptm-subline {
                  margin-top: 0.25rem;
                  color: var(--muted);
                  font-size: 0.9rem;
                }
                .turnier-logo {
                  width: auto;
                  max-width: 30vw;
                  max-height: 78px;
                  object-fit: contain;
                }
                .section-nav {
                  position: sticky;
                  top: 0;
                  z-index: 100;
                  display: flex;
                  gap: 0.5rem;
                  overflow-x: auto;
                  padding: 0.75rem clamp(1rem, 4vw, 2.5rem);
                  background: rgba(255, 255, 255, 0.96);
                  border-bottom: 1px solid var(--line);
                  box-shadow: 0 4px 16px rgba(23, 32, 51, 0.06);
                  -webkit-overflow-scrolling: touch;
                }
                .section-nav a {
                  flex: 0 0 auto;
                  min-height: 2.25rem;
                  display: inline-flex;
                  align-items: center;
                  padding: 0.4rem 0.8rem;
                  border: 1px solid var(--line);
                  border-radius: 0.45rem;
                  background: var(--surface);
                  color: var(--accent-strong);
                  font-size: 0.92rem;
                  font-weight: 600;
                  text-decoration: none;
                  white-space: nowrap;
                }
                .section-nav a:hover,
                .section-nav a:focus-visible {
                  border-color: var(--accent);
                  background: var(--accent-soft);
                  outline: none;
                }
                main {
                  width: min(100%, 1440px);
                  margin: 0 auto;
                  padding: 1rem clamp(0.75rem, 3vw, 2rem) 2rem;
                }
                .export-section {
                  scroll-margin-top: 5.5rem;
                  margin: 0 0 1.25rem;
                  padding: clamp(0.75rem, 2.5vw, 1.25rem);
                  background: var(--surface);
                  border: 1px solid var(--line);
                  border-radius: 0.5rem;
                  box-shadow: var(--shadow);
                }
                .section-heading {
                  display: flex;
                  align-items: center;
                  justify-content: space-between;
                  gap: 0.75rem;
                  margin-bottom: 0.75rem;
                  padding-bottom: 0.7rem;
                  border-bottom: 1px solid var(--line);
                }
                .section-heading h2 {
                  margin: 0;
                  font-size: 1.2rem;
                  line-height: 1.2;
                  overflow-wrap: anywhere;
                }
                .tbl-scroll {
                  overflow-x: auto;
                  border: 1px solid var(--line);
                  border-radius: 0.4rem;
                  background: var(--surface);
                  -webkit-overflow-scrolling: touch;
                }
                table {
                  border-collapse: collapse;
                  min-width: max-content;
                  font-size: 0.92rem;
                }
                thead tr { background: var(--surface-soft); }
                td, th {
                  padding: 5px 7px;
                  border-color: var(--line);
                }
                .pdf-btn {
                  flex: 0 0 auto;
                  min-height: 2.4rem;
                  display: inline-flex;
                  align-items: center;
                  gap: 0.45rem;
                  padding: 0.45rem 0.8rem;
                  border: 1px solid var(--accent);
                  border-radius: 0.45rem;
                  background: var(--accent);
                  color: #ffffff;
                  font-size: 0.9rem;
                  font-weight: 700;
                  text-decoration: none;
                  white-space: nowrap;
                }
                .pdf-btn:hover,
                .pdf-btn:focus-visible {
                  background: var(--accent-strong);
                  border-color: var(--accent-strong);
                  outline: none;
                }
                """
                + ExportFooterHtml.webCss()
                + """
                @media (max-width: 640px) {
                  .page-header {
                    align-items: flex-start;
                    padding: 1rem;
                  }
                  .page-header h1 { font-size: 1.35rem; }
                  .ptm-subline { font-size: 0.85rem; }
                  .turnier-logo { max-width: 92px; max-height: 56px; }
                  .section-nav { padding: 0.65rem 1rem; }
                  main { padding: 0.75rem 0.65rem 1.5rem; }
                  .export-section {
                    padding: 0.65rem;
                    border-radius: 0.45rem;
                  }
                  .section-heading {
                    align-items: flex-start;
                    flex-direction: column;
                  }
                  .section-heading h2 { font-size: 1.05rem; }
                  .pdf-btn { width: 100%; justify-content: center; }
                  table { font-size: 0.88rem; }
                  td, th { padding: 4px 6px; }
                  footer { padding: 0 1rem 1.25rem; }
                }
                """;
    }
}
