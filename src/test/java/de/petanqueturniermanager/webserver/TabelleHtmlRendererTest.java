/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.webserver;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class TabelleHtmlRendererTest {

    private final TabelleHtmlRenderer renderer = new TabelleHtmlRenderer();

    @Test
    void leeresTabelleModelRendertValideTabelle() {
        var model = modellMit(List.of());
        var html = renderer.render(model);
        assertThat(html).startsWith("<table>").endsWith("</table>");
        assertThat(html).contains("<colgroup>").contains("</colgroup>");
        assertThat(html).contains("<tbody>").contains("</tbody>");
    }

    @Test
    void mergeSlaveOhneId_wirdUebersprungen() {
        // Gitter: 1 Zeile, 2 Spalten — zweite Spalte ist Merge-Slave (null)
        var zelle = zelleModel("cell-0-0", "A", standardStil());
        var gitter = List.of(Arrays.asList("cell-0-0", null));
        var model = modellMit2Spalten(gitter, Map.of("cell-0-0", zelle));
        var html = renderer.render(model);
        // Genau ein <td>
        assertThat(html).containsOnlyOnce("<td");
        assertThat(html).doesNotContain("<td></td>"); // kein leeres td für Slave
        assertThat(html).contains(">A<");
    }

    @Test
    void idInGitterAberNichtInZellenMap_RendertLeeresTd() {
        var gitter = List.of(List.of("cell-0-0"));
        var model = modellMit2Spalten(gitter, Collections.emptyMap());
        var html = renderer.render(model);
        assertThat(html).contains("<td></td>");
    }

    @Test
    void spalteBreiteNull_wirdAusgeblendet() {
        // Spalte 0: normal, Spalte 1: Breite 0 → versteckt
        var zelle0 = zelleModel("cell-0-0", "X", standardStil());
        var zelle1 = zelleModel("cell-0-1", "Y", standardStil());
        var gitter = List.of(List.of("cell-0-0", "cell-0-1"));
        var zellen = Map.of("cell-0-0", zelle0, "cell-0-1", zelle1);
        var model = new TabelleModel(
                1, 2, gitter, zellen,
                Map.of(0, 2000, 1, 0), // Spalte 1 versteckt
                Map.of(0, 600),
                0, 0, 0, "", "", "", "", "", "");
        var html = renderer.render(model);
        assertThat(html).contains("<col style=\"display:none\" />");
        assertThat(html).contains(">X<");
        assertThat(html).doesNotContain(">Y<"); // Y in versteckter Spalte
    }

    @Test
    void fettTrue_RendertFontWeightBold() {
        var stit = new StyleModel(true, false, null, null, null, null,
                1, 1, 0, false, null, 0, null, null, null, null);
        var model = einspaltigesModell("cell-0-0", "Text", stit);
        var html = renderer.render(model);
        assertThat(html).contains("font-weight:bold");
    }

    @Test
    void rotation90Grad_RendertWritingModeVerticalRl() {
        var stil = new StyleModel(false, false, null, null, null, null,
                1, 1, 90, false, null, 0, null, null, null, null);
        var model = einspaltigesModell("cell-0-0", "Kopf", stil);
        var html = renderer.render(model);
        assertThat(html).contains("writing-mode:vertical-rl");
    }

    @Test
    void rotation270Grad_RendertWritingModeVerticalRl() {
        var stil = new StyleModel(false, false, null, null, null, null,
                1, 1, 270, false, null, 0, null, null, null, null);
        var model = einspaltigesModell("cell-0-0", "Kopf", stil);
        var html = renderer.render(model);
        assertThat(html).contains("writing-mode:vertical-rl");
    }

    @Test
    void pdfRotation90Grad_RendertTransformStattWritingMode() {
        var stil = new StyleModel(false, false, null, null, null, null,
                1, 1, 90, false, null, 0, null, null, null, null);
        var model = einspaltigesModell("cell-0-0", "Kopf", stil);
        var html = TabelleHtmlRenderer.fuerPdf().render(model);
        assertThat(html)
                .doesNotContain("writing-mode")
                .contains("<span style=\"display:inline-block;white-space:nowrap;transform:rotate(90deg);")
                .contains(">Kopf</span>");
    }

    @Test
    void pdfRotation270Grad_RendertTransformStattWritingMode() {
        var stil = new StyleModel(false, false, null, null, null, null,
                1, 1, 270, false, null, 0, null, null, null, null);
        var model = einspaltigesModell("cell-0-0", "Kopf", stil);
        var html = TabelleHtmlRenderer.fuerPdf().render(model);
        assertThat(html)
                .doesNotContain("writing-mode")
                .contains("<span style=\"display:inline-block;white-space:nowrap;transform:rotate(270deg);")
                .contains(">Kopf</span>");
    }

    @Test
    void rotation45Grad_RendertTransformRotate() {
        var stil = new StyleModel(false, false, null, null, null, null,
                1, 1, 45, false, null, 0, null, null, null, null);
        var model = einspaltigesModell("cell-0-0", "Kopf", stil);
        var html = renderer.render(model);
        assertThat(html).contains("transform:rotate(45deg)");
        assertThat(html).doesNotContain("writing-mode");
    }

    @Test
    void scriptInjectionInZellwert_wirdEscaped() {
        var model = einspaltigesModell("cell-0-0", "<script>alert(1)</script>", standardStil());
        var html = renderer.render(model);
        assertThat(html).doesNotContain("<script>");
        assertThat(html).contains("&lt;script&gt;");
    }

    @Test
    void kopfZeilenAnzahl_ErzeugtThead() {
        var zelle0 = zelleModel("cell-0-0", "Kopf", standardStil());
        var zelle1 = zelleModel("cell-1-0", "Daten", standardStil());
        var gitter = List.of(List.of("cell-0-0"), List.of("cell-1-0"));
        var model = new TabelleModel(
                2, 1, gitter, Map.of("cell-0-0", zelle0, "cell-1-0", zelle1),
                Map.of(0, 2000), Map.of(0, 600, 1, 600),
                0, 0, 1, "", "", "", "", "", "");
        var html = renderer.render(model);
        assertThat(html).contains("<thead>");
        assertThat(html).contains("</thead>");
        // Kopfzeile in thead vor tbody
        assertThat(html.indexOf("<thead>")).isLessThan(html.indexOf("<tbody>"));
    }

    @Test
    void zeilenumbruchFalse_RendertNowrap() {
        var model = einspaltigesModell("cell-0-0", "Text", standardStil());
        var html = renderer.render(model);
        assertThat(html).contains("white-space:nowrap");
    }

    @Test
    void zeilenumbruchTrue_RendertNormal() {
        var stil = new StyleModel(false, false, null, null, null, null,
                1, 1, 0, true, null, 0, null, null, null, null);
        var model = einspaltigesModell("cell-0-0", "Text", stil);
        var html = renderer.render(model);
        assertThat(html).contains("white-space:normal");
    }

    // --- Hilfsmethoden ---

    private TabelleModel einspaltigesModell(String id, String wert, StyleModel stil) {
        var zelle = zelleModel(id, wert, stil);
        var gitter = List.of(List.of(id));
        return new TabelleModel(1, 1, gitter, Map.of(id, zelle),
                Map.of(0, 2000), Map.of(0, 600),
                0, 0, 0, "", "", "", "", "", "");
    }

    private TabelleModel modellMit(List<List<String>> gitter) {
        return new TabelleModel(gitter.size(), 0, gitter, Collections.emptyMap(),
                Collections.emptyMap(), Collections.emptyMap(),
                0, 0, 0, "", "", "", "", "", "");
    }

    private TabelleModel modellMit2Spalten(List<List<String>> gitter, Map<String, ZelleModel> zellen) {
        return new TabelleModel(gitter.size(), 2, gitter, zellen,
                Map.of(0, 2000, 1, 2000), Map.of(0, 600),
                0, 0, 0, "", "", "", "", "", "");
    }

    private ZelleModel zelleModel(String id, String wert, StyleModel stil) {
        return new ZelleModel(id, wert, stil);
    }

    private StyleModel standardStil() {
        return new StyleModel(false, false, null, null, null, null,
                1, 1, 0, false, null, 0, null, null, null, null);
    }
}
