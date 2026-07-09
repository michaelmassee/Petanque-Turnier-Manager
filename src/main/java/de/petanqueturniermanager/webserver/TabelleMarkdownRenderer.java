/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.webserver;

import java.util.List;

/**
 * Rendert ein {@link TabelleModel} als Markdown-Pipe-Tabelle.
 * <p>
 * Reine Textausgabe — Zellformatierung (Farbe, Fett, Ausrichtung) entfällt,
 * nur der Zellwert wird übernommen. Merge-Slaves (gitter-Eintrag {@code null})
 * werden wie im HTML-Rendering übersprungen.
 */
public class TabelleMarkdownRenderer {

    public String render(TabelleModel model) {
        var sb = new StringBuilder();
        var gitter = model.getGitter();
        int spalten = sichtbareSpaltenAnzahl(model);
        if (gitter.isEmpty()) {
            return sb.toString();
        }

        renderZeile(model, gitter.get(0), sb);
        sb.append(trennzeile(spalten));

        for (int r = 1; r < gitter.size(); r++) {
            renderZeile(model, gitter.get(r), sb);
        }
        return sb.toString();
    }

    private int sichtbareSpaltenAnzahl(TabelleModel model) {
        int anzahl = 0;
        for (int c = 0; c < model.getSpalten(); c++) {
            if (!istVersteckt(model.getSpaltenBreiten().get(c))) {
                anzahl++;
            }
        }
        return anzahl;
    }

    private void renderZeile(TabelleModel model, List<String> zeile, StringBuilder sb) {
        var spaltenBreiten = model.getSpaltenBreiten();
        sb.append("|");
        for (int c = 0; c < zeile.size(); c++) {
            if (istVersteckt(spaltenBreiten.get(c))) {
                continue;
            }
            String id = zeile.get(c);
            if (id == null) {
                sb.append(" |");
                continue;
            }
            ZelleModel zelle = model.getZellen().get(id);
            String wert = zelle == null || zelle.wert() == null ? "" : zelle.wert();
            sb.append(" ").append(escaped(wert)).append(" |");
        }
        sb.append("\n");
    }

    private String trennzeile(int spalten) {
        var sb = new StringBuilder("|");
        for (int c = 0; c < spalten; c++) {
            sb.append(" --- |");
        }
        return sb.append("\n").toString();
    }

    private String escaped(String wert) {
        return wert.replace("|", "\\|").replace("\n", " ").replace("\r", "");
    }

    private static boolean istVersteckt(Integer breite) {
        return breite != null && breite == 0;
    }
}
