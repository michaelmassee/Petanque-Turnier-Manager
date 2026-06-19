package de.petanqueturniermanager.webserver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CompositeViewDetailDialogTest {

    @Test
    void panelShareInSplitLiestLinkenUndRechtenAnteil() {
        SplitKnoten layout = new SplitTeilung("H", 35, new SplitBlatt(0), new SplitBlatt(1));

        assertThat(CompositeViewDetailDialog.panelShareInSplit(layout, 0, "H")).isEqualTo(35);
        assertThat(CompositeViewDetailDialog.panelShareInSplit(layout, 1, "H")).isEqualTo(65);
        assertThat(CompositeViewDetailDialog.panelShareInSplit(layout, 0, "V")).isEqualTo(-1);
    }

    @Test
    void panelShareInSplitLiestVerschachtelteBreiteUndHoehe() {
        SplitKnoten layout = new SplitTeilung("H", 70,
                new SplitTeilung("V", 40, new SplitBlatt(0), new SplitBlatt(1)),
                new SplitBlatt(2));

        assertThat(CompositeViewDetailDialog.panelShareInSplit(layout, 0, "H")).isEqualTo(70);
        assertThat(CompositeViewDetailDialog.panelShareInSplit(layout, 0, "V")).isEqualTo(40);
        assertThat(CompositeViewDetailDialog.panelShareInSplit(layout, 1, "H")).isEqualTo(70);
        assertThat(CompositeViewDetailDialog.panelShareInSplit(layout, 1, "V")).isEqualTo(60);
        assertThat(CompositeViewDetailDialog.panelShareInSplit(layout, 2, "H")).isEqualTo(30);
        assertThat(CompositeViewDetailDialog.panelShareInSplit(layout, 2, "V")).isEqualTo(-1);
    }

    @Test
    void panelShareInSplitLiestSichtbareBreiteVonDreiHorizontalenPanels() {
        SplitKnoten layout = new SplitTeilung("H", 50,
                new SplitBlatt(0),
                new SplitTeilung("H", 50, new SplitBlatt(1), new SplitBlatt(2)));

        assertThat(CompositeViewDetailDialog.panelShareInSplit(layout, 0, "H")).isEqualTo(50);
        assertThat(CompositeViewDetailDialog.panelShareInSplit(layout, 1, "H")).isEqualTo(25);
        assertThat(CompositeViewDetailDialog.panelShareInSplit(layout, 2, "H")).isEqualTo(25);
    }

    @Test
    void panelShareInSplitLiestSichtbareHoeheVonDreiVertikalenPanels() {
        SplitKnoten layout = new SplitTeilung("V", 40,
                new SplitBlatt(0),
                new SplitTeilung("V", 50, new SplitBlatt(1), new SplitBlatt(2)));

        assertThat(CompositeViewDetailDialog.panelShareInSplit(layout, 0, "V")).isEqualTo(40);
        assertThat(CompositeViewDetailDialog.panelShareInSplit(layout, 1, "V")).isEqualTo(30);
        assertThat(CompositeViewDetailDialog.panelShareInSplit(layout, 2, "V")).isEqualTo(30);
    }

    @Test
    void aktualisierePanelShareInSplitSetztLinkenAnteilDirekt() {
        SplitKnoten aktualisiert = CompositeViewDetailDialog.aktualisierePanelShareInSplit(
                new SplitTeilung("H", 50, new SplitBlatt(0), new SplitBlatt(1)), 0, "H", 30);

        assertThat(aktualisiert)
                .isInstanceOfSatisfying(SplitTeilung.class, teilung -> assertThat(teilung.groesse()).isEqualTo(30));
    }

    @Test
    void aktualisierePanelShareInSplitSetztRechtenAnteilAlsGegenwert() {
        SplitKnoten aktualisiert = CompositeViewDetailDialog.aktualisierePanelShareInSplit(
                new SplitTeilung("H", 50, new SplitBlatt(0), new SplitBlatt(1)), 1, "H", 30);

        assertThat(aktualisiert)
                .isInstanceOfSatisfying(SplitTeilung.class, teilung -> assertThat(teilung.groesse()).isEqualTo(70));
    }

    @Test
    void aktualisierePanelShareInSplitAendertVerschachtelteBreiteUndHoehe() {
        SplitKnoten layout = new SplitTeilung("H", 70,
                new SplitTeilung("V", 40, new SplitBlatt(0), new SplitBlatt(1)),
                new SplitBlatt(2));

        SplitKnoten breiteAktualisiert = CompositeViewDetailDialog.aktualisierePanelShareInSplit(layout, 0, "H", 55);
        assertThat(breiteAktualisiert)
                .isInstanceOfSatisfying(SplitTeilung.class, teilung -> assertThat(teilung.groesse()).isEqualTo(55));

        SplitKnoten hoeheAktualisiert = CompositeViewDetailDialog.aktualisierePanelShareInSplit(layout, 0, "V", 65);
        assertThat(hoeheAktualisiert)
                .isInstanceOfSatisfying(SplitTeilung.class, root ->
                        assertThat(root.links()).isInstanceOfSatisfying(SplitTeilung.class,
                                links -> assertThat(links.groesse()).isEqualTo(65)));
    }

    @Test
    void aktualisierePanelShareInSplitRechnetSichtbareBreiteInLokalenSplitUm() {
        SplitKnoten layout = new SplitTeilung("H", 50,
                new SplitBlatt(0),
                new SplitTeilung("H", 50, new SplitBlatt(1), new SplitBlatt(2)));

        SplitKnoten aktualisiert = CompositeViewDetailDialog.aktualisierePanelShareInSplit(layout, 1, "H", 30);

        assertThat(CompositeViewDetailDialog.panelShareInSplit(aktualisiert, 0, "H")).isEqualTo(50);
        assertThat(CompositeViewDetailDialog.panelShareInSplit(aktualisiert, 1, "H")).isEqualTo(30);
        assertThat(CompositeViewDetailDialog.panelShareInSplit(aktualisiert, 2, "H")).isEqualTo(20);
        assertThat(aktualisiert)
                .isInstanceOfSatisfying(SplitTeilung.class, root ->
                        assertThat(root.rechts()).isInstanceOfSatisfying(SplitTeilung.class,
                                rechts -> assertThat(rechts.groesse()).isEqualTo(60)));
    }

    @Test
    void aktualisierePanelShareInSplitRechnetSichtbareHoeheInLokalenSplitUm() {
        SplitKnoten layout = new SplitTeilung("V", 40,
                new SplitBlatt(0),
                new SplitTeilung("V", 50, new SplitBlatt(1), new SplitBlatt(2)));

        SplitKnoten aktualisiert = CompositeViewDetailDialog.aktualisierePanelShareInSplit(layout, 1, "V", 36);

        assertThat(CompositeViewDetailDialog.panelShareInSplit(aktualisiert, 0, "V")).isEqualTo(40);
        assertThat(CompositeViewDetailDialog.panelShareInSplit(aktualisiert, 1, "V")).isEqualTo(36);
        assertThat(CompositeViewDetailDialog.panelShareInSplit(aktualisiert, 2, "V")).isEqualTo(24);
        assertThat(aktualisiert)
                .isInstanceOfSatisfying(SplitTeilung.class, root ->
                        assertThat(root.rechts()).isInstanceOfSatisfying(SplitTeilung.class,
                                rechts -> assertThat(rechts.groesse()).isEqualTo(60)));
    }

    @Test
    void restShareInSplitNimmtRestAusDreiHorizontalenPanels() {
        SplitKnoten layout = new SplitTeilung("H", 50,
                new SplitBlatt(0),
                new SplitTeilung("H", 60, new SplitBlatt(1), new SplitBlatt(2)));

        assertThat(CompositeViewDetailDialog.restShareInSplit(layout, 2, "H")).isEqualTo(20);
    }

    @Test
    void restShareInSplitNimmtRestAusDreiVertikalenPanels() {
        SplitKnoten layout = new SplitTeilung("V", 40,
                new SplitBlatt(0),
                new SplitTeilung("V", 60, new SplitBlatt(1), new SplitBlatt(2)));

        assertThat(CompositeViewDetailDialog.restShareInSplit(layout, 2, "V")).isEqualTo(24);
    }

    @Test
    void aktualisierePanelShareInSplitValidiertProzentbereich() {
        SplitKnoten layout = new SplitTeilung("H", 50, new SplitBlatt(0), new SplitBlatt(1));

        assertThatThrownBy(() -> CompositeViewDetailDialog.aktualisierePanelShareInSplit(layout, 0, "H", 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CompositeViewDetailDialog.aktualisierePanelShareInSplit(layout, 0, "H", 100))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
