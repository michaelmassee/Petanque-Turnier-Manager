/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.upload;

import org.apache.commons.text.StringEscapeUtils;

import de.petanqueturniermanager.helper.i18n.I18n;

/**
 * Gemeinsamer Footer für HTML- und PDF-Exports.
 */
final class ExportFooterHtml {

    private ExportFooterHtml() {
    }

    static String html(boolean targetBlank) {
        String target = targetBlank ? " target=\"_blank\"" : "";
        return """
                <footer>
                <a href="%s"%s>%s</a>
                <a class="ptm-footer-logo-link" href="%s"%s>
                <img class="ptm-footer-logo" src="%s" alt="Pétanque Turnier Manager" />
                </a>
                </footer>
                """.formatted(
                StringEscapeUtils.escapeHtml4(ExportHtmlSeite.PTM_URL),
                target,
                StringEscapeUtils.escapeHtml4(I18n.get("export.liga.footer.text")),
                StringEscapeUtils.escapeHtml4(ExportHtmlSeite.PTM_URL),
                target,
                ExportHtmlSeite.PTM_EXPORT_LOGO_DATA_URI);
    }

    static String webCss() {
        return """
                footer {
                  display: flex;
                  align-items: center;
                  justify-content: space-between;
                  margin: 0 auto;
                  width: min(100%, 1440px);
                  padding: 0.75rem 2rem 1.5rem;
                  color: var(--muted);
                  font-size: 0.85rem;
                }
                footer a { color: var(--muted); }
                .ptm-footer-logo { width: 40px; height: 39px; object-fit: contain; opacity: 0.7; }
                .ptm-footer-logo-link:hover .ptm-footer-logo,
                .ptm-footer-logo-link:focus-visible .ptm-footer-logo { opacity: 1; }
                """;
    }

    static String pdfCss() {
        return """
                footer {
                  margin-top: 0.5cm;
                  padding-top: 0.25cm;
                  border-top: 0.5pt solid #d8dee8;
                  color: #667085;
                  font-size: 8pt;
                }
                footer a {
                  color: #667085;
                  text-decoration: none;
                }
                .ptm-footer-logo-link {
                  float: right;
                }
                .ptm-footer-logo {
                  width: 32px;
                  height: 31px;
                  object-fit: contain;
                  opacity: 0.75;
                }
                """;
    }
}
