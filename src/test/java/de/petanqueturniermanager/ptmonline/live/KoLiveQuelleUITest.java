/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.live;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.ko.KoTurnierTestDaten;

/**
 * Live-Stand aus einem KO-Turnierbaum mit Cadrage: die Cadrage ist die erste Runde, danach folgen die Runden des
 * Baums bis zum Finale, jeweils mit den weitergereichten Siegern. Die Testdaten spielen das Turnier komplett durch
 * und vergeben Bahnen (bei Cadrage nur für die Cadrage).
 */
class KoLiveQuelleUITest extends BaseCalcUITest {

    @BeforeEach
    @Override
    public void beforeTest() {
        super.beforeTest();
        RandomSource.setSeed(42L);
    }

    @AfterEach
    void resetRandom() {
        RandomSource.reset();
    }

    @Test
    void cadrageUndAlleBaumrundenMitErgebnissenUndBahnen() throws Exception {
        new KoTurnierTestDaten(wkingSpreadsheet, 10).generate();

        List<LiveRunde> runden = LiveStandQuellen.fuer(wkingSpreadsheet, TurnierSystem.KO, null).orElseThrow().lese()
                .runden();

        assertThat(runden).extracting(runde -> runde.partien().size()).containsExactly(2, 4, 2, 1);
        assertThat(runden).extracting(runde -> runde.partien().get(0).stufe()).containsExactly(
                I18n.get("column.header.cadrage"), I18n.get("ko.runde.titel.ntel.finale", 4),
                I18n.get("ko.runde.titel.halbfinale"), I18n.get("ko.runde.titel.finale"));
        assertThat(runden.get(0).partien()).allSatisfy(
                partie -> assertThat(partie.bahn()).as("Bahn der Cadrage").matches("\\d+"));
        for (LiveRunde runde : runden) {
            assertThat(runde.partien()).allSatisfy(partie -> {
                assertThat(partie.punkteA()).isNotNull();
                assertThat(partie.punkteB()).isNotNull();
            });
            List<Integer> teams = runde.partien().stream()
                    .flatMap(partie -> Stream.concat(partie.teamA().stream(), partie.teamB().stream())).toList();
            assertThat(teams).as("Runde %d: jedes Team höchstens einmal", runde.nr()).doesNotHaveDuplicates()
                    .allSatisfy(nr -> assertThat(nr).isBetween(1, 10));
        }
        assertSiegerSpielenWeiter(runden);
    }

    /** Die Teams jeder Runde sind die Sieger der Partien davor (bzw. in Runde 1 der Baum-Einstieg). */
    private static void assertSiegerSpielenWeiter(List<LiveRunde> runden) {
        for (int i = 2; i < runden.size(); i++) {
            List<Integer> sieger = runden.get(i - 1).partien().stream()
                    .map(partie -> partie.punkteA() > partie.punkteB() ? partie.teamA().get(0) : partie.teamB().get(0))
                    .toList();
            List<Integer> teams = runden.get(i).partien().stream()
                    .flatMap(partie -> Stream.concat(partie.teamA().stream(), partie.teamB().stream())).toList();
            assertThat(teams).as("Runde %d", runden.get(i).nr()).containsExactlyInAnyOrderElementsOf(sieger);
        }
    }
}
