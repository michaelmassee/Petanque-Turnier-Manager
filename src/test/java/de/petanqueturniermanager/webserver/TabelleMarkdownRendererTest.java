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

class TabelleMarkdownRendererTest {

    private final TabelleMarkdownRenderer renderer = new TabelleMarkdownRenderer();

    @Test
    void leeresTabelleModelRendertLeerenString() {
        var model = modellMit(List.of());
        assertThat(renderer.render(model)).isEmpty();
    }

    @Test
    void ersteZeileWirdHeaderMitTrennzeile() {
        var kopf = zelleModel("cell-0-0", "Name", standardStil());
        var daten = zelleModel("cell-1-0", "Anna", standardStil());
        var gitter = List.of(List.of("cell-0-0"), List.of("cell-1-0"));
        var model = new TabelleModel(2, 1, gitter, Map.of("cell-0-0", kopf, "cell-1-0", daten),
                Map.of(0, 2000), Map.of(0, 600, 1, 600),
                0, 0, 1, "", "", "", "", "", "");

        String md = renderer.render(model);
        String[] zeilen = md.split("\n");
        assertThat(zeilen[0]).isEqualTo("| Name |");
        assertThat(zeilen[1]).isEqualTo("| --- |");
        assertThat(zeilen[2]).isEqualTo("| Anna |");
    }

    @Test
    void mergeSlaveOhneId_wirdAlsLeereZelleGerendert() {
        var zelle = zelleModel("cell-0-0", "A", standardStil());
        var gitter = List.of(Arrays.asList("cell-0-0", null));
        var model = modellMit2Spalten(gitter, Map.of("cell-0-0", zelle));
        assertThat(renderer.render(model)).contains("| A | |");
    }

    @Test
    void spalteBreiteNull_wirdAusgeblendet() {
        var zelle0 = zelleModel("cell-0-0", "X", standardStil());
        var zelle1 = zelleModel("cell-0-1", "Y", standardStil());
        var gitter = List.of(List.of("cell-0-0", "cell-0-1"));
        var zellen = Map.of("cell-0-0", zelle0, "cell-0-1", zelle1);
        var model = new TabelleModel(1, 2, gitter, zellen,
                Map.of(0, 2000, 1, 0), Map.of(0, 600),
                0, 0, 0, "", "", "", "", "", "");
        String md = renderer.render(model);
        assertThat(md).contains("X").doesNotContain("Y");
    }

    @Test
    void pipeZeichenImWertWirdEscaped() {
        var model = einspaltigesModell("cell-0-0", "a|b");
        assertThat(renderer.render(model)).contains("a\\|b");
    }

    private TabelleModel einspaltigesModell(String id, String wert) {
        var zelle = zelleModel(id, wert, standardStil());
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
