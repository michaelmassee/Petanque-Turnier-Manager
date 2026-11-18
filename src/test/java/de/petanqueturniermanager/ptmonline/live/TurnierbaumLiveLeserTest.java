/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.live;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.helper.position.Position;

class TurnierbaumLiveLeserTest {

    @Test
    void liestScorePositionenInGespeicherterReihenfolge() {
        assertThat(TurnierbaumLiveLeser.scorePositionen("PTM_EDIT:3,2|3,4| 7 , 3 |x,1|9"))
                .containsExactly(Position.from(3, 2), Position.from(3, 4), Position.from(7, 3));
    }

    @Test
    void bahnNurInRunde1HatNurDieErsteErgebnisSpalteEineBahn() {
        var bahnSpalten = new TurnierbaumLiveLeser.BahnSpalten(true, true);
        assertThat(bahnSpalten.inSpalte(0)).isTrue();
        assertThat(bahnSpalten.inSpalte(1)).isFalse();
        assertThat(bahnSpalten.inSpalte(2)).isFalse();
    }

    @Test
    void bahnInAllenRundenHatJedeErgebnisSpalteEineBahn() {
        var bahnSpalten = new TurnierbaumLiveLeser.BahnSpalten(true, false);
        assertThat(bahnSpalten.inSpalte(0)).isTrue();
        assertThat(bahnSpalten.inSpalte(3)).isTrue();
    }

    @Test
    void ohneSpielbahnHatKeineErgebnisSpalteEineBahn() {
        var bahnSpalten = new TurnierbaumLiveLeser.BahnSpalten(false, false);
        assertThat(bahnSpalten.inSpalte(0)).isFalse();
        assertThat(bahnSpalten.inSpalte(1)).isFalse();
    }

    @Test
    void siegerDesFeldsIstDieSeiteMitMehrPunktenImFinale() {
        assertThat(feld(LivePartie.teams(3, 7, 9, 13, null, null)).sieger())
                .contains(new LiveKoSieger("A-Finale", List.of(7)));
        assertThat(feld(LivePartie.teams(3, 7, 13, 2, null, null)).sieger())
                .contains(new LiveKoSieger("A-Finale", List.of(3)));
    }

    @Test
    void ohneEntschiedenesFinaleKeinSieger() {
        assertThat(feld(null).sieger()).isEmpty();
        assertThat(feld(LivePartie.teams(3, 7, 13, null, null, null)).sieger()).isEmpty();
        assertThat(feld(LivePartie.teams(3, 7, 10, 10, null, null)).sieger()).isEmpty();
    }

    private static TurnierbaumLiveLeser.Feld feld(LivePartie finale) {
        return new TurnierbaumLiveLeser.Feld("A-Finale", List.of(), finale);
    }

    @Test
    void ohneScoreDatenKeinePositionen() {
        assertThat(TurnierbaumLiveLeser.scorePositionen(null)).isEmpty();
        assertThat(TurnierbaumLiveLeser.scorePositionen("etwas anderes")).isEmpty();
    }
}
