/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.live;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class RundenBloeckeTest {

    @Test
    void vierTeamsSpielenZweiPartienJeRunde() {
        List<LivePartie> partien = List.of(partie(1, 2), partie(3, 4), partie(1, 3), partie(2, 4), partie(1, 4),
                partie(2, 3));

        assertThat(RundenBloecke.inRundenTeilen(partien)).extracting(List::size).containsExactly(2, 2, 2);
    }

    @Test
    void ungeradeTeamzahlZaehltDasFreilosAlsPartie() {
        List<LivePartie> partien = List.of(partie(1, 2), LivePartie.freilos(3, null, null), partie(1, 3),
                LivePartie.freilos(2, null, null));

        assertThat(RundenBloecke.inRundenTeilen(partien)).extracting(List::size).containsExactly(2, 2);
    }

    private static LivePartie partie(int a, int b) {
        return LivePartie.teams(a, b, null, null, null, null);
    }
}
