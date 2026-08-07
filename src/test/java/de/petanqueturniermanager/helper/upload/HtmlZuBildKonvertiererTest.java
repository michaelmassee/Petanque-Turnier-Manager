/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.upload;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.webserver.StyleModel;
import de.petanqueturniermanager.webserver.TabelleHtmlRenderer;
import de.petanqueturniermanager.webserver.TabelleModel;
import de.petanqueturniermanager.webserver.ZelleModel;

/**
 * {@link HtmlZuBildKonvertierer} rendert immer großzügig überbreit/-hoch und schneidet danach auf
 * den tatsächlichen Inhalt zu (siehe Klassen-Doku) – die Tests hier decken genau dieses
 * Zuschneiden ab: schmale Tabellen bekamen vorher rechts einen weißen Balken, breite (v.a.
 * Colspan-lastige) Tabellen wurden rechts abgeschnitten.
 */
class HtmlZuBildKonvertiererTest {

    private static final String ROT = "#ff0000";

    @Test
    void konvertiertHtmlZuEngZugeschnittenemPng() throws Exception {
        String fragment = """
                <table><tbody><tr><td style="border:1px solid #000000;">
                Müller ÄÖÜ äöü ß Pétanque € ✓
                </td></tr></tbody></table>
                """;
        byte[] png = HtmlZuBildKonvertierer.konvertiere(fragment, "");

        assertThat(png).isNotEmpty();
        BufferedImage bild = ImageIO.read(new ByteArrayInputStream(png));
        assertThat(bild).isNotNull();
        assertThat(bild.getWidth()).isGreaterThan(0);
        // Sowohl Breite als auch Höhe folgen dem Inhalt, nicht der großzügigen Rendergröße.
        assertThat(bild.getHeight()).isLessThan(bild.getWidth() * 10);
    }

    @Test
    void schmaleTabelleHatKeinenWeissenBalkenRechts() throws Exception {
        BufferedImage bild = rendereEinzeiligeTabelle(3);
        assertRechteKanteIstRot(bild);
    }

    @Test
    void breiteTabelleWirdRechtsNichtAbgeschnitten() throws Exception {
        BufferedImage bild = rendereEinzeiligeTabelle(20);
        assertRechteKanteIstRot(bild);
    }

