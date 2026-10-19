/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.onlinesync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.helper.i18n.I18n;

/** Verbindbar ist nur ein offenes Online-Turnier mit gleichem System, gleicher Anmeldeart und gleicher Formation. */
class OnlineVerbindbarkeitTest {

    private static final OnlineVerbindbarkeit SCHWEIZER_DOUBLETTE = OnlineVerbindbarkeit.fuer(TurnierSystem.SCHWEIZER,
            false, Optional.of("doublette"));

    @Test
    void passendesTurnierIstVerbindbar() {
        assertThat(SCHWEIZER_DOUBLETTE.bewerte(online("schweizer", "forme", "doublette")).istVerbindbar()).isTrue();
    }

    @Test
    void abgeschlossenesTurnierIstNichtVerbindbar() {
        OnlineTournamentDto turnier = online("schweizer", "forme", "doublette");
        turnier.status = "finished";

        assertThat(SCHWEIZER_DOUBLETTE.bewerte(turnier).hinderungsgrund())
                .contains(I18n.get("ptmonline.turnier.verbinden.grund.abgeschlossen"));
    }

    @Test
    void anderesTurniersystemNenntDasOnlineSystem() {
        assertThat(SCHWEIZER_DOUBLETTE.bewerte(online("ko", "forme", "doublette")).hinderungsgrund())
                .contains(I18n.get("ptmonline.turnier.verbinden.grund.turniersystem", TurnierSystem.KO.getBezeichnung()));
    }

    @Test
    void teamMeldelistePasstNurZuTeamAnmeldung() {
        assertThat(SCHWEIZER_DOUBLETTE.bewerte(online("schweizer", "melee", "doublette")).hinderungsgrund())
                .contains(I18n.get("ptmonline.turnier.verbinden.grund.anmeldeart",
                        I18n.get("ptmonline.anmeldeart.melee"), I18n.get("ptmonline.anmeldeart.forme")));
    }

    @Test
    void meleeAnmeldelistePasstNurZuMeleeAnmeldung() {
        OnlineVerbindbarkeit melee = OnlineVerbindbarkeit.fuer(TurnierSystem.SCHWEIZER, true, Optional.of("doublette"));

        assertThat(melee.bewerte(online("schweizer", "melee", "doublette")).istVerbindbar()).isTrue();
        assertThat(melee.bewerte(online("schweizer", "forme", "doublette")).istVerbindbar()).isFalse();
    }

    @Test
    void supermeleeVerlangtSupermeleeAnmeldung() {
        OnlineVerbindbarkeit supermelee = OnlineVerbindbarkeit.fuer(TurnierSystem.SUPERMELEE, false,
                Optional.of("triplette"));

        assertThat(supermelee.bewerte(online("rangliste", "supermelee", "triplette")).istVerbindbar()).isTrue();
        assertThat(supermelee.bewerte(online("rangliste", "forme", "triplette")).istVerbindbar()).isFalse();
    }

    @Test
    void andereFormationNenntOnlineUndDokument() {
        assertThat(SCHWEIZER_DOUBLETTE.bewerte(online("schweizer", "forme", "triplette")).hinderungsgrund())
                .contains(I18n.get("ptmonline.turnier.verbinden.grund.formation",
                        Formation.TRIPLETTE.getBezeichnung(), Formation.DOUBLETTE.getBezeichnung()));
    }

    @Test
    void nurTeamnamePasstZuFormationAndere() {
        OnlineVerbindbarkeit nurTeamname = OnlineVerbindbarkeit.fuer(TurnierSystem.KO, false,
                Optional.of(OnlineVerbindbarkeit.vergleichswert(Formation.NUR_TEAMNAME)));
        OnlineTournamentDto andere = online("ko", "forme", "tete");
        andere.formationOther = true;

        assertThat(nurTeamname.bewerte(andere).istVerbindbar()).isTrue();
        assertThat(nurTeamname.bewerte(online("ko", "forme", "tete")).hinderungsgrund())
                .contains(I18n.get("ptmonline.turnier.verbinden.grund.formation",
                        Formation.TETE.getBezeichnung(), I18n.get("ptmonline.formation.andere")));
    }

    @Test
    void ohneDokumentFormationEntfaelltDerVergleich() {
        OnlineVerbindbarkeit liga = OnlineVerbindbarkeit.fuer(TurnierSystem.LIGA, false, Optional.empty());

        assertThat(liga.bewerte(online("liga", "forme", "triplette")).istVerbindbar()).isTrue();
    }

    @Test
    void bewertetAlleTurniereInListenreihenfolge() {
        OnlineTournamentDto passend = online("schweizer", "forme", "doublette");
        OnlineTournamentDto unpassend = online("schweizer", "forme", "tete");

        assertThat(SCHWEIZER_DOUBLETTE.bewerte(List.of(unpassend, passend)))
                .extracting(OnlineVerbindbarkeit.Bewertung::turnier, OnlineVerbindbarkeit.Bewertung::istVerbindbar)
                .containsExactly(tuple(unpassend, false),
                        tuple(passend, true));
    }

    @Test
    void ordnetDokumentFormationDenOnlineWertenZu() {
        assertThat(OnlineVerbindbarkeit.vergleichswert(Formation.TETE)).isEqualTo("tete");
        assertThat(OnlineVerbindbarkeit.vergleichswert(Formation.DOUBLETTE)).isEqualTo("doublette");
        assertThat(OnlineVerbindbarkeit.vergleichswert(Formation.TRIPLETTE)).isEqualTo("triplette");
        assertThat(OnlineVerbindbarkeit.vergleichswert(Formation.MELEE)).isEqualTo(OnlineTournamentDto.FORMATION_ANDERE);
        assertThat(OnlineVerbindbarkeit.vergleichswert(Formation.NUR_TEAMNAME))
                .isEqualTo(OnlineTournamentDto.FORMATION_ANDERE);
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
