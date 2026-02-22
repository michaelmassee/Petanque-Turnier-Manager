package de.petanqueturniermanager.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class TeamMeldungenTest {

    private TeamMeldungen meldungen;

    @Before
    public void setup() {
        meldungen = new TeamMeldungen();
    }

    @Test
    public void testAddTeamWennNichtVorhanden() throws Exception {
        meldungen.addTeamWennNichtVorhanden(Team.from(1));
        assertThat(meldungen.size()).isEqualTo(1);
    }

    @Test
    public void testAddTeamKeinDuplikat() throws Exception {
        meldungen.addTeamWennNichtVorhanden(Team.from(1));
        meldungen.addTeamWennNichtVorhanden(Team.from(1));
        assertThat(meldungen.size()).isEqualTo(1);
    }

    @Test
    public void testAddMehrereTeams() throws Exception {
        meldungen.addTeamWennNichtVorhanden(Team.from(1));
        meldungen.addTeamWennNichtVorhanden(Team.from(2));
        meldungen.addTeamWennNichtVorhanden(Team.from(3));
        assertThat(meldungen.size()).isEqualTo(3);
    }

    @Test
    public void testAddTeamListeWennNichtVorhanden() throws Exception {
        List<Team> teamListe = Arrays.asList(Team.from(1), Team.from(2), Team.from(3));
        meldungen.addTeamWennNichtVorhanden(teamListe);
        assertThat(meldungen.size()).isEqualTo(3);
    }

    @Test
    public void testAddTeamListeKeineDuplikate() throws Exception {
        meldungen.addTeamWennNichtVorhanden(Team.from(1));
        List<Team> teamListe = Arrays.asList(Team.from(1), Team.from(2));
        meldungen.addTeamWennNichtVorhanden(teamListe);
        assertThat(meldungen.size()).isEqualTo(2);
    }

    @Test(expected = NullPointerException.class)
    public void testAddTeamListeNull() throws Exception {
        meldungen.addTeamWennNichtVorhanden((List<Team>) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddTeamListeLeer() throws Exception {
        meldungen.addTeamWennNichtVorhanden(Arrays.asList());
    }

    @Test(expected = NullPointerException.class)
    public void testAddTeamNull() throws Exception {
        meldungen.addTeamWennNichtVorhanden((Team) null);
    }

    @Test
    public void testGetMeldungenSortedByNr() throws Exception {
        meldungen.addTeamWennNichtVorhanden(Team.from(5));
        meldungen.addTeamWennNichtVorhanden(Team.from(2));
        meldungen.addTeamWennNichtVorhanden(Team.from(8));
        meldungen.addTeamWennNichtVorhanden(Team.from(1));

        List<IMeldung<Team>> sortiert = meldungen.getMeldungenSortedByNr();

        assertThat(sortiert).hasSize(4);
        assertThat(sortiert.get(0).getNr()).isEqualTo(1);
        assertThat(sortiert.get(1).getNr()).isEqualTo(2);
        assertThat(sortiert.get(2).getNr()).isEqualTo(5);
        assertThat(sortiert.get(3).getNr()).isEqualTo(8);
    }

    @Test
    public void testGetTeamFindetTeam() throws Exception {
        meldungen.addTeamWennNichtVorhanden(Team.from(3));
        meldungen.addTeamWennNichtVorhanden(Team.from(7));

        Team gefunden = meldungen.getTeam(7);

        assertThat(gefunden).isNotNull();
        assertThat(gefunden.getNr()).isEqualTo(7);
    }

    @Test
    public void testGetTeamNichtGefunden() throws Exception {
        meldungen.addTeamWennNichtVorhanden(Team.from(1));

        Team gefunden = meldungen.getTeam(99);

        assertThat(gefunden).isNull();
    }

    @Test
    public void testIsValidMitZuWenigTeams() throws Exception {
        meldungen.addTeamWennNichtVorhanden(Team.from(1));
        meldungen.addTeamWennNichtVorhanden(Team.from(2));
        meldungen.addTeamWennNichtVorhanden(Team.from(3));
        // genau 3 Teams -> ungueltig (benoetigt > 3)
        assertThat(meldungen.isValid()).isFalse();
    }

    @Test
    public void testIsValidMitAusreichendTeams() throws Exception {
        meldungen.addTeamWennNichtVorhanden(Team.from(1));
        meldungen.addTeamWennNichtVorhanden(Team.from(2));
        meldungen.addTeamWennNichtVorhanden(Team.from(3));
        meldungen.addTeamWennNichtVorhanden(Team.from(4));
        // 4 Teams -> gueltig
        assertThat(meldungen.isValid()).isTrue();
    }

    @Test
    public void testIsValidLeer() throws Exception {
        assertThat(meldungen.isValid()).isFalse();
    }

    @Test
    public void testSizeNachHinzufuegen() throws Exception {
        assertThat(meldungen.size()).isEqualTo(0);
        meldungen.addTeamWennNichtVorhanden(Team.from(1));
        assertThat(meldungen.size()).isEqualTo(1);
        meldungen.addTeamWennNichtVorhanden(Team.from(2));
        assertThat(meldungen.size()).isEqualTo(2);
    }

    @Test
    public void testFluentChaining() throws Exception {
        TeamMeldungen result = meldungen
                .addTeamWennNichtVorhanden(Team.from(1))
                .addTeamWennNichtVorhanden(Team.from(2));
        assertThat(result).isSameAs(meldungen);
        assertThat(meldungen.size()).isEqualTo(2);
    }

}
