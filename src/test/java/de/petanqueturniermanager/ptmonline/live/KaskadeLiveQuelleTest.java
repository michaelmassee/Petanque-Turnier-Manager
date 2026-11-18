/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.live;

import static de.petanqueturniermanager.kaskade.spielrunde.KaskadeSpielrundeSheet.ERG_TEAM_A_SPALTE;
import static de.petanqueturniermanager.kaskade.spielrunde.KaskadeSpielrundeSheet.ERG_TEAM_B_SPALTE;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.basesheet.meldeliste.MeldelisteUuidIndex;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;

class KaskadeLiveQuelleTest {

    private static final int ERSTE_ZEILE = 2;

    @Test
    void partienDerVorrundeTragenIhrenErgebnisOrtFreiloseNicht() {
        RangeData werte = new RangeData(new Object[][] { //
                { "A", 1.0, 1.0, 2.0, 13.0, 7.0 }, //
                { "", 2.0, 3.0, "", "", "" } });
        String[][] formeln = { { "A", "1", "1", "2", "13", "7" }, { "", "2", "3", "", "", "" } };
        LiveZellbereich zellen = LiveZellbereich.aus(0, ERSTE_ZEILE, werte, formeln,
                MeldelisteUuidIndex.aus(new String[0][0], new int[0]));

        List<LivePartie> partien = KaskadeLiveQuelle.lesePartien(zellen,
                zeile -> ErgebnisOrt.inZeile("schluessel", "Kaskade 1", zeile, ERG_TEAM_A_SPALTE));

        assertThat(partien).hasSize(2);
        assertThat(partien.getFirst().ergebnisOrt()).isNotNull().satisfies(ort -> {
            assertThat(ort.blattName()).isEqualTo("Kaskade 1");
            assertThat(ort.punkteA().getSpalte()).isEqualTo(ERG_TEAM_A_SPALTE);
            assertThat(ort.punkteB().getSpalte()).isEqualTo(ERG_TEAM_B_SPALTE);
            assertThat(ort.punkteA().getZeile()).isEqualTo(ERSTE_ZEILE);
        });
        assertThat(partien.getLast().istFreilos()).isTrue();
        assertThat(partien.getLast().ergebnisOrt()).isNull();
    }
}
