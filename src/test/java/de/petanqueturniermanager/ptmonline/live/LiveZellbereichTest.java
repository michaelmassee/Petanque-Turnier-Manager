/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.live;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LiveZellbereichTest {

    @Test
    void liestTeamNrAusTeamnamenFormel() {
        String formel = "=PTM.ALG.FORMATTEAMANZEIGE(1;2;0;INDEX($'Meldeliste'.$A$1:$Z$999;"
                + "MATCH(17;$'Meldeliste'.$A$1:$A$999;0);0))";

        assertThat(LiveZellbereich.nrAusFormel(formel)).isEqualTo(17);
    }

    @Test
    void wertOderFremdeFormelLiefertKeineNummer() {
        assertThat(LiveZellbereich.nrAusFormel("17")).isZero();
        assertThat(LiveZellbereich.nrAusFormel("=IF($D$3>$D$4;$C$3;$C$4)")).isZero();
        assertThat(LiveZellbereich.nrAusFormel(null)).isZero();
    }
}
