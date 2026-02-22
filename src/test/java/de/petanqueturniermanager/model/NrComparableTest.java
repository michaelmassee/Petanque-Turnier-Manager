package de.petanqueturniermanager.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Testet die abstrakte Basisklasse NrComparable via der konkreten Klasse Team.
 */
public class NrComparableTest {

    @Test
    public void testKonstruktorNrNull() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> Team.from(0));
    }

    @Test
    public void testKonstruktorNrNegativ() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> Team.from(-1));
    }

    @Test
    public void testGetNr() throws Exception {
        Team team = Team.from(5);
        assertThat(team.getNr()).isEqualTo(5);
    }

    @Test
    public void testCompareToAufsteigendSortierung() throws Exception {
        // Niedrigere Nr kommt zuerst bei aufsteigender Sortierung
        Team team1 = Team.from(1);
        Team team5 = Team.from(5);

        assertThat(team1.compareTo(team5)).isNegative();
        assertThat(team5.compareTo(team1)).isPositive();
    }

    @Test
    public void testCompareToGleich() throws Exception {
        Team team3a = Team.from(3);
        Team team3b = Team.from(3);
        assertThat(team3a.compareTo(team3b)).isZero();
    }

    @Test
    public void testCompareToNull() throws Exception {
        // null gilt als kleiner, daher gibt this.compareTo(null) positive Zahl zurueck
        Team team = Team.from(1);
        assertThat(team.compareTo(null)).isPositive();
    }

    @Test
    public void testSortierungMitCollections() throws Exception {
        List<Team> teams = new ArrayList<>();
        teams.add(Team.from(8));
        teams.add(Team.from(2));
        teams.add(Team.from(5));
        teams.add(Team.from(1));

        Collections.sort(teams);

        assertThat(teams.get(0).getNr()).isEqualTo(1);
        assertThat(teams.get(1).getNr()).isEqualTo(2);
        assertThat(teams.get(2).getNr()).isEqualTo(5);
        assertThat(teams.get(3).getNr()).isEqualTo(8);
    }

    @Test
    public void testEqualsGleicheNr() throws Exception {
        Team team3a = Team.from(3);
        Team team3b = Team.from(3);
        assertThat(team3a).isEqualTo(team3b);
    }

    @Test
    public void testEqualsVerschiedeneNr() throws Exception {
        Team team1 = Team.from(1);
        Team team2 = Team.from(2);
        assertThat(team1).isNotEqualTo(team2);
    }

    @Test
    public void testEqualsNull() throws Exception {
        Team team = Team.from(1);
        assertThat(team.equals(null)).isFalse();
    }

    @Test
    public void testEqualsSichSelbst() throws Exception {
        Team team = Team.from(1);
        assertThat(team.equals(team)).isTrue();
    }

    @Test
    public void testHashCodeGleicheNr() throws Exception {
        Team team3a = Team.from(3);
        Team team3b = Team.from(3);
        assertThat(team3a.hashCode()).isEqualTo(team3b.hashCode());
    }

    @Test
    public void testHashCodeVerschiedeneNr() throws Exception {
        Team team1 = Team.from(1);
        Team team2 = Team.from(2);
        // verschiedene Nr sollten unterschiedliche Hashcodes liefern
        assertThat(team1.hashCode()).isNotEqualTo(team2.hashCode());
    }

}
