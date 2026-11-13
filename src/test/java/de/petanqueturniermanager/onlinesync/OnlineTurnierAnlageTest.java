/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.onlinesync;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.MeleeAnmeldungModus;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

/** Ein leeres Dokument legt aus dem Online-Turnier ein Dokument an, das sich danach damit verbinden lässt. */
class OnlineTurnierAnlageTest {

    static Stream<Arguments> abbildbareTurniere() {
        return Stream.of(
                Arguments.of("schweizer", "forme", "doublette", false, TurnierSystem.SCHWEIZER, Formation.DOUBLETTE),
                Arguments.of("maastrichter", "forme", "triplette", false, TurnierSystem.MAASTRICHTER,
                        Formation.TRIPLETTE),
                Arguments.of("jeder_gegen_jeden", "forme", "tete", false, TurnierSystem.JGJ, Formation.TETE),
                Arguments.of("ko", "forme", "tete", true, TurnierSystem.KO, Formation.NUR_TEAMNAME),
                Arguments.of("poule_ab", "melee", "triplette", false, TurnierSystem.POULE, Formation.TRIPLETTE),
                Arguments.of("kaskaden", "forme", "doublette", false, TurnierSystem.KASKADE, Formation.DOUBLETTE),
                Arguments.of("formule_x", "melee", "doublette", false, TurnierSystem.FORMULEX, Formation.DOUBLETTE),
                Arguments.of("trip_tete", "forme", "triplette", false, TurnierSystem.TRIPTETE, Formation.TRIPLETTE),
                Arguments.of("rangliste", "supermelee", "triplette", false, TurnierSystem.SUPERMELEE,
                        Formation.TRIPLETTE));
    }

    @ParameterizedTest
    @MethodSource("abbildbareTurniere")
    void angelegtesDokumentIstMitDemTurnierVerbindbar(String typ, String anmeldeart, String formation,
            boolean formationAndere, TurnierSystem erwartetesSystem, Formation erwarteteFormation) {
        OnlineTournamentDto turnier = online(typ, anmeldeart, formation);
        turnier.formationOther = formationAndere;

        OnlineTurnierAnlage anlage = OnlineTurnierAnlage.aus(turnier).orElseThrow();

        assertThat(anlage.turnierSystem()).isEqualTo(erwartetesSystem);
        assertThat(anlage.formation()).contains(erwarteteFormation);
        assertThat(anlage.verbindbarkeit().bewerte(turnier).istVerbindbar()).isTrue();
    }

    @Test
    void meleeAnmeldungWirdMeleeAnmeldelisteMitFesterTeamgroesse() {
        OnlineTurnierAnlage anlage = OnlineTurnierAnlage.aus(online("schweizer", "melee", "doublette")).orElseThrow();

        assertThat(anlage.meleeAnmeldung()).contains(MeleeAnmeldungModus.EINFACH);
    }

    @Test
    void ligaHatKeineFormationUndNimmtDenTurniernamen() {
        OnlineTournamentDto turnier = online("liga", "forme", "triplette");
        turnier.name = "Bezirksliga";

        OnlineTurnierAnlage anlage = OnlineTurnierAnlage.aus(turnier).orElseThrow();

        assertThat(anlage.formation()).isEmpty();
        assertThat(anlage.name()).isEqualTo("Bezirksliga");
        assertThat(anlage.verbindbarkeit().bewerte(turnier).istVerbindbar()).isTrue();
    }

    @Test
    void teamnameIstOhneSpielernamenPflicht() {
        OnlineTournamentDto turnier = online("ko", "forme", "tete");
        turnier.formationOther = true;
        OnlineTurnierAnlage nurTeamname = OnlineTurnierAnlage.aus(turnier).orElseThrow();
        OnlineTurnierAnlage doublette = OnlineTurnierAnlage.aus(online("ko", "forme", "doublette")).orElseThrow();

        assertThat(nurTeamname.teamnameAnzeigen(false)).isTrue();
        assertThat(doublette.teamnameAnzeigen(false)).isFalse();
    }

    @Test
    void nichtAbbildbareTurniere() {
        OnlineTournamentDto meleeAndere = online("schweizer", "melee", "tete");
        meleeAndere.formationOther = true;

        assertThat(OnlineTurnierAnlage.aus(meleeAndere)).isEmpty();
        assertThat(OnlineTurnierAnlage.aus(online("schweizer", "melee", "tete"))).isEmpty();
        assertThat(OnlineTurnierAnlage.aus(online("trip_tete", "forme", "doublette"))).isEmpty();
        assertThat(OnlineTurnierAnlage.aus(online("liga", "melee", "doublette"))).isEmpty();
        assertThat(OnlineTurnierAnlage.aus(online("rangliste", "supermelee", "tete"))).isEmpty();
        assertThat(OnlineTurnierAnlage.aus(online("rangliste", "forme", "doublette"))).isEmpty();
        assertThat(OnlineTurnierAnlage.aus(online("unbekannt", "forme", "doublette"))).isEmpty();
    }

    @Test
    void ohneOnlineTypGibtEsKeinSystem() {
        assertThat(TurnierSystemOnlineTypMapping.turnierSystem(null, "forme")).isEqualTo(Optional.empty());
    }

    private static OnlineTournamentDto online(String type, String registrationType, String formation) {
        OnlineTournamentDto dto = new OnlineTournamentDto();
        dto.type = type;
        dto.registrationType = registrationType;
        dto.formation = formation;
        dto.status = "registration";
        return dto;
    }
}
