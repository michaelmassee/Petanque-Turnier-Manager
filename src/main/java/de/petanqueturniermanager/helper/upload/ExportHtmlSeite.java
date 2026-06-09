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
    private static final String PTM_EXPORT_LOGO_DATA_URI = "data:image/png;base64,"
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
        sb.append("<img class=\"ptm-export-logo\" src=\"").append(PTM_EXPORT_LOGO_DATA_URI)
                .append("\" alt=\"Pétanque Turnier Manager\">");
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
                .ptm-export-logo {
                  flex: 0 0 auto;
                  width: 72px;
                  height: 70px;
                  object-fit: contain;
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
                  .ptm-export-logo { width: 48px; height: 47px; }
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
