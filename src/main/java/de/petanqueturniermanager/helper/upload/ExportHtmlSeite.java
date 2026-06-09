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

    private static final String PTM_URL = "https://michaelmassee.github.io/Petanque-Turnier-Manager/";

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
            sb.append("<img class=\"logo\" src=\"").append(StringEscapeUtils.escapeHtml4(logoUrl))
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

        sb.append("<footer>\n");
        sb.append("<a href=\"").append(StringEscapeUtils.escapeHtml4(PTM_URL)).append("\" target=\"_blank\">");
        sb.append(StringEscapeUtils.escapeHtml4(I18n.get("export.liga.footer.text")));
        sb.append("</a>\n</footer>\n");

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
                .page-header img.logo {
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
                footer {
                  margin: 0 auto;
                  width: min(100%, 1440px);
                  padding: 0 2rem 1.5rem;
                  color: var(--muted);
                  font-size: 0.85rem;
                }
                footer a { color: var(--muted); }
                @media (max-width: 640px) {
                  .page-header {
                    align-items: flex-start;
                    padding: 1rem;
                  }
                  .page-header h1 { font-size: 1.35rem; }
                  .ptm-subline { font-size: 0.85rem; }
                  .page-header img.logo { max-width: 92px; max-height: 56px; }
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
