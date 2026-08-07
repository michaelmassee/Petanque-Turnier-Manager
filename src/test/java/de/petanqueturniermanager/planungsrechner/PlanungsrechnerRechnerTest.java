package de.petanqueturniermanager.planungsrechner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.planungsrechner.PlanungsrechnerRechner.ZeitlimitErgebnis;
import de.petanqueturniermanager.planungsrechner.PlanungsrechnerRechner.ZeitplanErgebnis;

public class PlanungsrechnerRechnerTest {

    // Durchgehendes Beispiel aus turniersysteme/ZEITPLAN.md: 16 Teams (=8 Paarungen), 3 Bahnen
    // -> Durchgaenge [3, 3, 2], Start 09:00, Zeitlimit 15 Min, Durchgang-Pause 5 Min, Runden-Pause 10 Min.

    @Test
    public void testBerechneZeitlimit_ausZeitplanMdBeispiel() {
        ZeitlimitErgebnis ergebnis = PlanungsrechnerRechner.berechneZeitlimit(
                LocalTime.of(9, 0), LocalTime.of(9, 55), 16, 3, 1, 5, 10);

        assertThat(ergebnis.durchgaengeProRunde()).isEqualTo(3);
        assertThat(ergebnis.zeitlimitProDurchgangMinuten()).isEqualTo(15);
    }

    @Test
    public void testBerechneZeitplan_ausZeitplanMdBeispiel_zweiRunden() {
        ZeitplanErgebnis ergebnis = PlanungsrechnerRechner.berechneZeitplan(
                LocalTime.of(9, 0), 16, 3, 2, 5, 10, 15);

        assertThat(ergebnis.eintraege()).hasSize(6);

        assertThat(ergebnis.eintraege().get(0).rundeNr()).isEqualTo(1);
        assertThat(ergebnis.eintraege().get(0).durchgangNr()).isEqualTo(1);
        assertThat(ergebnis.eintraege().get(0).start()).isEqualTo(LocalTime.of(9, 0));
        assertThat(ergebnis.eintraege().get(0).ende()).isEqualTo(LocalTime.of(9, 15));

        assertThat(ergebnis.eintraege().get(1).start()).isEqualTo(LocalTime.of(9, 20));
        assertThat(ergebnis.eintraege().get(1).ende()).isEqualTo(LocalTime.of(9, 35));

        assertThat(ergebnis.eintraege().get(2).start()).isEqualTo(LocalTime.of(9, 40));
        assertThat(ergebnis.eintraege().get(2).ende()).isEqualTo(LocalTime.of(9, 55));

        // Runde 2 startet nach Runden-Pause (10 Min) auf dem Ende von Runde 1
        assertThat(ergebnis.eintraege().get(3).rundeNr()).isEqualTo(2);
        assertThat(ergebnis.eintraege().get(3).durchgangNr()).isEqualTo(1);
        assertThat(ergebnis.eintraege().get(3).start()).isEqualTo(LocalTime.of(10, 5));
        assertThat(ergebnis.eintraege().get(3).ende()).isEqualTo(LocalTime.of(10, 20));

        assertThat(ergebnis.eintraege().get(5).ende()).isEqualTo(LocalTime.of(11, 0));
        assertThat(ergebnis.turnierEnde()).isEqualTo(LocalTime.of(11, 0));
    }

    @Test
    public void testDurchgaengeGleichEins_keineDurchgangPauseWirksam() {
        // 6 Teams (3 Paarungen), 5 Bahnen -> alle Paarungen passen in einen Durchgang
        ZeitplanErgebnis ergebnis = PlanungsrechnerRechner.berechneZeitplan(
                LocalTime.of(9, 0), 6, 5, 1, 5, 10, 20);

        assertThat(ergebnis.eintraege()).hasSize(1);
        assertThat(ergebnis.eintraege().get(0).start()).isEqualTo(LocalTime.of(9, 0));
        assertThat(ergebnis.eintraege().get(0).ende()).isEqualTo(LocalTime.of(9, 20));
        assertThat(ergebnis.turnierEnde()).isEqualTo(LocalTime.of(9, 20));
    }

    @Test
    public void testUngeradeTeamzahl_abgerundetePaarungen() {
        // 7 Teams -> 3 Paarungen (Integer-Division, kein Freilos-Konzept in diesem Rechner)
        ZeitplanErgebnis ergebnis = PlanungsrechnerRechner.berechneZeitplan(
                LocalTime.of(9, 0), 7, 2, 1, 5, 10, 10);

        // 3 Paarungen / 2 Bahnen -> Durchgaenge [2, 1]
        assertThat(ergebnis.eintraege()).hasSize(2);
    }

    @Test
    public void testBerechneZeitlimit_rundungAbwaerts() {
        // 50 Minuten verfuegbare Spielzeit auf 3 Durchgaenge -> 16 (nicht 16,67)
        ZeitlimitErgebnis ergebnis = PlanungsrechnerRechner.berechneZeitlimit(
                LocalTime.of(9, 0), LocalTime.of(10, 0), 16, 3, 1, 5, 10);

        // 60 Min gesamt - 2*5 Min Durchgang-Pausen = 50 Min verfuegbare Spielzeit / 3 Durchgaenge
        assertThat(ergebnis.zeitlimitProDurchgangMinuten()).isEqualTo(16);
    }

    @Test
    public void testIllegalArg_endeVorStart() {
        assertThatThrownBy(() -> PlanungsrechnerRechner.berechneZeitlimit(
                LocalTime.of(10, 0), LocalTime.of(9, 0), 16, 3, 1, 5, 10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testIllegalArg_zeitFuerPausenNichtAusreichend() {
        assertThatThrownBy(() -> PlanungsrechnerRechner.berechneZeitlimit(
                LocalTime.of(9, 0), LocalTime.of(9, 5), 16, 3, 1, 5, 10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testIllegalArg_anzahlBahnenNull() {
        assertThatThrownBy(() -> PlanungsrechnerRechner.berechneZeitplan(
                LocalTime.of(9, 0), 16, 0, 1, 5, 10, 15))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testIllegalArg_zuWenigTeams() {
        assertThatThrownBy(() -> PlanungsrechnerRechner.berechneZeitplan(
                LocalTime.of(9, 0), 1, 3, 1, 5, 10, 15))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testIllegalArg_zeitlimitNull() {
        assertThatThrownBy(() -> PlanungsrechnerRechner.berechneZeitplan(
                LocalTime.of(9, 0), 16, 3, 1, 5, 10, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
