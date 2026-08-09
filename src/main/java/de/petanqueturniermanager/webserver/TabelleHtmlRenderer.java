/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.webserver;

import java.util.List;

import org.apache.commons.text.StringEscapeUtils;

/**
 * Rendert ein {@link TabelleModel} als HTML-{@code <table>}-Fragment.
 * <p>
 * Wiederverwendbar für alle Turniersysteme — keinerlei Liga-spezifische Logik.
 * <p>
 * Rendering-Regeln orientieren sich an {@code frontend/src/Cell.jsx} und
 * {@code frontend/src/Panel.jsx}, damit Export und Live-Ansicht vergleichbar aussehen.
 */
public class TabelleHtmlRenderer {

    private static final float UNO_ZU_PX = 37.795f;
    private static final int SPALTENBREITE_FALLBACK = 2000;
    private static final int ZEILENHOEHE_FALLBACK = 600;
    private final boolean pdfKompatibel;

    public TabelleHtmlRenderer() {
        this(false);
    }

    private TabelleHtmlRenderer(boolean pdfKompatibel) {
        this.pdfKompatibel = pdfKompatibel;
    }

    public static TabelleHtmlRenderer fuerPdf() {
        return new TabelleHtmlRenderer(true);
    }

    public String render(TabelleModel model) {
        return render(model, false);
    }

    /**
     * @param mitKopfFusszeile rendert zusätzlich die Seiten-Kopf-/Fußzeile (Links/Mitte/Rechts aus
     *                          dem PageStyle) als eigene {@code <tbody>}- bzw. {@code <tfoot>}-Zeile
     *                          mit {@code colspan} über alle Spalten. Beide Elemente sind dadurch
     *                          strukturell an die Tabellenbreite gebunden (im Gegensatz zu einem
     *                          einfachen {@code <div>}, das in der Bild-Export-Einbettung auf volle
     *                          Seitenbreite gestreckt würde). {@code <caption>} wurde bewusst NICHT
     *                          verwendet: openhtmltopdf bindet dessen Breite nicht an die Tabelle,
     *                          sondern an die Seite (empirisch belegt in
     *                          {@code HtmlZuBildKonvertiererTest#kopfUndFusszeileBleibenAnTabellenbreiteGebunden}).
     *                          Die 3-Spalten-Aufteilung Links/Mitte/Rechts nutzt ebenfalls eine
     *                          verschachtelte Tabelle statt Flexbox, da openhtmltopdf kein
     *                          {@code display:flex} unterstützt.
     */
    public String render(TabelleModel model, boolean mitKopfFusszeile) {
        var sb = new StringBuilder();
        sb.append("<table>");
        renderColgroup(model, sb);
        if (mitKopfFusszeile) {
            renderKopfzeileZeile(model, sb);
        }
        int kopfZeilenAnzahl = model.getKopfZeilenAnzahl();
        renderThead(model, kopfZeilenAnzahl, sb);
        if (mitKopfFusszeile) {
            renderFusszeileTfoot(model, sb);
        }
        renderTbody(model, kopfZeilenAnzahl, sb);
        sb.append("</table>");
        return sb.toString();
    }

    private void renderKopfzeileZeile(TabelleModel model, StringBuilder sb) {
        if (istLeer(model.getKopfzeileLinks(), model.getKopfzeileMitte(), model.getKopfzeileRechts())) {
            return;
        }
        sb.append("<tbody><tr><td colspan=\"").append(model.getSpalten()).append("\">");
        renderDreiSpaltenZeile(model.getKopfzeileLinks(), model.getKopfzeileMitte(), model.getKopfzeileRechts(), sb);
        sb.append("</td></tr></tbody>");
    }

