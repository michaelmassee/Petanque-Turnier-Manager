/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.live;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.basesheet.meldeliste.MeldelisteUuidIndex;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;

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

    @Test
    void numerischerTeamnameZaehltNichtAlsTeamNr() {
        String uuidTeam1 = "11111111-1111-4111-8111-111111111111";
        String uuidTeam2 = "22222222-2222-4222-8222-222222222222";
        MeldelisteUuidIndex index = MeldelisteUuidIndex.aus(
                new String[][] { { "Nr" }, { "=PTM.ALG.TNRUUID(\"" + uuidTeam1 + "\")" },
                        { "=PTM.ALG.TNRUUID(\"" + uuidTeam2 + "\")" } },
                new int[] { 0, 1, 2 });
        String teamnameFormel = "=PTM.ALG.FORMATTEAMANZEIGE(PTM.ALG.UUIDZEILE(\"" + uuidTeam1 + "\";1))";
        LiveZellbereich zellen = LiveZellbereich.aus(0, 0, new RangeData(new Object[][] { { "2", 2.0 } }),
                new String[][] { { teamnameFormel, "2" } }, index);

        assertThat(zellen.teamNr(0, 0)).as("Teamname „2“ von Team 1").isEqualTo(1);
        assertThat(zellen.teamNr(1, 0)).as("angezeigte Nummer ohne Formel").isEqualTo(2);
    }
}