    /**
     * Reale Struktur einer Supermelee-Spielrunde: zwei Colspan-Kopfzeilen ("Spielrunde N" über
     * 6 Spalten, "Mannschaft A"/"Mannschaft B"/"Ergebnis" über je 3/3/2 Spalten) plus mehrere
     * Datenzeilen mit langen Namen – Repro für einen konkret gemeldeten Fall, bei dem die letzte
     * Spalte rechts abgeschnitten wurde.
     */
    @Test
    void spielrundeMitColspanHeaderWirdRechtsNichtAbgeschnitten() throws Exception {
        var normal = new StyleModel(false, false, null, null, null, null,
                1, 1, 0, false, null, 0, null, null, null, null);
        var rowspan2 = new StyleModel(false, false, "#cccccc", null, "center", null,
                1, 2, 0, false, null, 0, null, null, null, null);
        var colspan6 = new StyleModel(false, false, "#ffcc00", null, "center", null,
                6, 1, 0, false, null, 0, null, null, null, null);
        var colspan3 = new StyleModel(false, false, "#00ccff", null, "center", null,
                3, 1, 0, false, null, 0, null, null, null, null);
        var colspan2 = new StyleModel(false, false, "#00ff00", null, "center", null,
                2, 1, 0, false, null, 0, null, null, null, null);
        var randMarker = new StyleModel(false, false, ROT, null, null, null,
                1, 1, 0, false, null, 0, null, null, null, null);

        Map<Integer, Integer> spaltenBreiten = new LinkedHashMap<>();
        spaltenBreiten.put(0, 499);
        for (int c = 1; c <= 6; c++) {
            spaltenBreiten.put(c, 4001);
        }
        for (int c = 7; c <= 8; c++) {
            spaltenBreiten.put(c, 1799);
        }

        Map<String, ZelleModel> zellen = new LinkedHashMap<>();
        List<List<String>> gitter = new ArrayList<>();

        zellen.put("c-0-0", new ZelleModel("c-0-0", "#", rowspan2));
        zellen.put("c-0-1", new ZelleModel("c-0-1", "Spielrunde 4", colspan6));
        gitter.add(new ArrayList<>(Arrays.asList("c-0-0", "c-0-1", null, null, null, null, null, null, null)));

        zellen.put("c-1-1", new ZelleModel("c-1-1", "Mannschaft A", colspan3));
        zellen.put("c-1-4", new ZelleModel("c-1-4", "Mannschaft B", colspan3));
        zellen.put("c-1-7", new ZelleModel("c-1-7", "Ergebnis", colspan2));
        gitter.add(new ArrayList<>(Arrays.asList(null, "c-1-1", null, null, "c-1-4", null, null, "c-1-7", null)));

        String[][] daten = {
                { "1", "Lott, Milan", "Tafel, Rosa", "Molz, Kornelia", "Steuermann, Wiegand", "Mühlmann, Arne",
                        "Kühnle, Dietmar", "12", "72" },
                { "2", "Bohnet, Collin", "Bäder, Rita", "Bremen, Ehrentraut", "Zoll, Rosmarie", "Gehrig, Marina",
                        "Steinbrink, Diethelm", "10", "11" },
                { "3", "Gerhard, Niko", "Erickson, Mariegret", "Schneeweiß, Eveline", "Witschel, Danny",
                        "Pokorny, Elfgard", "Dold, Otbert", "5", "10" },
        };
        for (int r = 0; r < daten.length; r++) {
            var zeile = new ArrayList<String>();
            for (int c = 0; c < 9; c++) {
                String id = "c-" + (r + 2) + "-" + c;
                zellen.put(id, new ZelleModel(id, daten[r][c], c == 8 ? randMarker : normal));
                zeile.add(id);
            }
            gitter.add(zeile);
        }

        Map<Integer, Integer> zeilenHoehen = new LinkedHashMap<>();
        for (int r = 0; r < gitter.size(); r++) {
            zeilenHoehen.put(r, 600);
        }

        var model = new TabelleModel(gitter.size(), 9, gitter, zellen, spaltenBreiten, zeilenHoehen,
                0, 0, 0, "", "", "", "", "", "");

        String fragment = TabelleHtmlRenderer.fuerPdf().render(model);
        byte[] png = HtmlZuBildKonvertierer.konvertiere(fragment, "");
        BufferedImage bild = ImageIO.read(new ByteArrayInputStream(png));
        assertThat(bild).isNotNull();

        // Letzte Datenzeile (rot markierte Spalte 8) statt Bildmitte, damit deterministisch eine
        // Zeile mit dem Marker getroffen wird, unabhängig von der Anzahl Kopfzeilen.
        int y = bild.getHeight() - 5;
        Color pixel = new Color(bild.getRGB(bild.getWidth() - 1, y), false);
        assertThat(pixel).as("rechte Bildkante in der letzten Datenzeile sollte im roten "
                + "Zellhintergrund liegen, nicht abgeschnitten sein")
                .isEqualTo(Color.RED);
    }

    private void assertRechteKanteIstRot(BufferedImage bild) {
        int letztePixelSpalte = bild.getWidth() - 1;
        Color pixel = new Color(bild.getRGB(letztePixelSpalte, bild.getHeight() / 2), false);
        assertThat(pixel).as("rechte Bildkante sollte im roten Zellhintergrund liegen, nicht Weißraum "
                + "(zu breite Seite) oder eine andere Farbe durch Abschneiden")
                .isEqualTo(Color.RED);
    }

    private BufferedImage rendereEinzeiligeTabelle(int spaltenAnzahl) throws Exception {
        var stil = new StyleModel(false, false, ROT, null, null, null,
                1, 1, 0, false, null, 0, null, null, null, null);
        var gitterZeile = new ArrayList<String>();
        Map<String, ZelleModel> zellen = new LinkedHashMap<>();
        Map<Integer, Integer> spaltenBreiten = new LinkedHashMap<>();
        for (int c = 0; c < spaltenAnzahl; c++) {
            String id = "cell-0-" + c;
            gitterZeile.add(id);
            zellen.put(id, new ZelleModel(id, "Zelle " + c, stil));
            spaltenBreiten.put(c, 2000);
        }
        var model = new TabelleModel(1, spaltenAnzahl, List.of(gitterZeile), zellen,
                spaltenBreiten, Map.of(0, 600), 0, 0, 0, "", "", "", "", "", "");

        String fragment = TabelleHtmlRenderer.fuerPdf().render(model);
        byte[] png = HtmlZuBildKonvertierer.konvertiere(fragment, "");
        BufferedImage bild = ImageIO.read(new ByteArrayInputStream(png));
        assertThat(bild).isNotNull();
        return bild;
    }
}
