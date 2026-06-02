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

    public String render(TabelleModel model) {
        var sb = new StringBuilder();
        sb.append("<table>");
        renderColgroup(model, sb);
        renderKopfUndRumpf(model, sb);
        sb.append("</table>");
        return sb.toString();
    }

    private void renderColgroup(TabelleModel model, StringBuilder sb) {
        sb.append("<colgroup>");
        var spaltenBreiten = model.getSpaltenBreiten();
        for (int c = 0; c < model.getSpalten(); c++) {
            Integer breite = spaltenBreiten.get(c);
            if (istVersteckt(breite)) {
                sb.append("<col style=\"display:none\">");
            } else {
                int px = zuPx(breite != null ? breite : SPALTENBREITE_FALLBACK);
                sb.append("<col style=\"width:").append(px).append("px\">");
            }
        }
        sb.append("</colgroup>");
    }

    private void renderKopfUndRumpf(TabelleModel model, StringBuilder sb) {
        int kopfZeilenAnzahl = model.getKopfZeilenAnzahl();
        var gitter = model.getGitter();

        if (kopfZeilenAnzahl > 0) {
            sb.append("<thead>");
            for (int r = 0; r < kopfZeilenAnzahl && r < gitter.size(); r++) {
                renderZeile(model, gitter.get(r), r, sb);
            }
            sb.append("</thead>");
        }

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
        sb.append(StringEscapeUtils.escapeHtml4(zelle.wert() != null ? zelle.wert() : ""));
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
        if (rot == 90 || rot == 270) {
            css.append("writing-mode:vertical-rl;");
        } else if (rot != 0) {
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

    private String bereinigeFontFamily(String fontFamily) {
        return fontFamily.replaceAll("[;\"'<>]", "");
    }

    private String formatPt(float pt) {
        if (pt == Math.floor(pt)) {
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
