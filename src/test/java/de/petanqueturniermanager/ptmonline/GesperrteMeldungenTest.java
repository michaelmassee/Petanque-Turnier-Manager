/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;

class GesperrteMeldungenTest {

    private static TeamMeldungen teams(int... nummern) {
        TeamMeldungen meldungen = new TeamMeldungen();
        for (int nr : nummern) {
            meldungen.addTeamWennNichtVorhanden(Team.from(nr));
        }
        return meldungen;
    }

    @Test
    void gesperrteTeamsFehlenInDerAuslosungUndWerdenBenannt() {
        List<String> entfernt = new ArrayList<>();

        TeamMeldungen auslosbar = PtmOnlineSpielrundeSync.ohne(teams(3, 1, 7, 5),
                Map.of(7, "Konto doppelt: Berg", 1, "Team unvollständig: Adler"), entfernt);

        assertThat(auslosbar.teams()).extracting(Team::getNr).as("Reihenfolge bleibt").containsExactly(3, 5);
        assertThat(entfernt).containsExactlyInAnyOrder("Konto doppelt: Berg", "Team unvollständig: Adler");
    }

    @Test
    void ohneGesperrteBleibtDieSammlungUnveraendert() {
        TeamMeldungen meldungen = teams(1, 2);
        List<String> entfernt = new ArrayList<>();

        assertThat(PtmOnlineSpielrundeSync.ohne(meldungen, Map.of(9, "x"), entfernt)).isSameAs(meldungen);
        assertThat(entfernt).isEmpty();
    }
}
