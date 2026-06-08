/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.supermelee.export;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.webserver.TabelleHtmlRenderer;
import de.petanqueturniermanager.webserver.TabelleModel;
import de.petanqueturniermanager.webserver.TabellenMapper;

/**
 * Erzeugt eine responsive HTML-Exportseite für ein Supermêlée-Turnier.
 * <p>
 * Rendert Spieltag-Ranglisten und Endrangliste via {@link TabellenMapper} + {@link TabelleHtmlRenderer}.
 */
public class SupermeleeHtmlExportSeite {

    private static final String PTM_URL = "https://michaelmassee.github.io/Petanque-Turnier-Manager/";

    private final WorkingSpreadsheet workingSpreadsheet;
    private final SheetHelper sheetHelper;
    private final TabellenMapper tabellenMapper = new TabellenMapper();
    private final TabelleHtmlRenderer tabelleHtmlRenderer = new TabelleHtmlRenderer();

    private String logoUrl;
    private String turniername;
    private String endranglistePdfUrl;
    private final List<SpieltagSection> spieltagSections = new ArrayList<>();

    record SpieltagSection(String sheetName, String titel, String pdfUrl) {
    }

    private SupermeleeHtmlExportSeite(WorkingSpreadsheet workingSpreadsheet) {
        this.workingSpreadsheet = workingSpreadsheet;
        this.sheetHelper = workingSpreadsheet != null ? new SheetHelper(workingSpreadsheet) : null;
    }

    public static SupermeleeHtmlExportSeite from(WorkingSpreadsheet workingSpreadsheet) {
        return new SupermeleeHtmlExportSeite(workingSpreadsheet);
    }

    public SupermeleeHtmlExportSeite logoUrl(String url) {
        this.logoUrl = url;
        return this;
    }

    public SupermeleeHtmlExportSeite turniername(String name) {
        this.turniername = name;
        return this;
    }

    public SupermeleeHtmlExportSeite endranglistePdfUrl(String url) {
        this.endranglistePdfUrl = url;
        return this;
    }

    public SupermeleeHtmlExportSeite fuegeSpieltagHinzu(String sheetName, String titel, String pdfUrl) {
        spieltagSections.add(new SpieltagSection(sheetName, titel, pdfUrl));
        return this;
    }

    public String erstelle() throws GenerateException {
        var doc = workingSpreadsheet.getWorkingSpreadsheetDocument();
        var endranglisteHtml = renderSheet(SheetNamen.endrangliste(), doc);

        var spieltagHtmls = new ArrayList<String>();
        for (var section : spieltagSections) {
            spieltagHtmls.add(renderSheet(section.sheetName(), doc));
        }

        return assembliere(endranglisteHtml, spieltagHtmls);
    }

    private String renderSheet(String sheetName, com.sun.star.sheet.XSpreadsheetDocument doc)
            throws GenerateException {
        var sheet = sheetHelper.findByName(sheetName);
        if (sheet == null) {
            return "<p><em>" + StringEscapeUtils.escapeHtml4(sheetName) + " nicht gefunden</em></p>";
        }
        TabelleModel model = tabellenMapper.map(sheet, doc);
        return tabelleHtmlRenderer.render(model);
    }

