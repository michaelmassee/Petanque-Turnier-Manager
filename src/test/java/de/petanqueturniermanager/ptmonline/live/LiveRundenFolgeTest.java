/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.live;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class LiveRundenFolgeTest {

    private static final LivePartie A = LivePartie.teams(1, 2, null, null, null, null);
    private static final LivePartie B = LivePartie.teams(3, 4, null, null, null, null);
    private static final LivePartie C = LivePartie.teams(5, 6, null, null, null, null);

    @Test
    void phasenFolgenAufeinanderUndParalleleGruppenTeilenSichDieRunde() {
        List<LiveRunde> runden = new LiveRundenFolge().anhaengen(List.of(List.of(A)))
                .parallelAnhaengen(List.of(List.of(List.of(B)), List.of(List.of(C), List.of(A)))).runden();

        assertThat(runden).containsExactly(new LiveRunde(1, List.of(A)), new LiveRunde(2, List.of(B, C)),
                new LiveRunde(3, List.of(A)));
    }

    @Test
    void leereRundenAmEndeEntfallen() {
        List<LiveRunde> runden = new LiveRundenFolge().anhaengen(List.of(List.of(A), List.of(), List.of()))
                .runden();

        assertThat(runden).containsExactly(new LiveRunde(1, List.of(A)));
    }
}