    private void renderFusszeileTfoot(TabelleModel model, StringBuilder sb) {
        if (istLeer(model.getFusszeileLinks(), model.getFusszeileMitte(), model.getFusszeileRechts())) {
            return;
        }
        sb.append("<tfoot><tr><td colspan=\"").append(model.getSpalten()).append("\">");
        renderDreiSpaltenZeile(model.getFusszeileLinks(), model.getFusszeileMitte(), model.getFusszeileRechts(), sb);
        sb.append("</td></tr></tfoot>");
    }

    /** Links/Mitte/Rechts-Aufteilung über eine verschachtelte 3-Spalten-Tabelle (kein Flexbox-Support in openhtmltopdf). */
    private void renderDreiSpaltenZeile(String links, String mitte, String rechts, StringBuilder sb) {
        String stil = "border:none;font-family:Arial,Helvetica,sans-serif;font-size:9pt;";
        sb.append("<table style=\"width:100%;\"><tr>")
                .append("<td style=\"").append(stil).append("text-align:left;\">")
                .append(escapeMitZeilenumbruch(links)).append("</td>")
                .append("<td style=\"").append(stil).append("text-align:center;\">")
                .append(escapeMitZeilenumbruch(mitte)).append("</td>")
                .append("<td style=\"").append(stil).append("text-align:right;\">")
                .append(escapeMitZeilenumbruch(rechts)).append("</td>")
                .append("</tr></table>");
    }

    private static boolean istLeer(String links, String mitte, String rechts) {
        return links.isBlank() && mitte.isBlank() && rechts.isBlank();
    }

    private void renderColgroup(TabelleModel model, StringBuilder sb) {
        sb.append("<colgroup>");
        var spaltenBreiten = model.getSpaltenBreiten();
        for (int c = 0; c < model.getSpalten(); c++) {
            Integer breite = spaltenBreiten.get(c);
            if (istVersteckt(breite)) {
                sb.append("<col style=\"display:none\" />");
            } else {
                int px = zuPx(breite != null ? breite : SPALTENBREITE_FALLBACK);
                sb.append("<col style=\"width:").append(px).append("px\" />");
            }
        }
        sb.append("</colgroup>");
    }

    private void renderThead(TabelleModel model, int kopfZeilenAnzahl, StringBuilder sb) {
        var gitter = model.getGitter();
        if (kopfZeilenAnzahl <= 0) {
            return;
        }
        sb.append("<thead>");
        for (int r = 0; r < kopfZeilenAnzahl && r < gitter.size(); r++) {
            renderZeile(model, gitter.get(r), r, sb);
        }
        sb.append("</thead>");
    }

    private void renderTbody(TabelleModel model, int kopfZeilenAnzahl, StringBuilder sb) {
        var gitter = model.getGitter();
        sb.append("<tbody>");
        for (int r = kopfZeilenAnzahl; r < gitter.size(); r++) {
            renderZeile(model, gitter.get(r), r, sb);
        }
        sb.append("</tbody>");
    }

    private void renderZeile(TabelleModel model, List<String> zeile, int zeilenIdx, StringBuilder sb) {
        Integer hoehe = model.getZeilenHoehen().get(zeilenIdx);
        int px = zuPx(hoehe != null ? hoehe : ZEILENHOEHE_FALLBACK);
        sb.append("<tr style=\"height:").append(px).append("px\">");

        var spaltenBreiten = model.getSpaltenBreiten();
        for (int c = 0; c < zeile.size(); c++) {
            if (istVersteckt(spaltenBreiten.get(c))) {
                continue;
            }
            String id = zeile.get(c);
            if (id == null) {
                continue; // Merge-Slave
            }
            ZelleModel zelle = model.getZellen().get(id);
            if (zelle == null) {
                sb.append("<td></td>"); // catch-Fall aus TabellenMapper
            } else {
                renderZelle(zelle, sb);
            }
        }

        sb.append("</tr>");
    }