    private String assembliere(String endranglisteHtml, List<String> spieltagHtmls) {
        var sb = new StringBuilder(16384);

        sb.append("<!DOCTYPE html>\n<html lang=\"de\">\n<head>\n");
        sb.append("<meta charset=\"UTF-8\">\n");
        sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        sb.append("<title>");
        if (StringUtils.isNotBlank(turniername)) {
            sb.append(StringEscapeUtils.escapeHtml4(turniername)).append(" – ");
        }
        sb.append("Supermêlée</title>\n");
        sb.append("<style>\n").append(css()).append("</style>\n");
        sb.append("</head>\n<body>\n");

        sb.append("<header>\n");
        sb.append("<div class=\"header-text\">");
        if (StringUtils.isNotBlank(turniername)) {
            sb.append("<h1>").append(StringEscapeUtils.escapeHtml4(turniername)).append("</h1>");
        } else {
            sb.append("<h1>Supermêlée</h1>");
        }
        sb.append("<div class=\"ptm-subline\">Pétanque Turnier Manager</div>");
        sb.append("</div>");
        if (StringUtils.isNotBlank(logoUrl)) {
            sb.append("<img class=\"logo\" src=\"").append(StringEscapeUtils.escapeHtml4(logoUrl))
                    .append("\" alt=\"Logo\">");
        }
        sb.append("\n</header>\n");

        sb.append("<nav>\n");
        sb.append(navLink("endrangliste", I18n.get("export.supermelee.nav.endrangliste")));
        for (var section : spieltagSections) {
            sb.append(navLink("spieltag-" + section.sheetName(), section.titel()));
        }
        sb.append("\n</nav>\n");

        sb.append("<main>\n");
        appendSection(sb, "endrangliste", I18n.get("export.supermelee.nav.endrangliste"), endranglistePdfUrl,
                endranglisteHtml);
        for (int i = 0; i < spieltagSections.size(); i++) {
            var section = spieltagSections.get(i);
            appendSection(sb, "spieltag-" + section.sheetName(), section.titel(), section.pdfUrl(),
                    spieltagHtmls.get(i));
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
        sb.append("<section id=\"").append(id).append("\">\n");
        sb.append("<h2>").append(StringEscapeUtils.escapeHtml4(titel)).append("</h2>\n");
        if (StringUtils.isNotBlank(pdfUrl)) {
            sb.append("<a class=\"pdf-btn\" href=\"").append(StringEscapeUtils.escapeHtml4(pdfUrl))
                    .append("\" download>&#128462; ")
                    .append(StringEscapeUtils.escapeHtml4(I18n.get("export.liga.pdf.herunterladen")))
                    .append("</a>\n");
        }
        sb.append("<div class=\"tbl-scroll\">\n").append(tabelleHtml).append("\n</div>\n");
        sb.append("</section>\n");
    }

    private String navLink(String id, String label) {
        return "<a href=\"#" + id + "\">" + StringEscapeUtils.escapeHtml4(label) + "</a>\n";
    }

    private String css() {
        return """
                *, *::before, *::after { box-sizing: border-box; }
                body { font-family: system-ui, sans-serif; margin: 0; color: #222; }
                header { display: flex; align-items: center; gap: 1rem; padding: 1rem 1.5rem;
                         background: #f4f4f4; border-bottom: 2px solid #ddd; }
                .header-text { flex: 1; }
                header h1 { margin: 0; font-size: 1.6rem; }
                .ptm-subline { font-size: 0.8rem; color: #888; margin-top: 2px; }
                header img.logo { max-height: 80px; object-fit: contain; }
                nav { position: sticky; top: 0; z-index: 100; background: #fff;
                      border-bottom: 1px solid #ddd; padding: 0.4rem 1.5rem;
                      display: flex; gap: 1.5rem; flex-wrap: wrap; }
                nav a { text-decoration: none; color: #0055cc; font-size: 0.9rem; }
                nav a:hover { text-decoration: underline; }
                main { padding: 1rem 1.5rem; }
                section { margin-bottom: 2.5rem; }
                section h2 { border-bottom: 1px solid #ccc; padding-bottom: 0.3rem; }
                .tbl-scroll { overflow-x: auto; }
                table { border-collapse: collapse; font-size: 0.9rem; }
                thead tr { background: #e8edf5; }
                td, th { padding: 4px 6px; }
                .pdf-btn { display: inline-flex; align-items: center; gap: 0.4rem;
                           margin-bottom: 0.5rem; padding: 0.3rem 0.7rem;
                           border: 1px solid #888; border-radius: 4px;
                           text-decoration: none; color: #333; font-size: 0.85rem; }
                .pdf-btn:hover { background: #f0f0f0; }
                footer { margin-top: 2rem; padding: 0.8rem 1.5rem; font-size: 0.8rem;
                         color: #888; border-top: 1px solid #eee; }
                footer a { color: #888; }
                """;
    }
}
