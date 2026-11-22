/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Die drei Teamname-Modi, im Dokument wie in PTM-Online. */
class TeamnameModusTest {

    @Test
    void ausDokument() {
        assertThat(TeamnameModus.von(Formation.DOUBLETTE, false)).isEqualTo(TeamnameModus.KEIN);
        assertThat(TeamnameModus.von(Formation.DOUBLETTE, true)).isEqualTo(TeamnameModus.ZUSATZ);
        assertThat(TeamnameModus.von(Formation.NUR_TEAMNAME, false)).isEqualTo(TeamnameModus.NUR_TEAMNAME);
    }

    @Test
    void ausOnline() {
        assertThat(TeamnameModus.ausOnline(false, false)).isEqualTo(TeamnameModus.KEIN);
        assertThat(TeamnameModus.ausOnline(false, true)).isEqualTo(TeamnameModus.ZUSATZ);
        assertThat(TeamnameModus.ausOnline(true, false)).isEqualTo(TeamnameModus.NUR_TEAMNAME);
    }

    @Test
    void teamnameSpalteUndOnlineSchalterStimmenUeberein() {
        assertThat(TeamnameModus.KEIN.teamnameSpalte()).isFalse();
        assertThat(TeamnameModus.ZUSATZ.teamnameSpalte()).isTrue();
        assertThat(TeamnameModus.NUR_TEAMNAME.teamnameSpalte()).isTrue();
    }
}