    private void renderZelle(ZelleModel zelle, StringBuilder sb) {
        StyleModel s = zelle.stil();
        sb.append("<td");
        if (s.colspan() > 1) {
            sb.append(" colspan=\"").append(s.colspan()).append("\"");
        }
        if (s.rowspan() > 1) {
            sb.append(" rowspan=\"").append(s.rowspan()).append("\"");
        }
        sb.append(" style=\"").append(buildCss(s)).append("\"");
        sb.append(">");
        String wert = escapeMitZeilenumbruch(zelle.wert());
        if (pdfKompatibel && istRechtwinkligGedreht(s.rotationGrad())) {
            sb.append("<span style=\"").append(buildPdfRotationCss(s.rotationGrad())).append("\">")
                    .append(wert)
                    .append("</span>");
        } else {
            sb.append(wert);
        }
        sb.append("</td>");
    }

    private String buildCss(StyleModel s) {
        var css = new StringBuilder();
        css.append("overflow:hidden;");

        if (s.hintergrundfarbe() != null) {
            css.append("background-color:").append(s.hintergrundfarbe()).append(";");
        }
        if (s.schriftfarbe() != null) {
            css.append("color:").append(s.schriftfarbe()).append(";");
        }
        if (s.fett()) {
            css.append("font-weight:bold;");
        }
        if (s.kursiv()) {
            css.append("font-style:italic;");
        }
        if (s.schriftart() != null) {
            css.append("font-family:").append(bereinigeFontFamily(s.schriftart())).append(";");
        }
        if (s.schriftgroesse() > 0) {
            css.append("font-size:").append(formatPt(s.schriftgroesse())).append("pt;");
        }
        if (s.ausrichtung() != null) {
            css.append("text-align:").append(s.ausrichtung()).append(";");
        }
        String va = s.vertikaleAusrichtung() != null ? s.vertikaleAusrichtung() : "top";
        css.append("vertical-align:").append(va).append(";");
        css.append(s.zeilenumbruch() ? "white-space:normal;" : "white-space:nowrap;");

        int rot = s.rotationGrad();
        if (!pdfKompatibel && istRechtwinkligGedreht(rot)) {
            css.append("writing-mode:vertical-rl;");
        } else if (!(pdfKompatibel && istRechtwinkligGedreht(rot)) && rot != 0) {
            css.append("transform:rotate(").append(rot).append("deg);");
        }

        if (s.linienOben() != null) {
            css.append("border-top:").append(s.linienOben()).append(";");
        }
        if (s.linienUnten() != null) {
            css.append("border-bottom:").append(s.linienUnten()).append(";");
        }
        if (s.linienLinks() != null) {
            css.append("border-left:").append(s.linienLinks()).append(";");
        }
        if (s.linienRechts() != null) {
            css.append("border-right:").append(s.linienRechts()).append(";");
        }

        return css.toString();
    }

    private static String escapeMitZeilenumbruch(String wert) {
        return StringEscapeUtils.escapeXml11(wert != null ? wert : "")
                .replace("\r\n", "<br/>").replace("\n", "<br/>").replace("\r", "<br/>");
    }

    private boolean istRechtwinkligGedreht(int rotationGrad) {
        return rotationGrad == 90 || rotationGrad == 270;
    }

    private String buildPdfRotationCss(int rotationGrad) {
        return "display:inline-block;white-space:nowrap;transform:rotate(" + rotationGrad
                + "deg);transform-origin:center center;";
    }

    private String bereinigeFontFamily(String fontFamily) {
        return fontFamily.replaceAll("[;\"'<>]", "");
    }

    private String formatPt(float pt) {
        if (Float.compare(pt, (float) Math.floor(pt)) == 0) {
            return String.valueOf((int) pt);
        }
        return String.valueOf(pt);
    }

    private static boolean istVersteckt(Integer breite) {
        return breite != null && breite == 0;
    }

    private static int zuPx(int unoEinheit) {
        return Math.round(unoEinheit / UNO_ZU_PX);
    }
}
