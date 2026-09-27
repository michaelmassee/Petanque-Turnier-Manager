/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.ptmonline.dto.LiveMatchDto;
import de.petanqueturniermanager.ptmonline.dto.LiveRankingEntryDto;
import de.petanqueturniermanager.ptmonline.live.LivePartie;
import de.petanqueturniermanager.ptmonline.live.LiveRanglistenEintrag;
import de.petanqueturniermanager.ptmonline.live.LiveRunde;

class LiveUebertragungsDatenTest {

    private static final Map<Integer, List<String>> ONLINE_IDS = Map.of(
            1, List.of("r1"), 2, List.of("r2"), 3, List.of("r3"), 4, List.of("r4"),
            7, List.of("m1", "m2"), 8, List.of("m3", "m4"));

    @Test
    void uebertraegtPartieMitErgebnisBahnUndStufe() {
        List<LiveMatchDto> matches = LiveUebertragungsDaten.matches(
                new LiveRunde(1, List.of(LivePartie.teams(1, 2, 13, 7, " 5 ", "Halbfinale"))), ONLINE_IDS);

        assertThat(matches).containsExactly(new LiveMatchDto(List.of("r1"), List.of("r2"), 13, 7, "5", "Halbfinale"));
    }

    @Test
    void freilosHatLeeresTeamB() {
        List<LiveMatchDto> matches = LiveUebertragungsDaten.matches(
                new LiveRunde(1, List.of(LivePartie.freilos(3, null, null))), ONLINE_IDS);

        assertThat(matches).containsExactly(new LiveMatchDto(List.of("r3"), List.of(), null, null, null, null));
    }

    @Test
    void meleeTeamWirdAufAlleSpielerIdsAbgebildet() {
        List<LiveMatchDto> matches = LiveUebertragungsDaten.matches(
                new LiveRunde(1, List.of(LivePartie.teams(7, 8, null, null, null, null))), ONLINE_IDS);

        assertThat(matches).singleElement().satisfies(match -> {
            assertThat(match.teamA()).containsExactly("m1", "m2");
            assertThat(match.teamB()).containsExactly("m3", "m4");
        });
    }

    @Test
    void partieMitNichtZugeordneterSeiteEntfaelltStattAlsFreilosZuErscheinen() {
        List<LiveMatchDto> matches = LiveUebertragungsDaten.matches(new LiveRunde(1, List.of(
                LivePartie.teams(1, 99, null, null, null, null), LivePartie.teams(2, 3, null, null, null, null))),
                ONLINE_IDS);

        assertThat(matches).extracting(LiveMatchDto::teamA).containsExactly(List.of("r2"));
    }

    @Test
    void supermeleeTeamMitNichtZugeordnetemSpielerEntfaellt() {
        LivePartie partie = new LivePartie(List.of(1, 99), List.of(2, 3), null, null, null, null);

        assertThat(LiveUebertragungsDaten.matches(new LiveRunde(1, List.of(partie)), ONLINE_IDS)).isEmpty();
    }

    @Test
    void mehrfachEingeteilteMeldungWirdNurEinmalUebertragen() {
        List<LiveMatchDto> matches = LiveUebertragungsDaten.matches(new LiveRunde(1, List.of(
                LivePartie.teams(1, 2, null, null, null, null), LivePartie.teams(2, 3, null, null, null, null))),
                ONLINE_IDS);

        assertThat(matches).hasSize(1);
    }

    @Test
    void ergebnisAusserhalbNullBisDreizehnGiltAlsLaufend() {
        List<LiveMatchDto> matches = LiveUebertragungsDaten.matches(
                new LiveRunde(1, List.of(LivePartie.teams(1, 2, 126, 0, null, null))), ONLINE_IDS);

        assertThat(matches).singleElement().satisfies(match -> {
            assertThat(match.scoreA()).isNull();
            assertThat(match.scoreB()).isNull();
        });
    }

    @Test
    void halbesErgebnisGiltAlsLaufend() {
        List<LiveMatchDto> matches = LiveUebertragungsDaten.matches(
                new LiveRunde(1, List.of(LivePartie.teams(1, 2, 13, null, null, null))), ONLINE_IDS);

        assertThat(matches).singleElement().extracting(LiveMatchDto::scoreA).isNull();
    }

    @Test
    void bahnWirdAufVierzigZeichenGekuerzt() {
        String bahn = "B".repeat(50);
        List<LiveMatchDto> matches = LiveUebertragungsDaten.matches(
                new LiveRunde(1, List.of(LivePartie.teams(1, 2, null, null, bahn, null))), ONLINE_IDS);

        assertThat(matches).singleElement().extracting(LiveMatchDto::court)
                .isEqualTo("B".repeat(LiveUebertragungsDaten.MAX_BAHN_LAENGE));
    }

    @Test
    void ranglisteLaesstNichtZugeordneteAusUndVerwirftNegativeWerte() {
        List<LiveRankingEntryDto> rangliste = LiveUebertragungsDaten.rangliste(List.of(
                LiveRanglistenEintrag.team(1, 1, 3, 39, 20), LiveRanglistenEintrag.team(2, 99, 2, 30, 25),
                LiveRanglistenEintrag.team(2, 2, 2, -1, 25)), ONLINE_IDS);

        assertThat(rangliste).containsExactly(new LiveRankingEntryDto(1, List.of("r1"), 3, 39, 20),
                new LiveRankingEntryDto(2, List.of("r2"), 2, null, 25));
    }
}
