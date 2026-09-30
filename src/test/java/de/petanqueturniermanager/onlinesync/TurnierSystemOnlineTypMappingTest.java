/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.onlinesync;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

class TurnierSystemOnlineTypMappingTest {

    private static OnlineTournamentDto online(String type, String registrationType) {
        OnlineTournamentDto dto = new OnlineTournamentDto();
        dto.type = type;
        dto.registrationType = registrationType;
        return dto;
    }

    @Test
    void meleeAnmeldelistePasstNurZuMeleeAnmeldung() {
        assertThat(TurnierSystemOnlineTypMapping.passtZu(TurnierSystem.SCHWEIZER, true, online("schweizer", "melee")))
                .isTrue();
        assertThat(TurnierSystemOnlineTypMapping.passtZu(TurnierSystem.SCHWEIZER, true, online("schweizer", "forme")))
                .isFalse();
    }

    @Test
    void teamMeldelistePasstNurZuTeamAnmeldung() {
        assertThat(TurnierSystemOnlineTypMapping.passtZu(TurnierSystem.SCHWEIZER, false, online("schweizer", "forme")))
                .isTrue();
        assertThat(TurnierSystemOnlineTypMapping.passtZu(TurnierSystem.SCHWEIZER, false, online("schweizer", "melee")))
                .isFalse();
    }

    @Test
    void supermeleeVerlangtImmerSupermeleeAnmeldung() {
        assertThat(TurnierSystemOnlineTypMapping.passtZu(TurnierSystem.SUPERMELEE, false,
                online("rangliste", "supermelee"))).isTrue();
        assertThat(TurnierSystemOnlineTypMapping.passtZu(TurnierSystem.SUPERMELEE, false,
                online("rangliste", "forme"))).isFalse();
    }

    @Test
    void falscherTurniertypPasstNie() {
        assertThat(TurnierSystemOnlineTypMapping.passtZu(TurnierSystem.SCHWEIZER, false, online("ko", "forme")))
                .isFalse();
    }
}
